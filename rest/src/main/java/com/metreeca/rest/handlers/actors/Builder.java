/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Virtual resource builder.
 *
 * <p>Handles retrieval requests on virtual linked data resources.</p>
 *
 * <dl>
 *
 * <dt>Request {@link ShapeFormat} body {optional}</dt>
 *
 * <dd>An optional linked data shape driving the retrieval process.</dd>
 *
 * <dt>Response {@link ShapeFormat} body {optional}</dt>
 *
 * <dd>If the request includes a shape payload, the response includes the derived shape actually used in the resource
 * retrieval process, redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task,
 * {@link Form#verify} mode and {@link Form#detail} view.</dd>
 *
 * <dt>Response {@link RDFFormat} body</dt>
 *
 * <dd>The response includes the {@linkplain RDFFormat RDF description} of the virtual request {@linkplain
 * Request#item() focus item}, as retrieved from the {@linkplain #model(Function) virtual model generator}.</dd>
 *
 * <dd>Empty generated models are reported with a {@link Response#NotFound} status code.</dd>
 *
 * </dl>
 */
public final class Builder extends Actor<Builder> {

	private Function<Request, Collection<Statement>> model=request -> emptySet();

	private final Graph graph=tool(Graph.Factory);


	public Builder() {
		delegate(handler(Form.relate, Form.detail, (request, shape) -> {

			final Collection<Statement> model=this.model.apply(request);

			return request.reply(response -> model.isEmpty()

					? response.status(Response.NotFound)

					: response.status(Response.OK)

					.body(shape()).set(shape.accept(mode(Form.verify)))// hide filtering constraints // !!! add only if non empty
					.body(rdf()).set(shape.accept(new Restrictor(model, singleton(response.item()))).collect(toList()))
			);

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the SPARQL virtual model generator.
	 *
	 * <p>The model for the virtual resource is generated by a SPARQL graph query executed on the shared {@linkplain
	 * Graph#Factory graph} tool, with the following pre-defined bindings:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>this</td>
	 * <td>the value of the request {@linkplain Request#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>user</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>time</td>
	 * <td>an {@code xsd:dateTime} literal representing the current system time with millisecond precision</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param graph the SPARQL graph query to be used as virtual model generator for this builder; empty queries are
	 *              ignored
	 *
	 * @return this builder
	 *
	 * @throws NullPointerException if {@code graph} is null
	 */
	public Builder model(final String graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph query");
		}

		return model(request -> graph.isEmpty() ? emptySet() : this.graph.query(connection -> {

			final GraphQuery query=connection.prepareGraphQuery(QueryLanguage.SPARQL, graph, request.base());

			query.setBinding("this", request.item());
			query.setBinding("user", request.user());
			query.setBinding("time", time(true));

			final Collection<Statement> model=new ArrayList<>();

			query.evaluate(new AbstractRDFHandler() {
				@Override public void handleStatement(final Statement statement) { model.add(statement); }
			});

			return model;

		}));
	}

	/**
	 * Configures the virtual model generator.
	 *
	 * @param model a function mapping from a request to a possibly empty RDF model; must return a non null value
	 *
	 * @return this builder
	 *
	 * @throws NullPointerException if {@code model} is null
	 */
	public Builder model(final Function<Request, Collection<Statement>> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		this.model=model;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Builder post(final BiFunction<Response, Model, Model> filter) {
		return super.post(filter);
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Model restrictor.
	 *
	 * <p>Recursively extracts from a model and an initial collection of source values all the statements compatible
	 * with a shape.</p>
	 */
	private static final class Restrictor extends Shape.Probe<Stream<Statement>> {

		private final Collection<Statement> model;
		private final Collection<Value> sources;


		private Restrictor(final Collection<Statement> model, final Collection<Value> sources) {
			this.model=model;
			this.sources=sources;
		}


		@Override protected Stream<Statement> fallback(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Statement> visit(final Trait trait) {

			final Step step=trait.getStep();

			final IRI iri=step.getIRI();
			final boolean inverse=step.isInverse();

			final Function<Statement, Value> source=inverse
					? Statement::getObject
					: Statement::getSubject;

			final Function<Statement, Value> target=inverse
					? Statement::getSubject
					: Statement::getObject;

			final Collection<Statement> restricted=model.stream()
					.filter(s -> sources.contains(source.apply(s)) && iri.equals(s.getPredicate()))
					.collect(toList());

			final Set<Value> focus=restricted.stream()
					.map(target)
					.collect(toSet());

			return Stream.concat(restricted.stream(), trait.getShape().accept(new Restrictor(model, focus)));
		}

		@Override public Stream<Statement> visit(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Test test) {
			return Stream.concat(test.getPass().accept(this), test.getFail().accept(this));
		}

		@Override public Stream<Statement> visit(final Group group) {
			return group.getShape().accept(this);
		}

	}

}
