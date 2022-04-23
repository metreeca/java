/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.link.shapes;

import com.metreeca.link.Shape;
import com.metreeca.link.Values;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.link.Values.inverse;
import static com.metreeca.link.Values.literal;
import static com.metreeca.link.shapes.All.all;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Datatype.datatype;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.guard;
import static com.metreeca.link.shapes.Lang.lang;
import static com.metreeca.link.shapes.Link.link;
import static com.metreeca.link.shapes.Localized.localized;
import static com.metreeca.link.shapes.MaxCount.maxCount;
import static com.metreeca.link.shapes.MinCount.minCount;
import static com.metreeca.link.shapes.Or.or;
import static com.metreeca.link.shapes.Range.range;
import static com.metreeca.link.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


final class AndTest {

    //@Test void testOrdering() {
    //	assertThat(and(
    //
    //			field(RDF.FIRST),
    //			convey(field(RDF.VALUE)),
    //			field(RDF.REST)
    //
    //	).map(new Shape.Probe<Collection<Shape>>() {
    //
    //		@Override public Collection<Shape> probe(final And and) { return and.shapes(); }
    //
    //	})).containsExactly(
    //
    //			field(RDF.FIRST),
    //			convey(field(RDF.VALUE)),
    //			field(RDF.REST)
    //
    //	);
    //}

    @Nested final class Optimization {

        private final Value a=literal(1);
        private final Value b=literal(2);
        private final Value c=literal(3);


        @Test void testSimplifyConstants() {
            assertThat(and(or(), field(RDF.TYPE))).isEqualTo(or());
        }

        @Test void testUnwrapSingletons() {
            assertThat(and(datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
        }

        @Test void testUnwrapUniqueValues() {
            assertThat(and(datatype(RDF.NIL), datatype(RDF.NIL))).isEqualTo(datatype(RDF.NIL));
        }

        @Test void testCollapseDuplicates() {
            assertThat(and(datatype(RDF.NIL), datatype(RDF.NIL), minCount(1))).isEqualTo(and(datatype(RDF.NIL),
                    minCount(1)));
        }

        @Test void testPreserveOrder() {
            assertThat(and(

                    field(RDF.FIRST), field(RDF.REST)

            ).map(new Shape.Probe<Collection<Shape>>() {

                @Override public Collection<Shape> probe(final And and) { return and.shapes(); }

            })).containsExactly(

                    field(RDF.FIRST), field(RDF.REST)

            );
        }


        @Test void testCollapseToAbstractDerivedDatatypes() {
            assertThat(and(datatype(Values.IRIType), datatype(Values.ResourceType)))
                    .isEqualTo(datatype(Values.IRIType));
        }

        @Test void testCollapseToConcreteDerivedDatatypes() {
            assertThat(and(datatype(Values.LiteralType), datatype(XSD.STRING)))
                    .isEqualTo(datatype(XSD.STRING));
        }

        @Test void testRetainUnrelatedDatatypes() {
            assertThat(and(datatype(Values.ResourceType), datatype(XSD.STRING)))
                    .isEqualTo(and(datatype(Values.ResourceType), datatype(XSD.STRING)));
        }

        @Test void testOptimizeRange() {

            assertThat(and(range(a, b), range(b, c))).isEqualTo(range(b));

            assertThat(and(range(a, b), range())).isEqualTo(range(a, b));
            assertThat(and(range(), range(b, c))).isEqualTo(range(b, c));

            assertThatIllegalArgumentException().isThrownBy(() -> and(range(a), range(b)));

        }

        @Test void testOptimizeLang() {

            assertThat(and(lang("en", "it"), lang("en", "fr"))).isEqualTo(lang("en"));

            assertThat(and(lang(), lang("en", "fr"))).isEqualTo(lang("en", "fr"));
            assertThat(and(lang("en", "it"), lang())).isEqualTo(lang("en", "it"));

            assertThatIllegalArgumentException().isThrownBy(() -> and(lang("en"), lang("fr")));

        }


        @Test void testOptimizeMinCount() {
            assertThat(and(minCount(10), minCount(100))).isEqualTo(minCount(100));
        }

        @Test void testOptimizeMaxCount() {
            assertThat(and(maxCount(10), maxCount(100))).isEqualTo(maxCount(10));
        }

        @Test void testOptimizeAll() {
            assertThat(and(all(a, b), all(b, c))).isEqualTo(all(a, b, c));
        }

        @Test void testOptimizeLocalized() {
            assertThat(and(localized(), localized())).isEqualTo(localized());
        }


        @Test void testMergeCompatibleFields() {
            assertThat(and(

                    field(RDF.VALUE, minCount(1)),
                    field(RDF.VALUE, maxCount(3))

            )).isEqualTo(

                    field(RDF.VALUE, and(minCount(1), maxCount(3)))

            );
        }

        @Test void testDifferentiateInverseFields() {
            assertThat(and(

                    field(RDF.VALUE),
                    field(inverse(RDF.VALUE))

            ).map(new Shape.Probe<Collection<Shape>>() {

                @Override public Collection<Shape> probe(final And and) { return and.shapes(); }

            })).containsExactly(

                    field(RDF.VALUE),
                    field(inverse(RDF.VALUE))

            );
        }

        @Test void testCollapseEqualFieldLabels() {
            assertThat(and(

                    field("alias", RDF.VALUE),
                    field("alias", RDF.VALUE)

            )).isEqualTo(

                    field("alias", RDF.VALUE)

            );
        }

        @Test void testCollapseCompatibleFieldLabels() {
            assertThat(and(

                    field(RDF.VALUE),
                    field("alias", RDF.VALUE)

            )).isEqualTo(

                    field("alias", RDF.VALUE)

            );
        }

        @Test void testReportClashingFieldLabels() {
            assertThatIllegalArgumentException().isThrownBy(() -> and(

                    field("x", RDF.VALUE),
                    field("y", RDF.VALUE)

            ));
        }


        @Test void testMergeDirectLinks() {
            assertThat(and(

                    link(OWL.SAMEAS, minCount(1)),
                    link(OWL.SAMEAS, maxCount(2))

            )).isEqualTo(link(OWL.SAMEAS, and(minCount(1), maxCount(2))));
        }

        @Test void testMergeInverseLinks() {
            assertThat(and(

                    link(inverse(OWL.SAMEAS), minCount(1)),
                    link(inverse(OWL.SAMEAS), maxCount(2))

            )).isEqualTo(link(inverse(OWL.SAMEAS), and(minCount(1), maxCount(2))));
        }

        @Test void testReportConflictingLinks() {
            assertThatIllegalArgumentException().isThrownBy(() -> and(

                    link(OWL.SAMEAS, minCount(1)),
                    link(inverse(OWL.SAMEAS), maxCount(2))

            ));
        }


        @Test void testMergeCompatibleWhens() {
            assertThat(and(

                    when(guard("axis", "value"), minCount(1)),
                    when(guard("axis", "value"), maxCount(3))

            )).isEqualTo(

                    when(guard("axis", "value"), and(minCount(1), maxCount(3)))

            );
        }

    }

}
