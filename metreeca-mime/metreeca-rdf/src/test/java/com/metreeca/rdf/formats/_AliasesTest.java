/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.formats;

import com.metreeca.rdf.ValuesTest;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.formats._Aliases.aliases;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

final class _AliasesTest {

	@Test void testGuessAliasFromIRI() {

		assertThat(aliases(field(RDF.VALUE)))
				.as("direct")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		assertThat(aliases(field(inverse(RDF.VALUE))))
				.as("inverse")
				.isEqualTo(singletonMap(inverse(RDF.VALUE), "valueOf")); // !!! valueOf?

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(aliases(field(RDF.VALUE, alias("alias"))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), field(RDF.VALUE))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(aliases(and(field(RDF.VALUE, alias("alias")))))
				.as("group")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

		assertThat(aliases(field(RDF.VALUE, and(alias("alias")))))
				.as("conjunction")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

	}

	@Test void testMergeDuplicateFields() {

		// nesting required to prevent and() from collapsing duplicates

		assertThat(aliases(and(field(RDF.VALUE), and(field(RDF.VALUE)))))
				.as("system-guessed")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		// nesting required to prevent and() from collapsing duplicates

		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), and(field(RDF.VALUE, alias("alias"))))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

	}


	@Test void testHandleMultipleAliases() {

		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("two")))))
				.as("clashing")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("one")))))
				.as("repeated")
				.isEqualTo(singletonMap(RDF.VALUE, "one"));

	}

	@Test void testMergeAliases() {
		assertThat(aliases(and(field(RDF.TYPE), field(RDF.VALUE))))
				.as("merged")
				.isEqualTo(Stream.of(

						new AbstractMap.SimpleImmutableEntry<>(RDF.TYPE, "type"),
						new AbstractMap.SimpleImmutableEntry<>(RDF.VALUE, "value")

				).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	@Test void testIgnoreClashingAliases() {

		assertThat(aliases(and(field(RDF.VALUE), field(iri("urn:example:value")))))
				.as("different fields")
				.isEmpty();

		// fall back to system-guess alias

		assertThat(aliases(and(field(RDF.VALUE, alias("one")), field(RDF.VALUE, alias("two")))))
				.as("same field")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

	}

	@Test void testIgnoreReservedAliases() {

		assertThat(aliases(field(iri(ValuesTest.Base, "@id"))))
				.as("ignore reserved system-guessed aliases")
				.isEmpty();

		assertThat(aliases(field(RDF.VALUE, alias("@id"))))
				.as("ignore reserved user-defined aliases")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

	}

}
