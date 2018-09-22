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

public class _CreatorTest {

	//private Request.Writer std(final Request.Writer request) {
	//	return request
	//			.method(Request.POST)
	//			.path("/employees/");
	//}
	//
	//private Request.Writer create(final Request.Writer request) {
	//	return std(request)
	//			.user(RDF.NIL)
	//			.roles(Manager)
	//			.text("@prefix : <http://example.com/terms#> . <>"
	//					+" :forename 'Tino' ;"
	//					+" :surname 'Faussone' ;"
	//					+" :email 'tfaussone@classicmodelcars.com' ;"
	//					+" :title 'Sales Rep' ;"
	//					+" :seniority 1 .");
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testCreate() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(this::create)
	//
	//			.response(response -> {
	//
	//				final String location=response.header("Location").orElse("");
	//
	//				assertEquals("creation reported", Response.Created, response.status());
	//
	//				assertTrue("location header set", location.startsWith(response.request().focus().stringValue()));
	//				assertTrue("response empty", response.text().isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final IRI iri=iri(location);
	//
	//					assertTrue("graph updated", export(connection).containsAll(asList(
	//							statement(iri, RDF.TYPE, term("Employee")),
	//							statement(iri, term("forename"), literal("Tino")),
	//							statement(iri, term("surname"), literal("Faussone"))
	//					)));
	//
	//				}
	//
	//			});
	//}
	//
	//@Test public void testCreateSlug() {
	//	testbed().handler(() -> creator(Employee).slug((request, model) -> "slug"))
	//
	//			.request(this::create)
	//
	//			.response(response -> {
	//
	//				final String location=response.header("Location").orElse("");
	//
	//				assertEquals("creation reported", Response.Created, response.status());
	//
	//				assertEquals("location header set", location, item("employees/slug").stringValue());
	//				assertTrue("response empty", response.text().isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final IRI iri=iri(location);
	//
	//					assertTrue("graph updated", export(connection).containsAll(asList(
	//							statement(iri, RDF.TYPE, term("Employee")),
	//							statement(iri, term("forename"), literal("Tino")),
	//							statement(iri, term("surname"), literal("Faussone"))
	//					)));
	//
	//				}
	//
	//			});
	//}
	//
	//@Test public void testCreatePipe() {
	//
	//	final IRI forename=term("forename");
	//	final IRI surname=term("surname");
	//
	//	final Function<IRI, BiFunction<Request, Model, Model>> upper=predicate -> (request, model) -> {
	//
	//		final IRI focus=request.focus();
	//		final String value=model
	//				.filter(focus, predicate, null)
	//				.objects()
	//				.stream()
	//				.findFirst()
	//				.map(Value::stringValue)
	//				.orElse("");
	//
	//		model.remove(focus, predicate, null);
	//		model.add(focus, predicate, literal(value.toUpperCase(Locale.ROOT)));
	//
	//		return model;
	//
	//	};
	//
	//	testbed()
	//
	//			.handler(() -> creator(Employee)
	//
	//					.pipe(upper.apply(forename))
	//					.pipe(upper.apply(surname)) // test pipe chaining
	//
	//			)
	//
	//			.request(this::create)
	//
	//			.response(response -> {
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					assertTrue("first pipe applied", connection.hasStatement(
	//							response.focus(), forename, literal("TINO"), true
	//					));
	//
	//					assertTrue("second pipe applied", connection.hasStatement(
	//							response.focus(), surname, literal("FAUSSONE"), true
	//					));
	//
	//				}
	//
	//			});
	//
	//}
	//
	//
	//@Test public void testPostProcess() {
	//	testbed().handler(() -> server(creator(Employee)
	//
	//			.slug(auto(Employee))
	//
	//			.wrap(processor().script(sparql("insert { ?this :code ?name } where {}")))
	//
	//			.wrap(processor().script(sparql("insert { ?this rdfs:label ?label } where {\n"
	//					+"\t?this :forename ?forename; :surname ?surname; :code ?code. \n"
	//					+"\tbind (concat(?forename, ' ', ?surname, ' (#', ?code, ')') as ?label)\n"
	//					+"}")))))
	//
	//			.request(this::create)
	//
	//			.response(response -> {
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					connection.exportStatements(response.focus(), null, null, true, new TurtleWriter(System.out));
	//
	//					assertTrue("graph post-processed", connection.hasStatement(
	//							response.focus(), term("code"), null, true
	//					));
	//
	//					assertTrue("processors executed in insertion order", connection.hasStatement(
	//							response.focus(), RDFS.LABEL, literal("Tino Faussone (#1)"), true
	//					));
	//
	//				}
	//			});
	//}
	//
	//@Test public void testPostProcessInsideTXN() {
	//	testbed().handler(() -> server(creator(Employee)
	//
	//			.wrap(handler -> (request, response) -> {
	//				throw new RuntimeException("abort");  // should cause txn rollback
	//			})))
	//
	//			.request(this::create)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.InternalServerError, response.status());
	//
	//				assertTrue("error detailed", response.json() instanceof Map);
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", connection.isEmpty());
	//				}
	//
	//			});
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testUnauthorized() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(request -> std(request)
	//					.user(Form.none)
	//					.done())
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Unauthorized, response.status());
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", export(connection).isEmpty());
	//				}
	//
	//			});
	//}
	//
	//@Test public void testForbidden() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(RDF.FIRST, RDF.REST)
	//					.done()
	//			)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Forbidden, response.status());
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", export(connection).isEmpty());
	//				}
	//
	//			});
	//}
	//
	//
	//@Test public void testIllegalData() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(Manager)
	//					.text("{")
	//			)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.BadRequest, response.status());
	//
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", export(connection).isEmpty());
	//				}
	//
	//			});
	//}
	//
	//@Test public void testInvalidData() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(Manager)
	//					.json(object(
	//							field("forename", "Tino"),
	//							field("surname", "Faussone")
	//					))
	//			)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.UnprocessableEntity, response.status());
	//
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", export(connection).isEmpty());
	//				}
	//
	//			});
	//}
	//
	//@Test public void testRestrictedData() {
	//	testbed().handler(() -> creator(Employee))
	//
	//			.request(request -> std(request)
	//					.roles(LinkTest.Salesman)
	//					.json(object(
	//							field("forename", "Tino"),
	//							field("surname", "Faussone"),
	//							field("email", "tfaussone@classicmodelcars.com"),
	//							field("title", "Sales Rep"),
	//							field("seniority", "1")
	//					)))
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.BadRequest, response.status()); // !!! vs Forbidden
	//
	//				assertTrue("error detailed", response.json() instanceof Map);
	//				assertTrue("location header not set", response.header("Location").orElse("").isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertTrue("graph unchanged", export(connection).isEmpty());
	//				}
	//
	//			});
	//}
	//
}