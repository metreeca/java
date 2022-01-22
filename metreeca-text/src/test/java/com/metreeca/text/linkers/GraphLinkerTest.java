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

package com.metreeca.text.linkers;

import com.metreeca.rest.Toolbox;
import com.metreeca.rest.Xtream;
import com.metreeca.text.*;
import com.metreeca.text.finders.ModelFinder;
import com.metreeca.text.finders.NGramFinder;
import com.metreeca.text.lemmatizers.OpenLemmatizer;
import com.metreeca.text.matchers.WikidataMatcher;
import com.metreeca.text.taggers.OpenTagger;
import com.metreeca.text.tokenizers.PatternTokenizer;

import java.util.function.Predicate;

import static com.metreeca.rest.Toolbox.resource;
import static com.metreeca.text.Model.test;

final class GraphLinkerTest {

	private static final Predicate<Token> NN=token -> token.is("NN", "NNS");
	private static final Predicate<Token> NNP=token -> token.is("NNP", "NNPS");
	private static final Predicate<Token> UJJ=((Predicate<Token>)token -> token.is("JJ")).and(Token::isUpper);
	private static final Predicate<Token> CD=token -> token.is("CD");

	private static final Predicate<Token> STOP=
			corpus(resource(Range.class, "models/corpus.stops.en.txt"))
					.or(corpus(resource(Range.class, "models/corpus.units.txt")));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public static void main(final String... args) {
		new Toolbox().exec(() -> Xtream

				.of("Kim is a novel by Nobel Prize-winning English author Kipling."
						+" It was first published in book form by Macmillan & Co. Ltd in October 1901."
				)

				//.of("Jack Ma founded Alibaba in Hangzhou with investments from SoftBank and Goldman.")
				//.of("Lincoln, Nebraska's StarTran Bus Service has ordered 10 New Flyer electric buses.")

				//.of("The new law strengthens the authorities' powers in detaining suspects and imposing public order "
				//		+"in Ankara.\n"
				//		+"Turkey 's parliament has ratified a tough anti-terrorism bill proposed by the ruling party, "
				//		+"six days after the two-year-long state of emergency ended.\n"
				//		+"The new anti-terror law, which was passed on Wednesday, strengthens the authorities' powers "
				//		+"in detaining suspects and imposing public order.\n"
				//		+"\n"
				//		+"The measure drafted by President Recep Tayyip Erdogan's Justice and Development Party (AKP) "
				//		+"retains aspects of emergency rule and will be valid for three years.\n"
				//		+"It also authorizes the government to dismiss personnel of Turkish Armed Forces, police and "
				//		+"gendarmerie departments, public servants and workers if they are found linked to a terror "
				//		+"organization.")

				.map(Token::new)

				.map(new PatternTokenizer())
				.map(new OpenTagger(resource(Range.class, "models/tagger.en.bin")))
				.map(new OpenLemmatizer(resource(Range.class, "models/lemmatizer.en.bin")))

				.flatMap(new GraphLinker()

						.reader(token -> token.text(NNP.or(UJJ).test(token)))

						.finder(new ModelFinder("*", Model

										.alt(
												test(UJJ).opt().and(test(NNP)).and(test(NNP.or(CD)).star()),
												test(NN).plus(),
												test(UJJ)
										)

										.some(STOP.negate())

								)
										.andThen(chunks -> chunks.flatMap(new NGramFinder()))

						)

						.matcher(new WikidataMatcher())

				)

				.forEach(System.out::println)

		);
	}

}