/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rest;

import com.metreeca.http.Input;
import com.metreeca.http.Output;
import com.metreeca.rest.codecs.TextCodec;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.InternalServerError;

/**
 * Message payload codec {thread-safe}.
 *
 * <p>Manages a structured message {@linkplain Message#payload(Class) payload}:</p>
 *
 * <ul>
 *
 *     <li>encoding it into the message {@link Output} payload;</li>
 *     <li>decoding it from the message {@link Input} payload.</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / Concrete subclasses must be thread-safe.</p>
 *
 * @param <V> the type of the message payload managed by the codec
 */
public abstract class Codec<V> implements Handler {

    @Override public final Response handle(final Request request, final Function<Request, Response> forward) {

        try {

            return forward.apply(process(request)).map(response -> {

                try {

                    return process(response);

                } catch ( final RuntimeException e ) {

                    return request.reply(InternalServerError).cause(e);

                }

            });

        } catch ( final IllegalArgumentException e ) {

            return request.reply(BadRequest).payload(TextCodec.Text, e.getMessage());

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <M extends Message<M>> M process(final M message) {
        return encode(message)
                .or(() -> decode(message))
                .orElse(message);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Encodes message payload.
     *
     * @param message the message whose payload is to be encoded
     *
     * @return an optional {@code message} with {@link Message#headers() headers} and {@link Output} payload configured
     * to output the source structured payload managed by this codec, if one was found; an empty optional, otherwise
     *
     * @throws NullPointerException     if {@code message} is null
     * @throws IllegalArgumentException if the {@code message} payload is malformed
     */
    protected abstract <M extends Message<M>> Optional<M> encode(final M message);

    /**
     * Decodes message payload.
     *
     * @param message the message whose payload is to be decoded
     *
     * @return an optional {@code message} with the structured payload managed by this codec decoded from the {@code
     * message} {@link Input} or another structured payload, if one was found; an empty optional, otherwise
     *
     * @throws NullPointerException     if {@code message} is null
     * @throws IllegalArgumentException if the {@code message} payload is malformed
     */
    protected abstract <M extends Message<M>> Optional<M> decode(final M message) throws IllegalArgumentException;

}
