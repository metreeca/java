package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

public final class LocalizedShape extends ContainerShape {

    public static Shape localized() {
        return new LocalizedShape();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private LocalizedShape() { }


    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
