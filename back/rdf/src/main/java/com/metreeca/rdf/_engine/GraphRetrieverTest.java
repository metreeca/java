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

package com.metreeca.rdf._engine;

import com.metreeca.rdf.Values;
import com.metreeca.rdf.services.Graph;
import com.metreeca.tree.Order;
import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.All;
import com.metreeca.tree.shapes.Any;
import com.metreeca.tree.shapes.Clazz;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.metreeca.tree.Order.decreasing;
import static com.metreeca.tree.Order.increasing;
import static com.metreeca.tree.queries.Edges.edges;
import static com.metreeca.tree.queries.Items.items;
import static com.metreeca.tree.queries.Stats.stats;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.Like.like;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.Meta.meta;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.MinExclusive.minExclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.MinLength.minLength;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;
import static com.metreeca.tree.things.Lists.list;
import static com.metreeca.tree.things.Values.*;
import static com.metreeca.tree.things.ValuesTest.decode;
import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.item;
import static com.metreeca.rdf.ValuesTest.term;
import static com.metreeca.rdf.services.GraphTest.tuples;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.tree.Shape.filter;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.stream.Collectors.toList;


final class GraphRetrieverTest extends GraphProcessorTest {

	private static final IRI StardogDefault=iri("tag:stardog:api:context:default");


	private Collection<Statement> query(final Query query) {
		return service(Graph.graph()).exec(connection -> {
			return query

					.map(new GraphRetriever(service(logger()), connection, root))

					.stream()

					// ;(virtuoso) counts reported as xsd:int // !!! review dependency

					.map(statement -> type(statement.getObject()).equals(XMLSchema.INT) ? statement(
							statement.getSubject(),
							statement.getPredicate(),
							literal(integer(statement.getObject()).orElse(BigInteger.ZERO)),
							statement.getContext()
					) : statement)

					.collect(toList());
		});
	}

	private List<Statement> graph(final String sparql) {
		return GraphTest.model(sparql)

				.stream()

				// ;(stardog) statement from default context explicitly tagged // !!! review dependency

				.map(statement -> StardogDefault.equals(statement.getContext()) ? statement(
						statement.getSubject(),
						statement.getPredicate(),
						statement.getObject()
				) : statement)

				.collect(toList());
	}


	//// Queries ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Edges {

		@Test void testEmptyShape() {
			exec(() -> ModelAssert.assertThat(query(

					items(and())

			)).isEmpty());
		}

		@Test void testEmptyResultSet() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(RDF.TYPE, all(RDF.NIL)))

			)).isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> ModelAssert.assertThat(query(

					items(any(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056")))

			)).isIsomorphicTo(graph( // empty template => symmetric+labelled concise bounded description

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t\n"
							+"\t?employee ?d ?r.\n"
							+"\t?r ?i ?employee.\n"
							+"\n"
							+"\t?r rdf:type ?t.\n"
							+"\t?r rdfs:label ?l.\n"
							+"\t?r rdfs:comment ?c.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?employee {\n"
							+"\t\t<employees/1002>\n"
							+"\t\t<employees/1056>\n"
							+"\t}\n"
							+"\n"
							+"\t{ ?employee ?d ?r } union { ?r ?i ?employee }\n"
							+"\n"
							+"\toptional { ?r rdf:type ?t }\n"
							+"\toptional { ?r rdfs:label ?l }\n"
							+"\toptional { ?r rdfs:comment ?c }\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMatching() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(RDF.TYPE, All.all(ValuesTest.term("Employee"))))

			)).isIsomorphicTo(graph(

					"construct { form:root ldp:contains ?employee. ?employee a :Employee }"
							+" where { ?employee a :Employee }"

			)));
		}

		@Test void testSorting() {
			exec(() -> {

				final String query="select ?employee "
						+" where { ?employee a :Employee; rdfs:label ?label; :office ?office }";

				final Shape shape=filter().then(Clazz.clazz(ValuesTest.term("Employee")));

				final Function<Query, List<Value>> actual=edges -> query(edges)
						.stream()
						.filter(Values.pattern(null, LDP.CONTAINS, null))
						.map(Statement::getObject)
						.distinct()
						.collect(toList());

				final Function<String, List<Value>> expected=sparql -> GraphTest.tuples(sparql)
						.stream()
						.map(map -> map.get("employee"))
						.distinct()
						.collect(toList());


				Assertions.assertThat(actual.apply(items(shape)))
						.as("default (on value)")
						.containsExactlyElementsOf(expected.apply(query+" order by ?employee"));

				Assertions.assertThat(actual.apply(items(shape, increasing(RDFS.LABEL))))
						.as("custom increasing")
						.containsExactlyElementsOf(expected.apply(query+" order by ?label"));

				Assertions.assertThat(actual.apply(items(shape, decreasing(RDFS.LABEL))))
						.as("custom decreasing")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?label)"));

				Assertions.assertThat(actual.apply(items(shape, Order.increasing(ValuesTest.term("office")), increasing(RDFS.LABEL))))
						.as("custom combined")
						.containsExactlyElementsOf(expected.apply(query+" order by ?office ?label"));

				Assertions.assertThat(actual.apply(items(shape, decreasing())))
						.as("custom on root")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?employee)"));

			});
		}

	}

	@Nested final class Stats {

		@Test void testEmptyResultSet() {
			exec(() -> ModelAssert.assertThat(query(

					stats(field(RDF.TYPE, all(RDF.NIL)))

			)).isIsomorphicTo(decode(

					"form:root form:count 0 ."

			)));
		}

		@Test void testEmptyProjection() {
			exec(() -> ModelAssert.assertThat(query(

					stats(Clazz.clazz(ValuesTest.term("Employee")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:count ?count; form:min ?min; form:max ?max;\n"
							+"\n"
							+"\t\t\tform:stats form:iri.\n"
							+"\t\t\t\n"
							+"\tform:iri form:count ?count; form:min ?min; form:max ?max.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
							+"\n"
							+"\t\t?p a :Employee\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testRootConstraints() {
			exec(() -> ModelAssert.assertThat(query(

					com.metreeca.tree.queries.Stats.stats(All.all(ValuesTest.item("employees/1370")), ValuesTest.term("account"))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root \n"
							+"\t\tform:count ?count; form:min ?min; form:max ?max.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tselect (count(?account) as ?count) (min(?account) as ?min) (max(?account) as ?max) {\n"
							+"\n"
							+"\t\t<employees/1370> :account ?account\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"}"

			)));
		}

	}

	@Nested final class Items {

		@Test void testEmptyResultSet() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(RDF.TYPE, all(RDF.NIL)))

			)).isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> ModelAssert.assertThat(query(

					com.metreeca.tree.queries.Items.items(Clazz.clazz(ValuesTest.term("Employee")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:items [\n"
							+"\t\tform:value ?employee;\n"
							+"\t\tform:count 1\n"
							+"\t].\n"
							+"\n"
							+"\t?employee rdfs:label ?label.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?employee a :Employee; \n"
							+"\t\trdfs:label ?label.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testRootConstraints() {
			exec(() -> ModelAssert.assertThat(query(

					items(All.all(ValuesTest.item("employees/1370")), list(ValuesTest.term("account")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:items [\n"
							+"\t\tform:value ?account;\n"
							+"\t\tform:count 1\n"
							+"\t].\n"
							+"\n"
							+"\t?account rdfs:label ?label.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t<employees/1370> :account ?account.\n"
							+"\n"
							+"\t?account rdfs:label ?label.\n"
							+"\n"
							+"}"

			)));
		}

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Annotations {

		@Test void testMeta() {
			exec(() -> ModelAssert.assertThat(query(

					items(meta(RDF.VALUE, RDF.NIL))

			)).as("ignore annotations")
					.isEmpty());
		}

		@Test void testGuard() {
			exec(() -> Assertions.assertThatThrownBy(() ->
					query(items(guard(RDF.VALUE, RDF.NIL)))

			).as("reject partially redacted shapes")
					.isInstanceOf(UnsupportedOperationException.class));
		}

	}

	@Nested final class TermConstraints {

		@Test void testDatatype() {
			exec(() -> {

				ModelAssert.assertThat(query(

						items(field(ValuesTest.term("code"), datatype(XMLSchema.INTEGER)))

				)).isEmpty();

				ModelAssert.assertThat(query(

						items(field(ValuesTest.term("code"), datatype(XMLSchema.STRING)))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\tform:root ldp:contains ?item.\n"
								+"\t?item :code ?code.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?item :code ?code filter ( datatype(?code) = xsd:string )\n"
								+"\n"
								+"}"

				));

			});
		}

		@Test void testClazz() {
			exec(() -> ModelAssert.assertThat(query(

					items(and(field(RDF.TYPE), Clazz.clazz(ValuesTest.term("Employee"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee a ?type\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?type { :Employee }\n"
							+"\n"
							+"\t?employee a ?type\n"
							+"\n"
							+"}"

			)));
		}


		@Test void testMinExclusiveConstraint() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("seniority"), minExclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :seniority ?seniority.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :seniority ?seniority filter (?seniority > 3)\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMaxExclusiveConstraint() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("seniority"), maxExclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :seniority ?seniority.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :seniority ?seniority filter (?seniority < 3)\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMinInclusiveConstraint() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("seniority"), minInclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :seniority ?seniority.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :seniority ?seniority filter (?seniority >= 3)\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMaxInclusiveConstraint() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("seniority"), maxInclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :seniority ?seniority.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :seniority ?seniority filter (?seniority <= 3)\n"
							+"\n"
							+"}"

			)));
		}


		@Test void testMinLength() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("forename"), minLength(5)))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :forename ?forename.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :forename ?forename filter (strlen(str(?forename)) >= 5)\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMaxLength() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("forename"), maxLength(5)))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :forename ?forename.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :forename ?forename filter (strlen(str(?forename)) <= 5)\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testPattern() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(RDFS.LABEL, pattern("\\bgerard\\b", "i")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item rdfs:label ?label.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item rdfs:label ?label filter regex(?label, '\\\\bgerard\\\\b', 'i')\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testLike() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(RDFS.LABEL, like("ger bo")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item rdfs:label ?label.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item rdfs:label ?label, 'Gerard Bondur'^^xsd:string\n"
							+"\n"
							+"}"

			)));
		}

	}

	@Nested final class SetConstraints {

		@Test void testMinCount() {
			exec(() -> Assertions.assertThatThrownBy(() -> query(

					items(field(ValuesTest.term("employee"), minCount(3)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}

		@Test void testMaxCount() {
			exec(() -> Assertions.assertThatThrownBy(() -> query(

					items(field(ValuesTest.term("employee"), maxCount(3)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}


		@Test void testIn() {
			exec(() -> Assertions.assertThatThrownBy(() -> query(

					items(field(ValuesTest.term("office"), in(ValuesTest.item("employees/1621"), ValuesTest.item("employees/1625"))))

			)).isInstanceOf(UnsupportedOperationException.class));
		}


		@Test void testAllDirect() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("employee"),
							all(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056"))
					))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item :employee ?employee, <employees/1002>, <employees/1056>.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAllInverse() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(inverse(ValuesTest.term("office")),
							all(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056"))
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?employee :office ?office.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office ^:office ?employee, <employees/1002>, <employees/1056>.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAllRoot() {
			exec(() -> ModelAssert.assertThat(query(

					items(and(
							all(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056")),
							field(RDF.TYPE)
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee a ?type\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?employee { <employees/1002> <employees/1056> }\n"
							+"\n"
							+"\t?employee a ?type\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAllSingleton() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("employee"), All.all(ValuesTest.item("employees/1002"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, <employees/1002>\n"
							+"\n"
							+"}"

			)));
		}


		@Test void testAny() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("employee"),
							any(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056")))
					)

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, ?value filter (?value in (<employees/1002>, <employees/1056>))\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAnySingleton() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("employee"),
							Any.any(ValuesTest.item("employees/1002")))
					)

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, <employees/1002>\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAnyRoot() {
			exec(() -> ModelAssert.assertThat(query(

					items(and(
							any(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1056")),
							field(RDFS.LABEL)
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee rdfs:label ?label.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?employee {\n"
							+"\t\t<employees/1002>\n"
							+"\t\t<employees/1056>\n"
							+"\t}\n"
							+"\n"
							+"\t?employee rdfs:label ?label\n"
							+"\n"
							+"}"

			)));
		}

	}

	@Nested final class StructuralConstraints {

		@Test void testField() {
			exec(() -> ModelAssert.assertThat(query(

					items(field(ValuesTest.term("country")))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :country ?country.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item :country ?country.\n"
							+"\n"
							+"}"

			)));
		}

	}

	@Nested final class LogicalConstraints {

		@Test void testAnd() {
			exec(() -> ModelAssert.assertThat(query(

					items(and(
							field(ValuesTest.term("country")),
							field(ValuesTest.term("city"))
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :country ?country; :city ?city.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item :country ?country; :city ?city.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testOr() {
			exec(() -> ModelAssert.assertThat(query(

					items(and(
							field(RDF.TYPE),
							or(
									Clazz.clazz(ValuesTest.term("Office")),
									Clazz.clazz(ValuesTest.term("Employee"))
							)))


			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item a ?type.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?type {\n"
							+"\t\t:Office\n"
							+"\t\t:Employee\n"
							+"\t}\n"
							+"\n"
							+"\t?item a ?type.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testWhen() {
			exec(() -> Assertions.assertThatThrownBy(() -> query(

					items(when(guard(RDF.VALUE, RDF.NIL), clazz(RDFS.LITERAL)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> ModelAssert.assertThat(query(

				items(and(
						field(ValuesTest.term("employee")),
						filter().then(field(ValuesTest.term("employee"), any(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1188"))))
				))

		)).isIsomorphicTo(graph(

				"construct {\n"
						+"\n"
						+"\tform:root ldp:contains ?office.\n"
						+"\t?office :employee ?employee\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}"

		)));
	}

}
