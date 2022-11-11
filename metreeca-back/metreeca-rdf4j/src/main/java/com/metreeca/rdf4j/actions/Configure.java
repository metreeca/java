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

package com.metreeca.rdf4j.actions;

import com.metreeca.core.services.Logger;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.core.services.Logger.time;
import static com.metreeca.core.toolkits.Lambdas.task;
import static com.metreeca.link.Values.lang;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

import static java.lang.String.format;
import static java.util.Comparator.comparing;

/**
 * RDF configuration action.
 *
 * <p>Uploads RDF-based specs to a {@linkplain #Configure(IRI) configuration context} in a {@linkplain #graph(Graph)
 * target graph}. Note that:</p>
 *
 * <ul>
 *     <li>the configuration context is cleared before uploading;</li>
 *     <li>declared RDF namespaces are incrementally uploaded to the target graph, overwriting existing definitions.</li>
 * </ul>
 */
public final class Configure implements Consumer<Collection<URL>> {

    private final IRI context;

    private Graph graph=service(Graph.graph());

    private Set<String> langs=Set.of("", "en");

    private final Logger logger=service(logger());


    /**
     * Creates a configuration action.
     *
     * @param context the IRI of the configuration context in the target graph
     *
     * @throws NullPointerException if {@code context} is null
     */
    public Configure(final IRI context) {

        if ( context == null ) {
            throw new NullPointerException("null context");
        }

        this.context=context;
    }


    /**
     * Configures the target graph (default to the {@linkplain Graph#graph() shared graph service}).
     *
     * @param graph the target graph for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public Configure graph(final Graph graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        this.graph=graph;

        return this;
    }


    /**
     * Configures the languages to be retained.
     *
     * @param langs the languages to be retained; may include the empty string for retaining untagged literals; if empty
     *              retains every language
     *
     * @return this action
     *
     * @throws NullPointerException if {@code langs} is null or contains null values
     */
    public Configure langs(final String... langs) {

        if ( langs == null || Arrays.stream(langs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        this.langs=new HashSet<>(Set.of(langs));

        return this;
    }

    /**
     * Configures the languages to be retained.
     *
     * @param langs the languages to be retained; may include the empty string for retaining untagged literals; if empty
     *              retains every language
     *
     * @return this action
     *
     * @throws NullPointerException if {@code langs} is null or contains null values
     */
    public Configure langs(final Set<String> langs) {

        if ( langs == null || langs.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        this.langs=new HashSet<>(langs);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public void accept(final Collection<URL> ontologies) {

        if ( ontologies == null || ontologies.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null ontologies");
        }

        graph.update(task(connection -> {

            final Model model=new LinkedHashModel();

            ontologies.forEach(path -> {
                try ( final InputStream input=path.openStream() ) {

                    Rio.createParser(TURTLE) // !!! guess format

                            .setRDFHandler(new AbstractRDFHandler() {

                                @Override public void handleNamespace(final String prefix, final String uri) {
                                    if ( !prefix.isEmpty() ) { // ignore default namespaces
                                        model.setNamespace(prefix, uri);
                                    }
                                }

                                @Override public void handleStatement(final Statement statement) {
                                    if ( !statement.getObject().isLiteral()
                                            || langs.isEmpty() || langs.contains(lang(statement.getObject()))
                                    ) {
                                        model.add(statement);
                                    }
                                }

                            })

                            .parse(input);

                } catch ( final IOException e ) {

                    throw new UncheckedIOException(e);

                }
            });

            time(() -> {

                model.getNamespaces().stream()

                        .sorted(comparing(Namespace::getPrefix))

                        .peek(entry -> logger.info(this, format(
                                "%-8s %s", entry.getPrefix(), entry.getName()
                        )))

                        .forEach(namespace ->
                                connection.setNamespace(namespace.getPrefix(), namespace.getName())
                        );

                connection.clear(context);
                connection.add(model, context);

            }).apply(elapsed -> logger.info(this, format(
                    "uploaded <%,d> statements to <%s> in <%,d> ms", model.size(), context, elapsed
            )));

        }));
    }

}
