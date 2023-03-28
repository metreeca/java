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

package com.metreeca.http;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Message body format.
 *
 * <p>Encodes and decodes structured {@linkplain Message message} payloads from raw messages.</p>
 *
 * @param <V> the type of the structured payload managed by the format
 */
public interface Format<V> {

    /**
     * Retrieves the payload default MIME type.
     *
     * @return the default MIME type of the structured payload managed by this format
     */
    public String mime();

    /**
     * Retrieves the payload Java type.
     *
     * @return the Java type of the structured payload managed by this format
     */
    public Class<V> type();


    /**
     * Decodes a message body.
     *
     * @param message the source message
     *
     * @return the structured payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty
     * optional if {@code message} doesn't contain the expected content
     *
     * @throws NullPointerException if {@code message} is null
     * @throws FormatException      if the raw {@code message} input is malformed
     * @throws UncheckedIOException if an I/O error occurred while decoding the raw {@code message} input
     * @see Message#body(Format)
     */
    public Optional<V> decode(final Message<?> message) throws FormatException, UncheckedIOException;

    /**
     * Encoded a value into a message body.
     *
     * @param message the target message
     * @param value   the value to be encoded into {@code message}
     * @param <M>     the {@code message} type
     *
     * @return the target {@code message} with its {@linkplain Message#headers(Map) headers} and its raw
     * {@linkplain Message#output(Consumer) output} configured to represent the encoded version of {@code value}
     *
     * @throws NullPointerException if {@code message} is null
     * @throws FormatException       if {@code value} cannot be legally encoded into {@code message} according to the
     *                              specs included in the {@linkplain Message#request() originating request}
     * @see Message#body(Format, Object)
     */
    public <M extends Message<M>> M encode(final M message, final V value) throws FormatException;

}
