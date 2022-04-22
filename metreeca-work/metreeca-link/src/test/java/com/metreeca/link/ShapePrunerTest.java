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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Values.iri;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.convey;
import static com.metreeca.link.shapes.Guard.filter;
import static com.metreeca.link.shapes.Like.like;

import static org.assertj.core.api.Assertions.assertThat;


final class ShapePrunerTest {

    private static final IRI property=iri("test:x");
    private static final Shape constraint=like("constraint");


    @Nested final class Filter {

        private Shape prune(final Shape shape) {
            return shape.map(new ShapePruner(true));
        }


        @Test void testPrune() {
            assertThat(prune(constraint)).isEqualTo(and());
            assertThat(prune(field(property, constraint))).isEqualTo(and());
        }

        @Test void testRetainFilter() {
            assertThat(prune(filter(constraint))).isEqualTo(constraint);
            assertThat(prune(field(property, filter(constraint)))).isEqualTo(field(property, constraint));
        }

        @Test void testRemoveConvey() {
            assertThat(prune(convey(constraint))).isEqualTo(and());
            assertThat(prune(convey(field(property)))).isEqualTo(and());
        }

    }


    @Nested final class Convey {

        private Shape prune(final Shape shape) {
            return shape.map(new ShapePruner(false));
        }


        @Test void testPrune() {
            assertThat(prune(constraint)).isEqualTo(and());
            assertThat(prune(field(property, constraint))).isEqualTo(field(property));
        }

        @Test void testRemoveFilter() {
            assertThat(prune(filter(constraint))).isEqualTo(and());
            assertThat(prune(field(property, filter(constraint)))).isEqualTo(field(property));
        }

        @Test void testRetainConvey() {
            assertThat(prune(convey(constraint))).isEqualTo(constraint);
            assertThat(prune(convey(field(property)))).isEqualTo(field(property));
        }

    }

}