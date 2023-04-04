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

import com.metreeca.http.*;
import com.metreeca.jsonld.formats.Bean;
import com.metreeca.rest.*;
import com.metreeca.rest.json.JSON;

import java.io.*;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.jsonld.formats.Bean.codec;
import static com.metreeca.jsonld.formats.Bean.engine;
import static com.metreeca.rest.Frame.frame;
import static com.metreeca.rest.Query.filter;
import static com.metreeca.rest.Query.query;
import static com.metreeca.rest.Trace.trace;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;


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
                .map(this::decode)
                .map(query -> {

                    try {

                        final Class<?> clazz=model.value().getClass();
                        final Object object=codec.decode(new StringReader(query), clazz);

                        // ;( may happen if query contains top-level filters, aggregates or range metadata…

                        return merge(frame(object), model).orElseThrow(() -> new IllegalArgumentException(format(
                                "unable to parse query as <%s> template", clazz.getSimpleName()
                        )));

                    } catch ( final JSON.Exception e ) {

                        throw new FormatException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                })
                .orElseGet(model::copy);

        final String expected=request.item();
        final String provided=template.id();

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


    //// !!! factor ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <T> Optional<Frame<T>> merge(final Frame<T> frame, final Frame<T> specs) {

        if ( frame.value().getClass().isAssignableFrom(specs.value().getClass()) ) {

            frame.entries(true).forEach(entry -> {

                final String field=entry.getKey();

                final Object value=entry.getValue();
                final Object model=specs.get(field);

                if ( model instanceof Query ) { // merge filters

                    final Query<Object> filters=query(((Query<?>)model)
                            .filters().entrySet().stream()
                            .map(filter -> filter(filter.getKey(), filter.getValue()))
                            .collect(toList())
                    );

                    if ( value instanceof Query ) {

                        frame.set(field, query((Query<?>)value, filters));

                    } else if ( value instanceof Collection ) {

                        // !!! merge specs filters
                        // !!! handles 0/1/multiple items

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    } else {

                        // !!! merge specs filters
                        // !!! ignore? report?

                        throw new UnsupportedOperationException(";( be implemented"); // !!!

                    }

                } else if ( model != null && !(value instanceof Query) ) { // merge specs value to support virtual
                    // entities

                    if (

                            value instanceof Boolean && value.equals(false)
                                    || value instanceof Number && ((Number)value).intValue() == 0
                                    || value instanceof String && ((String)value).isBlank()
                                    || value instanceof Collection && ((Collection<?>)value).isEmpty()

                    ) {

                        frame.set(field, model);

                    }

                }

            });

            return Optional.of(frame);

        } else {

            return Optional.empty();

        }
    }


    private String decode(final String query) {
        return query.startsWith("%7B") ? URLDecoder.decode(query, UTF_8)
                // !!! Base64
                // !!! form
                : query;

    }

}
