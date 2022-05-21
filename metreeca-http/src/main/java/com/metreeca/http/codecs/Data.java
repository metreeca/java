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

package com.metreeca.http.codecs;

import com.metreeca.core.Feeds;
import com.metreeca.http.Codec;
import com.metreeca.http.Message;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;


/**
 * Binary message codec.
 */
public final class Data implements Codec<byte[]> {

    /**
     * The default MIME type for binary messages ({@value}).
     */
    public static final String MIME="application/octet-stream";

    /**
     * A pattern matching binary MIME types, for instance {@code application/zip or image/png}.
     */
    public static final Pattern MIMEPattern=Pattern.compile("(?i)^(application|image)/.+$");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<byte[]> type() {
        return byte[].class;
    }


    /**
     * @return the binary payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty optional
     * if the {@code "Content-Type"} {@code message} header is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<byte[]> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try ( final InputStream input=message.input().get() ) {

                        return Feeds.data(input);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });

    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the binary {@code
     * value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final byte... value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))
                .header("Content-Length", valueOf(value.length))

                .output(output -> Feeds.data(output, value));
    }

}
