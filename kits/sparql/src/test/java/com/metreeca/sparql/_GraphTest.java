/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.sparql;

import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.sparql.Graph.auto;
import static com.metreeca.sparql.Graph.graph;
import static com.metreeca.sparql.GraphTest.model;

import static java.util.stream.Collectors.joining;


public final class _GraphTest {

	private final Statement data=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);

	private void exec(final Runnable... tasks) {
		new Tray()
				.set(engine(), GraphEngine::new)
				.set(graph(), GraphTest::graph)
				.exec(tasks)
				.clear();
	}


	@Test void testConfigureRequest() {
		exec(() -> assertThat(_Graph.<Request>query(

				sparql("\n"
						+"construct { \n"
						+"\n"
						+"\t?this\n"
						+"\t\t:time $time;\n"
						+"\t\t:stem $stem;\n"
						+"\t\t:name $name;\n"
						+"\t\t:task $task;\n"
						+"\t\t:base $base;\n"
						+"\t\t:item $item;\n"
						+"\t\t:user $user;\n"

						+"\t\t:custom $custom.\n"
						+"\n"
						+"} where {}\n"
				),

				(request, query) -> query.setBinding("custom", literal(123))

				)

						.apply(new Request()

										.method(Request.POST)
										.base(Base)
										.path("/test/request"),

								new LinkedHashModel(set(data))
						)
				)

						.as("bindings configured")
						.hasSubset(decode("\n"
								+"<test/request>\n"
								+"\t:stem <test/>;\n"
								+"\t:name 'request';\n"
								+"\t:task 'POST';\n"
								+"\t:base <>;\n"
								+"\t:item <test/request>;\n"
								+"\t:user form:none.\n"
						))

						.as("timestamp configured")
						.hasStatement(item("test/request"), term("time"), null)

						.as("custom settings applied")
						.hasStatement(item("test/request"), term("custom"), literal(123))

						.as("existing statements forwarded")
						.hasSubset(data)

		);
	}

	@Test void testConfigureResponse() {
		exec(() -> assertThat(_Graph.<Response>query(

				sparql("\n"
						+"construct { \n"
						+"\n"
						+"\t?this\n"
						+"\t\t:time $time;\n"
						+"\t\t:stem $stem;\n"
						+"\t\t:name $name;\n"
						+"\t\t:task $task;\n"
						+"\t\t:base $base;\n"
						+"\t\t:item $item;\n"
						+"\t\t:user $user;\n"
						+"\t\t:code $code;\n"

						+"\t\t:custom $custom.\n"
						+"\n"
						+"} where {}\n"
				),

				(request, query) -> query.setBinding("custom", literal(123))

				)

						.apply(new Response(new Request()

										.method(Request.POST)
										.base(Base)
										.path("/test/request"))

										.status(Response.OK)
										.header("Location", Base+"test/response"),

								new LinkedHashModel(set(data))
						)
				)

						.as("bindings configured")
						.hasSubset(decode("\n"
								+"<test/response>\n"
								+"\t:stem <test/>;\n"
								+"\t:name 'response';\n"
								+"\t:task 'POST';\n"
								+"\t:base <>;\n"
								+"\t:item <test/request>;\n"
								+"\t:user form:none;\n"
								+"\t:code 200.\n"
						))

						.as("timestamp configured")
						.hasStatement(item("test/response"), term("time"), null)

						.as("custom settings applied")
						.hasStatement(item("test/response"), term("custom"), literal(123))

						.as("existing statements forwarded")
						.hasSubset(data)

		);
	}


	@Test void testGenerateAutoIncrementingIds() {
		exec(() -> {

			final BiFunction<Request, Collection<Statement>, String> auto=auto();

			final Request request=new Request().base(Base).path("/target/");

			final String one=auto.apply(request, set());
			final String two=auto.apply(request, set());

			Assertions.assertThat(one).isNotEqualTo(two);

			assertThat(model())
					.doesNotHaveStatement(iri(request.stem(), one), null, null)
					.doesNotHaveStatement(iri(request.stem(), two), null, null);

		});
	}



	//// Graph Operations //////////////////////////////////////////////////////////////////////////////////////////////

	private static final Logger logger=Logger.getLogger(ValuesTest.class.getName());

	private static final String SPARQLPrefixes=Prefixes.entrySet().stream()
			.map(entry -> "prefix "+entry.getKey()+": <"+entry.getValue()+">")
			.collect(joining("\n"));

	public static String sparql(final String sparql) {
		return SPARQLPrefixes+"\n\n"+sparql; // !!! avoid prefix clashes
	}


	public static List<Map<String, Value>> select(final RepositoryConnection connection, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final List<Map<String, Value>> tuples=new ArrayList<>();

			connection
					.prepareTupleQuery(QueryLanguage.SPARQL, sparql(sparql), Base)
					.evaluate(new AbstractTupleQueryResultHandler() {
						@Override public void handleSolution(final BindingSet bindings) {

							final Map<String, Value> tuple=new LinkedHashMap<>();

							for (final Binding binding : bindings) {
								tuple.put(binding.getName(), binding.getValue());
							}

							tuples.add(tuple);

						}
					});

			return tuples;

		} catch ( final MalformedQueryException e ) {

			throw new MalformedQueryException(e.getMessage()+"----\n\n\t"+sparql.replace("\n", "\n\t"));

		}
	}

	public static Model construct(final RepositoryConnection connection, final String sparql) {
		try {

			logger.info("evaluating SPARQL query\n\n\t"
					+sparql.replace("\n", "\n\t")+(sparql.endsWith("\n") ? "" : "\n"));

			final Model model=new LinkedHashModel();

			connection
					.prepareGraphQuery(QueryLanguage.SPARQL, sparql(sparql), Base)
					.evaluate(new StatementCollector(model));

			return model;

		} catch ( final MalformedQueryException e ) {

			throw new MalformedQueryException(e.getMessage()+"----\n\n\t"+sparql.replace("\n", "\n\t"));

		}
	}


	public static Model export(final RepositoryConnection connection, final Resource... contexts) {

		final Model model=new TreeModel();

		connection.export(new StatementCollector(model), contexts);

		return model;
	}


	@SafeVarargs public static Supplier<RepositoryConnection> sandbox(final Iterable<Statement>... datasets) {

		if ( datasets == null ) {
			throw new NullPointerException("null datasets");
		}

		final Repository repository=new SailRepository(new MemoryStore());

		repository.init();

		try (final RepositoryConnection connection=repository.getConnection()) {
			for (final Iterable<Statement> dataset : datasets) {

				if ( dataset == null ) {
					throw new NullPointerException("null dataset");
				}

				connection.add(dataset);
			}
		}

		return repository::getConnection;
	}

}
