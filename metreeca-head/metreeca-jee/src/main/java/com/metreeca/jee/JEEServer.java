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

package com.metreeca.jee;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Loader;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.services.Logger.logger;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;


/**
 * Java EE Servlet connector.
 *
 * <p>Connects web applications managed by Servlet 3.1 container with resource handlers based on the Metreeca/Java
 * framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the {@linkplain Toolbox toolbox} managing shared services required by resource handlers;
 * </li>
 *
 * <li>intercepts HTTP requests and handles them using a {@linkplain Handler handler} loaded from the toolbox;</li>
 *
 * <li>forwards HTTP requests to the enclosing web application if no response is committed by REST handlers.</li>
 *
 * </ul>
 */
public abstract class JEEServer implements Filter {

	private static Supplier<Handler> delegate() { return () -> request -> request.reply(identity()); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Toolbox toolbox=new Toolbox();


	/**
	 * Configures the delegate handler factory.
	 *
	 * @param factory the delegate handler factory; takes as argument a shared service manager (which may configured
	 *                   with
	 *                additional application-specific services as a side effect) and must return a non-null handler
	 *                to be
	 *                used as entry point for serving requests
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code factory} is null or returns a null value
	 */
	protected JEEServer delegate(final Function<Toolbox, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		toolbox.set(delegate(), () -> requireNonNull(factory.apply(toolbox), "null handler"));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void init(final FilterConfig config) {

		final ServletContext context=config.getServletContext();

		try {

			toolbox

					.set(Toolbox.storage(), () -> storage(context))
					.set(Loader.loader(), () -> loader(context))

					.get(delegate()); // force handler loading during filter initialization

		} catch ( final Throwable t ) {

			try ( final StringWriter message=new StringWriter() ) {

				t.printStackTrace(new PrintWriter(message));

				toolbox.get(logger()).error(this, "error during initialization: "+message);

				context.log("error during initialization", t);

				throw t;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				this.toolbox.clear();

			}

		}
	}

	@Override public void destroy() {

		toolbox.clear();

	}


	private Path storage(final ServletContext context) {
		return ((File)context.getAttribute(ServletContext.TEMPDIR)).toPath();
	}

	private Loader loader(final ServletContext context) {
		return path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			try {

				return Optional.ofNullable(context.getResource(path));

			} catch ( final MalformedURLException e ) {

				throw new IllegalArgumentException(e);

			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void doFilter(
			final ServletRequest request, final ServletResponse response, final FilterChain chain
	) throws ServletException, IOException {

		try {

			toolbox.exec(() -> toolbox.get(delegate())
					.handle(request((HttpServletRequest)request))
					.accept(_response -> response((HttpServletResponse)response, _response))
			);

			if ( !response.isCommitted() ) {
				chain.doFilter(request, response);
			}

		} catch ( final RuntimeException e ) {

			if ( !e.toString().toLowerCase(Locale.ROOT).contains("broken pipe") ) {
				toolbox.get(logger()).error(this, "unhandled exception", e);
			}

		}

	}


	private Request request(final HttpServletRequest http) {

		final String target=http.getRequestURL().toString();
		final String path=http.getRequestURI().substring(http.getContextPath().length());
		final String base=target.substring(0, target.length()-path.length()+1);
		final String query=http.getQueryString();

		final Request request=new Request()
				.method(http.getMethod())
				.base(base)
				.path(path)
				.query(query != null ? query : "");

		for (final Map.Entry<String, String[]> parameter : http.getParameterMap().entrySet()) {

			final String key=parameter.getKey();
			final String[] value=parameter.getValue();

			if ( nonNull(key) && nonNull(value) ) { // ;( possibly null header names…
				request.parameters(key, asList(value));
			}
		}

		for (final String name : list(http.getHeaderNames())) {
			request.headers(name, list(http.getHeaders(name)));
		}

		return request.body(input(), () -> {
			try {
				return http.getInputStream();
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});

	}

	private void response(final HttpServletResponse http, final Response response) {
		if ( response.status() > 0 ) { // unprocessed requests fall through the host container

			http.setStatus(response.status());

			response.headers().forEach((name, values) ->
					values.forEach(value -> http.addHeader(name, value))
			);

			response.body(output()).accept(e -> {}, target -> { // ignore missing response bodies
				try {

					target.accept(http.getOutputStream());

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});

			if ( !http.isCommitted() ) { // flush if not already committed by bodies
				try {
					http.flushBuffer();
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}

		}
	}

}
