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

package com.metreeca.rdf4j.actions;

import com.metreeca.core.services.Logger;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.core.services.Logger.time;
import static com.metreeca.link.Values.lang;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;


/**
 * RDF upload action.
 *
 * <p>Uploads RDF statements to a {@linkplain #graph(Graph) target graph}.</p>
 */
public final class Upload implements Consumer<Collection<Statement>> {

    private static final Resource[] DefaultContexts=new Resource[0];


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Graph graph=service(Graph.graph());

    private Resource[] contexts=DefaultContexts;
    private Set<String> langs=Set.of();

    private final AtomicBoolean clear=new AtomicBoolean();
    private final AtomicLong count=new AtomicLong();

    private final Logger logger=service(logger());


    /**
     * Configures the target graph (default to the {@linkplain Graph#graph() shared graph service}).
     *
     * @param graph the target graph for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public Upload graph(final Graph graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        this.graph=graph;

        return this;
    }

    /**
     * Configures the target upload contexts (default to the empty array, that is to the default context).
     *
     * @param contexts an array of contexts statements are to be uploaded to
     *
     * @return this action
     *
     * @throws NullPointerException if {@code contexts} is null or contains null values
     */
    public Upload contexts(final Resource... contexts) {

        if ( contexts == null || Arrays.stream(contexts).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null contexts");
        }

        this.contexts=contexts.clone();

        return this;
    }


    /**
     * Configures the languages to be retained.
     *
     * <p><strong>Warning</strong> / Untagged literals are always retained.</p>
     *
     * @param langs the <a href="https://www.rfc-editor.org/rfc/rfc5646.html">language tags</a> to be retained; if empty,
     *              all languages are retained
     *
     * @return this action
     *
     * @throws NullPointerException if {@code langs} is null or contains null values
     */
    public Upload langs(final String... langs) {

        if ( langs == null || Arrays.stream(langs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        this.langs=Arrays.stream(langs)
                .map(lang -> lang.toLowerCase(Locale.ROOT))
                .collect(toSet());

        return this;
    }

    /**
     * Configures the languages to be retained.
     *
     * <p><strong>Warning</strong> / Untagged literals are always retained.</p>
     *
     * @param langs the <a href="https://www.rfc-editor.org/rfc/rfc5646.html">language tags</a> to be retained; if empty,
     *              all languages are retained
     *
     * @return this action
     *
     * @throws NullPointerException if {@code langs} is null or contains null values
     */
    public Upload langs(final Collection<String> langs) {

        if ( langs == null || langs.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        this.langs=langs.stream()
                .map(lang -> lang.toLowerCase(Locale.ROOT))
                .collect(toSet());

        return this;
    }


    /**
     * Configures the clear flag (default to {@code false}).
     *
     * @param clear {@code true} if the target contexts are to be cleared before the first upload performed by this
     *              action; {@code false}, otherwise
     *
     * @return this action
     */
    public Upload clear(final boolean clear) {

        this.clear.set(clear);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Uploads RDF statements.
     *
     * @param statements a collection of RDF statements to be uploaded to the {@linkplain #graph(Graph) target graph};
     *                   null or empty collections are silently ignored
     */
    @Override public void accept(final Collection<Statement> statements) {
        if ( statements != null && !statements.isEmpty() ) {

            final String targets=contexts.length == 0 ? "default context" : Arrays.stream(contexts)
                    .map(Value::stringValue)
                    .collect(joining(", "));

            graph.update(connection -> time(() -> {

                if ( clear.getAndSet(false) ) {

                    connection.clear(contexts);

                    logger.info(this, format(
                            "cleared <%s>", targets
                    ));

                }

                if ( statements instanceof NamespaceAware ) {
                    ((NamespaceAware)statements).getNamespaces().stream()

                            .filter(not(namespace -> namespace.getPrefix().isEmpty()))

                            .sorted(comparing(Namespace::getPrefix))

                            .peek(entry -> logger.info(this, format(
                                    "%-8s %s", entry.getPrefix(), entry.getName()
                            )))

                            .forEach(namespace -> connection.setNamespace(namespace.getPrefix(), namespace.getName()));
                }

                if ( !statements.isEmpty() ) {
                    connection.add(

                            langs.isEmpty()
                                    ? statements
                                    : () -> statements.stream().filter(this::retain).iterator(),

                            contexts

                    );
                }

            }).apply(t -> logger.info(this, format(
                    "uploaded <%,d / %,d> statements to <%s> in <%,d> ms",
                    statements.size(), count.addAndGet(statements.size()), targets, t
            ))));

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean retain(final Statement statement) {
        return retain(statement.getObject());
    }

    private boolean retain(final Value value) {
        return retain(lang(value));
    }

    private boolean retain(final String lang) {
        return lang.isEmpty() || langs.contains(lang);
    }

}
