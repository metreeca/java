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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.MethodNotAllowed;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest._MessageException.status;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class RouterTest {

    @Nested final class Paths {

        private Request request(final String path) {
            return new Request().path(path);
        }

        private Handler handler() {
            return (request, forward) -> request.reply()
                    .status(OK)
                    .header("path", request.path()
                    );
        }


        @Test void testCheckPaths() {

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("empty path")
                    .isThrownBy(() -> new Router()
                            .path("", handler())
                    );

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("missing leading slash path")
                    .isThrownBy(() -> new Router()
                            .path("path", handler())
                    );

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("malformed placeholder step")
                    .isThrownBy(() -> new Router()
                            .path("/pa{}th", handler())
                    );

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("malformed prefix step")
                    .isThrownBy(() -> new Router()
                            .path("/pa*th", handler())
                    );

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("inline prefix step")
                    .isThrownBy(() -> new Router()
                            .path("/*/path", handler())
                    );

            assertThatExceptionOfType(IllegalStateException.class)
                    .as("existing path")
                    .isThrownBy(() -> new Router()
                            .path("/path", handler())
                            .path("/path", handler())
                    );

        }

        @Test void testIgnoreUnknownPath() {
            new Router()

                    .path("/path", handler())

                    .handle(request("/unknown"), Request::reply)

                    .map(response -> assertThat(response)
                            .as("request ignored")
                            .hasStatus(0)
                            .doesNotHaveHeader("path")
                    );
        }


        @Test void testMatchesLiteralPath() {

            final Router router=new Router().path("/path", handler());

            router.handle(request("/path"), Request::reply).map(response -> assertThat(response)
                    .hasHeader("path", "/path")
            );

            router.handle(request("/path/"), Request::reply).map(response -> assertThat(response)
                    .doesNotHaveHeader("path")
            );

            router.handle(request("/path/unknown"), Request::reply).map(response -> assertThat(response)
                    .doesNotHaveHeader("path")
            );

        }

        @Test void testMatchesPlaceholderPath() {

            final Router router=new Router().path("/head/{id}/tail", handler());

            router.handle(request("/head/path/tail"), Request::reply).map(response -> assertThat(response)
                    .hasHeader("path", "/head/path/tail")
            );

            router.handle(request("/head/tail"), Request::reply).map(response -> assertThat(response)
                    .doesNotHaveHeader("path")
            );

            router.handle(request("/head/path/path/tail"), Request::reply).map(response -> assertThat(response)
                    .doesNotHaveHeader("path")
            );

        }

        @Test void testSavePlaceholderValuesAsRequestParameters() {

            new Router().path("/{head}/{tail}", handler())
                    .handle(request("/one/two"), Request::reply)
                    .map(response -> assertThat(response.request())
                            .as("placeholder values saved as parameters")
                            .hasParameter("head", "one")
                            .hasParameter("tail", "two")
                    );

            new Router().path("/{}/{}", handler())
                    .handle(request("/one/two"), Request::reply)
                    .map(response -> assertThat(response.request())
                            .has(new Condition<>(
                                    request -> request.parameters().isEmpty(),
                                    "empty placeholders ignored")
                            )
                    );

        }

        @Test void testMatchesPrefixPath() {

            final Router router=new Router().path("/head/*", handler());

            router.handle(request("/head/path"), Request::reply).map(response -> assertThat(response)
                    .hasHeader("path", "/head/path")
            );

            router.handle(request("/head/path/path"), Request::reply).map(response -> assertThat(response)
                    .hasHeader("path", "/head/path/path")
            );

            router.handle(request("/head"), Request::reply).map(response -> assertThat(response)
                    .doesNotHaveHeader("path")
            );

        }


        @Test void testPreferFirstMatch() {

            final Router router=new Router()

                    .path("/path", (request, forward) -> request.reply(100))
                    .path("/*", (request, forward) -> request.reply(200));

            router.handle(request("/path"), Request::reply).map(response -> assertThat(response).hasStatus(100));
            router.handle(request("/path/path"), Request::reply).map(response -> assertThat(response).hasStatus(200));

        }

    }

    @Nested final class Methods {

        private Response handler(final Request request) {
            return request.reply(OK).output(output -> {
                try {
                    output.write("body".getBytes());
                } catch ( final IOException e ) {
                    throw new UncheckedIOException(e);
                }
            });
        }


        @Test void testHandleOPTIONSByDefault() {
            new Router()

                    .get((request, forward) -> handler(request))

                    .handle(new Request().method(Request.OPTIONS), Request::reply)

                    .map(response -> assertThat(response)
                            .hasStatus(OK)
                            .hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
                    );
        }

        @Test void testIncludeAllowHeaderOnUnsupportedMethods() {
            new Router()

                    .get((request, forward) -> handler(request))

                    .handle(new Request().method(Request.POST), Request::reply)

                    .map(response -> assertThat(response)
                            .hasStatus(MethodNotAllowed)
                            .hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
                    );
        }

        @Test void testHandleHEADByDefault() {
            new Router()

                    .get((request, forward) -> handler(request))

                    .handle(new Request().method(Request.HEAD), Request::reply)

                    .map(response -> assertThat(response)
                            .hasStatus(OK)
                            .doesNotHaveBody()
                    );
        }

        @Test void testRejectHEADIfGetIsNotSupported() {
            new Router()

                    .post(status(Response.Created))

                    .handle(new Request().method(Request.HEAD), Request::reply)

                    .map(response -> assertThat(response)
                            .hasStatus(MethodNotAllowed)
                    );
        }

    }

}
