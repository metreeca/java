/*
 * Copyright © 2013-2023 Metreeca srl. All rights reserved.
 */

package com.metreeca.jsonld.handlers;

import com.metreeca.core.Locator;
import com.metreeca.http.Handler;
import com.metreeca.http.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.bean.Trace.trace;
import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.Response.UnprocessableEntity;
import static com.metreeca.http.ResponseAssert.assertThat;


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
