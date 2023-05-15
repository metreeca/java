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

import com.metreeca.http.services.Logger;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.services.Logger.time;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

/**
 * SPARQL update action.
 *
 * <p>Executes SPARQL updates against the {@linkplain #graph(Graph) target graph}.</p>
 */
public final class Update extends Action<Update> implements Consumer<String> {

    private final AtomicBoolean clear=new AtomicBoolean();


    private final Logger logger=service(Logger.logger());


    /**
     * Configures the clear flag (default to {@code false}).
     *
     * @param clear {@code true} if default {@linkplain #remove(IRI...) remove} and {@linkplain #insert(IRI) insert}
     *              contexts are to be cleared before the first update is executed by this action; {@code false},
     *              otherwise
     *
     * @return this action
     */
    public Update clear(final boolean clear) {

        this.clear.set(clear);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Executes a SPARQL tuple query.
     *
     * @param update the update to be executed against the {@linkplain #graph(Graph) target graph} after {@linkplain
     *               #configure(Operation) configuring} it; null or empty queries are silently ignored
     */
    @Override public void accept(final String update) {
        if ( update != null && !update.isEmpty() ) {
            graph().update(connection -> time(() -> {

                if ( clear.getAndSet(false) ) {

                    final Set<IRI> remove=dataset().getDefaultRemoveGraphs();
                    final IRI insert=dataset().getDefaultInsertGraph();

                    final Resource[] contexts=Stream

                            .concat(
                                    Optional.ofNullable(remove).stream().flatMap(Collection::stream),
                                    Optional.ofNullable(insert).stream()
                            )

                            .toArray(Resource[]::new);

                    if ( contexts.length > 0 ) {

                        connection.clear(contexts);

                        logger.info(this, format(
                                "cleared <%s>", Arrays.stream(contexts)
                                        .map(Value::stringValue)
                                        .collect(joining(", "))
                        ));

                    }

                }

                configure(connection.prepareUpdate(SPARQL, update, base())).execute();

            }).apply(t ->

                    logger.info(this, format("executed in <%,d> ms", t))

            ));
        }
    }

}
