/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tree.shapes;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Or.or;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toSet;


final class AnyTest {

	@Test void testInspectUniversal() {

		final Any any=any(1, 2, 3);

		assertThat(any(any))
				.contains(any.getValues());
	}

	@Test void testInspectDisjunction() {

		final Any x=any(1, 2, 3);
		final Any y=any(2, 3, 4);

		assertThat(any(or(x, y)))
				.as("all defined")
				.contains(Stream.concat(x.getValues().stream(), y.getValues().stream()).collect(toSet()));

		assertThat(any(or(x, and())))
				.as("some defined")
				.contains(x.getValues());

		assertThat(any(or(and(), and())))
				.as("none defined")
				.isEmpty();

	}

	@Test void testInspectOtherShape() {
		assertThat(any(and()))
				.isEmpty();
	}

}
