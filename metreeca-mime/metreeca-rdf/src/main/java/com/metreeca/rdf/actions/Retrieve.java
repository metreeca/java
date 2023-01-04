/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.rdf.actions;

import com.metreeca.http.actions.*;
import com.metreeca.rdf.codecs.RDF;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.eclipse.rdf4j.rio.helpers.BasicParserSettings.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Linked dataset retrieval.
 *
 * <p>Maps linked datasets URLs to RDF descriptions retrieved by dereferencing them; unknown datasets are mapped to
 * empty descriptions.</p>
 */
public final class Retrieve implements Function<String, Model> {

	private static final Model EmptyModel=new LinkedHashModel().unmodifiable();


	private static String mimes() {

		final List<RDFFormat> formats=RDFParserRegistry.getInstance().getKeys().stream()

				.sorted(Comparator

						.comparing(RDFFormat::supportsRDFStar, Boolean::compareTo)
						.thenComparing(RDFFormat::supportsContexts, Boolean::compareTo)
						.thenComparing(RDFFormat::supportsNamespaces, Boolean::compareTo)
						.thenComparing(RDFFormat.TURTLE::equals, Boolean::compareTo)

						.reversed()
				)

				.collect(toList());

		final int size=formats.size();

		return IntStream.range(0, size).boxed().flatMap(i -> formats.get(i).getMIMETypes().stream().map(mime ->

				String.format("%s;q=%.2f", mime, (size-i.floatValue())/size))

		).collect(joining(", "));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Model apply(final String url) {
		return Optional.of(url)

				.flatMap(new Query(request -> request.headers("Accept", mimes())))

				.flatMap(new Fetch())

				.flatMap(new Parse<>(new RDF(codec -> codec
						.set(VERIFY_URI_SYNTAX, false)
						.set(FAIL_ON_UNKNOWN_DATATYPES, false)
						.set(VERIFY_DATATYPE_VALUES, false)
						.set(NORMALIZE_DATATYPE_VALUES, false)
				)))

				.orElseGet(() -> EmptyModel);
	}

}
