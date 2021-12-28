/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.json;

import com.metreeca.json.shifts.*;

import java.util.function.Function;

/**
 * Focus shift operator.
 *
 * <p>Describes how to navigate from a value to a set of linked values.</p>
 */
public abstract class Shift {

	public abstract <V> V map(final Shift.Probe<V> probe);

	public final <V> V map(final Function<Shift, V> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Shift probe.
	 *
	 * <p>Generates a result by probing shifts.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public abstract static class Probe<V> implements Function<Shift, V> {

		@Override public final V apply(final Shift shift) {
			return shift == null ? null : shift.map(this);
		}


		//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Step step) { return probe((Shift)step); }

		public V probe(final Seq seq) { return probe((Shift)seq); }

		public V probe(final Alt alt) { return probe((Shift)alt); }


		//// Fallback //////////////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Probes a generic shift.
		 *
		 * @param shift the generic shift to be probed
		 *
		 * @return the result generated by probing {@code shift}; by default {@code null}
		 */
		public V probe(final Shift shift) { return null; }

	}

}
