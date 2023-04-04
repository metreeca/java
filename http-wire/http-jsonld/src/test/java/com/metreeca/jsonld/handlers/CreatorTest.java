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

package com.metreeca.jsonld.handlers;

import org.junit.jupiter.api.Test;

final class CreatorTest {


    @Test void testCreateResource() {
        //exec(
        //
        //		frame -> {
        //
        //			assertThat(frame.focus()).as("generated unique iri").isNotEqualTo(focus);
        //			assertThat(frame.values(RDF.VALUE)).as("rewritten body").containsExactly(frame.focus());
        //
        //			return true;
        //
        //		},
        //
        //		() -> new Creator()
        //
        //				.handle(shape(new Request(), shape)
        //								.body(new JSONLD(), frame(focus)
        //										.value(RDF.VALUE, focus)
        //								),
        //						Request::reply
        //				)
        //
        //				.map(response -> assertThat(response)
        //						.hasStatus(Created)
        //						.hasAttribute(Shape.class, shape -> assertThat(shape).isEqualTo(or()))
        //						.doesNotHaveBody()
        //				)
        //
        //);
    }

    @Test void testReportClash() {
        //assertThatIllegalStateException().isThrownBy(() -> exec(frame -> false, () -> new Creator()
        //
        //		.handle(shape(new Request(), shape)
        //						.body(new JSONLD(), frame(item("/"))),
        //				Request::reply
        //		)
        //
        //));
    }

}