/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.codecs.ShapeCodec;
import com.metreeca.form.probes.Inferencer;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.next.*;
import com.metreeca.next.formats._RDF;
import com.metreeca.next.formats._Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;


/**
 * Model-driven RDF body manager.
 *
 * <p>Drives the lifecycle of linked data resources managed by the wrapped handler associating them to a {@linkplain
 * #shape(Shape) shape} model:
 *
 * <ul>
 *
 * <li>associates the shape model to incoming requests as a {@link _Shape} body;</li
 *
 * <li>associates the shape model to outgoing responses as a {@link _Shape} body;</li>
 *
 * <li>advertises the association between the response focus {@linkplain Response#item() item} and the shape model
 * through a "{@code Link: <resource?specs>; rel=http://www.w3.org/ns/ldp#constrainedBy}" header;</li>
 *
 * <li>handles GET requests for the advertised shape model resource ({@code <resource?specs>}) with a response
 * containing an RDF body structured like:
 *
 * <pre>{@code @prefix form: <app://form.metreeca.com/terms#>
 *
 * <resource> ldp:constrainedBy <resource?specs>.
 *
 * <resource?specs>
 *      [form:create <shape>];
 *      [form:relate <shape>];
 *      [form:update <shape>];
 *      [form:delete <shape>].
 *
 * }</pre>
 *
 * where {@code <shape>} is the RDF description generated by {@link ShapeCodec#encode(Shape, Collection)} of the {@link
 * #shape(Shape) shape} model {@linkplain Redactor redacted} taking into account the target resource task and the
 * current request {@linkplain Request#user() user}; target tasks assciated with an empty task-specific shape are
 * omitted. from the description.</li>
 *
 * </ul>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpr-resource">Linked Data Platform 1.0 - § 4.2.1.6 Resource -
 * ldp:constrainedBy</a>
 */
public final class Driver implements Wrapper {

	private static final String SpecsQuery="specs";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the shape model for this driver.
	 *
	 * @param shape the shape driving the lifecycle of the linked data resources managed by the wrapped handler
	 *
	 * @return this driver
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Driver shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape.accept(new Optimizer());

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> specs(request).orElseGet(() ->
				handler.handle(request.map(this::before)).map(this::after)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Responder> specs(final Request request) {

		// !!! handle structured query strings
		// !!! handle HEAD requests on ?specs
		// !!! check resource existence in wrapped container (HEAD request? what about virtual containers?)

		return shape != null && request.method().equals(Request.GET) && request.query().equals(SpecsQuery) ?

				Optional.of(request.reply(response -> {

					final IRI focus=request.item();
					final IRI specs=iri(focus+"?"+SpecsQuery);

					final Collection<Statement> model=new ArrayList<>();

					model.add(statement(focus, LDP.CONSTRAINED_BY, specs));

					final Shape shape=this.shape
							.accept(role(request.roles())) // limit shape to user-visible details
							.accept(mode(Form.verify)) // hide internal filtering specs
							.accept(new Inferencer())
							.accept(new Optimizer());

					final ShapeCodec codec=new ShapeCodec();

					for (final IRI task : list(Form.create, Form.relate, Form.update, Form.delete)) {

						final Shape spec=shape.accept(task(task));

						if ( !empty(spec) ) {
							model.add(statement(specs, task, codec.encode(spec, model)));
						}

					}

					return response.status(Response.OK)
							// !!! .body(_Shape.Format, ___) provide (recursive) shape for task shapes ;)
							.body(_RDF.Format, model);

				})) :

				Optional.empty();
	}


	private Request before(final Request request) {
		return shape == null ? request : request
				.body(_Shape.Format, shape);
	}

	private Response after(final Response response) {
		return shape == null ? response : response
				.header("+link", String.format("<%s?%s>; rel=%s", response.item(), SpecsQuery, LDP.CONSTRAINED_BY)) // !!! append value
				.body(_Shape.Format, shape);
	}

}
