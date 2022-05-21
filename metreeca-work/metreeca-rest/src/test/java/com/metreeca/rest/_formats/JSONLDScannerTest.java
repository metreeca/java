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

package com.metreeca.rest._formats;

import com.metreeca.link.Shape;
import com.metreeca.link.Trace;
import com.metreeca.rest._Either;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static com.metreeca.link.ModelAssert.assertThat;
import static com.metreeca.link.Shape.required;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.All.all;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Any.any;
import static com.metreeca.link.shapes.Datatype.datatype;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.guard;
import static com.metreeca.link.shapes.Lang.lang;
import static com.metreeca.link.shapes.Like.like;
import static com.metreeca.link.shapes.Link.link;
import static com.metreeca.link.shapes.Localized.localized;
import static com.metreeca.link.shapes.MaxCount.maxCount;
import static com.metreeca.link.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.link.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.link.shapes.MaxLength.maxLength;
import static com.metreeca.link.shapes.MinCount.minCount;
import static com.metreeca.link.shapes.MinExclusive.minExclusive;
import static com.metreeca.link.shapes.MinInclusive.minInclusive;
import static com.metreeca.link.shapes.MinLength.minLength;
import static com.metreeca.link.shapes.Or.or;
import static com.metreeca.link.shapes.Pattern.pattern;
import static com.metreeca.link.shapes.Range.range;
import static com.metreeca.link.shapes.Stem.stem;
import static com.metreeca.link.shapes.When.when;
import static com.metreeca.rest._EitherAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class JSONLDScannerTest {

	private final IRI s=iri("test:s");

	private final IRI p=iri("test:p");
	private final IRI q=iri("test:q");
	private final IRI r=iri("test:r");

	private final IRI x=iri("test:x");
	private final IRI y=iri("test:y");
	private final IRI z=iri("test:z");


	@Nested final class Validation {

        private _Either<Trace, Collection<Statement>> scan(final Shape shape, final Statement... model) {
            return JSONLDScanner.scan(s, shape, asList(model));
        }


		@Test void testValidateShapeEnvelope() {

			final Shape shape=field(p, all(x));

			assertThat(scan(shape, statement(s, p, x))).hasRight(singletonList(statement(s, p, x)));
			assertThat(scan(shape, statement(s, p, x), statement(s, q, y))).hasRight(singletonList(statement(s, p, x)));

		}


		@Test void testValidateLink() {

			final Shape shape=link(OWL.SAMEAS, field(p, minCount(1)));

			assertThat(scan(shape, statement(s, p, x))).hasRight();
			assertThat(scan(shape, statement(s, q, x))).hasLeft();

		}

		@Test void testValidateField() {

			final Shape shape=field(p, minCount(1));

			assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
			assertThat(scan(shape, statement(s, q, x))).hasLeft();

			assertThat(scan(shape)).hasLeft();

		}

		@Test void testValidateDirectFields() {

			final Shape shape=field(p, all(y));

			assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
			assertThat(scan(shape, statement(s, p, z))).hasLeft();

		}

		@Test void testValidateInverseFields() {

			final Shape shape=field(inverse(p), all(x));

			assertThat(scan(shape, statement(x, p, s))).hasRight();
			assertThat(scan(shape, statement(y, p, s))).hasLeft();

		}

		@Test void testValidateMultipleValues() {

			final Shape shape=field(p, field(q, required()));

			assertThat(scan(shape,

					statement(s, p, x),
					statement(s, p, y),
					statement(x, q, x),
					statement(y, q, y)

			)).hasRight();

		}


		@Test void testValidateAnd() {

			final Shape shape=field(p, and(any(x), any(y)));

			assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
			assertThat(scan(shape, statement(s, p, x), statement(s, p, z))).hasLeft();

			assertThat(scan(shape)).hasLeft();

		}

		@Test void testValidateOr() {

			final Shape shape=field(p, or(all(x, y), all(x, z)));

			assertThat(scan(shape, statement(s, p, x), statement(s, p, y), statement(s, p, z))).hasRight();
			assertThat(scan(shape, statement(s, p, y), statement(s, p, z))).hasLeft();

		}

		@Test void ValidateWhen() {

			final Shape shape=field(p, when(
					datatype(XSD.INTEGER),
					maxInclusive(literal(100)),
					maxInclusive(literal("10"))
			));

			assertThat(scan(shape, statement(s, p, literal(100)))).hasRight();
			assertThat(scan(shape, statement(s, p, literal("100")))).hasLeft();
		}


		@Test void testReportUnredactedGuard() {
			assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					scan(when(guard("axis", "value"), maxInclusive(literal(100))))
			);
		}

	}

	@Nested final class Constraints {

        private _Either<Trace, Collection<Statement>> scan(final Shape shape, final Value... values) {

            return JSONLDScanner.scan(s, field(p, shape), Arrays
                    .stream(values)
                    .map(v -> statement(s, p, v))
                    .collect(toList())
            );
        }


		@Test void testValidateDatatype() {

			final IRI iri=iri("http://example.com/");
			final BNode bnode=bnode();

			final Literal number=literal(1);
			final Literal string=literal("text");

			final Literal typed=literal(LocalDate.parse("2020-09-25"));
			final Literal tagged=literal("text", "en");


			assertThat(scan(datatype(ValueType), iri)).hasRight();
			assertThat(scan(datatype(ValueType), bnode)).hasRight();
			assertThat(scan(datatype(ValueType), number)).hasRight();

			assertThat(scan(datatype(ResourceType), iri)).hasRight();
			assertThat(scan(datatype(ResourceType), bnode)).hasRight();
			assertThat(scan(datatype(ResourceType), number)).hasLeft();

			assertThat(scan(datatype(BNodeType), bnode)).hasRight();
			assertThat(scan(datatype(BNodeType), number)).hasLeft();

			assertThat(scan(datatype(IRIType), iri)).hasRight();
			assertThat(scan(datatype(IRIType), bnode)).hasLeft();

			assertThat(scan(datatype(LiteralType), string)).hasRight();
			assertThat(scan(datatype(LiteralType), number)).hasRight();
			assertThat(scan(datatype(LiteralType), bnode)).hasLeft();

			assertThat(scan(datatype(XSD.STRING), string)).hasRight();
			assertThat(scan(datatype(XSD.STRING), bnode)).hasLeft();

			assertThat(scan(datatype(XSD.DATE), typed)).hasRight();
			assertThat(scan(datatype(XSD.DATE), bnode)).hasLeft();

			assertThat(scan(datatype(RDF.LANGSTRING), tagged)).hasRight();
			assertThat(scan(datatype(RDF.LANGSTRING), iri)).hasLeft();

			assertThat(scan(datatype(XSD.BOOLEAN), True)).hasRight();
			assertThat(scan(datatype(XSD.BOOLEAN), bnode)).hasLeft();

			assertThat(scan(datatype(IRIType))).hasRight();

		}

		@Test void testValidateRange() {

			final Shape shape=range(x, y);

			assertThat(scan(shape, x, y)).hasRight();
			assertThat(scan(shape, x, y, z)).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateGenericLang() {

			final Shape shape=lang();

			assertThat(scan(shape, literal("one", "en"))).hasRight();
			assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).hasRight();

			assertThat(scan(shape, iri("http://example.com/"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateRestrictedLang() {

			final Shape shape=lang("en", "it");

			assertThat(scan(shape, literal("one", "en"))).hasRight();

			assertThat(scan(shape, literal("ein", "de"))).hasLeft();
			assertThat(scan(shape, iri("http://example.com/"))).hasLeft();

			assertThat(scan(shape)).hasRight();
		}


		@Test void testValidateMinExclusive() {

			final Shape shape=minExclusive(literal(1));

			assertThat(scan(shape, literal(2))).hasRight();
			assertThat(scan(shape, literal(1))).hasLeft();
			assertThat(scan(shape, literal(0))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateMaxExclusive() {

			final Shape shape=maxExclusive(literal(10));

			assertThat(scan(shape, literal(2))).hasRight();
			assertThat(scan(shape, literal(10))).hasLeft();
			assertThat(scan(shape, literal(100))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateMinInclusive() {

			final Shape shape=minInclusive(literal(1));

			assertThat(scan(shape, literal(2))).hasRight();
			assertThat(scan(shape, literal(1))).hasRight();
			assertThat(scan(shape, literal(0))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateMaxInclusive() {

			final Shape shape=maxInclusive(literal(10));

			assertThat(scan(shape, literal(2))).hasRight();
			assertThat(scan(shape, literal(10))).hasRight();
			assertThat(scan(shape, literal(100))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}


		@Test void testValidateMinLength() {

			final Shape shape=minLength(3);

			assertThat(scan(shape, literal(100))).hasRight();
			assertThat(scan(shape, literal(99))).hasLeft();

			assertThat(scan(shape, literal("100"))).hasRight();
			assertThat(scan(shape, literal("99"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateMaxLength() {

			final Shape shape=maxLength(2);

			assertThat(scan(shape, literal(99))).hasRight();
			assertThat(scan(shape, literal(100))).hasLeft();

			assertThat(scan(shape, literal("99"))).hasRight();
			assertThat(scan(shape, literal("100"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidatePattern() {

			final Shape shape=pattern(".*\\.org");

			assertThat(scan(shape, iri("http://example.org"))).hasRight();
			assertThat(scan(shape, iri("http://example.com"))).hasLeft();

			assertThat(scan(shape, literal("example.org"))).hasRight();
			assertThat(scan(shape, literal("example.com"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateLike() {

			final Shape shape=like("ex.org", true);

			assertThat(scan(shape, iri("http://exampe.org/"))).hasRight();
			assertThat(scan(shape, iri("http://exampe.com/"))).hasLeft();

			assertThat(scan(shape, literal("example.org"))).hasRight();
			assertThat(scan(shape, literal("example.com"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}

		@Test void testValidateStem() {

			final Shape shape=stem("http://example.com/");

			assertThat(scan(shape, iri("http://example.com/"))).hasRight();
			assertThat(scan(shape, iri("http://example.net/"))).hasLeft();

			assertThat(scan(shape, iri("http://example.com/resource"))).hasRight();
			assertThat(scan(shape, iri("http://example.net/resource"))).hasLeft();

			assertThat(scan(shape, literal("http://example.com/resource"))).hasRight();
			assertThat(scan(shape, literal("http://example.net/resource"))).hasLeft();

			assertThat(scan(shape)).hasRight();

		}


		@Test void testValidateMinCount() {

			final Shape shape=minCount(2);

			assertThat(scan(shape, literal(1), literal(2), literal(3))).hasRight();
			assertThat(scan(shape, literal(1))).hasLeft();

		}

		@Test void testValidateMaxCount() {

			final Shape shape=maxCount(2);

			assertThat(scan(shape, literal(1), literal(2))).hasRight();
			assertThat(scan(shape, literal(1), literal(2), literal(3))).hasLeft();

		}

		@Test void testValidateAll() {

			final Shape shape=all(x, y);

			assertThat(scan(shape, x, y, z)).hasRight();
			assertThat(scan(shape, x)).hasLeft();

			assertThat(scan(shape)).hasLeft();

		}

		@Test void testValidateAny() {

			final Shape shape=any(x, y);

			assertThat(scan(shape, x)).hasRight();
			assertThat(scan(shape, z)).hasLeft();

			assertThat(scan(shape)).hasLeft();

		}

		@Test void testValidateLocalized() {

			final Shape shape=localized();

			assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).hasRight();
			assertThat(scan(shape, literal("one", "en"), literal("two", "en"))).hasLeft();


			assertThat(scan(shape)).hasRight();

		}

	}

	@Nested final class Trimming {

		private Collection<Statement> scan(final Shape shape, final Statement... model) {
            return JSONLDScanner.scan(s, shape, asList(model))
                    .fold(e -> Optional.<Collection<Statement>>empty(), Optional::of)
                    .orElse(emptySet());
        }


		@Test void testPruneField() {
			assertThat(scan(field(p),

					statement(s, p, x),
					statement(s, q, x)

			)).isIsomorphicTo(

					statement(s, p, x)

			);
		}

		@Test void testPruneLanguages() {
			assertThat(scan(field(p, lang("en")),

					statement(s, p, literal("one", "en")),
					statement(s, q, literal("uno", "it"))


			)).isIsomorphicTo(

					statement(s, p, literal("one", "en"))

			);
		}

		@Test void testTraverseAnd() {
			assertThat(scan(and(field(p), field(q)),

					statement(s, p, x),
					statement(s, q, x),
					statement(s, r, x)


			)).isIsomorphicTo(

					statement(s, p, x),
					statement(s, q, x)

			);
		}

		@Test void testTraverseField() {
			assertThat(scan(field(p, field(q)),

					statement(s, p, x),
					statement(s, q, z),

					statement(x, q, y),
					statement(x, p, z)


			)).isIsomorphicTo(

					statement(s, p, x),
					statement(x, q, y)

			);
		}

		@Test void testTraverseOr() {
			assertThat(scan(or(field(p), field(q)),

					statement(s, p, x),
					statement(s, q, y),
					statement(s, r, z)

			)).isIsomorphicTo(

					statement(s, p, x),
					statement(s, q, y)

			);
		}

		@Test void testTraverseWhen() {

			assertThat(scan(when(stem("test:"), field(p), field(q)),

					statement(s, p, x),
					statement(s, q, y),
					statement(s, r, z)

			)).isIsomorphicTo(

					statement(s, p, x)

			);

			assertThat(scan(when(stem("work:"), field(p), field(q)),

					statement(s, p, x),
					statement(s, q, y),
					statement(s, r, z)

			)).isIsomorphicTo(

					statement(s, q, y)

			);

		}

	}

}