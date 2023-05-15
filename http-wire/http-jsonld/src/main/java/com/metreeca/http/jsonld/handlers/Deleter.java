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

package com.metreeca.http.jsonld.handlers;

import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.link.Engine;

import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.NoContent;
import static com.metreeca.http.Response.NotFound;
import static com.metreeca.http.jsonld.formats.Bean.engine;
import static com.metreeca.link.Frame.frame;

/**
 * Model-driven resource deleter.
 *
 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
 * item}.</p>
 *
 * <ul>
 * <li>deletes the existing description of the resource matching the redacted request shape with the assistance of the
 * shared linked data {@linkplain Engine#delete(Object) engine}.</li>
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

    private final Class<?> type;

    private final Engine engine=service(engine());


    public Deleter(final Object model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        this.type=model.getClass();
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final String item=request.item();

        return engine.delete(frame(type).id(item))

                .map(frame -> request.reply(NoContent))

                .orElseGet(() -> request.reply(NotFound));

    }

}
