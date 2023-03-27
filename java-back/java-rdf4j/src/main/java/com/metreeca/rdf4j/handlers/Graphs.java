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

import com.metreeca.core.toolkits.Identifiers;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.http.codecs.Data;
import com.metreeca.http.handlers.Worker;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.turtle.TurtleParserFactory;
import org.eclipse.rdf4j.rio.turtle.TurtleWriterFactory;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.metreeca.core.toolkits.Lambdas.task;
import static com.metreeca.http.Message.mimes;
import static com.metreeca.http.Response.*;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.codecs.RDF.service;

import static java.lang.String.format;


/**
 * SPARQL 1.1 Graph Store endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Graph Store endpoint exposing the contents of the shared {@linkplain Graph
 * graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public final class Graphs extends Endpoint<Graphs> {

    public Graphs() {
        delegate(new Worker()
                .get(this::get)
                .put(this::put)
                .delete(this::delete)
                .post(this::post)
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
     */
    private Response get(final Request request, final Function<Request, Response> forward) {

        final boolean catalog=request.parameters().isEmpty();

        final String target=graph(request);
        final String accept=request.header("Accept").orElse("");

        if ( target == null && !catalog ) {

            return request.reply(BadRequest, "missing target graph parameter");

        } else if ( !queryable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else if ( catalog ) { // graph catalog

            final IRI focus=iri(request.item());
            final Model model=new LinkedHashModel();

            graph().query(task(connection -> {
                try ( final RepositoryResult<Resource> contexts=connection.getContextIDs() ) {
                    while ( contexts.hasNext() ) {

                        final Resource context=contexts.next();

                        model.add(statement(focus, RDF.VALUE, context));
                        model.add(statement(context, RDF.TYPE, VOID.DATASET));

                    }
                }
            }));

            return request.reply(OK)
                    .body(new com.metreeca.rdf.codecs.RDF(), model);

        } else {

            final RDFWriterFactory factory
                    =service(RDFWriterRegistry.getInstance(), mimes(accept)).orElseGet(TurtleWriterFactory::new);

            final RDFFormat format=factory.getRDFFormat();
            final Resource context=target.isEmpty() ? null : iri(target);

            try ( final ByteArrayOutputStream data=new ByteArrayOutputStream() ) {

                graph().query(task(connection -> connection.export(factory.getWriter(data), context)));

                return graph().query(connection -> request.reply().map(response -> response.status(OK)

                        .header("Content-Type", format.getDefaultMIMEType())
                        .header("Content-Disposition", format("attachment; filename=\"%s.%s\"",
                                target.isEmpty() ? "default" : target, format.getDefaultFileExtension()
                        ))

                        .body(new Data(), data.toByteArray())));

            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }

        }
    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-put
     */
    private Response put(final Request request, final Function<Request, Response> forward) {

        final String target=graph(request);

        if ( target == null ) {

            return request.reply(BadRequest, "missing target graph parameter");

        } else if ( !updatable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else {

            final Resource context=target.isEmpty() ? null : iri(target);
            final String content=request.header("Content-Type").orElse("");

            // !!! If a clients issues a POST or PUT with a content type that is not understood by the
            // !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

            final RDFParserFactory factory=service(RDFParserRegistry.getInstance(), mimes(content))
                    .orElseGet(TurtleParserFactory::new);// !!! review fallback handling

            return graph().update(connection -> { // binary format >> no rewriting
                try ( final InputStream input=request.input().get() ) {

                    final boolean exists=exists(connection, context);

                    connection.clear(context);
                    connection.add(input, request.base(), factory.getRDFFormat(), context);

                    return request.reply().map(response ->
                            response.status(exists ? Response.NoContent :
                                    Response.Created));

                } catch ( final IOException e ) {

                    logger().warning(this, "unable to read RDF payload", e);

                    return request.reply(InternalServerError, e.getMessage());

                } catch ( final RDFParseException e ) {

                    logger().warning(this, "malformed RDF payload", e);

                    return request.reply(BadRequest, e.getMessage());

                } catch ( final RepositoryException e ) {

                    logger().warning(this, "unable to update graph "+context, e);

                    return request.reply(InternalServerError, e.getMessage());

                }
            });
        }

    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-delete
     */
    private Response delete(final Request request, final Function<Request, Response> forward) {

        final String target=graph(request);

        if ( target == null ) {

            return request.reply(BadRequest, "missing target graph parameter");

        } else if ( !updatable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else {

            final Resource context=target.isEmpty() ? null : iri(target);

            return graph().update(connection -> {
                try {

                    final boolean exists=exists(connection, context);

                    connection.clear(context);

                    return request.reply().map(response ->
                            response.status(exists ? Response.NoContent : Response.NotFound));

                } catch ( final RepositoryException e ) {

                    logger().warning(this, "unable to update graph "+context, e);

                    return request.reply(InternalServerError, e.getMessage());

                }
            });
        }

    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
     */
    private Response post(final Request request, final Function<Request, Response> forward) {

        // !!! support  "multipart/form-data"
        // !!! support graph creation with IRI identifying the underlying Graph Store

        final String target=graph(request);

        if ( target == null ) {

            return request.reply(BadRequest, "missing target graph parameter");

        } else if ( !updatable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else {

            final Resource context=target.isEmpty() ? null : iri(target);
            final String content=request.header("Content-Type").orElse("");

            // !!! If a clients issues a POST or PUT with a content type that is not understood by the
            // !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

            final RDFParserFactory factory=service(RDFParserRegistry.getInstance(), mimes(content))
                    .orElseGet(TurtleParserFactory::new); // !!! review fallback


            return graph().update(connection -> { // binary format >> no rewriting
                try ( final InputStream input=request.input().get() ) {

                    final boolean exists=exists(connection, context);

                    connection.add(input, request.base(), factory.getRDFFormat(), context);

                    return request.reply().map(response ->
                            response.status(exists ? Response.NoContent : Response.Created));

                } catch ( final IOException e ) {

                    logger().warning(this, "unable to read RDF payload", e);

                    return request.reply(InternalServerError, e.getMessage());

                } catch ( final RDFParseException e ) {

                    logger().warning(this, "malformed RDF payload", e);

                    return request.reply(BadRequest, e.getMessage());

                } catch ( final RepositoryException e ) {

                    logger().warning(this, "unable to update graph "+context, e);

                    return request.reply(InternalServerError, e.getMessage());

                }
            });

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String graph(final Request request) {

        final List<String> defaults=request.parameters("default");
        final List<String> nameds=request.parameters("graph");

        final boolean dflt=defaults.size() == 1 && defaults.get(0).isEmpty();
        final boolean named=nameds.size() == 1 && Identifiers.AbsoluteIRIPattern.matcher(nameds.get(0)).matches();

        return dflt && named ? null : dflt ? "" : named ? nameds.get(0) : null;
    }

    private boolean exists(final RepositoryConnection connection, final Resource context) {

        try ( final RepositoryResult<Resource> contexts=connection.getContextIDs() ) {

            while ( contexts.hasNext() ) {
                if ( contexts.next().equals(context) ) { return true; }
            }

        }

        return connection.hasStatement(null, null, null, true, context);
    }

}
