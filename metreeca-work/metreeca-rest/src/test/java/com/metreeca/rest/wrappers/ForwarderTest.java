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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.Response.MovedPermanently;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;

final class ForwarderTest {

	private final Handler relocator=new Forwarder(MovedPermanently)

			.rewrite("(http://example)(?:\\.\\w+)/(.*)", "$1.com/$2")
			.rewrite("http:(.*)", "https:$1")

			.wrap((request, next) -> request.reply(OK));


    @Test void testRelocate() {
        relocator

                .handle(new Request()
                                .base("http://example.org/")
                                .path("/path"),
                        Request::reply
                )

				.map(response -> assertThat(response)
                        .hasStatus(MovedPermanently)
                        .hasHeader("Location", "https://example.com/path")
                );
    }

    @Test void testForward() {
        relocator

                .handle(new Request()
                                .base("https://example.com/")
                                .path("/path"),
                        Request::reply
                )

				.map(response -> assertThat(response)
                        .hasStatus(OK)
                        .doesNotHaveHeader("Location")
                );
    }

}