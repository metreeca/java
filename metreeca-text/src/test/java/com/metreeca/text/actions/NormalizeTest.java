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

package com.metreeca.text.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class NormalizeTest {

	@Test void testSpace() {

		final String text="  leading \t\u00A0trailing\n\r";

		assertThat(new Normalize().space(true).apply(text)).isEqualTo("leading trailing");
		assertThat(new Normalize().space(false).apply(text)).isEqualTo(text);
	}

	@Test void testMarks() {

		final String text="èé";

		assertThat(new Normalize().marks(true).apply(text)).isEqualTo("ee");
		assertThat(new Normalize().marks(false).apply(text)).isEqualTo(text);
	}

	@Test void testSmart() {

		final String text="‘’“”‹› ";

		assertThat(new Normalize().smart(true).apply(text)).isEqualTo("''\"\"<> ");
		assertThat(new Normalize().smart(false).apply(text)).isEqualTo(text);
	}

	@Test void testLower() {

		final String text="AaÈè";

		assertThat(new Normalize().lower(true).apply(text)).isEqualTo("aaèè");
		assertThat(new Normalize().lower(false).apply(text)).isEqualTo(text);
	}

}