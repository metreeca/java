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
 * <p>Manages structured {@linkplain Message message} payloads:</p>
 * *
 *
 * @param <V> the type of the message payload managed by the codec
 */
public abstract class Payload<V> {

    private V value;
    private Throwable error;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract Class<V> type();


    public <R> R map(final Supplier<? extends R> empty, final Function<Throwable, R> error, final Function<V, R> value) {

        if ( empty == null ) {
            throw new NullPointerException("null empty");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        if ( error == null ) {
            throw new NullPointerException("null error");
        }

        return this.value != null ? value.apply(this.value)
                : this.error != null ? error.apply(this.error)
                : empty.get();

    }


    public <M extends Message<M>> Payload<V> decode(final M message) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        this.value=null;
        this.error=null;

        message.attribute(type()).ifPresentOrElse(v -> this.value=v, () -> {

            try {

                _decode(message).ifPresent(v -> message.attribute(type(), this.value=v));

            } catch ( final IllegalArgumentException e ) {

                this.error=e;

            }

        });

        return this;
    }

    public <M extends Message<M>> M encode(final M message, final V value) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        this.value=value;
        this.error=null;

        return _encode(message.attribute(type(), value), value);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract <M extends Message<M>> Optional<V> _decode(final M message) throws IllegalArgumentException;

    protected abstract <M extends Message<M>> M _encode(M message, V value) throws IllegalArgumentException;

}
