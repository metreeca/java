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

package com.metreeca.text.tokenizers;

import com.metreeca.text.Chunk;
import com.metreeca.text.Token;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;


public final class PatternTokenizer implements Function<Token, Chunk> {

	private static final Pattern BranchPattern=Pattern.compile(
			"\\(\\?<(?<type>[a-zA-Z][a-zA-Z0-9]*)>(?<pattern>.*)\\)"
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Pattern pattern=Pattern.compile("");

	private final Map<String, BiFunction<Token, Matcher, Token>> group2mapper=new HashMap<>();


	public PatternTokenizer defaults() {
		return urls()
				.emails()

				.codes()

				.numbers()
				.symbols()
				.words()

				.pattern("\\.{3}") // ellipses
				.pattern("\\S"); // other
	}


	public PatternTokenizer urls() {
		return pattern("(?<NNP>\\bhttps?://[-+&@#/%?=~_|!:,.;\\p{LD}]*[-+&@#/%=~_|\\p{LD}])");
	}

	public PatternTokenizer emails() {

		final String body="\\p{LD}[-+_\\p{LD}]"; // letter ord digit the letter, digit or separator
		final String head="\\b"+body+"*(?:\\."+body+"*)*"; // dot separated words
		final String tail="\\.\\p{L}{2,}"; // leading dot with at least two letters

		return pattern("(?<NNP>\\b"+head+"@"+head+tail+"\\b)");
	}

	public PatternTokenizer codes() { // codes, acronyms, …

		final String body="[-._&/\\p{LD}]"; // separators, letters and digits

		final String head="\\b(?:\\p{LD}"+body+"*)?"; // body with leading letter or digit at word boundary
		final String tail="(?:"+body+"*\\p{LD})?\\b"; // body with trailing letter or digit at word boundary

		final String nonLeadingUppercaseLetter=body+"\\p{Lu}";
		final String atLeastALetterAndADigit="\\p{L}"+body+"*\\p{Digit}";
		final String atLeastADigitAndALetter="\\p{Digit}"+body+"*\\p{L}";

		return pattern("(?<NNP>"+head+"(?:"
				+nonLeadingUppercaseLetter
				+"|"+atLeastALetterAndADigit
				+"|"+atLeastADigitAndALetter
				+")"+tail+")"
		);
	}

	public PatternTokenizer numbers() {
		return pattern("(?<CD>[-+]?(?:\\d*(:?[.,]\\d{3})*[.,])?\\d+\\b)");
	}

	public PatternTokenizer symbols() {
		return pattern("(?<SYM>[-+]?\\p{S})");
	}

	public PatternTokenizer words() {
		return pattern("\\b(?:\\p{L}-)?\\p{L}\\p{Ll}*\\b");
	}


	public PatternTokenizer pattern(final String pattern) {
		return pattern(pattern, (t, m) -> t);
	}

	public PatternTokenizer pattern(final String pattern, final Function<Token, Token> mapper) {
		return pattern(pattern, (t, m) -> mapper.apply(t));
	}

	public PatternTokenizer pattern(final String pattern, final BiFunction<Token, Matcher, Token> mapper) {

		if ( pattern == null ) {
			throw new NullPointerException("null pattern");
		}

		final StringBuilder builder=new StringBuilder(this.pattern.pattern());

		if ( builder.length() > 0 ) {
			builder.append('|');
		}

		final String group=format("p%d", group2mapper.size());

		builder.append("(?<").append(group).append('>');

		final Matcher matcher=BranchPattern.matcher(pattern);

		if ( matcher.matches() ) {

			final String type=matcher.group("type");

			group2mapper.put(group, (t, m) -> mapper.apply(t.type(type), m));

			builder.append(matcher.group("pattern"));

		} else {

			group2mapper.put(group, mapper);

			builder.append(pattern);

		}

		builder.append(')');

		this.pattern=Pattern.compile(builder.toString());

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Chunk apply(final Token token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		if ( pattern.pattern().isEmpty() ) { defaults(); }

		final Collection<Token> tokens=new ArrayList<>();

		for (final Matcher matcher=pattern.matcher(token.text()); matcher.find(); ) {
			tokens.add(mapper(matcher).apply(
					token.clip(matcher.start(), matcher.end())
			));
		}

		return new Chunk(tokens);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private UnaryOperator<Token> mapper(final Matcher matcher) {

		for (final Map.Entry<String, BiFunction<Token, Matcher, Token>> entry : group2mapper.entrySet()) {
			if ( matcher.group(entry.getKey()) != null ) {
				return token -> entry.getValue().apply(token, matcher);
			}
		}

		return token -> token;
	}

}
