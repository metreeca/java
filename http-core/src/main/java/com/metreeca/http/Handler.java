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

package com.metreeca.http;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;


/**
 * Resource handler {thread-safe}.
 *
 * <p>Exposes and manages the state of a linked data resource, generating {@linkplain Response responses} in reaction
 * to {@linkplain Request requests}.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
@FunctionalInterface public interface Handler {

    /**
     * Creates a conditional handler.
     *
     * @param test the request predicate used to decide if requests and responses are to be routed to the handler
     * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true} on
     *             the request
     *
     * @return a conditional handler that routes requests and responses to the {@code pass} handler if the {@code test}
     * predicate evaluates to {@code true} on the request or falls through to the processing pipeline otherwise
     *
     * @throws NullPointerException if either {@code test} or {@code pass} is null
     */
    public static Handler handler(final Predicate<Request> test, final Handler pass) {

        if ( test == null ) {
            throw new NullPointerException("null test predicate");
        }

        if ( pass == null ) {
            throw new NullPointerException("null pass handler");
        }

        return handler(test, pass, (request, forward) -> forward.apply(request));
    }

    /**
     * Creates a conditional handler.
     *
     * @param test the request predicate used to select the handler requests and responses are to be routed to
     * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true} on
     *             the request
     * @param fail the handler requests and responses are to be routed to when {@code test} evaluates to {@code false }
     *             on the request
     *
     * @return a conditional handler that routes requests and responses either to the {@code pass} or the {@code fail}
     * handler according to the results of the {@code test} predicate
     *
     * @throws NullPointerException if any of the arguments is null
     */
    public static Handler handler(final Predicate<Request> test, final Handler pass, final Handler fail) {

        if ( test == null ) {
            throw new NullPointerException("null test predicate");
        }

        if ( pass == null ) {
            throw new NullPointerException("null pass handler");
        }

        if ( fail == null ) {
            throw new NullPointerException("null fail handler");
        }

        return (request, forward) -> (test.test(request) ? pass : fail).handle(request, forward);
    }


    /**
     * Creates a handler pipeline.
     *
     * @param handlers the pipeline stages
     *
     * @return a composite handler where {@code handlers} handle requests in a pipeline
     *
     * @throws NullPointerException if {@code handlers} is null or contains null elements
     */
    public static Handler handler(final Handler... handlers) {

        if ( handlers == null || Arrays.stream(handlers).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null handlers");
        }

        return handler(asList(handlers));
    }

    /**
     * Creates a handler pipeline.
     *
     * @param handlers the pipeline stages
     *
     * @return a composite handler where {@code handlers} handle requests in a pipeline
     *
     * @throws NullPointerException if {@code handlers} is null or contains null elements
     */
    public static Handler handler(final List<Handler> handlers) {

        if ( handlers == null || handlers.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null handlers");
        }


        final int size=handlers.size();

        if ( size == 0 ) {

            return (request, forward) -> request.reply();

        } else if ( size == 1 ) {

            return handlers.get(0);

        } else {

            final Handler head=handlers.get(0);
            final Handler tail=(size == 2) ? handlers.get(1) : handler(handlers.subList(1, handlers.size()));

            return (request, forward) -> head.handle(request, _request -> tail.handle(_request, forward));

        }

    }


    /**
     * Creates a request body preprocessing handler.
     *
     * @param format the message format used to handle the request body
     * @param mapper the request body mapper
     * @param <T>    the type of the structured payload managed by {@code format}
     *
     * @return a handler replacing the original request body managed by {@code format} with the value produced by mapping
     * it with {@code mapper}
     *
     * @throws NullPointerException if either {@code format} or {@code mapper} is null or {@code mapper} returns a null
     *                              value
     */
    public static <T> Handler request(final Format<T> format, final UnaryOperator<T> mapper) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return (request, forward) -> forward.apply(request.body(format,
                requireNonNull(mapper.apply(request.body(format)), "null mapper return value ")
        ));
    }

    /**
     * Creates a response body postprocessing handler.
     *
     * @param format  the message format used to handle the response body
     * @param mapper the response body mapper
     * @param <T>    the type of the structured payload managed by {@code format}
     *
     * @return a handler replacing the original response body managed by {@code format} with the value produced by
     * mapping
     * it with {@code mapper}
     *
     * @throws NullPointerException if either {@code format} or {@code mapper} is null or {@code mapper} returns a null
     *                              value
     */
    public static <T> Handler response(final Format<T> format, final UnaryOperator<T> mapper) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return (request, forward) -> forward.apply(request).map(response -> response.body(format,
                requireNonNull(mapper.apply(response.body(format)), "null mapper return value ")
        ));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Handles a request.
     *
     * @param request the inbound request for the managed linked data resource
     * @param forward a function forwarding {@code request} to the tail of the handling pipeline
     *
     * @return a response generated in reaction to {@code request}
     *
     * @throws NullPointerException if either {@code request} or {@code forward} is null
     * @throws NullPointerException if {@code forward} returns a null value
     */
    public Response handle(final Request request, final Function<Request, Response> forward);

}