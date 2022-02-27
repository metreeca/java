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
import com.metreeca.json.shapes.ObjectShape.Field;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static com.metreeca.json.Value.array;
import static com.metreeca.json.Value.string;

import static java.lang.String.format;

final class ShapeValidator extends Shape.Visitor<Value> {

    private final Value focus;


    ShapeValidator(final Value focus) {
        this.focus=focus;
    }


    @Override public Value visit(final ObjectShape shape) {
        return focus == null ? string("missing <object> value") : focus.object().map(object -> {

            Value.Builder traces=null; // on-demand initialization

            for (final Entry<String, Field> entry : shape.fields().entrySet()) {

                final String label=entry.getKey();
                final Field field=entry.getValue();

                final Optional<Value> trace=field.shape().validate(object.get(label));

                if ( trace.isPresent() ) {
                    (traces == null ? traces=new Value.Builder() : traces).value(label, trace.get());
                }

            }

            for (final String label : object.keySet()) {
                if ( !shape.fields().containsKey(label) ) {

                    (traces == null ? traces=new Value.Builder() : traces).string(label, format(
                            "unknown field <%s>", label
                    ));

                }
            }

            return traces == null ? null : traces.build();

        }).orElseGet(() -> string("not an <object> value"));
    }

    @Override public Value visit(final ArrayShape shape) {
        return focus == null ? string("missing <array> value") : focus.array().map(validate(

                shape.minCount().map(minSize(List::size)),
                shape.maxCount().map(maxSize(List::size))

        )).orElseGet(() -> string("not an <array> value"));
    }

    @Override public Value visit(final OptionalShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final LocalizedShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }


    @Override public Value visit(final IdShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final DatetimeShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final DateShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final TimeShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }


    @Override public Value visit(final StringShape shape) {
        return focus == null ? string("missing <string> value") : focus.string().map(validate(

                shape.minLength().map(minSize(String::length)),
                shape.maxLength().map(maxSize(String::length)),

                shape.minInclusive().map(minInclusive()),
                shape.maxInclusive().map(maxInclusive()),
                shape.minExclusive().map(minExclusive()),
                shape.maxExclusive().map(maxExclusive()),

                shape.range().map(range())

        )).orElseGet(() -> string("not a <string> value"));
    }

    @Override public Value visit(final IntegerShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final DecimalShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final FloatingShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }

    @Override public Value visit(final BooleanShape shape) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs private <V> Function<V, Value> validate(final Optional<Function<V, Value>>... constraints) {
        return value -> {

            List<Value> traces=null; // on-demand initialization

            for (final Optional<Function<V, Value>> constraint : constraints) {

                final Value issue=constraint.map(function -> function.apply(value)).orElse(null);

                if ( issue != null ) {
                    (traces == null ? traces=new ArrayList<>() : traces).add(issue);
                }

            }

            return traces == null ? null : array(traces);

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <V> Function<Integer, Function<V, Value>> minSize(final ToIntFunction<? super V> metric) {
        return minLength -> value -> metric.applyAsInt(value) >= minLength ? null : string(format(
                "length not greater than or equal to <%d>", minLength
        ));
    }

    private <V> Function<Integer, Function<V, Value>> maxSize(final ToIntFunction<? super V> metric) {
        return maxLength -> value -> metric.applyAsInt(value) <= maxLength ? null : string(format(
                "length not lower than or equal to <%d>", maxLength
        ));
    }


    private <V extends Comparable<V>> Function<V, Function<V, Value>> minInclusive() {
        return minInclusive -> value -> minInclusive.compareTo(value) <= 0 ? null : string(format(
                "not greater than or equal to <%s>", minInclusive
        ));
    }

    private <V extends Comparable<V>> Function<V, Function<V, Value>> maxInclusive() {
        return maxInclusive -> value -> maxInclusive.compareTo(value) >= 0 ? null : string(format(
                "not lower than or equal to <%s>", maxInclusive
        ));
    }

    private <V extends Comparable<V>> Function<V, Function<V, Value>> minExclusive() {
        return minExclusive -> value -> minExclusive.compareTo(value) < 0 ? null : string(format(
                "not greater than <%s>", minExclusive
        ));
    }

    private <V extends Comparable<V>> Function<V, Function<V, Value>> maxExclusive() {
        return maxExclusive -> value -> maxExclusive.compareTo(value) > 0 ? null : string(format(
                "not lower than <%s>", maxExclusive
        ));
    }


    private <V extends Comparable<V>> Function<Set<V>, Function<V, Value>> range() {
        return range -> value -> range.contains(value) ? null : string(format(
                "not within accepted range <%s>", range
        ));
    }

}
