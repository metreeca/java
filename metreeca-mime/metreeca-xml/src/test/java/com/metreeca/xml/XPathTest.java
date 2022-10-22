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

package com.metreeca.xml;

import com.metreeca.core.Xtream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.xml.XPath.decode;
import static com.metreeca.xml.codecs.XML.xml;

import static org.assertj.core.api.Assertions.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;


final class XPathTest {

    @Test void testDecodeNumericEntities() {
        assertThat(decode("Italy&#x2019;s &#8220;most powerful&#8221; car"))
                .isEqualTo("Italy’s “most powerful” car");
    }

    @Test void testUseEnclosingNamespaces() {
        assertThat(Xtream

                .of("<ns:x xmlns:ns='http://example.com/o'><ns:y><ns:z>text</ns:z></ns:y></ns:x>")

                .map(xml -> xml(new ByteArrayInputStream(xml.getBytes(UTF_8))))

                .map(XPath::new).flatMap(xpath -> xpath.nodes("//ns:y"))
                .map(XPath::new).flatMap(xpath -> xpath.strings("//ns:z"))

        ).containsExactly("text");
    }

}
