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
import com.metreeca.link.*;
import com.metreeca.link.json.JSON;

import org.eclipse.rdf4j.model.Value;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.Response.BadRequest;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

/**
 * JSON-LD message format.
 */
public final class JSONLD implements Format<Frame> {

    /**
     * The default MIME type for JSON-LD messages ({@value}).
     */
    public static final String MIME="application/ld+json";

    /**
     * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
     */
    public static final Pattern MIMEPattern=Pattern.compile(
            "(?i:^(text/json|application/(?:.*\\+)?json)(?:\\s*;.*)?$)"
    );


    /**
     * Retrieves the default wire format codec factory.
     *
     * @return the default wire format codec factory
     */
    public static Supplier<Codec> codec() {
        return JSON::json;
    }

    /**
     * Retrieves the default data storage engine factory.
     *
     * @return the default data storage engine factory, which throws an exception reporting the service as undefined
     */
    public static Supplier<Store> store() {
        return () -> { throw new IllegalStateException("undefined store service"); };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<Frame> type() {
        return Frame.class;
    }


    /**
     * <p><strong>Warning</strong> / Decoding is completely driven by the format: embedded {@code @context} objects are
     * ignored.</p>
     *
     * @return the JSON-LD payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account
     * its {@linkplain Shape shape} {@linkplain Message#attribute(Class) attribute} or an empty optional if the
     * {@code "Content-Type"} {@code message} header is not empty and is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<Frame> decode(final Message<?> message) throws FormatException {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(MIME))
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset())
                    ) {

                        return service(codec()).decode(
                                URI.create(message.item()),
                                reader,
                                message.attribute(Shape.class).orElseGet(Shape::shape)
                        );

                    } catch ( final UnsupportedEncodingException|CodecException e ) {

                        throw new FormatException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
     * {@linkplain Shape shape} {@linkplain Message#attribute(Class) attribute} are embedded only if
     * {@code Content-Type} is {@value MIME}.</p>
     *
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the JSON-LD
     * representation of {@code value} generated taking into account the {@code message}
     * {@linkplain Shape shape} {@linkplain Message#attribute(Class) attribute}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Frame value) throws FormatException {

        final String item=message.item();

        if ( value.id().map(Value::stringValue).filter(not(item::equals)).isPresent() ) {
            throw new IllegalArgumentException(format(
                    "message item <%s> and frame id %s don't match", item, value.id()
            ));
        }


        final String mime=message

                .header("Content-Type") // content-type explicitly defined by handler

                .orElseGet(() -> mimes(message.request().header("Accept").orElse("")).contains(MIME)

                        ? MIME  // application/ld+json accepted
                        : "application/json"  // default to application/json

                );


        final List<String> langs=message.request().langs();
        final boolean global=langs.isEmpty() || langs.contains("*");

        // !!! localization

        return message

                .header("Content-Type", mime)

                .output(output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        service(codec()).encode(
                                URI.create(message.item()),
                                writer,
                                message.attribute(Shape.class).orElseGet(Shape::shape),
                                value
                        );

                        // !!! mime.equals(MIME) // include context objects for application/ld+json

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

}
