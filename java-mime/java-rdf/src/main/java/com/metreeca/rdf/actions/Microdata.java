

/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.rdf.actions;

import com.metreeca.core.services.Logger;
import com.metreeca.core.toolkits.Identifiers;
import com.metreeca.link.Values;
import com.metreeca.rdf.schemas.Schema;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.w3c.dom.*;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.link.Values.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Microdata parser.
 *
 * <p>Converts microdata annotations in an XML document into a stream of equivalent RDF statements.</p>
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/microdata.html">HTML Living Standard - § 5 Microdata</a>
 */
public final class Microdata implements Function<Node, Stream<Statement>> {

	private static final String Id="id";

	private static final String ItemScope="itemscope";
	private static final String ItemId="itemid";
	private static final String ItemType="itemtype";
	private static final String ItemProp="itemprop";
	private static final String ItemRef="itemref";

	private static final Pattern SpacePattern=Pattern.compile("[\t\n\f\r ]+");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Logger logger=service(logger());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Statement> apply(final Node node) {
		return node instanceof Document ? items(((Document)node).getDocumentElement())
				: node instanceof Element ? items((Element)node)
				: Stream.empty();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Statement> items(final Element element) {
		return items(element, false, ids(element));
	}

	private Stream<Statement> items(final Element element, final boolean scoped, final Map<String, Element> ids) {
		if ( element.hasAttribute(ItemScope) ) {

			return item(element, ids).getValue();

		} else {

			final boolean scoping=scoped || attribute(element, Id).filter(ids::containsKey).isPresent();

			if ( element.hasAttribute(ItemId) ) {
				logger.warning(this, format("%s requires %s", ItemId, ItemScope)); // !!! report open tag
			}

			if ( element.hasAttribute(ItemType) ) {
				logger.warning(this, format("%s requires %s", ItemType, ItemScope)); // !!! report open tag
			}

			if ( element.hasAttribute(ItemRef) ) {
				logger.warning(this, format("%s outside %s", ItemRef, ItemScope)); // !!! report open tag
			}

			if ( element.hasAttribute(ItemProp) && !(scoping) ) {
				logger.warning(this, format("%s outside %s", ItemProp, ItemScope)); // !!! report open tag
			}

			return children(element).flatMap(child -> items(child, scoping, ids));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map.Entry<Resource, Stream<Statement>> item(final Element element, final Map<String, Element> ids) {

		if ( element.hasAttribute(ItemId) && !element.hasAttribute(ItemType) ) {
			logger.warning(this, format("%s requires %s", ItemId, ItemType)); // !!! report open tag
		}

		final Resource id=id(element);
		final List<IRI> types=types(element).collect(toList());

		final String vocabulary=types.stream().findFirst().map(IRI::getNamespace).orElse(Terms.stringValue());

		Stream<Statement> model=types.stream().map(type -> statement(id, RDF.TYPE, Schema.normalize(type)));

		final Collection<Element> visited=new HashSet<>(singleton(element));
		final Queue<Element> pending=Stream.concat(children(element), attribute(element, ItemRef).stream()

				.flatMap(refs -> Arrays.stream(SpacePattern.split(refs)))

				.map(ref -> {

					final Element target=ids.get(ref);

					if ( target == null ) {
						logger.warning(this, format("undefined %s <%s>", ItemRef, ref));
					}

					return target;

				})

				.filter(Objects::nonNull)

		).collect(toCollection(ArrayDeque::new));

		while ( !pending.isEmpty() ) {

			final Element current=pending.remove();
			final List<IRI> props=props(current, vocabulary).collect(toList());

			if ( !visited.add(current) ) {

				logger.warning(this, format("%s cycle", ItemRef)); // !!! report open tag

			} else if ( current.hasAttribute(ItemScope) ) {

				final Entry<Resource, Stream<Statement>> value=item(current, ids);

				model=Stream.concat(model, props.stream()
						.map(prop -> statement(id, Schema.normalize(prop), Schema.normalize(value.getKey())))
				);

				model=Stream.concat(model, value.getValue());

			} else {

				model=Stream.concat(model, value(current).stream()
						.flatMap(value -> props.stream()
								.map(prop -> statement(id, Schema.normalize(prop), Schema.normalize(value))))
				);

				pending.addAll(children(current).collect(toList()));

			}

		}

		return Map.entry(id, model);
	}


	private Resource id(final Element element) { // !!! refactor
		return attribute(element, ItemId)

				.map(id -> Optional.ofNullable(element.getBaseURI())

						.map(base -> {

							try { return URI.create(base); } catch ( final IllegalArgumentException e ) {

								logger.warning(this, format("malformed base URI <%s>", base));

								return null;

							}

						})

						.map(base -> {

							try { return base.resolve(id); } catch ( final IllegalArgumentException e ) {

								logger.warning(this, format("malformed %s <%s>", ItemId, id)); // !!! report open tag

								return null;

							}

						})

						.map(URI::toString)
						.orElse(id)
				)

				.filter(id -> {

					if ( Identifiers.AbsoluteIRIPattern.matcher(id).matches() ) { return true; } else {

						logger.warning(this, format("malformed %s <%s>", ItemId, id)); // !!! report open tag

						return false;
					}

				})

				.map(Values::iri)
				.map(Resource.class::cast)

				.orElseGet(Values::bnode);
	}

	private Stream<IRI> types(final Element element) {

		final AtomicReference<String> vocabulary=new AtomicReference<>();
		final Set<IRI> types=ConcurrentHashMap.newKeySet();

		return attribute(element, ItemType).stream()

				.flatMap(type -> Arrays.stream(SpacePattern.split(type)))

				.map(type -> {

					try { return iri(URI.create(type)); } catch ( final IllegalArgumentException e ) {

						logger.warning(this, format("malformed %s <%s>", ItemType, type));

						return null;

					}

				})

				.filter(Objects::nonNull)

				.peek(type -> {

					if ( !types.add(type) ) {
						logger.warning(this, format("repeated %s value <%s>", ItemType, type));
					}

				})

				.peek(type -> vocabulary.accumulateAndGet(type.getNamespace(), (current, merged) -> {

					if ( current == null || current.equals(merged) ) {

						return merged;

					} else {

						logger.warning(this, format("multiple %s vocabularies <%s>", ItemType, merged));

						return current;

					}

				}))

				.distinct();
	}

	private Stream<IRI> props(final Element element, final String vocabulary) {

		final Set<IRI> props=ConcurrentHashMap.newKeySet();

		return attribute(element, ItemProp).stream()

				.flatMap(type -> Arrays.stream(SpacePattern.split(type)))

				.map(prop -> {

					try {

						return iri(URI.create(vocabulary).resolve(prop));

					} catch ( final IllegalArgumentException e ) {

						logger.warning(this, format("malformed %s <%s>", ItemProp, prop));

						return null;

					}

				})

				.filter(Objects::nonNull)

				.peek(prop -> {

					if ( !props.add(prop) ) {
						logger.warning(this, format("repeated %s value <%s>", ItemProp, prop));
					}

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Value> value(final Element element) {
		return element.getTagName().equalsIgnoreCase("meta") ? text(element, "content")

				: element.getTagName().equalsIgnoreCase("audio") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("embed") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("iframe") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("img") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("source") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("track") ? link(element, "src")
				: element.getTagName().equalsIgnoreCase("video") ? link(element, "src")

				: element.getTagName().equalsIgnoreCase("a") ? link(element, "href")
				: element.getTagName().equalsIgnoreCase("area") ? link(element, "href")
				: element.getTagName().equalsIgnoreCase("link") ? link(element, "href")

				: element.getTagName().equalsIgnoreCase("object") ? link(element, "data")

				: element.getTagName().equalsIgnoreCase("data") ? text(element, "value")
				: element.getTagName().equalsIgnoreCase("meter") ? number(element, "value")
				: element.getTagName().equalsIgnoreCase("time") ? temporal(element, "datetime")

				: text(element.getTextContent()).map(Values::literal);
	}

	private Optional<Value> link(final Element element, final String attribute) {
		return attribute(element, attribute, true)
				.map(value -> {

					try {

						return URI.create(Optional.ofNullable(element.getBaseURI()).orElse(Values.Base)).resolve(value);

					} catch ( final IllegalArgumentException e ) {

						logger.warning(this, format("malformed %s URL attribute <%s>", attribute, value));

						return null;
					}

				})
				.map(Values::iri);
	}

	private Optional<Value> number(final Element element, final String attribute) {
		return attribute(element, attribute, true)
				.map(value -> {

					try {

						return Double.parseDouble(value);

					} catch ( final NumberFormatException e ) {

						logger.warning(this, format("malformed %s float attribute <%s>", attribute, value));

						return null;
					}

				})
				.map(Values::literal);
	}

	private Optional<Value> temporal(final Element element, final String attribute) {
		return attribute(element, attribute, true)
				.map(Optional::of)
				.orElseGet(() -> text(element.getTextContent()))
				.map(value -> {

					final List<Function<String, Literal>> parsers=asList(
							v -> literal(literal(v).temporalAccessorValue()),
							v -> literal(literal(v).temporalAmountValue())
					);

					for (final Function<String, Literal> parser : parsers) {
						try { return parser.apply(value); } catch ( final RuntimeException ignored ) { }
					}

					logger.warning(this, format("malformed %s time value <%s>", attribute, value));

					return literal(value); // support custom post-processing converters

				});
	}

	private Optional<Value> text(final Element element, final String attribute) {
		return attribute(element, attribute, true).map(Values::literal);
	}

	private Optional<String> text(final String value) {
		return Optional.of(value).map(String::trim).filter(v -> !v.isEmpty());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String tag(final Element element) {

		final StringBuilder builder=new StringBuilder(100);

		builder.append("<").append(element.getTagName().toLowerCase(Locale.ROOT));

		final NamedNodeMap attributes=element.getAttributes();

		for (int i=0; i < attributes.getLength(); ++i) {
			builder.append(" ").append(attributes.item(i).getNodeName())
					.append("='").append(attributes.item(i).getNodeValue()).append("'");
		}

		builder.append("/>");

		return builder.toString();
	}

	private Optional<String> attribute(final Element element, final String attribute) {
		return attribute(element, attribute, false);
	}

	private Optional<String> attribute(final Element element, final String attribute, final boolean required) {
		return Optional.of(element.getAttribute(attribute)).map(String::trim).filter(value -> {

			final boolean empty=value.isEmpty();
			final boolean defined=element.hasAttribute(attribute);

			if ( empty && defined ) {
				logger.warning(this, format("empty %s attribute", attribute)); // !!! report open tag
			}

			if ( required && !defined ) {
				logger.warning(this, format("missing %s attribute", attribute)); // !!! report open tag
			}

			return !empty;

		});
	}

	private Stream<Element> children(final Node node) {
		return Stream.of(node.getChildNodes())
				.flatMap(list -> IntStream.range(0, list.getLength()).mapToObj(list::item))
				.filter(Element.class::isInstance)
				.map(Element.class::cast);
	}

	private Map<String, Element> ids(final Element element) {

		final Map<String, Element> ids=new HashMap<>();
		final Set<String> refs=new HashSet<>();

		final Queue<Element> pending=new ArrayDeque<>(singleton(element));

		while ( !pending.isEmpty() ) {

			final Element current=pending.remove();

			attribute(current, Id).ifPresent(id -> ids.put(id, current));
			attribute(current, ItemRef).ifPresent(refs::add);

			children(current).forEach(pending::add);
		}

		ids.keySet().retainAll(refs); // retain ids only if actually referenced from itemref

		return ids;
	}

}
