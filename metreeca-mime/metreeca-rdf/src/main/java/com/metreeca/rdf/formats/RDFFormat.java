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

package com.metreeca.rdf.formats;

import com.metreeca.rest.*;
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.OutputFormat;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.metreeca.link.Values.iri;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;


/**
 * RDF message format.
 */
public final class RDFFormat extends Format<Collection<Statement>> {

	/**
	 * The default MIME type for RDF messages ({@value}).
	 */
	public static final String MIME="text/turtle";


	/**
	 * Locates a file format service in a registry.
	 *
	 * @param registry the registry the file format service is to be located from
	 * @param fallback the fallback file format to be used as key if no service in the registry is matched by one of
	 *                 the suggested MIME {@code types}
	 * @param types    the suggested MIME types for the file format service to be located
	 * @param <F>      the type of the file format services listed by the {@code registry}
	 * @param <S>      the type of the file file format service to be located
	 *
	 * @return the located file format service
	 *
	 * @throws NullPointerException if any parameter is null
	 */
	public static <F extends FileFormat, S> S service(
			final FileFormatServiceRegistry<F, S> registry, final F fallback, final Collection<String> types
	) {

		if ( registry == null ) {
			throw new NullPointerException("null registry");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		if ( types == null || types.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null types");
		}

		final Function<String, Optional<F>> matcher=prefix -> {

			for (final F format : registry.getKeys()) { // first try to match with the default MIME type
				if ( format.getDefaultMIMEType().toLowerCase(Locale.ROOT).startsWith(prefix) ) {
					return Optional.of(format);
				}
			}

			for (final F format : registry.getKeys()) { // try alternative MIME types too
				for (final String type : format.getMIMETypes()) {
					if ( type.toLowerCase(Locale.ROOT).startsWith(prefix) ) {
						return Optional.of(format);
					}
				}
			}

			return Optional.empty();

		};

		return registry

				.get(types.stream()

						.map(type -> type.equals("*/*") ? Optional.of(fallback)
								: type.endsWith("/*") ? matcher.apply(type.substring(0, type.indexOf('/')+1))
								: registry.getFileFormatForMIMEType(type)
						)

						.filter(Optional::isPresent)
						.map(Optional::get)
						.findFirst()

						.orElse(fallback)
				)

				.orElseThrow(() -> new IllegalArgumentException(String.format(
						"unsupported fallback format <%s>", fallback.getDefaultMIMEType()
				)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an RDF message format.
	 *
	 * @return a new RDF message format
	 */
	public static RDFFormat rdf() {
		return rdf(options -> {});
	}

	/**
	 * Creates a customized RDF message format.
	 *
	 * @param customizer the RDF parser/writer customizer; takes as argument a customizable RIO configuration
	 *
	 * @return a new customized RDF message format
	 */
	public static RDFFormat rdf(final Consumer<RioConfig> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		return new RDFFormat(customizer);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses an RDF document.
	 *
	 * @param input  the input stream the RDF document is to be parsed from
	 * @param base   the possibly null base URL for the RDF document to be parsed
	 * @param parser the RDF parser
	 *
	 * @return either a parsing exception or the RDF model parsed from {@code input}
	 *
	 * @throws NullPointerException if either {@code input} or {@code parser} is null
	 */
	public static Either<MessageException, Collection<Statement>> rdf(
			final InputStream input, final String base, final RDFParser parser
	) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		final ParseErrorCollector errorCollector=new ParseErrorCollector();

		parser.setParseErrorListener(errorCollector);

		final Collection<Statement> model=new LinkedHashModel(); // order-preserving and writable

		parser.setRDFHandler(new AbstractRDFHandler() {

			@Override public void handleStatement(final Statement statement) {
				model.add(statement);
			}

		});

		try {

			parser.parse(input, base); // resolve relative IRIs wrt the request focus

		} catch ( final RDFParseException e ) {

			if ( errorCollector.getFatalErrors().isEmpty() ) { // exception not always reported by parser…
				errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
			}

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}

		final List<String> fatals=errorCollector.getFatalErrors();
		final List<String> errors=errorCollector.getErrors();
		final List<String> warnings=errorCollector.getWarnings();

		if ( fatals.isEmpty() ) { // return model

			return Right(model);

		} else { // report errors // !!! log warnings/error/fatals?

			final JsonObjectBuilder trace=Json.createObjectBuilder()

					.add("format", parser.getRDFFormat().getDefaultMIMEType());

			if ( !fatals.isEmpty() ) { trace.add("fatals", Json.createArrayBuilder(fatals)); }
			if ( !errors.isEmpty() ) { trace.add("errors", Json.createArrayBuilder(errors)); }
			if ( !warnings.isEmpty() ) { trace.add("warnings", Json.createArrayBuilder(warnings)); }

			return Left(status(BadRequest, trace.build()));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Consumer<RioConfig> customizer;


	private RDFFormat(final Consumer<RioConfig> customizer) {
		this.customizer=customizer;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the default MIME type for RDF messages ({@value MIME})
	 */
	@Override public String mime() {
		return MIME;
	}


	/**
     * Decodes the RDF {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
     * body, if one is available, taking into account the RDF serialization format defined by the {@code message} {@code
     * Content-Type} header, defaulting to {@code text/turtle}
     */
    @Override public <M extends Message<M>> Either<MessageException, Collection<Statement>> decode(final M message) {
        return message.body(input()).fold(error -> Left(status(UnsupportedMediaType, "no RDF body")), source -> {

            final IRI focus=iri(message.item());

            final String base=focus.stringValue();
            final String type=message.header("Content-Type").orElse("");

            final RDFParser parser=service(RDFParserRegistry.getInstance(), TURTLE, mimes(type)).getParser();

            customizer.accept(parser.getParserConfig());

			try ( final InputStream input=source.get() ) {

				return rdf(input, base, parser);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}

		});
	}

	/**
	 * Configures {@code message} {@code Content-Type} header, unless already defined, and encodes
	 * the RDF {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the RDF serialization selected according to the {@code Accept} header of the {@code message}
	 * originating request, defaulting to {@code text/turtle}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

		final List<String> types=mimes(message.request().header("Accept").orElse(""));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=service(registry, TURTLE, types);

		final IRI focus=iri(message.item());
		final String base=focus.stringValue(); // relativize IRIs wrt the response focus

		return message

				// try to set content type to the actual type requested even if it's not the default one

				.header("Content-Type", types.stream()
						.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
						.findFirst()
						.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType())
				)

				.body(output(), output -> {

					try {

						final RDFWriter writer=factory.getWriter(output, base);

						customizer.accept(writer.getWriterConfig());

						Rio.write(value, writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("malformed base IRI {"+base+"}", e);
					}

				});
	}

}
