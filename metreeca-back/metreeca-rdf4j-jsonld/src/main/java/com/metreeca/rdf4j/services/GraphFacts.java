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

package com.metreeca.rdf4j.services;

import com.metreeca.core.Snippets;
import com.metreeca.core.Strings;
import com.metreeca.http.services.Logger;
import com.metreeca.link.Shape;
import com.metreeca.link.Values;
import com.metreeca.link.shapes.*;
import com.metreeca.rdf4j.Config;
import com.metreeca.rdf4j.SPARQL;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.core.Snippets.text;
import static com.metreeca.core.Snippets.*;
import static com.metreeca.http.Locator.service;
import static com.metreeca.http.services.Logger.logger;
import static com.metreeca.http.services.Logger.time;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.All.all;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Link.link;
import static com.metreeca.link.shapes.Or.or;
import static com.metreeca.link.shapes.When.when;
import static com.metreeca.rdf4j.SPARQL.datatype;
import static com.metreeca.rdf4j.SPARQL.edge;
import static com.metreeca.rdf4j.SPARQL.eq;
import static com.metreeca.rdf4j.SPARQL.filter;
import static com.metreeca.rdf4j.SPARQL.gt;
import static com.metreeca.rdf4j.SPARQL.gte;
import static com.metreeca.rdf4j.SPARQL.in;
import static com.metreeca.rdf4j.SPARQL.isBlank;
import static com.metreeca.rdf4j.SPARQL.isIRI;
import static com.metreeca.rdf4j.SPARQL.isLiteral;
import static com.metreeca.rdf4j.SPARQL.lang;
import static com.metreeca.rdf4j.SPARQL.lt;
import static com.metreeca.rdf4j.SPARQL.lte;
import static com.metreeca.rdf4j.SPARQL.neq;
import static com.metreeca.rdf4j.SPARQL.optional;
import static com.metreeca.rdf4j.SPARQL.or;
import static com.metreeca.rdf4j.SPARQL.regex;
import static com.metreeca.rdf4j.SPARQL.str;
import static com.metreeca.rdf4j.SPARQL.string;
import static com.metreeca.rdf4j.SPARQL.strlen;
import static com.metreeca.rdf4j.SPARQL.strstarts;
import static com.metreeca.rdf4j.SPARQL.union;
import static com.metreeca.rdf4j.SPARQL.values;
import static com.metreeca.rdf4j.SPARQL.var;

import static java.lang.String.format;
import static java.lang.String.valueOf;

abstract class GraphFacts {

	static final String root="0";


	static String alias(final String id) {
		return id+'a';
	}

	static String value(final String id) {
		return id+'v';
	}


	/**
	 * @param id  a template node id
	 * @param ids the set of known template node ids
	 *
	 * @return the id of the principal node proxied by {@code id}
	 *
	 * @see "linking @ metreeca-link design notes"
	 */
	static String principal(final String id, final Collection<String> ids) {
		return id.endsWith("a") ? id
				: id.endsWith("v") ? id.substring(0, id.length()-1)
				: Optional.of(alias(id)).filter(ids::contains).orElse(id);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Config config;

	private int label=1; // the next label available for tagging (0 reserved for the root node)

	private final Logger logger=service(logger());


	GraphFacts(final Config config) {
		this.config=config;
	}


	Config config() {
		return config;
	}


	String label() {
		return valueOf(label++);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	String compile(final Supplier<String> generator) {
		return time(generator).apply((t, v) -> logger

				.debug(this, () -> format("executing %s", v))
				.debug(this, () -> format("generated in <%,d> ms", t))

		);
	}

	void evaluate(final Runnable task) {
		time(task).apply(t -> logger

				.debug(this, () -> format("evaluated in <%,d> ms", t))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Consumer<Appendable> tree(final Shape shape, final boolean required) {
		return list(

				all(shape) // root universal constraints
						.map(values -> space(values(var(root), values)))
						.map(Snippets::space)
						.orElse(nothing()),

				space(shape.map(new TreeProbe(root, required)))

		);
	}

	static Shape path(final Shape shape, final List<IRI> path) {
		return Optional.of(shape.map(new PathProbe(path)))

				.filter(s -> path.isEmpty() || !s.equals(and()))

				.orElseThrow(() ->

						new IllegalArgumentException(format("unknown path step %s", Values.format(path.get(0))))

				);
	}

	static String hook(final Shape shape, final List<IRI> path) {
		return hook(root, shape, path);
	}


	private static String hook(final String anchor, final Shape shape, final List<IRI> path) {
		return Optional.ofNullable(shape.map(new HookProbe(anchor, path)))

				.orElseGet(() -> {

					if ( !path.isEmpty() ) {
						throw new IllegalArgumentException(format("unknown path step %s", Values.format(path.get(0))));
					}

					return anchor;

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TreeProbe extends Shape.Probe<Consumer<Appendable>> {

		private final String anchor;

		private final boolean required;


		private TreeProbe(final String anchor, final boolean filter) {
			this.anchor=anchor;
			this.required=filter;
		}


		@Override public Consumer<Appendable> probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing() : line(filter(

					iri.equals(ResourceType) ? or(isBlank(var(anchor)), isIRI(var(anchor)))

							: iri.equals(BNodeType) ? isBlank(var(anchor))
							: iri.equals(IRIType) ? isIRI(var(anchor))
							: iri.equals(LiteralType) ? isLiteral(var(anchor))
							: iri.equals(RDF.LANGSTRING) ? neq(lang(var(anchor)), string(""))

							: eq(datatype(var(anchor)), SPARQL.value(iri))

			));
		}

		@Override public Consumer<Appendable> probe(final Clazz clazz) {
			return edge(var(anchor), text("a/rdfs:subClassOf*"), SPARQL.value(clazz.iri()));
		}

		@Override public Consumer<Appendable> probe(final Range range) {
			if ( required ) {

				return probe((Shape)range); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return range.values().isEmpty() ? nothing() : line(filter(
						in(var(anchor), range.values().stream().map(SPARQL::value))
				));

			}
		}

		@Override public Consumer<Appendable> probe(final Lang lang) {
			if ( required ) {

				return probe((Shape)lang); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return lang.tags().isEmpty() ? nothing() : filter(
						in(lang(var(anchor)),
								lang.tags().stream().map(Strings::quote).map(Snippets::text))
				);

			}
		}


		@Override public Consumer<Appendable> probe(final MinExclusive minExclusive) {
			return filter(gt(var(anchor), SPARQL.value(minExclusive.limit())));
		}

		@Override public Consumer<Appendable> probe(final MaxExclusive maxExclusive) {
			return filter(lt(var(anchor), SPARQL.value(maxExclusive.limit())));
		}

		@Override public Consumer<Appendable> probe(final MinInclusive minInclusive) {
			return filter(gte(var(anchor), SPARQL.value(minInclusive.limit())));
		}

		@Override public Consumer<Appendable> probe(final MaxInclusive maxInclusive) {
			return filter(lte(var(anchor), SPARQL.value(maxInclusive.limit())));
		}


		@Override public Consumer<Appendable> probe(final MinLength minLength) {
			return filter(gte(strlen(str(var(anchor))), text(minLength.limit())));
		}

		@Override public Consumer<Appendable> probe(final MaxLength maxLength) {
			return filter(lte(strlen(str(var(anchor))), text(maxLength.limit())));
		}


		@Override public Consumer<Appendable> probe(final Pattern pattern) {
			return filter(regex(
					str(var(anchor)), text(Strings.quote(pattern.expression())), text(Strings.quote(pattern.flags()))
			));
		}

		@Override public Consumer<Appendable> probe(final Like like) {
			return filter(regex(
					str(var(anchor)), text(Strings.quote(like.toExpression()))
			));
		}

		@Override public Consumer<Appendable> probe(final Stem stem) {
			return filter(strstarts(
					str(var(anchor)), text(Strings.quote(stem.prefix()))
			));
		}


		@Override public Consumer<Appendable> probe(final MinCount minCount) {
			return required ? probe((Shape)minCount) : nothing();
		}

		@Override public Consumer<Appendable> probe(final MaxCount maxCount) {
			return required ? probe((Shape)maxCount) : nothing();
		}


		@Override public Consumer<Appendable> probe(final All all) {
			return nothing(); // universal constraints handled by skeleton()
		}

		@Override public Consumer<Appendable> probe(final Any any) {
			return space(anchor.equals(root)
					? values(var(anchor), any.values())
					: filter(in(var(anchor), any.values().stream().map(SPARQL::value)))
			);
		}

		@Override public Consumer<Appendable> probe(final Localized localized) {
			return required ? probe((Shape)localized) : nothing();
		}


		@Override public Consumer<Appendable> probe(final Field field) {

			final String label=field.label();
			final IRI iri=field.iri();
			final Shape shape=field.shape();

			return shape.map(new Shape.Probe<>() {

				@Override public Consumer<Appendable> probe(final Or or) { // push field inside union
					return space(union(or.shapes().stream().map(s ->
							block(space(field(label, iri, s).map(TreeProbe.this)))
					)));
				}

				@Override protected Consumer<Appendable> probe(final Shape shape) {

					final Consumer<Appendable> list=list(

							space(edge(var(anchor), SPARQL.value(iri), indent(list(", ", Stream.concat(

									Stream.of(var(label)), // filtering/projection hook

									all(shape).orElseGet(Collections::emptySet).stream().map(SPARQL::value)

							))))),

							space(shape.map(new TreeProbe(label, required)))

					);

					return required ? list : space(optional(list));
				}

			});

		}

		@Override public Consumer<Appendable> probe(final Link link) {

			final IRI iri=link.iri();
			final Shape shape=link.shape();

			final String id=direct(iri) ? value(anchor) : alias(anchor);

			return list(

					space(edge(var(anchor), SPARQL.value(iri), var(id))),

					space(shape.map(new TreeProbe(id, required)))

			);
		}


		@Override public Consumer<Appendable> probe(final And and) {
			return list(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Consumer<Appendable> probe(final Or or) {
			return space(union(or.shapes().stream().map(s -> block(space(s.map(this))))));
		}


		@Override public Consumer<Appendable> probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

	private static final class PathProbe extends Shape.Probe<Shape> {

		private final List<IRI> path;


		PathProbe(final List<IRI> path) {
			this.path=path;
		}


		@Override public Shape probe(final Field field) {
			return path.isEmpty() || !field.iri().equals(path.get(0)) ? and() : field(
					field.label(), field.iri(), path(field.shape(), path.subList(1, path.size()))
			);
		}

		@Override public Shape probe(final Link link) {
			return link(link.iri(), link.shape().map(this));
		}


		@Override public Shape probe(final When when) {
			return when(when.test(), when.pass().map(this), when.fail().map(this));
		}

		@Override public Shape probe(final And and) {
			return and(and.shapes().stream().map(this));
		}

		@Override public Shape probe(final Or or) {
			return or(or.shapes().stream().map(this));
		}


		@Override protected Shape probe(final Shape shape) {
			return and();
		}

	}

	private static final class HookProbe extends Shape.Probe<String> {

		private final String anchor;
		private final List<IRI> path;


		HookProbe(final String anchor, final List<IRI> path) {
			this.anchor=anchor;
			this.path=path;
		}


		@Override public String probe(final Field field) {
			return path.isEmpty() || !field.iri().equals(path.get(0)) ? null : Optional
					.ofNullable(hook(field.label(), field.shape(), path.subList(1, path.size())))
					.orElse(field.label());
		}

		@Override public String probe(final Link link) {
			return Optional
					.ofNullable(link.shape().map(this))
					.orElseGet(() -> direct(link.iri()) ? value(anchor) : alias(anchor));
		}


		@Override public String probe(final When when) {
			throw new UnsupportedOperationException(when.toString()); // make sure we can't reach multiple field hooks
		}

		@Override public String probe(final And and) {
			return probe(and.shapes().stream());
		}

		@Override public String probe(final Or or) {
			return probe(or.shapes().stream());
		}


		private String probe(final Stream<Shape> shapes) {
			return shapes.map(this).filter(Objects::nonNull).findFirst().orElse(null);
		}

	}

}
