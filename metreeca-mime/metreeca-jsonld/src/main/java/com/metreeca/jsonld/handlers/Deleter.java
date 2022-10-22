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
import com.metreeca.jsonld.services.Engine;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.shapes.Guard;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.core.Locator.service;
import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.NoContent;
import static com.metreeca.http.Response.NotFound;
import static com.metreeca.jsonld.services.Engine.engine;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Values.iri;
import static com.metreeca.link.shapes.Guard.Delete;
import static com.metreeca.link.shapes.Guard.Detail;


/**
 * Model-driven resource deleter.
 *
 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
 * item}.</p>
 *
 * <ul>
 *
 * <li>redacts the {@linkplain JSONLD#shape(Message) shape} associated with the request according to the request
 * user {@linkplain Request#roles() roles};</li>
 *
 * <li>performs shape-based {@linkplain Operator#keeper(Object, Object) authorization}, considering the subset of
 * the request shape enabled by the {@linkplain Guard#Delete} task and the {@linkplain Guard#Detail} view.</li>
 *
 * <li>deletes the existing description of the resource matching the redacted request shape with the assistance of the
 * shared linked data {@linkplain Engine#delete(Frame, Shape) engine}.</li>
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
public final class Deleter extends Operator {

    private final Engine engine=service(engine());


    /**
     * Creates a resource deleter.
     */
    public Deleter() {
        delegate(handler(
                keeper(Delete, Detail),
                wrapper(),
                delete()
        ));
    }


    private Handler delete() {
        return (request, next) -> {

            final IRI item=iri(request.item());
            final Shape shape=JSONLD.shape(request);

            return engine.delete(frame(item), shape)

                    .map(frame -> request.reply(NoContent))

                    .orElseGet(() -> request.reply(NotFound)); // !!! 410 Gone if previously known

        };
    }
}
