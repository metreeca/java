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

package com.metreeca.text.models;

import java.io.File;

public final class Trainer {

	private static final File samples=new File("finder.person.it.txt");
	private static final File model=new File("finder.person.it.bin");


	//@Test void fetch() throws IOException {
	//	try ( final Writer writer=new FileWriter(samples) ) {
	//		new Toolbox()
	//
	//				.exec(() -> Stream
	//
	//						.of("select ?name {\n"
	//								+"\n"
	//								+"\t[] wdt:P31 wd:Q5;\n"
	//								+"\t\twdt:P1559 ?name;\n"
	//								+"\t\twdt:P569 ?birth.\n"
	//								+"\n"
	//								+"\tfilter(lang(?name)  = 'it')\n"
	//								+"\tfilter(year(?birth) >= 1700)\n"
	//								+"\n"
	//								+"}"
	//						)
	//
	//						.flatMap(new TupleQuery().graph(Wikidata.Graph()))
	//
	//						.map(bindings -> bindings.getValue("name"))
	//
	//						.map(value -> string(value).orElse(""))
	//
	//						.filter(name -> name.contains(" ")) // exclude one-word pseudonyms
	//						.filter(name -> name.length() <= 25) // exclude composite names
	//
	//						.map(new Clean().space(true).marks(true))
	//
	//						.sorted()
	//						.distinct()
	//
	//						.peek(System.out::println)
	//
	//						.forEach(name -> {
	//							try {
	//
	//								writer.write(name);
	//								writer.write('\n');
	//
	//							} catch ( final IOException e ) {
	//								throw new UncheckedIOException(e);
	//							}
	//						})
	//
	//				)
	//
	//				.clear();
	//	}
	//}

	//@Test void train() throws IOException {
	//
	//	final InputStreamFactory inputs=new MarkableFileInputStreamFactory(samples);
	//
	//	final ObjectStream<NameSample> stream=new NameSampleDataStream(
	//			new PlainTextByLineStream(inputs, StandardCharsets.UTF_8));
	//
	//	final TrainingParameters params=new TrainingParameters();
	//
	//	params.put(TrainingParameters.ITERATIONS_PARAM, 70);
	//	params.put(TrainingParameters.CUTOFF_PARAM, 1);
	//
	//	final TokenNameFinderModel nameFinderModel=NameFinderME.train("it", null, stream,
	//			params, TokenNameFinderFactory.create(null, null, Collections.emptyMap(), new BioCodec()));
	//
	//
	//	nameFinderModel.serialize(new FileOutputStream(model));
	//
	//}

}
