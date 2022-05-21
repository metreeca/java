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

package com.metreeca.rest;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * HTTP response.
 */
public final class Response extends Message<Response> {

	public static final int OK=200; // https://tools.ietf.org/html/rfc7231#section-6.3.1
	public static final int Created=201; // https://tools.ietf.org/html/rfc7231#section-6.3.2
	public static final int Accepted=202; // https://tools.ietf.org/html/rfc7231#section-6.3.3
	public static final int NonAuthoritativeInformation=203; // https://tools.ietf.org/html/rfc7231#section-6.3.4
	public static final int NoContent=204; // https://tools.ietf.org/html/rfc7231#section-6.3.5

	public static final int MultipleChoices=300; // https://tools.ietf.org/html/rfc7231#section-6.4.1
	public static final int MovedPermanently=301; // https://tools.ietf.org/html/rfc7231#section-6.4.2
	public static final int Found=302; // https://tools.ietf.org/html/rfc7231#section-6.4.3
	public static final int SeeOther=303; // https://tools.ietf.org/html/rfc7231#section-6.4.4
	public static final int NotModified=304; // https://tools.ietf.org/html/rfc7232#section-4.1
	public static final int TemporaryRedirect=307; // https://tools.ietf.org/html/rfc7231#section-6.4.7
	public static final int PermanentRedirect=308; // https://tools.ietf.org/html/rfc7538#section-3

	public static final int BadRequest=400; // https://tools.ietf.org/html/rfc7231#section-6.5.1
	public static final int Unauthorized=401; // https://tools.ietf.org/html/rfc7235#section-3.1
	public static final int Forbidden=403; // https://tools.ietf.org/html/rfc7231#section-6.5.3
	public static final int NotFound=404; // https://tools.ietf.org/html/rfc7231#section-6.5.4
	public static final int MethodNotAllowed=405; // https://tools.ietf.org/html/rfc7231#section-6.5.5
	public static final int Conflict=409; // https://tools.ietf.org/html/rfc7231#section-6.5.8
	public static final int PayloadTooLarge=413; // https://tools.ietf.org/html/rfc7231#section-6.5.11
	public static final int UnsupportedMediaType=415; // https://tools.ietf.org/html/rfc7231#section-6.5.13
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4
	public static final int GatewayTimeout=504; // https://tools.ietf.org/html/rfc7231#section-6.6.5


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Request request; // the originating request

	private int status; // the HTTP status code


	/**
	 * Creates a new response for a request.
	 *
	 * @param request the originating request for the new response
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Response(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		this.request=request;
	}


	@Override Response self() {
		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the focus item IRI of this response.
	 *
	 * @return the absolute IRI included in the {@code Location} header of this response, if defined; the {@linkplain
	 * Request#item() focus item} IRI of the originating request otherwise
	 */
	@Override public String item() {

		final String base=request().item();

		return header("Location")
				.map(location -> {
					try {

						return new URI(base).resolve(location).toString();

					} catch ( final URISyntaxException e ) {

						return null;

					}
				})
				.orElse(base);
	}

	/**
	 * Retrieves the originating request for this response.
	 *
	 * @return the originating request for this response
	 */
	@Override public Request request() {
		return request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if this response is successful.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in the {@code 2XX} range; {@code false}
	 * otherwise
	 */
	public boolean success() {
		return status/100 == 2;
	}

	/**
	 * Checks if this response is an error.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in beyond the {@code 3XX} range; {@code false}
	 * otherwise
	 */
	public boolean error() {
		return status/100 > 3;
	}


	//// Outcome //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the status code of this response.
	 *
	 * @return the status code of this response
	 */
	public int status() {
		return status;
	}

	/**
	 * Configures the status code of this response.
	 *
	 * @param status the status code of this response
	 *
	 * @return this response
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
	 */
	public Response status(final int status) {

		if ( status != 0 ) { // reserved for system use
			if ( status < 100 || status > 599 ) {
				throw new IllegalArgumentException("illegal status code ["+status+"]");
			}
		}

		this.status=status;

		return this;
	}

}
