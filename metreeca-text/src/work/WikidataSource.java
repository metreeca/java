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

import com.metreeca.open.Wikidata;
import com.metreeca.text.Match;
import com.metreeca.text.endo.mkII.GraphLinkerII;
import com.metreeca.rdf.Values;
import com.metreeca.rdf4j.actions.GraphQuery;
import com.metreeca.rdf4j.actions.TupleQuery;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Frame;

import org.eclipse.rdf4j.model.*;

import java.math.BigInteger;
import java.util.Collection;

import static com.metreeca.rdf.Cell.cell;
import static java.util.stream.Collectors.joining;

public final class WikidataSource implements GraphLinkerII.Source {

	private final int size=100;

	private final Graph graph=Wikidata.Graph();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Xtream<Match<String, Resource>> lookup(final Xtream<String> anchors) {
		return anchors

				.batch(size)

				.flatMap(new Frame<Collection<String>>()

						.model("select ?n ?s ?l where {\n"
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
								+"\t\t\t\t\t\tmwapi:language 'en';\n"
								+"\t\t\t\t\t\tmwapi:limit 'max'.\n"
								+"\n"
								+"\t\t?s wikibase:apiOutputItem mwapi:item.\n"
								+"\t\t?n wikibase:apiOrdinal true.\n"
								+"\n"
								+"\t}\n"
								+"\n"
								+"\t?s rdfs:label|skos:altLabel ?l filter (lang(?l) = 'en')\n"
								+"\n"
								+"}"
						)

						.value("anchors", batch -> batch.stream()
								.map(Values::quote)
								.collect(joining("\n\t\t"))
						)

				)

				.flatMap(new TupleQuery().graph(graph))

				.map(bindings -> new Match<>(
						bindings.getValue("l").stringValue(),
						(Resource)bindings.getValue("s"),
						-((Literal)bindings.getValue("n")).intValue()
				));
	}

	@Override public Xtream<Match<Resource, Collection<Statement>>> expand(final Xtream<Resource> resources) {
		return resources

				.batch(size)

				.flatMap(new Frame<Collection<Resource>>()

						.model("construct { ?s ?p ?o. ?t ?q ?s } where {\n"
								+"\n"
								+"\tvalues ?s {\n"
								+"\t\t{candidates}\n"
								+"\t}\n"
								+"\n"
								+"\t?s ?p ?o filter ("
								+" \n"
								+"\t\tisIRI(?o)\n"
								+"\t\t&& strstarts(str(?o), 'http://www.wikidata.org/entity/Q')\n"
								+"\t\t&& not exists { ?o wdt:P31 wd:Q4167410 } # disambiguation pages\n"
								+"\t"
								+")\n"
								+"\n"
								+"}"
						)

						.value("candidates", batch -> batch.stream()
								.map(Values::format)
								.collect(joining("\n\t\t"))
						)

				)

				.flatMap(new GraphQuery().graph(graph))

				.groupBy(Statement::getSubject)

				.map(entry -> cell(entry.getKey()).insert(entry.getValue()).get())

				.map(cell -> new Match<>(cell.focus(), cell.model(),
						cell.integer(Wikidata.SITELINKS).orElse(BigInteger.ZERO).intValue()
				));
	}

}
