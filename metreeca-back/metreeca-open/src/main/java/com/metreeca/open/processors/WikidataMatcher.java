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

package com.metreeca.open.processors;

import com.metreeca.core.Strings;
import com.metreeca.json.Frame;
import com.metreeca.json.Values;
import com.metreeca.open.actions.Wikidata;
import com.metreeca.rdf4j.actions.GraphQuery;
import com.metreeca.rdf4j.linkers.GraphLinker;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Fill;
import com.metreeca.text.Match;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.core.Strings.quote;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.shifts.Alt.alt;

import static java.util.stream.Collectors.joining;

/**
 * Wikidata candidate entity matcher.
 *
 * <p>Maps a stream of textual anchors to a stream of candidate entity descriptions extracted from Wikidata.</p>
 *
 * @see GraphLinker
 * @see <a href="https://www.wikidata.org/">Wikidata</a>
 */
public final class WikidataMatcher implements Function<Stream<String>, Stream<Match<String, Frame>>> {

	private final String lang="en";
	private final IRI[] labels={ RDFS.LABEL, SKOS.ALT_LABEL, Wikidata.P1549 };

	private final Graph graph=Wikidata.Graph();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Match<String, Frame>> apply(final Stream<String> anchors) {
		return Xtream.from(anchors)

				.batch(100)

				.flatMap(new Fill<Collection<String>>()

						.model("construct { ?s ?p ?o; rdf:value ?n } where {\n"
								+"\n"
								+"\tvalues ?a {\n"
								+"\t\t{anchors}\n"
								+"\t}\n"
								+"\n"
								+"\tservice wikibase:mwapi {\n"
								+"\n"
								+"\t\tbd:serviceParam wikibase:endpoint 'www.wikidata.org';\n"
								+"\t\t\t\t\t\twikibase:api 'EntitySearch';\n"
								+"\n"
								+"\t\t\t\t\t\tmwapi:search ?a;\n"
								+"\t\t\t\t\t\tmwapi:language {lang};\n"
								+"\t\t\t\t\t\tmwapi:limit 'max'.\n"
								+"\n"
								+"\t\t?s wikibase:apiOutputItem mwapi:item.\n"
								+"\t\t?n wikibase:apiOrdinal true.\n"
								+"\n"
								+"\t} \n"
								+"\n"
								+"\t?s ?p ?o filter ("
								+"isIRI(?o) && strstarts(str(?o), str(wd:Q)) || lang(?o) = {lang} && ?p in (\n"
								+"\t\t{labels}, \n"
								+"\t\tschema:description\n"
								+"\t))\n"
								+"\n"
								+"}")

						.value("anchors", batch -> batch.stream()
								.map(Strings::quote)
								.collect(joining("\n\t\t"))
						)

						.value("labels", Arrays
								.stream(labels)
								.map(Values::format)
								.collect(joining(",\n\t\t"))
						)

						.value("lang", quote(lang))

				)

				.flatMap(new GraphQuery().graph(graph))

				.groupBy(Statement::getSubject)

				.map(entry -> frame(entry.getKey(), entry.getValue()))

				.filter(frame -> frame.values(Wikidata.P31).noneMatch(Wikidata.Q4167410::equals))

				.flatMap(frame -> {

					final int weight=-frame
							.integer(RDF.VALUE)
							.orElse(BigInteger.ZERO)
							.intValue();

					return frame.strings(alt(labels))
							.map(label -> new Match<>(label, frame, weight));

				});
	}

}
