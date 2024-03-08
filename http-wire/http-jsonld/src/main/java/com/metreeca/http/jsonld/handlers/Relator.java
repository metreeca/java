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
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Store;
import com.metreeca.link.json.JSON;
import com.metreeca.link.json.JSONException;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.jsonld.formats.JSONLD.store;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.json.JSON.json;

import static java.util.function.Predicate.not;


/**
 * Model-driven resource retriever.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request
 * {@linkplain Request#item() item}:</p>
 *
 * <ul>
 *
 * <li>extracts the expected response model from the request {@linkplain Request#query() query}, if one is provided,
 * and merges it with the provided default model;</li>
 *
 * <li>validates the merged model against the request {@linkplain Shape shape}
 * {@linkplain Message#attribute(Class) attribute};</li>
 *
 * <li>retrieves the existing description of the resource matching the merged response model with the assistance of the
 * shared linked data {@linkplain Store#create(Shape, Frame) storage engine}.</li>
 *
 * </ul>
 *
 * <p>If the shared linked data engine was able to retrieve a resource matching the request item IRI, generates a
 * response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#OK} status code;</li>
 *
 * <li>a {@link JSONLD JSON-LD} body containing a description of the request item. </li>
 *
 * </ul>
 *
 * <p>Otherwise, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NotFound} status code;</li>
 *
 * </ul>
 */
public class Relator implements Handler {

    private static final JSON json=json();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Frame model;

    private final Store store=service(store());


    public Relator(final Frame model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        this.model=model;
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        try {

            final Shape shape=request.attribute(Shape.class).orElseGet(Shape::shape);

            final Frame model=Optional.of(request.query())
                    .filter(not(String::isEmpty))
                    .map(query -> json.decode(query, shape).merge(this.model))
                    .orElse(this.model);

            // !!! validate model

            return store.retrieve(shape, model.set(frame(field(ID, iri(request.item())))))

                    .map(frame -> request.reply(OK)
                            .attribute(Shape.class, shape)
                            .body(new JSONLD(), model.id().isPresent() ? frame : frame.set(frame(field(ID))))
                    )

                    .orElseGet(() -> request.reply(NotFound));

        } catch ( final JSONException e ) {

            return request.reply(BadRequest, e.getMessage());

        }

    }

}
