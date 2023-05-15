

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

package com.metreeca.http.rdf.actions;

import com.metreeca.http.rdf.schemas.Schema;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.metreeca.http.rdf.Values.literal;
import static com.metreeca.http.rdf.Values.statement;

/**
 * RDF default text localization.
 *
 * <p>Assigns a default language tag to untagged string object values associated to a specific set of predicates
 * identifying human-readable textual values.</p>
 */
public final class Localize implements Function<Statement, Statement> {

	private final String lang;

	private Set<IRI> predicates=Set.of(
			RDFS.LABEL, RDFS.COMMENT,
			SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL,
			Schema.name, Schema.description, Schema.disambiguatingDescription
	);


	/**
	 * Creates an RDF default localization action.
	 *
	 * @param lang the default language tag to be assigned to untagged string object values
	 *
	 * @throws NullPointerException     if {@code lang} is null
	 * @throws IllegalArgumentException if {@code lang} is not a legal language tag
	 */
	public Localize(final String lang) { // !!! auto-tagging

		if ( lang == null ) {
			throw new NullPointerException("null lang");
		}

		if ( lang.isEmpty() ) { // !!! extend
			throw new IllegalArgumentException("malformed lang tag");
		}

		this.lang=lang;
	}


	/**
	 * Configures the target predicate set.
	 *
	 * @param predicates the set of predicates  identifying human-readable textual values; defaults to well-known
	 *                   human-readable predicates in the {@link RDFS rdfs}, {@link SKOS skos} and {@link Schema schema}
	 *                   namespaces
	 *
	 * @throws NullPointerException if {@code predicates} is null or contains null elements
	 */
	public void predicates(final Collection<IRI> predicates) {

		if ( predicates == null || predicates.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null predicates");
		}

		this.predicates=Set.copyOf(predicates);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Statement apply(final Statement statement) {
		return literal(statement.getObject())

				.filter(object -> predicates.contains(statement.getPredicate()))
				.filter(object -> object.getDatatype().equals(XSD.STRING))

				.map(Value::stringValue)
				.map(text -> literal(text, lang))

				.map(text -> statement(statement.getSubject(), statement.getPredicate(), text))

				.orElse(statement);
	}

}
