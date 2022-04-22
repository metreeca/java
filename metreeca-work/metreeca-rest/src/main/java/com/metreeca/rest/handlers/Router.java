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

package com.metreeca.rest.handlers;


import com.metreeca.rest.*;

import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Request.*;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;


/**
 * Request router.
 *
 * <dl>
 *
 *     <dt><strong>Path-Based Routing</strong></dt>
 *
 *     <dd>
 *
 *          <p>Delegates request processing to a handler selected on the basis of the request HTTP
 *          {@linkplain Request#path() path}, ignoring the leading segment possibly already matched by wrapping
 *          routers.</p>
 *
 *          <p>Requests are forwarded to a {@linkplain #path(String, Handler) registered} handler if their path is
 *          matched by an associated pattern defined by a sequence of steps according to the following rules:</p>
 *
 *          <ul>
 *
 *          <li>the empty step ({@code /}) matches only an empty step ({@code /});</li>
 *
 *          <li>literal steps {@code /<step>} match path steps verbatim;</li>
 *
 *          <li>wildcard steps {@code /{}} match a single path step;</li>
 *
 *          <li>placeholder steps {@code /{<key>}} match a single path step, adding the matched {@code <key>}/{@code
 *          <step>} entry to request {@linkplain Request#parameters() parameters}; the matched {@code <step>} name is
 *          URL-decoded before use;</li>
 *
 *          <li>prefix steps {@code /*} match one or more trailing path steps.</li>
 *
 *          </ul>
 *
 *          <p>Registered path patterns are tested in order of definition.</p>
 *
 *     </dd>
 *
 *     <dt><strong>Method-Based Routing</strong></dt>
 *
 *     <dd>
 *
 *          <p>If the route index doesn't contain a matching handler, delegates request processing to a handler
 *          selected on the basis of the request HTTP {@linkplain Request#method() method}.</p>
 *
 *          <p>{@linkplain Request#OPTIONS OPTIONS} and {@linkplain Request#HEAD HEAD} methods are delegated to
 *          user-overridable default handlers.</p>
 *
 *     </dd>
 *
 * </dl>
 */
public final class Router implements Handler {

	private static final Entry<Class<String>, String> RoutingPrefix=entry(
			String.class, Router.class.getName()+"RoutingPrefix"
	);


	private static final Pattern KeyPattern=Pattern.compile(
			"\\{(?<key>[^}]*)}"
	);

	private static final Pattern PathPattern=Pattern.compile(String.format(
			"(?<prefix>(/[^/*{}]*|/%s)*)(?<suffix>/\\*)?", KeyPattern.pattern()
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Function<Request, Optional<Response>>> routes=new LinkedHashMap<>();
	private final Map<String, Handler> methods=new LinkedHashMap<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a handler to this router.
	 *
	 * @param path    the path pattern the handler to be added will be bound to
	 * @param handler the handler to be added to this router at {@code path}
	 *
	 * @return this router
	 *
	 * @throws NullPointerException     if either {@code path} or {@code handler} is null
	 * @throws IllegalArgumentException if {@code path} is not a well-formed sequence of steps
	 * @throws IllegalStateException    if {@code path} is already bound to a handler
	 */
	public Router path(final String path, final Handler handler) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		final Matcher matcher=PathPattern.matcher(path);

		if ( path.isEmpty() || !matcher.matches() ) {
			throw new IllegalArgumentException("malformed path <"+path+">");
		}

		final String prefix=matcher.group("prefix");
		final String suffix=matcher.group("suffix");

		final Function<Request, Optional<Response>> route=route(
				prefix == null ? "" : prefix,
				suffix == null ? "" : suffix,
				handler
		);

		if ( routes.putIfAbsent(path, route) != null ) {
			throw new IllegalStateException("path already mapped <"+path+">");
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the handler for the OPTIONS HTTP method.
	 *
	 * @param handler the handler to be delegated for OPTIONS HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router options(final Handler handler) {
		return method(OPTIONS, handler);
	}


	/**
	 * Configures the handler for the HEAD HTTP method.
	 *
	 * @param handler the handler to be delegated for HEAD HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router head(final Handler handler) {
		return method(HEAD, handler);
	}

	/**
	 * Configures the handler for the GET HTTP method.
	 *
	 * @param handler the handler to be delegated for GET HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router get(final Handler handler) {
		return method(GET, handler);
	}


	/**
	 * Configures the handler for the POST HTTP method.
	 *
	 * @param handler the handler to be delegated for POST HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router post(final Handler handler) {
		return method(POST, handler);
	}

	/**
	 * Configures the handler for the PUT HTTP method.
	 *
	 * @param handler the handler to be delegated for HTTP PUT method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router put(final Handler handler) {
		return method(PUT, handler);
	}

	/**
	 * Configures the handler for the PATCH HTTP method.
	 *
	 * @param handler the handler to be delegated for PATCH HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router patch(final Handler handler) {
		return method(PATCH, handler);
	}

	/**
	 * Configures the handler for the DELETE HTTP method.
	 *
	 * @param handler the handler to be delegated for DELETE HTTP method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is null
	 */
	public Router delete(final Handler handler) {
		return method(DELETE, handler);
	}


	/**
	 * Configures the handler for a HTTP method.
	 *
	 * @param method  the HTTP method whose handler is to be configured
	 * @param handler the handler to be delegated for {@code method}
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if either {@code method} or {@code handler} is null
	 */
	public Router method(final String method, final Handler handler) {

		if ( method == null ) {
			throw new NullPointerException("null method");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		if ( method.equals(GET) ) {
			methods.putIfAbsent(HEAD, this::head);
		}

		methods.put(method, handler);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Response handle(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return routes.values().stream()

				.map(route -> route.apply(request))

				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()

				.orElseGet(() -> (

						methods.isEmpty() ? status(NotFound)
								: methods.getOrDefault(request.method(), this::options)

				).handle(request));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Function<Request, Optional<Response>> route(
			final String prefix, final String suffix, final Handler handler
	) {

		final Collection<String> keys=new LinkedHashSet<>();

		final Matcher scanner=KeyPattern.matcher(prefix.isEmpty() ? "" : Pattern.quote(prefix));

		final StringBuilder buffer=new StringBuilder(2*(prefix.length()+suffix.length())).append("(");

		while ( scanner.find() ) { // collect placeholder keys and replace with wildcard step patterns

			final String key=scanner.group("key");

			if ( !key.isEmpty() && !keys.add(key) ) {
				throw new IllegalArgumentException("repeated placeholder key <"+key+">");
			}

			scanner.appendReplacement(buffer, key.isEmpty()
					? "\\\\E[^/]*\\\\Q"
					: "\\\\E(?<${key}>[^/]*)\\\\Q"
			);

		}

		scanner.appendTail(buffer).append(suffix.isEmpty() ? ")" : ")(/.*)");

		final Pattern pattern=Pattern.compile(buffer.toString());

		return request -> {

			final String head=request.attribute(RoutingPrefix).orElse("");
			final String tail=request.path().substring(head.length());

			return Optional.of(pattern.matcher(tail))

					.filter(Matcher::matches)

					.map(matcher -> {

						keys.forEach(key -> {
							request.parameter(key, URLDecoder.decode(matcher.group(key), UTF_8));
						});

						return request.attribute(RoutingPrefix, head+matcher.group(1));

					})

					.map(handler::handle);

		};
	}


	private Response head(final Request request) {
		return handle(request.method(GET)).map(response -> response
				.headers("Content-Length", emptyList())
				.body(output(), target -> { })
		);
	}

	private Response options(final Request request) {
		return request.reply().map(response -> response
				.status(request.method().equals(OPTIONS) ? OK : MethodNotAllowed)
				.headers("Allow", Stream.of(Set.of(OPTIONS), methods.keySet())
						.flatMap(Collection::stream)
						.collect(toList())
				)
		);
	}

}
