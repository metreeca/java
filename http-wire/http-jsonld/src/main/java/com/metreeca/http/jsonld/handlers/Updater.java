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

import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.jsonld.formats.JSONLD.store;
import static com.metreeca.link.Frame.iri;
import static com.metreeca.link.Trace.trace;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

/**
 * Model-driven resource updater.
 *
 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
 * item}.</p>
 *
 * <ul>
 *
 * <li>validates the {@link JSONLD JSON-LD} request body against the request {@linkplain Shape shape}
 * {@linkplain Message#attribute(Class) attribute}; malformed or invalid payloads are reported respectively with a
 * {@value Response#BadRequest} or a {@value Response#UnprocessableEntity} status code;</li>
 *
 * <li>updates the existing description of the resource matching the request shape with the assistance of the
 * shared linked data {@linkplain Store#update(IRI, Shape, Frame)}  storage engine}.</li>
 *
 * </ul>
 *
 * <p>If the shared linked data engine was able to locate a resource matching the request item, generates a response
 * including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NoContent} status code.</li>
 *
 * </ul>
 *
 * <p>Otherwise, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NotFound} status code.</li>
 *
 * </ul>
 */
public class Updater implements Handler {

    private final Store store=service(store());


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final String item=request.item();
        final Shape shape=request.attribute(Shape.class).orElseGet(Shape::shape);
        final Frame frame=request.body(new JSONLD());

        return frame.id()

                .filter(not(id -> id.stringValue().matches(item)))

                .map(id -> request.reply(UnprocessableEntity)
                        .body(new JSONTrace(), trace(format("mismatched id <%s>", id)))
                )

                .orElseGet(() -> shape.validate(frame)

                        .map(trace -> request.reply(UnprocessableEntity)
                                .body(new JSONTrace(), trace)
                        )

                        .orElseGet(() -> request.reply(
                                store.update(iri(item), shape, frame) ? NoContent : NotFound
                        ))

                );
    }

}
