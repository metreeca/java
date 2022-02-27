package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.math.BigDecimal;

public final class DecimalShape extends ComparableShape<BigDecimal> {

    public static Shape decimal() {
        return new DecimalShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private DecimalShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
