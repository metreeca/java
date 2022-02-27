package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.String.format;

public abstract class ContainerShape extends Shape {

    public static <S extends ContainerShape> Consumer<S> minCount(final int minCount) {

        if ( minCount < 0 ) {
            throw new IllegalArgumentException(format("negative min count <%d>", minCount));
        }

        return shape -> ((ContainerShape)shape).minCount=minCount;
    }

    public static Consumer<ArrayShape> maxCount(final int maxCount) {

        if ( maxCount < 0 ) {
            throw new IllegalArgumentException(format("negative max count <%d>", maxCount));
        }

        return shape -> ((ContainerShape)shape).maxCount=maxCount;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Integer minCount;
    private Integer maxCount;


    ContainerShape() { }


    public Optional<Integer> minCount() {
        return Optional.ofNullable(minCount);
    }

    public Optional<Integer> maxCount() {
        return Optional.ofNullable(maxCount);
    }


}
