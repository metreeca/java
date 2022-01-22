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

package com.metreeca.text.matchers;

import com.metreeca.json.Frame;
import com.metreeca.json.Values;
import com.metreeca.rdf4j.actions.GraphQuery;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Fill;
import com.metreeca.text.Match;
import com.metreeca.text.Notes;

import org.eclipse.rdf4j.model.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.services.Logger.logger;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

public final class GraphDBMatcher implements Function<Stream<String>, Stream<Match<String, Frame>>> {

	private static final IRI HasRDFRank=iri("http://www.ontotext.com/owlim/RDFRank#hasRDFRank");

	private static final Pattern EscapePattern=compile("[^\\s\\p{LD}]");

	private static String lucene(final CharSequence keywords) {
		return "\""+EscapePattern.matcher(keywords).replaceAll("\\\\$0")+"\"";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int size=1_000;
	private final String index="entities";

	private final Collection<IRI> labels=Notes.Labels;
	private Collection<String> languages=Notes.Languages;

	private final Graph graph=service(graph());


	public GraphDBMatcher languages(final Collection<String> languages) {

		if ( languages == null || languages.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null languages");
		}

		this.languages=new HashSet<>(languages);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Match<String, Frame>> apply(final Stream<String> anchors) {
		return Xtream.from(anchors)

				.batch(size)

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
								+"\t\t{labels}, \n"
								+"\t\trdfs:description\n"
								+"\t))\n"
								+"\n"
								+"}"
						)

						.value("index", index)

						.value("anchors", batch -> batch.stream()
								.map(s -> quote(lucene(s)))
								.collect(joining("\n\t\t"))
						)

						.value("languages", languages.stream()
								.map(Values::quote)
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
							.decimal(HasRDFRank)
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

	public static final class Indexer implements Runnable {

		@Override public void run() {
			service(graph()).update(connection -> { // ;( in a separate transaction

				service(logger()).info(GraphDBMatcher.class, "updating RDF rank");

				connection.add(bnode(), iri("http://www.ontotext.com/owlim/RDFRank#compute"), bnode());

				return this;

			});
		}

	}

}
