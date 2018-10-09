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


import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.wild;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * RDF processor.
 *
 * <p>Process {@linkplain RDFFormat RDF} payloads for incoming request and outgoing responses and executes SPARQL
 * Update post-processing scripts.</p>
 *
 * <p>If the incoming request is not {@linkplain Request#safe() safe}, wrapped handlers are executed inside a single
 * transaction on the system {@linkplain Graph#Factory graph database}, which is automatically committed on {@linkplain
 * Response#success() successful} response or rolled back otherwise.</p>
 */
public final class Processor implements Wrapper {

	private BiFunction<Request, Model, Model> pre;
	private BiFunction<Response, Model, Model> post;

	private final Collection<String> scripts=new ArrayList<>();

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Inserts a request RDF pre-processing filter.
	 *
	 * <p>The filter is chained after previously inserted pre-processing filters and executed on incoming requests and
	 * their {@linkplain RDFFormat RDF} payload, if one is present, or ignored, otherwise.</p>
	 *
	 * <p>If the request includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	 * statements outside the allowed shape envelope.</p>
	 *
	 * @param filter the request RDF pre-processing filter to be inserted; takes as argument an incoming request and its
	 *               {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor pre(final BiFunction<Request, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.pre=chain(pre, filter);

		return this;
	}

	/**
	 * Inserts a response RDF post-processing filter.
	 *
	 * <p>The filter is chained after previously inserted post-processing filters and executed on {@linkplain
	 * Response#success() successful} outgoing responses and their {@linkplain RDFFormat RDF} payload, if one is
	 * present, or ignored, otherwise.</p>
	 *
	 * <p>If the response includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	 * statements outside the allowed shape envelope.</p>
	 *
	 * @param filter the response RDF post-processing filter to be inserted; takes as argument a successful outgoing
	 *               response and its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor post(final BiFunction<Response, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.post=chain(post, filter);

		return this;
	}

	/**
	 * Inserts a SPARQL Update housekeeping script.
	 *
	 * <p>The script is executed on the shared {@linkplain Graph#Factory graph} tool on {@linkplain Response#success()
	 * successful} request processing by wrapped handlers and before applying {@linkplain #post(BiFunction)
	 * post-processing filters}, with the following pre-defined bindings:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>this</td>
	 * <td>the value of the response {@linkplain Response#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>stem</td>
	 * <td>the {@linkplain IRI#getNamespace() namespace} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>name</td>
	 * <td>the local {@linkplain IRI#getLocalName() name} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>user</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>time</td>
	 * <td>an {@code xsd:dateTime} literal representing the current system time with millisecond precision</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param script the SPARQL Update housekeeping script to be executed by this processor on successful request
	 *               processing; empty scripts are ignored
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code script} is null
	 */
	public Processor sync(final String script) {

		if ( script == null ) {
			throw new NullPointerException("null script script");
		}

		if ( !script.isEmpty() ) {
			scripts.add(script);
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Connector()
				.wrap(pre())
				.wrap(post())
				.wrap(sync())
				.wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> handler.handle(process(request, pre));
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request)
				.map(response -> response.success() ? process(response, post) : response);
	}

	private Wrapper sync() {
		return handler -> request -> handler.handle(request).map(response -> {

			if ( response.success() && !scripts.isEmpty() ) {
				graph.update(connection -> {

					final IRI user=response.request().user();
					final IRI item=response.item();

					for (final String update : scripts) {

						final Update operation=connection.prepareUpdate(QueryLanguage.SPARQL, update, request.base());

						operation.setBinding("this", item);
						operation.setBinding("stem", iri(item.getNamespace()));
						operation.setBinding("name", literal(item.getLocalName()));
						operation.setBinding("user", user);
						operation.setBinding("time", time(true));

						operation.execute();

					}

				});
			}

			return response;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T extends Message<T>> BiFunction<T, Model, Model> chain(
			final BiFunction<T, Model, Model> pipeline, final BiFunction<T, Model, Model> filter
	) {

		final BiFunction<T, Model, Model> checked=(request, model) ->
				requireNonNull(filter.apply(request, model), "null filter return value");

		return (pipeline == null) ? checked
				: (request, model) -> checked.apply(request, pipeline.apply(request, model));
	}

	private <T extends Message<T>> T process(final T message, final BiFunction<T, Model, Model> filter) {
		return message.body(rdf()).pipe(statements -> (filter == null) ? statements
				: trim(message, filter.apply(message, new LinkedHashModel(statements)))
		);
	}

	private <T extends Message<T>> Collection<Statement> trim(final T message, final Model model) {

		final Shape shape=message.shape();
		final Set<Value> focus=singleton(message.item());

		return wild(shape) ? model : shape // !!! migrate wildcard handling to Trimmer
				.accept(new Trimmer(model, focus))
				.collect(toList());
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Model trimmer.
	 *
	 * <p>Recursively extractsall the statements compatible with a shape from a model and an initial collection of
	 * source values .</p>
	 */
	private static final class Trimmer extends Shape.Probe<Stream<Statement>> {

		private final Collection<Statement> model;
		private final Collection<Value> focus;


		private Trimmer(final Collection<Statement> model, final Collection<Value> focus) {
			this.model=model;
			this.focus=focus;
		}


		@Override protected Stream<Statement> fallback(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Statement> visit(final Trait trait) {

			final Step step=trait.getStep();

			final IRI iri=step.getIRI();
			final boolean inverse=step.isInverse();

			final Function<Statement, Value> source=inverse
					? Statement::getObject
					: Statement::getSubject;

			final Function<Statement, Value> target=inverse
					? Statement::getSubject
					: Statement::getObject;

			final Collection<Statement> restricted=model.stream()
					.filter(s -> focus.contains(source.apply(s)) && iri.equals(s.getPredicate()))
					.collect(toList());

			final Set<Value> focus=restricted.stream()
					.map(target)
					.collect(toSet());

			return Stream.concat(restricted.stream(), trait.getShape().accept(new Trimmer(model, focus)));
		}

		@Override public Stream<Statement> visit(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Test test) {
			return Stream.concat(test.getPass().accept(this), test.getFail().accept(this));
		}

		@Override public Stream<Statement> visit(final Group group) {
			return group.getShape().accept(this);
		}

	}

}
