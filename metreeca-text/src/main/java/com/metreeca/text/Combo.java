/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.text;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

public abstract class Combo<V, R> implements Function<V, R> {

	@SafeVarargs public static <V> Function<V, V> chain(final Function<V, V>... steps) {

		if ( steps == null ) {
			throw new NullPointerException("null steps");
		}

		return chain(asList(steps));
	}

	public static <V> Function<V, V> chain(final Iterable<Function<V, V>> steps) {

		if ( steps == null ) {
			throw new NullPointerException("null steps");
		}

		Function<V, V> combo=identity();

		for (final Function<V, V> step : steps) {

			if ( step == null ) {
				throw new NullPointerException("null steps");
			}

			combo=combo.andThen(step);

		}

		return combo;
	}


	@SafeVarargs public static Function<Token, Chunk> nest(final Function<Token, Chunk>... steps) {

		if ( steps == null ) {
			throw new NullPointerException("null steps");
		}

		return nest(asList(steps));
	}

	public static Function<Token, Chunk> nest(final Iterable<Function<Token, Chunk>> steps) {

		if ( steps == null ) {
			throw new NullPointerException("null steps");
		}

		Function<Token, Stream<Token>> combo=Stream::of;

		for (final Function<Token, Chunk> step : steps) {

			if ( step == null ) {
				throw new NullPointerException("null steps");
			}

			combo=combo.andThen(tokens -> tokens.map(step).map(Chunk::tokens).flatMap(Collection::stream));

		}

		return combo.andThen(tokens -> new Chunk(tokens.collect(toList())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Function<V, R> delegate;


	protected void delegate(final Function<V, R> delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public R apply(final V v) {
		return delegate == null ? null : delegate.apply(v);
	}

}
