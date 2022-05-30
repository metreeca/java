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


import com.metreeca.http.*;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * Wrapping pre/post-processing handler.
 *
 * <p>Rejects all requests with no enabled user {@link Request#roles()} with a {@link Response#Unauthorized} status
 * code.</p>
 */
public final class Wrapper implements Handler {

    private Function<Request, Request> before=identity();
    private Function<Response, Response> after=identity();


    /**
     * Adds a request pre-processor.
     *
     * @param processor a request mapping function; must return a non-null value
     *
     * @return this wrapping handler
     *
     * @throws NullPointerException if {@code processor} is null or returns a null value
     */
    public Wrapper before(final Function<Request, Request> processor) {

        if ( processor == null ) {
            throw new NullPointerException("null processor");
        }

        before=before.andThen(request ->
                requireNonNull(processor.apply(request), "null pre-processor return value")
        );

        return this;
    }

    /**
     * Adds a response post-processor.
     *
     * @param processor a response mapping function; must return a non-null value
     *
     * @return this wrapping handler
     *
     * @throws NullPointerException if {@code processor} is null or returns a null value
     */
    public Wrapper after(final Function<Response, Response> processor) {

        if ( processor == null ) {
            throw new NullPointerException("null processor");
        }

        after=after.andThen(response ->
                requireNonNull(processor.apply(response), "null post-processor return value")
        );

        return this;
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return forward.apply(request.map(before)).map(after);
    }

}
