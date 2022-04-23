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

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.lang.Math.max;

public final class Model<T> {

	public static <T> Model<T> dot() {
		return new Model<>(values -> values.isEmpty() ? -1 : 1);
	}

	public static <T> Model<T> test(final Predicate<T> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return new Model<>(values -> !values.isEmpty() && predicate.test(values.get(0)) ? 1 : -1);
	}


	@SafeVarargs public static <T> Model<T> seq(final Model<T>... models) {

		if ( models == null || Arrays.stream(models).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null models");
		}

		return new Model<>(values -> {

			int i=0;

			for (final Model<T> model : models) {

				final int j=model.find(values.subList(i, values.size()));

				if ( j >= 0 ) {

					i+=j;

				} else {

					return -1;

				}

			}

			return i;

		});
	}

	@SafeVarargs public static <T> Model<T> alt(final Model<T>... models) {

		if ( models == null || Arrays.stream(models).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null models");
		}

		return new Model<>(values -> Arrays
				.stream(models)
				.mapToInt(model -> model.find(values))
				.max()
				.orElse(-1)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final ToIntFunction<? super List<T>> specs;


	private Model(final ToIntFunction<? super List<T>> specs) { this.specs=specs; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int find(final List<T> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return specs.applyAsInt(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Model<T> opt() {
		return new Model<>(values -> max(0, find(values)));
	}

	public Model<T> star() {
		return new Model<>(values -> {

			final int n=values.size();

			int i=0;

			while ( i < n ) {

				final int l=find(values.subList(i, n));

				if ( l >= 0 ) {

					i+=l;

				} else {

					return i;

				}

			}

			return i;

		});
	}

	public Model<T> plus() {
		return new Model<>(values -> {

			final int n=values.size();

			int i=0;

			while ( i < n ) {

				final int l=find(values.subList(i, n));

				if ( l >= 0 ) {

					i+=l;

				} else {

					return i > 0 ? i : -1;

				}

			}

			return i > 0 ? i : -1;

		});
	}


	public Model<T> at() {
		return new Model<>(values -> find(values) >= 0 ? 0 : -1);
	}

	public Model<T> not() {
		return new Model<>(values -> find(values) >= 0 ? -1 : 0);
	}


	public Model<T> and(final Model<T> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return seq(this, model);
	}

	public Model<T> or(final Model<T> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return alt(this, model);
	}


	public Model<T> all(final Predicate<T> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return new Model<>(values -> {

			final int n=find(values);

			return n > 0 && values.subList(0, n).stream().allMatch(predicate) ? n : -1;

		});
	}

	public Model<T> some(final Predicate<T> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return new Model<>(values -> {

			final int n=find(values);

			return n > 0 && values.subList(0, n).stream().anyMatch(predicate) ? n : -1;

		});
	}

	public Model<T> none(final Predicate<T> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return new Model<>(values -> {

			final int n=find(values);

			return n > 0 && values.subList(0, n).stream().noneMatch(predicate) ? n : -1;

		});
	}

}
