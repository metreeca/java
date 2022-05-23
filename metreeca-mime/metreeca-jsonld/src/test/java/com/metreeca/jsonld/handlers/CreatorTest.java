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

package com.metreeca.jsonld.handlers;

import com.metreeca.http.Request;
import com.metreeca.jsonld.codecs.JSONLD;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.http.Response.Created;
import static com.metreeca.http.ResponseAssert.assertThat;
import static com.metreeca.jsonld.handlers.OperatorTest.exec;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Values.item;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Or.or;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

final class CreatorTest {

	private final IRI focus=item("/");
	private final Shape shape=field(RDF.VALUE);


	@Test void testCreateResource() {
		exec(

				frame -> {

					assertThat(frame.focus()).as("generated unique iri").isNotEqualTo(focus);
					assertThat(frame.values(RDF.VALUE)).as("rewritten body").containsExactly(frame.focus());

					return true;

				},

				() -> {
					new Creator()

							.handle(JSONLD.shape(new Request(), shape)
											.body(new JSONLD(), frame(focus)
													.value(RDF.VALUE, focus)
											),
									Request::reply
							)

							.map(response -> {
										return assertThat(response)
												.hasStatus(Created)
												.hasAttribute(Shape.class, shape -> assertThat(shape).isEqualTo(or()))
												.doesNotHaveBody(new JSONLD());
									}
							);
				}

		);
	}

	@Test void testReportClash() {
		assertThatIllegalStateException().isThrownBy(() -> exec(frame -> false, () -> {
					new Creator()

							.handle(JSONLD.shape(new Request()
													, shape)
											.body(new JSONLD(), frame(item("/"))),
									Request::reply
							);
				}

		));
	}

}