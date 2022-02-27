package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

public final class OptionalShape extends Shape {

    public static Shape optional(final Shape shape) {
        return new OptionalShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private OptionalShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
