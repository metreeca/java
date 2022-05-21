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

import com.metreeca.rest.*;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.json.*;
import javax.json.stream.JsonParsingException;

import static com.metreeca.rest.Response.BadRequest;

import static java.util.Collections.singletonMap;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;


/**
 * JSON message codec.
 *
 * @see <a href="https://javaee.github.io/jsonp/">JSR 374 - Java API for JSON Processing</a>
 */
public final class JSON implements Codec<JsonObject> {

    /**
     * The default MIME type for JSON messages ({@value}).
     */
    public static final String MIME="application/json";

    /**
     * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
     */
    public static final Pattern MIMEPattern=Pattern.compile(
            "(?i:^(text/json|application/(?:.*\\+)?json)(?:\\s*;.*)?$)"
    );


    private static final JsonWriterFactory JsonWriters=Json.createWriterFactory(singletonMap(PRETTY_PRINTING, true));


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses a JSON object.
     *
     * @param reader the reader the JSON object is to be parsed from
     *
     * @return the JSON object parsed from {@code reader}
     *
     * @throws NullPointerException if {@code reader} is null
     * @throws CodecException       if {@code reader} contains a malformed document
     */
    public static JsonObject json(final Reader reader) throws CodecException {

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        try ( final JsonReader jsonReader=Json.createReader(reader) ) {

            return jsonReader.readObject();

        } catch ( final JsonParsingException e ) {

            throw new CodecException(BadRequest, e.getMessage());

        }
    }

    /**
     * Writes a JSON object.
     *
     * @param <W>    the type of the {@code writer} the JSON object is to be written to
     * @param writer the writer the JSON object is to be written to
     * @param object the JSON object to be written
     *
     * @return the target {@code writer}
     *
     * @throws NullPointerException if either {@code writer} or {@code object} is null
     */
    public static <W extends Writer> W json(final W writer, final JsonObject object) {

        if ( writer == null ) {
            throw new NullPointerException("null writer");
        }

        if ( object == null ) {
            throw new NullPointerException("null object");
        }

        try ( final JsonWriter jsonWriter=JsonWriters.createWriter(writer) ) {

            jsonWriter.writeObject(object);

            return writer;

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<JsonObject> type() {
        return JsonObject.class;
    }


    /**
     * @return the JSON payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty optional if
     * the {@code "Content-Type"} {@code message} header is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<JsonObject> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset())
                    ) {

                        return json(reader);

                    } catch ( final UnsupportedEncodingException e ) {

                        throw new CodecException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });

    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the JSON {@code
     * value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final JsonObject value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {
                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        json(writer, value);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }
                });
    }

}
