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

package com.metreeca.link.shapes;

import com.metreeca.core.Strings;
import com.metreeca.link.Shape;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.link.Values.derives;
import static com.metreeca.link.Values.direct;
import static com.metreeca.link.Values.format;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Any.any;
import static com.metreeca.link.shapes.Lang.lang;
import static com.metreeca.link.shapes.Range.range;
import static com.metreeca.link.shapes.When.when;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;


/**
 * Disjunctive logical constraint.
 *
 * <p>States that the focus set is consistent with at least one shape in a given target set.</p>
 */
public final class Or extends Shape {

	private static final Shape empty=new Or(Collections.emptySet());


	public static Shape or() {
		return empty;
	}

	public static Shape or(final Shape... shapes) {

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return or(asList(shapes));
	}

	public static Shape or(final Collection<? extends Shape> shapes) {

		if ( shapes == null || shapes.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return or(shapes.stream());
	}

	public static Shape or(final Stream<? extends Shape> shapes) {
		return pack(shapes.peek(shape -> requireNonNull(shape, "null shapes"))

				// flatten nested shaped shapes of the same type

				.flatMap(new Shape.Probe<Stream<Shape>>() {

					@Override public Stream<Shape> probe(final Or or) { return or.shapes().stream(); }

					@Override public Stream<Shape> probe(final Shape shape) { return Stream.of(shape); }

				})

				// group by shape type preserving order

				.collect(groupingBy(Shape::getClass, LinkedHashMap::new, toCollection(LinkedHashSet::new)))

				.entrySet()
				.stream()

				// merge shapes sets

				.flatMap(entry -> merge(entry.getKey(), entry.getValue().stream()))

				.collect(toList())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Shape pack(final List<? extends Shape> shapes) {
		return shapes.contains(and()) ? and() // always pass
				: shapes.size() == 1 ? shapes.iterator().next()
				: new Or(shapes);
	}

	private static Stream<? extends Shape> merge(final Class<? extends Shape> clazz, final Stream<Shape> shapes) {
		return clazz.equals(Datatype.class) ? datatypes(shapes.map(Datatype.class::cast))
				: clazz.equals(Range.class) ? ranges(shapes.map(Range.class::cast))
				: clazz.equals(Lang.class) ? langs(shapes.map(Lang.class::cast))
				: clazz.equals(MinCount.class) ? minCounts(shapes.map(MinCount.class::cast))
				: clazz.equals(MaxCount.class) ? maxCounts(shapes.map(MaxCount.class::cast))
				: clazz.equals(Any.class) ? anys(shapes.map(Any.class::cast))
				: clazz.equals(Localized.class) ? localizeds(shapes.map(Localized.class::cast))
				: clazz.equals(Field.class) ? fields(shapes.map(Field.class::cast))
				: clazz.equals(Link.class) ? links(shapes.map(Link.class::cast))
				: clazz.equals(When.class) ? whens(shapes.map(When.class::cast))
				: shapes;
	}


	private static Stream<? extends Shape> datatypes(final Stream<Datatype> datatypes) {

		final Collection<Datatype> space=datatypes.collect(toList());

		return space.stream().filter(lower -> space.stream()
				.filter(upper -> !upper.equals(lower))
				.noneMatch(upper -> derives(upper.iri(), lower.iri()))
		);
	}

	private static Stream<? extends Shape> ranges(final Stream<Range> ranges) {
		return Stream.of(range(ranges // value sets union
				.map(Range::values)
				.flatMap(Collection::stream)
				.collect(toCollection(LinkedHashSet::new))
		));
	}

	private static Stream<? extends Shape> langs(final Stream<Lang> langs) {
		return Stream.of(lang(langs
				.map(Lang::tags)
				.reduce((x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toCollection(LinkedHashSet::new)))
				.orElseGet(Collections::emptySet)));
	}

	private static Stream<? extends Shape> minCounts(final Stream<MinCount> minCounts) {
		return minCounts
				.mapToInt(MinCount::limit)
				.filter(limit -> limit > 0)
				.min()
				.stream()
				.mapToObj(MinCount::minCount);
	}

	private static Stream<? extends Shape> maxCounts(final Stream<MaxCount> maxCounts) {
		return maxCounts
				.mapToInt(MaxCount::limit)
				.filter(limit -> limit < Integer.MAX_VALUE)
				.max()
				.stream()
				.mapToObj(MaxCount::maxCount);
	}

	private static Stream<? extends Shape> anys(final Stream<Any> anys) {
		return Stream.of(any(anys // value sets union
				.map(Any::values)
				.flatMap(Collection::stream)
				.collect(toSet())
		));
	}

	private static Stream<? extends Shape> localizeds(final Stream<Localized> localizeds) {
		return localizeds.distinct();
	}

	private static Stream<? extends Shape> fields(final Stream<Field> fields) {
		return fields

				.collect(groupingBy(Field::iri, LinkedHashMap::new, reducing((x, y) -> new Field(

						Field.label(x.label(), y.label()).orElseThrow(() -> new IllegalArgumentException(format(
								"clashing labels <%s> / <%s> for field %s", x.label(), y.label(), format(x.iri())
						))),

						x.iri(),

						or(x.shape(), y.shape())

				))))

				.values()
				.stream()

				.filter(Optional::isPresent)
				.map(Optional::get)

				.map(field -> field.shape().equals(or()) ? and() : field);
	}

	private static Stream<? extends Shape> links(final Stream<Link> links) {
		return links

				.collect(groupingBy(Link::iri, LinkedHashMap::new, reducing((x, y) -> new Link(

						x.iri(),

						or(x.shape(), y.shape())

				))))

				.values()
				.stream()

				.filter(Optional::isPresent)
				.map(Optional::get)

				.map(new Function<Link, Shape>() {

					private final AtomicReference<Boolean> way=new AtomicReference<>();

					@Override public Shape apply(final Link link) {
						if ( link.shape().equals(or()) ) { return and(); } else {

							final Boolean x=direct(link.iri());
							final Boolean y=way.getAndSet(x);

							if ( y != null && !y.equals(x) ) {
								throw new IllegalArgumentException("both direct and inverse links specified");
							}

							return link;
						}
					}
				});
	}

	private static Stream<? extends Shape> whens(final Stream<When> whens) {
		return whens

				.collect(groupingBy(When::test))

				.entrySet()
				.stream()

				.map(entry -> when(entry.getKey(),
						or(entry.getValue().stream().map(When::pass)),
						or(entry.getValue().stream().map(When::fail))
				));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<Shape> shapes;


	private Or(final Collection<? extends Shape> shapes) {
		this.shapes=new LinkedHashSet<>(shapes);
	}


	public Collection<Shape> shapes() {
		return Collections.unmodifiableCollection(shapes);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Or
				&& shapes.equals(((Or)object).shapes);
	}

	@Override public int hashCode() {
		return shapes.hashCode();
	}

	@Override public String toString() {
		return "or("+(shapes.isEmpty() ? "" : shapes.stream()
				.map(shape -> Strings.indent(shape.toString()))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}

}
