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

package com.metreeca.text;

public final class Match<S, T> {

	private final S source;
	private final T target;

	private final double weight;


	public Match(final S source, final T target) {
		this(source, target, 0);
	}

	public Match(final S source, final T target, final double weight) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		this.source=source;
		this.target=target;
		this.weight=weight;
	}


	public S source() {
		return source;
	}

	public <V> Match<V, T> source(final V source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		return new Match<>(source, target, weight);
	}


	public T target() {
		return target;
	}

	public <V> Match<S, V> target(final V target) {

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		return new Match<>(source, target, weight);
	}


	public double weight() {
		return weight;
	}

	public Match<S, T> weight(final double weight) {
		return new Match<>(source, target, weight);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Match
				&& source.equals(((Match<?, ?>)object).source)
				&& target.equals(((Match<?, ?>)object).target)
				&& weight-((Match<?, ?>)object).weight == 0;
	}

	@Override public int hashCode() {
		return source.hashCode()^target.hashCode()^Double.hashCode(weight);
	}

	@Override public String toString() {
		return String.format("%3.3f %s › %s", weight, source, target);
	}

}
