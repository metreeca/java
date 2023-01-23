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

package com.metreeca.http.handlers;

import com.metreeca.http.Request;
import com.metreeca.http.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.http.Response.*;
import static com.metreeca.http.ResponseAssert.assertThat;

@Nested final class WorkerTest {

    private Response handler(final Request request) {
        return request.reply(OK).output(output -> {
            try {
                output.write("body".getBytes());
            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }
        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test void testHandleOPTIONSByDefault() {
        new Worker()

                .get((request, forward) -> handler(request))

                .handle(new Request().method(Request.OPTIONS), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                        .hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
                );
    }

    @Test void testHandleHEADByDefault() {
        new Worker()

                .get((request, forward) -> handler(request))

                .handle(new Request().method(Request.HEAD), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                        .doesNotHaveBody()
                );
    }

    @Test void testRejectUnsupportedMethodsWithAllowHeader() {
        new Worker()

                .get((request, forward) -> handler(request))

                .handle(new Request().method(Request.POST), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(MethodNotAllowed)
                        .hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
                );
    }

    @Test void testRejectHEADIfGetIsNotSupported() {
        new Worker()

                .post((request, forward) -> request.reply(Created))

                .handle(new Request().method(Request.HEAD), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(MethodNotAllowed)
                );
    }

}
