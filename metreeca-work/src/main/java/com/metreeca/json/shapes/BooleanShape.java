package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

public final class BooleanShape extends ComparableShape<Boolean> {

    public static Shape optional() {
        return new BooleanShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private BooleanShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
