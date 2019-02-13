/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Sets;
import com.metreeca.rest.Result;
import com.metreeca.rest.handlers.Actor.NotImplementedException;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.engines.Flock.flock;


/**
 * Shape-driven resource engine.
 *
 * <p>Manages CRUD lifecycle operations on resource descriptions defined by a shape.</p>
 */
final class ShapedResource extends GraphEntity {

	private final Graph graph;
	private final Flock flock;

	private final Shape relate;
	private final Shape update;
	private final Shape delete;

	private final Shape convey;


	ShapedResource(final Graph graph, final Shape shape) {

		this.graph=graph;
		this.flock=flock(metas(shape)).orElseGet(Flock.None::new);

		this.relate=redact(shape, Form.relate, Form.detail);
		this.update=redact(shape, Form.update, Form.detail);
		this.delete=redact(shape, Form.delete, Form.detail);

		this.convey=relate.map(new Redactor(Form.mode, Form.convey)).map(new Optimizer());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> relate(final IRI resource) {
		return graph.query(connection -> { return retrieve(resource, relate).orElseGet(Sets::set); });
	}

	@Override public <V, E> Result<V, E> relate(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser, final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		return parser.apply(relate).fold(

				query -> {

					if ( query.equals(edges(relate))) {

						return Value(mapper.apply(convey, relate(resource)));

					} else {

						throw new NotImplementedException("shaped resource filtered retrieval not supported");

					}
				},

				Result::Error
		);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		throw new NotImplementedException("shaped related resource creation not supported");
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		return graph.update(connection -> {

			return retrieve(resource, update).map(current -> { // identify updatable description

				// validate before updating graph to support snapshot transactions

				final Focus focus=new GraphValidator().validate(resource, update, model);

				if ( focus.assess(Issue.Level.Error) ) {

					connection.rollback();

				} else {

					connection.remove(current);
					connection.add(model);

					connection.commit();
				}

				return focus;

			});

		});
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		return graph.update(connection -> {

			// !!! merge retrieve/remove operations into a single SPARQL update txn
			// !!! must check resource existence anyway and would break for CBD shapes

			return retrieve(resource, delete).map(current -> { // identify deletable description

				flock.remove(connection, resource, current).remove(current);

				return resource;

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Collection<Statement>> retrieve(final IRI resource, final Shape shape) {
		return Optional.of(new GraphRetriever())
				.map(retriever -> retriever.retrieve(resource, edges(and(all(resource), shape))))
				.filter(model -> !model.isEmpty());
	}

}
