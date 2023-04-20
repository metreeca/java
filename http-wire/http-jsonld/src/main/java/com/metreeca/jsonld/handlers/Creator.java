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

package com.metreeca.jsonld.handlers;

import com.metreeca.core.toolkits.Identifiers;
import com.metreeca.http.*;
import com.metreeca.jsonld.formats.Bean;
import com.metreeca.link.*;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.toolkits.Identifiers.AbsoluteIRIPattern;
import static com.metreeca.core.toolkits.Identifiers.md5;
import static com.metreeca.http.Response.*;
import static com.metreeca.jsonld.formats.Bean.engine;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Trace.trace;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

/**
 * Model-driven resource creator.
 *
 * <p>Handles creation requests on the linked data resources identified by the request {@linkplain Request#item()
 * focus item}:</p>
 *
 * <ul>
 *
 * <li>validates the {@link Bean JSON-LD} request body against its expected shape; malformed or invalid
 * payloads are reported respectively with a {@value Response#BadRequest} or a {@value Response#UnprocessableEntity}
 * status code;</li>
 *
 * <li>generates a unique IRI for the resource to be created on the basis on the stem of the the request IRI and
 * the value of the {@code Slug} request header, if one is found, or a random id, otherwise;</li>
 *
 * <li>rewrites the request body to the assigned IRI and stores it with the assistance of the shared linked data
 * {@linkplain Engine#create(Object) engine}.</li>
 *
 * </ul>
 *
 * <p>If the shared linked data engine was able to create a resource matching the request item, generates a response
 * including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#Created} status code;</li>
 *
 * <li>a {@code Location} HTTP response header advertising the IRI of the newly created resource.</li>
 *
 * </ul>
 *
 * <p>Otherwise, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#Conflict} status code.</li>
 *
 * </ul>
 */
public class Creator implements Handler {

    private final Frame<Object> model;

    private Function<Request, String> slug=request -> md5();

    private final Engine engine=service(engine());


    public Creator(final Object model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        this.model=frame(model);
    }


    /**
     * Configures the slug generator.
     *
     * @param slug a function mapping from the creation request to the identifier to be assigned to the newly created
     *             resource; must return a non-null non-clashing absolute IRI
     *
     * @return this creator handler
     *
     * @throws NullPointerException if {@code slug} is null or returns null values
     */
    public Creator slug(final Function<Request, String> slug) {

        if ( slug == null ) {
            throw new NullPointerException("null slug");
        }

        this.slug=slug;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final Frame<?> body=frame(request.body(new Bean<>(model.getClass())));

        final String expected=request.item();
        final String provided=body.id();

        if ( Optional.ofNullable(provided)

                .filter(not(String::isEmpty))
                .filter(not(expected::equals))

                .isPresent()

        ) {

            return request.reply(UnprocessableEntity)
                    .body(new Bean<>(Trace.class), trace(format("mismatched id <%s>", provided)));

        } else {

            final String created=Objects.requireNonNull(slug.apply(request), "null generated slug");

            if ( !AbsoluteIRIPattern.matcher(created).matches() ) {
                throw new IllegalArgumentException(format("generated slug <%s> is not an absolute IRI", created));
            }

            return body.validate()

                    .map(trace -> request.reply(UnprocessableEntity)
                            .body(new Bean<>(Trace.class), trace)
                    )

                    .orElseGet(() -> engine.create(body.id(created))

                            .map(frame -> request.reply(Created, URI.create(Identifiers.path(frame.id()))))

                            .orElseGet(() -> request.reply(Conflict))

                    );

        }

    }

}