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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class RangeTest {

	private final Range reference=_range(3, 7);
	private final Range intersecting=_range(5, 8);
	private final Range contiguous=_range(7, 10);
	private final Range disjoint=_range(8, 10);

	private Range _range(final int lower, final int upper) {
		return new Range() {

			@Override public int lower() { return lower; }

			@Override public int upper() { return upper; }

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testBorders() {

		assertThat(reference.borders(reference)).isFalse();
		assertThat(reference.borders(intersecting)).isFalse();
		assertThat(reference.borders(disjoint)).isFalse();

		assertThat(reference.borders(contiguous)).isTrue();
		assertThat(contiguous.borders(reference)).isTrue();

	}

	@Test void testIntersects() {

		assertThat(reference.intersects(reference)).isTrue();

		assertThat(reference.intersects(intersecting)).isTrue();
		assertThat(intersecting.intersects(reference)).isTrue();

		assertThat(reference.intersects(contiguous)).isFalse();
		assertThat(contiguous.intersects(reference)).isFalse();

		assertThat(reference.intersects(disjoint)).isFalse();
		assertThat(disjoint.intersects(reference)).isFalse();

	}


}