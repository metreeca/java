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

package com.metreeca.rest._actions;


import com.metreeca.rest.Context;
import com.metreeca.rest.Feed;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;


final class TextTest {

	private void exec(final Runnable task) {
		new Context().exec(task).clear();
	}


	@Test void test() {
		exec(() -> assertThat

				(Feed
						.of("test")

						.flatMap(new Text<String>("{base}:{x}{y}")
								.values("base", Stream::of)
								.values("x", string -> Stream.of("1", "2"))
								.values("y", string -> Stream.of("2", "3"))
						)

						.collect(toList())
				)

				.containsExactlyInAnyOrder(
						"test:12",
						"test:13",
						"test:22",
						"test:23"
				)

		);
	}

}
