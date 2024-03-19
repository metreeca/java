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
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.ResponseAssert.assertThat;
import static com.metreeca.link.Shape.type;

import static org.assertj.core.api.Assertions.assertThat;


final class DriverTest {

    @Test void testConfigureRequestShape() {

        final IRI type=RDFS.CONTAINER;
        final Shape test=type(type);

        handler(new Driver(test), (request, next) -> {

            assertThat(request.attribute(Shape.class).flatMap(Shape::type)).contains(Set.of(type));

            return request.reply(OK);

        })

                .handle(new Request(), Request::reply)

                .map(response -> assertThat(response)
                        .hasStatus(OK)
                );
    }

}
