/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.assets;


import com.metreeca.json.Shape;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;


final class GraphUpdater extends GraphProcessor {

	private final Graph graph=com.metreeca.rest.Context.asset(graph());


	com.metreeca.rest.Future<com.metreeca.rest.Response> handle(final com.metreeca.rest.Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private com.metreeca.rest.Future<com.metreeca.rest.Response> holder(final com.metreeca.rest.Request request) {
		return request.reply(status(InternalServerError, new UnsupportedOperationException("holder PUT method")));
	}

	private com.metreeca.rest.Future<com.metreeca.rest.Response> member(final com.metreeca.rest.Request request) {
		return request.body(jsonld()).fold(

				request::reply, model -> request.reply(response -> graph.exec(connection -> {

					final IRI item=iri(request.item());
					final Shape shape=request.attribute(shape());

					return Optional

							.of(fetch(connection, item, items(shape)))

							.filter(current -> !current.isEmpty())

							.map(current -> {

								connection.remove(current);
								connection.add(model);

								return response.status(com.metreeca.rest.Response.NoContent);

							})

							.orElseGet(() ->

									response.status(Response.NotFound) // !!! 410 Gone if previously known

							);

				}))

		);
	}

}

