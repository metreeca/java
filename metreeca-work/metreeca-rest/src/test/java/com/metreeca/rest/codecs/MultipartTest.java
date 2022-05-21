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

package com.metreeca.rest.codecs;

import com.metreeca.rest.*;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import static com.metreeca.rest.MessageAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;


final class MultipartTest {

    @Nested final class Input {

        private static final String type="multipart/form-data; boundary=\"boundary\"";

        private Supplier<InputStream> content() {
            return content("\n"
                    +"preamble\n"
                    +"\n"
                    +"--boundary\n"
                    +"Content-Disposition: form-data; name=\"main\"\n"
                    +"Content-Type: text/turtle\n"
                    +"\n"
                    +"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    +"\n"
                    +"<> rdf:value rdf.nil.\n"
                    +"\n"
                    +"--boundary\t\t\n"
                    +"Content-Disposition: form-data; name=\"file\"; filename=\"example.txt\"\n"
                    +"Content-Type: text/plain\n"
                    +"\n"
                    +"text\n"
                    +"--boundary--\n"
                    +"\n"
                    +"\n"
                    +"epilogue\n"
            );
        }

        private Supplier<InputStream> content(final String content) {
            return () -> new ByteArrayInputStream(
                    content.replace("\n", "\r\n").getBytes(UTF_8)
            );
        }


        @Test void testParseMultipartBodies() {
            assertThat(new Request()

                    .header("Content-Type", type)

                    .input(content())

                    .body(new Multipart(250, 1000))

            ).satisfies(parts -> {

                assertThat(parts.size())
                        .isEqualTo(2);

                assertThat(parts.keySet())
                        .containsExactly("main", "file");

                assertThat(parts.get("file"))
                        .as("part available by name")
                        .hasItem("file:example.txt")
                        .hasHeader("Content-Disposition")
                        .hasBody(new Text(), text -> assertThat(text)
                                .isEqualTo("text")
                        );

            });
        }

        @Test void testCacheIdempotentResults() {

            final Request request=new Request()
                    .header("Content-Type", type)
                    .input(content());

            final Map<String, Message<?>> one=request
                    .body(new Multipart(250, 1000));

            final Map<String, Message<?>> two=request
                    .body(new Multipart());

            assertThat(one)
                    .as("idempotent")
                    .isSameAs(two);
        }


        @Test void testIgnoreUnrelatedContentTypes() {

            assertThat(new Request()

                    .header("Content-Type", "plain/test")
                    .input(content())

            ).doesNotHaveBody(new Multipart());
        }

        @Test void testRejectMalformedPayloads() {
            assertThatExceptionOfType(CodecException.class).isThrownBy(() -> new Request()

                    .header("Content-Type", "multipart/data")
                    .input(content())

                    .body(new Multipart())

            );
        }

    }

    @Nested final class Output {

        @Test void testGenerateRandomBoundary() {
            new Request().reply()

                    .map(response -> response.status(Response.OK)

                            .body(new Multipart(), Map.ofEntries(

                                    entry("one", response.part("one").body(new Text(), "one")),
                                    entry("two", response.part("two").body(new Text(), "two"))

                            ))

                    )

                    .map(response -> MessageAssert.assertThat(response)

                            .has(new Condition<>(
                                    r -> r.header("Content-Type").filter(s -> s.contains("; boundary=")).isPresent(),
                                    "multipart boundary set"
                            ))

                    );
        }

        @Test void testPreserveCustomBoundary() {
            assertThat(new Request().reply()

                    .header("Content-Type", "multipart/form-data; boundary=1234567890")
                    .body(new Multipart(), Map.of())

            )

                    .hasHeader("Content-Type", "multipart/form-data; boundary=1234567890")

                    .hasTextOutput(text -> assertThat(text)
                            .contains("--1234567890--")
                    );

        }

    }

}
