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

package com.metreeca.rdf4j;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Values.iri;
import static com.metreeca.rdf4j.Scribe.code;
import static com.metreeca.rdf4j.Scribe.text;

final class ScribeTest {

    @Nested final class Assembling {

        @Test void testText() {
            Assertions.assertThat(code(text(100))).isEqualTo("100");
            Assertions.assertThat(code(text(iri("test:iri")))).isEqualTo("<test:iri>");
            Assertions.assertThat(code(text("verbatim"))).isEqualTo("verbatim");
        }

    }

    @Nested final class Formatting {

        private String format(final CharSequence text) {
            return code(text(text));
        }


        @Test void testCollapseFeeds() {
            Assertions.assertThat(format("x\fy")).isEqualTo("x\n\ny");
            Assertions.assertThat(format("x\n\f\n\fy")).isEqualTo("x\n\ny");
        }

        @Test void testCollapseFolds() {
            Assertions.assertThat(format("x\ry")).isEqualTo("x y");
            Assertions.assertThat(format("x\r\r\ry")).isEqualTo("x y");
            Assertions.assertThat(format("x\n\ry")).isEqualTo("x\n\ny");
            Assertions.assertThat(format("x\n\r\r\ry")).isEqualTo("x\n\ny");
        }

        @Test void testCollapseNewlines() {
            Assertions.assertThat(format("x\ny")).isEqualTo("x\ny");
            Assertions.assertThat(format("x\n\n\n\ny")).isEqualTo("x\ny");
        }

        @Test void testCollapseSpaces() {
            Assertions.assertThat(format("x y")).isEqualTo("x y");
            Assertions.assertThat(format("x    y")).isEqualTo("x y");
        }


        @Test void testIgnoreLeadingWhitespace() {
            Assertions.assertThat(format(" {}")).isEqualTo("{}");
            Assertions.assertThat(format("\n{}")).isEqualTo("{}");
            Assertions.assertThat(format("\r{}")).isEqualTo("{}");
            Assertions.assertThat(format("\f{}")).isEqualTo("{}");
            Assertions.assertThat(format("\f \n\r{}")).isEqualTo("{}");
        }

        @Test void testIgnoreTrailingWhitespace() {
            Assertions.assertThat(format("{} ")).isEqualTo("{}");
            Assertions.assertThat(format("{}\n")).isEqualTo("{}");
            Assertions.assertThat(format("{}\r")).isEqualTo("{}");
            Assertions.assertThat(format("{}\f")).isEqualTo("{}");
            Assertions.assertThat(format("{} \f\n\r")).isEqualTo("{}");
        }


        @Test void testIgnoreLineLeadingWhitespace() {
            Assertions.assertThat(format("x\n  x")).isEqualTo("x\nx");
        }

        @Test void testIgnoreLineTrailingWhitespace() {
            Assertions.assertThat(format("x  \nx")).isEqualTo("x\nx");
        }


        @Test void tesExpandFolds() {
            Assertions.assertThat(format("x\rx\n\rx")).isEqualTo("x x\n\nx");
        }

        @Test void testStripWhitespaceInsidePairs() {
            Assertions.assertThat(format("( x )")).isEqualTo("(x)");
            Assertions.assertThat(format("[ x ]")).isEqualTo("[x]");
            Assertions.assertThat(format("{ x }")).isEqualTo("{ x }");
        }


        @Test void testIndentBraceBlocks() {
            Assertions.assertThat(format("{\nx\n}\ny")).isEqualTo("{\n    x\n}\ny");
            Assertions.assertThat(format("{\f{ x }\f}")).isEqualTo("{\n\n    { x }\n\n}");
        }

        @Test void testInlineBraceBlocks() {
            Assertions.assertThat(format("{ {\nx\n} }\ny")).isEqualTo("{ {\n    x\n} }\ny");
        }

        @Test void test() {
            System.out.println(code(text("\rwhere {\f{\rselect {\f@\f} limit }\f}")));
        }

    }

}