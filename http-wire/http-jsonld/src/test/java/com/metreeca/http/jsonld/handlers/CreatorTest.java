/*
 * Copyright © 2013-2024 Metreeca srl
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

package com.metreeca.http.jsonld.handlers;

import com.metreeca.http.Request;
import com.metreeca.http.jsonld.formats.JSONLD;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.http.Response.Conflict;
import static com.metreeca.http.Response.Created;
import static com.metreeca.http.ResponseAssert.assertThat;
import static com.metreeca.http.jsonld.handlers.OperatorTest.exec;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Shape.property;
import static com.metreeca.link.Shape.shape;

import static org.assertj.core.api.Assertions.assertThat;

final class CreatorTest {

    @Test void testCreateResource() {

        final IRI id=iri("test:/path");

        exec(

                (iri, frame) -> {

                    assertThat(iri).as("assign unique id").isNotIn(id);
                    assertThat(frame.value(RDF.VALUE)).as("rewritten body").contains(iri);

                    return true;

                },

                () -> new Creator()

                        .handle(new Request()
                                        .attribute(Shape.class, property(RDF.VALUE, shape()))
                                        .base("test:/")
                                        .path("/path")
                                        .body(new JSONLD(), frame(
                                                field(RDF.VALUE, id)
                                        )),
                                Request::reply
                        )

                        .map(response -> assertThat(response)
                                .hasStatus(Created)
                                .doesNotHaveAttribute(Shape.class)
                                .doesNotHaveBody()
                        )

        );
    }

    @Test void testReportClash() {
        exec((iri, frame) -> false, () -> new Creator()

                .handle(new Request()
                                .body(new JSONLD(), frame()),
                        Request::reply
                )

                .map(response -> assertThat(response)
                        .hasStatus(Conflict)
                        .doesNotHaveBody()
                )

        );
    }

}