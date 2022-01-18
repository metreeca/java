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

package com.metreeca.text.endo.mkII;

import com.metreeca.text.Match;
import com.metreeca.text.Range;
import com.metreeca.rdf.Cell;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Clean;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.metreeca.rdf.Cell.cell;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Logger.time;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.*;

public final class GraphLinkerII implements Function<Range, Collection<Match<Range, Cell>>> {

	public static interface Source {

		public Xtream<Match<String, Resource>> lookup(final Xtream<String> anchors);

		public Xtream<Match<Resource, Collection<Statement>>> expand(final Xtream<Resource> resources);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Predicate<Range> spotter=range -> false; // !!! default?

	private Function<Range, String> extractor=range -> range.isUpper() ? range.text() : range.root();
	private UnaryOperator<String> normalizer=new Clean().space(true).marks(true).smart(true);

	private Source source=new GraphDBSource();

	private final Logger logger=service(logger());


	public GraphLinkerII spotter(final Predicate<Range> spotter) {

		if ( spotter == null ) {
			throw new NullPointerException("null spotter");
		}

		this.spotter=spotter;

		return this;
	}


	public GraphLinkerII extractor(final Function<Range, String> extractor) {

		if ( extractor == null ) {
			throw new NullPointerException("null extractor");
		}

		this.extractor=extractor;

		return this;
	}

	public GraphLinkerII normalizer(final UnaryOperator<String> normalizer) {

		if ( normalizer == null ) {
			throw new NullPointerException("null normalizer");
		}

		this.normalizer=normalizer;

		return this;
	}


	public GraphLinkerII source(final Source source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		this.source=source;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Match<Range, Cell>> apply(final Range range) {
		if ( range == null || range.text().isEmpty() ) { return emptySet(); } else {

			return time(() -> {

				final List<Range> ranges=range.ranges().stream()
						.map(r -> r.text(normalizer.compose(extractor).apply(r)))
						.collect(toList());

				return Xtream

						.from(ranges)

						.filter(spotter).map(Range::text).distinct() // extract unique targets

						.pipe(source::lookup) // look up candidates with a label containing at least a target

						.map(m -> m.source(normalizer.apply(m.source()))).distinct() // remove diacritics

						.flatMap(candidate -> // look for complete anchors

								anchor(candidate.source(), ranges).map(candidate::source)

						)

						.sorted(Comparator // prefer longer anchors
								.<Match<Range, Resource>>comparingInt(match -> match.source().text().length())
								.reversed()
						)

						.prune((x, y) -> // remove partially overlapping anchors
								x.source().intersects(y.source()) && !x.source().matches(y.source())
						)

						.batch(toList())

						.flatMap(candidates -> { // !!! only if ambiguities are actually present

							final Map<Resource, Double> weights=weight(Xtream
									.from(candidates)
									.map(Match::target)
									.pipe(source::expand)
									.collect(toList())
							);

							return candidates.stream().map(match -> match
									.target(cell(match.target()).get())
									.weight(Optional
											.ofNullable(weights.get(match.target()))
											.orElse(0.0)
									));

						})

						.sorted(Comparator // prefer weightier anchors
								.<Match<Range, Cell>>comparingDouble(Match::weight)
								.reversed()
						)

						.prune((x, y) -> // remove overlapping anchors
								x.source().intersects(y.source())
						)

						.collect(toList());

			}).apply((t, v) -> logger.info(this, format(
					"linked <%,d> entities from <%,d> chars in <%,d> ms (<%,d> chars/s)",
					v.size(),
					range.text().length(),
					t,
					1000L*range.text().length()/t
			)));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Range> anchor(final CharSequence label, final List<Range> ranges) {
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

				.filter(rs -> rs.stream().anyMatch(spotter)) // at least a target word

				.map(Range::range);
	}

	private Map<Resource, Double> weight(final Collection<Match<Resource, Collection<Statement>>> candidates) {

		final double min=candidates.parallelStream().mapToDouble(Match::weight).min().orElse(0);
		final double max=candidates.parallelStream().mapToDouble(Match::weight).max().orElse(0);

		final double u=0.1; // upper weight
		final double l=0.0; // lower weight

		final double p=(max > min) ? (u-l)/(max-min) : 0;
		final double q=(max > min) ? l-p*min : u;


		final Collection<Value> resources=candidates.stream()

				.map(Match::source)

				.collect(toSet());

		final Collection<Statement> connections=candidates.stream()

				.flatMap(candidate -> candidate.target().stream())

				.filter(statement -> resources.contains(statement.getSubject()))
				.filter(statement -> resources.contains(statement.getObject()))
				.filter(statement -> !statement.getSubject().equals(statement.getObject()))

				.collect(toSet());

		final Map<Value, Long> connectivity=resources.stream().collect(toMap(
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


		return candidates.parallelStream()

				.map(match -> match.weight(
						connectivity.getOrDefault(match.source(), 0L)+p*match.weight()+q
				))

				.collect(toMap(Match::source, Match::weight));

	}

}
