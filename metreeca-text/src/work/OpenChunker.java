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

package com.metreeca.text.open;

import com.metreeca.text.*;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedMap;


public final class OpenChunker implements Function<Chunk, Stream<Chunk>> {

	private static final Map<String, ChunkerModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final ChunkerModel model;


	public OpenChunker(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model URL");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new ChunkerModel(input);
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

		final String[] tags=new ChunkerME(model).chunk(
				tokens.stream().map(Token::text).toArray(String[]::new),
				tokens.stream().map(Token::type).toArray(String[]::new)
		);

		final Collection<Chunk> chunks=new ArrayList<>();

		for (int s=0, e=1; e <= tags.length; ++e) {
			if ( e == tags.length || !tags[e].startsWith("I-") ) {

				chunks.add(new Chunk(tokens.subList(s, e))
						.type(tags[s].startsWith("B-") ? tags[s].substring(2) : tags[s])
				);

				s=e;

			}
		}

		return chunks.stream();
	}

}
