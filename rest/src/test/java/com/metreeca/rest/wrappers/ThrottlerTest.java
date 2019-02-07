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

package com.metreeca.rest.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.Shape.relate;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.And.pass;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.container;
import static com.metreeca.rest.wrappers.Throttler.entity;
import static com.metreeca.rest.wrappers.Throttler.resource;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toSet;


final class ThrottlerTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(task)
				.clear();
	}


	private Request request() {
		return new Request().base(ValuesTest.Base);
	}


	@Nested final class Simple {

		private Throttler throttler() {
			return new Throttler(Form.any, Form.any);
		}

		private Handler handler(final Collection<Statement> model) {
			return request -> request.reply(response -> response.status(Response.OK).body(rdf(), model));
		}


		@Test void testAcceptEmptyRequestPayload() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.OK))
			);
		}

		@Test void testAcceptDescriptionRequestPayload() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request().body(rdf(), decode("<> rdf:value rdf:nil.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}


		@Test void testTrimDisconnectedResponsePayload() {
			exec(() -> throttler()

					.wrap(handler(decode("<> rdf:value rdf:nil. rdf:first rdf:value rdf:rest.")))

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(statement(response.item(), RDF.VALUE, RDF.NIL))
							)
					)
			);

		}

	}

	@Nested final class Shaped {

		private final Shape shape=Employee;

		private final IRI task=Form.relate;
		private final IRI view=Form.detail;
		private final IRI role=ValuesTest.Salesman;


		private Throttler throttler() {
			return new Throttler(task, view);
		}

		private Request request() {
			return ThrottlerTest.this.request()
					.shape(shape)
					.roles(role);
		}

		private Handler handler(final Collection<Statement> model, final Shape shape) {
			return request -> request.reply(response -> response.status(Response.OK).shape(shape).body(rdf(), model));
		}


		@Test void testRedactRequestShape() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request())

					.accept(response -> assertThat(response.request().shape())
							.isEqualTo(shape.map(new Redactor(map(
									entry(Form.task, set(task)),
									entry(Form.view, set(view)),
									entry(Form.role, set(role))
							))).map(new Optimizer()))
					)
			);
		}

		@Test void testExpandRequestModel() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(new Request()
							.shape(field(RDFS.LABEL, literal("request")))
					)

					.accept(response -> assertThat(response)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(statement(response.item(), RDFS.LABEL, literal("request")))
							)
					)
			);

		}

		@Test void testExpandResponseModel() {
			exec(() -> throttler()

					.wrap(echo().with(handler -> request -> handler.handle(request).map(response ->
							response.shape(field(RDFS.LABEL, literal("response")))
					)))

					.handle(new Request())

					.accept(response -> assertThat(response)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(statement(response.item(), RDFS.LABEL, literal("response")))
							)
					)
			);

		}


		@Test void testRejectUnauthorizedRequests() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request().roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
			);
		}

		@Test void testRejectUnauthorizedRequestsIgnoringAnnotations() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request().roles(Form.none).shape(and(shape, meta(RDF.VALUE, RDF.NIL))))

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
			);
		}

		@Test void testRejectForbiddenRequests() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request().user(RDF.NIL).roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.Forbidden))
			);
		}


		@Test void testAcceptEmptyRequestPayload() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.OK))
			);
		}

		@Test void testAcceptCompatibleRequestPayload() {
			exec(() -> throttler()

					.wrap(echo())

					.handle(request().body(rdf(), decode("<> :email 'tino.faussone@example.com'.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}


		@Test void testRedactResponseShape() {
			exec(() -> throttler()

					.wrap(handler(decode(""), shape))

					.handle(request())

					.accept(response -> assertThat(response.request().shape())
							.isEqualTo(shape.map(new Redactor(map(
									entry(Form.task, set(task)),
									entry(Form.view, set(view)),
									entry(Form.role, set(role))
							))).map(new Optimizer()))
					)
			);
		}

		@Test void testTrimExceedingResponsePayload() {
			exec(() -> throttler()

					.wrap(handler(decode("<> :email 'tino.faussone@example.com'; :seniority 5 ."), shape))

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.as("extended with implied statements and trimmed")
									.isIsomorphicTo(decode("<> a :Employee; :email 'tino.faussone@example.com'."))
							)
					)
			);

		}

	}

	@Nested final class Splitters {

		private final Shape Container=and(relate().then(
				field(RDF.TYPE, LDP.DIRECT_CONTAINER),
				field(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
				field(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
				),
				field(RDFS.LABEL, Textual),
				field(RDFS.COMMENT, Textual)
		);


		@Nested final class Entity {

			@Test void testForwardAndAnnotateComboShape() {
				assertThat(entity().apply(Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("all fields retained")
								.isEqualTo(fields(Employees.map(new Optimizer())).keySet())
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(entity().apply(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(entity().apply(Employee))
						.as("only resource shape found")
						.isEqualTo(Employee.map(new Optimizer()));
			}

		}

		@Nested final class Resource {

			@Test void testExtractAndAnnotateResourceShapeFromComboShape() {
				assertThat(resource().apply(Employees))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only resource fields retained")
								.isEqualTo(fields(Employee.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(resource().apply(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(resource().apply(Employee))
						.as("only resource shape found")
						.isEqualTo(Employee.map(new Optimizer()));
			}

			@Test void testPreserveExistingAnnotations() {

				final Shape shape=and(meta(LDP.CONTAINER, LDP.BASIC_CONTAINER));

				assertThat(resource().apply(shape)).isEqualTo(meta(LDP.CONTAINER, LDP.BASIC_CONTAINER));

			}

		}

		@Nested final class Container {

			@Test void testExtractAndAnnotateContainerShapeFromComboShape() {
				assertThat(container().apply(Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("only container fields retained")
								.isEqualTo(fields(Employees)
										.keySet().stream()
										.filter(iri -> !iri.equals(LDP.CONTAINS))
										.collect(toSet())
								)
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testIgnoreResourceShape() {
				assertThat(container().apply(Employee))
						.as("no container shape found")
						.isEqualTo(pass());

			}

			@Test void testPreserveExistingAnnotations() {

				final Shape shape=and(meta(LDP.CONTAINER, LDP.BASIC_CONTAINER), field(LDP.CONTAINS, required()));

				assertThat(container().apply(shape)).isEqualTo(meta(LDP.CONTAINER, LDP.BASIC_CONTAINER));

			}

		}

	}

}
