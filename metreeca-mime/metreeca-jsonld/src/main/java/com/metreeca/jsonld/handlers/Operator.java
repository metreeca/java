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

package com.metreeca.jsonld.handlers;

import com.metreeca.http.*;
import com.metreeca.http.handlers.Delegator;
import com.metreeca.link.Shape;
import com.metreeca.link.shapes.Guard;

import java.util.function.UnaryOperator;

import static com.metreeca.http.Response.Forbidden;
import static com.metreeca.http.Response.Unauthorized;
import static com.metreeca.jsonld.codecs.JSONLD.shape;
import static com.metreeca.link.shapes.Guard.*;

/**
 * Model-driven resource handler.
 *
 * <p>Handles request/response customization through user-provider wrapper handlers.</p>
 */
public abstract class Operator extends Delegator {

    private Handler wrapper=(request, forward) -> forward.apply(request);


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
    protected Handler keeper(final Object task, final Object view) {
        return (request, forward) -> {

            final Shape shape=shape(request) // visible taking into account task/area

                    .redact(Task, task)
                    .redact(View, view)
                    .redact(Mode, Convey);

            final Shape baseline=shape // visible to anyone

                    .redact(Role);

            final Shape authorized=shape // visible to user

                    .redact(Role, request.roles());


            final UnaryOperator<Request> incoming=message -> shape(message, shape(message)

                    .redact(Role, message.roles())
                    .redact(Task, task)
                    .redact(View, view)

                    .localize(message.request().langs())
            );

            final UnaryOperator<Response> outgoing=message -> shape(message, shape(message)

                    .redact(Role, message.request().roles())
                    .redact(Task, task)
                    .redact(View, view)
                    .redact(Mode, Convey)

                    .localize(message.request().langs())
            );

            return baseline.empty() ? request.reply(Forbidden)
                    : authorized.empty() ? request.reply(Unauthorized)
                    : forward.apply(request.map(incoming)).map(outgoing);

        };
    }


    /**
     * Retrieves the custom wrapper handler.
     *
     * @return the wrapper handler used to customize requests and responses
     */
    protected Handler wrapper() {
        return (request, forward) -> wrapper.handle(request, forward);
    }

    /**
     * Configures the custom wrapper handler.
     *
     * @param wrapper the wrapper handler used to customize requests and responses
     *
     * @return this operator
     *
     * @throws NullPointerException if {@code wrappers} is null or contains null elements
     */
    public Operator wrapper(final Handler wrapper) {

        if ( wrapper == null ) {
            throw new NullPointerException("null wrappers");
        }

        this.wrapper=wrapper;

        return this;
    }

}
