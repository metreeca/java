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

package com.metreeca.json.codecs;

import com.metreeca.http.*;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;

import static com.metreeca.http.Response.BadRequest;

import static java.util.Collections.singletonMap;


/**
 * JSON message codec.
 *
 * @see <a href="https://javaee.github.io/jsonp/">JSR 374 - Java API for JSON Processing</a>
 */
public final class JSON implements Codec<JsonValue> {

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


    private static final JsonWriterFactory JsonWriters=
            Json.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses a JSON value.
     *
     * @param reader the reader the JSON value is to be parsed from
     *
     * @return the JSON object parsed from {@code reader}
     *
     * @throws NullPointerException if {@code reader} is null
     * @throws CodecException       if {@code reader} contains a malformed document
     */
    public static JsonValue json(final Reader reader) throws CodecException {

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        try ( final JsonReader jsonReader=Json.createReader(reader) ) {

            return jsonReader.readValue();

        } catch ( final JsonParsingException e ) {

            throw new CodecException(BadRequest, e.getMessage());

        }
    }

    /**
     * Writes a JSON value.
     *
     * @param <W>    the type of the {@code writer} the JSON value is to be written to
     * @param writer the writer the JSON value is to be written to
     * @param value  the JSON value to be written
     *
     * @return the target {@code writer}
     *
     * @throws NullPointerException if either {@code writer} or {@code value} is null
     */
    public static <W extends Writer> W json(final W writer, final JsonValue value) {

        if ( writer == null ) {
            throw new NullPointerException("null writer");
        }

        if ( value == null ) {
            throw new NullPointerException("null object");
        }

        try ( final JsonWriter jsonWriter=JsonWriters.createWriter(writer) ) {

            jsonWriter.write(value);

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

    @Override public Class<JsonValue> type() {
        return JsonValue.class;
    }


    /**
     * @return the JSON payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty optional if
     * the {@code "Content-Type"} {@code message} header is not empty and is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<JsonValue> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(MIME))
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
    @Override public <M extends Message<M>> M encode(final M message, final JsonValue value) {
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
