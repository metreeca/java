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

package com.metreeca.rest.formats;

import com.metreeca.http.Either;
import com.metreeca.json.*;
import com.metreeca.json.shapes.Or;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import javax.json.*;

import static com.metreeca.http.Either.Left;
import static com.metreeca.http.Either.Right;
import static com.metreeca.http.Locator.service;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Trace.trace;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.lang;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

/**
 * Model-driven JSON-LD message format.
 */
public final class JSONLDFormat extends Format<Frame> {

	/**
	 * The default MIME type for JSON-LD messages ({@value}).
	 */
	public static final String MIME="application/ld+json";


	/**
	 * Creates a JSON-LD message format.
	 *
	 * @return a new JON-LD message format
	 */
	public static JSONLDFormat jsonld() {
		return new JSONLDFormat();
	}


	/**
	 * Retrieves the default JSON-LD shape service factory.
	 *
	 * @return the default shape factory, which returns an {@linkplain Or#or() empty disjunction}, that is a shape the
	 * always fails to validate
	 */
	public static Supplier<Shape> shape() {
		return Or::or;
	}

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
	public static Either<MessageException, Query> query(final IRI focus, final Shape shape, final String query) {

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

			return Right(new JSONLDParser(focus, shape, service(keywords())).parse(query));

		} catch ( final JsonException e ) {

			return Left(status(BadRequest, e));

		} catch ( final NoSuchElementException e ) {

			return Left(status(UnprocessableEntity, e));

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

	private JSONLDFormat() { }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the default MIME type for JSON-LD messages ({@value MIME})
	 */
	@Override public String mime() {
		return MIME;
	}


	/**
	 * Decodes the JSON-LD {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat}
	 * body, if one is available and the {@code message} {@code Content-Type} header is either missing or matched by
	 * {@link JSONFormat#MIMEPattern}
	 *
	 * <p><strong>Warning</strong> / Decoding is completely driven by the {@code message}
	 * {@linkplain JSONLDFormat#shape() shape attribute}: embedded {@code @context} objects are ignored.</p>
	 */
	@Override public Either<MessageException, Frame> decode(final Message<?> message) {
		return message

				.header("Content-Type")

				.filter(JSONFormat.MIMEPattern.asPredicate().or(String::isEmpty))

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset());
							final JsonReader jsonReader=Json.createReader(reader)
					) {

						final IRI focus=iri(message.item());
						final Shape shape=message.get(shape());
						final Map<String, String> keywords=service(keywords());

						final Collection<Statement> model=decode(focus, shape, keywords, jsonReader.readObject());

						return validate(focus, shape, model).fold(

								trace -> Left(status(UnprocessableEntity, trace.toJSON())),

								value -> Right(frame(focus, model)) // use model to include inferred statements

						);

					} catch ( final JsonException e ) {

						if ( e.getCause() instanceof IOException ) {
							throw new UncheckedIOException((IOException)e.getCause());
						}

						return Left(status(BadRequest, e));

					} catch ( final UnsupportedEncodingException e ) {

						return Left(status(BadRequest, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no JSON-LD body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value JSONFormat#MIME}, unless already defined, and
	 * encodes the JSON-LD model {@code value} into the output stream accepted by the {@code message} {@link
	 * OutputFormat} body.
	 *
	 * <p>If the originating {@code message} {@linkplain Message#request() request} includes an {@code Accept-Language}
	 * header, a suitably {@linkplain Shape#localize localized} version of the message shape is used in the conversion
	 * process and only matching tagged literals from {@code value} are included in the response body.</p>
	 *
	 * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
	 * {@linkplain JSONLDFormat#shape() shape attribute} are embedded only if {@code Content-Type} is {@value MIME}.</p>
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Frame value) {

		final String item=message.item();
		final Value focus=value.focus();

		if ( !focus.isIRI() || !focus.stringValue().equals(item) ) {
			throw new IllegalArgumentException(format(
					"message item <%s> and frame focus %s don't match", item, format(focus)
			));
		}

		final Shape shape=message.get(shape());
		final List<String> langs=message.request().langs();

		final boolean global=langs.isEmpty() || langs.contains("*");

		final String mime=message

				.header("Content-Type") // content-type explicitly defined by handler

				.orElseGet(() -> mimes(message.request().header("Accept").orElse("")).stream()

						// application/ld+json or application/json accepted?

						.filter(type -> type.equals(MIME) || type.equals(JSONFormat.MIME)).findFirst()

						// default to application/json

						.orElse(JSONFormat.MIME)

				);


		final Collection<Statement> localized=value.model().filter(statement -> {

			if ( global ) { return true; } else { // retain only tagged literals with an accepted language

				final String lang=lang(statement.getObject());

				return lang.isEmpty() || langs.contains(lang);

			}

		}).collect(toList());

		final Collection<Statement> validated=validate(value.focus(), shape, localized).fold(trace -> {

			throw status(InternalServerError, trace(trace("invalid JSON-LD payload"), trace).toJSON());

		});

		return message

				.header("Content-Type", mime)

				.body(output(), output -> {

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
