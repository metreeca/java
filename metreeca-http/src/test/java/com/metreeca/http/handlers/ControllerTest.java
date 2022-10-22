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

import org.junit.jupiter.api.Test;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.Response.Unauthorized;
import static com.metreeca.http.ResponseAssert.assertThat;


final class ControllerTest {

    private void exec(final Runnable... tasks) {
        new Locator()
                .exec(tasks)
                .clear();
    }


    private Request request() {
        return new Request();
    }

    private Handler ok() {
        return (request, next) -> request.reply(OK);
    }


    @Test void testAccepted() {
        exec(() -> handler(new Controller("x", "y"), ok())

                .handle(request().roles("x"), Request::reply)

                .map(response -> assertThat(response).hasStatus(OK))
        );
    }

    @Test void testRejected() {
        exec(() -> handler(new Controller("x", "y"), ok())

                .handle(request().roles("z"), Request::reply)

                .map(response -> assertThat(response).hasStatus(Unauthorized))
        );
    }

    @Test void testRejectedEmpty() {
        exec(() -> handler(new Controller(), ok())

                .handle(request(), Request::reply)

                .map(response -> assertThat(response).hasStatus(Unauthorized))
        );
    }

}
