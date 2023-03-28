/*
 * Copyright © 2013-2023 Metreeca srl. All rights reserved.
 */

package com.metreeca.jsonld.handlers;

import com.metreeca.bean.*;
import com.metreeca.bean.json.JSON;
import com.metreeca.http.*;
import com.metreeca.jsonld.codecs.Bean;

import java.net.URLDecoder;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.bean.Frame.frame;
import static com.metreeca.bean.Query.filter;
import static com.metreeca.bean.Query.query;
import static com.metreeca.bean.Trace.trace;
import static com.metreeca.core.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.jsonld.codecs.Bean.codec;
import static com.metreeca.jsonld.codecs.Bean.engine;

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

    private final JSON json=service(codec());
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
                .map(query -> merge(frame(json.decode(query, model.value().getClass())), model))
                .orElseGet(model::copy);


        final String expected=request.item();
        final String provided=template.id();

        if ( Optional.ofNullable(provided)

                .filter(not(String::isEmpty))
                .filter(not(expected::equals))

                .isPresent()

        ) {

            return request.reply(UnprocessableEntity)
                    .body(new Bean<>(Trace.class), trace(format("mismatched id ‹%s›", provided)));

        } else {

            return engine.retrieve(template.id(expected))

                    .map(frame -> request.reply(OK)
                            .body(new Bean<>(Object.class), provided == null ? frame.id(null) : frame) // !!! factor
                    )

                    .orElseGet(() -> request.reply(NotFound));

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <T> Frame<T> merge(final Frame<T> frame, final Frame<T> template) { // !!! factor

        frame.entries(true).forEach(entry -> {

            final String field=entry.getKey();

            final Object value=entry.getValue();
            final Object model=template.get(field);

            // extend frame with template values to support virtual entities

            if ( value instanceof Query ) { // merge filters

                if ( model instanceof Query ) {

                    frame.set(field, query((Query<?>)value, query(((Query<?>)model)
                            .filters().entrySet().stream()
                            .map(filter -> filter(filter.getKey(), filter.getValue()))
                            .collect(toList())
                    )));

                }

            } else { // copy value

                frame.set(field, model);

            }

        });

        return frame;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String decode(final String query) {
        return query.startsWith("%7B") ? URLDecoder.decode(query, UTF_8)
                // !!! Base64
                // !!! form
                : query;

    }

}
