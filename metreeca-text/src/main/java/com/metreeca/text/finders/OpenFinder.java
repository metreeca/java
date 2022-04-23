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

package com.metreeca.text.finders;

import com.metreeca.text.Chunk;
import com.metreeca.text.Token;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedMap;


public final class OpenFinder implements Function<Chunk, Stream<Chunk>> {

	private static final Map<String, TokenNameFinderModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final TokenNameFinderModel model;


	public OpenFinder(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model URL");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new TokenNameFinderModel(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Stream<Chunk> apply(final Chunk chunk) {

		if ( chunk == null ) {
			throw new NullPointerException("null chunk");
		}

		final List<Token> tokens=chunk.tokens();

		return Arrays

				.stream(new NameFinderME(model)
						.find(tokens.stream().map(Token::text).toArray(String[]::new)) // !!! verbatim?
				)

				.map(span -> new Chunk(tokens.subList(span.getStart(), span.getEnd()))
						.type(span.getType().toUpperCase(Locale.ROOT))
						.weight(span.getProb())
				);

	}

}
