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

package com.metreeca.text.endo.mkIII;

import com.metreeca.text.Match;
import com.metreeca.text.Range;
import com.metreeca.rdf.Cell;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Clean;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.model.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.metreeca.text.Range.range;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Logger.time;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.*;

public final class GraphLinkerIII implements Function<Range, Stream<Match<Range, Cell>>> {

	private Function<Range, String> extractor=range -> range.isUpper() ? range.text() : range.root();
	private Function<String, String> normalizer=new Clean().space(true).marks(true).smart(true).space(true);

	private Function<Range, Stream<Range>> scanner=range -> range.ranges().stream(); // !!! default?
	private Function<Stream<String>, Stream<Match<String, Cell>>> matcher=anchors -> Stream.empty(); // !!! default?


	private final Logger logger=service(logger());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public GraphLinkerIII extractor(final Function<Range, String> extractor) {

		if ( extractor == null ) {
			throw new NullPointerException("null extractor");
		}

		this.extractor=extractor;

		return this;
	}

	public GraphLinkerIII normalizer(final Function<String, String> normalizer) {

		if ( extractor == null ) {
			throw new NullPointerException("null normalizer");
		}

		this.normalizer=normalizer;

		return this;
	}


	public GraphLinkerIII scanner(final Function<Range, Stream<Range>> scanner) {

		if ( scanner == null ) {
			throw new NullPointerException("null scanner");
		}

		this.scanner=scanner;

		return this;
	}

	public GraphLinkerIII matcher(final Function<Stream<String>, Stream<Match<String, Cell>>> matcher) {

		if ( matcher == null ) {
			throw new NullPointerException("null matcher");
		}

		this.matcher=matcher;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Match<Range, Cell>> apply(final Range range) {
		if ( range == null || range.text().isEmpty() ) { return Stream.empty(); } else {

			return time(() -> {

				final List<Range> ranges=range.ranges().stream()
						.map(r -> r.text(extractor.andThen(normalizer).apply(r)))
						.collect(toList());

				final List<Range> targets=range(ranges).map(scanner).collect(toList());

				return Xtream

						.from(targets)

						.map(Range::text)// extract targets
						.distinct()

						.pipe(matcher) // match targets to candidates

						.map(match -> match.source(normalizer.apply(match.source()))) // normalize labels
						.distinct()

						.flatMap(match -> // look for complete anchors
								anchor(match.source(), ranges, targets).map(match::source)
						)

						.sorted(Comparator // prefer longer anchors
								.<Match<Range, Cell>>comparingInt(match -> match.source().text().length())
								.reversed()
						)

						.prune((x, y) -> // remove partially overlapping anchors
								!x.source().matches(y.source()) && x.source().intersects(y.source())
						)

						.batch(toList())

						.bagMap(this::weight) // compute local rank

						.sorted(Comparator // prefer weightier anchors
								.<Match<Range, Cell>>comparingDouble(Match::weight)
								.reversed()
						)

						.prune((x, y) -> // remove overlapping anchors
								x.source().intersects(y.source())
						);

			}).apply((t, v) -> logger.info(this, format(
					"processed <%,d> chars in <%,d> ms (<%,d> chars/s)",
					range.text().length(),
					t,
					1000L*range.text().length()/t
			)));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Range> anchor(final CharSequence label, final List<Range> ranges, final List<Range> targets) {
		return IntStream.range(0, ranges.size())

				.mapToObj(i -> {

					final StringBuilder anchor=new StringBuilder(label.length());

					int j=i;

					while ( j < ranges.size() && anchor.length() < label.length() ) {

						if ( j > i && !ranges.get(j-1).borders(ranges.get(j)) ) {
							anchor.append(' ');
						}

						anchor.append(ranges.get(j++).text());
					}

					return i < j && anchor.toString().contentEquals(label) ? ranges.subList(i, j) : null;

				})

				.filter(Objects::nonNull)

				.map(Range::range)

				// the complete anchor range must include at least one of the original partial anchors
				// this avoids matching ranges like 'is' if the text includes 'Iceland'

				.filter(anchor -> targets.stream().anyMatch(anchor::contains));
	}

	private Collection<Match<Range, Cell>> weight(final Collection<Match<Range, Cell>> matches) {

		final double min=matches.parallelStream().mapToDouble(Match::weight).min().orElse(0);
		final double max=matches.parallelStream().mapToDouble(Match::weight).max().orElse(0);

		final DoubleUnaryOperator weight=transform(min, max, 0.0, 0.1);

		final Map<Value, Set<Value>> resources=Xtream // resources to alternatives for the same anchor

				.from(matches)

				.groupBy(match -> match.source().text(), mapping(match -> match.target().focus(), toSet()))

				.map(Map.Entry::getValue)

				.flatMap(entities -> entities.stream().flatMap(x ->
						entities.stream().map(y ->
								new SimpleImmutableEntry<>(x, y)
						)
				))

				.collect(groupingBy(Map.Entry::getKey, mapping(SimpleImmutableEntry::getValue, toSet())));

		final Collection<Statement> connections=matches.stream()

				.map(Match::target)
				.map(Cell::model)

				.flatMap(Collection::stream)

				// consider only links to other candidates

				.filter(statement -> resources.containsKey(statement.getSubject()))
				.filter(statement -> resources.containsKey(statement.getObject()))

				// ignore links to alternatives for the same anchor

				.filter(statement -> !resources.getOrDefault(statement.getSubject(), emptySet())
						.contains(statement.getObject())
				)

				.collect(toSet());

		final Map<Value, Long> connectivity=resources.keySet().stream().collect(toMap(
				value -> value,
				value -> Stream
						.concat(
								connections.stream()
										.filter(statement -> statement.getSubject().equals(value))
										.map(Statement::getObject),
								connections.stream()
										.filter(statement -> statement.getObject().equals(value))
										.map(Statement::getSubject)
						)
						.distinct()
						.count()
		));

		return matches.parallelStream()

				.map(match -> match.weight(weight.applyAsDouble(match.weight())
						+connectivity.getOrDefault(match.target().focus(), 0L) // !!! scale
				))

				.collect(toList());
	}


	private DoubleUnaryOperator transform(final double xmin, final double xmax, final double ymin, final double ymax) {

		final double p=(xmax > xmin) ? (ymax-ymin)/(xmax-xmin) : 0;
		final double q=(xmax > xmin) ? ymin-p*xmin : ymax;

		return v -> p*v+q;

	}

}
