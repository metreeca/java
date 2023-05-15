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

package com.metreeca.json;

import com.metreeca.http.work.Xtream;

import javax.json.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * JSONPath processor.
 *
 * <p>Applies JSONPath expression to a target JSON value.</p>
 */
public final class JSONPath {

    private static final String Wildcard="*";

    private static final Pattern IndexPattern=Pattern.compile("\\d+");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final JsonValue target;


    /**
     * Creates a JSONPath processor.
     *
     * @param target the target JSON value for the processor
     *
     * @throws NullPointerException if {@code value} is null
     */
    public JSONPath(final JsonValue target) {

        if ( target == null ) {
            throw new NullPointerException("null value");
        }

        this.target=target;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Boolean> bool(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return bools(path).findFirst();
    }

    public Xtream<Boolean> bools(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return values(path)
                .map(v -> v == JsonValue.TRUE ? Boolean.TRUE : v == JsonValue.FALSE ? Boolean.FALSE : null)
                .filter(Objects::nonNull);
    }


    public Optional<BigInteger> integer(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return integers(path).findFirst();
    }

    public Xtream<BigInteger> integers(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return values(path)
                .filter(JsonNumber.class::isInstance)
                .map(JsonNumber.class::cast)
                .map(JsonNumber::bigIntegerValue);
    }


    public Optional<BigDecimal> decimal(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return decimals(path).findFirst();
    }

    public Xtream<BigDecimal> decimals(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return values(path)
                .filter(JsonNumber.class::isInstance)
                .map(JsonNumber.class::cast)
                .map(JsonNumber::bigDecimalValue);
    }


    /**
     * Retrieves a string from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return an optional non-empty string produced by evaluating {@code path} against the target value, if one was
     * available and not empty; an empty optional, otherwise
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Optional<String> string(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return strings(path).findFirst();
    }

    /**
     * Retrieves strings from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of non-empty strings produced by evaluating {@code path} against the target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Xtream<String> strings(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return values(path)
                .map(v -> v instanceof JsonString ? ((JsonString)v).getString() : "")
                .filter(s -> !s.isEmpty());
    }


    /**
     * Retrieves a JSON object from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return an optional JSON object produced by evaluating {@code path} against the target value, if one was
     * available; an empty optional, otherwise
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Optional<JsonObject> object(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return objects(path).findFirst();
    }

    /**
     * Retrieves JSON objects from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of JSON objects produced by evaluating {@code path} against the target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Xtream<JsonObject> objects(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return values(path)
                .filter(value -> value.getValueType() == JsonValue.ValueType.OBJECT)
                .map(JsonValue::asJsonObject);
    }


    /**
     * Retrieves a JSON array from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return an optional JSON array produced by evaluating {@code path} against the target value, if one was available;
     * an empty optional, otherwise
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Optional<JsonArray> array(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return arrays(path).findFirst();
    }

    /**
     * Retrieves JSON arrays from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of JSON objects produced by evaluating {@code path} against the target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Xtream<JsonArray> arrays(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return values(path)
                .filter(value -> value.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray);
    }


    /**
     * Retrieves JSONPath processors from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of JSONPath processors targeting the values generated by evaluating {@code path} against the
     * current target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Optional<JsonValue> value(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return values(path).findFirst();
    }

    /**
     * Retrieves JSON values from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of JSON values produced by evaluating {@code path} against the target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Xtream<JsonValue> values(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        Xtream<JsonValue> values=Xtream.of(target);

        for (final String step : Arrays
                .stream(path.split("\\."))
                .map(String::trim)
                .filter(not(String::isEmpty))
                .collect(toList())
        ) {
            values=values.flatMap(value -> {

                switch ( value.getValueType() ) {

                    case OBJECT: return step.equals(Wildcard)

                            ? value.asJsonObject().values().stream()
                            : Stream.of(value.asJsonObject().get(step));

                    case ARRAY: return step.equals(Wildcard) ? value.asJsonArray().stream()
                            : IndexPattern.matcher(step).matches() ?
                            Stream.of(value.asJsonArray().get(parseInt(step))) // !!! bounds
                            : Stream.<JsonValue>empty();

                    default:

                        return Xtream.empty();

                }

            }).filter(Objects::nonNull);
        }

        return values;
    }


    /**
     * Retrieves a JSONPath processor from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return an optional JSONPath processor targeting the value generated by evaluating {@code path} against the
     * current target value, if one was available; an empty optional, otherwise
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Optional<JSONPath> path(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return paths(path).findFirst();
    }

    /**
     * Retrieves JSON values from the target value.
     *
     * @param path the JSONPath expression to be evaluated against the target value
     *
     * @return a stream of JSON values produced by evaluating {@code path} against the target value
     *
     * @throws NullPointerException if {@code path} is null
     */
    public Xtream<JSONPath> paths(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null JSONPath expression");
        }

        return values(path).map(JSONPath::new);
    }


    public Xtream<Entry<String, JSONPath>> entries(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        return objects(path).flatMap(object -> object.entrySet().stream()).map(entry ->
                entry(entry.getKey(), new JSONPath(entry.getValue()))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public boolean equals(final Object object) {
        return this == object || object instanceof JSONPath
                && target.equals(((JSONPath)object).target);
    }

    @Override public int hashCode() {
        return target.hashCode();
    }

    @Override public String toString() {
        return target.toString();
    }

}
