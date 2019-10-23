/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.handlers;

import com.metreeca.rdf.Values;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.JSONAssert;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import javax.json.JsonValue;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf.services.GraphTest.exec;
import static com.metreeca.rdf.services.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;


final class SPARQLTest {

	private static final Object Root=new Object();

	private static final Statement First=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);
	private static final Statement Rest=statement(RDF.NIL, RDF.VALUE, RDF.REST);


	private SPARQL endpoint() {
		return new SPARQL().query(Root).update(Root);
	}


	private SPARQL _private(final SPARQL endpoint) {
		return endpoint;
	}

	private SPARQL _public(final SPARQL endpoint) {
		return endpoint.query();
	}


	private Request anonymous(final Request request) {
		return request;
	}

	private Request authenticated(final Request request) {
		return request.roles(Root);
	}


	private Request get(final Request request) {
		return request.method(Request.GET);
	}

	private Request post(final Request request) {
		return request.method(Request.POST);
	}


	private Request bool() {
		return new Request()
				.header("Accept", "application/json")
				.parameters("query", "ask { ?s ?p ?o }");
	}

	private Request tuple() {
		return new Request()
				.header("Accept", "application/json")
				.parameters("query", "select ?o { ?s ?p ?o }");
	}

	private Request graph() {
		return new Request()
				.header("Accept", "text/turtle")
				.parameters("query", "construct where { ?s ?p ?o }");
	}

	private Request update() {
		return new Request()
				.header("Accept", "application/json")
				.parameter("update", format(
						"prefix rdf: <%s> insert data { rdf:nil rdf:value rdf:rest }", RDF.NAMESPACE)
				);
	}

	private Request malformed(final String type) {
		return new Request().parameter(type, "!!!");
	}


	private Consumer<JsonValue> hasBooleanValue(final boolean value) {
		return json -> JSONAssert.assertThat(json.asJsonObject()).hasField("boolean", value);
	}

	private Consumer<JsonValue> hasBindings(final IRI... iris) {
		return json -> assertThat(json
				.asJsonObject()
				.getJsonObject("results")
				.getJsonArray("bindings")
				.stream()
				.map(b -> b.asJsonObject().getJsonObject("o"))
				.filter(b -> b.getString("type").equals("uri"))
				.map(b -> b.getString("value"))
				.map(Values::iri)
				.collect(toList())
		).containsExactly(iris);
	}

	private Consumer<Model> hasObjects(final IRI... objects) {
		return model -> assertThat(model.objects()).containsExactly(objects);
	}


	//// Boolean Query /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json)
								.satisfies(hasBooleanValue(true))
						)
				)
		);
	}

	@Test void testPrivateAuthenticatedPOSTBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json ->
								JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)))));
	}


	@Test void testPublicAnonymousGETBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAnonymousPOSTBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAuthenticatedGETBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAuthenticatedPOSTBooleanQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}


	//// Tuple Query ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETTupleQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTTupleQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETTupleQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPrivateAuthenticatedPOSTTupleQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}


	@Test void testPublicAnonymousGETTupleQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAnonymousPOSTTupleQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedGETTupleQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedPOSTTupleQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}


	//// Graph Query ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETGraphQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTGraphQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(anonymous(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETGraphQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST))
						)
				));
	}

	@Test void testPrivateAuthenticatedPOSTGraphQuery() {
		exec(model(asList(First, Rest)), () -> _private(endpoint())

				.handle(authenticated(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}


	@Test void testPublicAnonymousGETGraphQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAnonymousPOSTGraphQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(anonymous(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedGETGraphQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST))
						)
				));
	}

	@Test void testPublicAuthenticatedPOSTGraphQuery() {
		exec(model(asList(First, Rest)), () -> _public(endpoint())

				.handle(authenticated(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.satisfies(hasObjects(RDF.FIRST, RDF.REST))
						)
				));
	}


	//// Update ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(anonymous(get(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(anonymous(post(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(authenticated(get(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(model()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}

	@Test void testPrivateAuthenticatedPOSTUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(model()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}


	@Test void testPublicAnonymousGETUpdate() {
		exec(model(singletonList(First)), () -> _public(endpoint())

				.handle(anonymous(get(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPublicAnonymousPOSTUpdate() {
		exec(model(singletonList(First)), () -> _public(endpoint())

				.handle(anonymous(post(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPublicAuthenticatedGETUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(model()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}

	@Test void testPublicAuthenticatedPOSTUpdate() {
		exec(model(singletonList(First)), () -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JSONAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(model()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}


	//// Malformed /////////////////////////////////////////////////////////////////////////////////////////////////////


	@Test void testGETNoAction() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(get(new Request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTNoAction() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(post(new Request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}


	@Test void testGETBothQueryAndUpdate() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(get(new Request())
						.parameter("query", "query")
						.parameter("update", "update")
				))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTBothQueryAndUpdate() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(post(new Request())
						.parameter("query", "query")
						.parameter("update", "update")
				))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}


	@Test void testGETQueryMalformed() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(get(malformed("query"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTQueryMalformed() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(post(malformed("query"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testGETUpdateMalformed() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(get(malformed("update"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTUpdateMalformed() {
		exec(model(emptyList()), () -> _private(endpoint())

				.handle(authenticated(post(malformed("update"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

}
