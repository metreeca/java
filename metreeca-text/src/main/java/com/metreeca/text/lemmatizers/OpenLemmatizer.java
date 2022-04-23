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

package com.metreeca.text.lemmatizers;

import com.metreeca.text.Chunk;
import com.metreeca.text.Token;

import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;


public final class OpenLemmatizer implements UnaryOperator<Chunk> {

	/*
	 * see https://stackoverflow.com/a/57911951/739773 for training
	 */
	private static final Map<String, LemmatizerModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final LemmatizerModel model;


	public OpenLemmatizer(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new LemmatizerModel(input);
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

		final String[] lemmas=new LemmatizerME(model).lemmatize(
				tokens.stream().map(Token::text).toArray(String[]::new),
				tokens.stream().map(Token::type).toArray(String[]::new)
		);

		return new Chunk(IntStream.range(0, tokens.size())
				.mapToObj(i -> tokens.get(i).map(token -> token.root().isEmpty() ? token.root(lemmas[i]) : token))
				.collect(toList())
		);

	}

}
