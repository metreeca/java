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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.form.things.Values;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.Collection;

import static com.metreeca.rest.wrappers.Transactor.transactor;
import static com.metreeca.form.things.Bindings.bindings;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


/**
 * Activity tracer.
 *
 * <p>Creates an audit trail record in the shared {@linkplain Graph#Factory graph} tool on successful request processing
 * by the wrapped handler.</p>
 */
public final class Tracer implements Wrapper {

	private Value task=RDF.NIL;
	private String sparql="";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Tracer task(final Value task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		this.task=task;

		return this;
	}

	public Tracer sparql(final String sparql) {

		if ( sparql == null ) {
			throw new NullPointerException("null sparql");
		}

		this.sparql=sparql;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {
			try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
				transactor(connection, true).wrap((_request, _response) -> handler.handle(

						writer ->

								writer.copy(_request).done(),

						reader -> {

							if ( reader.success() ) {

								final String method=_request.method();

								final IRI trace=iri();

								final IRI user=_request.user();
								final IRI item=reader.focus();
								final Value task=!this.task.equals(RDF.NIL) ? this.task // !!! refactor
										: method.equals(Request.GET) ? Link.relate
										: method.equals(Request.PUT) ? Link.update
										: method.equals(Request.DELETE) ? Link.delete
										: method.equals(Request.POST) ? reader.status() == Response.Created ? Link.create : Link.update
										: _request.safe() ? Link.relate : Link.update;


								final Literal time=Values.time(true);

								// add default trace record

								final Collection<Statement> model=new ArrayList<>();

								model.add(statement(trace, RDF.TYPE, Link.Trace));
								model.add(statement(trace, Link.item, item));
								model.add(statement(trace, Link.task, task));
								model.add(statement(trace, Link.user, user));
								model.add(statement(trace, Link.time, time));

								connection.add(model);

								// add custom info

								if ( !sparql.isEmpty() ) {
									bindings()

											.set("this", trace)
											.set("item", item)
											.set("task", task)
											.set("user", user)
											.set("time", time)

											.bind(connection.prepareUpdate(SPARQL, sparql, _request.base()))

											.execute();
								}

							}

							_response.copy(reader).done();

						}

				)).handle(request, response);
			}
		};
	}

}