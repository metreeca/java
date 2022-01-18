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

package com.metreeca.text.finders;

import com.metreeca.text.Chunk;
import com.metreeca.text.Token;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Math.min;

public final class NGramFinder implements Function<Chunk, Stream<Chunk>> {

	private final int min;
	private final int max;


	public NGramFinder() {
		this(1);
	}

	public NGramFinder(final int min) {
		this(min, Integer.MAX_VALUE);
	}

	public NGramFinder(final int min, final int max) {

		if ( min < 0 ) {
			throw new IllegalArgumentException("minimum size less than 0");
		}

		if ( max <= 0 ) {
			throw new IllegalArgumentException("maximum minimum size less than or equal to 0");
		}

		this.min=min;
		this.max=max;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Chunk> apply(final Chunk chunk) {

		if ( chunk == null ) {
			throw new NullPointerException("null chunk");
		}

		final List<Token> tokens=chunk.tokens();
		final Collection<Chunk> ngrams=new ArrayList<>();

		//if ( head(tokens.get(0)) && tail(tokens.get(tokens.size()-1)) ) {
		//	ngrams.add(chunk);
		//}

		for (int lower=0, size=tokens.size(); lower < size; ++lower) {
			for (int upper=lower+min, limit=min(lower+max, size); upper <= limit; ++upper) {
				if ( head(tokens.get(lower)) && tail(tokens.get(upper-1)) ) {
					ngrams.add(new Chunk(tokens.subList(lower, upper)));
				}
			}
		}

		return ngrams.stream();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean head(final Token token) {
		return token.isUpper() || token.is("NN", "NNS", "NNP", "NNPS");
	}

	private boolean tail(final Token token) {
		return token.isUpper() || token.is("NN", "NNS", "NNP", "NNPS", "CD");
	}

}
