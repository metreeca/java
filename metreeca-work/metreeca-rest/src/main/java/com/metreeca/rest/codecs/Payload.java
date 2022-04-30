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

package com.metreeca.rest.codecs;

import com.metreeca.rest.Message;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Message payload.
 *
 * <p>Manages structured {@linkplain Message message} payloads.</p>
 *
 * @param <V> the type of the managed message payload
 */
public abstract class Payload<V> {

    private V value;
    private Throwable error;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the payload value type.
     *
     * @return the type of the structured value managed by this payload
     */
    public abstract Class<V> type();

    /**
     * Retrieves the payload MIME type.
     *
     * @return the MIME type for this payload
     */
    public abstract String mime();


    /**
     * Retrieves the structured message payload.
     *
     * @param message the message whose payload is to be retrieved
     * @param <M>     the type of {@code message}
     *
     * @return this payload, updated with the value retrieved from {@code message} or the error that occurred in the
     * process
     *
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public <M extends Message<M>> Payload<V> get(final M message) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        this.value=null;
        this.error=null;

        message.attribute(type()).ifPresentOrElse(v -> this.value=v, () -> {

            try {

                decode(message).ifPresent(v -> message.attribute(type(), this.value=v));

            } catch ( final IllegalArgumentException e ) {

                this.error=e;

            }

        });

        return this;
    }

    /**
     * Configures the structured message payload.
     *
     * @param message the message whose payload is to be configured
     * @param <M>     the type of {@code message}
     *
     * @return this payload, updated with the contatnet retrieved from {@code message}
     *
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public <M extends Message<M>> M set(final M message, final V value) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        this.value=value;
        this.error=null;

        return encode(message.attribute(type(), value), value);
    }


    public <R> R map(final Supplier<? extends R> empty, final Function<Throwable, R> error, final Function<V, R> value) {

        if ( empty == null ) {
            throw new NullPointerException("null empty");
        }

        if ( error == null ) {
            throw new NullPointerException("null error");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return this.value != null ? value.apply(this.value)
                : this.error != null ? error.apply(this.error)
                : empty.get();

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract <M extends Message<M>> Optional<V> decode(final M message) throws IllegalArgumentException;

    protected abstract <M extends Message<M>> M encode(M message, V value) throws IllegalArgumentException;

}
