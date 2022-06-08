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

package com.metreeca.jsonld.codecs;

import com.metreeca.http.*;
import com.metreeca.link.*;
import com.metreeca.link.shapes.Or;

import org.eclipse.rdf4j.model.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.json.*;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.Response.*;
import static com.metreeca.jsonld.codecs.JSONLDInspector.driver;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Trace.trace;
import static com.metreeca.link.Values.iri;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

/**
 * Model-driven JSON-LD message codec.
 */
public final class JSONLD implements Codec<Frame> {

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
     * Retrieves the default JSON-LD keywords service factory.
     *
     * <p>The keywords service maps JSON-LD {@code @keywords} to user-defined aliases.</p>
     *
     * @return the default keywords factory, which returns an empty map
     */
    public static Supplier<Map<String, String>> keywords() {
        return Collections::emptyMap;
    }


    /**
     * Retrieves the JSON-LD shape of a message.
     *
     * @param message the message whose shape is to be retrieved
     *
     * @return the shape associated with {@code message} i f one is fouund; an {@linkplain Or#or() empty disjunction},
     * that is a shape the always fails to validate, otherwise
     *
     * @throws NullPointerException if {@code message} is null
     */
    public static Shape shape(final Message<?> message) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        return message.attribute(Shape.class).orElseGet(Or::or);
    }

    /**
     * Configures the JSON-LD shape of a message.
     *
     * @param message the message whose shape is to be configured
     * @param shape   the shape to be associted with {@code message}
     *
     * @return the shape associated with {@code message} i f one is fouund; an {@linkplain Or#or() empty disjunction},
     * that is a shape the always fails to validate, otherwise
     *
     * @throws NullPointerException if either {@code message} or {@code shape} is null
     */
    public static <M extends Message<M>> M shape(final M message, final Shape shape) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        return message.attribute(Shape.class, shape);
    }


    private static final JsonWriterFactory JsonWriters=Json.createWriterFactory(singletonMap(PRETTY_PRINTING, true));


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Decodes a shape-based query.
     *
     * @param focus the target IRI for the decoding process; relative IRIs will be resolved against it
     * @param shape the base shape for the decoded query
     * @param query the query to be decoded
     *
     * @return either a message exception reporting a decoding issue or the decoded query
     *
     * @throws NullPointerException if any parameter is null
     */
    public static Query query(final IRI focus, final Shape shape, final String query) {

        if ( query == null ) {
            throw new NullPointerException("null query");
        }

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        try {

            return new JSONLDParser(focus, shape, service(keywords())).parse(query);

        } catch ( final JsonException e ) {

            throw new CodecException(BadRequest, e.getMessage());

        } catch ( final NoSuchElementException e ) {

            throw new CodecException(UnprocessableEntity, e.getMessage());

        }
    }


    /**
     * Decodes a shape-based JSON-LD entity.
     *
     * @param focus    the target IRI for the decoding process; relative IRIs will be resolved against it
     * @param shape    the expected shape for the JSON-LD entity
     * @param keywords a map from JSON-LD {@code @keywords} to user-defined aliases
     * @param json     the JSON-LD entity to be decoded
     *
     * @return an RDF representation of the decoded {@code entity}
     *
     * @throws NullPointerException if any parameter is null or contains null values
     */
    public static Collection<Statement> decode(
            final IRI focus, final Shape shape, final Map<String, String> keywords,
            final JsonObject json
    ) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( keywords == null
                || keywords.keySet().stream().anyMatch(Objects::isNull)
                || keywords.values().stream().anyMatch(Objects::isNull)
        ) {
            throw new NullPointerException("null keywords");
        }

        if ( json == null ) {
            throw new NullPointerException("null json");
        }

        return new JSONLDDecoder(

                focus,
                shape,
                keywords

        ).decode(json);

    }

    /**
     * Encodes a shape-based JSON-LD entity.
     *
     * @param focus    the IRI of the entity to be encoded; absolute IRIs will be relativized against it
     * @param shape    the target shape for the entity to be encoded
     * @param keywords a map from JSON-LD {@code @keywords} to user-defined aliases
     * @param model    the {@code focus}-centered RDF representation of the entity to be encoded
     *
     * @return a frame-based JSON-LD representation of the {@code focus} entity as described in {@code model}
     *
     * @throws NullPointerException if any parameter is null or contains null values
     */
    public static JsonObject encode(
            final IRI focus, final Shape shape, final Map<String, String> keywords,
            final Collection<Statement> model
    ) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( keywords == null
                || keywords.keySet().stream().anyMatch(Objects::isNull)
                || keywords.values().stream().anyMatch(Objects::isNull)
        ) {
            throw new NullPointerException("null keywords");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new JSONLDEncoder(

                focus,
                shape,
                keywords,
                false

        ).encode(model);

    }

    /**
     * Encodes a shape-based JSON-LD entity.
     *
     * @param focus    the IRI of the entity to be encoded; absolute IRIs will be relativized against it
     * @param shape    the target shape for the entity to be encoded
     * @param keywords a map from JSON-LD {@code @keywords} to user-defined aliases
     * @param model    the {@code focus}-centered RDF representation of the entity to be encoded
     * @param context  a flag declaring if the JSON-LD {@code @context} should be included in the generated description
     *
     * @return a frame-based JSON-LD representation of the {@code focus} entity as described in {@code model}
     *
     * @throws NullPointerException if any parameter is null or contains null values
     */
    public static JsonObject encode(
            final IRI focus, final Shape shape, final Map<String, String> keywords,
            final Collection<Statement> model,
            final boolean context
    ) {

        return new JSONLDEncoder(

                focus,
                shape,
                keywords,
                context

        ).encode(model);

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
     * <p><strong>Warning</strong> / Decoding is completely driven by the {@code message}
     * {@linkplain JSONLD#shape(Message) shape attribute}: embedded {@code @context} objects are ignored.</p>
     *
     * @return the JSON-LD payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty optional
     * if the {@code "Content-Type"} {@code message} header is not empty and is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<Frame> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(MIME))
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset());
                            final JsonReader jsonReader=Json.createReader(reader)
                    ) {

                        final IRI focus=iri(message.item());
                        final Shape shape=shape(message);
                        final Map<String, String> keywords=service(keywords());

                        final Collection<Statement> model=decode(focus, shape, keywords, jsonReader.readObject());

                        return driver(shape).validate(focus, model)

                                .<Frame>map(trace -> { throw new CodecException(UnprocessableEntity, format(trace)); })

                                .orElseGet(() -> frame(focus, model));

                    } catch ( final JsonException e ) {

                        if ( e.getCause() instanceof IOException ) {
                            throw new UncheckedIOException((IOException)e.getCause());
                        }

                        throw new CodecException(BadRequest, e.getMessage());

                    } catch ( final UnsupportedEncodingException e ) {

                        throw new CodecException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * <p>If the originating {@code message} {@linkplain Message#request() request} includes an {@code Accept-Language}
     * header, a suitably {@linkplain Shape#localize localized} version of the message shape is used in the conversion
     * process and only matching tagged literals from {@code value} are included in the response body.</p>
     *
     * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
     * {@linkplain JSONLD#shape(Message) shape attribute} are embedded only if {@code Content-Type} is {@value
     * MIME}.</p>
     *
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the JSON-LD {@code
     * value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Frame value) {

        final String item=message.item();
        final Value focus=value.focus();

        if ( !focus.isIRI() || !focus.stringValue().equals(item) ) {
            throw new IllegalArgumentException(String.format(
                    "message item <%s> and frame focus %s don't match", item, Values.format(focus)
            ));
        }

        final Shape shape=shape(message);
        final List<String> langs=message.request().langs();

        final boolean global=langs.isEmpty() || langs.contains("*");

        final String mime=message

                .header("Content-Type") // content-type explicitly defined by handler

                .orElseGet(() -> mimes(message.request().header("Accept").orElse("")).contains(MIME)

                        ? MIME  // application/ld+json accepted
                        : "application/json"  // default to application/json

                );


        final Collection<Statement> localized=value.model().stream().filter(statement -> {

            if ( global ) { return true; } else { // retain only tagged literals with an accepted language

                final String lang=Values.lang(statement.getObject());

                return lang.isEmpty() || langs.contains(lang);

            }

        }).collect(toList());


        return driver(shape).validate(value.focus(), localized)

                .<M>map(trace -> {

                    throw new CodecException(InternalServerError,
                            format(trace(trace("invalid JSON-LD payload"), trace))
                    );

                })

                .orElseGet(() -> message

                        .header("Content-Type", mime)

                        .output(output -> {

                            try (
                                    final Writer writer=new OutputStreamWriter(output, message.charset());
                                    final JsonWriter jsonWriter=JsonWriters.createWriter(writer)
                            ) {


                                jsonWriter.writeObject(encode(
                                        iri(item),
                                        shape.localize(langs),
                                        service(keywords()), localized,
                                        mime.equals(MIME) // include context objects for application/ld+json
                                ));


                            } catch ( final IOException e ) {

                                throw new UncheckedIOException(e);

                            }

                        }));

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String format(final Trace trace) {

        try ( final StringWriter writer=new StringWriter() ) {

            Json
                    .createWriterFactory(singletonMap(PRETTY_PRINTING, true))
                    .createWriter(writer)
                    .write(json(trace));

            return writer.toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }

    }

    private JsonObject json(final Trace trace) {

        final JsonObjectBuilder builder=Json.createObjectBuilder();

        if ( !trace.issues().isEmpty() ) {
            builder.add("@errors", Json.createArrayBuilder(trace.issues()));
        }

        trace.fields().forEach((label, nested) -> {

            if ( !nested.isEmpty() ) {
                builder.add(label, json(nested));
            }

        });

        return builder.build();
    }

}
