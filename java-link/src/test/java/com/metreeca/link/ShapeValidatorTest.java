/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.link;

import com.metreeca.link.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.All.all;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Any.any;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.guard;
import static com.metreeca.link.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.link.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

final class ShapeValidatorTest {

    private final IRI s=iri("test:s");

    private final IRI p=iri("test:p");
    private final IRI q=iri("test:q");
    private final IRI r=iri("test:r");

    private final IRI x=iri("test:x");
    private final IRI y=iri("test:y");
    private final IRI z=iri("test:z");


    @Nested final class Validation {

        private Optional<Trace> scan(final Shape shape, final Statement... model) {
            return ShapeValidator.validate(shape, s, asList(model));
        }


        @Test void testValidateLink() {

            final Shape shape=Link.link(OWL.SAMEAS, field(p, MinCount.minCount(1)));

            assertThat(scan(shape, statement(s, p, x))).isNotPresent();
            assertThat(scan(shape, statement(s, q, x))).isPresent();

        }

        @Test void testValidateField() {

            final Shape shape=field(p, MinCount.minCount(1));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).isNotPresent();
            assertThat(scan(shape, statement(s, q, x))).isPresent();

            assertThat(scan(shape)).isPresent();

        }

        @Test void testValidateDirectFields() {

            final Shape shape=field(p, all(y));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).isNotPresent();
            assertThat(scan(shape, statement(s, p, z))).isPresent();

        }

        @Test void testValidateInverseFields() {

            final Shape shape=field(inverse(p), all(x));

            assertThat(scan(shape, statement(x, p, s))).isNotPresent();
            assertThat(scan(shape, statement(y, p, s))).isPresent();

        }

        @Test void testValidateMultipleValues() {

            final Shape shape=field(p, field(q, Shape.required()));

            assertThat(scan(shape,

                    statement(s, p, x),
                    statement(s, p, y),
                    statement(x, q, x),
                    statement(y, q, y)

            )).isNotPresent();

        }


        @Test void testValidateAnd() {

            final Shape shape=field(p, and(any(x), any(y)));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).isNotPresent();
            assertThat(scan(shape, statement(s, p, x), statement(s, p, z))).isPresent();

            assertThat(scan(shape)).isPresent();

        }

        @Test void testValidateOr() {

            final Shape shape=field(p, Or.or(all(x, y), all(x, z)));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y), statement(s, p, z))).isNotPresent();
            assertThat(scan(shape, statement(s, p, y), statement(s, p, z))).isPresent();

        }

        @Test void ValidateWhen() {

            final Shape shape=field(p, when(
                    Datatype.datatype(XSD.INTEGER),
                    maxInclusive(literal(100)),
                    maxInclusive(literal("10"))
            ));

            assertThat(scan(shape, statement(s, p, literal(100)))).isNotPresent();
            assertThat(scan(shape, statement(s, p, literal("100")))).isPresent();
        }


        @Test void testReportUnredactedGuard() {
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
                    scan(when(guard("axis", "value"), maxInclusive(literal(100))))
            );
        }

    }

    @Nested final class Constraints {

        private Optional<Trace> scan(final Shape shape, final Value... values) {
            return ShapeValidator.validate(field(p, shape), s, Arrays
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


            assertThat(scan(Datatype.datatype(ValueType), iri)).isNotPresent();
            assertThat(scan(Datatype.datatype(ValueType), bnode)).isNotPresent();
            assertThat(scan(Datatype.datatype(ValueType), number)).isNotPresent();

            assertThat(scan(Datatype.datatype(ResourceType), iri)).isNotPresent();
            assertThat(scan(Datatype.datatype(ResourceType), bnode)).isNotPresent();
            assertThat(scan(Datatype.datatype(ResourceType), number)).isPresent();

            assertThat(scan(Datatype.datatype(BNodeType), bnode)).isNotPresent();
            assertThat(scan(Datatype.datatype(BNodeType), number)).isPresent();

            assertThat(scan(Datatype.datatype(IRIType), iri)).isNotPresent();
            assertThat(scan(Datatype.datatype(IRIType), bnode)).isPresent();

            assertThat(scan(Datatype.datatype(LiteralType), string)).isNotPresent();
            assertThat(scan(Datatype.datatype(LiteralType), number)).isNotPresent();
            assertThat(scan(Datatype.datatype(LiteralType), bnode)).isPresent();

            assertThat(scan(Datatype.datatype(XSD.STRING), string)).isNotPresent();
            assertThat(scan(Datatype.datatype(XSD.STRING), bnode)).isPresent();

            assertThat(scan(Datatype.datatype(XSD.DATE), typed)).isNotPresent();
            assertThat(scan(Datatype.datatype(XSD.DATE), bnode)).isPresent();

            assertThat(scan(Datatype.datatype(RDF.LANGSTRING), tagged)).isNotPresent();
            assertThat(scan(Datatype.datatype(RDF.LANGSTRING), iri)).isPresent();

            assertThat(scan(Datatype.datatype(XSD.BOOLEAN), True)).isNotPresent();
            assertThat(scan(Datatype.datatype(XSD.BOOLEAN), bnode)).isPresent();

            assertThat(scan(Datatype.datatype(IRIType))).isNotPresent();

        }

        @Test void testValidateRange() {

            final Shape shape=Range.range(x, y);

            assertThat(scan(shape, x, y)).isNotPresent();
            assertThat(scan(shape, x, y, z)).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateGenericLang() {

            final Shape shape=Lang.lang();

            assertThat(scan(shape, literal("one", "en"))).isNotPresent();
            assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).isNotPresent();

            assertThat(scan(shape, iri("http://example.com/"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateRestrictedLang() {

            final Shape shape=Lang.lang("en", "it");

            assertThat(scan(shape, literal("one", "en"))).isNotPresent();

            assertThat(scan(shape, literal("ein", "de"))).isPresent();
            assertThat(scan(shape, iri("http://example.com/"))).isPresent();

            assertThat(scan(shape)).isNotPresent();
        }


        @Test void testValidateMinExclusive() {

            final Shape shape=MinExclusive.minExclusive(literal(1));

            assertThat(scan(shape, literal(2))).isNotPresent();
            assertThat(scan(shape, literal(1))).isPresent();
            assertThat(scan(shape, literal(0))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateMaxExclusive() {

            final Shape shape=MaxExclusive.maxExclusive(literal(10));

            assertThat(scan(shape, literal(2))).isNotPresent();
            assertThat(scan(shape, literal(10))).isPresent();
            assertThat(scan(shape, literal(100))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateMinInclusive() {

            final Shape shape=MinInclusive.minInclusive(literal(1));

            assertThat(scan(shape, literal(2))).isNotPresent();
            assertThat(scan(shape, literal(1))).isNotPresent();
            assertThat(scan(shape, literal(0))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateMaxInclusive() {

            final Shape shape=maxInclusive(literal(10));

            assertThat(scan(shape, literal(2))).isNotPresent();
            assertThat(scan(shape, literal(10))).isNotPresent();
            assertThat(scan(shape, literal(100))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }


        @Test void testValidateMinLength() {

            final Shape shape=MinLength.minLength(3);

            assertThat(scan(shape, literal(100))).isNotPresent();
            assertThat(scan(shape, literal(99))).isPresent();

            assertThat(scan(shape, literal("100"))).isNotPresent();
            assertThat(scan(shape, literal("99"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateMaxLength() {

            final Shape shape=MaxLength.maxLength(2);

            assertThat(scan(shape, literal(99))).isNotPresent();
            assertThat(scan(shape, literal(100))).isPresent();

            assertThat(scan(shape, literal("99"))).isNotPresent();
            assertThat(scan(shape, literal("100"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidatePattern() {

            final Shape shape=Pattern.pattern(".*\\.org");

            assertThat(scan(shape, iri("http://example.org"))).isNotPresent();
            assertThat(scan(shape, iri("http://example.com"))).isPresent();

            assertThat(scan(shape, literal("example.org"))).isNotPresent();
            assertThat(scan(shape, literal("example.com"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateLike() {

            final Shape shape=Like.like("ex.org", true);

            assertThat(scan(shape, iri("http://exampe.org/"))).isNotPresent();
            assertThat(scan(shape, iri("http://exampe.com/"))).isPresent();

            assertThat(scan(shape, literal("example.org"))).isNotPresent();
            assertThat(scan(shape, literal("example.com"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }

        @Test void testValidateStem() {

            final Shape shape=Stem.stem("http://example.com/");

            assertThat(scan(shape, iri("http://example.com/"))).isNotPresent();
            assertThat(scan(shape, iri("http://example.net/"))).isPresent();

            assertThat(scan(shape, iri("http://example.com/resource"))).isNotPresent();
            assertThat(scan(shape, iri("http://example.net/resource"))).isPresent();

            assertThat(scan(shape, literal("http://example.com/resource"))).isNotPresent();
            assertThat(scan(shape, literal("http://example.net/resource"))).isPresent();

            assertThat(scan(shape)).isNotPresent();

        }


        @Test void testValidateMinCount() {

            final Shape shape=MinCount.minCount(2);

            assertThat(scan(shape, literal(1), literal(2), literal(3))).isNotPresent();
            assertThat(scan(shape, literal(1))).isPresent();

        }

        @Test void testValidateMaxCount() {

            final Shape shape=MaxCount.maxCount(2);

            assertThat(scan(shape, literal(1), literal(2))).isNotPresent();
            assertThat(scan(shape, literal(1), literal(2), literal(3))).isPresent();

        }

        @Test void testValidateAll() {

            final Shape shape=all(x, y);

            assertThat(scan(shape, x, y, z)).isNotPresent();
            assertThat(scan(shape, x)).isPresent();

            assertThat(scan(shape)).isPresent();

        }

        @Test void testValidateAny() {

            final Shape shape=any(x, y);

            assertThat(scan(shape, x)).isNotPresent();
            assertThat(scan(shape, z)).isPresent();

            assertThat(scan(shape)).isPresent();

        }

        @Test void testValidateLocalized() {

            final Shape shape=Localized.localized();

            assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).isNotPresent();
            assertThat(scan(shape, literal("one", "en"), literal("two", "en"))).isPresent();


            assertThat(scan(shape)).isNotPresent();

        }

    }

}