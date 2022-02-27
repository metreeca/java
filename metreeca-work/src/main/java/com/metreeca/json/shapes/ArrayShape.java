package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Arrays.stream;

public final class ArrayShape extends ContainerShape {

    @SafeVarargs public static ArrayShape array(final Shape shape, final Consumer<ArrayShape>... options) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( options == null || stream(options).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null options");
        }

        final ArrayShape array=new ArrayShape(shape);

        stream(options).forEach(option -> option.accept(array));

        return array;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Shape shape;


    private ArrayShape(final Shape shape) {
        this.shape=shape;
    }


    public Shape shape() {
        return shape;
    }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
