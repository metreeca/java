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

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;

final class PatternTokenizerTest {

	private List<Token> tokens(final String text) {
		return Stream
				.of(new Token(text))
				.map(new PatternTokenizer())
				.map(Chunk::tokens)
				.flatMap(Collection::stream)
				.collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testTokenizeWords() {
		assertThat(tokens("Say: a this-and-that"))
				.extracting(Token::text)
				.containsExactly("Say", ":", "a", "this", "-", "and", "-", "that");
	}

	@Test void testTokenizeComposites() {
		assertThat(tokens("U-turn e-sports"))
				.extracting(Token::toString)
				.containsExactly("(* U-turn)", "(* e-sports)");
	}

	@Test void testTokenizeDiacritics() {
		assertThat(tokens("Škoda"))
				.extracting(Token::text)
				.containsExactly("Škoda");
	}

	@Test void testTokenizeNumbers() {
		assertThat(tokens("1 1.2 .1 +1,234 -1,234.56"))
				.extracting(Token::text)
				.containsExactly("1", "1.2", ".1", "+1,234", "-1,234.56");
	}

	@Test void testTokenizeSymbols() {
		assertThat(tokens("€"))
				.extracting(Token::toString)
				.containsExactly("(SYM €)");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testTokenizeURLs() {
		assertThat(tokens("from http://example.com/x/y, c@pcs"))
				.extracting(Token::toString)
				.contains("(NNP http://example.com/x/y)");
	}

	@Test void testTokenizeEmails() {
		assertThat(tokens("10 c@pcs from x.y@example.com"))
				.extracting(Token::toString)
				.contains("(NNP x.y@example.com)")
				.doesNotContain("(NNP c@pcs)");
	}

	@Test void testTokenizeENames() {
		assertThat(tokens("e-Crafter e-troFit E.ON itMoves iPod"))
				.extracting(Token::toString)
				.containsExactly("(NNP e-Crafter)", "(NNP e-troFit)", "(NNP E.ON)", "(NNP itMoves)", "(NNP iPod)");
	}

	@Test void testTokenizeAcronyms() {
		assertThat(tokens("R&D EV"))
				.extracting(Token::toString)
				.containsExactly("(NNP R&D)", "(NNP EV)");
	}

	@Test void testTokenizeCodes() {
		assertThat(tokens("L7E-CU U6ion Cov19 Cov-19B 9x ABC/12"))
				.extracting(Token::toString)
				.containsExactly(
						"(NNP L7E-CU)", "(NNP U6ion)",
						"(NNP Cov19)", "(NNP Cov-19B)",
						"(NNP 9x)", "(NNP ABC/12)"
				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testMapper() {
		assertThat(new PatternTokenizer()
				.pattern("\\w+", range -> range.type("CUSTOM"))
				.apply(new Token("test"))
				.toString()
		).isEqualTo("(* (CUSTOM test))");

	}
}