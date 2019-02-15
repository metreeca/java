/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.engines.GraphEngine;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import java.util.function.Function;

import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.wrappers.Throttler.entity;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.Tray.tool;


/**
 * LDP resource deleter.
 *
 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}, according to the following operating modes.</p>
 *
 * <p>If the request target is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
 *
 * </ul>
 *
 * <p>If the request includes an expected {@linkplain Request#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#delete}
 * task, {@link Form#convey} mode and {@link Form#detail} view.</li>
 *
 * <li>the existing RDF description of the target resource matched by the redacted shape is deleted.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the existing symmetric concise bounded description of the target resource is deleted.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is removed from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Deleter extends Delegator {

	private final Graph graph=tool(Graph.Factory);

	private final Function<Shape, GraphEngine> engine=shape -> new GraphEngine(graph, shape); // !!! cache


	public Deleter() {
		delegate(deleter().with(throttler()));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return wrapper(Request::container,
				new Throttler(Form.delete, Form.detail, entity()),
				new Throttler(Form.delete, Form.detail, resource())
		);
	}

	private Handler deleter() {
		return request -> request.container()? request.reply(

				new Failure().status(Response.NotImplemented).cause("container deletion not supported")

		) : request.reply(response -> request.shape().map(engine)

				.delete(request.item())

				.map(iri -> response.status(Response.NoContent))

				.orElseGet(() -> response.status(Response.NotFound)) // !!! 410 Gone if previously known

		);
	}

}
