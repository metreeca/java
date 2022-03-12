package com.metreeca.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Functional utilities.
 */
public final class Lambdas {

    /**
     * Converts a consumer into a function.
     *
     * @param consumer the consumer to be converted
     * @param <V>      the type of the value accepted by {@code consumer}
     * @param <R>      the type returned by the generated function
     *
     * @return a function forwarding its input value to {@code supplier} and returning a null value
     *
     * @throws NullPointerException if consumer is null
     */
    public static <V, R> Function<V, R> task(final Consumer<V> consumer) {

        if ( consumer == null ) {
            throw new NullPointerException("null consumer");
        }

        return value -> {

            consumer.accept(value);

            return null;

        };
    }

    /**
     * Creates a guarded function.
     *
     * @param function the function to be wrapped
     * @param <V>      the type of the {@code function} input value
     * @param <R>      the type of the {@code function} return value
     *
     * @return a function returning the value produced by applying {@code function} to its input value, if the input
     * value is not null and no exception is thrown in the process, or {@code null}, otherwise
     *
     * @throws NullPointerException if {@code function} is null
     */
    public static <V, R> Function<V, R> guarded(final Function<? super V, ? extends R> function) {

        if ( function == null ) {
            throw new NullPointerException("null function");
        }

        return value -> {
            try {

                return value == null ? null : function.apply(value);

            } catch ( final RuntimeException e ) {

                return null;

            }
        };

    }

    /**
     * Creates an auto-closing function.
     *
     * @param function the function to be wrapped
     * @param <V>      the type of the {@code function} input value
     * @param <R>      the type of the {@code function} return value
     *
     * @return a function returning the value produced by applying {@code function} to its autocloseable input value and
     * closing it after processing
     *
     * @throws NullPointerException if {@code function} is null
     */
    public static <V extends AutoCloseable, R> Function<V, R> closing(final Function<? super V, ? extends R> function) {

        if ( function == null ) {
            throw new NullPointerException("null function");
        }

        return value -> {

            try ( final V c=value ) {

                return function.apply(c);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Lambdas() { }

}
