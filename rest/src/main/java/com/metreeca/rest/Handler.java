/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest;

import com.metreeca.form.Form;

import java.util.Optional;
import java.util.function.Consumer;

import static com.metreeca.form.things.JSON.field;
import static com.metreeca.form.things.JSON.object;


/**
 * Linked data resource handler.
 *
 * <p>Exposes and manages the state of a linked data resource, generating outgoing {@linkplain Response responses}
 * in reaction to to incoming {@linkplain Request requests}.</p>
 */
@FunctionalInterface public interface Handler {

	//// Shared Handlers ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles refused requests.
	 *
	 * <p>Generates an empty response with a {@value Response#Unauthorized} status code, if the request {@linkplain
	 * Request#user()  user} is anonymous (that it's equal to {@link Form#none}), or a {@value Response#Forbidden}
	 * status code, otherwise.</p>
	 *
	 * @param request  the incoming request
	 * @param response the outgoing response
	 *
	 * @throws NullPointerException if either {@code request} or {@code response} is {@code null}
	 */
	public static void refused(final Request request, final Response response) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		if ( request.user().equals(Form.none) ) {
			unauthorized(request, response);
		} else {
			forbidden(request, response);
		}
	}

	/**
	 * Handles unauthorized requests.
	 *
	 * <p>Generates an empty response with a {@value Response#Unauthorized} status code.</p>
	 *
	 * @param request  the incoming request
	 * @param response the outgoing response
	 *
	 * @throws NullPointerException if either {@code request} or {@code response} is {@code null}
	 */
	public static void unauthorized(final Request request, final Response response) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		response.status(Response.Unauthorized).done(); // WWW-Authenticate header generated by wrappers
	}

	/**
	 * Handles forbidden requests.
	 *
	 * <p>Generates an empty response with a {@value Response#Forbidden} status code.</p>
	 *
	 * @param request  the incoming request
	 * @param response the outgoing response
	 *
	 * @throws NullPointerException if either {@code request} or {@code response} is {@code null}
	 */
	public static void forbidden(final Request request, final Response response) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		response.status(Response.Forbidden).done();
	}


	//// JSON Error Report Factories ///////////////////////////////////////////////////////////////////////////////////

	public static Object error(final String error) {
		return object(field("error", error));
	}

	public static Object error(final String error, final Throwable cause) {
		return error(error, Optional.ofNullable(cause.getMessage()).orElseGet(cause::toString));
	}

	public static Object error(final String error, final Object cause) {
		return object(field("error", error), field("cause", cause));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles a request/response exchange.
	 *
	 * @param request  the incoming request for the managed linked data resource
	 * @param response the outgoing response generated for the managed linked data resource in reaction to {@code
	 *                 request}
	 */
	public void handle(final Request request, final Response response);

	/**
	 * Handles a request/response exchange.
	 *
	 * @param source the source of the incoming request
	 * @param target the target of the outgoing response
	 *
	 * @throws NullPointerException if either {@code source} or {@code target} is {@code null}
	 */
	public default void handle(final Consumer<Request.Writer> source, final Consumer<Response.Reader> target) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		source.accept(new Request.Writer(request -> handle(request, new Response(request, target))));
	}


	/**
	 * Chains a wrapper with this handler.
	 *
	 * @param wrapper the wrapper to be chained with this handler
	 *
	 * @return the combined handler obtained by {@linkplain Wrapper#wrap(Handler) wrapping} {@code wrapper} around this
	 * handler
	 *
	 * @throws NullPointerException if {@code wrapper} is {@code null}
	 */
	public default Handler wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return wrapper.wrap(this);
	}

}
