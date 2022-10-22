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

package com.metreeca.rdf.codecs;

import com.metreeca.core.Locator;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

import static com.metreeca.http.Message.mimes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;


final class RDFTest {

    private void exec(final Runnable... tasks) {
        new Locator().exec(tasks).clear();
    }


    @Nested final class Services {

        private final RDFTestService Turtle=new RDFTestService(RDFFormat.TURTLE);
        private final RDFTestService RDFXML=new RDFTestService(RDFFormat.RDFXML);
        private final RDFTestService Binary=new RDFTestService(RDFFormat.BINARY);


        @Test void testServiceScanMimeTypes() {

            assertThat((Object)Binary)
                    .as("none matching")
                    .isSameAs(service(RDFFormat.BINARY, mimes("text/none")));

            assertThat((Object)Turtle)
                    .as("single matching")
                    .isSameAs(service(RDFFormat.BINARY, mimes("text/turtle")));

            assertThat((Object)Turtle)
                    .as("leading matching")
                    .isSameAs(service(RDFFormat.BINARY, asList("text/turtle", "text/plain")));

            assertThat((Object)Turtle)
                    .as("trailing matching")
                    .isSameAs(service(RDFFormat.BINARY, asList("text/turtle", "text/none")));

            assertThat((Object)Binary)
                    .as("wildcard")
                    .isSameAs(service(RDFFormat.BINARY, mimes("*/*, text/plain;q=0.1")));

            assertThat((Object)Turtle)
                    .as("type pattern")
                    .isSameAs(service(RDFFormat.BINARY, mimes("text/*, text/plain;q=0.1")));

        }

        @Test void testServiceTrapUnknownFallback() {
            assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
                    service(new RDFFormat(
                            "None", "text/none",
                            StandardCharsets.UTF_8, "",
                            RDFFormat.NO_NAMESPACES, RDFFormat.NO_CONTEXTS,
                            RDFFormat.NO_RDF_STAR
                    ), mimes("text/none"))
            );
        }


        private RDFTestService service(final RDFFormat fallback, final List<String> types) {

            final TestRegistry registry=new TestRegistry();

            registry.add(Binary); // no text/* MIME type
            registry.add(RDFXML); // no text/* MIME type
            registry.add(Turtle);

            return RDF.service(registry, types).or(() -> registry.get(fallback)).orElseThrow();
        }


        private final class TestRegistry
                extends FileFormatServiceRegistry<RDFFormat, RDFTestService> {

            private TestRegistry() {
                super(RDFTestService.class);
            }

            @Override protected RDFFormat getKey(final RDFTestService service) {
                return service.getFormat();
            }

        }

    }

}
