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

import com.metreeca.rest.Codec;
import com.metreeca.rest.Message;

import java.io.*;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.metreeca.core.Feeds.text;

/**
 * Textual message codec.
 */
public final class TextCodec extends Codec<String> {

    /**
     * The default MIME type for textual messages ({@value}).
     */
    public static final String MIME="text/plain";

    /**
     * A pattern matching textual MIME types, for instance {@code text/csv}.
     */
    public static final Pattern MIMEPattern=Pattern.compile("(?i:^text/.+$)");


    public static final Class<String> Text=String.class;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Class<String> type() {
        return String.class;
    }


    @Override protected <M extends Message<M>> Optional<M> decode(final M message) throws IllegalArgumentException {
        return _decode(message).map(value -> message.payload(type(), value));
    }

    @Override protected <M extends Message<M>> Optional<M> encode(final M message) {
        return message.payload(type()).map(payload -> _encode(message, payload));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <M extends Message<M>> M _encode(final M message, final String payload) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        writer.write(payload);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    private <M extends Message<M>> Optional<String> _decode(final M message) {
        return message.header("Content-Type").filter(MIMEPattern.asPredicate()).map(type -> {

            try (
                    final InputStream stream=message.input().get();
                    final Reader reader=new InputStreamReader(stream, message.charset())
            ) {

                return text(reader);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }

        });
    }

}