/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rest.wrappers;

import com.metreeca.link.Shape;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.shapes.Clazz.clazz;
import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;


final class DriverTest {

    @Test void testConfigureRequestShape() {

        final Shape test=clazz(RDF.NIL);

        new Driver(test)

                .wrap((request, next) -> {

                    assertThat(request)
							.hasAttribute(Shape.class, shape -> assertThat(shape).isEqualTo(shape));

                    return request.reply(OK);

                })

                .handle(new Request(), Request::reply)

				.map(response -> assertThat(response)
                        .hasStatus(OK)
                );
    }

}
