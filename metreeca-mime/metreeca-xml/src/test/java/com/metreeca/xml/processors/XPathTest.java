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

package com.metreeca.xml.processors;

import com.metreeca.core.Xtream;
import com.metreeca.http.Either;
import com.metreeca.xml.formats.XMLFormat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.xml.processors.XPath.decode;

import static java.nio.charset.StandardCharsets.UTF_8;


final class XPathTest {

    @Test void testDecodeNumericEntities() {
        Assertions.assertThat(decode("Italy&#x2019;s &#8220;most powerful&#8221; car"))
                .isEqualTo("Italy’s “most powerful” car");
    }

    @Test void testUseEnclosingNamespaces() {
        Assertions.assertThat(Xtream

                .of("<ns:x xmlns:ns='http://example.com/o'><ns:y><ns:z>text</ns:z></ns:y></ns:x>")

                .map(x -> XMLFormat.xml(new ByteArrayInputStream(x.getBytes(UTF_8))))

                .optMap(Either::get)

                .map(XPath::new).flatMap(xpath -> xpath.nodes("//ns:y"))
                .map(XPath::new).flatMap(xpath -> xpath.strings("//ns:z"))

        ).containsExactly("text");
    }

}
