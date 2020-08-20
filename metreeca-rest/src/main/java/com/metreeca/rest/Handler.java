/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest;

import java.util.function.Predicate;


/**
 * Resource handler {thread-safe}.
 *
 * <p>Exposes and manages the state of a linked data resource, generating {@linkplain Response responses} in reaction
 * to {@linkplain Request requests}.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
@FunctionalInterface public interface Handler {

	/**
	 * Creates a dummy handler.
	 *
	 * @return a dummy handler that generates an empty response regardless of the request
	 */
	public static Handler handler() {
		return request -> request.reply(response -> response);
	}

	/**
	 * Creates a conditional handler.
	 *
	 * @param test the request predicate used to decide if requests and responses are to be routed to the handler
	 * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true} on
	 *             the request
	 *
	 * @return a conditional handler that routes requests and responses to the {@code pass} handler if the {@code test}
	 * 		predicate evaluates to {@code true} on the request or to a {@linkplain #handler() dummy handler} otherwise
	 *
	 * @throws NullPointerException if either {@code test} or {@code pass} is null
	 */
	public static Handler handler(final Predicate<Request> test, final Handler pass) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass handler");
		}

		return handler(test, pass, handler());
	}

	/**
	 * Creates a conditional handler.
	 *
	 * @param test the request predicate used to select the handler requests and responses are to be routed to
	 * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true} on
	 *             the request
	 * @param fail the handler requests and responses are to be routed to when {@code test} evaluates to {@code false }
	 *             on the request
	 *
	 * @return a conditional handler that routes requests and responses either to the {@code pass} or the {@code fail}
	 * 		handler according to the results of the {@code test} predicate
	 *
	 * @throws NullPointerException if any of the arguments is null
	 */
	public static Handler handler(final Predicate<Request> test, final Handler pass, final Handler fail) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass handler");
		}

		if ( fail == null ) {
			throw new NullPointerException("null fail handler");
		}

		return request -> (test.test(request) ? pass : fail).handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles a request.
	 *
	 * @param request the inbound request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}; lazy
	 * 		processing supports streaming processing inside ephemeral context like database connections and transactions
	 */
	public Future<Response> handle(final Request request);


	/**
	 * Chains a wrapper.
	 *
	 * @param wrapper the wrapper to be chained
	 *
	 * @return the combined handler obtained by {@linkplain Wrapper#wrap(Handler) wrapping} this handler inside the
	 * 		combined wrapper generated by {@linkplain Wrapper#with(Wrapper) wrapping} {@code wrapper}
	 * 		<strong>inside</strong> previously chained wrappers
	 *
	 * @throws NullPointerException if {@code wrapper} is null
	 */
	public default Handler with(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		final class Chain implements Handler {

			private final Wrapper wrapper;
			private final Handler handler;
			private final Handler chained;


			private Chain(final Wrapper wrapper, final Handler handler) {
				this.wrapper=wrapper;
				this.handler=handler;
				this.chained=wrapper.wrap(handler);
			}


			@Override public Handler with(final Wrapper wrapper) {

				if ( wrapper == null ) {
					throw new NullPointerException("null wrapper");
				}

				return new Chain(this.wrapper.with(wrapper), handler);
			}

			@Override public Future<Response> handle(final Request request) {

				if ( request == null ) {
					throw new NullPointerException("null request");
				}

				return chained.handle(request);
			}

		}

		return new Chain(wrapper, this);
	}

}
