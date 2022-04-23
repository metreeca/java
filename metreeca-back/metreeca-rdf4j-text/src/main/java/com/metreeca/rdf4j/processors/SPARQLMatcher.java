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
import com.metreeca.core.Xtream;
import com.metreeca.http.services.Logger;
import com.metreeca.link.Frame;
import com.metreeca.link.Values;
import com.metreeca.rdf4j.actions.*;
import com.metreeca.rdf4j.linkers.GraphLinker;
import com.metreeca.rdf4j.schemas.Text;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.actions.Fill;
import com.metreeca.text.*;
import com.metreeca.text.tokenizers.PatternTokenizer;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.services.Logger.logger;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Values.literal;
import static com.metreeca.rdf4j.services.Graph.graph;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

/**
 * SPARQL candidate entity matcher.
 *
 * <p>Maps a stream of textual anchors to a stream of candidate entity descriptions extracted from a SPARQL endpoint
 * using a custom full-text index built and updated with the {@link Indexer} tool.</p>
 *
 * @see GraphLinker
 */
public final class SPARQLMatcher implements Function<Stream<String>, Stream<Match<String, Frame>>> {

    private final int size=1_000;


    private Collection<IRI> labels=Text.Labels;
    private Set<String> languages=Text.Languages;

    private final IRI context=RDF.NIL; // !!!
    private final Graph graph=service(graph());
    private final Logger logger=service(logger());


    public SPARQLMatcher labels(final IRI... labels) {

        if ( labels == null ) {
            throw new NullPointerException("null labels");
		}

		return labels(asList(labels));
	}

	public SPARQLMatcher labels(final Collection<IRI> labels) {

		if ( labels == null || labels.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}

		this.labels=new HashSet<>(labels);

		return this;
	}


	public SPARQLMatcher languages(final String... languages) {

		if ( languages == null ) {
			throw new NullPointerException("null languages");
		}

		return languages(asList(languages));
	}

	public SPARQLMatcher languages(final Collection<String> languages) {

		if ( languages == null || languages.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null languages");
		}

		this.languages=new HashSet<>(languages);

		return this;
	}


	@Override public Stream<Match<String, Frame>> apply(final Stream<String> anchors) {
		return Xtream.from(anchors)

				.batch(size)

				.flatMap(new Fill<Collection<String>>() // keep aligned with index definition

						.model("prefix text: <app://text.metreeca.com/terms#>\n"
								+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
								+"\n"
								+"construct { ?s ?p ?o; text:weight ?w } where {\n"
								+"\n"
								+"\tvalues ?a {\n"
								+"\t\t{anchors}\n"
								+"\t}\n"
								+"\n"
								+"\t?s ?p ?o; \n"
								+"\t\ttext:anchor ?a;\n"
								+"\t\ttext"
								+
								":weight ?w.\t\n"
								+"\n"
								+"\tfilter (isIRI(?o) || lang(?o) in ({languages}) && ?p in (\n"
								+"\t\t{labels}, \n"
								+"\t\trdfs:description\n"
								+"\t))\n"
								+"\n"
								+"}"
						)

						.value("anchors", batch -> batch.stream()
                                .map(Strings::quote)
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
                            .decimal(Text.weight)
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
     * SPARQL full-text indexer.
     *
     * <p>Creates a full-text index for {@link SPARQLMatcher}.</p>
     */
    public static final class Indexer implements Runnable {

        private Collection<IRI> labels=Text.Labels;
        private Set<String> languages=Text.Languages;
        private IRI context=RDF.NIL; // !!!

        private Function<Token, Chunk> analyzer=new PatternTokenizer().defaults();


        private final Graph graph=service(graph());
        private final Logger logger=service(logger());


        public Indexer labels(final IRI... labels) {

			if ( labels == null ) {
				throw new NullPointerException("null labels");
			}

			return labels(asList(labels));
		}

		public Indexer labels(final Collection<IRI> labels) {

			if ( labels == null || labels.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null labels");
			}

			this.labels=new HashSet<>(labels);

			return this;
		}


		public Indexer languages(final String... languages) {

			if ( languages == null ) {
				throw new NullPointerException("null languages");
			}

			return languages(asList(languages));
		}

		public Indexer languages(final Collection<String> languages) {

			if ( languages == null || languages.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null languages");
			}

			this.languages=new HashSet<>(languages);

			return this;
		}


		public Indexer context(final IRI context) {

			if ( context == null ) {
				throw new NullPointerException("null context");
			}

			this.context=context;

			return this;
		}


		public Indexer analyzer(final Function<Token, Chunk> analyzer) {

			if ( analyzer == null ) {
				throw new NullPointerException("null analyzer");
			}

			this.analyzer=analyzer;

			return this;
		}


		@Override public void run() {
			graph.update(connection -> {

				clean();

				anchors(connection);
				weights();

				return this;

			});
		}


		private void clean() {

			logger.info(this, "cleaning");

			Stream.of("prefix base: <app://text.metreeca.com/terms#>\n"
							+"\n"
							+"delete { ?e base:weight ?w; base:anchor ?a }\n"
							+"where { ?e a base:Entity; base:weight ?w; base:anchor ?a }")

					.forEach(new Update()
							.graph(graph)
							.remove(context) // !!! context
					);

		}

		private void anchors(final RepositoryConnection connection) {

			service(logger()).info(this, "extracting anchors");

			Stream
					.of("")

					.flatMap(new Fill<>()

							.model("prefix text: <app://text.metreeca.com/terms#>\n"
									+"\n"
									+"select ?e ?l {\n"
									+"\n"
									+"\tvalues ?p {\n"
									+"\t\t{labels}\n"
									+"\t}\n"
									+"\n"
									+"\t?e a text"
									+
									":Entity; ?p ?l filter (lang(?l) in ({languages}))\n"
									+"\n"
									+"}")

							.value("labels", labels.stream()
									.map(Values::format)
									.collect(joining("\n\t\t"))
							)

							.value("languages", languages.stream()
									.map(Values::literal)
									.map(Values::format)
									.collect(joining(", "))
							)

					)

					.flatMap(new TupleQuery() // !!! context / include inferred
							.graph(graph)
					)

					.forEach(bindings -> {

						final Resource entity=(Resource)bindings.getValue("e");
						final Value label=bindings.getValue("l");

						analyzer.apply(new Token(label.stringValue())).tokens().forEach(anchor ->
                                connection.add(entity, Text.anchor, literal(anchor.text(true)), context)
						);

					});
		}

		private void weights() {

			service(logger()).info(this, "computing weights");

			Stream

					.of("prefix text: <app://text.metreeca.com/terms#>\n"
							+"\n"
							+"insert { ?e text:weight ?w  } where {\n"
							+"\n"
							+"\tselect ?e (count(?c) as ?w) {\n"
							+"\n"
							+"\t\t?e a text"
							+
							":Entity. ?c ?p ?e.\n"
							+"\n"
							+"\t} group by ?e\n"
							+"\n"
							+"}"
					)

					.forEach(new Update()
							.graph(graph)
							.insert(context) // !!! review
					);
		}

	}

}
