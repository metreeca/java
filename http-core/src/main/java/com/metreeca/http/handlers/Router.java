/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.http.handlers;


import com.metreeca.http.Handler;
import com.metreeca.http.Request;
import com.metreeca.http.Response;

import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;


/**
 * Path-based request dispatcher.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP
 * {@linkplain Request#path() path}, ignoring the leading segment possibly already matched by wrapping routers.</p>
 *
 * <p>Requests are forwarded to a {@linkplain #path(String, Handler) registered} handler if their path is
 * matched (in order of definition) by an associated pattern defined by a sequence of steps according to the following
 * rules:</p>
 *
 * <ul>
 *      <li>the empty step ({@code /}) matches only an empty step ({@code /});</li>
 *
 *      <li>literal steps {@code /<steli>} match path steps verbatim;</li>
 *
 *      <li>wildcard steps {@code /{}} match a single path step;</li>
 *
 *      <li>placeholder steps {@code /{<key>}} match a single path step, adding the matched {@code <key>}/{@code
 *      <steli>} entry to request {@linkplain Request#parameters() parameters}; the matched {@code <steli>} name is
 *      URL-decoded before use;</li>
 *
 *      <li>prefix steps {@code /*} match one or more trailing path steps.</li>
 * </ul>
 *
 * <p>If no matching path is found, {@linkplain Handler#handle(Request, Function) forwards} the request to the tail of
 * the handling pipeline.</p>
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

    private final Map<String, Handler> routes=new LinkedHashMap<>();


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

        final Handler route=route(
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

    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        if ( request == null ) {
            throw new NullPointerException("null request");
        }

        return routes.values().stream()

                .map(route -> route.handle(request, forward))

                .filter(Objects::nonNull)
                .findFirst()

                .orElseGet(() -> forward.apply(request)); // fall through
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Handler route(
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

        return (request, forward) -> {

            final String head=request.attribute(RoutingPrefix).orElse("");
            final String tail=request.path().substring(head.length());

            return Optional.of(pattern.matcher(tail))

                    .filter(Matcher::matches)

                    .map(matcher -> {

                        keys.forEach(key -> request.parameter(key, URLDecoder.decode(matcher.group(key), UTF_8)));

                        return handler.handle(request.attribute(RoutingPrefix, head+matcher.group(1)), forward);

                    })

                    .orElse(null);

        };
    }

}
