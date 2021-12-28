/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Fetcher.URLFetcher;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.handlers.Router.router;

import static java.lang.String.format;

/**
 * Development server proxy.
 */
public final class Packer extends Delegator {

	public static Packer packer() {
		return new Packer();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final URLFetcher fetcher=new URLFetcher();


	private Packer() {
		delegate(router()

				.head(this::proxy)
				.get(this::proxy)

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> proxy(final Request request) {
		return request.header("Host")

				.map(host -> request

						.reply(response -> fetcher.apply(new Request()

								.method(request.method())
								.base(format("http://%s/", host))
								.path("/index.html")

								.headers(request.headers())

								// disable conditional requests

								.header("If-None-Match", "")
								.header("If-Modified-Since", "")

						))

						.map(response -> response.body(data()).fold(
								error -> { throw error; },
								value -> response.body(data(), value)
						))

				)

				.orElseGet(() -> request

						.reply(status(NotFound))

				);
	}

}
