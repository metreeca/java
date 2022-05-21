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

package com.metreeca.rdf.codecs;

import com.metreeca.rest.*;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.metreeca.link.Values.iri;
import static com.metreeca.rest.Message.mimes;
import static com.metreeca.rest.Response.BadRequest;


/**
 * RDF message codec.
 */
public final class RDF implements Codec<Collection<Statement>> {

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
     * @param input  the input stream the RDF document is to be parsed from
     * @param base   the possibly null base URL for the RDF document to be parsed
     * @param parser the RDF parser
     *
     * @return the RDF model parsed from {@code input}
     *
     * @throws NullPointerException if either {@code input} or {@code parser} is null
     * @throws CodecException       if {@code input} contains a malformed document
     */
    public static Collection<Statement> rdf(
            final InputStream input, final String base, final RDFParser parser
    ) throws CodecException {

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

        if ( fatals.isEmpty() ) {

            return model;

        } else { // !!! log warnings/error/fatals?

            final JsonObjectBuilder trace=Json.createObjectBuilder()

                    .add("format", parser.getRDFFormat().getDefaultMIMEType());

            if ( !fatals.isEmpty() ) { trace.add("fatals", Json.createArrayBuilder(fatals)); }
            if ( !errors.isEmpty() ) { trace.add("errors", Json.createArrayBuilder(errors)); }
            if ( !warnings.isEmpty() ) { trace.add("warnings", Json.createArrayBuilder(warnings)); }

            final StringWriter writer=new StringWriter();

            Json.createWriter(writer).write(trace.build());

            throw new CodecException(BadRequest, writer.toString()); // !!! JSON MIME

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Consumer<RioConfig> customizer;


    /**
     * Creates an RDF message codec.
     */
    public RDF() {
        this(options -> { });
    }

    /**
     * Creates a customized RDF message codec.
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

    @SuppressWarnings("unchecked")
    @Override public Class<Collection<Statement>> type() {
        return (Class<Collection<Statement>>)(Class<?>)Collection.class;
    }

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }


    /**
     * @return the RDF payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account the
     * RDF serialization format defined by the  {@code "Content-Type"} {@code message} header or an empty optional if the
     * {@code "Content-Type"} {@code message} is not associated with an RDF format in the {@link RDFParserRegistry}
     */
    @Override public Optional<Collection<Statement>> decode(final Message<?> message) {
        return message.header("Content-Type")

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
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the RDF {@code
     * value}, taking into account the RDF serialization selected according to the {@code "Accept"} header of the {@code
     * message} originating request, defaulting to {@code text/turtle}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

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
