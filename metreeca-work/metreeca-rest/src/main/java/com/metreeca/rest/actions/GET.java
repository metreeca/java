
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

import com.metreeca.rest.Format;
import com.metreeca.rest.Request;

import java.util.Optional;
import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * Resource retrieval.
 *
 * <p>Maps textual resource URIs to optional resource bodies.</p>
 *
 * @param <T> the type of the resource body
 */
public final class GET<T> implements Function<String, Optional<T>> {

	private final Format<T> format;
	private final Function<Request, Request> customizer;


	/**
	 * Creates a resource retriever.
	 *
	 * @param format the format of the resource to be retrieved
	 *
	 * @throws NullPointerException if {@code format} is null
	 */
	public GET(final Format<T> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		this.format=format;
		this.customizer=identity();
	}

	/**
	 * Creates a customized retriever.
	 *
	 * @param format     the format of the resource to be retrieved
	 * @param customizer the request customizer
	 *
	 * @throws NullPointerException i feither {@code format} or {@code customizer} is null
	 */
	public GET(final Format<T> format, final Function<Request, Request> customizer) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		this.format=format;
		this.customizer=customizer;
	}


	@Override public Optional<T> apply(final String url) {
		return Optional.of(url)

				.flatMap(new Query(customizer.compose(request -> request
						.header("Accept", format.mime())
				)))

				.flatMap(new Fetch())
				.flatMap(new Parse<>(format));
	}

}
