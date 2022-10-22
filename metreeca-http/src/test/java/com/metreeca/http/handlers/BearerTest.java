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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Request.GET;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.ResponseAssert.assertThat;


final class BearerTest {

    private void exec(final Runnable... tasks) {
        new Locator().exec(tasks).clear();
    }


    private Bearer bearer() {
        return new Bearer((token, request) -> token.equals("token") ? Optional.of(request) : Optional.empty());
    }

    private Handler status(final int status) {
        return (request, forward) -> request.reply(status);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test void testFallThroughToWrappedSchemes() {

        final Handler authenticator=(request, forward) -> forward.apply(request).map(response ->
                response.header("WWW-Authenticate", "Custom")
        );

        exec(() -> handler(bearer(), authenticator, status(Unauthorized))

                .handle(new Request()
                                .method(GET)
                                .header("Authorization", "Custom secret"),
                        Request::reply
                )

                .map(response -> assertThat(response)

                        .as("access denied")
                        .hasStatus(Unauthorized)

                        .as("fall-through challenge")
                        .hasHeader("WWW-Authenticate", "Custom")

                )
        );
    }


    @Nested final class Anonymous {

        @Test void testGranted() {
            exec(() -> handler(bearer(), status(OK))

                    .handle(new Request()
                                    .method(GET),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access granted")
                            .hasStatus(OK)

                            .as("challenge not included")
                            .doesNotHaveHeader("WWW-Authenticate"))
            );
        }

        @Test void testForbidden() {
            exec(() -> handler(bearer(), status(Forbidden))

                    .handle(new Request()
                                    .method(GET),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access denied")
                            .hasStatus(Forbidden)

                            .as("challenge not included")
                            .doesNotHaveHeader("WWW-Authenticate"))
            );
        }

        @Test void testUnauthorized() {
            exec(() -> handler(bearer(), status(Unauthorized))

                    .handle(new Request()
                                    .method(GET),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access denied")
                            .hasStatus(Unauthorized)

                            .as("challenge included without error")
                            .matches(r -> r
                                    .header("WWW-Authenticate").orElse("")
                                    .matches("Bearer realm=\".*\"")
                            )
                    )
            );
        }

    }


    @Nested final class TokenBearing {

        @Test void testGranted() {
            exec(() -> handler(bearer(), status(OK))

                    .handle(new Request()
                                    .method(GET)
                                    .header("Authorization", "Bearer token"),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access granted")
                            .hasStatus(OK)

                            .as("challenge not included")
                            .doesNotHaveHeader("WWW-Authenticate")
                    )
            );
        }

        @Test void testBadCredentials() {
            exec(() -> handler(bearer(), status(OK))

                    .handle(new Request()
                                    .method(GET)
                                    .header("Authorization", "Bearer qwertyuiop"),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access denied")
                            .hasStatus(Unauthorized)

                            .as("challenge included with error")
                            .matches(r -> r
                                    .header("WWW-Authenticate").orElse("")
                                    .matches("Bearer realm=\".*\", error=\"invalid_token\"")
                            )
                    )
            );
        }

        @Test void testForbidden() {
            exec(() -> handler(bearer(), status(Forbidden))

                    .handle(new Request()
                                    .method(GET)
                                    .header("Authorization", "Bearer token"),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access denied")
                            .hasStatus(Forbidden)

                            .as("challenge not included")
                            .doesNotHaveHeader("WWW-Authenticate")
                    )
            );
        }

        @Test void testUnauthorized() {
            exec(() -> handler(bearer(), status(Unauthorized))

                    .handle(new Request()
                                    .method(GET)
                                    .header("Authorization", "Bearer token"),
                            Request::reply
                    )

                    .map(response -> assertThat(response)

                            .as("access denied")
                            .hasStatus(Unauthorized)

                            .as("challenge included without error")
                            .matches(r -> r
                                    .header("WWW-Authenticate").orElse("")
                                    .matches("Bearer realm=\".*\"")
                            )
                    )
            );
        }

    }

}
