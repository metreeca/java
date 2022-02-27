package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Arrays.stream;

public abstract class ComparableShape<V extends Comparable<? super V>> extends Shape {

    public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> minInclusive(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null min value");
        }

        return shape -> {
            ((ComparableShape<V>)shape).minInclusive=value;
            ((ComparableShape<V>)shape).minExclusive=null;
        };

    }

    public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> maxInclusive(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null min value");
        }

        return shape -> {
            ((ComparableShape<V>)shape).maxInclusive=value;
            ((ComparableShape<V>)shape).maxExclusive=null;
        };

    }

    public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> minExclusive(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null min value");
        }

        return shape -> {
            ((ComparableShape<V>)shape).minInclusive=null;
            ((ComparableShape<V>)shape).minExclusive=value;
        };

    }

    public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> maxExclusive(final V value) {

        if ( value == null ) {
            throw new NullPointerException("null min value");
        }

        return shape -> {
            ((ComparableShape<V>)shape).maxInclusive=null;
            ((ComparableShape<V>)shape).maxExclusive=value;
        };

    }


    @SafeVarargs public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> range(final V... values) {

        if ( values == null || stream(values).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null range values");
        }

        return shape -> ((ComparableShape<V>)shape).range=Set.of(values);

    }

    public static <V extends Comparable<V>, S extends ComparableShape<V>> Consumer<S> range(final Collection<V> values) {

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null range values");
        }

        return shape -> ((ComparableShape<V>)shape).range=Set.copyOf(values);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private V minInclusive;
    private V maxInclusive;

    private V minExclusive;
    private V maxExclusive;

    private Set<V> range;


    ComparableShape() { }


    public Optional<V> minInclusive() {
        return Optional.ofNullable(minInclusive);
    }

    public Optional<V> maxInclusive() {
        return Optional.ofNullable(maxInclusive);
    }


    public Optional<V> minExclusive() {
        return Optional.ofNullable(minExclusive);
    }

    public Optional<V> maxExclusive() {
        return Optional.ofNullable(maxExclusive);
    }


    public Optional<Set<V>> range() {
        return Optional.ofNullable(range);
    }

}
