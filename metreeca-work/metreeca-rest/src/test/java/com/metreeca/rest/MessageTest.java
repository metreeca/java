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

package com.metreeca.rest;

import com.metreeca.rest.codecs.Text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.rest.MessageAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertSame;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;


final class MessageTest {

    private Message<?> message() {
        return new Request();
    }


    @Nested final class HeadersTest {

        @Test void testHeadersIgnoreHeaderCase() {
            assertThat(message()
                    .header("TEST-header", "value")
            )
                    .hasHeader("test-header", "value");
        }

        @Test void testHeadersIgnoreEmptyHeaders() {
            assertThat(message()
                    .headers("test-header", emptySet())
            )
                    .doesNotHaveHeader("test-header");
        }

        @Test void testHeadersOverwritesValues() {
            assertThat(message()
                    .header("test-header", "one")
                    .header("test-header", "two")
            )
                    .hasHeader("test-header", "two");
        }

    }

    @Nested final class BodyTest {

        @Test void testBodyCaching() {

            final Message<?> message=message()
                    .header("Content-Type", Text.MIME)
                    .input(() -> new ByteArrayInputStream("test".getBytes(UTF_8)));

            assertSame(message.body(new Text()), message.body(new Text()));

        }

    }

}
