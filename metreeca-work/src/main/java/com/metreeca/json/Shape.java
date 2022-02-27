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

import com.metreeca.json.shapes.*;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * JSON-LD shape.
 */
public abstract class Shape {

    public static Shape shape(final Supplier<? extends Shape> factory) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        return new Lazy(factory);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract <V> V accept(final Visitor<V> visitor);


    public Optional<Value> validate(final Value value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return Optional.ofNullable(accept(new ShapeValidator(value)));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shape visitor.
     *
     * <p>Generates a result by visiting shapes.</p>
     *
     * @param <V> the type of the generated result value
     */
    public abstract static class Visitor<V> implements Function<Shape, V> {

        @Override public final V apply(final Shape shape) {

            if ( shape == null ) {
                throw new NullPointerException("null shape");
            }

            return shape.accept(this);
        }


        public abstract V visit(final ObjectShape shape);

        public abstract V visit(final ArrayShape shape);

        public abstract V visit(final OptionalShape shape);


        public abstract V visit(final IdShape shape);

        public abstract V visit(final DatetimeShape shape);

        public abstract V visit(final DateShape shape);

        public abstract V visit(final TimeShape shape);


        public abstract V visit(final LocalizedShape shape);

        public abstract V visit(final StringShape shape);

        public abstract V visit(final IntegerShape shape);

        public abstract V visit(final DecimalShape shape);

        public abstract V visit(final FloatingShape shape);

        public abstract V visit(final BooleanShape shape);

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Lazy extends Shape {

        private Shape shape;

        private final Supplier<? extends Shape> factory;


        private Lazy(final Supplier<? extends Shape> factory) { this.factory=factory; }


        @Override public <V> V accept(final Visitor<V> visitor) {

            if ( visitor == null ) {
                throw new NullPointerException("null visitor");
            }

            return shape().accept(visitor);
        }


        private Shape shape() {
            return shape == null
                    ? requireNonNull(shape=factory.get(), "null shape factory return value")
                    : shape;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Lazy
                    && factory.equals(((Lazy)object).factory);
        }

        @Override public int hashCode() {
            return factory.hashCode();
        }

        @Override public String toString() {
            return shape().toString();
        }

    }

}
