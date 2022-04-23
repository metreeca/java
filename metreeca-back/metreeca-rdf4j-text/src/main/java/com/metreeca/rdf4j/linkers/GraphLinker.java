/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rdf4j.linkers;

import com.metreeca.link.Frame;
import com.metreeca.text.*;
import com.metreeca.text.actions.Normalize;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.metreeca.link.Frame.frame;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.*;

/**
 * Graph-driven entity linker
 */
public final class GraphLinker implements Function<Chunk, Stream<Match<Chunk, Frame>>> {

	private Function<Chunk, Stream<Chunk>> finder=chunk -> chunk.tokens().stream().map(Chunk::new); // !!! default?

	private Function<Token, String> reader=token -> token.text(token.isUpper());
	private Function<String, String> normalizer=new Normalize().space(true).marks(true).smart(true);

	private Function<Stream<String>, Stream<Match<String, Frame>>> matcher=anchors -> Stream.empty(); // !!! default?


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures anchor extraction.
	 *
	 * @param finder a function extracting a stream of anchor chunks from a phrase chunk
	 *
	 * @return this linker
	 *
	 * @throws NullPointerException if {@code finder} is {@code null}
	 */
	public GraphLinker finder(final Function<Chunk, Stream<Chunk>> finder) {

		if ( finder == null ) {
			throw new NullPointerException("null finder");
		}

		this.finder=finder;

		return this;
	}

	/**
	 * Configures token to text conversion.
	 *
	 * @param reader a function mapping tokens to textual representations to be fed to the {@linkplain #matcher}
	 *
	 * @return this linker
	 *
	 * @throws NullPointerException if {@code reader} is {@code null}
	 */
	public GraphLinker reader(final Function<Token, String> reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		this.reader=reader;

		return this;
	}

	/**
	 * Configures text normalization.
	 *
	 * @param normalizer a text normalization function
	 *
	 * @return this linker
	 *
	 * @throws NullPointerException if {@code normalizer} is {@code null}
	 */
	public GraphLinker normalizer(final Function<String, String> normalizer) {

		if ( normalizer == null ) {
			throw new NullPointerException("null normalizer");
		}

		this.normalizer=normalizer;

		return this;
	}

	/**
	 * Configures candidate identification.
	 *
	 * @param matcher a function converting a stream of anchors to a stream of candidate entity descriptions
	 *
	 * @return this linker
	 *
	 * @throws NullPointerException if {@code matcher} is {@code null}
	 */
	public GraphLinker matcher(final Function<Stream<String>, Stream<Match<String, Frame>>> matcher) {

		if ( matcher == null ) {
			throw new NullPointerException("null matcher");
		}

		this.matcher=matcher;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Match<Chunk, Frame>> apply(final Chunk chunk) {
		if ( chunk == null || chunk.tokens().isEmpty() ) { return Stream.empty(); } else {

			final List<Chunk> targets=chunk
					.map(finder)
					.collect(toList());

			final List<Token> tokens=chunk.tokens().stream()
					.map(token -> token.root(reader.andThen(normalizer).apply(token)))
					.collect(toList());

			return matcher // match targets to candidates

					.apply(targets.stream()
							.map(Chunk::text)// extract target anchors
							.map(normalizer)
							.distinct()
					)

					.map(match -> match.source(normalizer.apply(match.source()))) // normalize labels
					.distinct()

					.flatMap(match -> // look for complete anchors
							anchor(match.source(), tokens).map(match::source)
					)

					// make sure the complete anchor includes at least one of the original partial anchors
					// to prevent matching tokens like 'is' if the text includes 'Iceland'

					.filter(match -> targets.stream().anyMatch(target -> match.source().contains(target)))

					.sorted(Comparator // prefer longer anchors
							.<Match<Chunk, Frame>>comparingInt(match -> match.source().length())
							.reversed()
					)

					.filter(new Pruner((x, y) -> // remove partially overlapping anchors
							!x.source().matches(y.source()) && x.source().intersects(y.source())
					))

					.collect(collectingAndThen(toList(), matches -> weight(matches).stream())) // compute local rank

					.sorted(Comparator // prefer weightier anchors
							.<Match<Chunk, Frame>>comparingDouble(Match::weight)
							.reversed()
					)

					.filter(new Pruner((x, y) -> // retain only the weightiest match for each anchor
							x.source().intersects(y.source())
					))

					// retain only minimal identifying infos

					.map(match -> match.target(frame(match.target().focus())
							.values(RDF.TYPE, match.target().values(RDF.TYPE))
							.values(RDFS.LABEL, match.target().values(RDFS.LABEL))
							.values(RDFS.COMMENT, match.target().values(RDFS.COMMENT))
					));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Chunk> anchor(final CharSequence label, final List<Token> tokens) {
		return IntStream.range(0, tokens.size())

				.mapToObj(i -> {

					final StringBuilder anchor=new StringBuilder(label.length());

					int j=i;

					while ( j < tokens.size() && anchor.length() < label.length() ) {

						if ( j > i && !tokens.get(j-1).borders(tokens.get(j)) ) {
							anchor.append(' ');
						}

						anchor.append(tokens.get(j++).root()); // use the extracted/normalized text
					}

					return i < j && anchor.toString().contentEquals(label) ? tokens.subList(i, j) : null;

				})

				.filter(Objects::nonNull)

				.map(Chunk::new);
	}

	private Collection<Match<Chunk, Frame>> weight(final Collection<Match<Chunk, Frame>> matches) {

		final double min=matches.parallelStream().mapToDouble(Match::weight).min().orElse(0);
		final double max=matches.parallelStream().mapToDouble(Match::weight).max().orElse(0);

		final DoubleUnaryOperator weight=transform(min, max, 0.0, 0.1);

		final Map<Value, Set<Value>> resources=matches.stream() // resources to alternatives for the same anchor

				.collect(groupingBy(
						match -> match.source().text(),
						mapping(match -> match.target().focus(), toSet())
				))

				.values().stream()

				.flatMap(entities -> entities.stream().flatMap(x ->
						entities.stream().map(y ->
								new SimpleImmutableEntry<>(x, y)
						)
				))

				.collect(groupingBy(Map.Entry::getKey, mapping(SimpleImmutableEntry::getValue, toSet())));

		final Collection<Statement> connections=matches.stream()

				.map(Match::target)
				.flatMap(Frame::model)

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Pruner implements Predicate<Match<Chunk, Frame>> {

		private final BiPredicate<Match<Chunk, Frame>, Match<Chunk, Frame>> clash;

		private final Collection<Match<Chunk, Frame>> matches=new ArrayList<>();


		private Pruner(final BiPredicate<Match<Chunk, Frame>, Match<Chunk, Frame>> clash) {
			this.clash=clash;
		}


		@Override public boolean test(final Match<Chunk, Frame> x) {
			synchronized ( matches ) {
				return matches.stream().noneMatch(y -> clash.test(x, y)) && matches.add(x);
			}
		}

	}

}
