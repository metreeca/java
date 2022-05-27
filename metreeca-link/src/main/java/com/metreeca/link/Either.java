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

package com.metreeca.link;

import java.util.function.Function;


/**
 * Alternative values.
 *
 * <p>Wraps a pair of mutually exclusive values.</p>
 *
 * @param <R> the type of the left alternative value
 * @param <L> the type of the right alternative value
 */
public abstract class Either<L, R> {

    /**
     * Creates a left alternative value.
     *
     * @param value the left value to be wrapped
     * @param <L>   the type of the right alternative value
     * @param <R>   the type of the left alternative value
     *
     * @return an alternative values pair wrapping the supplied left {@code value}
     *
     * @throws NullPointerException if {@code value} is null
     */
    public static <L, R> Either<L, R> Left(final L value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Either<>() {

            @Override public <V> V fold(
                    final Function<? super L, ? extends V> left,
                    final Function<? super R, ? extends V> right
            ) {

                if ( left == null ) {
                    throw new NullPointerException("null failure mapper");
                }

                if ( right == null ) {
                    throw new NullPointerException("null success mapper");
                }

                return left.apply(value);
            }

            @Override public String toString() {
                return String.format("Left(%s)", value);
            }

        };
    }

    /**
     * Creates a right alternative value.
     *
     * @param value the right value to be wrapped
     * @param <L>   the type of the right alternative value
     * @param <R>   the type of the left alternative value
     *
     * @return an alternative values pair wrapping the supplied right {@code value}
     *
     * @throws NullPointerException if {@code value} is null
     */
    public static <L, R> Either<L, R> Right(final R value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return new Either<>() {

            @Override public <V> V fold(
                    final Function<? super L, ? extends V> left,
                    final Function<? super R, ? extends V> right
            ) {

                if ( right == null ) {
                    throw new NullPointerException("null success mapper");
                }

                if ( left == null ) {
                    throw new NullPointerException("null failure mapper");
                }

                return right.apply(value);
            }

            @Override public String toString() {
                return String.format("Right(%s)", value);
            }

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Folds alternative values.
     *
     * @param <V>   the type of the folded value
     * @param left  a function mapping from the left alternative value to the folded value
     * @param right a function mapping from the right alternative value to the folded value
     *
     * @return the folded value, generated as required either by {@code left} or {@code right}
     *
     * @throws NullPointerException if either {@code right} or {@code left} is null or returns a null value
     */
    public abstract <V> V fold(
            final Function<? super L, ? extends V> left,
            final Function<? super R, ? extends V> right
    );

}