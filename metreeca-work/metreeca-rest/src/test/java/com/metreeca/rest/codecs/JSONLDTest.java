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

package com.metreeca.rest.codecs;

import com.metreeca.http.*;
import com.metreeca.json.JSONAssert;
import com.metreeca.json.codecs.JSON;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.json.Json;

import static com.metreeca.core.Lambdas.task;
import static com.metreeca.http.Response.*;
import static com.metreeca.http.ResponseAssert.assertThat;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Shape.required;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shapes.And.and;
import static com.metreeca.link.shapes.Field.field;
import static com.metreeca.link.shapes.Localized.localized;
import static com.metreeca.rest.codecs.JSONLD.*;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

final class JSONLDTest {

    private static final String base="http://example.com/";

    private final IRI direct=iri(base, "/direct");
    private final IRI nested=iri(base, "/nested");
    private final IRI reverse=iri(base, "/reverse");
    private final IRI outlier=iri(base, "/outlier");


    private void exec(final Runnable task) {
        new Locator().exec(task).clear();
    }


    @Nested final class Decoder {

        private Request request(final String json) {
            return JSONLD.shape(new Request().base(base)

                            .header("Content-Type", MIME), field(direct, required()))

                    .input(() -> new ByteArrayInputStream(json.getBytes(UTF_8)));
        }

        private Response response(final Request request) {
            return request.reply(OK).map(response -> shape(response, shape(request))
                    .body(new JSONLD(), request.body(new JSONLD()))
            );
        }


        @Test void testReportMalformedPayload() {

            exec(() -> assertThatExceptionOfType(CodecException.class)

                    .isThrownBy(() ->
                            request("{").map(this::response)
                    )

                    .satisfies(e -> Assertions.assertThat(e.getStatus())
                            .isEqualTo(BadRequest)
                    ));

            //exec(() -> request("{")
            //
            //        .map(this::response)
            //
            //        .map(response -> assertThat(response)
            //                .hasStatus(BadRequest)
            //        )
            //);

        }

        @Test void testReportInvalidPayload() {

            exec(() -> assertThatExceptionOfType(CodecException.class)

                    .isThrownBy(() ->
                            request("{}").map(this::response)
                    )

                    .satisfies(e -> Assertions.assertThat(e.getStatus())
                            .isEqualTo(UnprocessableEntity)
                    ));

            //exec(() -> request("{}")
            //
            //        .map(this::response)
            //
            //        .map(response -> assertThat(response)
            //                .hasStatus(UnprocessableEntity)
            //        )
            //);
        }

    }

    @Nested final class Encoder {

        private Request request() {
            return new Request().base(base);
        }


        private Response response(final Response response) {

            final IRI item=iri(response.item());
            final BNode bnode=bnode();

            return JSONLD.shape(response.status(OK), and(

                            field(direct, required(),
                                    field(nested, required())
                            ),

                            field("reverse", inverse(reverse), required())

                    ))

                    .body(new JSONLD(), frame(item, asList(

                            statement(item, direct, bnode),
                            statement(bnode, nested, item),
                            statement(bnode, reverse, item),
                            statement(item, outlier, bnode)

                    )));
        }


        @Test void testHandleGenericRequests() {
            exec(() -> request().reply().map(this::response)

                    .map(response -> assertThat(response)
                            .hasHeader("Content-Type", JSON.MIME)
                            .hasBody(new JSON(), json -> JSONAssert.assertThat(json)
                                    .doesNotHaveField("@context")
                            )
                    )

            );
        }

        @Test void testHandlePlainJSONRequests() {
            exec(() -> request()

                    .header("Accept", JSON.MIME).reply().map(this::response)

                    .map(response -> assertThat(response)
                            .hasHeader("Content-Type", JSON.MIME)
                            .hasBody(new JSON(), json -> JSONAssert.assertThat(json)
                                    .doesNotHaveField("@context")
                            )
                    )

            );
        }

        @Test void testHandleJSONLDRequests() {
            exec(() -> request()

                    .header("Accept", MIME).reply().map(this::response)

                    .map(response -> assertThat(response)
                            .hasHeader("Content-Type", MIME)
                            .hasBody(new JSON(), json -> JSONAssert.assertThat(json)
                                    .hasField("@context")
                            )
                    )

            );
        }

        @Test void testGenerateJSONLDContextObjects() {
            new Locator()

                    .set(keywords(), () -> singletonMap("@id", "id"))

                    .exec(() -> request()

                            .header("Accept", MIME).reply().map(this::response)

                            .map(response -> assertThat(response)

                                    .hasHeader("Content-Type", MIME)

                                    .hasBody(new JSON(), json -> JSONAssert.assertThat(json)

                                            .hasField("@context", context -> JSONAssert.assertThat(context)

                                                    .hasField("id", "@id") // keywords at top level

                                                    .hasField("direct", direct.stringValue())
                                                    .hasField("reverse", Json.createObjectBuilder()
                                                            .add("@reverse", reverse.stringValue())
                                                    )

                                            )

                                            .hasField("direct", value -> JSONAssert.assertThat(value)

                                                    .hasField("@context", context -> JSONAssert.assertThat(context)

                                                            .doesNotHaveField("id") // keywords only at top level

                                                            .hasField("nested", nested.stringValue())

                                                    )

                                            )

                                    )
                            )
                    )

                    .clear();
        }


        @Test void testTrimPayload() {
            exec(() -> request().reply().map(this::response)

                    .map(response -> assertThat(response)
                            .hasBody(new JSON(), json -> JSONAssert.assertThat(json)
                                    .doesNotHaveField("outlier")
                            )
                    )

            );
        }

        @Test void testLocalizePayload() {
            exec(() -> new Request()

                    .base(base)
                    .header("Accept-Language", "en").reply().map(response1 -> {

                        final IRI item=iri(response1.item());

                        return JSONLD.shape(response1.status(OK), field(direct, localized()))

                                .body(new JSONLD(), frame(item, asList(

                                        statement(item, direct, literal("one", "en")),
                                        statement(item, direct, literal("uno", "it")),
                                        statement(item, direct, literal("ein", "de"))

                                )));
                    })

                    .map(response -> assertThat(response)
                            .hasBody(new JSON(), json -> JSONAssert.assertThat(json)
                                    .hasField("direct", "one")
                            )
                    )
            );
        }

        @Test void testReportInvalidPayload() {
            exec(() -> assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> request().reply().map(response1 -> response(response1).body(new JSONLD(), frame(iri(response1.item())))).map(task(response ->
                    response.output().accept(new ByteArrayOutputStream())
            ))));

        }

    }

}