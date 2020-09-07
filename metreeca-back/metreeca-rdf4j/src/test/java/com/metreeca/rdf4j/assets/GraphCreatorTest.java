/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.assets;


import com.metreeca.core.*;
import com.metreeca.json.Shape;
import com.metreeca.json.probes.Redactor;
import com.metreeca.rdf.Values;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.assets.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.core.ResponseAssert.assertThat;
import static com.metreeca.json.Shape.filter;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.ValueAssert.assertThat;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.ValuesTest.*;
import static com.metreeca.rdf.formats.JSONLDFormat.jsonld;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static org.assertj.core.api.Assertions.assertThat;


final class GraphCreatorTest {

	private static final Shape Employee=and(
			filter().then(field(RDF.TYPE, all(term("Employee")))),
			ValuesTest.Employee
					.map(new Redactor(Shape.Role, v -> true))
					.map(new Redactor(Shape.Task, v -> true))
					.map(new Redactor(Shape.Area, Shape.Detail))
	);


	@Nested final class Holder {

		private Request request() {
			return new Request()
					.base(ValuesTest.Base)
					.path("/employees/")
					.header("Slug", "slug")
					.body(jsonld(), decode("</employees/>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :email 'tfaussone@classicmodelcars.com' ;"
							+" :title 'Sales Rep' ;"
							+" :seniority 1 ."
					)).attribute(Engine.shape(), Employee);
		}


		@Test void testCreate() {
			exec(() -> new GraphCreator()

					.handle(request())

					.accept(response -> {

						final IRI location=response.header("Location")
								.map(Values::iri)
								.orElse(null);

						assertThat(response)
								.hasStatus(Response.Created)
								.doesNotHaveBody();

						assertThat(location)
								.as("resource created with supplied slug")
								.isEqualTo(item("employees/slug"));

						assertThat(model())
								.as("resource description stored into the graph")
								.hasSubset(
										statement(location, RDF.TYPE, term("Employee")),
										statement(location, term("forename"), literal("Tino")),
										statement(location, term("surname"), literal("Faussone"))
								);

					}));
		}

		@Test void testConflictingSlug() {
			exec(() -> {

				final GraphCreator creator=new GraphCreator();

				creator.handle(request()).accept(response -> {});

				final Model snapshot=model();

				creator.handle(request()).accept(response -> {

					assertThat(response)
							.hasStatus(Response.InternalServerError);

					assertThat(model())
							.as("graph unchanged")
							.isIsomorphicTo(snapshot);

				});

			});
		}

	}

	@Nested final class Member {

		@Test void testNotImplemented() {
			exec(() -> new GraphCreator()

					.handle(new Request()
							.roles(ValuesTest.Manager)
							.base(ValuesTest.Base)
							.path("/employees/9999").attribute(Engine.shape(), ValuesTest.Employee)
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.InternalServerError);

						assertThat(Context.asset(graph()).exec(RepositoryConnection::isEmpty))
								.as("storage unchanged")
								.isTrue();

					})
			);
		}

	}

}
