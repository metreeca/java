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

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.toList;


/**
 * Basic container browser.
 *
 * <p>Handles retrieval requests on linked data basic container.</p>
 *
 * <dl>
 *
 * <dt>Request {@link ShapeFormat} body {optional}</dt>
 *
 * <dd>An optional linked data shape driving the retrieval process.</dd>
 *
 * <dt>Response {@link ShapeFormat} body {optional}</dt>
 *
 * <dd>If the request includes a shape payload, the response includes the derived shape actually used in the container
 * retrieval process, redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task,
 * {@link Form#verify} mode and {@link Form#digest} view.</dd>
 *
 * <dt>Response shape-driven {@link RDFFormat} body</dt>
 *
 * <dd>If the request includes a {@link ShapeFormat} body, the response includes the {@linkplain RDFFormat RDF
 * description} of the request {@linkplain Request#item() focus item}, {@linkplain LDP#CONTAINS containing} the RDF
 * descriptions of the virtual container items matched by the redacted linked data {@linkplain Shape shape}.</dd>
 *
 * <dd>If the request contains a {@code Prefer} header requesting the {@link LDP#PREFER_EMPTY_CONTAINER}
 * representation, virtual item descriptions are omitted.</dd>
 *
 * <dd>If the request contains a filtering {@linkplain Request#query(Shape) query}, only matching virtual container
 * item descriptionss are included.</dd>
 *
 * <dt>Response shapeless {@link RDFFormat} body</dt>
 *
 * <dd><strong>Warning</strong> / Shapeless container retrieval is not yet supported and is reported with a {@linkplain
 * Response#NotImplemented} HTTP status code.</dd>
 *
 * </dl>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
 */
public final class Browser extends Actor<Browser> {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return handler(Form.relate, Form.digest, shape ->

				Shape.empty(shape) ? direct(request) : driven(request, shape)

		).handle(request)

				.map(response -> response.headers("+Link",

						link(LDP.CONTAINER, "type"),
						link(LDP.BASIC_CONTAINER, "type")

				))

				.map(response -> response.success() ?

						response.headers("Vary", "Accept", "Prefer") : response

				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request) {
		return request.reply(response -> response.map(new Failure<>()
				.status(Response.NotImplemented)
				.cause("shapeless container browsing not supported"))
		);
	}

	private Responder driven(final Request request, final Shape shape) {
		return request.reply(response -> request.query(shape).map(

				query -> graph.query(connection -> {

					return response.status(Response.OK).map(r -> query.accept(new Query.Probe<Response>() {

						@Override public Response visit(final Edges edges) {

							final Collection<Statement> model=new ArrayList<>();

							// container metadata

							model.add(statement(request.item(), RDF.TYPE, LDP.BASIC_CONTAINER));

							if ( !include(request, LDP.PREFER_EMPTY_CONTAINER) ) {

								// retrieve filtered content from repository

								final Map<Value, Collection<Statement>> matches=new SPARQLEngine(connection).browse(query);

								matches.forEach((item, statements) -> {
									model.add(statement(request.item(), LDP.CONTAINS, item));
									model.addAll(statements);
								});

							} else {
								response.header("Preference-Applied", String.format(
										"return=representation; include=\"%s\"", LDP.PREFER_EMPTY_CONTAINER
								));
							}

							return r.body(shape()).set(trait(LDP.CONTAINS, shape.accept(mode(Form.verify)))) // hide filtering constraints
									.body(rdf()).set(model);
						}

						@Override public Response visit(final Stats stats) { // !!! re/factor

							return r.body(shape()).set(StatsShape)
									.body(rdf()).set(new SPARQLEngine(connection).browse(query).values().stream()
											.flatMap(statements -> statements.stream().map(statement -> rewrite(statement, Form.meta, request.item())))
											.collect(toList()));
						}

						@Override public Response visit(final Items items) {

							return r.body(shape()).set(ItemsShape)
									.body(rdf()).set(new SPARQLEngine(connection).browse(query).values().stream()
											.flatMap(statements -> statements.stream().map(statement -> rewrite(statement, Form.meta, request.item())))
											.collect(toList()));
						}

					}));

				}),

				response::map

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean include(final Request request, final IRI include) {

		// !!! handle multiple uris in include parameter
		// !!! handle omit parameter (with multiple uris)
		// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters

		final String representation=request
				.headers("Prefer")
				.stream()
				.map(value -> {

					final Matcher matcher=RepresentationPattern.matcher(value);

					return matcher.matches() ? matcher.group("representation") : "";

				})
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");

		return representation.equals(include.stringValue());

	}

}
