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
import com.metreeca.http.jsonld.formats.JSONLD;
import com.metreeca.http.jsonld.formats.JSONTrace;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.util.Collection;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.jsonld.formats.JSONLD.store;
import static com.metreeca.http.toolkits.Identifiers.AbsoluteIRIPattern;
import static com.metreeca.http.toolkits.Identifiers.md5;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Trace.trace;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * Model-driven resource creator.
 *
 * <p>Handles creation requests on the linked data resources identified by the request
 * {@linkplain Request#item() item}:</p>
 *
 * <ul>
 *
 * <li>validates the {@link JSONLD JSON-LD} request body against the request {@linkplain Shape shape}
 * {@linkplain Message#attribute(Class) attribute}; malformed or invalid payloads are reported respectively with a
 * {@value Response#BadRequest} or a {@value Response#UnprocessableEntity} status code;</li>
 *
 * <li>generates a unique IRI for the resource to be created on the basis on the stem of the the request IRI and
 * the value of the {@code Slug} request header, if one is found, or a random id, otherwise;</li>
 *
 * <li>rewrites the request body to the assigned IRI and stores it with the assistance of the shared linked data
 * {@linkplain Store#create(Shape, Frame) storage engine}.</li>
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

    private Function<Request, String> slug=request -> URI.create(request.item()).resolve(md5()).toString();

    private final Store store=service(store());


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

        final String item=request.item();
        final Shape shape=request.attribute(Shape.class).orElseGet(Shape::shape);
        final Frame body=request.body(new JSONLD());

        return body.id()

                .filter(not(id -> id.stringValue().matches(item)))

                .map(id -> request.reply(UnprocessableEntity)
                        .body(new JSONTrace(), trace(format("mismatched id <%s>", id)))
                )

                .orElseGet(() -> shape.validate(body)

                        .map(trace -> request.reply(UnprocessableEntity)
                                .body(new JSONTrace(), trace)
                        )

                        .orElseGet(() -> {

                            final String id=requireNonNull(slug.apply(request), "null generated id");

                            if ( !AbsoluteIRIPattern.matcher(id).matches() ) {
                                throw new IllegalArgumentException(format(
                                        "generated id <%s> is not an absolute IRI", id
                                ));
                            }

                            final IRI current=iri(item);
                            final IRI updated=iri(id);

                            return store.create(shape, rewrite(body, current, updated).set(frame(field(ID, updated))))
                                    ? request.reply(Created, URI.create(URI.create(id).getRawPath()))
                                    : request.reply(Conflict);

                        })
                );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Frame rewrite(final Frame frame, final IRI current, final IRI updated) {
        return frame(frame.fields().entrySet().stream()
                .map(e -> field(e.getKey(), rewrite(e.getValue(), current, updated)))
                .collect(toList())
        );
    }

    private Collection<Value> rewrite(final Collection<Value> values, final IRI current, final IRI updated) {
        return values.stream()
                .map(value -> rewrite(value, current, updated))
                .collect(toList());
    }

    private Value rewrite(final Value value, final IRI current, final IRI updated) {
        return value.equals(current) ? updated : value;
    }

}
