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

package com.metreeca.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

final class XtreamTest {

    @Nested final class Loop {

        @Test void testIncludeStartingPoint() {
            assertThat(Xtream.of(0).loop(n -> n < 2 ? Xtream.of(n+1) : Xtream.empty()))
                    .containsExactly(0, 1, 2);
        }

    }

    @Nested final class Scan {

        @Test void testIncludeStartingPoint() {
            assertThat(Xtream.of(0).scan(n -> n < 3
                    ? Xtream.of(entry(Stream.of(n+1), Stream.of(String.valueOf(n))))
                    : Xtream.empty()
            ))
                    .containsExactly("0", "1", "2");
        }

    }

}