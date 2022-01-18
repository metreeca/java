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

package com.metreeca.text;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("FieldNotUsedInToString")
public final class Chunk implements Range {

	private final List<Token> tokens;

	private final String type;

	private final double weight;


	public Chunk(final Token... tokens) {
		this(asList(tokens));
	}

	public Chunk(final Collection<Token> tokens) {

		if ( tokens == null || tokens.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tokens");
		}

		this.tokens=tokens.stream()
				.sorted(Comparator.comparing(Token::lower))
				.collect(toList());

		for (int i=0, n=this.tokens.size(); i < n; i++) {
			if ( i > 0 && this.tokens.get(i).intersects(this.tokens.get(i-1)) ) {
				throw new IllegalArgumentException("overlapping tokens");
			}
		}

		this.type="";
		this.weight=0;
	}


	private Chunk(final List<Token> tokens, final String type, final double weight) {

		this.tokens=tokens;

		this.type=type;
		this.weight=weight;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public int lower() {
		return tokens.isEmpty() ? 0 : tokens.get(0).lower();
	}

	@Override public int upper() {
		return tokens.isEmpty() ? 0 : tokens.get((tokens.size()-1)).upper();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<Token> tokens() {
		return unmodifiableList(tokens);
	}

	public Chunk tokens(final Collection<Token> tokens) {

		if ( tokens == null || tokens.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tokens");
		}

		return new Chunk(new ArrayList<>(tokens), type, weight);
	}


	public String text() {
		return text(true);
	}

	public String text(final Predicate<Chunk> verbatim) {
		return text(verbatim.test(this));
	}

	public String text(final boolean verbatim) {

		final StringBuilder builder=new StringBuilder(5*tokens.size());

		for (int i=0, n=tokens.size(); i < n; i++) {

			if ( i > 0 && tokens.get(i-1).upper() < tokens.get(i).lower() ) {
				builder.append(' ');
			}

			builder.append(tokens.get(i).text(verbatim));

		}

		return builder.toString();
	}


	public String type() {
		return type;
	}

	public Chunk type(final String type) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		return new Chunk(tokens, type, weight);
	}


	public double weight() {
		return weight;
	}

	public Chunk weight(final double weight) {
		return new Chunk(tokens, type, weight);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <R> R map(final Function<Chunk, R> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		return String.format("(%s %s)",
				type.isEmpty() ? "*" : type,
				tokens.stream().map(Token::toString).collect(joining(" "))
		);
	}

}
