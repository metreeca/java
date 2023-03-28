/*
 * Copyright © 2013-2023 Metreeca srl. All rights reserved.
 */

package com.metreeca.jsonld.handlers;

import com.metreeca.bean.Trace;
import com.metreeca.http.*;
import com.metreeca.jsonld.codecs.Bean;

import java.util.*;
import java.util.function.Function;

import static com.metreeca.bean.Trace.trace;
import static com.metreeca.http.Response.UnprocessableEntity;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;


/**
 * Validating preprocessor.
 *
 * <p>Applies custom validation {@linkplain #Validator(Function[]) rules} to incoming requests.</p>
 */
public final class Validator implements Handler {

    private final Collection<Function<Request, Trace>> rules;


    /**
     * Creates a validating preprocessor.
     *
     * <p>Validation rules handle a target request and must return a non-null but possibly
     * {@linkplain Trace#empty() empty} validation traces; if the trace is not empty, the request fails with a
     * {@link Response#UnprocessableEntity} status code; otherwise, the request is routed to the wrapped handler.</p>
     *
     * @param rules the custom validation rules to be applied to incoming requests
     *
     * @throws NullPointerException if {@code rules} is null or contains null values
     */
    @SafeVarargs public Validator(final Function<Request, Trace>... rules) {

        if ( rules == null || stream(rules).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null rules");
        }

        this.rules=new LinkedHashSet<>(asList(rules));
    }

    /**
     * Creates a validating preprocessor.
     *
     * <p>Validation rules handle a target request and must return a non-null but possibly
     * {@linkplain Trace#empty() empty} validation traces; if the trace is not empty, the request fails with a
     * {@link Response#UnprocessableEntity} status code; otherwise, the request is routed to the wrapped handler.</p>
     *
     * @param rules the custom validation rules to be applied to incoming requests
     *
     * @throws NullPointerException if {@code rules} is null or contains null values
     */
    public Validator(final Collection<Function<Request, Trace>> rules) {

        if ( rules == null || rules.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null rules");
        }

        this.rules=new LinkedHashSet<>(rules);
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return Optional

                .of(trace(rules.stream()
                        .map(rule -> rule.apply(request))
                        .collect(toList())
                ))

                .filter(trace -> !trace.empty())

                .map(trace -> request.reply(UnprocessableEntity)
                        .body(new Bean<>(Trace.class), trace)
                )

                .orElseGet(() -> forward.apply(request));
    }

}
