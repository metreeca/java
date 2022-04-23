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

import com.metreeca.link.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Values.iri;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Datatype.datatype;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Field.labels;
import static com.metreeca.link.shapes.Or.or;
import static com.metreeca.link.shapes.When.when;

import static org.assertj.core.api.Assertions.*;

import static java.util.Collections.singletonMap;


final class FieldTest {

    @Nested final class Optimization {

        @Test void testPruneDeadPaths() {
            assertThat(field(RDF.VALUE, or())).isEqualTo(and());
        }

    }

    @Nested class Labels {

        @Test void testInspectAnd() {
            assertThat(labels(and(

                    field(RDF.FIRST),
                    field(RDF.REST)

            ))).containsKeys("first", "rest");
        }

        @Test void testInspectOr() {
            assertThat(labels(or(

                    field(RDF.FIRST),
                    field(RDF.REST)

            ))).containsKeys("first", "rest");
        }

        @Test void testInspectWhen() {
            assertThat(labels(when(

                    datatype(RDF.NIL),
                    field(RDF.FIRST),
                    field(RDF.REST)

            ))).containsKeys("first", "rest");
        }

        @Test void testInspectOtherShapes() {
            assertThat(labels(and())).isEmpty();
        }


        @Test void testGuessLabelFromIRI() {

            assertThat(labels(field(RDF.VALUE)))
                    .as("direct")
                    .containsKey("value");

            assertThat(labels(field(Values.inverse(RDF.VALUE))))
                    .as("inverse")
                    .containsKey("valueOf");

        }

        @Test void testRetrieveUserDefinedLabel() {
            assertThat(labels(field("alias", RDF.VALUE)))
                    .as("user-defined")
                    .containsKey("alias");
        }

        @Test void testPreferUserDefinedFields() {
            assertThat(labels(and(field("alias", RDF.VALUE), field(RDF.VALUE))))
                    .as("user-defined")
                    .containsKey("alias");
        }


        @Test void testReportConflictingFields() {
            assertThatThrownBy(() -> labels(and(
                    field("alias", RDF.FIRST),
                    field("alias", RDF.REST)
            ))).isInstanceOf(IllegalArgumentException.class);
        }

        @Test void testReportConflictingProperties() {
            assertThatThrownBy(() -> labels(and(field(RDF.VALUE), field(iri("urn:example#value"), and()))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test void testReportReservedLabels() {

            assertThatIllegalArgumentException().isThrownBy(() ->
                    labels(field(iri("http://exampe.com/", "@id"), and()))
            );

            assertThatIllegalArgumentException().isThrownBy(() ->
                    labels(field("@id", RDF.VALUE))
            );

            assertThatIllegalArgumentException().isThrownBy(() ->
                    labels(field("id", RDF.VALUE), singletonMap("@id", "id"))
            );

        }

        @Test void testIgnoreCompatibleDuplicates() {
            assertThat(labels(or(

                    and(
                            field(RDF.FIRST),
                            field(RDF.REST)
                    ),
                    field(RDF.FIRST)

            ))).containsKeys(
                    "first", "rest"
            );
        }

    }

}
