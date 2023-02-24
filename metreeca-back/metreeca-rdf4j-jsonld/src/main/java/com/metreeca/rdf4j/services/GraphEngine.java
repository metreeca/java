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

package com.metreeca.rdf4j.services;

import com.metreeca.jsonld.services.Engine;
import com.metreeca.link.*;
import com.metreeca.link.queries.*;

import org.eclipse.rdf4j.model.*;

import java.util.Optional;

import static com.metreeca.core.Locator.service;
import static com.metreeca.link.queries.Items.items;
import static com.metreeca.rdf4j.services.Graph.graph;


/**
 * Model-driven graph engine.
 *
 * <p>Handles model-driven CRUD operations on linked data resources stored in the shared {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	private final Graph graph=service(graph());


	private Iterable<Statement> statements(final Frame frame) {
		return () -> frame.model().iterator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional.of(frame.focus())

				.filter(item -> !(item.isResource()
						&& connection.hasStatement((Resource)item, null, null, true)
				))

				.map(item -> {

					connection.add(statements(frame));

					return frame;

				})
		);
	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		return Optional

				.of(query.map(new QueryProbe(this, frame.focus())))

				.filter(current -> !current.isEmpty());
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional

				.of(items(shape).map(new QueryProbe(this, frame.focus())))

				.filter(current -> !current.isEmpty())

				.map(current -> {

					// ;( remove also server-managed properties…

					connection.remove((Resource)frame.focus(), null, null);

					connection.remove(statements(current));
					connection.add(statements(frame));

					return frame;

				})
		);
	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
		return graph.update(connection -> Optional

				.of(items(shape).map(new QueryProbe(this, frame.focus())))

				.filter(current -> !current.isEmpty())

				.map(current -> {

					connection.remove(statements(current));

					return current;

				})
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class QueryProbe extends Query.Probe<Frame> {

		private final Engine engine;
		private final Value focus;


		QueryProbe(final Engine engine, final Value focus) {
			this.engine=engine;
			this.focus=focus;
		}


		@Override public Frame probe(final Items items) {
			return new GraphItems(engine).process(focus, items);
		}

		@Override public Frame probe(final Terms terms) {
			return new GraphTerms(engine).process(focus, terms);
		}

		@Override public Frame probe(final Stats stats) {
			return new GraphStats(engine).process(focus, stats);
		}

	}

}
