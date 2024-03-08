/*
 * Copyright © 2013-2024 Metreeca srl
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

package com.metreeca.http.jsonld.formats;

import com.metreeca.http.Format;
import com.metreeca.http.FormatException;
import com.metreeca.http.Message;
import com.metreeca.link.Shape;
import com.metreeca.link.Trace;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Optional;
import java.util.function.Consumer;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.jsonld.formats.JSONLD.codec;

/**
 * JSON validation trace message format.
 */
public final class JSONTrace implements Format<Trace> {

    /**
     * The default MIME type for JSON validation trace messages ({@value}).
     */
    public static final String MIME="application/json";


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<Trace> type() {
        return Trace.class;
    }


    /**
     * @return an empty optional
     */
    @Override public Optional<Trace> decode(final Message<?> message) throws FormatException {
        return Optional.empty();
    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the JSON
     * representation of {@code value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Trace value) throws FormatException {

        final String mime=message
                .header("Content-Type") // content-type explicitly defined by handler
                .orElse(MIME);

        return message

                .header("Content-Type", mime)

                .output(output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        service(codec()).encode(writer, message.attribute(Shape.class).orElseGet(Shape::shape), value);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

}
