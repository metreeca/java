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

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Issue.Level;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.function.BiFunction;

import javax.json.JsonValue;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.repository.util.Connections.getStatement;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;


/**
 * LDP resource creator.
 *
 * <p>Handles creation requests on the stored linked data basic resource container identified by the request
 * {@linkplain Request#item() focus item}.</p>
 *
 * <p>If the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#create}
 * task, {@link Form#verify} mode and {@link Form#detail} view;</li>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain an RDF description of the resource to be created
 * matched by the redacted shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain a symmetric concise bounded description of the
 * resource to be created; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
 *
 * </ul>
 *
 * <p>The request RDF body must describe the resource to be created using the request {@linkplain Request#item() focus
 * item} as subject.</p>
 *
 * <p>On successful body validation:</p>
 *
 * <ul>
 *
 * <li>the resource to be created is assigned a unique IRI based on the stem of the request {@linkplain Request#item()
 * focus item} and a name provided by either the default {@linkplain #uuid() UUID-based} or a {@linkplain
 * #Creator(BiFunction) custom-provided} slug generator;</li>
 *
 * <li>the request RDF body is rewritten to the assigned IRI and stored into the graph database;</li>
 *
 * <li>the target basic container identified by the request focus item is connected to the newly created resource using
 * the {@link LDP#CONTAINS ldp:contains} property.</li>
 *
 * </ul>
 *
 * <p>On successful resource creation, the IRI of the newly created resource is advertised through the {@code Location}
 * HTTP response header.</p>
 *
 * <p>Regardless of the operating mode, RDF data is inserted into the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Creator extends Actor {

	/**
	 * Creates a random UUID-based slug generator.
	 *
	 * @return a slug generator returning a new random UUID for each call
	 */
	public static BiFunction<Request, Collection<Statement>, String> uuid() {
		return (request, model) -> randomUUID().toString();
	}

	/**
	 * Creates a sequential auto-incrementing slug generator.
	 *
	 * <p><strong>Warning</strong> / SPARQL doesn't natively support auto-incrementing ids: auto-incrementing slug
	 * calls are partly serialized in the system {@linkplain Graph#Factory graph} database using an internal lock
	 * object; this strategy may fail for distributed containers or external concurrent updates on the SPARQL endpoint,
	 * causing requests to fail with an {@link Response#InternalServerError} or {@link Response#Conflict} status
	 * code.</p>
	 *
	 * @return a slug generator returning an auto-incrementing numeric id unique to the focus item of the request
	 */
	public static BiFunction<Request, Collection<Statement>, String> auto() {
		return new AutoGenerator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Trace trace=tool(Trace.Factory);

	/*
	 * Shared lock for taming serialization issues with slug operations (concurrent graph txns may produce conflicts).
	 */
	private final Object lock=new Object();


	private final BiFunction<Request, Collection<Statement>, String> slug;


	/**
	 * Creates a resource creator with a {@linkplain #uuid() UUID} slug generator.
	 */
	public Creator() {
		this(uuid());
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param slug a function mapping from the creation request and its RDF payload to the name to be assigned to the
	 *             newly created resource; must return a non-null and non-empty value
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public Creator(final BiFunction<Request, Collection<Statement>, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		delegate(creator().with(throttler()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return new Throttler(Form.create, Form.detail, resource());
	}

	private Handler creator() {
		return request -> request.body(rdf()).fold(

				model -> {
					synchronized ( lock ) { // attempt to serialize slug operations from multiple txns

						final String name=slug.apply(request, model);

						if ( name == null ) {
							throw new NullPointerException("null resource name");
						}

						if ( name.isEmpty() ) {
							throw new IllegalArgumentException("empty resource name");
						}

						final IRI source=request.item();
						final IRI target=iri(request.stem(), name);

						return request.reply(response -> engine(request.shape())

								// !!! recognize txns failures due to conflicting slugs and report as 409 Conflict

								.create(source, target, trace.debug(this, rewrite(source, target, model)))

								.map(focus -> focus.assess(Level.Error) // shape violations

										? response.map(new Failure()
										.status(Response.UnprocessableEntity)
										.error(Failure.DataInvalid)
										.trace(focus))

										: response
										.status(Response.Created)
										.header("Location", target.stringValue())

								)

								.orElseGet(() -> {

									trace.error(this, format("conflicting slug {%s}", target));

									return response.map(new Failure()
											.status(Response.InternalServerError)
											.cause("see server logs for details")
									);

								})

						);

					}
				},

				request::reply

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> rewrite(final IRI source, final IRI target, final Collection<Statement> model) {
		return model.stream().map(statement -> rewrite(source, target, statement)).collect(toList());
	}

	private Statement rewrite(final IRI source, final IRI target, final Statement statement) {
		return statement(
				rewrite(source, target, statement.getSubject()),
				rewrite(source, target, statement.getPredicate()),
				rewrite(source, target, statement.getObject()),
				rewrite(source, target, statement.getContext())
		);
	}

	private <T extends Value> T rewrite(final T source, final T target, final T value) {
		return source.equals(value) ? target : value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AutoGenerator implements BiFunction<Request, Collection<Statement>, String> {

		private static final IRI Auto=iri("app://rest.metreeca.com/terms#", "auto");


		private final Graph graph=tool(Graph.Factory);


		@Override public String apply(final Request request, final Collection<Statement> model) {
			return graph.update(connection -> {

				// !!! custom name pattern
				// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
				// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)

				final IRI stem=iri(request.stem());

				long id=getStatement(connection, stem, Auto, null)
						.map(Statement::getObject)
						.filter(value -> value instanceof Literal)
						.map(value -> {
							try {
								return ((Literal)value).longValue();
							} catch ( final NumberFormatException e ) {
								return 0L;
							}
						})
						.orElse(0L);

				IRI iri;

				do {

					iri=iri(stem.stringValue(), String.valueOf(++id));

				} while ( connection.hasStatement(iri, null, null, true)
						|| connection.hasStatement(null, null, iri, true) );

				connection.remove(stem, Auto, null);
				connection.add(stem, Auto, literal(id));

				return String.valueOf(id);

			});
		}
	}

}
