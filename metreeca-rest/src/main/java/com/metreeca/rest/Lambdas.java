/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.*;

/**
 * Lambda utilities.
 */
public final class Lambdas {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a guarded function.
     *
     * @param function the function to be guarded
     * @param <V>      the type of the {@code function} input value
     * @param <R>      the type of the {@code function} return value
     *
     * @return a function that returns the value produced by applying {@code function} to its input value, if it is
     * not null and no exception is thrown in the process, or {@code null}, otherwise
     *
     * @throws NullPointerException if {@code function} is null
     */
    public static <V, R> Function<V, R> guarded(final Function<V, R> function) {

        if ( function == null ) {
            throw new NullPointerException("null function");
        }

        return v -> {
            try {

                return v == null ? null : function.apply(v);

            } catch ( final RuntimeException e ) {

                return null;

            }
        };

    }


    /**
     * Creates an unchecked runnable.
     *
     * @param runnable the checked runnable to be unchecked
     *
     * @return a runnable wrapping checked exception thrown by {@code runnable} in a corresponding unchecked exception
     *
     * @throws NullPointerException if {@code runnable} is null
     */
    public static Runnable unchecked(final CheckedRunnable runnable) {

        if ( runnable == null ) {
            throw new NullPointerException("null runnable");
        }

        return () -> {
            try {

                runnable.run();

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }
        };
    }

    /**
     * Creates an unchecked consumer.
     *
     * @param consumer the checked consumer to be unchecked
     * @param <T>      the type of the consumer input value
     *
     * @return a consumer wrapping checked exception thrown by {@code consumer} in a corresponding unchecked exception
     *
     * @throws NullPointerException if {@code consumer} is null
     */
    public static <T> Consumer<T> unchecked(final CheckedConsumer<T> consumer) {

        if ( consumer == null ) {
            throw new NullPointerException("null consumer");
        }

        return t -> {
            try {

                consumer.accept(t);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }
        };
    }

    /**
     * Creates an unchecked supplier.
     *
     * @param supplier the checked supplier to be unchecked
     * @param <T>      the type of the supplier return value
     *
     * @return a supplier wrapping checked exception thrown by {@code supplier} in a corresponding unchecked exception
     *
     * @throws NullPointerException if {@code supplier} is null
     */
    public static <T> Supplier<T> unchecked(final CheckedSupplier<T> supplier) {

        if ( supplier == null ) {
            throw new NullPointerException("null supplier");
        }

        return () -> {
            try {

                return supplier.get();

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }
        };
    }

    /**
     * Creates an unchecked function.
     *
     * @param function the checked function to be unchecked
     * @param <T>      the type of the function input value
     * @param <R>      the type of the function return value
     *
     * @return a function wrapping checked exception thrown by {@code function} in a corresponding unchecked exception
     *
     * @throws NullPointerException if {@code function} is null
     */
    public static <T, R> Function<T, R> unchecked(final CheckedFunction<T, R> function) {

        if ( function == null ) {
            throw new NullPointerException("null function");
        }

        return t -> {
            try {

                return function.apply(t);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }
        };
    }

    /**
     * Creates an unchecked bifunction.
     *
     * @param bifunction the checked bifunction to be unchecked
     * @param <T>        the type of the bifunction first input value
     * @param <U>        the type of the bifunction second input value
     * @param <R>        the type of the bifunction return value
     *
     * @return a bifunction wrapping checked exception thrown by {@code bifunction} in a corresponding unchecked
     * exception
     *
     * @throws NullPointerException if {@code bifunction} is null
     */
    public static <T, U, R> BiFunction<T, U, R> unchecked(final CheckedBiFunction<T, U, R> bifunction) {

        if ( bifunction == null ) {
            throw new NullPointerException("null bifunction");
        }

        return (t, u) -> {
            try {

                return bifunction.apply(t, u);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);

            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Lambdas() {} // utility


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Runnable throwing checked exceptions.
     */
    @FunctionalInterface public static interface CheckedRunnable {

        public void run() throws Exception;

    }

    /**
     * Consumer throwing checked exceptions.
     *
     * @param <T> the type of value accepted by this consumer
     */
    @FunctionalInterface public static interface CheckedConsumer<T> {

        public void accept(final T t) throws Exception;

    }

    /**
     * Supplier throwing checked exceptions.
     *
     * @param <T> the type of results supplied by this supplier
     */
    @FunctionalInterface public static interface CheckedSupplier<T> {

        public T get() throws Exception;

    }

    /**
     * Function throwing checked exceptions.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface public static interface CheckedFunction<T, R> {

        public R apply(final T t) throws Exception;

    }

    /**
     * Function throwing checked exceptions.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface public static interface CheckedBiFunction<T, U, R> {

        public R apply(final T t, final U u) throws Exception;

    }

}
