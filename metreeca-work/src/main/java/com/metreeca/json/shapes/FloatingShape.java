package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

public final class FloatingShape extends ComparableShape<Double> {

    public static Shape floating() {
        return new FloatingShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private FloatingShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
