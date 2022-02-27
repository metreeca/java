/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.json;

import com.metreeca.json.shapes.StringShape;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.metreeca.json.Value.array;
import static com.metreeca.json.Value.object;
import static com.metreeca.json.Value.string;
import static com.metreeca.json.shapes.ArrayShape.array;
import static com.metreeca.json.shapes.ComparableShape.*;
import static com.metreeca.json.shapes.ContainerShape.maxCount;
import static com.metreeca.json.shapes.ContainerShape.minCount;
import static com.metreeca.json.shapes.ObjectShape.field;
import static com.metreeca.json.shapes.ObjectShape.object;
import static com.metreeca.json.shapes.OptionalShape.optional;
import static com.metreeca.json.shapes.StringShape.maxLength;
import static com.metreeca.json.shapes.StringShape.string;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.List.of;

final class ShapeValidatorTest {

    private ShapeValidator validator(final Value value) {
        return new ShapeValidator(value);
    }


    @Nested final class ObjectTest {

        @Test void testValidatePresence() {

            final Shape shape=object();

            assertThat(shape.accept(validator(object(Map.of())))).isNull();
            assertThat(shape.accept(validator(null))).isNotNull();

        }

        @Test void testValidateType() {

            final Shape shape=object();

            assertThat(shape.accept(validator(object(Map.of())))).isNull();
            assertThat(shape.accept(new ShapeValidator(string("")))).isNotNull();

        }

        @Test void testValidateFieldSet() {

            final Shape shape=object(
                    field("expected", string()),
                    field("optional", optional(string()))
            );

            assertThat(shape.accept(validator(object(Map.of("expected", string("")))))).isNull();

            assertThat(shape.accept(validator(object(Map.of())))).isNotNull();
            assertThat(shape.accept(validator(object(Map.of("unexpected", string("")))))).isNotNull();

        }

        @Test void testValidateFieldShapes() { }

    }

    @Nested final class ArrayTest {

        @Test void testValidatePresence() {

            final Shape shape=array(string());

            assertThat(shape.accept(validator(array(of())))).isNull();
            assertThat(shape.accept(validator(null))).isNotNull();

        }

        @Test void testValidateType() {

            final Shape shape=array(string());

            assertThat(shape.accept(validator(array(of(string("")))))).isNull();
            assertThat(shape.accept(validator(string("")))).isNotNull();

        }

        @Test void testValidateItemShapes() { }

        @Test void testValidateMinLength() {

            final Shape shape=array(string(), minCount(2));

            assertThat(shape.accept(validator(array(of(string("uno"), string("due")))))).isNull();
            assertThat(shape.accept(validator(array(of(string("uno")))))).isNotNull();

        }

        @Test void testValidateMaxLength() {

            final Shape shape=array(string(), maxCount(2));

            assertThat(shape.accept(validator(array(of(string("uno"), string("due")))))).isNull();
            assertThat(shape.accept(validator(array(of(string("uno"), string("due"), string("tre")))))).isNotNull();

        }

    }

    @Nested final class OptionalTest {

    }

    @Nested final class StringTest {

        @Test void testValidatePresence() {

            final Shape shape=string();

            assertThat(shape.accept(validator(string("")))).isNull();
            assertThat(shape.accept(validator(null))).isNotNull();

        }

        @Test void testValidateType() {

            final Shape shape=string();

            assertThat(shape.accept(validator(string("")))).isNull();
            assertThat(shape.accept(validator(object(Map.of())))).isNotNull();

        }

        @Test void testValidateMinLength() {

            final Shape shape=string(StringShape.minLength(3));

            assertThat(shape.accept(validator(string("----")))).isNull();
            assertThat(shape.accept(validator(string("---")))).isNull();
            assertThat(shape.accept(validator(string("--")))).isNotNull();

        }

        @Test void testValidateMaxLength() {

            final Shape shape=string(maxLength(3));

            assertThat(shape.accept(validator(string("--")))).isNull();
            assertThat(shape.accept(validator(string("---")))).isNull();
            assertThat(shape.accept(validator(string("----")))).isNotNull();

        }

        @Test void testValidateMinInclusive() {

            final Shape shape=string(minInclusive("bb"));

            assertThat(shape.accept(validator(string("bc")))).isNull();
            assertThat(shape.accept(validator(string("bb")))).isNull();
            assertThat(shape.accept(validator(string("az")))).isNotNull();

        }

        @Test void testValidateMaxInclusive() {

            final Shape shape=string(maxInclusive("bb"));

            assertThat(shape.accept(validator(string("az")))).isNull();
            assertThat(shape.accept(validator(string("bb")))).isNull();
            assertThat(shape.accept(validator(string("bc")))).isNotNull();

        }

        @Test void testValidateMinExclusive() {

            final Shape shape=string(minExclusive("bb"));

            assertThat(shape.accept(validator(string("bc")))).isNull();
            assertThat(shape.accept(validator(string("bb")))).isNotNull();
            assertThat(shape.accept(validator(string("az")))).isNotNull();

        }

        @Test void testValidateMaxExclusive() {

            final Shape shape=string(maxExclusive("bb"));

            assertThat(shape.accept(validator(string("az")))).isNull();
            assertThat(shape.accept(validator(string("bb")))).isNotNull();
            assertThat(shape.accept(validator(string("bc")))).isNotNull();

        }

        @Test void testValidateRange() {

            final Shape shape=string(range("a", "b", "c"));

            assertThat(shape.accept(validator(string("a")))).isNull();
            assertThat(shape.accept(validator(string("x")))).isNotNull();

        }

    }

}