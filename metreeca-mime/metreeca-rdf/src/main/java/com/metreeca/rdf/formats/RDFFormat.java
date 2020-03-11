/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.formats;

import com.metreeca.rdf.Formats;
import com.metreeca.rdf.Values;
import com.metreeca.rdf.wrappers.Rewriter;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.OutputFormat;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.*;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.json.*;

import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONFormat.context;
import static com.metreeca.rest.formats.OutputFormat.output;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


/**
 * RDF body format.
 */
public final class RDFFormat extends Format<Collection<Statement>> {

	/**
	 * The plain <a href="http://www.json.org/">JSON</a> file format.
	 *
	 * The file extension {@code .json} is recommend for JSON documents. The media type is {@code application/json}. The
	 * character encoding is {@code UTF-8}.
	 */
	public static final org.eclipse.rdf4j.rio.RDFFormat RDFJSONFormat=new org.eclipse.rdf4j.rio.RDFFormat("JSON",
			asList("application/json", "text/json"),
			StandardCharsets.UTF_8,
			singletonList("json"),
			Values.iri("http://www.json.org/"),
			org.eclipse.rdf4j.rio.RDFFormat.NO_NAMESPACES,
			org.eclipse.rdf4j.rio.RDFFormat.NO_CONTEXTS
	);

	/**
	 * Sets the focus resource for codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Resource> RioFocus=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Focus", "Resource focus", null
	);

	/**
	 * Sets the expected shape for the resources handled by codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Shape> RioShape=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Shape", "Resource shape", null
	);

	/**
	 * Sets the expected JSON-LD context for codecs.
	 *
	 * <p>Defaults to an empty object.</p>
	 */
	public static final RioSetting<JsonObject> RioContext=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Context", "JSON-LD context", JsonValue.EMPTY_JSON_OBJECT
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Custom header providing the external base for on-demand RDF body rewriting.
	 *
	 * @see Rewriter
	 */
	public static final String ExternalBase="-External-Base"; // !!! review/remove/hide


	/**
	 * Creates an RDF body format.
	 *
	 * @return a new RDF body format
	 */
	public static RDFFormat rdf() {
		return new RDFFormat();
	}


	///// Casts ////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts an object to an IRI.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return an IRI obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to an IRI
	 */
	public static IRI iri(final Object object) {
		return as(object, IRI.class);
	}

	/**
	 * Converts an object to a value.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return a value obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to a value
	 */
	public static Value value(final Object object) {
		return as(object, Value.class);
	}


	private static <T> T as(final Object object, final Class<T> type) {
		if ( object == null || type.isInstance(object) ) {

			return type.cast(object);

		} else {

			throw new UnsupportedOperationException(String.format("unsupported type {%s} / expected %s",
					object.getClass().getName(), type.getName()
			));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final InputFormat input=input();
	private final OutputFormat output=output();

	private final JsonObject context=service(context());


	private RDFFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public List<IRI> path(final String base, final Shape shape, final String path) {
		return new RDFJSONDecoder(base, context) {}.path(path, shape); // !!! pass base as argument and factor decoder instance
	}

	@Override public Object value(final String base, final Shape shape, final JsonValue value) {
		return new RDFJSONDecoder(base, context) {}.value(value, shape, null).getKey(); // !!! pass base as argument and factor decoder instance
	}


	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link InputFormat}
	 * representation, if present;  a failure reporting RDF processing errors with the {@link Response#BadRequest}
	 * status, otherwise
	 */
	@Override public Result<Collection<Statement>, Failure> get(final Message<?> message) {
		return message.body(input).fold(

				supplier -> {

					final IRI focus=Values.iri(message.item());
					final Shape shape=message.shape();

					final String base=focus.stringValue();
					final String type=message.header("Content-Type").orElse("");

					final RDFParser parser=Formats
							.service(RDFParserRegistry.getInstance(), TURTLE, type)
							.getParser();

					parser.set(RioShape, shape);
					parser.set(RioFocus, focus);
					parser.set(RioContext, context);

					parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
					parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

					parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
					parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

					final ParseErrorCollector errorCollector=new ParseErrorCollector();

					parser.setParseErrorListener(errorCollector);

					final Collection<Statement> model=new LinkedHashModel(); // order-preserving and writable

					final String internal=message.request().base();
					final String external=message.header(ExternalBase).orElse(internal); // made available by Rewriter

					parser.setRDFHandler(external.equals(internal) ? new AbstractRDFHandler() {

						@Override public void handleStatement(final Statement statement) {
							model.add(statement);
						}

					} : new AbstractRDFHandler() {

						@Override public void handleStatement(final Statement statement) {
							model.add(rewrite(external, internal, statement));
						}

					});

					try (final InputStream input=supplier.get()) {

						parser.parse(input, base); // resolve relative IRIs wrt the request focus

					} catch ( final RDFParseException e ) {

						if ( errorCollector.getFatalErrors().isEmpty() ) { // exception possibly not reported by parser…
							errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
						}

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

					final List<String> fatals=errorCollector.getFatalErrors();
					final List<String> errors=errorCollector.getErrors();
					final List<String> warnings=errorCollector.getWarnings();

					if ( fatals.isEmpty() ) { // return model

						return Value(model);

					} else { // report errors // !!! log warnings/error/fatals?

						final JsonObjectBuilder trace=Json.createObjectBuilder()

								.add("format", parser.getRDFFormat().getDefaultMIMEType());

						if ( !fatals.isEmpty() ) { trace.add("fatals", Json.createArrayBuilder(fatals)); }
						if ( !errors.isEmpty() ) { trace.add("errors", Json.createArrayBuilder(errors)); }
						if ( !warnings.isEmpty() ) { trace.add("warnings", Json.createArrayBuilder(warnings)); }

						return Error(new Failure()
								.status(Response.BadRequest)
								.error(Failure.BodyMalformed)
								.trace(trace.build()));

					}

				},

				Result::Error

		);
	}

	/**
	 * Configures the {@link OutputFormat} representation of {@code message} to write the RDF {@code value} to the
	 * accepted output stream and, unless already defined, sets the {@code Content-Type} header to the MIME type of the
	 * RDF serialization selected according to the {@code Accept} header of the request associated to the message, if
	 * one is present, or to {@code "text/turtle"}, otherwise.
	 */
	@Override public <M extends Message<M>> M set(final M message, final Collection<Statement> value) {

		final List<String> types=Formats.types(message.request().headers("Accept"));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, TURTLE, types);

		return message

				// try to set content type to the actual type requested even if it's not the default one

				.header("~Content-Type", types.stream()
						.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
						.findFirst()
						.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType())
				)

				.body(output, target -> {

					final IRI focus=Values.iri(message.item());
					final Shape shape=message.shape();

					final String base=focus.stringValue();

					final String internal=message.request().base();
					final String external=message.header(ExternalBase).orElse(internal); // made available by Rewriter

					try (final OutputStream output=target.get()) {

						final RDFWriter writer=factory.getWriter(output, base); // relativize IRIs wrt the response focus

						writer.set(RioShape, shape);
						writer.set(RioFocus, focus);
						writer.set(RioContext, context);

						Rio.write(external.equals(internal) ? value : rewrite(internal, external, value), writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("unsupported base IRI {"+base+"}", e);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Iterable<Statement> rewrite(final String source, final String target, final Iterable<Statement> statements) {
		return () -> new Iterator<Statement>() {

			private final Iterator<Statement> iterator=statements.iterator();

			@Override public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override public Statement next() {
				return rewrite(source, target, iterator.next());
			}

		};
	}


	private Statement rewrite(final String source, final String target, final Statement statement) {
		return statement == null ? null : statement(
				rewrite(source, target, statement.getSubject()),
				rewrite(source, target, statement.getPredicate()),
				rewrite(source, target, statement.getObject()),
				rewrite(source, target, statement.getContext())
		);
	}

	private Value rewrite(final String source, final String target, final Value value) {
		return value instanceof IRI ? rewrite(source, target, (IRI)value) : value;
	}

	private Resource rewrite(final String source, final String target, final Resource resource) {
		return resource instanceof IRI ? rewrite(source, target, (IRI)resource) : resource;
	}

	private IRI rewrite(final String source, final String target, final IRI iri) {
		return iri == null ? null : Values.iri(rewrite(source, target, iri.stringValue()));
	}

	private String rewrite(final String source, final String target, final String string) {
		return string.startsWith(source) ? target+string.substring(source.length()) : string;
	}

}