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

import com.metreeca.http.*;
import com.metreeca.json.codecs.JSON;
import com.metreeca.link.*;
import com.metreeca.link.shapes.Or;
import com.metreeca.rest.Either;

import org.eclipse.rdf4j.model.*;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import javax.json.*;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Response.*;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Trace.trace;
import static com.metreeca.link.Values.format;
import static com.metreeca.link.Values.iri;
import static com.metreeca.link.Values.lang;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
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


    /**
     * Validate a JSON-LD model against a shape.
     *
     * @param focus the target IRI for the validation process
     * @param shape the target shape for the validation process
     * @param model the JSON-LD model to be validated
     *
     * @return either a shape validation trace detailing model issues or the subset of the input {@code model} reachable
     * from the target {@code focus} according to {@code shape}
     *
     * @throws NullPointerException if any parameter is null
     */
    public static Either<Trace, Collection<Statement>> validate(
            final Value focus, final Shape shape, final Collection<Statement> model
    ) {

        if ( shape == null ) {
            throw new NullPointerException("null shape");
        }

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return JSONLDScanner.scan(focus, shape, model);
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
     * Decodes the JSON-LD {@code message} body from the input stream supplied by the {@code message} {@link
     * Message#input()}, if the {@code message} {@code Content-Type} header is matched by {@link JSON#MIMEPattern}
     *
     * <p><strong>Warning</strong> / Decoding is completely driven by the {@code message}
     * {@linkplain JSONLD#shape(Message) shape attribute}: embedded {@code @context} objects are ignored.</p>
     */

    @Override public Optional<Frame> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .filter(JSON.MIMEPattern.asPredicate())

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

                        return validate(focus, shape, model).fold(

                                trace -> { throw new CodecException(UnprocessableEntity, trace.toJSON().toString()); },

                                value -> frame(focus, model) // use model to include inferred statements

                        );

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
     * Configures {@code message} {@code Content-Type} header to {@value JSON#MIME}, unless already defined, and encodes
     * the JSON-LD model {@code value} into the output stream accepted by the {@code message} {@link Message#output()}.
     *
     * <p>If the originating {@code message} {@linkplain Message#request() request} includes an {@code Accept-Language}
     * header, a suitably {@linkplain Shape#localize localized} version of the message shape is used in the conversion
     * process and only matching tagged literals from {@code value} are included in the response body.</p>
     *
     * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
     * {@linkplain JSONLD#shape(Message) shape attribute} are embedded only if {@code Content-Type} is {@value
     * MIME}.</p>
     */
    @Override public <M extends Message<M>> M encode(final M message, final Frame value) {

        final String item=message.item();
        final Value focus=value.focus();

        if ( !focus.isIRI() || !focus.stringValue().equals(item) ) {
            throw new IllegalArgumentException(format(
                    "message item <%s> and frame focus %s don't match", item, format(focus)
            ));
        }

        final Shape shape=shape(message);
        final List<String> langs=message.request().langs();

        final boolean global=langs.isEmpty() || langs.contains("*");

        final String mime=message

                .header("Content-Type") // content-type explicitly defined by handler

                .orElseGet(() -> Message.mimes(message.request().header("Accept").orElse("")).stream()

                        // application/ld+json or application/json accepted?

                        .filter(type -> type.equals(MIME) || type.equals(JSON.MIME)).findFirst()

                        // default to application/json

                        .orElse(JSON.MIME)

                );


        final Collection<Statement> localized=value.model().filter(statement -> {

            if ( global ) { return true; } else { // retain only tagged literals with an accepted language

                final String lang=lang(statement.getObject());

                return lang.isEmpty() || langs.contains(lang);

            }

        }).collect(toList());

        final Collection<Statement> validated=validate(value.focus(), shape, localized).fold(

                trace -> {

                    throw new CodecException(InternalServerError,
                            trace(trace("invalid JSON-LD payload"), trace).toJSON().toString());

                },

                identity()

        );

        return message

                .header("Content-Type", mime)

                .output(output -> {

                    try (
                            final Writer writer=new OutputStreamWriter(output, message.charset());
                            final JsonWriter jsonWriter=JsonWriters.createWriter(writer)
                    ) {


                        jsonWriter.writeObject(encode(
                                iri(item),
                                shape.localize(langs),
                                service(keywords()), validated,
                                mime.equals(MIME) // include context objects for application/ld+json
                        ));


                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

}
