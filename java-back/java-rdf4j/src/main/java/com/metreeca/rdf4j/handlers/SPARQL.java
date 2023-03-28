/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.rdf4j.handlers;

import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.http.formats.Data;
import com.metreeca.http.handlers.Worker;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLBooleanJSONWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriterFactory;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.core.toolkits.Lambdas.guarded;
import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.Response.*;
import static com.metreeca.rdf.formats.RDF.service;


/**
 * SPARQL 1.1 Query/Update endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the shared {@linkplain Graph
 * graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public final class SPARQL extends Endpoint<SPARQL> {

    private Consumer<Operation> options=operation -> { };


    public SPARQL() {
        delegate(new Worker()
                .get(this::process)
                .post(this::process)
        );
    }


    /**
     * Configures the options for this endpoint.
     *
     * @param options an options configurator; takes as argument the SPARQL operation to be configured
     *
     * @return this endpoint
     *
     * @throws NullPointerException if {@code options} is null
     */
    public SPARQL options(final Consumer<Operation> options) {

        if ( options == null ) {
            throw new NullPointerException("null options");
        }

        this.options=options;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Response process(final Request request, final Function<Request, Response> forward) {
        return graph().query(connection -> {
            try {

                final Operation operation=operation(request, connection);

                if ( operation == null ) { // !!! return void description for GET

                    return request.reply(BadRequest, "missing query/update parameter");

                } else if ( operation instanceof Query && !queryable(request.roles())
                        || operation instanceof Update && !updatable(request.roles())
                ) {

                    return request.reply().map(response -> response.status(Unauthorized));

                } else if ( operation instanceof BooleanQuery ) {

                    return process(request, (BooleanQuery)operation);

                } else if ( operation instanceof TupleQuery ) {

                    return process(request, (TupleQuery)operation);

                } else if ( operation instanceof GraphQuery ) {

                    return process(request, (GraphQuery)operation);

                } else if ( operation instanceof Update ) {

                    if ( connection.isActive() ) {

                        return process(request, (Update)operation);

                    } else {

                        try {

                            connection.begin();

                            final Response response=process(request, (Update)operation);

                            connection.commit();

                            return response;

                        } finally {

                            if ( connection.isActive() ) { connection.rollback(); }

                        }

                    }


                } else {

                    return request.reply(NotImplemented, operation.getClass().getName());

                }

            } catch ( final MalformedQueryException|IllegalArgumentException e ) {

                return request.reply(BadRequest, e.getMessage());

            } catch ( final UnsupportedOperationException e ) {

                return request.reply(NotImplemented, e.getMessage());

            } catch ( final RuntimeException e ) {

                // !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

                return request.reply(InternalServerError, e.getMessage());

            }
        });
    }


    private Operation operation(final Request request, final RepositoryConnection connection) {

        final Optional<String> query=request.parameter("query");
        final Optional<String> update=request.parameter("update");
        final Optional<String> infer=request.parameter("infer");
        final Optional<String> timeout=request.parameter("timeout");

        final Collection<String> basics=request.parameters("default-graph-uri");
        final Collection<String> nameds=request.parameters("named-graph-uri");

        final Operation operation=query.isPresent() ? connection.prepareQuery(query.get())
                : update.map(connection::prepareUpdate).orElse(null);

        if ( operation != null ) {

            final ValueFactory factory=connection.getValueFactory();
            final SimpleDataset dataset=new SimpleDataset();

            basics.stream().distinct().forEachOrdered(basic -> dataset.addDefaultGraph(factory.createIRI(basic)));
            nameds.stream().distinct().forEachOrdered(named -> dataset.addNamedGraph(factory.createIRI(named)));

            operation.setDataset(dataset);
            operation.setMaxExecutionTime(timeout.map(guarded(Integer::valueOf)).filter(v -> v > 0).orElse(60));
            operation.setIncludeInferred(infer.map(Boolean::parseBoolean).orElse(true));

            options.accept(operation);

        }

        return operation;

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Response process(final Request request, final BooleanQuery query) {

        final String accept=request.header("Accept").orElse("");

        final BooleanQueryResultWriterFactory factory
                =service(BooleanQueryResultWriterRegistry.getInstance(), mimes(accept))
                .orElseGet(SPARQLBooleanJSONWriterFactory::new);

        try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

            factory.getWriter(output).handleBoolean(query.evaluate());

            return request.reply().map(response -> response.status(OK)
                    .header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
                    .body(new Data(), output.toByteArray()));

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }

    }

    private Response process(final Request request, final TupleQuery query) {

        final String accept=request.header("Accept").orElse("");

        final TupleQueryResultWriterFactory factory
                =service(TupleQueryResultWriterRegistry.getInstance(), mimes(accept))
                .orElseGet(SPARQLResultsJSONWriterFactory::new);

        try (
                final TupleQueryResult result=query.evaluate();
                final ByteArrayOutputStream output=new ByteArrayOutputStream()
        ) {

            final TupleQueryResultWriter writer=factory.getWriter(output);

            writer.startDocument();
            writer.startQueryResult(result.getBindingNames());

            while ( result.hasNext() ) { writer.handleSolution(result.next()); }

            writer.endQueryResult();

            return request.reply().map(response -> response.status(OK)
                    .header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
                    .body(new Data(), output.toByteArray()));

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    private Response process(final Request request, final GraphQuery query) {


        final String accept=request.header("Accept").orElse("");

        final RDFWriterFactory factory
                =service(RDFWriterRegistry.getInstance(), mimes(accept))
                .orElseGet(NTriplesWriterFactory::new);


        try (
                final GraphQueryResult result=query.evaluate();
                final ByteArrayOutputStream output=new ByteArrayOutputStream()
        ) {

            final RDFWriter writer=factory.getWriter(output);

            writer.startRDF();

            for (final Map.Entry<String, String> entry : result.getNamespaces().entrySet()) {
                writer.handleNamespace(entry.getKey(), entry.getValue());
            }

            while ( result.hasNext() ) { writer.handleStatement(result.next()); }

            writer.endRDF();

            return request.reply().map(response -> response.status(OK)
                    .header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
                    .body(new Data(), output.toByteArray()));

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }

    }

    private Response process(final Request request, final Update update) {

        final String accept=request.header("Accept").orElse("");

        final BooleanQueryResultWriterFactory factory
                =service(BooleanQueryResultWriterRegistry.getInstance(), mimes(accept))
                .orElseGet(SPARQLBooleanJSONWriterFactory::new);

        try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

            update.execute();

            factory.getWriter(output).handleBoolean(true);

            return request.reply().map(response -> response.status(OK)
                    .header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
                    .body(new Data(), output.toByteArray()));

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

}
