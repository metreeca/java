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

package com.metreeca.rest.operators;


import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.IRIPattern;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.md5;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.Guard.Create;
import static com.metreeca.json.shapes.Guard.Detail;
import static com.metreeca.rest.Response.Created;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Wrapper.keeper;
import static com.metreeca.rest.Xtream.encode;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;
import static com.metreeca.rest.services.Engine.engine;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * Model-driven resource creator.
 *
 * <p>Handles creation requests on the linked data collection identified by the request {@linkplain Request#item()
 * focus item}:</p>
 *
 * <ul>
 *
 * <li>redacts the {@linkplain JSONLDFormat#shape() shape} associated with the request according to the request
 * user {@linkplain Request#roles() roles};</li>
 *
 * <li>performs shape-based {@linkplain Wrapper#keeper(Object, Object) authorization}, considering the subset of
 * the request shape enabled by the {@linkplain Guard#Create} task and the {@linkplain Guard#Detail} view;</li>
 *
 * <li>validates the {@link JSONLDFormat JSON-LD} request body against the request shape; malformed or invalid
 * payloads are reported respectively with a {@value Response#BadRequest} or a {@value Response#UnprocessableEntity}
 * status code;</li>
 *
 * <li>generates a unique IRI for the resource to be created on the basis on the stem of the the request IRI and
 * the value of the {@code Slug} request header, if one is found, or a random id, otherwise;</li>
 *
 * <li>rewrites the request body to the assigned IRI and stores it with the assistance of the shared linked data
 * {@linkplain Engine#create(Frame, Shape) engine}; the target collection identified by the request focus item is
 * connected to the newly created resource as {@linkplain Shape#outline(Value...) outlined} in the filtering
 * constraints in the request shape.</li>
 *
 * </ul>
 *
 * <p>On successful completion, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#Created} status code;</li>
 *
 * <li>a {@code Location} HTTP response header advertising the IRI of the newly created resource.</li>
 *
 * </ul>
 */
public final class Creator extends Delegator {

	/**
	 * Creates a resource creator with a UUID-based slug generator.
	 *
	 * @return a new resource creator
	 */
	public static Creator creator() {
		return new Creator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Function<Request, String> slug=request -> md5();

	private final Engine engine=service(engine());


	private Creator() {
		delegate(rewrite().wrap(create()).with( // rewrite immediately before handler, after custom wrappers
				keeper(Create, Detail)
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the slug generator.
	 *
	 * @param slug a function mapping from the creation request to the identifier to be assigned to the newly created
	 *             resource; must return a non-null non-clashing value
	 *
	 * @return this creator handler
	 *
	 * @throws NullPointerException if {@code slug} is null or returns null values
	 */
	public Creator slug(final Function<Request, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		return this;
	}

	/**
	 * Configures the slug generator.
	 *
	 * @param slug a function mapping from the creation request and its {@linkplain JSONLDFormat JSON-LD} payload to the
	 *             identifier to be assigned to the newly created resource; must return a non-null non-clashing value
	 *
	 * @return this creator handler
	 *
	 * @throws NullPointerException if {@code slug} is null or returns null values
	 */
	public Creator slug(final BiFunction<? super Request, ? super Frame, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=request -> slug.apply(request, request.body(jsonld()).fold(
				error -> frame(iri(request.item())),
				value -> value
		));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper rewrite() {
		return handler -> request -> {

			final String name=encode( // encode slug as IRI path component
					requireNonNull(slug.apply(request), "null resource name")
			);

			final IRI source=iri(request.item());
			final IRI target=iri(source, name);

			return handler.handle(request
					.path(request.path()+name)
					.map(jsonld(), frame -> frame(target, rewrite(target, source, frame.model())))
			);
		};
	}

	private Handler create() {
		return request -> {

			final IRI item=iri(request.item());
			final Shape shape=request.attribute(shape());

			return request.body(jsonld()).fold(request::reply, frame -> engine.create(frame, shape)

					.map(Frame::focus)

					.map(iri -> request.reply(response -> response.status(Created)
							.header("Location", Optional // root-relative to support relocation
									.of(iri.stringValue())
									.map(IRIPattern::matcher)
									.filter(Matcher::matches)
									.map(matcher -> matcher.group("pathall"))
									.orElse(iri.stringValue())
							)
					))

					.orElseThrow(() ->
							new IllegalStateException(format("existing resource identifier %s", format(item)))
					)

			);

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> rewrite(final IRI target, final IRI source, final Collection<Statement> model) {
		return model.stream().map(statement -> rewrite(target, source, statement)).collect(toList());
	}

	private Statement rewrite(final IRI target, final IRI source, final Statement statement) {
		return statement(
				rewrite(target, source, statement.getSubject()),
				rewrite(target, source, statement.getPredicate()),
				rewrite(target, source, statement.getObject()),
				rewrite(target, source, statement.getContext())
		);
	}

	private <T extends Value> T rewrite(final T target, final T source, final T value) {
		return source.equals(value) ? target : value;
	}

}
