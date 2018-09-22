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

package com.metreeca.utow;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats._Input;
import com.metreeca.rest.formats._Reader;
import com.metreeca.rest.formats._Writer;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.Trace;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.form.things.Transputs.reader;


/*
 * Undertow gateway.
 *
 * <p>Provides a gateway between a web application managed by a <a href="http://undertow.io/">Undertow</a> server and
 * resource handlers based on the Metreeca/Link linked data framework:</p>
 *
 * <ul>
 *
 * <li>initializes and destroys the shared tool {@linkplain Tray tray} managing platform components required by
 * resource handlers;</li>
 *
 * <li>intercepts HTTP requests and handles them using the {@linkplain Server server} tool provided by the shared tool
 * tray.</li>
 *
 * </ul>
 *
 * @deprecated Work in progress
 */
@Deprecated public final class Gateway implements HttpHandler {

	public static void run(final int port, final String host, final Function<Tray, Handler> loader) {

		if ( port < 0 ) {
			throw new IllegalArgumentException("illegal port ["+port+"]");
		}

		if ( host == null ) {
			throw new NullPointerException("null host");
		}

		if ( loader == null ) {
			throw new NullPointerException("null loader");
		}

		final Gateway gateway=new Gateway();

		final Undertow server=Undertow.builder()
				.addHttpListener(port, host, gateway.start(loader))
				.build();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> { // !!! ;( randomly skipped

			gateway.stop();
			server.stop();

		}));

		server.start();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Tray tray=new Tray();


	private GracefulShutdownHandler handler;


	@Override public void handleRequest(final HttpServerExchange exchange) throws Exception {
		handler.handleRequest(exchange);
	}


	public Gateway start(final Function<Tray, Handler> loader) {

		if ( loader == null ) {
			throw new NullPointerException("null loader");
		}

		if ( handler != null ) {
			throw new IllegalStateException("active gateway");
		}

		tray.get(Trace.Factory).info(this, "starting");

		final Handler handler=loader.apply(tray);

		this.handler=new GracefulShutdownHandler(new CanonicalPathHandler(exchange -> handler

				.handle(request(exchange))
				.accept(response(exchange))

		));

		return this;
	}


	public Gateway stop() {

		if ( handler == null ) {
			throw new IllegalStateException("inactive gateway");
		}

		tray.get(Trace.Factory).info(this, "stopping");

		try {

			handler.shutdown();
			handler.addShutdownListener(success -> tray.clear());
			handler.awaitShutdown();

		} catch ( final InterruptedException ignored ) {

		} finally {

			handler=null;

		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request request(final HttpServerExchange exchange) {

		// !!! handle multi-part forms (https://stackoverflow.com/questions/37839418/multipart-form-data-example-using-undertow)

		final String url=exchange.getRequestURL();

		return new Request()

				.method(exchange.getRequestMethod().toString())

				.base(url.substring(0, url.length()-exchange.getRequestPath().length()+1))
				.path(exchange.getRelativePath()) // canonical form

				.query(exchange.getQueryString())
				.parameters(exchange.getQueryParameters())

				.with(request -> exchange.getRequestHeaders().forEach(header ->
						request.headers(header.getHeaderName().toString(), header)
				))

				.body(_Input.Format, exchange::getInputStream)

				.body(_Reader.Format, () -> reader(exchange.getInputStream(), exchange.getRequestCharset()));
	}

	private Consumer<Response> response(final HttpServerExchange exchange) {
		return response -> {

			exchange.setStatusCode(response.status());

			response.headers().forEach((name, values) ->
					exchange.getResponseHeaders().putAll(HttpString.tryFromString(name), values)
			);

			try (final StringWriter writer=new StringWriter(1000)) {

				response.body(_Writer.Format).value().ifPresent(consumer -> consumer.accept(writer));

				exchange.getResponseSender().send(writer.toString()); // !!! stream

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			// !!! handle binary data
		};
	}

}