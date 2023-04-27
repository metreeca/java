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

import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.jsonld.formats.Bean;
import com.metreeca.link.Engine;
import com.metreeca.link.Frame;
import com.metreeca.link.Trace;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.jsonld.formats.Bean.engine;
import static com.metreeca.link.Frame.frame;
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
 * <li>validates the {@link Bean JSON-LD} request body against its expected shape; malformed or invalid
 * payloads are reported respectively with a {@value Response#BadRequest} or a {@value Response#UnprocessableEntity}
 * status code;</li>
 *
 * <li>updates the existing description of the resource matching the redacted request shape with the assistance of the
 * shared linked data {@linkplain Engine#update(Object) engine}.</li>
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

    private final Class<?> type;

    private final Engine engine=service(engine());


    public Updater(final Object model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        this.type=model.getClass();
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final Frame<?> body=frame(request.body(new Bean<>(type)));

        final String expected=request.item();
        final String provided=body.id(); // !!! resolve against request.base()

        if ( Optional.ofNullable(provided)

                .filter(not(String::isEmpty))
                .filter(not(expected::equals))

                .isPresent()

        ) {

            return request.reply(UnprocessableEntity)
                    .body(new Bean<>(Trace.class), trace(format("mismatched id <%s>", provided)));

        } else {

            return body.validate()

                    .map(trace -> request.reply(UnprocessableEntity)
                            .body(new Bean<>(Trace.class), trace)
                    )

                    .orElseGet(() -> engine.update(body.id(expected))

                            .map(frame -> request.reply(NoContent))

                            .orElseGet(() -> request.reply(NotFound))

                    );

        }

    }

}
