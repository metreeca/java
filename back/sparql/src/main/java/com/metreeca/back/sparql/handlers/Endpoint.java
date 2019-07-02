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

package com.metreeca.back.sparql.handlers;

import com.metreeca.form.Form;
import com.metreeca.rest.Request;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.back.sparql.Graph;
import com.metreeca.tray.Tray;
import com.metreeca.tray.Trace;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Sets.set;

import static java.util.Collections.disjoint;


/**
 * SPARQL 1.1 endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 endpoint exposing the contents of the system {@linkplain Graph#graph() graph
 * database}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are restricted
 * to users in the {@linkplain Form#root root} {@linkplain Request#roles() role}, unless otherwise specified.</p>
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-overview-20130321/">SPARQL 1.1 Overview</a>
 */
public abstract class Endpoint<T extends Endpoint<T>> extends Delegator {

	private int timeout=60; // endpoint operations timeout [s]

	private Set<IRI> query=set(Form.root); // roles enabled for query operations
	private Set<IRI> update=set(Form.root); // roles enabled for update operations

	private final Graph graph=Tray.tool(Graph.graph());
	private final Trace trace=Tray.tool(Trace.trace());


	@SuppressWarnings("unchecked") private T self() {
		return (T)this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected int timeout() {
		return timeout;
	}


	protected boolean queryable(final Collection<IRI> roles) {
		return query.isEmpty() || !disjoint(query, roles);
	}

	protected boolean updatable(final Collection<IRI> roles) {
		return update.isEmpty() || !disjoint(update, roles);
	}


	protected Graph graph() {
		return graph;
	}

	protected Trace trace() {
		return trace;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures timeout for endpoint requests.
	 *
	 * @param timeout the timeout for endpoint requests in seconds; 0 to disable timeouts
	 *
	 * @return this endpoint
	 *
	 * @throws IllegalArgumentException if {@code timeout} is less than 0
	 */
	public T timeout(final int timeout) {

		if ( timeout < 0 ) {
			throw new IllegalArgumentException("illegal timeout ["+timeout+"]");
		}

		this.timeout=timeout;

		return self();
	}


	/**
	 * Configures the roles for query operations.
	 *
	 * @param roles the user {@linkplain Request#roles(IRI...) roles} enabled to perform query operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T query(final IRI... roles) {
		return query(list(roles));
	}

	/**
	 * Configures the roles for query operations.
	 *
	 * @param roles the user {@linkplain Request#roles(IRI...) roles} enabled to perform query operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T query(final Collection<? extends IRI> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		this.query=new HashSet<>(roles);

		return self();
	}

	/**
	 * Configures the roles for update operations.
	 *
	 * @param roles the user {@linkplain Request#roles(IRI...) roles} enabled to perform update operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T update(final IRI... roles) {
		return update(list(roles));
	}

	/**
	 * Configures the roles for update operations.
	 *
	 * @param roles the user {@linkplain Request#roles(IRI...) roles} enabled to perform update operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T update(final Collection<? extends IRI> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		this.update=new HashSet<>(roles);

		return self();
	}

}
