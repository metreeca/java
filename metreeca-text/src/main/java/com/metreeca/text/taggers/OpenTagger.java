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

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;


public final class OpenTagger implements UnaryOperator<Chunk> {

	private static final Map<String, POSModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final POSModel model;


	public OpenTagger(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model URL");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new POSModel(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Chunk apply(final Chunk chunk) {

		if ( chunk == null ) {
			throw new NullPointerException("null chunk");
		}

		final List<Token> tokens=chunk.tokens();

		final String[] tags=new POSTaggerME(model).tag(tokens.stream()
				.map(Token::text)
				.toArray(String[]::new)
		);

		return chunk.tokens(IntStream.range(0, tags.length)
				.mapToObj(i -> tokens.get(i).map(token -> token.type().isEmpty() ? token.type(tags[i]) : token))
				.collect(toList())
		);

	}

}
