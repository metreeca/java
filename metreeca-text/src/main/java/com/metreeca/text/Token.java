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


import com.metreeca.text.actions.Normalize;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("FieldNotUsedInToString")
public final class Token implements Range {

	private static final Map<String, Predicate<Token>> corpora=synchronizedMap(new HashMap<>());


	public static Predicate<Token> corpus(final URL corpus) {

		if ( corpus == null ) {
			throw new NullPointerException("null corpus");
		}

		return corpora.computeIfAbsent(corpus.toExternalForm(), url -> {
			try ( final BufferedReader reader=new BufferedReader(new InputStreamReader(corpus.openStream(), UTF_8)) ) {

				return corpus(reader.lines()

						.filter(line -> !line.isEmpty())
						.filter(line -> !line.startsWith("#"))

						.collect(toSet())
				);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public static Predicate<Token> corpus(final String... entries) {

		if ( entries == null || Arrays.stream(entries).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null entries");
		}

		return corpus(asList(entries));
	}

	public static Predicate<Token> corpus(final Collection<String> entries) {

		if ( entries == null || entries.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null entries");
		}

		final Normalize normalize=new Normalize().space(true).lower(true);
		final Collection<String> set=entries.stream().map(normalize).collect(toSet());

		return token -> set.contains(normalize.apply(token.text(false)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;

	private final String root;
	private final String type;

	private final double weight;

	private final int lower;
	private final int upper;


	public Token(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text;
		this.root="";

		this.lower=0;
		this.upper=text.length();

		this.type="";

		this.weight=0;
	}


	private Token(
			final String text, final int lower, final int upper,
			final String root, final String type, final double weight
	) {

		this.text=text;
		this.root=root;

		this.lower=lower;
		this.upper=upper;

		this.type=type;
		this.weight=weight;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public int lower() {
		return lower;
	}

	@Override public int upper() {
		return upper;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean is(final String... types) {

		if ( types == null ) {
			throw new NullPointerException("null types");
		}

		return !type.isEmpty() && (types.length == 0 || asList(types).contains(type));
	}


	public boolean isLower() {
		return !text.isEmpty() && Character.isLowerCase(text.charAt(0));
	}

	public boolean isUpper() {
		return !text.isEmpty() && Character.isUpperCase(text.charAt(0));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String text() {
		return text;
	}

	public String text(final Predicate<Token> verbatim) {
		return text(verbatim.test(this));
	}

	public String text(final boolean verbatim) {
		return verbatim || root.isEmpty() ? text : root;
	}

	public Token text(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return new Token(text, lower, upper, root, type, weight);
	}


	public String root() {
		return root;
	}

	public Token root(final String root) {

		if ( type == null ) {
			throw new NullPointerException("null root");
		}

		return new Token(text, lower, upper, root, type, weight);
	}


	public String type() {
		return type;
	}

	public Token type(final String type) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		return new Token(text, lower, upper, root, type, weight);
	}


	public double weight() {
		return weight;
	}

	public Token weight(final double weight) {
		return new Token(text, lower, upper, root, type, weight);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <R> R map(final Function<Token, R> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	public Token clip(final int lower, final int upper) {

		if ( lower < 0 || lower > upper || upper > this.upper-this.lower ) {
			throw new IllegalArgumentException("illegal bounds ["+lower+","+upper+")");
		}

		return new Token(
				text.substring(lower, upper), this.lower+lower, this.lower+upper,
				"", "", 0
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		return String.format("(%s %s)", type.isEmpty() ? "*" : type, text);
	}

}
