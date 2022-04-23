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

package com.metreeca.rest.actions;

import com.metreeca.http.services.Logger;
import com.metreeca.rest.Request;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;

import static java.lang.String.format;


/**
 * Request generation.
 *
 * <p>Maps textual resource URIs to optional resource requests.</p>
 */
public final class Query implements Function<String, Optional<Request>> {

	private final Function<Request, Request> customizer;

	private final Logger logger=service(Logger.logger());


	/**
	 * Creates a new default request generator.
	 */
	public Query() {
		this(request -> request);
	}

	/**
	 * Creates a new customized request generator.
	 *
	 * @param customizer the request customizer
	 *
	 * @throws NullPointerException if {@code customizer} is null
	 */
	public Query(final Function<Request, Request> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		this.customizer=customizer;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Generates a resource request
	 *
	 * @param resource the textual URI of the target resource
	 *
	 * @return a optional GET or possibly customized request for the given resource {@code resource}, if it was not null
	 * and successfully parsed into absolute {@linkplain Request#base() base}, {@linkplain Request#path() path} and
	 * {@linkplain Request#query() query} components; an empty optional, otherwise, logging an error to the {@linkplain
	 * Logger#logger() shared event logger}
	 */
	@Override public Optional<Request> apply(final String resource) {
		return Optional.ofNullable(resource)

				.map(uri -> {
					try {

						return new URI(uri).normalize();

					} catch ( final URISyntaxException e ) {

						logger.error(this, format("unable to parse resource URI <%s>", uri));

						return null;

					}
				})

				.filter(URI::isAbsolute)

				.map(uri -> request(uri)

						.method(Request.GET)

						.query(Optional.ofNullable(uri.getRawQuery()).orElse(""))

						.map(customizer)

				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request request(final URI uri) {
		switch ( uri.getScheme() ) {

			case "http":
			case "https":

				return httpx(uri);

			case "file":

				return file(uri);

			case "jar":

				return jar(uri);

			default:

				return wild(uri);

		}
	}


	private Request httpx(final URI uri) {
		return new Request()

				.base(uri.getScheme()+":"+Optional
						.ofNullable(uri.getRawAuthority())
						.map(s -> "//"+s+"/")
						.orElse("/")
				)

				.path(Optional.ofNullable(uri.getRawPath()).orElse("/"));
	}

	private Request file(final URI uri) {
		return new Request()
				.base("file:/")
				.path(uri.getRawSchemeSpecificPart());
	}

	private Request jar(final URI uri) {

		final String part=uri.getRawSchemeSpecificPart();
		final int bang=part.indexOf("!/");

		if ( bang < 0 ) {
			throw new IllegalArgumentException(format("missing '!/' {%s}", uri));
		}

		return new Request()
				.base("jar:"+part.substring(0, bang+2))
				.path(part.substring(bang+1));
	}

	private Request wild(final URI uri) {
		throw new UnsupportedOperationException(format("unsupported uri scheme {%s}", uri));
	}

}
