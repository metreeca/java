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


public class _UpdaterTest {

	//private Request.Writer std(final Request.Writer writer) {
	//	return writer
	//			.method(Request.PUT)
	//			.path("/employees/1370"); // Gerard Hernandez
	//}
	//
	//private Request.Writer update(final Request.Writer request) {
	//	return std(request)
	//			.user(RDF.NIL)
	//			.roles(ValuesTest.Salesman)
	//			.text("@prefix : <http://example.com/terms#> . <>"
	//					+":forename 'Tino';"
	//					+":surname 'Faussone';"
	//					+":email 'tfaussone@example.com';"
	//					+":title 'Sales Rep'.");
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testUpdate() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.dataset(small())
	//
	//			.request(this::update)
	//
	//			.response(response -> {
	//
	//				assertEquals("success reported", Response.NoContent, response.status());
	//				assertTrue("no details", response.text().isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final Model actual=construct(connection, "construct where { <employees/1370> ?p ?o }");
	//
	//					assertSubset("items retrieved", response.request().rdf(), actual);
	//					assertEquals("server-managed properties preserved",
	//							singleton(literal("Gerard Hernandez")),
	//							actual.filter(null, RDFS.LABEL, null).objects());
	//
	//				}
	//
	//			});
	//}
	//
	//@Test public void testUpdatePipe() {
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
	//			.handler(() -> updater(Employee)
	//
	//					.pipe(upper.apply(forename))
	//					.pipe(upper.apply(surname)) // test pipe chaining
	//
	//			)
	//
	//			.dataset(small())
	//
	//			.request(this::update)
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
	//	testbed().handler(() -> server(updater(ValuesTest.Employee)
	//
	//			.wrap(new Processor().script(sparql("delete { $this rdfs:label ?_label } \n"
	//					+"insert { $this rdfs:label ?label }\n"
	//					+"where { \n"
	//					+"\n"
	//					+"\t$this rdfs:label ?_label;\n"
	//					+"\t\t:forename ?forename;\n"
	//					+"\t\t:surname ?surname.\n"
	//					+"\n"
	//					+"\tbind (concat(?forename, ' ', ?surname) as ?label)\n"
	//					+"\n"
	//					+"}")))))
	//
	//			.dataset(small())
	//
	//			.request(this::update)
	//
	//			.response(response -> {
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					assertTrue("graph post-processed", connection.hasStatement(
	//							response.focus(), RDFS.LABEL, literal("Tino Faussone"), true
	//					));
	//
	//				}
	//			});
	//}
	//
	//@Test public void testPostProcessInsideTXN() {
	//	testbed().handler(() -> server(updater(Employee)
	//
	//			.wrap(handler -> (request, response) -> {
	//				throw new RuntimeException("abort");  // should cause txn rollback
	//			})))
	//
	//			.dataset(small())
	//
	//			.request(this::update)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.InternalServerError, response.status());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertIsomorphic("graph unchanged", export(connection), small());
	//				}
	//
	//			});
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testUnauthorized() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.request(request -> std(request)
	//					.user(Form.none)
	//					.done())
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Unauthorized, response.status());
	//
	//			});
	//}
	//
	//@Test public void testForbidden() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(RDF.FIRST, RDF.REST)
	//					.done())
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Forbidden, response.status());
	//
	//			});
	//}
	//
	//
	//@Test public void testMalformedData() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.dataset(small())
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(Manager)
	//					.text("<employees/1370>"))
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.BadRequest, response.status());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//			});
	//}
	//
	//@Test public void tesInvalidData() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.dataset(small())
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(Manager)
	//					.text(encode(decode("<employees/1370>" // missing seniority/supervisor/subordinate
	//							+":forename 'Tino';"
	//							+":surname 'Faussone';"
	//							+":email 'tfaussone@example.com';"
	//							+":title 'Sales Rep'."))))
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.UnprocessableEntity, response.status());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//			});
	//}
	//
	//@Test public void testRestrictedData() {
	//	testbed().handler(() -> updater(Employee))
	//
	//			.dataset(small())
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(Salesman)
	//					.text(encode(decode("<employees/1370>"
	//							+":forename 'Tino';"
	//							+":surname 'Faussone';"
	//							+":email 'tfaussone@example.com';"
	//							+":title 'Sales Rep';"
	//							+":seniority 5 ." // outside envelope
	//					))))
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.UnprocessableEntity, response.status()); // vs Forbidden
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//			});
	//}

}