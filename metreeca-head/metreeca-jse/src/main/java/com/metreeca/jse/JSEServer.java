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

package com.metreeca.jse;

import com.metreeca.http.*;
import com.metreeca.http.handlers.Server;
import com.metreeca.http.services.Logger;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.NotFound;
import static com.metreeca.http.services.Logger.logger;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * Java SE HTTP server connector.
 *
 * <p>Connects web applications managed by a native Java SE {@linkplain HttpServer HTTP server} with
 * resource handlers based on the Metreeca/Java framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the service {@linkplain Locator toolbox} managing shared services required by resource
 * handlers;
 * </li>
 *
 * <li>handles HTTP requests using a {@linkplain Handler handler} loaded from the service locator.</li>
 *
 * </ul>
 */
public final class JSEServer {

    private static final Pattern ContextPattern=Pattern.compile(
            "(?<base>(?:\\w+://[^/?#]*)?)(?<path>.*)"
    );


    private static Supplier<Handler> delegate() { return () -> (request, next) -> request.reply().map(identity()); }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private InetSocketAddress address=new InetSocketAddress(Optional.ofNullable(System.getenv("PORT"))
            .map(Integer::parseInt) // abort on exception
            .orElse(8080)
    );

    private String base="";
    private String path="/";

    private final Locator locator=new Locator();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Configures the socket address.
     *
     * @param address the socket address to listen to
     *
     * @return this server
     *
     * @throws NullPointerException if {@code address} is null
     */
    public JSEServer address(final InetSocketAddress address) {

        if ( address == null ) {
            throw new NullPointerException("null address");
        }

        this.address=address;

        return this;
    }

    /**
     * Configures the context.
     *
     * @param context the context IRI for the root resource of this server; accepts root-relative paths
     *
     * @return this server
     *
     * @throws NullPointerException     if {@code context} is null
     * @throws IllegalArgumentException if {@code context} is malformed
     */
    public JSEServer context(final String context) {

        if ( context == null ) {
            throw new NullPointerException("null context");
        }

        final Matcher matcher=ContextPattern.matcher(context);

        if ( !matcher.matches() ) {
            throw new IllegalArgumentException(format("malformed context IRI <%s>", context));
        }

        this.base=matcher.group("base");

        this.path=Optional
                .of(matcher.group("path"))
                .map(p -> p.startsWith("/") ? p : "/"+p)
                .map(p -> p.endsWith("/") ? p : p+"/")
                .get();

        return this;
    }

    /**
     * Configures the delegate handler factory.
     *
     * @param factory the delegate handler factory; takes as argument a shared service manager (which may configured with
     *                additional application-specific services as a side effect) and must return a non-null handler to be
     *                used as entry point for serving requests
     *
     * @return this server
     *
     * @throws NullPointerException if {@code factory} is null or returns a null value
     */
    public JSEServer delegate(final Function<Locator, Handler> factory) {

        if ( factory == null ) {
            throw new NullPointerException("null handler factory");
        }

        locator.set(delegate(), () -> handler(
                new Server(),
                requireNonNull(factory.apply(locator), "null handler")
        ));

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void start() {
        try {

            final Handler handler=locator.get(delegate());
            final Logger logger=locator.get(logger());

            final HttpServer server=HttpServer.create(address, 0);

            server.setExecutor(Executors.newCachedThreadPool());

            server.createContext(path, exchange -> {
                try {

                    response(exchange, handler.handle(request(exchange), Request::reply).map(response ->
                            response.status() > 0 ? response : response.status(NotFound)
                    ));

                } catch ( final RuntimeException e ) {

                    if ( !e.toString().toLowerCase(Locale.ROOT).contains("broken pipe") ) {
                        logger.error(this, "unhandled exception", e);
                    }

                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                logger.info(this, "server stopping");

                try { server.stop(0); } catch ( final RuntimeException e ) {
                    logger.error(this, "unhandled exception while stopping server", e);
                }

                try { locator.clear(); } catch ( final RuntimeException e ) {
                    logger.error(this, "unhandled exception while releasing resources", e);
                }

                logger.info(this, "server stopped");

            }));

            logger.info(this, "server starting");

            server.start();

            logger.info(this, format("server listening at <http://%s:%d%s>", // !!! protocol?
                    address.getHostString().replace("0.0.0.0", "localhost"), address.getPort(), path
            ));

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Request request(final HttpExchange exchange) {

        final String origin=format("http://%s:%d", // !!! protocol?
                exchange.getLocalAddress().getHostString(),
                exchange.getLocalAddress().getPort()
        );

        final URI uri=exchange.getRequestURI();
        final Headers headers=exchange.getRequestHeaders();

        return new Request()

                .method(exchange.getRequestMethod())

                .base((base.isEmpty() ? origin : base)+path)
                .path(Optional.ofNullable(uri.getPath()).orElse("/").substring(path.length()-1))

                .query(Optional.ofNullable(uri.getRawQuery()).orElse(""))

                .map(request -> {

                    headers.forEach((name, values) -> {

                        if ( name != null && values != null ) {  // ;( possibly null header names…
                            request.headers(name, values);
                        }

                    });

                    return request;

                })

                .input(exchange::getRequestBody);
    }

    private void response(final HttpExchange exchange, final Response response) {
        try {

            final Headers headers=exchange.getResponseHeaders();

            response.headers().entrySet().stream() // Content-Length is generated by server
                    .filter(entry -> !entry.getKey().equalsIgnoreCase("Content-Length"))
                    .forEachOrdered(entry -> headers.put(entry.getKey(), List.of(entry.getValue())));

            response.header("Content-Type").ifPresentOrElse(

                    type -> {
                        try ( final OutputStream output=exchange.getResponseBody() ) {

                            exchange.sendResponseHeaders(response.status(), response.header("Content-Length")
                                    .map(Long::parseUnsignedLong)
                                    .orElse(0L) // chunked
                            );

                            response.output().accept(output);

                        } catch ( final IOException e ) {
                            throw new UncheckedIOException(e);
                        }
                    },

                    () -> {
                        try {

                            exchange.sendResponseHeaders(response.status(), -1L); // no output

                        } catch ( final IOException e ) {
                            throw new UncheckedIOException(e);
                        }
                    }

            );

        } finally {

            exchange.close();

        }
    }

}
