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
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Store;

import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.NoContent;
import static com.metreeca.http.Response.NotFound;
import static com.metreeca.http.jsonld.formats.JSONLD.store;
import static com.metreeca.link.Frame.*;

/**
 * Model-driven resource deleter.
 *
 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
 * item}:</p>
 *
 * <ul>
 *
 * <li>deletes the existing description of the resource with the assistance of the shared linked data
 * {@linkplain Store#create(Shape, Frame) storage engine}.</li>
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
public class Deleter implements Handler {

    private final Store store=service(store());


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final String item=request.item();
        final Shape shape=request.attribute(Shape.class).orElseGet(Shape::shape);

        return request.reply(store.delete(shape, frame(field(ID, iri(item)))) ? NoContent : NotFound);

    }

}
