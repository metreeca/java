package com.metreeca.json;

import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.shapes.ArrayShape.array;
import static com.metreeca.json.shapes.IdShape.id;
import static com.metreeca.json.shapes.ObjectShape.field;
import static com.metreeca.json.shapes.ObjectShape.object;
import static com.metreeca.json.shapes.OptionalShape.optional;

final class ShapeTest {

    private Shape concept() {
        return shape(() -> object(

                field("id", id()),

                field("narrower", optional(array(concept())))

        ));
    }

}