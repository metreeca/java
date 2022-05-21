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

package com.metreeca.rest._wrappers;

import com.metreeca.rest.*;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Response.SeeOther;
import static com.metreeca.rest._MessageException.status;

import static java.util.Objects.requireNonNull;


/**
 * Resource aliaser.
 *
 * <p>Redirects request for alias resources to the canonical resource they {@linkplain #Aliaser(Function) resolve}
 * to.</p>
 *
 * <p>Empty or idempotent requests, that is requests whose {@link Request#item() focus item} is resolved to an empty
 * optional, to an empty string or to itself, are delegated to the wrapped handler.</p>
 */
public final class Aliaser implements _Wrapper {

	private final Function<Request, Optional<String>> resolver;


	/**
	 * Creates a resource aliaser.
	 *
	 * @param resolver the resource resolving function; takes as argument a request and returns the canonical IRI for
	 *                   the
	 *                 aliased request {@linkplain Request#item() item}, if one was identified, or an empty optional,
	 *                 otherwise
	 *
	 * @throws NullPointerException if {@code resolver} is null or returns a null value
	 */
	public Aliaser(final Function<Request, Optional<String>> resolver) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		this.resolver=resolver;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return (request, next) -> alias(request).orElseGet(() -> handler.handle(request, next));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Response> alias(final Request request) {
        return requireNonNull(resolver.apply(request), "null resolver return value")

		        .filter(resource -> !resource.isEmpty())
		        .filter(resource -> !idempotent(request.item(), resource))

		        .map(resource -> request.reply().map(status(SeeOther, resource)));
    }


	private boolean idempotent(final String item, final String resource) {
		return item.equals(URI.create(item).resolve(resource).toString());
	}

}
