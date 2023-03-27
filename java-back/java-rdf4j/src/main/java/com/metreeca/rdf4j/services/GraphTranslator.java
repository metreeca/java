/*
 * Copyright © 2020-2022 EC2U Alliance
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

package com.metreeca.rdf4j.services;

import com.metreeca.http.services.Translator;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.TupleQuery;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.core.Locator.service;
import static com.metreeca.http.services.Translator.translator;
import static com.metreeca.rdf.Values.*;

import static java.util.stream.Collectors.toList;

/**
 * Graph-based text translator.
 *
 * <p>Retrieves existing text translations from a {@linkplain Graph graph}.</p>
 */
public final class GraphTranslator implements Translator {

    /**
     * Translates tagged literals in an RDF model.
     *
     * <p>Generates missing {@linkplain Translator#translator() translations} of tagged literals in an RDF model.</p>
     *
     * @param model the model to be translated
     * @param langs the target set of languages; ignored if empty
     *
     * @return a copy of {@code model} extended with missing tagged literal translations for the target languages
     *
     * @throws NullPointerException if either {@code model} or {@code langs} is null or contains null values
     */
    public static Collection<Statement> translate(final Collection<Statement> model, final Collection<String> langs) {

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        if ( langs == null || langs.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null langs");
        }

        return model.isEmpty() || langs.isEmpty() ? model : Stream

                .concat(

                        model.stream(),

                        model.stream()

                                .filter(statement -> langs.contains(lang(statement.getObject())))

                                .flatMap(statement -> langs.parallelStream().flatMap(target -> {

                                    final Resource subject=statement.getSubject();
                                    final IRI predicate=statement.getPredicate();
                                    final Value object=statement.getObject();

                                    final boolean translated=model.stream()
                                            .filter(pattern(subject, predicate, null))
                                            .map(Statement::getObject)
                                            .anyMatch(v -> lang(v).equals(target));

                                    return translated ? Stream.empty() : service(translator())
                                            .translate(target, lang(object), object.stringValue())
                                            .map(v -> statement(subject, predicate, literal(v, target)))
                                            .stream();

                                }))

                )

                .collect(toList());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Graph graph=service(Graph.graph());


    /**
     * Configures the target graph (default to the {@linkplain Graph#graph() shared graph service}).
     *
     * @param graph the target graph for this translator
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public GraphTranslator graph(final Graph graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        this.graph=graph;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Optional<String> translate(final String target, final String source, final String text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        if ( source == null ) {
            throw new NullPointerException("null source");
        }

        return source.isEmpty() ? Optional.empty() : text.isBlank() ? Optional.of(text) : graph.query(connection -> {

            final TupleQuery query=connection.prepareTupleQuery(""
                    +"select distinct (sample(?o) as ?translation) \n"
                    +"\n"
                    +"where {\n"
                    +"\n"
                    +"\t?s ?p $text, ?o. "
                    +"\t\n"
                    +"\tbind (lang(?o) as ?l)\n"
                    +"\tfilter (?l = $target)\n"
                    +"\n"
                    +"}\n"
                    +"\n"
                    +"group by ?s ?p ?l\n"
                    +"having (count(distinct ?o) = 1)"
            );

            query.setBinding("text", literal(text, source));
            query.setBinding("target", literal(target));

            final List<String> translations=query.evaluate().stream()
                    .map(bindings -> bindings.getValue("translation"))
                    .map(Value::stringValue)
                    .collect(toList());

            return translations.size() == 1 ? Optional.of(translations.get(0)) : Optional.empty();

        });
    }

}
