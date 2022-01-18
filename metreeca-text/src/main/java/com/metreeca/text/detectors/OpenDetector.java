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

package com.metreeca.text.detectors;

import com.metreeca.text.Token;

import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;

import java.io.*;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.Collections.synchronizedMap;


public final class OpenDetector implements Function<Token, Optional<Entry<String, Double>>> {

	private static final Map<String, LanguageDetectorModel> models=synchronizedMap(new HashMap<>());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final LanguageDetectorModel model;


	public OpenDetector(final URL model) {

		if ( model == null ) {
			throw new NullPointerException("null model URL");
		}

		this.model=models.computeIfAbsent(model.toExternalForm(), url -> {
			try ( final InputStream input=model.openStream() ) {
				return new LanguageDetectorModel(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Entry<String, Double>> apply(final Token token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		return Optional.ofNullable(new LanguageDetectorME(model).predictLanguage(token.text()))
				.map(language -> new SimpleImmutableEntry<>(language.getLang(), language.getConfidence()));
	}

}
