package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.math.BigInteger;

public final class IntegerShape extends ComparableShape<BigInteger> {

    public static Shape integer() {
        return new IntegerShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private IntegerShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
