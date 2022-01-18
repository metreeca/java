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

package com.metreeca.text.endo.mkII;

import com.metreeca.work.Base;
import com.metreeca.text.Match;
import com.metreeca.rdf.Values;
import com.metreeca.rdf4j.actions.GraphQuery;
import com.metreeca.rdf4j.actions.TupleQuery;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Frame;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.metreeca.rdf.Cell.cell;
import static com.metreeca.rdf.Values.quote;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Context.service;
import static java.math.BigDecimal.ZERO;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

public final class GraphDBSource implements GraphLinkerII.Source {

	private static final Pattern EscapePattern=compile("[^\\s\\p{LD}]");

	private static String lucene(final CharSequence keywords) {
		return "\""+EscapePattern.matcher(keywords).replaceAll("\\\\$0")+"\"";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String index="entities";
	private final int batch=1_000;

	private final Graph graph=service(graph());

	@Override public Xtream<Match<String, Resource>> lookup(final Xtream<String> anchors) {
		return anchors

				.batch(batch)

				.flatMap(new Frame<Collection<String>>() // keep aligned with index definition

						.model("prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
								+"prefix skos: <http://www.w3.org/2004/02/skos/core#>\n"
								+"\n"
								+"prefix lucene: <http://www.ontotext.com/connectors/lucene#>\n"
								+"prefix index: <http://www.ontotext.com/connectors/lucene/instance#>\n"
								+"\n"
								+"select distinct ?s ?l where {\n"
								+"\n"
								+"\tvalues ?a {\n"
								+"\t\t{anchors}\n"
								+"\t}\n"
								+"\n"
								+"\t[a index:{index}; lucene:query ?a; lucene:entities ?s].\n"
								+"\t\t\n"
								+"\tvalues ?p {\n"
								+"\t\trdfs:label\n"
								+"\t\tskos:prefLabel\n"
								+"\t\tskos:altLabel\n"
								+"\t\tskos:hiddenLabel\n"
								+"\t}\n"
								+"\n"
								+"\t?s ?p ?l filter (lang(?l) in ('', 'en'))\n"
								+"\n"
								+"}"
						)

						.value("index", index)

						.value("anchors", batch -> batch.stream()
								.map(s -> quote(GraphDBSource.lucene(s)))
								.collect(joining("\n\t\t"))
						)

				)

				.flatMap(new TupleQuery().graph(graph))

				.map(bindings -> new Match<>(
						bindings.getValue("l").stringValue(),
						(Resource)bindings.getValue("s")
				));
	}

	@Override public Xtream<Match<Resource, Collection<Statement>>> expand(final Xtream<Resource> resources) {
		return resources

				.batch(batch)

				.flatMap(new Frame<Collection<Resource>>()

						.model("prefix base: <"+Base.Namespace+">\n"
								+"\n"
								+"prefix rank: <http://www.ontotext.com/owlim/RDFRank#>\n"
								+"\n"
								+"construct {\n"
								+"\n"
								+"\t?s ?p ?o; base:weight ?w.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?s {\n"
								+"\t\t{candidates}\n"
								+"\t}\n"
								+"\n"
								+"\t?s ?p ?o; rank:hasRDFRank5 ?w filter isIRI(?o)\n"
								+"\n"
								+"}")

						.value("candidates", batch -> batch.stream()
								.map(Values::format)
								.collect(joining("\n\t\t"))
						)

				)

				.flatMap(new GraphQuery().graph(graph))

				.groupBy(Statement::getSubject)

				.map(entry -> cell(entry.getKey()).insert(entry.getValue()).get())

				.map(cell -> new Match<>(cell.focus(), cell.model(),
						cell.decimal(Base.weight).orElse(ZERO).doubleValue()
				));
	}

}
