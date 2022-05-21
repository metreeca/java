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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;
import com.metreeca.rest.codecs.Text;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;
import java.util.function.Supplier;

import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.MultipartFormat.multipart;
import static com.metreeca.rest.formats.OutputFormat.output;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.function.Function.identity;


final class MultipartFormatTest {

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
            new Request()

                    .header("Content-Type", type)

                    .input(content())

                    .body(multipart(250, 1000))

                    .fold(

                            error -> Assertions.fail("unexpected failure {"+error+"}"), parts -> {

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

                                return this;

                            }

                    );
        }

        @Test void testCacheIdempotentResults() {

            final Request request=new Request()
                    .header("Content-Type", type)
                    .body(InputFormat.input(), content());

            final Map<String, Message<?>> one=request
                    .body(multipart(250, 1000))
                    .fold(e -> Assertions.fail("missing multipart body"), identity());

            final Map<String, Message<?>> two=request
                    .body(multipart())
                    .fold(e -> Assertions.fail("missing multipart body"), identity());

            Assertions.assertThat(one)
                    .as("idempotent")
                    .isSameAs(two);
        }


        @Test void testIgnoreUnrelatedContentTypes() {
            new Request()

                    .header("Content-Type", "plain/test")
                    .body(InputFormat.input(), content())

                    .body(multipart())

                    .fold(
                            Assertions::assertThat, parts -> Assertions.fail("unexpected multipart body")
                    );
        }

        @Test void testRejectMalformedPayloads() {
            new Request()

                    .header("Content-Type", "multipart/data")
                    .body(InputFormat.input(), content())

                    .body(multipart())

                    .fold(

                            error -> {

                                new Request().reply().map(error).map(response -> assertThat(response)
                                        .as("missing boundary parameter")
                                        .hasStatus(Response.BadRequest)
                                );


                                return this;
                            }, parts -> {
                                Assertions.fail("unexpected multipart body");


                                return this;

                            }

                    );
        }

    }

    @Nested final class Output {

        @Test void testGenerateRandomBoundary() {
            new Request().reply()

                    .map(response -> response.status(Response.OK)

                            .body(multipart(), Map.ofEntries(

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
            new Request().reply().map(response1 -> response1

                            .status(Response.OK)
                            .header("Content-Type", "multipart/form-data; boundary=1234567890")
                            .body(multipart(), Map.of()))

                    .map(response -> MessageAssert.assertThat(response)

                            .hasHeader("Content-Type", "multipart/form-data; boundary=1234567890")

                            .hasBody(output(), target -> {

                                final ByteArrayOutputStream output=new ByteArrayOutputStream();

                                target.accept(output);

                                assertThat(output.toString(UTF_8))
                                        .contains("--1234567890--");

                            })

                    );
        }

    }

}
