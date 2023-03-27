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

import com.metreeca.core.Locator;
import com.metreeca.http.Request;
import com.metreeca.http.Response;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.ResponseAssert.assertThat;


final class AliaserTest {

    private void exec(final Runnable... tasks) {
        new Locator()
                .exec(tasks)
                .clear();
    }

    private Aliaser aliaser(final String canonical) {
        return new Aliaser(request ->
                request.path().equals("/alias") ? Optional.of(canonical) : Optional.empty());
    }

    private Request request(final String path) {
        return new Request()
                .base("http://example.com/")
                .path(path);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test void testRedirectAliasedItem() {
        exec(() -> handler(aliaser("/canonical"), ((request, next) -> request.reply(OK)))

                .handle(request("/alias"), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(Response.SeeOther)
                        .hasHeader("Location", "/canonical")
                )
        );
    }

    @Test void testForwardIdempotentItems() {
        exec(() -> handler(aliaser("/alias"), ((request, next) -> request.reply(OK)))

                .handle(request("/alias"), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                )
        );
    }

    @Test void testForwardOtherItems() {
        exec(() -> handler(aliaser("/canonical"), ((request, next) -> request.reply(OK)))

                .handle(request("/other"), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                )
        );
    }

}
