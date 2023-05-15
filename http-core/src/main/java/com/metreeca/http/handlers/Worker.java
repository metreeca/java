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


import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.http.Request.*;
import static com.metreeca.http.Response.MethodNotAllowed;
import static com.metreeca.http.Response.OK;

import static java.util.stream.Collectors.toList;


/**
 * Method-based request dispatcher.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP
 * {@linkplain Request#method() method}; {@linkplain Request#OPTIONS OPTIONS} and {@linkplain Request#HEAD HEAD} methods
 * are delegated to user-overridable default handlers.</p>
 *
 * <p>If no matching method is found, replies with a {@link Response#MethodNotAllowed MethodNotAllowed} status
 * code.</p>
 */
public final class Worker implements Handler {

    private final Map<String, Handler> methods=new LinkedHashMap<>();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Configures the handler for the OPTIONS HTTP method.
     *
     * @param handler the handler to be delegated for OPTIONS HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker options(final Handler handler) {
        return method(OPTIONS, handler);
    }


    /**
     * Configures the handler for the HEAD HTTP method.
     *
     * @param handler the handler to be delegated for HEAD HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker head(final Handler handler) {
        return method(HEAD, handler);
    }

    /**
     * Configures the handler for the GET HTTP method.
     *
     * @param handler the handler to be delegated for GET HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker get(final Handler handler) {
        return method(GET, handler);
    }


    /**
     * Configures the handler for the POST HTTP method.
     *
     * @param handler the handler to be delegated for POST HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker post(final Handler handler) {
        return method(POST, handler);
    }

    /**
     * Configures the handler for the PUT HTTP method.
     *
     * @param handler the handler to be delegated for HTTP PUT method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker put(final Handler handler) {
        return method(PUT, handler);
    }

    /**
     * Configures the handler for the PATCH HTTP method.
     *
     * @param handler the handler to be delegated for PATCH HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker patch(final Handler handler) {
        return method(PATCH, handler);
    }

    /**
     * Configures the handler for the DELETE HTTP method.
     *
     * @param handler the handler to be delegated for DELETE HTTP method
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public Worker delete(final Handler handler) {
        return method(DELETE, handler);
    }


    /**
     * Configures the handler for a HTTP method.
     *
     * @param method  the HTTP method whose handler is to be configured
     * @param handler the handler to be delegated for {@code method}
     *
     * @return this dispatcher
     *
     * @throws NullPointerException if either {@code method} or {@code handler} is null
     */
    public Worker method(final String method, final Handler handler) {

        if ( method == null ) {
            throw new NullPointerException("null method");
        }

        if ( handler == null ) {
            throw new NullPointerException("null handler");
        }

        if ( method.equals(GET) ) {
            methods.putIfAbsent(HEAD, this::head);
        }

        methods.put(method, handler);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        if ( request == null ) {
            throw new NullPointerException("null request");
        }

        return methods.getOrDefault(request.method(), this::options).handle(request, forward);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Response head(final Request request, final Function<Request, Response> forward) {
        return handle(request.method(GET), forward).map(response -> response
                .header("Content-Length", "")
                .output(target -> { })
        );
    }

    private Response options(final Request request, final Function<Request, Response> forward) {
        return request.reply().map(response -> response
                .status(request.method().equals(OPTIONS) ? OK : MethodNotAllowed)
                .headers("Allow", Stream.of(Set.of(OPTIONS), methods.keySet())
                        .flatMap(Collection::stream)
                        .collect(toList())
                )
        );
    }

}
