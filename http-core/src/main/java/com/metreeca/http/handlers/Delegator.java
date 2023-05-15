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

package com.metreeca.http.handlers;

import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;

import java.util.function.Function;

/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, possibly assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

    private Handler delegate=(request, forward) -> request.reply();


    /**
     * Configures the delegate handler.
     *
     * @param delegate the handler request processing is delegated to
     *
     * @return this handler
     *
     * @throws NullPointerException if {@code delegate} is null
     */
    protected Delegator delegate(final Handler delegate) {

        if ( delegate == null ) {
            throw new NullPointerException("null delegate");
        }

        this.delegate=delegate;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return delegate.handle(request, forward);
    }

}
