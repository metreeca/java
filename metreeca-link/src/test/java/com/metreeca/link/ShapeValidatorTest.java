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

package com.metreeca.link;

import com.metreeca.link.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static com.metreeca.link.EitherAssert.assertThat;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.guard;
import static com.metreeca.link.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.link.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
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

        private Either<Trace, Collection<Statement>> scan(final Shape shape, final Statement... model) {
            return ShapeValidator.validate(s, shape, asList(model));
        }


        @Test void testValidateShapeEnvelope() {

            final Shape shape=field(p, All.all(x));

            assertThat(scan(shape, statement(s, p, x))).hasRight(singletonList(statement(s, p, x)));
            assertThat(scan(shape, statement(s, p, x), statement(s, q, y))).hasRight(singletonList(statement(s, p, x)));

        }


        @Test void testValidateLink() {

            final Shape shape=Link.link(OWL.SAMEAS, field(p, MinCount.minCount(1)));

            assertThat(scan(shape, statement(s, p, x))).hasRight();
            assertThat(scan(shape, statement(s, q, x))).hasLeft();

        }

        @Test void testValidateField() {

            final Shape shape=field(p, MinCount.minCount(1));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
            assertThat(scan(shape, statement(s, q, x))).hasLeft();

            assertThat(scan(shape)).hasLeft();

        }

        @Test void testValidateDirectFields() {

            final Shape shape=field(p, All.all(y));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
            assertThat(scan(shape, statement(s, p, z))).hasLeft();

        }

        @Test void testValidateInverseFields() {

            final Shape shape=field(inverse(p), All.all(x));

            assertThat(scan(shape, statement(x, p, s))).hasRight();
            assertThat(scan(shape, statement(y, p, s))).hasLeft();

        }

        @Test void testValidateMultipleValues() {

            final Shape shape=field(p, field(q, Shape.required()));

            assertThat(scan(shape,

                    statement(s, p, x),
                    statement(s, p, y),
                    statement(x, q, x),
                    statement(y, q, y)

            )).hasRight();

        }


        @Test void testValidateAnd() {

            final Shape shape=field(p, And.and(Any.any(x), Any.any(y)));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y))).hasRight();
            assertThat(scan(shape, statement(s, p, x), statement(s, p, z))).hasLeft();

            assertThat(scan(shape)).hasLeft();

        }

        @Test void testValidateOr() {

            final Shape shape=field(p, Or.or(All.all(x, y), All.all(x, z)));

            assertThat(scan(shape, statement(s, p, x), statement(s, p, y), statement(s, p, z))).hasRight();
            assertThat(scan(shape, statement(s, p, y), statement(s, p, z))).hasLeft();

        }

        @Test void ValidateWhen() {

            final Shape shape=field(p, when(
                    Datatype.datatype(XSD.INTEGER),
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

        private Either<Trace, Collection<Statement>> scan(final Shape shape, final Value... values) {

            return ShapeValidator.validate(s, field(p, shape), Arrays
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


            assertThat(scan(Datatype.datatype(ValueType), iri)).hasRight();
            assertThat(scan(Datatype.datatype(ValueType), bnode)).hasRight();
            assertThat(scan(Datatype.datatype(ValueType), number)).hasRight();

            assertThat(scan(Datatype.datatype(ResourceType), iri)).hasRight();
            assertThat(scan(Datatype.datatype(ResourceType), bnode)).hasRight();
            assertThat(scan(Datatype.datatype(ResourceType), number)).hasLeft();

            assertThat(scan(Datatype.datatype(BNodeType), bnode)).hasRight();
            assertThat(scan(Datatype.datatype(BNodeType), number)).hasLeft();

            assertThat(scan(Datatype.datatype(IRIType), iri)).hasRight();
            assertThat(scan(Datatype.datatype(IRIType), bnode)).hasLeft();

            assertThat(scan(Datatype.datatype(LiteralType), string)).hasRight();
            assertThat(scan(Datatype.datatype(LiteralType), number)).hasRight();
            assertThat(scan(Datatype.datatype(LiteralType), bnode)).hasLeft();

            assertThat(scan(Datatype.datatype(XSD.STRING), string)).hasRight();
            assertThat(scan(Datatype.datatype(XSD.STRING), bnode)).hasLeft();

            assertThat(scan(Datatype.datatype(XSD.DATE), typed)).hasRight();
            assertThat(scan(Datatype.datatype(XSD.DATE), bnode)).hasLeft();

            assertThat(scan(Datatype.datatype(RDF.LANGSTRING), tagged)).hasRight();
            assertThat(scan(Datatype.datatype(RDF.LANGSTRING), iri)).hasLeft();

            assertThat(scan(Datatype.datatype(XSD.BOOLEAN), True)).hasRight();
            assertThat(scan(Datatype.datatype(XSD.BOOLEAN), bnode)).hasLeft();

            assertThat(scan(Datatype.datatype(IRIType))).hasRight();

        }

        @Test void testValidateRange() {

            final Shape shape=Range.range(x, y);

            assertThat(scan(shape, x, y)).hasRight();
            assertThat(scan(shape, x, y, z)).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateGenericLang() {

            final Shape shape=Lang.lang();

            assertThat(scan(shape, literal("one", "en"))).hasRight();
            assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).hasRight();

            assertThat(scan(shape, iri("http://example.com/"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateRestrictedLang() {

            final Shape shape=Lang.lang("en", "it");

            assertThat(scan(shape, literal("one", "en"))).hasRight();

            assertThat(scan(shape, literal("ein", "de"))).hasLeft();
            assertThat(scan(shape, iri("http://example.com/"))).hasLeft();

            assertThat(scan(shape)).hasRight();
        }


        @Test void testValidateMinExclusive() {

            final Shape shape=MinExclusive.minExclusive(literal(1));

            assertThat(scan(shape, literal(2))).hasRight();
            assertThat(scan(shape, literal(1))).hasLeft();
            assertThat(scan(shape, literal(0))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateMaxExclusive() {

            final Shape shape=MaxExclusive.maxExclusive(literal(10));

            assertThat(scan(shape, literal(2))).hasRight();
            assertThat(scan(shape, literal(10))).hasLeft();
            assertThat(scan(shape, literal(100))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateMinInclusive() {

            final Shape shape=MinInclusive.minInclusive(literal(1));

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

            final Shape shape=MinLength.minLength(3);

            assertThat(scan(shape, literal(100))).hasRight();
            assertThat(scan(shape, literal(99))).hasLeft();

            assertThat(scan(shape, literal("100"))).hasRight();
            assertThat(scan(shape, literal("99"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateMaxLength() {

            final Shape shape=MaxLength.maxLength(2);

            assertThat(scan(shape, literal(99))).hasRight();
            assertThat(scan(shape, literal(100))).hasLeft();

            assertThat(scan(shape, literal("99"))).hasRight();
            assertThat(scan(shape, literal("100"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidatePattern() {

            final Shape shape=Pattern.pattern(".*\\.org");

            assertThat(scan(shape, iri("http://example.org"))).hasRight();
            assertThat(scan(shape, iri("http://example.com"))).hasLeft();

            assertThat(scan(shape, literal("example.org"))).hasRight();
            assertThat(scan(shape, literal("example.com"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateLike() {

            final Shape shape=Like.like("ex.org", true);

            assertThat(scan(shape, iri("http://exampe.org/"))).hasRight();
            assertThat(scan(shape, iri("http://exampe.com/"))).hasLeft();

            assertThat(scan(shape, literal("example.org"))).hasRight();
            assertThat(scan(shape, literal("example.com"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }

        @Test void testValidateStem() {

            final Shape shape=Stem.stem("http://example.com/");

            assertThat(scan(shape, iri("http://example.com/"))).hasRight();
            assertThat(scan(shape, iri("http://example.net/"))).hasLeft();

            assertThat(scan(shape, iri("http://example.com/resource"))).hasRight();
            assertThat(scan(shape, iri("http://example.net/resource"))).hasLeft();

            assertThat(scan(shape, literal("http://example.com/resource"))).hasRight();
            assertThat(scan(shape, literal("http://example.net/resource"))).hasLeft();

            assertThat(scan(shape)).hasRight();

        }


        @Test void testValidateMinCount() {

            final Shape shape=MinCount.minCount(2);

            assertThat(scan(shape, literal(1), literal(2), literal(3))).hasRight();
            assertThat(scan(shape, literal(1))).hasLeft();

        }

        @Test void testValidateMaxCount() {

            final Shape shape=MaxCount.maxCount(2);

            assertThat(scan(shape, literal(1), literal(2))).hasRight();
            assertThat(scan(shape, literal(1), literal(2), literal(3))).hasLeft();

        }

        @Test void testValidateAll() {

            final Shape shape=All.all(x, y);

            assertThat(scan(shape, x, y, z)).hasRight();
            assertThat(scan(shape, x)).hasLeft();

            assertThat(scan(shape)).hasLeft();

        }

        @Test void testValidateAny() {

            final Shape shape=Any.any(x, y);

            assertThat(scan(shape, x)).hasRight();
            assertThat(scan(shape, z)).hasLeft();

            assertThat(scan(shape)).hasLeft();

        }

        @Test void testValidateLocalized() {

            final Shape shape=Localized.localized();

            assertThat(scan(shape, literal("one", "en"), literal("uno", "it"))).hasRight();
            assertThat(scan(shape, literal("one", "en"), literal("two", "en"))).hasLeft();


            assertThat(scan(shape)).hasRight();

        }

    }

    @Nested final class Trimming {

        private Collection<Statement> scan(final Shape shape, final Statement... model) {
            return ShapeValidator.validate(s, shape, asList(model))
                    .fold(e -> Optional.<Collection<Statement>>empty(), Optional::of)
                    .orElse(emptySet());
        }


        @Test void testPruneField() {
            ModelAssert.assertThat(scan(field(p),

                    statement(s, p, x),
                    statement(s, q, x)

            )).isIsomorphicTo(

                    statement(s, p, x)

            );
        }

        @Test void testPruneLanguages() {
            ModelAssert.assertThat(scan(field(p, Lang.lang("en")),

                    statement(s, p, literal("one", "en")),
                    statement(s, q, literal("uno", "it"))


            )).isIsomorphicTo(

                    statement(s, p, literal("one", "en"))

            );
        }

        @Test void testTraverseAnd() {
            ModelAssert.assertThat(scan(And.and(field(p), field(q)),

                    statement(s, p, x),
                    statement(s, q, x),
                    statement(s, r, x)


            )).isIsomorphicTo(

                    statement(s, p, x),
                    statement(s, q, x)

            );
        }

        @Test void testTraverseField() {
            ModelAssert.assertThat(scan(field(p, field(q)),

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
            ModelAssert.assertThat(scan(Or.or(field(p), field(q)),

                    statement(s, p, x),
                    statement(s, q, y),
                    statement(s, r, z)

            )).isIsomorphicTo(

                    statement(s, p, x),
                    statement(s, q, y)

            );
        }

        @Test void testTraverseWhen() {

            ModelAssert.assertThat(scan(when(Stem.stem("test:"), field(p), field(q)),

                    statement(s, p, x),
                    statement(s, q, y),
                    statement(s, r, z)

            )).isIsomorphicTo(

                    statement(s, p, x)

            );

            ModelAssert.assertThat(scan(when(Stem.stem("work:"), field(p), field(q)),

                    statement(s, p, x),
                    statement(s, q, y),
                    statement(s, r, z)

            )).isIsomorphicTo(

                    statement(s, q, y)

            );

        }

    }

}