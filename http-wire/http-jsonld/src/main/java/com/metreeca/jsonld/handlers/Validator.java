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
import com.metreeca.link.Trace;

import java.util.*;
import java.util.function.Function;

import static com.metreeca.http.Response.UnprocessableEntity;
import static com.metreeca.link.Trace.trace;

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
