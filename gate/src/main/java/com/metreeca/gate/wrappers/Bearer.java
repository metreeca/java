/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Response;
import com.metreeca.rest.Wrapper;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;


/**
 * Bearer token authenticator.
 *
 * <p>Manages bearer token authentication protocol delegating token validation to an authentication service.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>
 */
public final class Bearer implements Wrapper {

	private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BiFunction<Long, String, Optional<Wrapper>> authenticator;


	/**
	 * Creates a bearer token authenticator
	 *
	 * @param authenticator the delegated authentication service; takes as argument a timestamp for the operation and
	 *                      the bearer token presented with the request; must return an optional wrapper for
	 *                      pre/post-processing the request on successful token validation or an empty optional
	 *                      otherwise
	 *
	 * @throws NullPointerException if {@code authenticator} is null
	 */
	public Bearer(final BiFunction<Long, String, Optional<Wrapper>> authenticator) {

		if ( authenticator == null ) {
			throw new NullPointerException("null authenticator");
		}

		this.authenticator=authenticator;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(challenger())
				.with(authenticator());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a wrapper adding authentication challenge to unauthorized responses, unless already provided by nested
	 * authorization schemes
	 */
	private Wrapper challenger() {
		return handler -> request -> handler.handle(request).map(response ->
				response.status() == Response.Unauthorized && response.headers("WWW-Authenticate").isEmpty()
						? response.header("WWW-Authenticate", format("Bearer realm=\"%s\"", request.base()))
						: response
		);
	}

	/**
	 * @return a wrapper managing token-based authentication
	 */
	private Wrapper authenticator() {
		return handler -> request -> {

			// !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

			final String authorization=request.header("Authorization").orElse("");

			return Optional

					.of(BearerPattern.matcher(authorization))
					.filter(Matcher::matches)
					.map(matcher -> matcher.group("token"))

					// bearer token > authenticate

					.map(token -> authenticator.apply(currentTimeMillis(), token)

							// authenticated > handle request

							.map(wrapper -> wrapper.wrap(handler).handle(request))

							// not authenticated > report error

							.orElseGet(() -> request.reply(response -> response
									.status(Response.Unauthorized)
									.header("WWW-Authenticate", format(
											"Bearer realm=\"%s\", error=\"invalid_token\"", response.request().base()
									))
							))

					)

					// no bearer token > fall-through to other authorization schemes

					.orElseGet(() -> handler.handle(request));
		};
	}

}
