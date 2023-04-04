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

package com.metreeca.http.services;

import com.metreeca.core.services.Cache;
import com.metreeca.core.services.Logger;
import com.metreeca.http.Request;
import com.metreeca.http.Response;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.core.toolkits.Feeds.data;
import static com.metreeca.http.Request.GET;
import static com.metreeca.http.Response.MethodNotAllowed;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.String.format;


/**
 * Resource fetcher.
 *
 * <p>Fetches external resources through system-specific facilities.</p>
 */
@FunctionalInterface public interface Fetcher extends Function<Request, Response> {

    /**
     * Retrieves the default resource fetcher factory.
     *
     * @return the default resource fetcher factory, which creates {@link URLFetcher} instances
     */
    public static Supplier<Fetcher> fetcher() {
        return URLFetcher::new;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * URL resource fetcher.
     *
     * <p>Fetches external resources through the {@link URL#openConnection()} API.</p>
     */
    public static class URLFetcher implements Fetcher {

        private final Logger logger=service(logger());


        @Override public Response apply(final Request request) {
            switch ( request.item().substring(0, max(0, request.item().indexOf(':'))) ) {

                case "http":
                case "https":

                    return http(request);

                default:

                    return wild(request);

            }
        }


        private Response http(final Request request) { // !!! refactor
            try {

                final String method=request.method();
                final String resource=request.resource();

                logger.info(this, format("%s %s", method, resource));

                final HttpURLConnection connection=(HttpURLConnection)new URL(resource).openConnection();

                connection.setRequestMethod(method);
                connection.setDoOutput(method.equals(Request.POST) || method.equals(Request.PUT));

                connection.setInstanceFollowRedirects(true);

                // !!! connection.setConnectTimeout();
                // !!! connection.setReadTimeout();
                // !!! connection.setIfModifiedSince();

                if ( request.header("User-Agent").isEmpty() ) {
                    connection.addRequestProperty("User-Agent", "metreeca/java (https://github.com/metreeca/java)");
                }

                if ( request.header("Accept-Encoding").isEmpty() ) {
                    connection.addRequestProperty("Accept-Encoding", "gzip");
                }

                if ( request.header("Accept").isEmpty() ) {
                    connection.addRequestProperty("Accept", "text/html,"
                            +"application/xhtml+xml,"
                            +"application/xml;q=0.9;"
                            +"*/*;q=0.1"
                    );
                }

                request.headers().forEach(connection::addRequestProperty);

                connection.connect();

                if ( connection.getDoOutput() ) {

                    try (
                            final InputStream input=request.input().get();
                            final OutputStream output=connection.getOutputStream()
                    ) {

                        data(output, input);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                }

                final boolean head=connection.getRequestMethod().equalsIgnoreCase(Request.HEAD);
                final int code=connection.getResponseCode(); // !!! handle http > https redirection
                final String encoding=connection.getContentEncoding();

                return request.reply(min(max(100, code), 599)) // harden against illegal codes

                        .map(response -> {

                            connection.getHeaderFields().forEach((name, values) -> {

                                // ;( name may use null to hold status line

                                if ( name != null && !name.equalsIgnoreCase("Set-Cookie") ) {
                                    response.headers(name, values);
                                }

                            });

                            return response;
                        })

                        .input(() -> {
                            try {

                                return Optional

                                        .ofNullable(head ? null
                                                : code/100 == 2 ? connection.getInputStream()
                                                : connection.getErrorStream()
                                        )

                                        .map(input -> {
                                            try {

                                                return "gzip".equals(encoding)
                                                        ? new GZIPInputStream(input)
                                                        : input;

                                            } catch ( final IOException e ) {
                                                throw new UncheckedIOException(e);
                                            }
                                        })

                                        .orElseGet(InputStream::nullInputStream);

                            } catch ( final IOException e ) {
                                throw new UncheckedIOException(e);
                            }
                        });

            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }
        }

        private Response wild(final Request request) {

            final String method=request.method();
            final String resource=request.resource();

            if ( method.equals(GET) ) {

                logger.info(this, format("%s %s", method, resource));

                return new Response(request)

                        .status(Response.OK)

                        .input(() -> {
                            try {

                                final InputStream input=new URL(resource).openStream();

                                return resource.endsWith(".gz")
                                        ? new GZIPInputStream(input)
                                        : input;

                            } catch ( final IOException e ) {
                                throw new UncheckedIOException(e);
                            }
                        });

            } else {

                return request.reply(MethodNotAllowed);

            }
        }

    }

    /**
     * Caching resource fetcher.
     *
     * <p>Caches resources fetched by a delegate fetcher.</p>
     */
    public static final class CacheFetcher implements Fetcher { // !!! check caching headers

        private Fetcher delegate=service(fetcher(), fetcher());
        private Cache cache=service(Cache.cache());


        /**
         * Configures the delegate for this fetcher (defaults to the {@linkplain #fetcher() shared fetcher service}).
         *
         * @param delegate the delegate for this fetcher
         *
         * @return this fetcher
         *
         * @throws NullPointerException if {@code delegate} is null
         */
        public CacheFetcher delegate(final Fetcher delegate) {

            if ( delegate == null ) {
                throw new NullPointerException("null delegate");
            }

            this.delegate=delegate;

            return this;
        }

        /**
         * Configures the cache for this fetcher (defaults to the {@link Cache#cache() shared cache}).
         *
         * @param cache the cache for this fetcher
         *
         * @return this fetcher
         *
         * @throws NullPointerException if {@code cache} is null
         */
        public CacheFetcher cache(final Cache cache) {

            if ( cache == null ) {
                throw new NullPointerException("null cache");
            }

            this.cache=cache;

            return this;
        }


        @Override public Response apply(final Request request) {
            return request.safe() ? cache.retrieve(

                    format("%s %s", request.method(), request.resource()),

                    input -> decode(request, input),
                    output -> encode(delegate.apply(request), output)

            ) : delegate.apply(request);
        }


        private Response encode(final Response response, final OutputStream output) {
            if ( response.success() ) {

                try (
                        final InputStream input=response.input().get();
                        final ObjectOutputStream serialized=new ObjectOutputStream(output)
                ) {

                    final byte[] data=data(input);

                    serialized.writeInt(response.status());
                    serialized.writeObject(response.headers());
                    serialized.writeObject(data);
                    serialized.flush();

                    return response.input(() -> new ByteArrayInputStream(data));

                } catch ( final IOException e ) {

                    throw new UncheckedIOException(e);

                }

            } else {
                return response;
            }
        }

        @SuppressWarnings("unchecked") private Response decode(final Request request, final InputStream input) {
            try ( final ObjectInputStream serialized=new ObjectInputStream(input) ) {

                final int status=serialized.readInt();
                final Map<String, String> headers=(Map<String, String>)serialized.readObject();
                final byte[] body=(byte[])serialized.readObject();

                return new Response(request)
                        .status(status)
                        .headers(headers)
                        .input(() -> new ByteArrayInputStream(body));

            } catch ( final ClassNotFoundException unexpected ) {

                throw new RuntimeException(unexpected);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }
        }

    }

}
