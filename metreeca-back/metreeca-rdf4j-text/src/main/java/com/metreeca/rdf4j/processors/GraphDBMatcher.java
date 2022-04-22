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

package com.metreeca.rdf4j.processors;

import com.metreeca.core.Strings;
import com.metreeca.http.services.Logger;
import com.metreeca.json.Frame;
import com.metreeca.json.Values;
import com.metreeca.rdf4j.actions.GraphQuery;
import com.metreeca.rdf4j.linkers.GraphLinker;
import com.metreeca.rdf4j.schemas.Text;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Fill;
import com.metreeca.text.Match;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.Json;

import static com.metreeca.http.Toolbox.service;
import static com.metreeca.http.services.Logger.logger;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.rdf4j.services.Graph.graph;

import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Ontotext GraphDB candidate entity matcher.
 *
 * <p>Maps a stream of textual anchors to a stream of candidate entity descriptions extracted from an Ontotext GraphDB
 * endpoint using a custom full-text index built with the {@link Index} tool and RDF rank scores configured with the
 * {@link Rank} tool.</p>
 *
 * @see GraphLinker
 * @see <a href="https://graphdb.ontotext.com/documentation/standard/index.html">Ontotext GraphDB</a>
 */
public final class GraphDBMatcher implements Function<Stream<String>, Stream<Match<String, Frame>>> {

    private static final int BatchSize=1_000;
    private static final String DefaultIndex="entities";

    private static final Pattern EscapePattern=compile("[^\\s\\p{LD}]");

    private static String quote(final CharSequence keywords) {
        return "\""+EscapePattern.matcher(keywords).replaceAll("\\\\$0")+"\"";
    }


    private static final IRI hasRDFRank=iri("http://www.ontotext.com/owlim/RDFRank#hasRDFRank");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String index=DefaultIndex;

    private Collection<IRI> types=Set.of();
    private Collection<IRI> contexts=Set.of();
    private Collection<IRI> labels=Text.Labels;
    private Collection<String> languages=Text.Languages;


    private final Graph graph=service(graph());
    private final Logger logger=service(logger());


    /**
     * Configures the target RDF types.
     *
     * @param types the target RDF types
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code types} is null or contains null members
     */
    public GraphDBMatcher types(final IRI... types) {

        if ( types == null || Arrays.stream(types).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null types");
        }

        this.types=Set.of(types);

        return this;
    }

    /**
     * Configures the target RDF types.
     *
     * @param types the target RDF types
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code types} is null or contains null members
     */
    public GraphDBMatcher types(final Collection<IRI> types) {

        if ( types == null || types.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null types");
        }

        this.types=Set.copyOf(types);

        return this;
    }

    /**
     * Configures the target graph contexts.
     *
     * @param contexts the target graph contexts
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code contexts} is null or contains null members
     */
    public GraphDBMatcher contexts(final Collection<IRI> contexts) {

        if ( contexts == null || contexts.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null contexts");
        }

        this.contexts=Set.copyOf(contexts);

        return this;
    }

    /**
     * Configures the target graph contexts.
     *
     * @param contexts the target graph contexts
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code contexts} is null or contains null members
     */
    public GraphDBMatcher contexts(final IRI... contexts) {

        if ( contexts == null || Arrays.stream(contexts).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null contexts");
        }

        this.contexts=Set.of(contexts);

        return this;
    }

    /**
     * Configures the name of the supporting full-text index (defaults to {@value #DefaultIndex})
     *
     * @param index the name of the full-text index supporting candidate selection
     *
     * @return this matcher
     *
     * @throws NullPointerException     if {@code index} is null
     * @throws IllegalArgumentException if {@code index} is empty or malformed
     */
    public GraphDBMatcher index(final String index) {

        if ( index == null ) {
            throw new NullPointerException("null index name");
        }

        if ( index.isEmpty() ) { // !!! well-formedness
            throw new IllegalArgumentException("empty index name");
        }

        this.index=index;

        return this;
    }

    /**
     * Configures the target label properties (defaults to {@link Text#Labels}).
     *
     * @param labels the set of target label properties
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code labels} is null or contains null members
     */
    public GraphDBMatcher labels(final IRI... labels) {

        if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null labels");
        }

        this.labels=Set.of(labels);

        return this;
    }

    /**
     * Configures the target label properties (defaults to {@link Text#Labels}).
     *
     * @param labels the set of target label properties
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code labels} is null or contains null members
     */
    public GraphDBMatcher labels(final Collection<IRI> labels) {

        if ( labels == null || labels.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null labels");
        }

        this.labels=Set.copyOf(labels);

        return this;
    }

    /**
     * Configures the target languages (defaults to {@link Text#Languages}).
     *
     * @param languages the set of target languages
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code languages} is null or contains null members
     */
    public GraphDBMatcher languages(final String... languages) { // !!! empty string ›› untagged

        if ( languages == null || Arrays.stream(languages).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null languages");
        }

        this.languages=Set.of(languages);

        return this;
    }

    /**
     * Configures the target languages (defaults to {@link Text#Languages}).
     *
     * @param languages the set of target languages
     *
     * @return this matcher
     *
     * @throws NullPointerException if {@code languages} is null or contains null members
     */
    public GraphDBMatcher languages(final Collection<String> languages) { // !!! empty string ›› untagged

        if ( languages == null || languages.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null languages");
        }

        this.languages=Set.copyOf(languages);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the full-text index manager.
     *
     * @return the full-text index manager for this matcher
     */
    public Index index() {
        return new Index();
    }

    /**
     * Retrieves the RDF rank manager.
     *
     * @return the the RDF rank manager for this matcher
     */
    public Rank rank() {
        return new Rank();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Stream<Match<String, Frame>> apply(final Stream<String> anchors) {
        return Xtream.from(anchors)

                .batch(BatchSize)

                .flatMap(new Fill<Collection<String>>() // keep aligned with index definition

                        .model("prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                                +"\n"
                                +"prefix lucene: <http://www.ontotext.com/connectors/lucene#>\n"
                                +"prefix index: <http://www.ontotext.com/connectors/lucene/instance#>\n"
                                +"\n"
                                +"prefix rank: <http://www.ontotext.com/owlim/RDFRank#>\n"
                                +"\n"
                                +"construct { ?s ?p ?o; rank:hasRDFRank ?w } where {\n"
                                +"\n"
                                +"\tvalues ?a {\n"
                                +"\t\t{anchors}\n"
                                +"\t}\n"
                                +"\n"
                                +"\t[a index:{index}; lucene:query ?a; lucene:entities ?s].\n"
                                +"\t\t\n"
                                +"\t?s ?p ?o; rank:hasRDFRank5 ?w.\n"
                                +"\t\n"
                                +"\tfilter (isIRI(?o) || lang(?o) in ({languages}) && ?p in (\n" // !!! empty langs!
                                +"\t\t{labels}\n"
                                +"\t))\n"
                                +"\n"
                                +"}"
                        )

                        .value("index", index)

                        .value("anchors", batch -> batch.stream()
                                .map(s -> Strings.quote(quote(s)))
                                .collect(joining("\n\t\t"))
                        )

                        .value("languages", languages.stream()
                                .map(Strings::quote)
                                .collect(joining(", "))
                        )

                        .value("labels", labels.stream()
                                .map(Values::format)
                                .collect(joining(",\n\t\t"))
                        )


                )

                .flatMap(new GraphQuery().graph(graph))

                .groupBy(Statement::getSubject)

                .map(entry -> frame(entry.getKey(), entry.getValue()))

                .flatMap(frame -> {

                    final double weight=frame
                            .decimal(hasRDFRank)
                            .orElse(BigDecimal.ZERO)
                            .doubleValue();

                    return frame.model()

                            .filter(s -> labels.contains(s.getPredicate()))

                            .map(Statement::getObject)
                            .map(Value::stringValue)

                            .map(label -> new Match<>(label, frame, weight));

                });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @see <a href="https://graphdb.ontotext.com/documentation/standard/lucene-graphdb-connector.html">Ontotext GraphDB
     * - Lucene GraphDB Connector</a>
     */
    public final class Index {

        private final IRI listConnectors=lucene("listConnectors");
        private final IRI createConnector=lucene("createConnector");
        private final IRI dropConnector=lucene("dropConnector");


        private IRI lucene(final String name) {
            return iri("http://www.ontotext.com/connectors/lucene#", name);
        }

        private IRI index(final String index) {
            return iri("http://www.ontotext.com/connectors/lucene/instance#", index);
        }


        public GraphDBMatcher create() {

            final List<IRI> fields=List.copyOf(labels);

            return execute("creating", connection -> connection.add(index(index), createConnector, literal(Json

                    .createObjectBuilder(Map.ofEntries(

                            entry("readonly", false),
                            entry("detectFields", false),
                            entry("importGraph", false),
                            entry("boostProperties", List.of()),
                            entry("stripMarkup", false),

                            entry("types", types.stream().map(Value::stringValue).collect(toList())),
                            entry("languages", languages),

                            entry("fields", IntStream.range(0, fields.size())
                                    .mapToObj(i -> Map.ofEntries(
                                            entry("fieldName", "label$"+(i+1)),
                                            entry("propertyChain", List.of(fields.get(i).stringValue())),
                                            entry("indexed", true),
                                            entry("stored", false),
                                            entry("analyzed", true),
                                            entry("multivalued", true),
                                            entry("facet", false)
                                    ))
                                    .collect(toList()))
                    ))

                    .build()
                    .toString()
            )));
        }

        public GraphDBMatcher delete() {
            return execute("deleting", connection -> { });
        }


        private GraphDBMatcher execute(final String label, final Consumer<RepositoryConnection> task) {

            graph.update(connection -> {

                logger.info(GraphDBMatcher.class, String.format("%s <%s> lucene index", label, index));

                if ( connection.hasStatement(index(index), listConnectors, null, false) ) {
                    connection.add(index(index), dropConnector, bnode());
                }

                task.accept(connection);

                return this;

            });

            return GraphDBMatcher.this;
        }

    }

    /**
     * @see <a href="https://graphdb.ontotext.com/documentation/standard/rdf-rank.html">Ontotext GraphDB - RDF Rankr</a>
     */
    public final class Rank {

        private final IRI filtering=rank("filtering");
        private final IRI includeExplicit=rank("includeExplicit");
        private final IRI includeImplicit=rank("includeImplicit");
        private final IRI includedGraphs=rank("includedGraphs");
        private final IRI compute=rank("compute");


        private IRI rank(final String name) {
            return iri("http://www.ontotext.com/owlim/RDFRank#", name);
        }


        public GraphDBMatcher define() {
            return execute("defining", connection -> {


                connection.add(filtering, rank("setParam"), literal(true));
                connection.add(includeExplicit, rank("setParam"), literal(true));
                connection.add(includeImplicit, rank("setParam"), literal(true));

                contexts.forEach(context -> connection.add(context, includedGraphs, literal("add")));

            });
        }

        public GraphDBMatcher update() {
            return execute("ipdating", connection ->

                    connection.add(bnode(), compute, bnode())

            );
        }


        private GraphDBMatcher execute(final String label, final Consumer<RepositoryConnection> task) {

            graph.update(connection -> {

                logger.info(GraphDBMatcher.class, String.format("%s RDF rank", label));

                task.accept(connection);

                return this;

            });

            return GraphDBMatcher.this;
        }

    }

}
