package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;

public final class ObjectShape extends Shape {

    private static final Pattern NamePattern=compile("\\w+");
    private static final Pattern NamedIRIPattern=compile("[/#:]("+NamePattern+")(?:/|#|#_|#id|#this)?$");


    public static Shape object(final Field... fields) {

        if ( fields == null || stream(fields).anyMatch(java.util.Objects::isNull) ) {
            throw new NullPointerException("null fields");
        }

        return new ObjectShape(stream(fields).collect(toMap(

                Field::identify,
                identity(),

                (x, y) -> {

                    throw new IllegalArgumentException(format(
                            "multiple fields for label <%s>", x.identify()
                    ));

                },

                LinkedHashMap::new

        )));
    }


    public static Field field(final String id, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id"); // !!! well-formedness
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return field(id, "", shape);
    }

    public static Field field(final String id, final String label, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        try {

            return field(NamePattern.matcher(id).matches()
                    ? new URI("app", "/terms", id)
                    : new URI(id), label, shape);

        } catch ( final URISyntaxException e ) {

            throw new IllegalArgumentException(format("malformed id <%s>", id), e);

        }
    }

    public static Field field(final URI id, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id"); // !!! well-formedness
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return field(id, "", shape);
    }

    public static Field field(final URI id, final String label, final Shape shape) {

        if ( id == null ) {
            throw new NullPointerException("null id");
        }

        if ( !id.isAbsolute() ) {
            throw new IllegalArgumentException(format("relative id <%s>", id));
        }

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return new Field(false, id, label, shape);
    }


    public static Field reverse(final Field field) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        return new Field(true, field.iri, field.label, field.shape);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, Field> fields;


    private ObjectShape(final Map<String, Field> fields) {
        this.fields=unmodifiableMap(fields);
    }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, Field> fields() {
        return fields;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Field {

        private final boolean reverse;

        private final URI iri;
        private final String label;
        private final Shape shape;


        private Field(final boolean reverse, final URI iri, final String label, final Shape shape) {

            this.reverse=reverse;

            this.iri=iri;
            this.label=label;
            this.shape=shape;

        }


        public boolean reverse() {
            return reverse;
        }


        public URI iri() {
            return iri;
        }

        public String label() {
            return label;
        }

        public Shape shape() {
            return shape;
        }


        private String identify() {
            return Optional.of(label).filter(not(String::isEmpty))

                    .or(() -> Optional
                            .of(NamedIRIPattern.matcher(iri.toString()))
                            .filter(Matcher::find)
                            .map(matcher -> matcher.group(1))
                            .map(name -> reverse ? name+"Of" : name)
                    )

                    .orElseGet(iri::toString);
        }

    }

}
