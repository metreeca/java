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

package com.metreeca.http.handlers;

import com.metreeca.core.Locator;
import com.metreeca.http.Handler;
import com.metreeca.http.Request;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

import static com.metreeca.http.Request.POST;
import static com.metreeca.http.Response.InternalServerError;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.ResponseAssert.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


final class ServerTest {

    @Nested final class QueryParsing {

        private Map.Entry<String, ? extends List<String>> parameter(final String name,
                final List<String> values) {
            return new AbstractMap.SimpleImmutableEntry<>(name, values);
        }


        @Test void testPreprocessQueryParameters() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {

                Assertions.assertThat(request.parameters()).containsExactly(
                        parameter("one", singletonList("1")),
                        parameter("two", asList("2", "2"))
                );

                return request.reply(OK);

            })

                    .handle(new Request()
                                    .method(Request.GET)
                                    .query("one=1&two=2&two=2"),
                            Request::reply
                    )

                    .map(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
        }

        @Test void testPreprocessBodyParameters() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {

                Assertions.assertThat(request.parameters()).containsExactly(
                        parameter("one", singletonList("1")),
                        parameter("two", asList("2", "2"))
                );

                return request.reply(OK);

            })

                    .handle(

                            new Request()
                                    .method(POST)
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .input(() -> new ByteArrayInputStream("one=1&two=2&two=2".getBytes(UTF_8))),

                            Request::reply

                    )

                    .map(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
        }

        @Test void testPreprocessDontOverwriteExistingParameters() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {

                Assertions.assertThat(request.parameters()).containsExactly(
                        parameter("existing", singletonList("true"))
                );

                return request.reply(OK);

            })

                    .handle(new Request()
                                    .method(Request.GET)
                                    .query("one=1&two=2&two=2")
                                    .parameter("existing", "true"),
                            Request::reply
                    )

                    .map(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
        }

        @Test void testPreprocessQueryOnlyOnGET() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {

                Assertions.assertThat(request.parameters()).isEmpty();

                return request.reply(OK);

            })

                    .handle(new Request()
                                    .method(Request.PUT)
                                    .query("one=1&two=2&two=2"),
                            Request::reply
                    )

                    .map(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
        }

        @Test void testPreprocessBodyOnlyOnPOST() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {

                Assertions.assertThat(request.parameters()).isEmpty();

                return request.reply(OK);

            })

                    .handle(

                            new Request()
                                    .method(Request.PUT)
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .input(() -> new ByteArrayInputStream("one=1&two=2&two=2".getBytes(UTF_8))),

                            Request::reply

                    )

                    .map(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
        }

    }

    @Nested final class ErrorHandling {

        @Test void testTrapStrayExceptions() {
            Handler.handler(new Locator().get(Server::new), (request, next) -> {
                throw new UnsupportedOperationException("stray");
            })

                    .handle(new Request(), Request::reply)

                    .map(response -> assertThat(response)
                            .hasStatus(InternalServerError)
                    );

        }

    }

}
