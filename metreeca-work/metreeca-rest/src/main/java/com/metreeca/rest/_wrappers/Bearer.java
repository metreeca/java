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

package com.metreeca.rest._wrappers;

import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.rest.Handler;
import com.metreeca.rest._Wrapper;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;


/**
 * Bearer token authenticator.
 *
 * <p>Manages bearer token authentication protocol delegating token validation to an authentication service.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>
 */
public final class Bearer implements _Wrapper {

    private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final BiFunction<? super String, ? super Request, Optional<Request>> authenticator;


    /**
     * Creates a key-based bearer token authenticator.
     *
     * @param key   the fixed key to be presented as bearer token; will match no request if empty
     * @param roles a collection of values uniquely identifying the roles to be {@linkplain Request#role(Object...)
     *              assigned} to the request user on successful {@code key} validation
     *
     * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
     */
    public Bearer(final String key, final Object... roles) {

        if ( key == null ) {
            throw new NullPointerException("null key");
        }

        if ( roles == null || Stream.of(roles).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        this.authenticator=key.isEmpty()
                ? (token, request) -> Optional.empty()
                : (token, request) -> token.equals(key) ? Optional.of(request.roles(roles)) : Optional.empty();

    }

    /**
     * Creates a bearer token authenticator.
     *
     * @param authenticator the delegated authentication service; takes as argument the bearer token presented with the
     *                      request and the request itself; returns an optional configured request on successful token
     *                      validation or an empty optional otherwise
     *
     * @throws NullPointerException if {@code authenticator} is null
     */
    public Bearer(final BiFunction<? super String, ? super Request, Optional<Request>> authenticator) {

        if ( authenticator == null ) {
            throw new NullPointerException("null authenticator");
        }

        this.authenticator=authenticator;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Handler wrap(final Handler handler) {
        return handler
                .with(challenger())
                .with(authenticator());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return a wrapper adding authentication challenge to unauthorized responses, unless already provided by nested
     * authorization schemes
     */
    private _Wrapper challenger() {
		return handler -> (request, next) -> handler.handle(request, next).map(response ->
                response.status() == Response.Unauthorized && response.header("WWW-Authenticate").isEmpty()
                        ? response.header("WWW-Authenticate", format("Bearer realm=\"%s\"", request.base()))
                        : response
        );
	}

    /**
     * @return a wrapper managing token-based authentication
     */
    private _Wrapper authenticator() {
        return handler -> (request, next) -> {

            // !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

            final String authorization=request.header("Authorization").orElse("");

            return Optional

                    .of(BearerPattern.matcher(authorization))
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group("token"))

                    // bearer token > authenticate

                    .map(token -> authenticator.apply(token, request)

                            // authenticated > handle request

                            .map(request1 -> handler.handle(request1, next))

                            // not authenticated > report error

							.orElseGet(() -> request.reply().map(response -> response
                                    .status(Response.Unauthorized)
                                    .header("WWW-Authenticate", format(
                                            "Bearer realm=\"%s\", error=\"invalid_token\"", response.request().base()
									))))

                    )

                    // no bearer token > fall-through to other authorization schemes

                    .orElseGet(() -> handler.handle(request, next));
        };
    }

}
