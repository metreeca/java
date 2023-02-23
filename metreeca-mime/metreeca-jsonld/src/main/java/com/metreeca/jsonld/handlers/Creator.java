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
import com.metreeca.jsonld.codecs.JSONLD;
import com.metreeca.jsonld.services.Engine;
import com.metreeca.link.*;
import com.metreeca.link.shapes.Guard;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.toolkits.Identifiers.md5;
import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Request.encode;
import static com.metreeca.http.Response.Created;
import static com.metreeca.jsonld.services.Engine.engine;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Values.format;
import static com.metreeca.link.Values.iri;
import static com.metreeca.link.shapes.Guard.Create;
import static com.metreeca.link.shapes.Guard.Detail;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


/**
 * Model-driven resource creator.
 *
 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item()
 * focus item}:</p>
 *
 * <ul>
 *
 * <li>redacts the {@linkplain JSONLD#shape(Message) shape} associated with the request according to the request
 * user {@linkplain Request#roles() roles};</li>
 *
 * <li>performs shape-based {@linkplain Operator#keeper(Object, Object) authorization}, considering the subset of
 * the request shape enabled by the {@linkplain Guard#Create} task and the {@linkplain Guard#Detail} view;</li>
 *
 * <li>validates the {@link JSONLD JSON-LD} request body against the request shape; malformed or invalid
 * payloads are reported respectively with a {@value Response#BadRequest} or a {@value Response#UnprocessableEntity}
 * status code;</li>
 *
 * <li>generates a unique IRI for the resource to be created on the basis on the stem of the the request IRI and
 * the value of the {@code Slug} request header, if one is found, or a random id, otherwise;</li>
 *
 * <li>rewrites the request body to the assigned IRI and stores it with the assistance of the shared linked data
 * {@linkplain Engine#create(Frame, Shape) engine}; the target container identified by the request focus item is
 * connected to the newly created resource according to the filtering constraints in the request shape.</li>
 *
 * </ul>
 *
 * <p>On successful completion, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#Created} status code;</li>
 *
 * <li>a {@code Location} HTTP response header advertising the IRI of the newly created resource.</li>
 *
 * </ul>
 */
public final class Creator extends Operator {

    private Function<Request, String> slug=request -> md5();

    private final Engine engine=service(engine());


    /**
     * Creates a resource creator with a UUID-based slug generator.
     */
    public Creator() {
        delegate(handler(
                rewrite(),
                keeper(Create, Detail),
                wrapper(),
                create()
        ));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Configures the slug generator.
     *
     * @param slug a function mapping from the creation request to the identifier to be assigned to the newly created
     *             resource; must return a non-null non-clashing value
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

    /**
     * Configures the slug generator.
     *
     * @param slug a function mapping from the creation request and its {@linkplain JSONLD JSON-LD} payload to the
     *             identifier to be assigned to the newly created resource; must return a non-null non-clashing value
     *
     * @return this creator handler
     *
     * @throws NullPointerException if {@code slug} is null or returns null values
     */
    public Creator slug(final BiFunction<? super Request, ? super Frame, String> slug) {

        if ( slug == null ) {
            throw new NullPointerException("null slug");
        }

        this.slug=request -> slug.apply(request, request.body(new JSONLD()));

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Handler rewrite() {
        return (request, next) -> {

            final String name=encode( // encode slug as IRI path component
                    requireNonNull(slug.apply(request), "null resource name")
            );

            final IRI source=iri(request.item());
            final IRI target=iri(source, name);

            return next.apply(request
                    .path(request.path()+name)
                    .body(new JSONLD(), rewrite(target, source, request.body(new JSONLD())))
            );
        };
    }

    private Handler create() {
        return (request, next) -> {

            final IRI item=iri(request.item());
            final Shape shape=JSONLD.shape(request);

            return engine.create(request.body(new JSONLD()), shape)

                    .map(Frame::focus)

                    .map(focus -> request.reply().map(response -> response.status(Created).header("Location", Optional
                            .of(focus)
                            .filter(Value::isIRI)
                            .map(IRI.class::cast)
                            .map(Value::stringValue)
                            .map(Identifiers::path) // root-relative to support relocation
                            .orElse(focus.stringValue())
                    )))

                    .orElseThrow(() ->
                            new IllegalStateException(format("existing resource identifier %s", format(item)))
                    );

        };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Frame rewrite(final IRI target, final IRI source, final Frame frame) {
        return frame(rewrite(target, source, frame.focus()), rewrite(target, source, frame.model()));
    }

    private Collection<Statement> rewrite(final IRI target, final IRI source, final Collection<Statement> model) {
        return model.stream()
                .map(statement -> Values.statement(
                        (Resource)rewrite(target, source, statement.getSubject()),
                        (IRI)rewrite(target, source, statement.getPredicate()),
                        rewrite(target, source, statement.getObject())
                ))
                .collect(Collectors.toList());
    }

    private Value rewrite(final IRI target, final IRI source, final Value focus) {
        return source.equals(focus) ? target : focus;
    }

}
