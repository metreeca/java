/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.json.shapes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Or.or;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class LangTest {

	@Test void testRejectEmptyTags() {
		assertThatIllegalArgumentException().isThrownBy(() -> lang(""));
	}

	@Nested final class Optimization {

		@Test void testIgnoreEmptyValueSet() {
			assertThat(lang()).isEqualTo(or());
		}

		@Test void testCollapseDuplicates() {
			assertThat(lang("en", "it", "en")).isEqualTo(lang("en", "it"));
		}

	}

}