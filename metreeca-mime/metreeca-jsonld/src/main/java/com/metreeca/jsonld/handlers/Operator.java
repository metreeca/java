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

import com.metreeca.http.Handler;
import com.metreeca.http.handlers.Delegator;

import static com.metreeca.http.Handler.handler;

/**
 * Model-driven resource handler.
 *
 * <p>Handles request/response customization through user-provider wrapper handlers.</p>
 */
public abstract class Operator extends Delegator {

    private Handler wrapper=(request, forward) -> forward.apply(request);


    /**
     * Retrieves the custom wrapper handler.
     *
     * @return the wrapper handler used to customize requests and responses
     */
    protected Handler wrapper() {
        return (request, forward) -> wrapper.handle(request, forward);
    }

    /**
     * Configures the custom wrapper handlers.
     *
     * @param wrappers the wrapper handlers used to customize requests and responses
     *
     * @return this operator
     *
     * @throws NullPointerException if {@code wrappers} is null or contains null elements
     */
    public Operator wrapper(final Handler... wrappers) {

        if ( wrapper == null ) {
            throw new NullPointerException("null wrappers");
        }

        this.wrapper=handler(wrapper);

        return this;
    }

}
