/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form;

import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shifts.Step;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Shape-driven linked data query.
 */
public interface Query {

	public static Order increasing(final Step... steps) {
		return new Order(asList(steps), false);
	}

	public static Order decreasing(final Step... steps) {
		return new Order(asList(steps), true);
	}


	public <V> V map(final Probe<V> probe);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Query probe.
	 *
	 * <p>Generates a result by probing queries.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public static interface Probe<V> {

		public V probe(final Edges edges);

		public V probe(final Stats stats);

		public V probe(final Items items);

	}


	public static final class Order {

		private final List<Step> path;
		private final boolean inverse;


		public Order(final List<Step> path, final boolean inverse) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			if ( path.contains(null) ) {
				throw new IllegalArgumentException("illegal path element");
			}

			this.path=new ArrayList<>(path);
			this.inverse=inverse;
		}


		public List<Step> getPath() {
			return unmodifiableList(path);
		}

		public boolean isInverse() {
			return inverse;
		}


		@Override public boolean equals(final Object object) {
			return this == object || object instanceof Order
					&& path.equals(((Order)object).path)
					&& inverse == ((Order)object).inverse;
		}

		@Override public int hashCode() {
			return path.hashCode()^Boolean.hashCode(inverse);
		}

		@Override public String toString() {

			final StringBuilder builder=new StringBuilder(20*path.size());

			for (final Step step : path) {

				if ( builder.length() > 0 ) {
					builder.append('/');
				}

				builder.append(step.format());
			}

			return builder.insert(0, inverse ? "-" : "+").toString();
		}

	}

}
