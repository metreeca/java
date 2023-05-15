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

package com.metreeca.http.rdf;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.metreeca.http.rdf.Shift.Alt.alt;
import static com.metreeca.http.rdf.Shift.Seq.seq;
import static com.metreeca.http.rdf.Shift.Step.step;
import static com.metreeca.http.rdf.Values.term;

import static org.assertj.core.api.Assertions.assertThat;

final class ShiftTest {

    @Nested final class AltTest {

        private final Shift.Path p=step(term("p"));
        private final Shift.Path q=step(term("q"));


        @Nested final class Optimization {

            @Test void testUnwrapSingletons() {
                assertThat(alt(p)).isEqualTo(p);
            }

            @Test void testPreserveOrder() {
                assertThat(alt(p, q).map(new Shift.Probe<Set<Shift.Path>>() {

                    @Override public Set<Shift.Path> probe(final Shift.Alt alt) { return alt.paths(); }

                })).containsExactly(p, q);
            }

            @Test void testCollapseDuplicates() {

                assertThat(alt(p, p)).isEqualTo(p);

                assertThat(alt(p, p, q).map(new Shift.Probe<Set<Shift.Path>>() {

                    @Override public Set<Shift.Path> probe(final Shift.Alt alt) { return alt.paths(); }

                })).containsExactly(p, q);
            }

        }

    }

    @Nested final class SeqTest {

        private final Shift.Path p=step(term("p"));
        private final Shift.Path q=step(term("q"));


        @Nested final class Optimization {

            @Test void testUnwrapSingletons() {
                assertThat(seq(p)).isEqualTo(p);
            }

            @Test void testPreserveOrder() {
                assertThat(seq(p, q).map(new Shift.Probe<List<Shift.Path>>() {

                    @Override public List<Shift.Path> probe(final Shift.Seq seq) { return seq.paths(); }

                })).containsExactly(p, q);
            }

        }

    }
}