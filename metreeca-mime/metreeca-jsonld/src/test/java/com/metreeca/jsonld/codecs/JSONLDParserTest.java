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

package com.metreeca.jsonld.codecs;

import com.metreeca.link.*;
import com.metreeca.link.queries.*;
import com.metreeca.link.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.json.JsonException;

import static org.assertj.core.api.Assertions.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

final class JSONLDParserTest {

	private static final IRI x=Values.item("x");

	private static final Value One=Values.literal(1);
	private static final Value Ten=Values.literal(10);

	private static final Shape first=Field.field(RDF.FIRST);
	private static final Shape rest=Field.field(RDF.REST);

	private static final Shape shape=Field.field(RDF.FIRST, rest);


	private void items(final String query, final Shape shape, final Consumer<Items> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Items items) {

				tester.accept(items);

				return true;
			}

		});
	}

	private void terms(final String query, final Shape shape, final Consumer<Terms> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Terms terms) {

				tester.accept(terms);

				return true;
			}

		});
	}

	private void stats(final String query, final Shape shape, final Consumer<Stats> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Stats stats) {

				tester.accept(stats);

				return true;
			}

		});
	}


	private void query(final String query, final Shape shape, final Query.Probe<Boolean> probe) {
		assertThat(parse(query, shape).map(probe))
				.as("query processed")
				.isTrue();
	}

	private Query parse(final String query, final Shape shape) {
		return new JSONLDParser(Values.item(""), shape, emptyMap()).parse(query
				.replace('\'', '"')
				.replace("\\\"", "'")
		);
	}


	private Shape filtered(final Shape shape, final Shape filter) {
		return And.and(shape, Guard.filter(filter));
	}


	@Nested final class Encodings {

		@Test void testParseEmptyString() {
			items("", shape, items -> {

				assertThat(shape).as("base shape").isEqualTo(items.shape());
				assertThat(items.orders()).as("no orders").isEmpty();
				assertThat(items.offset()).as("no offset").isEqualTo(0);
				assertThat(items.limit()).as("no limit").isEqualTo(0);

			});
		}

		@Test void testParseEmptyObject() {
			items("{}", shape, items -> {

				assertThat(items.shape()).as("base shape").isEqualTo(shape);
				assertThat(items.orders()).as("no orders").isEmpty();
				assertThat(items.offset()).as("no offset").isEqualTo(0);
				assertThat(items.limit()).as("no limit").isEqualTo(0);

			});
		}

		@Test void testParseFormBasedQueries() {

			items("~first=keyword", shape, items -> {

				assertThat(items.shape()).isEqualTo(filtered(shape, Field.field(RDF.FIRST, Like.like(
						"keyword", true))));

			});

			items(".order=%2Bfirst.rest&.offset=1&.limit=2", shape, items -> {

				assertThat(items.orders()).containsExactly(Order.increasing(RDF.FIRST, RDF.REST));
				assertThat(items.offset()).isEqualTo(1L);
				assertThat(items.limit()).isEqualTo(2L);

			});

			terms(".terms=first.rest", shape, terms -> {

				assertThat(filtered(shape, And.and())).isEqualTo(terms.shape());
				assertThat(terms.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

			stats(".stats=first.rest", shape, stats -> {

				assertThat(filtered(shape, And.and())).isEqualTo(stats.shape());
				assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

		}

	}

	@Nested final class Paths {

		@Test void testParseEmptyPath() {
			stats("{ '.stats': '' }", shape, stats -> assertThat(stats.path())
					.isEmpty()
			);
		}

		@Test void testParseDirectSteps() {
			stats("{ '.stats': 'first' }", first, stats -> assertThat(stats.path())
					.containsExactly(RDF.FIRST)
			);
		}

		@Test void testParseInverseSteps() { // !!! inverse?
			stats("{ '.stats': 'firstOf' }", Field.field(Values.inverse(RDF.FIRST)),
					stats -> assertThat(stats.path())
							.containsExactly(Values.inverse(RDF.FIRST))
			);
		}

		@Test void testParseMultipleSteps() {

			stats("{ '.stats': 'first.rest' }", Field.field(RDF.FIRST, rest),
					stats -> assertThat(stats.path())
							.containsExactly(RDF.FIRST, RDF.REST)
			);

			stats("{ '.stats': 'firstOf.rest' }", Field.field(Values.inverse(RDF.FIRST), rest),
					stats -> assertThat(stats.path())
							.containsExactly(Values.inverse(RDF.FIRST), RDF.REST)
			);

		}

		@Test void testParseSortingCriteria() {

			items("{ '.order': '' }", shape, items -> assertThat(items.orders())
					.as("empty path")
					.containsExactly(Order.increasing())
			);

			items("{ '.order': '+' }", shape, items -> assertThat(items.orders())
					.as("empty path increasing")
					.containsExactly(Order.increasing())
			);

			items("{ '.order': '-' }", shape, items -> assertThat(items.orders())
					.as("empty path decreasing")
					.containsExactly(Order.decreasing())
			);

			items("{ '.order': 'first.rest' }", shape, items -> assertThat(items.orders())
					.containsExactly(Order.increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '.order': '+first.rest' }", shape, items -> assertThat(items.orders())
					.as("path increasing")
					.containsExactly(Order.increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '.order': '-first.rest' }", shape, items -> assertThat(items.orders())
					.as("path decreasing")
					.containsExactly(Order.decreasing(RDF.FIRST, RDF.REST)));

			items("{ '.order': [] }", shape, items -> assertThat(items.orders()).
					as("empty list")
					.isEmpty()
			);

			items("{ '.order': ['+first', '-first.rest'] }", shape, items -> assertThat(items.orders())
					.as("list")
					.containsExactly(Order.increasing(RDF.FIRST), Order.decreasing(RDF.FIRST, RDF.REST))
			);

		}


		@Test void testTraverseLinkPaths() {

			terms("{ '.terms' : 'first.rest' }", Link.link(OWL.SAMEAS, Field.field(RDF.FIRST, Field.field(RDF.REST))),
					terms -> assertThat(terms.path()).containsExactly(

							RDF.FIRST, RDF.REST

					));

			terms("{ '.terms' : 'first.rest' }", Field.field(RDF.FIRST, Link.link(OWL.SAMEAS, Field.field(RDF.REST))),
					terms -> assertThat(terms.path()).containsExactly(

							RDF.FIRST, RDF.REST

					));

			terms("{ '.terms' : 'first.rest' }", Field.field(RDF.FIRST, Field.field(RDF.REST, Link.link(OWL.SAMEAS))),
					terms -> assertThat(terms.path()).containsExactly(

							RDF.FIRST, RDF.REST

					));

		}

		@Test void testTraverseLinkFilters() {

			items("{ 'first.rest': 'any' }", Link.link(OWL.SAMEAS, Field.field(RDF.FIRST, Field.field(RDF.REST))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

							Link.link(OWL.SAMEAS, Field.field(RDF.FIRST, Field.field(RDF.REST))),

							Link.link(OWL.SAMEAS, Field.field(RDF.FIRST, Field.field(RDF.REST, Any.any(Values.literal(
									"any")))))

					)));

			items("{ 'first.rest': 'any' }", Field.field(RDF.FIRST, Link.link(OWL.SAMEAS, Field.field(RDF.REST))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

							Field.field(RDF.FIRST, Link.link(OWL.SAMEAS, Field.field(RDF.REST))),

							Field.field(RDF.FIRST, Link.link(OWL.SAMEAS, Field.field(RDF.REST, Any.any(Values.literal(
									"any")))))

					)));

			// link implies Resource object
			items("{ 'first.rest': 'any' }", Field.field(RDF.FIRST, Field.field(RDF.REST, Link.link(OWL.SAMEAS))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

							Field.field(RDF.FIRST, Field.field(RDF.REST, Link.link(OWL.SAMEAS))),

							Field.field(RDF.FIRST, Field.field(RDF.REST, Link.link(OWL.SAMEAS, Any.any(Values.item("any"
							)))))

					)));

		}


		@Test void testReportMalformedPaths() {
			assertThatThrownBy(() -> parse("{ '.order': '---' }", And.and())).isInstanceOf(JsonException.class);
		}

		@Test void testReportReservedPaths() {
			assertThatThrownBy(() -> parse("{ '.order': '.reserved' }", And.and())).isInstanceOf(JsonException.class);
		}

		@Test void testReportReferencesOutsideShapeEnvelope() {

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					terms("{ '.terms': 'nil' }", shape, items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					stats("{ '.stats': 'nil' }", shape, stats -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': 'nil' }", shape, items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '>= nil': 1 }", shape, items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ 'first.nil': 1 }", shape, items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': '-nil' }", shape, items -> { })
			);

		}

		@Test void testReportReferencesForEmptyShape() {

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					terms("{ '.terms': 'first' }", And.and(), items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					stats("{ '.stats': 'first' }", And.and(), stats -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': 'nil' }", And.and(), items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '>= nil': 1 }", And.and(), items -> { })
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ 'first.nil': 1 }", And.and(), items -> { })
			);

		}

	}

	@Nested final class Filters {

		@Test void testParseRootFilters() {

			final Value first=Values.item("first");
			final Value rest=Values.item("rest");


			items("{ '>': 1 }", shape, items -> assertThat(items.shape())
					.as("min exclusive")
					.isEqualTo(filtered(shape, MinExclusive.minExclusive(One)))
			);

			items("{ '<': 1 }", shape, items -> assertThat(items.shape())
					.as("max exclusive")
					.isEqualTo(filtered(shape, MaxExclusive.maxExclusive(One)))
			);

			items("{ '>=': 1 }", shape, items -> assertThat(items.shape())
					.as("min inclusive")
					.isEqualTo(filtered(shape, MinInclusive.minInclusive(One)))
			);

			items("{ '<=': 1 }", shape, items -> assertThat(items.shape())
					.as("max inclusive")
					.isEqualTo(filtered(shape, MaxInclusive.maxInclusive(One)))
			);


			items("{ '~': 'words' }", shape, items -> assertThat(items.shape())
					.as("like")
					.isEqualTo(filtered(shape, Like.like("words", true)))
			);

			items("{ '^': 'stem' }", shape, items -> assertThat(items.shape())
					.as("like")
					.isEqualTo(filtered(shape, Stem.stem("stem")))
			);


			items("{ '!': [] }", shape, items -> assertThat(items.shape())
					.as("universal (empty)")
					.isEqualTo(filtered(shape, All.all()))
			);

			items("{ '!': 'first' }", shape, items -> assertThat(items.shape())
					.as("universal (singleton)")
					.isEqualTo(filtered(shape, All.all(first)))
			);

			items("{ '!': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("universal (multiple)")
					.isEqualTo(filtered(shape, All.all(first, rest)))
			);


			items("{ '?': [] }", shape, items -> assertThat(items.shape())
					.as("existential (empty)")
					.isEqualTo(filtered(shape, Any.any()))
			);

			items("{ '?': 'first' }", shape, items -> assertThat(items.shape())
					.as("existential (singleton)")
					.isEqualTo(filtered(shape, Any.any(first)))
			);

			items("{ '?': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("existential (multiple)")
					.isEqualTo(filtered(shape, Any.any(first, rest)))
			);

		}

		@Test void testParsePathFilters() {

			items("{ '>= first.rest': 1 }", shape, items -> {
				assertThat(items.shape())
								.as("nested filter")
								.isEqualTo(filtered(shape,
										Field.field(RDF.FIRST, Field.field(RDF.REST, MinInclusive.minInclusive(One)))));
					}
			);

			items("{ 'first.rest': 1 }", shape, items -> {
				assertThat(items.shape())
								.as("nested filter singleton shorthand")
								.isEqualTo(filtered(shape, Field.field(RDF.FIRST,
                                        Field.field(RDF.REST, Any.any(One)))));
					}
			);

			items("{ 'first.rest': [1, 10] }", shape, items -> {
				assertThat(items.shape())
								.as("nested filter multiple shorthand")
								.isEqualTo(filtered(shape, Field.field(RDF.FIRST, Field.field(RDF.REST, Any.any(One,
                                        Ten)))));
					}
			);

		}

		@Test void testParseShapedFilters() {

			final Shape shape=Field.field(RDF.VALUE, Datatype.datatype(XSD.LONG));

			items("{ 'value': '4' }", shape, items -> assertThat(items.shape())
					.isEqualTo(filtered(shape, Field.field(RDF.VALUE, Any.any(Values.literal("4", XSD.LONG)))))
			);
		}

		@Test void testIgnoreEmptyFilters() {

			items("{ 'first': null }", shape, items -> assertThat(items.shape()).isEqualTo(shape));
			items("{ 'first': '' }", shape, items -> assertThat(items.shape()).isEqualTo(shape));
			items("{ 'first': [] }", shape, items -> assertThat(items.shape()).isEqualTo(shape));

		}

		@Test void testParseSliceLeniently() {

			items("{ '.offset': '1', '.limit': '2' }", shape, items -> assertThat(items)
					.isEqualTo(Items.items(shape, emptyList(), 1, 2))
			);

		}

	}

	@Nested final class Queries {

		@Test void testParsePlainQuery() {

			items("first=x&first.rest=y&first.rest=w+z", shape, items -> assertThat(items.shape())
					.isEqualTo(filtered(shape, Field.field(RDF.FIRST,
							Any.any(x),
							Field.field(RDF.REST, Any.any(Values.literal("y"), Values.literal("w z")))
					))));

			items("first=x&first.rest=y&.order=-first.rest&.order=first&.offset=1&.limit=2", shape, items -> {

				assertThat(items.orders())
						.containsExactly(Order.decreasing(RDF.FIRST, RDF.REST), Order.increasing(RDF.FIRST));

				assertThat(items.offset())
						.isEqualTo(1);

				assertThat(items.limit())
						.isEqualTo(2);

				assertThat(items.shape())
						.isEqualTo(filtered(shape, Field.field(RDF.FIRST, And.and(
								Any.any(x),
								Field.field(RDF.REST, Any.any(Values.literal("y")))
						))));
			});

		}

		@Test void testParseItemsQuery() {

			items("{ '.offset': 1, '.limit': 2 }", shape, items -> {

				assertThat(items.shape()).as("shape").isEqualTo(filtered(shape, And.and()));
				assertThat(items.offset()).as("offset").isEqualTo(1);
				assertThat(items.limit()).as("limit").isEqualTo(2);

			});

		}

		@Test void testParseTermsQuery() {

			terms("{ '.terms': 'first.rest', '.offset': 1, '.limit': 2 }", shape, terms -> {

				assertThat(filtered(shape, And.and()))
						.as("shape")
						.isEqualTo(terms.shape());

				assertThat(terms.path())
						.containsExactly(RDF.FIRST, RDF.REST);

				assertThat(terms.offset())
						.as("offset")
						.isEqualTo(1);

				assertThat(terms.limit())
						.as("limit")
						.isEqualTo(2);

			});

		}

		@Test void testParseStatsQuery() {

			stats("{ '.stats': 'first.rest', '.offset': 1, '.limit': 2 }", shape, stats -> {

				assertThat(filtered(shape, And.and())).isEqualTo(stats.shape());

				assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST);

				assertThat(stats.offset()).isEqualTo(1);
				assertThat(stats.limit()).isEqualTo(2);

			});

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class TestQueryProbe extends Query.Probe<Boolean> {

		@Override public Boolean probe(final Items items) { return false; }

		@Override public Boolean probe(final Terms terms) { return false; }

		@Override public Boolean probe(final Stats stats) { return false; }

	}

}
