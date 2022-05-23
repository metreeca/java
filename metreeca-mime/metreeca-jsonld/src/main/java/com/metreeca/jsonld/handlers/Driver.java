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
import com.metreeca.jsonld.codecs.JSONLD;
import com.metreeca.link.Shape;
import com.metreeca.link.shapes.Field;
import com.metreeca.link.shapes.Guard;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.metreeca.http.Response.Forbidden;
import static com.metreeca.http.Response.Unauthorized;
import static com.metreeca.jsonld.codecs.JSONLD.shape;
import static com.metreeca.link.Values.inverse;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.*;

import static java.util.Arrays.stream;


/**
 * Shape-based content driver.
 *
 * <p>Drives the lifecycle of linked data resources managed by wrapped handlers {@linkplain JSONLD#shape(Message)
 * associating} a {@linkplain #Driver(Shape...) shape} to incoming requests</p>
 *
 * <p>Wrapped handlers are responsible for:</p>
 *
 * <ul>
 *
 * <li>redacting the shape {@linkplain JSONLD#shape(Message) associated} to incoming request according to the
 * task to be performed;</li>
 *
 * <li>{@linkplain JSONLD#shape(Message) associating} a shape to outgoing responses in order to drive further
 * processing (e.g. JSON body mapping).</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / Both operations must be performed taking into account the {@linkplain Request#roles()
 * roles} of the current request {@linkplain Request#user() user}: no user-related shape redaction is performed by the
 * driver wrapper on behalf of nested handlers.</p>
 */
public final class Driver implements Handler {

    public static final IRI Direct=OWL.SAMEAS;
    public static final IRI Inverse=inverse(Direct);

    private static final String Tag="__link__";


    /**
     * Creates a direct collapsible {@link OWL#SAMEAS owl:sameAs} field.
     *
     * @param shapes the field shapes
     *
     * @return a {@linkplain Field field} with a direct {@link OWL#SAMEAS owl:sameAs} IRI and the given {@code shapes};
     * connected nodes in the {@linkplain JSONLD JSON-LD} response model will be collapsed to the subject node and the
     * link will be removed from the response shape
     *
     * @throws NullPointerException if {@code shapes} is null or contains null elements
     */
    public static Shape link(final Shape... shapes) {
        return field(Tag, Direct, shapes);
    }

    /**
     * Creates an inverse collapsible {@link OWL#SAMEAS owl:sameAs} field.
     *
     * @param shapes the field shapes
     *
     * @return a {@linkplain Field field} with an inverse {@link OWL#SAMEAS owl:sameAs} IRI and the given {@code shapes};
     * connected nodes in the {@linkplain JSONLD JSON-LD} response model will be collapsed to the subject node and the
     * link will be removed from the response shape
     *
     * @throws NullPointerException if {@code shapes} is null or contains null elements
     */
    public static Shape back(final Shape... shapes) {
        return field(Tag, Inverse, shapes);
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
    public static Handler keeper(final Object task, final Object view) {
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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Shape shape;

    /**
     * Creates a content driver.
     *
     * @param shapes the shapes driving the lifecycle of the linked data resources managed by the wrapped handler
     *
     * @throws NullPointerException if {@code shape} is null of ccntains null elements
     */
    public Driver(final Shape... shapes) {

        if ( shapes == null || stream(shapes).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shapes");
        }

        this.shape=and(shapes);
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return forward.apply(shape(request, shape));
    }

}
