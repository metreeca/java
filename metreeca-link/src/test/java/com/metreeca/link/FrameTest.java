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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.FrameAssert.assertThat;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shifts.Seq.seq;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

final class FrameTest {

    private static final IRI w=iri("http://example.com/w");
    private static final IRI x=iri("http://example.com/x");
    private static final IRI y=iri("http://example.com/y");
    private static final IRI z=iri("http://example.com/z");


    @Nested final class Assembling {

        @Test void testHandleDirectAndInverseTraits() {

            final Frame frame=frame(x)
                    .value(RDF.VALUE, y)
                    .value(inverse(RDF.VALUE), z);

            assertThat(frame.values(RDF.VALUE)).containsExactly(y);
            assertThat(frame.values(inverse(RDF.VALUE))).containsExactly(z);
        }

        @Test void testHandleNestedFrames() {

            final Frame frame=frame(w)
                    .value(RDF.VALUE, x)
                    .frame(RDF.VALUE, frame(y)
                            .value(RDF.VALUE, w)
                    )
                    .value(RDF.VALUE, z);

            assertThat(frame).isIsomorphicTo(frame(w, asList(
                    statement(w, RDF.VALUE, x),
                    statement(w, RDF.VALUE, y),
                    statement(y, RDF.VALUE, w),
                    statement(w, RDF.VALUE, z)
            )));
        }

        @Test void tesIgnoreLiteralSubjectsForDirectTraits() {

            final Frame frame=frame(literal(1));

            assertThat(frame.value(RDF.VALUE, x))
                    .isEqualTo(frame);
        }

        @Test void testIgnoreLiteralObjectsForInverseTraits() {

            final Frame frame=frame(x);

            assertThat(frame.value(inverse(RDF.VALUE), literal(1)))
                    .isEqualTo(frame);
        }

    }

    @Nested final class Importing {

        @Test void testImportDirectStatements() {
            assertThat(frame(x, singletonList(

                    statement(x, RDF.VALUE, y)

            ))).isIsomorphicTo(frame(x, singletonList(

                    statement(x, RDF.VALUE, y)

            )));
        }

        @Test void testImportInverseStatements() {
            assertThat(frame(x, singletonList(

                    statement(y, RDF.VALUE, x)

            ))).isIsomorphicTo(frame(x, singletonList(

                    statement(y, RDF.VALUE, x)

            )));
        }

        @Test void testIgnoreUnconnectedStatements() {
            assertThat(frame(x, asList(

                    statement(x, RDF.VALUE, y),
                    statement(w, RDF.VALUE, z)

            ))).isIsomorphicTo(frame(x, singletonList(

                    statement(x, RDF.VALUE, y)

            )));
        }

        @Test void testImportTransitiveStatements() {
            assertThat(frame(x, asList(

                    statement(x, RDF.FIRST, y),
                    statement(y, RDF.REST, z)

            ))).isIsomorphicTo(frame(x, asList(

                    statement(x, RDF.FIRST, y),
                    statement(y, RDF.REST, z)

            )));
        }

        @Test void testImportSelfLinks() {
            assertThat((

                    frame(x, singletonList(

                            statement(x, RDF.VALUE, x)

                    )).values(RDF.VALUE)

            )).containsExactly(x);
        }

        @Test void testImportBackLinks() {
            assertThat((

                    frame(x, asList(

                            statement(x, RDF.VALUE, y),
                            statement(y, RDF.VALUE, x)

                    )).values(seq(
                            RDF.VALUE, RDF.VALUE
                    ))

            )).containsExactly(x);
        }

    }

    @Nested final class Exporting {

        @Test void testExportDirectStatements() {
            assertThat(frame(x).value(RDF.VALUE, y)


            ).isIsomorphicTo(frame(x, singletonList(

                    statement(x, RDF.VALUE, y)

            )));
        }

        @Test void testExportInverseStatements() {
            assertThat(frame(x).value(inverse(RDF.VALUE), y)


            ).isIsomorphicTo(frame(x, singletonList(

                    statement(y, RDF.VALUE, x)

            )));
        }

        @Test void testExportTransitiveStatements() {
            assertThat(frame(x)

                    .frame(RDF.FIRST, frame(y)
                            .value(RDF.REST, z)
                    )


            ).isIsomorphicTo(frame(x, asList(

                    statement(x, RDF.FIRST, y),
                    statement(y, RDF.REST, z)

            )));
        }

    }

    @Nested final class Shifting {

        @Test void testShiftStep() {
            assertThat(

                    frame(w, List.of(
                            statement(w, RDF.FIRST, x),
                            statement(x, RDF.REST, y),
                            statement(x, RDF.REST, z)
                    ))

                            .values(RDF.FIRST)
                            .collect(toSet())

            ).containsExactlyInAnyOrder(x);
        }

        @Test void testShiftSeq() {
            assertThat(

                    frame(w, List.of(
                            statement(w, RDF.FIRST, x),
                            statement(x, RDF.REST, y),
                            statement(x, RDF.REST, z)
                    ))

                            .values(seq(RDF.FIRST, RDF.REST))
                            .collect(toSet())

            ).containsExactlyInAnyOrder(y, z);
        }

    }

}