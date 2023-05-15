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

package com.metreeca.http.jsonld.handlers;

import com.metreeca.http.Handler;
import com.metreeca.http.Locator;
import com.metreeca.http.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.Response.UnprocessableEntity;
import static com.metreeca.http.ResponseAssert.assertThat;
import static com.metreeca.link.Trace.trace;


final class ValidatorTest {

    private void exec(final Runnable... tasks) {
        new Locator()
                .exec(tasks)
                .clear();
    }

    private Handler ok() {
        return (request, next) -> request.reply(OK);
    }


    @Test void testAcceptValidRequests() {
        exec(() -> handler(new Validator(request -> trace()), ok())

                .handle(new Request(), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                )
        );
    }

    @Test void testRejectInvalidRequests() {
        exec(() -> handler(new Validator(request -> trace(trace("issue"), trace("issue"))), ok())

                .handle(new Request(), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(UnprocessableEntity)
                        .hasBody()
                )
        );
    }

}
