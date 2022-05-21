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

package com.metreeca.rest.codecs;

import com.metreeca.rest.Codec;
import com.metreeca.rest.Message;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.metreeca.core.Feeds.text;

import static java.lang.String.valueOf;

/**
 * Textual message codec.
 */
public final class Text implements Codec<String> {

    /**
     * The default MIME type for textual messages ({@value}).
     */
    public static final String MIME="text/plain";

    /**
     * A pattern matching textual MIME types, for instance {@code text/csv}.
     */
    public static final Pattern MIMEPattern=Pattern.compile("(?i:^text/.+$)");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<String> type() {
        return String.class;
    }


    /**
     * @return the textual payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account
     * the {@code message} {@linkplain Message#charset() charset} or an empty optional if the {@code "Content-Type"}
     * {@code message} header is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<String> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

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

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the textual {@code
     * value} taking into account the {@code message} {@linkplain Message#charset() charset}
     */
    @Override public <M extends Message<M>> M encode(final M message, final String value) {

        final Charset charset=message.charset();
        final byte[] bytes=value.getBytes(charset);

        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))
                .header("Content-Length", valueOf(bytes.length))

                .output(output -> {
                    try {

                        output.write(bytes);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }
                });

    }

}
