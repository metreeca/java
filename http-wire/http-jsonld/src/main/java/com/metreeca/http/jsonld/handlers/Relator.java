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

import com.metreeca.http.FormatException;
import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.http.jsonld.formats.Bean;
import com.metreeca.link.Codec;
import com.metreeca.link.Engine;
import com.metreeca.link.Frame;
import com.metreeca.link.Trace;
import com.metreeca.link.json.JSONException;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.jsonld.formats.Bean.codec;
import static com.metreeca.http.jsonld.formats.Bean.engine;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Trace.trace;

import static java.lang.String.format;
import static java.util.function.Predicate.not;


/**
 * Model-driven resource retriever.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
 * focus item}.</p>
 *
 * <p>Otherwise, if the shared linked data {@linkplain Engine#retrieve(Object) engine} was able to retrieve a
 * resource matching the request focus item IRI, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#OK} status code;</li>
 *
 * <li>a {@link Bean JSON-LD} body containing a description of the request item. </li>
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

    private final Frame<Object> model;

    private final Codec codec=service(codec());
    private final Engine engine=service(engine());


    public Relator(final Object model) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        this.model=frame(model);
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        final Frame<?> template=Optional.of(request.query())
                .filter(not(String::isEmpty))
                .map(query -> {

                    try {

                        final Class<?> clazz=model.value().getClass();
                        final Object object=codec.decode(new StringReader(query), clazz);

                        return frame(object).merge(model).orElseThrow(() -> new IllegalArgumentException(format(
                                "unable to parse query as <%s> model", clazz.getSimpleName()
                        )));

                    } catch ( final JSONException e ) {

                        throw new FormatException(BadRequest, e.getMessage(), e);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                })
                .orElseGet(model::copy);

        final String expected=request.item();
        final String provided=template.id(); // !!! resolve against request.base()

        if ( Optional.ofNullable(provided)

                .filter(not(String::isEmpty))
                .filter(not(expected::equals))

                .isPresent()

        ) {

            return request.reply(UnprocessableEntity)
                    .body(new Bean<>(Trace.class), trace(format("mismatched id <%s>", provided)));

        } else {

            return engine.retrieve(template.id(expected))

                    .map(frame -> request.reply(OK)
                            .body(new Bean<>(Object.class), provided == null ? frame.id(null) : frame) // !!! factor
                    )

                    .orElseGet(() -> request.reply(NotFound));

        }

    }

}
