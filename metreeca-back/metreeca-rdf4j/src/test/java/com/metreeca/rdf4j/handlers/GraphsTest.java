/*
 * Copyright © 2013-2022 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.handlers;

import com.metreeca.rdf4j.services.GraphTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.formats.InputFormat;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.http.Locator.service;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.encode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rdf4j.services.GraphTest.exec;
import static com.metreeca.rdf4j.services.GraphTest.export;
import static com.metreeca.rest.Response.Unauthorized;
import static com.metreeca.rest.ResponseAssert.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;


final class GraphsTest {

	private static final Statement First=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);
	private static final Statement Rest=statement(RDF.NIL, RDF.VALUE, RDF.REST);


	private Model catalog() {
		return new LinkedHashModel(asList(
				statement(Root, RDF.VALUE, RDF.NIL),
				statement(RDF.NIL, RDF.TYPE, VOID.DATASET)
		));
	}

	private Model dflt() {
		return service(graph()).query(connection -> {

			return export(connection, (Resource)null);

		});
	}

	private Model named() {
		return service(graph()).query(connection -> {

			return export(connection, RDF.NIL).stream()
					.map(s -> statement(s.getSubject(), s.getPredicate(), s.getObject())) // strip context info
					.collect(toCollection(LinkedHashModel::new));

		});
	}


	private Model model(final Statement... model) {
		return new LinkedHashModel(asList(model));
	}


	private Runnable named(final Statement... model) {
		return GraphTest.model(asList(model), RDF.NIL);
	}

	private Runnable dflt(final Statement... model) {
		return GraphTest.model(asList(model), (Resource)null);
	}


	private Graphs endpoint() {
		return Graphs.graphs().query(Root).update(Root);
	}

	private Request request() {
		return new Request().base(Base);
	}


	private Graphs _private(final Graphs endpoint) {
		return endpoint;
	}

	private Graphs _public(final Graphs endpoint) {
		return endpoint.query(emptySet());
	}


	private Request anonymous(final Request request) {
		return request;
	}

	private Request authenticated(final Request request) {
		return request.roles(Root);
	}


	private Request catalog(final Request request) {
		return request.method(Request.GET);
	}

	private Request get(final Request request) {
		return request.method(Request.GET);
	}

	private Request put(final Request request) {
		return request.method(Request.PUT).body(InputFormat.input(), () ->
				new ByteArrayInputStream(encode(model(Rest)).getBytes(UTF_8))
		);
	}

	private Request delete(final Request request) {
		return request.method(Request.DELETE);
	}

	private Request post(final Request request) {
		return request.method(Request.POST).body(InputFormat.input(), () ->
				new ByteArrayInputStream(encode(model(Rest)).getBytes(UTF_8))
		);
	}


	private Request dflt(final Request request) {
		return request.parameter("default", "");
	}

	private Request named(final Request request) {
		return request.parameter("graph", RDF.NIL.stringValue());
	}


	//// Catalog ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETCatalogPrivateAnonymous() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(anonymous(catalog(request())))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					return this;

				})
		);
	}

	@Test void testGETCatalogPrivateAuthorized() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(authenticated(catalog(request())))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}

	@Test void testGETCatalogPublicAnonymous() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(anonymous(catalog(request())))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}


	@Test void testGETCatalogPublicAuthorized() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(authenticated(catalog(request())))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}


	//// GET ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETDefaultPrivateAnonymous() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.map(response ->

						assertThat(response)
								.hasStatus(Unauthorized)
								.doesNotHaveBody()

				));
	}

	@Test void testGETDefaultPrivateAuthenticated() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)
				)
		);
	}

	@Test void testGETDefaultPublicAnonymous() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)));
	}

	@Test void testGETDefaultPublicAuthenticated() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)
				)
		);
	}


	@Test void testGETNamedPrivateAnonymous() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(anonymous(named(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(Unauthorized)
						.doesNotHaveBody()));
	}

	@Test void testGETNamedPrivateAuthenticated() {
		exec(dflt(First), named(Rest), () -> _private(endpoint())

				.handle(authenticated(named(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}

	@Test void testGETNamedPublicAnonymous() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(anonymous(named(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}

	@Test void testGETNamedPublicAuthenticated() {
		exec(dflt(First), named(Rest), () -> _public(endpoint())

				.handle(authenticated(named(get(request()))))

				.map(response -> assertThat(response)
						.hasStatus(com.metreeca.rest.Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}


	//// PUT ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPUTDefaultPrivateAnonymous() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPUTDefaultPrivateAuthenticated() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(Rest);

					return this;

				}));
	}

	@Test void testPUTDefaultPublicAnonymous() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPUTDefaultPublicAuthenticated() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(Rest);

					return this;

				}));
	}


	@Test void testPUTNamedPrivateAnonymous() {
		exec(named(First), () -> _private(endpoint())

				.handle(anonymous(named(put(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPUTNamedPrivateAuthenticated() {
		exec(named(First), () -> _private(endpoint())

				.handle(authenticated(named(put(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(Rest);

					return this;

				}));
	}

	@Test void testPUTNamedPublicAnonymous() {
		exec(named(First), () -> _public(endpoint())

				.handle(anonymous(named(put(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPUTNamedPublicAuthenticated() {
		exec(named(First), () -> _public(endpoint())

				.handle(authenticated(named(put(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(Rest);

					return this;

				}));
	}


	//// DELETE ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDELETEDefaultPrivateAnonymous() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testDELETEDefaultPrivateAuthenticated() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isEmpty();

					return this;

				}));
	}

	@Test void testDELETEDefaultPublicAnonymous() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testDELETEDefaultPublicAuthenticated() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isEmpty();

					return this;

				}));
	}


	@Test void testDELETENamedPrivateAnonymous() {
		exec(named(First), () -> _private(endpoint())

				.handle(anonymous(named(delete(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testDELETENamedPrivateAuthenticated() {
		exec(named(First), () -> _private(endpoint())

				.handle(authenticated(named(delete(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isEmpty();

					return this;

				}));
	}

	@Test void testDELETENamedPublicAnonymous() {
		exec(named(First), () -> _public(endpoint())

				.handle(anonymous(named(delete(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testDELETENamedPublicAuthenticated() {
		exec(named(First), () -> _public(endpoint())

				.handle(authenticated(named(delete(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isEmpty();

					return this;

				}));
	}


	//// POST ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPOSTDefaultPrivateAnonymous() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPOSTDefaultPrivateAuthenticated() {
		exec(dflt(First), () -> _private(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(model(First, Rest));

					return this;

				}));
	}

	@Test void testPOSTDefaultPublicAnonymous() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPOSTDefaultPublicAuthenticated() {
		exec(dflt(First), () -> _public(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(model(First, Rest));

					return this;

				}));
	}


	@Test void testPOSTNamedPrivateAnonymous() {
		exec(named(First), () -> _private(endpoint())

				.handle(anonymous(named(post(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPOSTNamedPrivateAuthenticated() {
		exec(named(First), () -> _private(endpoint())

				.handle(authenticated(named(post(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(model(First, Rest));

					return this;

				}));
	}

	@Test void testPOSTNamedPublicAnonymous() {
		exec(named(First), () -> _public(endpoint())

				.handle(anonymous(named(post(request()))))

				.map(response -> {

					assertThat(response)
							.hasStatus(Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

					return this;

				}));
	}

	@Test void testPOSTNamedPublicAuthenticated() {
		exec(named(First), () -> _public(endpoint())

				.handle(authenticated(named(post(request()))))

				.map(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(model(First, Rest));

					return this;

				}));
	}

}
