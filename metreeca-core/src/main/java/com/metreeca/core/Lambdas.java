/*
 * Copyright © 2013-2022 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.*;

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


    /**
     * Creates an unchecked runnable.
     *
     * @param runnable the checked runnable to be wrapped
     *
     * @return a runnable forwarding calls to {@code runnable} and wrapping any checked exception thrown in the process
     * within a suitable unchecked exception
     *
     * @throws NullPointerException if {@code runnable} is null
     */
    public static Runnable checked(final CheckedRunnable runnable) {

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
     * Creates an unchecked supplier.
     *
     * @param supplier the checked supplier to be wrapped
     * @param <V>      the type of the value supplied by {@code supplier}
     *
     * @return a supplier forwarding calls to {@code supplier} and wrapping any checked exception thrown in the process
     * within a suitable unchecked exception
     *
     * @throws NullPointerException if {@code supplier} is null
     */
    public static <V> Supplier<V> checked(final CheckedSupplier<? extends V> supplier) {

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
     * Creates an unchecked supplier.
     *
     * @param consumer the checked supplier to be wrapped
     * @param <V>      the type of the value consumed by {@code consumer}
     *
     * @return a consumer forwarding calls to {@code consumer} and wrapping any checked exception thrown in the process
     * within a suitable unchecked exception
     *
     * @throws NullPointerException if {@code consumer} is null
     */
    public static <V> Consumer<V> checked(final CheckedConsumer<? super V> consumer) {

        if ( consumer == null ) {
            throw new NullPointerException("null consumer");
        }

        return v -> {
            try {

                consumer.accept(v);

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
     * @param function the checked function to be wrapped
     * @param <V>      the type of the value processed by {@code function}
     * @param <R>      the type of the value retuned by {@code function}
     *
     * @return a function forwarding calls to {@code function} and wrapping any checked exception thrown in the process
     * within a suitable unchecked exception
     *
     * @throws NullPointerException if {@code function} is null
     */
    public static <V, R> Function<V, R> checked(final CheckedFunction<? super V, ? extends R> function) {

        if ( function == null ) {
            throw new NullPointerException("null function");
        }

        return v -> {
            try {

                return function.apply(v);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } catch ( final Exception e ) {

                throw new RuntimeException(e);
            }
        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Lambdas() { }


    //// Checked Lambdas ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * A {@link Runnable} throwing checked exceptions.
     */
    @FunctionalInterface public static interface CheckedRunnable {

        public void run() throws Exception;

    }

    /**
     * A {@link Supplier} throwing checked exceptions.
     *
     * @param <V> the type of the supplied value
     */
    @FunctionalInterface public static interface CheckedSupplier<V> {

        public V get() throws Exception;

    }

    /**
     * A {@link Consumer} throwing checked exceptions.
     *
     * @param <V> the type of the consumed value
     */
    @FunctionalInterface public static interface CheckedConsumer<V> {

        public void accept(final V value) throws Exception;

    }

    /**
     * A {@link Function} throwing checked exceptions.
     *
     * @param <V> the type of the value processed by {@code function}
     * @param <R> the type of the value retuned by {@code function}
     */
    @FunctionalInterface public static interface CheckedFunction<V, R> {

        public R apply(final V value) throws Exception;

    }

}
