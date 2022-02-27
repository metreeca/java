package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.time.LocalDate;

public final class DateShape extends ComparableShape<LocalDate> {

    public static Shape date() {
        return new DateShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private DateShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
