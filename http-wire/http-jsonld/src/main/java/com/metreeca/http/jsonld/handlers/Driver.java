/*
 * Copyright © 2013-2024 Metreeca srl
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
import com.metreeca.http.Message;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.link.Shape;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/**
 * Shape-based content driver.
 *
 * <p>Drives the lifecycle of linked data resources managed by wrapped handlers setting the {@linkplain Shape shape}
 * {@linkplain Message#attribute(Class) attribute} of incoming requests</p>
 *
 * <p>Wrapped handlers are responsible for:</p>
 *
 * <ul>
 *
 * <li>validating incoming request according to their {@linkplain Shape shape}
 * {@linkplain Message#attribute(Class) attribute} and the task to be performed;</li>
 *
 * <li>set the {@linkplain Shape shape} {@linkplain Message#attribute(Class) attribute} of outgoing responses
 * to drive further processing (e.g. JSON body mapping).</li>
 *
 * </ul>
 */
public final class Driver implements Handler {

    private final Function<Request, Shape> factory;

    /**
     * Creates a content driver.
     *
     * @param shape the shape driving the lifecycle of the linked data resources managed by the wrapped handler
     *
     * @throws NullPointerException if {@code shape} is null
     */
    public Driver(final Shape shape) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        this.factory=request -> shape;
    }

    /**
     * Creates a content driver.
     *
     * @param factory the request-aware shape factory driving the lifecycle of the linked data resources managed by the
     *                wrapped handler
     *
     * @throws NullPointerException if {@code factory} is null or returns a null value
     */
    public Driver(final Function<Request, Shape> factory) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        this.factory=factory;
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return forward.apply(request.attribute(Shape.class, requireNonNull(factory.apply(request))));
    }

}
