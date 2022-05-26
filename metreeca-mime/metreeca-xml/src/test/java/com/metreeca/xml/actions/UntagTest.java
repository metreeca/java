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

package com.metreeca.xml.actions;

import org.junit.jupiter.api.Test;

import static com.metreeca.xml.actions.Untag.untag;

import static org.assertj.core.api.Assertions.assertThat;

final class UntagTest {

    @Test void testConvertBlocks() {
        assertThat(untag(
                "<p>one</p>\n"
                        +"<div>two</div>"
        )).isEqualTo(
                "one\n\ntwo"
        );
    }

    @Test void testNormalizeBlockText() {
        assertThat(untag(
                "<p> \tone\n\ntwo\n\t</p>"
        )).isEqualTo(
                "one two"
        );
    }

    @Test void testConvertULs() {
        assertThat(untag(
                "<ul>\n"
                        +"    <li>one</li>\n"
                        +"    <li>two</li>\n"
                        +"</ul>"
        )).isEqualTo(
                "- one\n"
                        +"- two\n"
        );
    }

    //@Test void testConvertNestedULs() {
    //    assertThat(untag(
    //            "<ul>\n"
    //                    +"    <li>one</li>\n"
    //                    +"    <li>\n"
    //                    +"        <ul>\n"
    //                    +"            <li>two</li>\n"
    //                    +"            <li>three</li>\n"
    //                    +"        </ul>\n"
    //                    +"    </li>\n"
    //                    +"    <li>four</li>\n"
    //                    +"</ul>"
    //    )).isEqualTo(
    //            "-one\n"
    //                    +"  - two\n"
    //                    +"  - three\n"
    //                    +"- four\n"
    //    );
    //}

}