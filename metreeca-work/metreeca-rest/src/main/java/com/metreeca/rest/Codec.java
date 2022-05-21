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

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Message codec.
 *
 * <p>Encodes and decodes structured {@linkplain Message message} payloads from raw messages.</p>
 *
 * @param <V> the type of the structured payload managed by the codec
 */
public interface Codec<V> {

    /**
     * Retrieves the payload type.
     *
     * @return the type of the structured payload managed by this codec
     */
    public Class<V> type();

    /**
     * Retrieves the payload MIME type.
     *
     * @return the MIME type of the structured payload managed by this codec
     */
    public String mime();


    /**
     * Decodes a message.
     *
     * @param message the message to be decoded
     * @param <M>     the {@code message} type
     *
     * @return the structured payload decoded from the raw {@code message} {@linkplain Message#input()}
     *
     * @throws NullPointerException     if {@code message} is null
     * @throws UncheckedIOException     if an I/O error occurred while decoding the raw {@code message} input
     * @throws IllegalArgumentException if the raw {@code message} input is malformed
     */
    public <M extends Message<M>> Optional<V> decode(final M message) throws UncheckedIOException;

    /**
     * Encoded a value into a message.
     *
     * @param message the target message
     * @param value   the value to be encoded into {@code message}
     * @param <M>     the {@code message} type
     *
     * @return the input {@code message} with its raw {@linkplain Message#output(Consumer) output} configured to
     * represent the encoded version of {@code value}
     *
     * @throws NullPointerException     if {@code message} is null
     * @throws IllegalArgumentException if {@code value} cannot be legally encoded into {@code message}
     */
    public <M extends Message<M>> M encode(final M message, final V value) throws IllegalArgumentException;

}
