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

package com.metreeca.json.codecs;

import com.metreeca.http.CodecException;
import com.metreeca.http.Request;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import static com.metreeca.http.MessageAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.nio.charset.StandardCharsets.UTF_8;


final class JSONTest {

	private static final JsonObject TestJSON=Json.createObjectBuilder()
			.add("one", 1)
			.add("two", 2)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveJSON() {

		final Request request=new Request()
				.header("Content-Type", JSON.MIME)
				.input(() -> new ByteArrayInputStream(TestJSON.toString().getBytes(UTF_8)));

		assertThat(request.body(new JSON()))
				.isEqualTo(TestJSON);
	}

	@Test void testRetrieveJSONChecksContentType() {

		final Request request=new Request()
				.input(() -> new ByteArrayInputStream(TestJSON.toString().getBytes(UTF_8)));

		assertThatExceptionOfType(CodecException.class)
				.isThrownBy(() -> request.body(new JSON()));
	}

	@Test void testConfigureJSON() {
		assertThat(new Request().body(new JSON(), TestJSON))

				.hasTextOutput(json -> assertThat(Json.createReader(new StringReader(json)).readObject())
						.isEqualTo(TestJSON)
				);

	}

	@Test void testConfigureJSONSetsContentType() {

		final Request request=new Request().body(new JSON(), TestJSON);

		assertThat(request.header("content-type"))
				.isPresent()
				.contains(JSON.MIME);

	}

}
