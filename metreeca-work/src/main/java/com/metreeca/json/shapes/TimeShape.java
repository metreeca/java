package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.time.LocalTime;

public final class TimeShape extends ComparableShape<LocalTime> {

    public static Shape time() {
        return new TimeShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private TimeShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
