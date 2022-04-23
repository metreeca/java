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

package com.metreeca.rest;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.Request.GET;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;


final class RequestTest {

	@Test void testParametersIgnoreEmptyHeaders() {

		final Request request=new Request()
				.parameters("parameter", emptySet());

		assertThat(request.parameters().entrySet()).isEmpty();
	}

	@Test void testParametersOverwritesValues() {

		final Request request=new Request()
				.parameter("parameter", "one")
				.parameter("parameter", "two");

		assertThat(request.parameters("parameter")).containsExactly("two");

	}

	@Test void testRoute() {

		assertThat(new Request().method(GET).path("/path/file.ext").route()).isFalse();
		assertThat(new Request().method(GET).path("/path/route/").route()).isFalse();
		assertThat(new Request().method(GET).path("/path/route").route()).isFalse();

		assertThat(new Request()
				.method(GET)
				.path("/path/route")
				.header("Accept", "text/xhtml,charset=utf8")
				.route()
		).isTrue();

		assertThat(new Request()
				.method(GET)
				.path("/path/route.ext")
				.header("Accept", "text/xhtml,charset=utf8")
				.route()
		).isFalse();

		assertThat(new Request()
				.method(GET)
				.path("/path/route")
				.header("Accept", "application/xml,charset=utf8")
				.route()
		).isFalse();

	}

	@Test void testAsset() {

		assertThat(new Request().method(GET).path("/path/file.ext").asset()).isTrue();

		assertThat(new Request().method(GET).path("/path/route/").asset()).isFalse();
		assertThat(new Request().method(GET).path("/path/route").asset()).isFalse();

		assertThat(new Request()
				.method(GET)
				.path("/path/route")
				.header("Accept", "text/xhtml,charset=utf8")
				.asset()
		).isTrue();

		assertThat(new Request()
				.method(GET)
				.path("/path/route.ext")
				.header("Accept", "text/xhtml,charset=utf8")
				.asset()
		).isTrue();

		assertThat(new Request()
				.method(GET)
				.path("/path/route")
				.header("Accept", "application/xml,charset=utf8")
				.asset()
		).isFalse();

	}

}
