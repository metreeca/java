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

    @Override public Class<String> type() {
        return String.class;
    }

    @Override public String mime() {
        return MIME;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public <M extends Message<M>> Optional<String> decode(final M message) {
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

    @Override public <M extends Message<M>> M encode(final M message, final String value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        writer.write(value);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

}
