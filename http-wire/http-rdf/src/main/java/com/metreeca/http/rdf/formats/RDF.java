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

package com.metreeca.http.rdf.formats;

import com.metreeca.http.Format;
import com.metreeca.http.FormatException;
import com.metreeca.http.Message;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleParserFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.Response.BadRequest;
import static com.metreeca.http.rdf.Values.iri;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;


/**
 * RDF message format.
 */
public final class RDF implements Format<Model> {

    /**
     * The default MIME type for RDF messages ({@value}).
     */
    public static final String MIME="text/turtle";


    /**
     * Locates a file format service in a registry.
     *
     * @param registry the registry the file format service is to be located from
     * @param types    the suggested MIME types for the file format service to be located
     * @param <F>      the type of the file format services listed by the {@code registry}
     * @param <S>      the type of the file format service to be located
     *
     * @return the located file format service or an empty optional if no service matches one of the given {@code types}
     *
     * @throws NullPointerException if any parameter is null
     */
    public static <F extends FileFormat, S> Optional<S> service(
            final FileFormatServiceRegistry<F, S> registry, final Collection<String> types
    ) {

        if ( registry == null ) {
            throw new NullPointerException("null registry");
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

        return types.stream()

                .map(type -> type.equals("*/*") ? Optional.<F>empty()
                        : type.endsWith("/*") ? matcher.apply(type.substring(0, type.indexOf('/')+1))
                        : registry.getFileFormatForMIMEType(type)
                )

                .flatMap(Optional::stream)
                .findFirst()

                .flatMap(registry::get);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses an RDF document.
     *
     * @param url  the URL of the document to be parsed
     * @param base the base IRI relative IRIs in {@code url} will be resolved against; if empty, defaults to the
     *             {@code url}
     *
     * @return an unmodifiable RDF model parsed from {@code url}
     *
     * @throws NullPointerException     if {@code url} or {@code base} is null
     * @throws IllegalArgumentException if {@code url} is malformed
     * @throws FormatException          if {@code url} points to a malformed document
     */
    public static Model rdf(final URL url, final String base) throws FormatException {

        if ( url == null ) {
            throw new NullPointerException("null url");
        }

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        final RDFParserRegistry registry=RDFParserRegistry.getInstance();

        final RDFParser parser=registry
                .getFileFormatForFileName(url.getFile())
                .flatMap(registry::get)
                .orElseGet(TurtleParserFactory::new)
                .getParser();

        return rdf(url, base, parser);
    }

    /**
     * Parses an RDF document.
     *
     * @param url    the URL of the document to be parsed
     * @param base   the base IRI relative IRIs in {@code url} will be resolved against; if empty, defaults to the
     *               {@code url}
     * @param parser the RDF parser
     *
     * @return an unmodifiable RDF model parsed from {@code reader}
     *
     * @throws NullPointerException if any parameter is null
     * @throws FormatException      if {@code reader} contains a malformed documentthrows FormatException
     */
    public static Model rdf(final URL url, final String base, final RDFParser parser) throws FormatException {

        if ( url == null ) {
            throw new NullPointerException("null url");
        }

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        if ( parser == null ) {
            throw new NullPointerException("null parser");
        }

        try ( final InputStream input=url.openConnection().getInputStream() ) {

            return rdf(input, base.isEmpty() ? url.toString() : base, parser);

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Parses an RDF document.
     *
     * @param reader the reader the RDF document is to be parsed from
     * @param base   the base IRI relative IRIs in {@code url} will be resolved against; may be empty
     * @param parser the RDF parser
     *
     * @return an unmodifiable RDF model parsed from {@code reader}
     *
     * @throws NullPointerException if any parameter is null
     * @throws FormatException      if {@code reader} contains a malformed documentthrows FormatException
     */
    public static Model rdf(final Reader reader, final String base, final RDFParser parser) {

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        if ( parser == null ) {
            throw new NullPointerException("null parser");
        }

        return rdf(parser, p -> {

            try { p.parse(reader, base); } catch ( final IOException e ) { throw new UncheckedIOException(e); }

        });
    }

    /**
     * Parses an RDF document.
     *
     * @param input  the input stream the RDF document is to be parsed from
     * @param base   the base IRI relative IRIs in {@code url} will be resolved against; may be empty
     * @param parser the RDF parser
     *
     * @return an unmodifiable RDF model parsed from {@code input}
     *
     * @throws NullPointerException if any parameter is null
     * @throws FormatException      if {@code input} contains a malformed document
     */
    public static Model rdf(final InputStream input, final String base, final RDFParser parser) throws FormatException {

        if ( input == null ) {
            throw new NullPointerException("null input");
        }

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        if ( parser == null ) {
            throw new NullPointerException("null parser");
        }

        return rdf(parser, p -> {

            try { p.parse(input, base); } catch ( final IOException e ) { throw new UncheckedIOException(e); }

        });
    }


    private static Model rdf(final RDFParser parser, final Consumer<RDFParser> processor) throws FormatException {

        final ParseErrorCollector errorCollector=new ParseErrorCollector();

        parser.setParseErrorListener(errorCollector);

        final Model model=new LinkedHashModel(); // order-preserving and writable

        parser.setRDFHandler(new AbstractRDFHandler() {

            @Override public void handleNamespace(final String prefix, final String uri) {
                model.setNamespace(prefix, uri);
            }

            @Override public void handleStatement(final Statement statement) {
                model.add(statement);
            }

        });

        try {

            processor.accept(parser);

        } catch ( final RDFParseException e ) {

            if ( errorCollector.getFatalErrors().isEmpty() ) { // exception not always reported by parser…
                errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
            }

        }

        final List<String> fatals=errorCollector.getFatalErrors();
        final List<String> errors=errorCollector.getErrors();
        final List<String> warnings=errorCollector.getWarnings();

        if ( fatals.isEmpty() ) {

            return model.unmodifiable();

        } else { // !!! log warnings/error/fatals?

            throw new FormatException(BadRequest, Stream

                    .of(
                            fatals.stream().map(fatal -> format("!!! %s", fatal)),
                            errors.stream().map(error -> format(" !! %s", error)),
                            warnings.stream().map(warning -> format("  ! %s", warning))
                    )

                    .flatMap(stream -> stream)

                    .collect(joining("\n", format("%s\n", parser.getRDFFormat().toString()), ""))

            );

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Consumer<RioConfig> customizer;


    /**
     * Creates an RDF message format.
     */
    public RDF() {
        this(options -> { });
    }

    /**
     * Creates a customized RDF message format.
     *
     * @param customizer the RDF parser/writer customizer; takes as argument a customizable RIO configuration
     *
     * @throws NullPointerException if {@code customizer} is null
     */
    public RDF(final Consumer<RioConfig> customizer) {

        if ( customizer == null ) {
            throw new NullPointerException("null customizer");
        }

        this.customizer=customizer;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Class<Model> type() {
        return Model.class;
    }

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }


    /**
     * @return the RDF payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account the
     * RDF serialization format defined by the  {@code "Content-Type"} {@code message} header or an empty optional if
     * the {@code "Content-Type"} {@code message} is not empty and is not associated with an RDF format in the
     * {@link RDFParserRegistry}
     */
    @Override public Optional<Model> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(MIME))

                .flatMap(type -> service(RDFParserRegistry.getInstance(), mimes(type)))

                .map(factory -> {

                    final IRI focus=iri(message.item());
                    final String base=focus.stringValue();

                    final RDFParser parser=factory.getParser();

                    customizer.accept(parser.getParserConfig());

                    try ( final InputStream input=message.input().get() ) {

                        return rdf(input, base, parser);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the RDF
     * {@code value}, taking into account the RDF serialization selected according to the {@code "Accept"} header of the
     * {@code message} originating request, defaulting to {@code text/turtle}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Model value) {

        final List<String> types=mimes(message.request().header("Accept").orElse(""));

        final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
        final RDFWriterFactory factory=service(registry, types).orElseGet(TurtleWriterFactory::new);

        final IRI focus=iri(message.item());
        final String base=focus.stringValue(); // relativize IRIs wrt the response focus

        return message

                // try to set content type to the actual type requested even if it's not the default one

                .header("Content-Type", types.stream()
                        .filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
                        .findFirst()
                        .orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType())
                )

                .output(output -> {

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
