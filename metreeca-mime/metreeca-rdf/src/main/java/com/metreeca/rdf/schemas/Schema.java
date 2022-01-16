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

package com.metreeca.rdf.schemas;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.json.Values.iri;

/**
 * Schema.org RDF vocabulary.
 *
 * @see <a href="https://schema.org/">Schema.org</a>
 */
public final class Schema {

	public static final String Namespace="https://schema.org/";
	public static final String NamespaceLegacy="http://schema.org/";

	/**
	 * Creates a term in the schema.org namespace.
	 *
	 * @param id the identifer of the term to be created
	 *
	 * @return the schema.org term identified by {@code id}
	 *
	 * @throws NullPointerException if {@code id} is null
	 */
	public static IRI term(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return iri(Namespace, id);
	}


	public static final IRI name=term("name");
	public static final IRI description=term("description");
	public static final IRI disambiguatingDescription=term("disambiguatingDescription");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Schema() { }

}
