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

import com.metreeca.core.Feeds;
import com.metreeca.http.Output;
import com.metreeca.rest.Codec;
import com.metreeca.rest.Message;

import java.io.*;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.metreeca.core.Feeds.text;
import static com.metreeca.http.Input.Input;

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

    @Override protected <M extends Message<M>> Optional<M> encode(final M message) {
        return message.payload(Text).map(text -> message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .payload(Output.class, output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        writer.write(text);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                })

        );
    }

    @Override protected <M extends Message<M>> Optional<M> decode(final M message) throws IllegalArgumentException {
        return message.header("Content-Type").filter(MIMEPattern.asPredicate()).map(type -> {

            try (
                    final InputStream input=message.payload(Input).orElseGet(() -> Feeds::input).get();
                    final Reader reader=new InputStreamReader(input, message.charset())
            ) {

                return message.payload(Text, text(reader));

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }

        });
    }

}
