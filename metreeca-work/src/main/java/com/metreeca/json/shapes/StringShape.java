package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Arrays.stream;

public final class StringShape extends ComparableShape<String> {

    @SafeVarargs public static Shape string(final Consumer<StringShape>... options) {

        if ( options == null || stream(options).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null options");
        }

        final StringShape shape=new StringShape();

        stream(options).forEach(option -> option.accept(shape));

        return shape;
    }


    public static Consumer<StringShape> minLength(final int min) {

        if ( min < 0 ) {
            throw new IllegalArgumentException(String.format("negative min length <%d>", min));
        }

        return shape -> shape.minLength=min;
    }

    public static Consumer<StringShape> maxLength(final int max) {

        if ( max < 0 ) {
            throw new IllegalArgumentException(String.format("negative max length <%d>", max));
        }

        return shape -> shape.maxLength=max;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Integer minLength;
    private Integer maxLength;


    private StringShape() { }


    public Optional<Integer> minLength() {
        return Optional.ofNullable(minLength);
    }

    public Optional<Integer> maxLength() {
        return Optional.ofNullable(maxLength);
    }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
