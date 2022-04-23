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

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;


public final class OpenTokenizer implements Function<Token, Chunk> {

	private static final Map<String, TokenizerModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final TokenizerModel model;


	public OpenTokenizer(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model URL");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new TokenizerModel(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Chunk apply(final Token token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		return new Chunk(Stream

				.of(new TokenizerME(model).tokenizePos(token.text()))

				.map(span -> token.clip(span.getStart(), span.getEnd()))

				.collect(toList())
		);
	}

}
