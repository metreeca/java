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

package com.metreeca.link.shapes;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.All.all;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Any.any;
import static com.metreeca.link.shapes.Link.link;
import static com.metreeca.link.shapes.Or.or;

import static org.assertj.core.api.Assertions.assertThat;


final class AnyTest {

    @Nested final class Optimization {

        @Test void testIgnoreEmptyValueSet() {
            assertThat(any()).isEqualTo(and());
        }

        @Test void testCollapseDuplicates() {
            assertThat(any(True, True, False)).isEqualTo(any(True, False));
        }

        @Test void testConvertSingletonToUniversal() {
            assertThat(any(True)).isEqualTo(all(True));
        }

    }

    @Nested final class Probe {

        private final Value a=literal(1);
        private final Value b=literal(2);
        private final Value c=literal(3);

        @Test void testInspectAny() {
            assertThat(any(any(a, b, c)))
                    .hasValueSatisfying(values -> assertThat(values).containsExactly(a, b, c));
        }

        @Test void testInspectLink() {
            assertThat(any(link(OWL.SAMEAS, any(a, b, c))))
                    .hasValueSatisfying(values -> assertThat(values).containsExactly(a, b, c));
        }

        @Test void testInspectOr() {
            assertThat(any(or(any(a, b), any(b, c))))
                    .hasValueSatisfying(values -> assertThat(values).containsExactly(a, b, c));
        }

        @Test void testInspectOtherShape() {
            assertThat(any(and()))
                    .isEmpty();
        }
    }

}
