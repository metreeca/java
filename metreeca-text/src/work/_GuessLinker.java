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

package com.metreeca.text.endo;

import com.metreeca.text.Match;
import com.metreeca.text.Range;
import com.metreeca.rdf.Cell;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rdf.Cell.cell;
import static com.metreeca.rdf.Values.*;
import static java.util.Collections.indexOfSubList;
import static java.util.stream.Collectors.toList;


public final class _GuessLinker implements Function<Range, Collection<Match<Range, Cell>>> {


	@Override public Collection<Match<Range, Cell>> apply(final Range range) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	/*
	 * Link unmatched ranges to entity placeholders.
	 */
	private List<Match<Range, Cell>> guess(
			final List<Match<Range, Cell>> links, final Map<String, List<Range>> anchors
	) {

		final List<Range> guesses=anchors.values().stream().flatMap(Collection::stream)

				.filter(r -> r.isTyped(ModelFinder.Entity)) // consider only entity ranges

				.filter(r -> links.stream() // ignore ranges intersecting a match
						.map(Match::source)
						.noneMatch(source -> source.intersects(r))
				)

				.filter(r -> r.ranges().stream().noneMatch(l // ignore complex ranges
						-> l.isTyped("CC", "IN")
						&& !l.text().equals("&")
						&& !l.text().equals("for") && !l.text().equals("of") // !!! i18n
				))

				.sorted(Comparator.<Range, Integer>comparing(Range::length).reversed())

				.collect(toList());

		guesses.stream()

				.filter(r -> guesses.stream() // ignore non-maximal ranges
						.noneMatch(c -> !c.equals(r) && c.contains(r))
				)

				.map(r -> {

					final Cell target=links.stream()

							// look for the best/longest match containing the guess and use its target

							.filter(l -> indexOfSubList(words(l.source()), words(r)) >= 0)

							.max(Comparator
									.<Match<Range, Cell>, Double>comparing(Match::weight)
									.thenComparingInt(match -> match.source().length())
							)

							.map(Match::target)

							// if nothing is found, actually insert a target placeholder

							.orElseGet(() -> cell(bnode(md5(r.text())))
									.insert(RDFS.LABEL, literal(r.text()))
									.get()
							);

					return new Match<>(r, target, 0);

				})

				.forEach(links::add);

		return links;
	}


	private List<String> words(final Range range) {
		return leaves(range).map(Range::text).collect(toList());
	}

	private Stream<Range> leaves(final Range range) {
		return range.ranges().isEmpty() ? Stream.of(range) : range.ranges().stream().flatMap(this::leaves);
	}

}
