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

package com.metreeca.http.handlers;


import com.metreeca.http.*;

import java.util.*;
import java.util.function.Function;

import static com.metreeca.http.Response.Unauthorized;

import static java.util.Arrays.asList;

/**
 * Role-based access controller.
 *
 * <p>Rejects all requests with no enabled user {@link Request#roles()} with a {@link Response#Unauthorized} status
 * code.</p>
 */
public final class Controller implements Handler {

    private final Set<Object> roles;


    /**
     * Creates a role-based access controller.
     *
     * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform the action managed by the
     *              wrapped handler
     *
     * @throws NullPointerException if {@code roles} is null or contains null values
     */
    public Controller(final Object... roles) {

        if ( roles == null || Arrays.stream(roles).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        this.roles=new HashSet<>(asList(roles));
    }

    /**
     * Creates a role-based access controller.
     *
     * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform the action managed by the
     *              wrapped handler
     *
     * @throws NullPointerException if {@code roles} is null or contains null values
     */
    public Controller(final Collection<Object> roles) {

        if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        this.roles=new HashSet<>(roles);
    }


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return request.roles().stream().anyMatch(roles::contains)
                ? forward.apply(request) : request.reply(Unauthorized); // !!! 404 under strict security
    }

}
