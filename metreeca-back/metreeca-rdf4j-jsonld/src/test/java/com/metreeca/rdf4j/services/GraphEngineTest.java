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

package com.metreeca.rdf4j.services;

import com.metreeca.core.Locator;
import com.metreeca.jsonld.services.EngineTest;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static com.metreeca.jsonld.services.Engine.engine;
import static com.metreeca.rdf4j.services.Graph.graph;

final class GraphEngineTest extends EngineTest {

	@Override protected void exec(final Runnable... tasks) {
		new Locator()
				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
				.set(engine(), GraphEngine::new)
				.exec(tasks)
				.clear();
	}

}