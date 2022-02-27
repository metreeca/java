package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.time.LocalDateTime;

public final class DatetimeShape extends ComparableShape<LocalDateTime> {

    public static Shape datetime() {
        return new DatetimeShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private DatetimeShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
