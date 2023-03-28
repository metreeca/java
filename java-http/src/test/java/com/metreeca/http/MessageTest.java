/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.http;

import com.metreeca.http.formats.Text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.MessageAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;


final class MessageTest {

    private Message<?> message() {
        return new Request();
    }


    @Nested final class MIMETest {

        @Test void testParseStrings() {

            assertThat(mimes(""))
                    .as("empty")
                    .isEmpty();

            assertThat(mimes("text/turtle"))
                    .as("single")
                    .isEqualTo(singletonList("text/turtle"));

            assertThat(mimes("text/turtle, text/plain"))
                    .as("multiple")
                    .isEqualTo(asList("text/turtle", "text/plain"));

            assertThat(mimes("*/*"))
                    .as("wildcard")
                    .isEqualTo(singletonList("*/*"));

            assertThat(mimes("text/*"))
                    .as("type wildcard")
                    .isEqualTo(singletonList("text/*"));

        }

        @Test void testParseLeniently() {

            assertThat(mimes("text/Plain"))
                    .as("normalize case")
                    .isEqualTo(singletonList("text/plain"));

            assertThat(mimes(" text/plain ; q = 0.3"))
                    .as("ignores spaces")
                    .isEqualTo(singletonList("text/plain"));

            assertThat(mimes("text/turtle, text/plain\ttext/csv"))
                    .as("lenient separators")
                    .isEqualTo(asList("text/turtle", "text/plain", "text/csv"));

        }

        @Test void testSortOnQuality() {

            assertThat(mimes("text/turtle;q=0.1, text/plain;q=0.2"))
                    .as("sorted")
                    .isEqualTo(asList("text/plain", "text/turtle"));

            assertThat(mimes("text/turtle;q=0.1, text/plain"))
                    .as("sorted with default values")
                    .isEqualTo(asList("text/plain", "text/turtle"));

            assertThat(mimes("text/turtle;q=x, text/plain"))
                    .as("sorted with corrupt values")
                    .isEqualTo(asList("text/plain", "text/turtle"));

        }

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
