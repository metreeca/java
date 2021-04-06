/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rest.services;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.Stats;
import com.metreeca.json.queries.Terms;
import com.metreeca.json.shapes.Field;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.operators.Creator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.json.Values.term;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Model-driven storage engine.
 *
 * <p>Handles model-driven CRUD operations on resource managed by a specific storage backend.</p>
 */
public interface Engine {

	public static IRI terms=term("terms");
	public static IRI stats=term("stats");

	public static IRI value=term("value");
	public static IRI count=term("count");

	public static IRI min=term("min");
	public static IRI max=term("max");


	/**
	 * Resource annotation properties.
	 *
	 * <p>RDF properties for human-readable resource annotations (e.g. {@code rdf:label}, {@code rdfs:comment}, …).</p>
	 */
	public static Set<IRI> Annotations=unmodifiableSet(new HashSet<>(asList(RDFS.LABEL, RDFS.COMMENT)));


	/**
	 * Generates the response shape for a stats query.
	 *
	 * @param query the reference stats query
	 *
	 * @return a stats response shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code
	 * query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape StatsShape(final Stats query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Shape resource=ValueShape(query);

		return and(

				field(count, required(), datatype(XSD.INTEGER)),
				field(min, optional(), resource),
				field(max, optional(), resource),

				field(stats, multiple(),
						field(count, required(), datatype(XSD.INTEGER)),
						field(min, required(), resource),
						field(max, required(), resource)
				)

		);
	}

	/**
	 * Generates the response shape for a terms query.
	 *
	 * @param query the reference terms query
	 *
	 * @return a terms response shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code
	 * query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape TermsShape(final Terms query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Shape resource=ValueShape(query);

		return and(
				field(terms, multiple(),
						field(value, required(), resource),
						field(count, required(), datatype(XSD.INTEGER))
				)
		);
	}


	/**
	 * Generates the value shape for a query.
	 *
	 * @param query the reference query
	 *
	 * @return a value shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape ValueShape(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return and(query.shape()
				.redact(Mode, Convey)
				.walk(query.path())
				.map(Field::fields)
				.orElseGet(Stream::empty)
				.filter(field -> Annotations.contains(field.iri())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a transaction wrapper.
	 *
	 * @return a wrapper ensuring that requests are handled within a single transaction on the storage backend
	 */
	public Wrapper transaction();


	//// CRUD Operations ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles creation requests.
	 *
	 * <p>Handles creation requests of the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a creation request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the creation {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 * @implSpec Concrete implementations must assume that {@link Request#path()} was already configured with a unique
	 * identifier for the resource to be created and the {@linkplain JSONLDFormat JSON-LD} payload of the request
	 * rewritten accordingly, for instance by an outer {@link Creator} handler
	 */
	public Future<Response> create(final Request request);

	/**
	 * Handles retrieval requests.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using the message {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a retrieval request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the retrieval {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> relate(final Request request);

	/**
	 * Handles updating requests.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request an updating request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the updating {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> update(final Request request);

	/**
	 * Handles deletion requests.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using  the message {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a deletion request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the deletion {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> delete(final Request request);

}
