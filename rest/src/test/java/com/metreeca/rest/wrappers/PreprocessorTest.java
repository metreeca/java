/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.form.truths.ModelAssert;
import com.metreeca.rest.Request;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


final class PreprocessorTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	private BiFunction<Request, Model, Model> pre(final Value value) {
		return (request, model) -> {

			model.add(statement(request.item(), RDF.VALUE, value));

			return model;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessRequestRDFPayload() {
		exec(() -> new Preprocessor(pre(RDF.FIRST), pre(RDF.REST)) // multiple filters to test piping

				.wrap(echo())

				.handle(new Request().body(rdf(), emptyList())) // empty body to activate pre-processing

				.accept(response -> assertThat(response)
						.hasBody(rdf(), rfd -> ModelAssert.assertThat(rfd)
								.as("items retrieved")
								.hasSubset(asList(
										statement(response.item(), RDF.VALUE, RDF.FIRST),
										statement(response.item(), RDF.VALUE, RDF.REST)
								))
						)
				)
		);
	}

	@Test void testIgnoreMissingRequestRDFPayload() {
		exec(() -> new Preprocessor(pre(RDF.FIRST))

				.wrap(echo())

				.handle(new Request()) // no RDF payload

				.accept(response -> assertThat(response).hasBody(rdf())));
	}

}
