/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf.graphs;

import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.File;


/**
 * RDF4J memory graph store.
 *
 * <p>Manages task execution on an RDF4J {@link SailRepository} backed by a {@link MemoryStore}.</p>
 */
public final class RDF4JMemory extends Graph {

	private final SailRepository repository;


	/**
	 * Creates an ephemeral RDF4J memory graph.
	 */
	public RDF4JMemory() {
		this.repository=new SailRepository(new MemoryStore());
	}

	/**
	 * Creates a persistent RDF4J memory graph.
	 *
	 * @param storage the storage folder where the graph is to be persisted
	 *
	 * @throws NullPointerException     if {@code storage} is null
	 * @throws IllegalArgumentException if {@code storage} is not a folder
	 */
	public RDF4JMemory(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		if ( storage.exists() && !storage.isDirectory() ) {
			throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
		}

		this.repository=new SailRepository(new MemoryStore(storage));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Repository repository() {
		return repository;
	}

	/**
	 * @return {@inheritDoc} ({@link IsolationLevels#SERIALIZABLE})
	 */
	@Override protected IsolationLevel isolation() {
		return IsolationLevels.SERIALIZABLE;
	}

}
