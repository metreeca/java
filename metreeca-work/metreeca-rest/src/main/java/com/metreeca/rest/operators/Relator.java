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

package com.metreeca.rest.operators;

import com.metreeca.link.*;
import com.metreeca.link.queries.*;
import com.metreeca.link.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.http.Locator.service;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Shape.Contains;
import static com.metreeca.link.Values.iri;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Guard.*;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.keeper;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.query;
import static com.metreeca.rest.services.Engine.*;


/**
 * Model-driven resource relator.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
 * focus item}.</p>
 *
 * <ul>
 *
 * <li>redacts the {@linkplain JSONLDFormat#shape(Message) shape} associated with the request according to the request
 * user {@linkplain Request#roles() roles};</li>
 *
 * <li>performs shape-based {@linkplain Wrapper#keeper(Object, Object) authorization}, considering the subset of
 * the request shape enabled by the {@linkplain Guard#Relate} task and the {@linkplain Guard#Digest} view, if the
 * focus item is a {@linkplain Request#collection() collection}, or the {@linkplain Guard#Detail} view, otherwise.</li>
 *
 * </ul>
 *
 * <p>If the focus item is a {@linkplain Request#collection() collection}, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#OK} status code;</li>
 *
 * <li>a shape describing the results;</li>
 *
 * <li>a {@link JSONLDFormat JSON-LD} body containing a description of member linked data resources
 * retrieved with the assistance of the shared linked data {@linkplain Engine#relate(Frame, Query) engine} according to
 * the filtering constraints collected from the request shape and the
 * {@linkplain JSONLDFormat#query(IRI, Shape, String) query} component of the request IRI; the IRI of the target
 * collection is connected to the IRIs of the member resources using the {@link Shape#Contains ldp:contains} property.
 * </li>
 *
 * </ul>
 *
 * <p>Otherwise, if the shared linked data {@linkplain Engine#relate(Frame, Query) engine} is able to retrieve a
 * resource matching the request focus item IRI, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#OK} status code;</li>
 *
 * <li>the response includes the derived shape actually used in the retrieval process;</li>
 *
 * <li>a shape describing the results;</li>
 *
 * <li>a {@link JSONLDFormat JSON-LD} body containing a description of the request item retrieved with the assistance
 * of the shared linked data {@linkplain Engine#relate(Frame, Query) engine}. </li>
 *
 * </ul>
 *
 * <p>Otherwise, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NotFound} status code;</li>
 *
 * </ul>
 */
public final class Relator extends Handler.Base {

	private final Engine engine=service(engine());


	/**
	 * Creates a resource relator.
	 */
	private Relator() {
		delegate(relate().with(wrapper(Request::collection,
				keeper(Relate, Digest),
				keeper(Relate, Detail)
		)));
	}


	private Handler relate() {
		return request -> {

			final boolean collection=request.collection();

			final IRI item=iri(request.item());
			final Shape shape=JSONLDFormat.shape(request);

			return query(item, shape, request.query()).fold(mapper -> request.reply().map(mapper),
                    query -> engine.relate(frame(item), query)

		                    .map(frame -> request.reply().map(response -> JSONLDFormat.shape(response.status(OK),
                                            query.map(new ShapeProbe(collection)))
				                    .body(jsonld(), frame)))

					.orElseGet(() -> request.reply().map(response -> collection
							? JSONLDFormat.shape(response.status(OK), and()).body(jsonld(), frame(item)) // virtual container
							: response.status(NotFound))));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ShapeProbe extends Query.Probe<Shape> {

		private final boolean collection;


		private ShapeProbe(final boolean collection) {
			this.collection=collection;
		}


		@Override public Shape probe(final Items items) { // !!! add Shape.Contains if items.path is not empty
			return (collection ? field(Contains, items.shape()) : items.shape()).redact(Mode, Convey); // remove filters
		}

		@Override public Shape probe(final Stats stats) {
			return StatsShape(stats);
		}

		@Override public Shape probe(final Terms terms) {
			return TermsShape(terms);
		}

	}

}
