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

package com.metreeca.rdf4j.handlers;

import com.metreeca.core.Feeds;
import com.metreeca.core.Identifiers;
import com.metreeca.json.Shape;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.rio.*;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.core.Lambdas.task;
import static com.metreeca.json.Shape.exactly;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.Format.mimes;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.handlers.Router.router;

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

    private static final Shape GraphsShape=field(RDF.VALUE,
            field(RDF.TYPE), exactly(VOID.DATASET)
    );


    /**
     * Creates a graph store endpoint
     *
     * @return a new graph store endpoint
     */
    public static Graphs graphs() {
        return new Graphs();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Graphs() {
        delegate(router()
                .get((request, next) -> get(request))
                .put((request1, next) -> put(request1))
                .delete((request2, next) -> delete(request2))
                .post((request3, next) -> post(request3))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
     */
    private Response get(final Request request) {

        final boolean catalog=request.parameters().isEmpty();

        final String target=graph(request);
        final String accept=request.header("Accept").orElse("");

        if ( target == null && !catalog ) {

            return request.reply().map(status(BadRequest, "missing target graph parameter"));

        } else if ( !queryable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else if ( catalog ) { // graph catalog

            final IRI focus=iri(request.item());
            final Collection<Statement> model=new ArrayList<>();

            graph().query(task(connection -> {
                try ( final RepositoryResult<Resource> contexts=connection.getContextIDs() ) {
                    while ( contexts.hasNext() ) {

                        final Resource context=contexts.next();

                        model.add(statement(focus, RDF.VALUE, context));
                        model.add(statement(context, RDF.TYPE, VOID.DATASET));

                    }
                }
            }));

            return request.reply().map(response -> response.status(Response.OK)
                    .set(JSONLDFormat.shape(), GraphsShape)
                    .body(rdf(), model));

        } else {

            final RDFWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
                    RDFWriterRegistry.getInstance(), RDFFormat.TURTLE, mimes(accept)
            );

            final RDFFormat format=factory.getRDFFormat();
            final Resource context=target.isEmpty() ? null : iri(target);


            try ( final ByteArrayOutputStream data=new ByteArrayOutputStream() ) {

                graph().query(task(connection -> connection.export(factory.getWriter(data), context)));

                return graph().query(connection -> request.reply().map(response -> response.status(Response.OK)

                        .header("Content-Type", format.getDefaultMIMEType())
                        .header("Content-Disposition", format("attachment; filename=\"%s.%s\"",
                                target.isEmpty() ? "default" : target, format.getDefaultFileExtension()
                        ))

                        .body(data(), data.toByteArray())));

            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }

        }
    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-put
     */
    private Response put(final Request request) {

        final String target=graph(request);

        if ( target == null ) {

            return request.reply().map(status(BadRequest, "missing target graph parameter"));

        } else if ( !updatable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else {

            final Resource context=target.isEmpty() ? null : iri(target);
            final String content=request.header("Content-Type").orElse("");

            // !!! If a clients issues a POST or PUT with a content type that is not understood by the
            // !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

            final RDFParserFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
                    RDFParserRegistry.getInstance(), RDFFormat.TURTLE, mimes(content) // !!! review fallback
                    // handling
            );

            return graph().update(connection -> { // binary format >> no rewriting
                try ( final InputStream input=request.body(input()).fold(e -> Feeds.input(), Supplier::get) ) {

                    final boolean exists=exists(connection, context);

                    connection.clear(context);
                    connection.add(input, request.base(), factory.getRDFFormat(), context);

                    return request.reply().map(response ->
                            response.status(exists ? Response.NoContent :
                                    Response.Created));

                } catch ( final IOException e ) {

                    logger().warning(this, "unable to read RDF payload", e);

                    return request.reply().map(status(InternalServerError, e));

                } catch ( final RDFParseException e ) {

                    logger().warning(this, "malformed RDF payload", e);

                    return request.reply().map(status(BadRequest, e));

                } catch ( final RepositoryException e ) {

                    logger().warning(this, "unable to update graph "+context, e);

                    return request.reply().map(status(InternalServerError, e));

                }
            });
        }

    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-delete
     */
    private Response delete(final Request request) {

        final String target=graph(request);

        if ( target == null ) {

            return request.reply().map(status(BadRequest, "missing target graph parameter"));

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

                    return request.reply().map(status(InternalServerError, e));

                }
            });
        }

    }

    /*
     * https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
     */
    private Response post(final Request request) {

        // !!! support  "multipart/form-data"
        // !!! support graph creation with IRI identifying the underlying Graph Store

        final String target=graph(request);

        if ( target == null ) {

            return request.reply().map(status(BadRequest, "missing target graph parameter"));

        } else if ( !updatable(request.roles()) ) {

            return request.reply().map(response -> response.status(Response.Unauthorized));

        } else {

            final Resource context=target.isEmpty() ? null : iri(target);
            final String content=request.header("Content-Type").orElse("");

            // !!! If a clients issues a POST or PUT with a content type that is not understood by the
            // !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

            final RDFParserFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
                    RDFParserRegistry.getInstance(), RDFFormat.TURTLE, mimes(content) // !!! review fallback
            );

            return graph().update(connection -> { // binary format >> no rewriting
                try ( final InputStream input=request.body(input()).fold(e -> Feeds.input(),
                        Supplier::get) ) {

                    final boolean exists=exists(connection, context);

                    connection.add(input, request.base(), factory.getRDFFormat(), context);

                    return request.reply().map(response ->
                            response.status(exists ? Response.NoContent : Response.Created));

                } catch ( final IOException e ) {

                    logger().warning(this, "unable to read RDF payload", e);

                    return request.reply().map(status(InternalServerError, e));

                } catch ( final RDFParseException e ) {

                    logger().warning(this, "malformed RDF payload", e);

                    return request.reply().map(status(BadRequest, e));

                } catch ( final RepositoryException e ) {

                    logger().warning(this, "unable to update graph "+context, e);

                    return request.reply().map(status(InternalServerError, e));

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
