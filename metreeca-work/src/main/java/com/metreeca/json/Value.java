package com.metreeca.json;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

/**
 * JSON-LD value.
 */
public abstract class Value {

    private static final Function<Value, Object> asRaw=new RawVisitor() { };

    private static final Function<Value, Map<String, Value>> asObject=new TypedVisitor<>() {

        @Override public Map<String, Value> visit(final Map<String, Value> object) { return object; }

    };

    private static final Function<Value, List<Value>> asArray=new TypedVisitor<>() {

        @Override public List<Value> visit(final List<Value> array) { return array; }

    };

    private static final Function<Value, String> asString=new TypedVisitor<>() {

        @Override public String visit(final String string) { return string; }

    };


    static List<Value> list(final Value... values) {

        if ( values == null || stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return asList(values);
    }

    @SafeVarargs static Map<String, Value> map(final Entry<String, Value>... fields) { // ;( preserver order

        if ( fields == null || stream(fields).anyMatch(e -> e.getKey() == null || e.getValue() == null) ) {
            throw new NullPointerException("null fields");
        }

        final Map<String, Value> map=new LinkedHashMap<>();

        for (final Entry<String, Value> entry : fields) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Value object(final Map<String, Value> fields) {

        if ( fields == null || fields.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null) ) {
            throw new NullPointerException("null fields");
        }

        return new Value() {

            private final Map<String, Value> value=unmodifiableMap(fields);

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(value);
            }

        };
    }

    public static Value array(final List<Value> values) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return new Value() {

            private final List<Value> value=unmodifiableList(values);

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(value);
            }

        };
    }


    public static Value id(final URI uri) {

        if ( uri == null ) {
            throw new NullPointerException("null uri");
        }

        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(uri);
            }

        };
    }

    public static Value datetime(final LocalDateTime datetime) {

        if ( datetime == null ) {
            throw new NullPointerException("null datetime");
        }

        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(datetime);
            }

        };
    }

    public static Value date(final LocalDate date) {

        if ( date == null ) {
            throw new NullPointerException("null date");
        }

        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(date);
            }

        };
    }

    public static Value time(final LocalTime time) {

        if ( time == null ) {
            throw new NullPointerException("null time");
        }

        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(time);
            }

        };
    }


    public static Value string(final CharSequence sequence) {

        if ( sequence == null ) {
            throw new NullPointerException("null sequence");
        }

        return new Value() {

            private final String value=sequence.toString();

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(value);
            }

        };
    }

    public static Value integer(final long integer) {
        return integer(BigInteger.valueOf(integer));
    }

    public static Value integer(final BigInteger integer) {

        if ( integer == null ) {
            throw new NullPointerException("null integer");
        }

        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(integer);
            }

        };
    }

    public static Value decimal(final double decimal) {
        return decimal(BigDecimal.valueOf(decimal));
    }

    public static Value decimal(final BigDecimal decimal) {
        return decimal == null ? null : new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(decimal);
            }

        };
    }

    public static Value floating(final double floating) {
        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(floating);
            }

        };
    }

    public static Value bool(final boolean bool) {
        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit(bool);
            }

        };
    }

    public static Value nil() {
        return new Value() {

            @Override public <V> V accept(final Visitor<V> visitor) {

                if ( visitor == null ) {
                    throw new NullPointerException("null visitor");
                }

                return visitor.visit((Void)null);
            }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Read a JSON value.
     *
     * @param reader the reader for the JSON document to be read
     *
     * @return the JSON value read from {@code reader}
     *
     * @throws NullPointerException if {@code reader} is null
     * @throws IOException          if an error occurs while reading from {@code reader}
     * @throws ParseException       if {@code reader} doesn't contain a well-formed JSON object value
     * @implNote The reader conforms to <a href="http://tools.ietf.org/html/rfc4627">RFC 4180 The application/json Media
     * Type for JavaScript Object Notation (JSON)</a> with the following known deviations:
     *
     * <ul>
     *
     * <li>{@code null} values are silently ignored, both as object member values and as array elements;</li>
     * <li>unknown single character escape sequences in strings are converted to the escaped character.</li>
     *
     * </ul>
     */
    public Value read(final Reader reader) throws IOException, ParseException {

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        return new ValueReader(reader).read();
    }

    public static <W extends Writer> W write(final W writer, final Value value) throws IOException {

        if ( writer == null ) {
            throw new NullPointerException("null writer");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        new ValueWriter(writer).write(value);

        return writer;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Value() { }


    public abstract <V> V accept(final Visitor<V> visitor);


    public Optional<Value> value() {
        return Optional.of(this);
    }

    public Optional<Value> value(final String label) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        return value().map(asObject).map(object -> object.get(label));
    }

    public Stream<Value> values() {
        return value().map(asArray).stream().flatMap(Collection::stream);
    }

    public Stream<Value> values(final String label) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        return value(label).map(asArray).stream().flatMap(Collection::stream);
    }


    public Optional<Map<String, Value>> object() {
        return value().map(asObject);
    }

    public Optional<List<Value>> array() {
        return value().map(asArray);
    }


    public Optional<String> string() {
        return value().map(asString);
    }

    public Optional<String> string(final String label) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        return value(label).map(asString);
    }

    public Stream<String> strings() {
        return values().map(asString).filter(Objects::nonNull);
    }

    public Stream<String> strings(final String label) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        return values(label).map(asString).filter(Objects::nonNull);
    }


    @Override public boolean equals(final Object object) {
        return this == object || object instanceof Value
                && Objects.equals(asRaw.apply(this), asRaw.apply((Value)object));
    }

    @Override public int hashCode() {
        return Objects.hashCode(asRaw.apply(this));
    }

    @Override public String toString() {
        try ( final StringWriter writer=new StringWriter() ) {

            return write(writer, this).toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Builder {

        private final Map<String, Value> fields=new LinkedHashMap<>();


        private <V> Builder put(final String label, final V value, final Function<V, Value> builder) {

            fields.put(label, builder.apply(value));

            return this;
        }

        private <V> Builder put(final String label, final Stream<V> values, final Function<V, Value> builder) {

            fields.put(label, array(values
                    .map(value -> requireNonNull(value, "null values"))
                    .map(builder)
                    .collect(toList())
            ));

            return this;
        }


        public Builder value(final String label, final Value value) {

            if ( label == null ) {
                throw new NullPointerException("null label");
            }

            if ( value == null ) {
                throw new NullPointerException("null value");
            }

            return put(label, value, identity());
        }

        public Builder values(final String label, final Stream<Value> values) {

            if ( label == null ) {
                throw new NullPointerException("null label");
            }

            if ( values == null ) {
                throw new NullPointerException("null values");
            }

            return put(label, values, identity());
        }


        public Builder string(final String label, final CharSequence string) {

            if ( label == null ) {
                throw new NullPointerException("null label");
            }

            if ( string == null ) {
                throw new NullPointerException("null string");
            }

            return put(label, string, Value::string);
        }

        public Builder strings(final String label, final Stream<CharSequence> strings) {

            if ( label == null ) {
                throw new NullPointerException("null label");
            }

            if ( strings == null ) {
                throw new NullPointerException("null strings");
            }

            return put(label, strings, (Function<CharSequence, Value>)Value::string);
        }


        public Value build() {
            return object(fields); // !!! avoid checks
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract static class Visitor<V> implements Function<Value, V> {

        @Override public final V apply(final Value value) {

            if ( value == null ) {
                throw new NullPointerException("null value");
            }

            return value.accept(this);
        }


        public abstract V visit(final Map<String, Value> object);

        public abstract V visit(final List<Value> array);


        public abstract V visit(final URI id);

        public abstract V visit(final LocalDateTime datetime);

        public abstract V visit(final LocalDate date);

        public abstract V visit(final LocalTime time);

        public abstract V visit(final NavigableMap<Locale, List<String>> localized);


        public abstract V visit(final String string);

        public abstract V visit(final BigInteger integer);

        public abstract V visit(final BigDecimal decimal);

        public abstract V visit(final Double floating);

        public abstract V visit(final Boolean bool);

        public abstract V visit(final Void nil);

    }


    private abstract static class RawVisitor extends Visitor<Object> {

        @Override public Object visit(final Map<String, Value> object) { return object; }

        @Override public Object visit(final List<Value> array) { return array; }


        @Override public Object visit(final URI id) { return id; }

        @Override public Object visit(final LocalDateTime datetime) { return datetime; }

        @Override public Object visit(final LocalDate date) { return date; }

        @Override public Object visit(final LocalTime time) { return time; }

        @Override public Object visit(final NavigableMap<Locale, List<String>> localized) {
            return localized;
        }


        @Override public Object visit(final String string) { return string; }

        @Override public Object visit(final BigInteger integer) { return integer; }

        @Override public Object visit(final BigDecimal decimal) { return decimal; }

        @Override public Object visit(final Double floating) { return floating; }

        @Override public Object visit(final Boolean bool) { return bool; }

        @Override public Object visit(final Void nil) {
            return nil;
        }

    }

    private abstract static class TypedVisitor<V> extends Visitor<V> {

        @Override public V visit(final Map<String, Value> object) { return null; }

        @Override public V visit(final List<Value> array) { return null; }


        @Override public V visit(final URI id) { return null; }

        @Override public V visit(final LocalDateTime datetime) { return null; }

        @Override public V visit(final LocalDate date) { return null; }

        @Override public V visit(final LocalTime time) { return null; }

        @Override public V visit(final NavigableMap<Locale, List<String>> localized) {
            return null;
        }


        @Override public V visit(final String string) { return null; }

        @Override public V visit(final BigInteger integer) { return null; }

        @Override public V visit(final BigDecimal decimal) { return null; }

        @Override public V visit(final Double floating) { return null; }

        @Override public V visit(final Boolean bool) { return null; }

        @Override public V visit(final Void nil) {
            return null;
        }

    }

}
