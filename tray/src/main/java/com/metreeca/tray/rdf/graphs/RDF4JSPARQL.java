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
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;


/**
 * RDF4J SPARQL graph store.
 *
 * <p>Manages task execution on an RDF4J {@link SPARQLRepository}.</p>
 */
public final class RDF4JSPARQL extends Graph {

	private final SPARQLRepository repository; // ;( namespace ops silently ignored


	/**
	 * Creates an RDF4J SPARQL graph.
	 *
	 * @param url the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *            1.1 Protocol</a> query/update operations
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public RDF4JSPARQL(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null endpoint url");
		}

		repository=new SPARQLRepository(url);
	}

	/**
	 * Creates an RDF4J SPARQL graph.
	 *
	 * @param query the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *            1.1 Protocol</a> query operations
	 * @param update the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *            1.1 Protocol</a> update operations
	 *
	 * @throws NullPointerException if either {@code query} or {@code update} is null
	 */
	public RDF4JSPARQL(final String query, final String update) {

		if ( query == null ) {
			throw new NullPointerException("null query endpoint URL");
		}

		if ( update == null ) {
			throw new NullPointerException("null update endpoint URL");
		}

		repository=new SPARQLRepository(query, update);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the credentials for accessing the remote RDF repository.
	 *
	 * @param usr the username of the account on the remote RDF repository
	 * @param pwd the password of the account on the remote RDF repository
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if either {@code usr} or {@code pwd} is {@code null}
	 * @throws IllegalStateException if the backing remote repository was already initialized
	 */
	public RDF4JSPARQL credentials(final String usr, final String pwd) {

		if ( usr == null ) {
			throw new NullPointerException("null usr");
		}

		if ( pwd == null ) {
			throw new NullPointerException("null pwd");
		}

		if ( repository.isInitialized() ) {
			throw new IllegalStateException("active repository");
		}

		if ( !usr.isEmpty() || !pwd.isEmpty() ) {
			repository.setUsernameAndPassword(usr, pwd);
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Repository repository() {
		return repository;
	}

	/**
	 * @return {@inheritDoc} ({@link IsolationLevels#NONE})
	 */
	@Override protected IsolationLevel isolation() {
		return IsolationLevels.NONE;
	}

}