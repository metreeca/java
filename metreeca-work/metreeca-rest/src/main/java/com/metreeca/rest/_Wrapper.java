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

package com.metreeca.rest;


import com.metreeca.http.*;
import com.metreeca.link.Shape;
import com.metreeca.link.shapes.Guard;
import com.metreeca.rest.codecs.JSONLD;

import java.util.*;
import java.util.function.*;

import static com.metreeca.http.Response.Forbidden;
import static com.metreeca.http.Response.Unauthorized;
import static com.metreeca.link.shapes.Guard.*;
import static com.metreeca.rest.Handler.handler;
import static com.metreeca.rest.codecs.JSONLD.shape;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;


/**
 * Handler wrapper {thread-safe}.
 *
 * <p>Inspects and possibly alters {@linkplain Request requests} and {@linkplain Response responses} processed and
 * generated by resource {@linkplain Handler handlers}.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
@FunctionalInterface public interface _Wrapper {

    /**
     * Creates a conditional wrapper.
     *
     * @param test the request predicate used to decide if requests and responses are to be routed through the wrapper
     * @param pass the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
     *             true} on the request
     *
     * @return a conditional wrapper that routes requests and responses through the {@code pass} handler if the {@code
     * test} predicate evaluates to {@code true} on the request or to a dummy wrapper otherwise
     *
     * @throws NullPointerException if either {@code test} or {@code pass} is null
     */
    public static _Wrapper wrapper(final Predicate<Request> test, final _Wrapper pass) {

        if ( test == null ) {
            throw new NullPointerException("null test predicate");
        }

        if ( pass == null ) {
            throw new NullPointerException("null pass wrapper");
        }

        return wrapper(test, pass, handler -> handler);
    }

    /**
     * Creates a conditional wrapper.
     *
     * @param test the request predicate used to select the wrapper requests and responses are to be routed through
     * @param pass the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
     *             true} on the request
     * @param fail the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
     *             false} on the request
     *
     * @return a conditional wrapper that routes requests and responses either through the {@code pass} or the {@code
     * fail} wrapper according to the results of the {@code test} predicate
     *
     * @throws NullPointerException if any of the arguments is null
     */
    public static _Wrapper wrapper(final Predicate<Request> test, final _Wrapper pass, final _Wrapper fail) {

        if ( test == null ) {
            throw new NullPointerException("null test predicate");
        }

        if ( pass == null ) {
            throw new NullPointerException("null pass wrapper");
        }

        if ( fail == null ) {
            throw new NullPointerException("null fail wrapper");
        }

        return handler -> handler(test, pass.wrap(handler), fail.wrap(handler));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a pre-processing wrapper.
     *
     * @param mapper a request mapping function; must return a non-null value
     *
     * @return a wrapper that pre-process requests using {@code mapper}
     *
     * @throws NullPointerException if {@code mapper} is null or returns a null value
     */
    public static _Wrapper preprocessor(final Function<Request, Request> mapper) {

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return handler -> (request, next) -> handler.handle(
                requireNonNull(mapper.apply(request), "null mapper return value"),
                next);
    }

    /**
     * Creates a  {@linkplain Response#success() successful} post-processing wrapper.
     *
     * @param mapper a response mapping function; must return a non-null value
     *
     * @return a wrapper that post-process successful responses using {@code mapper}
     *
     * @throws NullPointerException if {@code mapper} is null or returns a null value
     */
    public static _Wrapper postprocessor(final Function<Response, Response> mapper) {

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return handler -> (request, next) -> handler.handle(request, next).map(response -> response.success()
                ? requireNonNull(mapper.apply(response), "null mapper return value")
                : response
        );
    }


    /**
     * Creates a pre-processing body wrapper.
     *
     * @param <V>    the type of the request body to be pre-processed
     * @param codec  the codec of the request body to be pre-processed
     * @param mapper the request body mapper; takes as argument a request and its {@code codec} body and must return a
     *               non-null updated value
     *
     * @return a wrapper that pre-process request {@code codec} bodies using {@code mapper}
     *
     * @throws NullPointerException if either {@code codec} or {@code mapper} is null
     */
    public static <V> _Wrapper preprocessor(
            final Codec<V> codec, final BiFunction<? super Request, ? super V, V> mapper
    ) {

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return handler -> (request, forward) -> handler.handle(

                request.body(codec,
                        requireNonNull(mapper.apply(request, request.body(codec)), "null mapper return value")
                ),

                forward

        );

    }

    /**
     * Creates a {@linkplain Response#success() successful} post-processing body wrapper.
     *
     * @param <V>    the type of the response body to be post-processed
     * @param codec  the codec of the response body to be post-processed
     * @param mapper the response body mapper; takes as argument a response and its {@code codec} body and must return a
     *               non-null updated value
     *
     * @return a wrapper that post-process successful response {@code codec} bodies using {@code mapper}
     *
     * @throws NullPointerException if either {@code codec} or {@code mapper} is null
     */
    public static <V> _Wrapper postprocessor(
            final Codec<V> codec, final BiFunction<? super Response, ? super V, V> mapper
    ) {

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return handler -> (request, next) -> handler.handle(request, next).map(response ->
                response.success() ? response.body(codec,
                        requireNonNull(mapper.apply(response, response.body(codec)), "null mapper return value")
                ) : response
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a role-based access controller.
     *
     * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform the action managed by the
     *              wrapped handler
     *
     * @return a role-based access controller rejecting all requests with no enabled user {@code roles} with a {@link
     * Response#Unauthorized} status code
     *
     * @throws NullPointerException if {@code roles} is null or contains null values
     */
    public static _Wrapper roles(final Object... roles) {

        if ( roles == null || Arrays.stream(roles).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        return roles(asList(roles));
    }

    /**
     * Creates a role-based access controller.
     *
     * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform the action managed by the
     *              wrapped handler
     *
     * @return a role-based access controller rejecting all requests with no enabled user {@code roles} with a {@link
     * Response#Unauthorized} status code
     *
     * @throws NullPointerException if {@code roles} is null or contains null values
     */
    public static _Wrapper roles(final Collection<Object> roles) {

        if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        return handler -> (request, next) -> request.roles().stream().anyMatch(roles::contains)
                ? handler.handle(request, next) : request.reply(Unauthorized); // !!! 404 under strict security
    }


    /**
     * Creates a shape-based access controller.
     *
     * @param task the accepted value for the {@linkplain Guard#Task task} parametric axis
     * @param view the accepted values for the {@linkplain Guard#View task} parametric axis
     *
     * @return a wrapper performing role-based shape redaction and shape-based authorization
     *
     * @throws NullPointerException if either {@code task} or {@code view} is null
     */
    public static _Wrapper keeper(final Object task, final Object view) {
        return handler -> (request, next) -> {

            final Shape shape=JSONLD.shape(request) // visible taking into account task/area

                    .redact(Task, task)
                    .redact(View, view)
                    .redact(Mode, Convey);

            final Shape baseline=shape // visible to anyone

                    .redact(Role);

            final Shape authorized=shape // visible to user

                    .redact(Role, request.roles());


            final UnaryOperator<Request> incoming=message -> JSONLD.shape(message, shape(message)

                    .redact(Role, message.roles())
                    .redact(Task, task)
                    .redact(View, view)

                    .localize(message.request().langs())
            );

            final UnaryOperator<Response> outgoing=message -> JSONLD.shape(message, shape(message)

                    .redact(Role, message.request().roles())
                    .redact(Task, task)
                    .redact(View, view)
                    .redact(Mode, Convey)

                    .localize(message.request().langs())
            );

            return baseline.empty() ? request.reply(Forbidden)
                    : authorized.empty() ? request.reply(Unauthorized)
                    : handler.handle(request.map(incoming), next).map(outgoing);

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Wraps a handler.
     *
     * @param handler the handler to be wrapped
     *
     * @return the combined handler generated by wrapping this wrapper around {@code handler}
     */
    public Handler wrap(final Handler handler);


    /**
     * Chains a wrapper.
     *
     * @param wrapper the handler to be chained
     *
     * @return the combined wrapper generated by wrapping this wrapper around {@code wrapper}
     *
     * @throws NullPointerException if {@code wrapper} is null
     */
    public default _Wrapper with(final _Wrapper wrapper) {

        if ( wrapper == null ) {
            throw new NullPointerException("null wrapper");
        }

        return handler -> wrap(wrapper.wrap(handler));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Delegating wrapper.
     *
     * <p>Delegates request processing to a {@linkplain #delegate(_Wrapper) delegate} wrapper, possibly assembled as a
     * combination of other wrappers.</p>
     */
    public abstract class Base implements _Wrapper {

        private _Wrapper delegate=handler -> handler;


        /**
         * Configures the delegate wrapper.
         *
         * @param delegate the wrapper request processing is delegated to
         *
         * @return this wrapper
         *
         * @throws NullPointerException if {@code delegate} is null
         */
        protected Base delegate(final _Wrapper delegate) {

            if ( delegate == null ) {
                throw new NullPointerException("null delegate");
            }

            this.delegate=delegate;

            return this;
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        @Override public _Wrapper with(final _Wrapper wrapper) { return delegate.with(wrapper); }

        @Override public Handler wrap(final Handler handler) { return delegate.wrap(handler); }
    }

}
