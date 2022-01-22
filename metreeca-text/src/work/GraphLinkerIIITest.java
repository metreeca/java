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

package com.metreeca.text.endo.mkIII;

import com.metreeca.text.Model;
import com.metreeca.text.Range;
import com.metreeca.text.endo.*;
import com.metreeca.text.endo.mkIV.WikidataMatcher;
import com.metreeca.text.open.OpenLemmatizer;
import com.metreeca.text.open.OpenTagger;
import com.metreeca.rest.Context;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Clean;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.metreeca.text.Model.test;
import static com.metreeca.text.Range.corpus;
import static com.metreeca.text.Range.type;
import static com.metreeca.rest.Codecs.resource;

final class GraphLinkerIIITest {

	private static final Predicate<Range> NN=type("NN", "NNS");
	private static final Predicate<Range> NNP=type("NNP", "NNPS");
	private static final Predicate<Range> UJJ=type("JJ").and(Range::isUpper);
	private static final Predicate<Range> CD=type("CD");

	private static final Predicate<Range> STOP=
			corpus(resource(Range.class, "open/models/corpus.stops.en.txt"))
					.or(corpus(resource(Range.class, "open/models/corpus.units.txt")));


	private static <V, R> Function<V, R> when(
			final Predicate<V> test, final Function<V, R> pass, final Function<V, R> fail
	) {
		return v -> test.test(v) ? pass.apply(v) : fail.apply(v);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void work() {
		new Context().exec(() -> Xtream

				.of("Kim is a novel by Nobel Prize-winning English author Kipling."
						+" It was first published in book form by Macmillan & Co. Ltd in October 1901."
				)

				//.of("Jack Ma founded Alibaba in Hangzhou with investments from SoftBank and Goldman.")

				//.of("The new law strengthens the authorities' powers in detaining suspects and imposing public order "
				//		+ "in Ankara.\n"
				//		+"Turkey 's parliament has ratified a tough anti-terrorism bill proposed by the ruling party, "
				//		+ "six days after the two-year-long state of emergency ended.\n"
				//		+"The new anti-terror law, which was passed on Wednesday, strengthens the authorities' powers "
				//		+ "in detaining suspects and imposing public order.\n"
				//		+"\n"
				//		+"The measure drafted by President Recep Tayyip Erdogan's Justice and Development Party (AKP) "
				//		+ "retains aspects of emergency rule and will be valid for three years.\n"
				//		+"It also authorizes the government to dismiss personnel of Turkish Armed Forces, police and "
				//		+ "gendarmerie departments, public servants and workers if they are found linked to a terror "
				//		+ "organization.")

				.map(new Clean().smart(true))
				.map(Range::new)

				.map(new PatternTokenizer())
				.map(new OpenTagger(resource(Range.class, "open/models/tagger.en.bin")))
				.map(new OpenLemmatizer(resource(Range.class, "open/models/lemmatizer.en.bin")))

				.flatMap(new GraphLinkerIII()

						.extractor(when(NNP.or(UJJ), Range::text, Range::root))
						.normalizer(new Clean().smart(true).marks(true))

						.scanner(new ModelFinder("*", Model

								.alt(
										test(UJJ).opt().and(test(NNP)).and(test(NNP.or(CD)).star()),
										test(NN).plus(),
										test(UJJ)
								)

								.some(STOP.negate())

						).andThen(ranges -> ranges.flatMap(new NGramScanner())))

						.matcher(new WikidataMatcher())

				)

				.forEach(System.out::println)

		);
	}

}