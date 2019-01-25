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

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


/**
 * Ordering criterion.
 */
public final class Order {

	public static Order increasing(final Shift... shifts) {
		return new Order(asList(shifts), false);
	}

	public static Order increasing(final List<Shift> shifts) {
		return new Order(shifts, false);
	}


	public static Order decreasing(final Shift... shifts) {
		return new Order(asList(shifts), true);
	}

	public static Order decreasing(final List<Shift> shifts) {
		return new Order(shifts, true);
	}


	private final List<Shift> path;
	private final boolean inverse;


	private Order(final List<Shift> path, final boolean inverse) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( path.contains(null) ) {
			throw new IllegalArgumentException("illegal path element");
		}

		this.path=new ArrayList<>(path);
		this.inverse=inverse;
	}


	public List<Shift> getPath() {
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

		for (final Shift shift : path) {

			if ( builder.length() > 0 ) {
				builder.append('/');
			}

			builder.append(shift);
		}

		return builder.insert(0, inverse ? "-" : "+").toString();
	}

}
