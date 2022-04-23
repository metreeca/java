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

package com.metreeca.text.taggers;

import com.metreeca.text.Chunk;
import com.metreeca.text.Token;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.toList;

public final class CorpusTagger implements UnaryOperator<Chunk> {

	private final Map<String, Predicate<Token>> types=new LinkedHashMap<>();


	public CorpusTagger type(final String type, final Predicate<Token> corpus) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		if ( type.isEmpty() ) {
			throw new IllegalArgumentException("empty type");
		}

		if ( corpus == null ) {
			throw new NullPointerException("null corpus");
		}

		types.put(type, corpus);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Chunk apply(final Chunk chunk) {

		if ( chunk == null ) {
			throw new NullPointerException("null chunk");
		}

		return chunk.tokens(chunk.tokens().stream()

				.map(token -> types.entrySet().stream()

						.map(entry -> token.type().isEmpty() && entry.getValue().test(token)
								? Optional.of(token.type(entry.getKey()))
								: Optional.<Token>empty()
						)

						.filter(Optional::isPresent)
						.map(Optional::get)

						.findFirst()
						.orElse(token)

				)

				.collect(toList())
		);
	}

}
