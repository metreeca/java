/*
 * Copyright © 2013-2024 Metreeca srl
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
import com.metreeca.http.formats.Data;
import com.metreeca.http.services.Fetcher.URLFetcher;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Request.GET;
import static com.metreeca.http.Request.HEAD;
import static com.metreeca.http.Response.NotModified;
import static com.metreeca.http.Response.OK;
import static com.metreeca.http.toolkits.Resources.input;
import static com.metreeca.http.toolkits.Resources.resource;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toMap;

/**
 * Static content publisher.
 */
public final class Publisher extends Delegator {

    private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");

    /**
     * MIME types by file extension (including dot).
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">
     * Common MIME types @ MDN</a>
     */
    private static final Map<String, String> MIMETypes=unmodifiableMap(Stream

            .of(input(resource(Publisher.class, ".tsv")))

            .flatMap(stream -> new BufferedReader(new InputStreamReader(stream, UTF_8)).lines())

            .filter(line -> !line.isEmpty())

            .map(line -> {

                final int tab=line.indexOf('\t');

                return Map.entry(line.substring(0, tab), line.substring(tab+1));

            })

            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))

    );


    /**
     * Computes HTML variants of a URL.
     *
     * <p>Generate alternative paths appending {@code .html}/{index.html} suffixes as suggested by the path, e.g.:</p>
     *
     * <ul>
     *
     *     <li>{@code …/path/} ›› {@code …/path/}, {@code …/path/index.html}</li>
     *     <li>{@code …/path/file} ›› {@code …/path/file}, {@code …/path/file.html}</li>
     *
     * </ul>
     *
     * <p>URL anchors ({@code #<anchors>} are properly preserved.</p>
     *
     * @param url the url whose variants are to be computed
     *
     * @return a stream of url variants
     *
     * @throws NullPointerException if {@code url} is null
     */
    public static Stream<String> variants(final String url) {

        if ( url == null ) {
            throw new NullPointerException("null url");
        }

        final Matcher matcher=URLPattern.matcher(url);

        if ( matcher.matches() ) {

            final String head=Optional.ofNullable(matcher.group(1)).orElse("");
            final String file=Optional.ofNullable(matcher.group(2)).orElse("");
            final String hash=Optional.ofNullable(matcher.group(3)).orElse("");

            return file.isEmpty() || file.equals(".") ? Stream.of(head+"index.html"+hash)
                    : file.endsWith(".html") ? Stream.of(head+file+hash)
                    : Stream.of(head+file+hash, head+file+".html"+hash);

        } else {

            return Stream.of(url);

        }
    }

    /**
     * Guess the MIME type of resource path.
     *
     * @param path the path of the resource whose MIME type is to be guessed
     *
     * @return the well-known MIME type associated with the extension of the {@code path} filename or
     * {@value Data#MIME}, if {@code path} doesn't include an extension or no well-known MIME type is defined
     *
     * @throws NullPointerException if {@code path} is null
     */
    public static String mime(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        final int slash=max(0, path.lastIndexOf('/'));
        final int dot=path.substring(slash).lastIndexOf('.');

        final String extension=dot >= 0 ? path.substring(slash+dot).toLowerCase(ROOT) : "";

        return MIMETypes.getOrDefault(extension, Data.MIME);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Path assets;
    private String fallback;

    private final Function<Request, Response> fetcher=new URLFetcher();


    public Publisher() {
        delegate(handler(
                (Handler)this::assets,
                (Handler)this::fallback
        ));
    }


    /**
     * Configures the static content root.
     *
     * @param root the root path of the content to be published
     *
     * @return this publisher
     *
     * @throws NullPointerException if {@code root} is null
     */
    public Publisher assets(final Path root) {

        if ( root == null ) {
            throw new NullPointerException("null root");
        }

        this.assets=root;

        return this;
    }

    /**
     * Configures the static content root.
     *
     * <p><strong>Warning</strong> / Only {@code file:} and {@code jar:} URLs are currently supported.</p>
     *
     * @param root the root URL of the content to be published (e.g. as returned by {@link Class#getResource(String)})
     *
     * @return this publisher
     *
     * @throws NullPointerException          if {@code root} is null
     * @throws IllegalArgumentException      if {@code root} is malformed
     * @throws UnsupportedOperationException if the {@code root} scheme is not supported
     */
    public Publisher assets(final URL root) {

        if ( root == null ) {
            throw new NullPointerException("null root");
        }

        final String scheme=root.getProtocol();

        if ( scheme.equals("file") ) {

            try {

                this.assets=Paths.get(root.toURI());

            } catch ( final URISyntaxException e ) {

                throw new IllegalArgumentException(e);

            }

        } else if ( scheme.equals("jar") ) {

            final String path=root.toString();
            final int separator=path.indexOf("!/");

            final String jar=path.substring(0, separator);
            final String entry=path.substring(separator+1);

            // load the filesystem from the service locator to have it automatically closed
            // !!! won't handle multiple publishers from the same filesystem

            final FileSystem filesystem=service(() -> {
                try {
                    return FileSystems.newFileSystem(URI.create(jar), emptyMap());
                } catch ( final IOException e ) {
                    throw new UncheckedIOException(e);
                }
            });

            this.assets=filesystem.getPath(entry);

        } else {

            throw new UnsupportedOperationException(format("unsupported URL scheme <%s>", root));

        }

        return this;
    }


    /**
     * Configures the fallback content for {@linkplain Request#route()} requests.
     *
     * @param fallback the absolute path of the fallback content
     *
     * @return this publisher
     *
     * @throws NullPointerException     if {@code fallback} is null
     * @throws IllegalArgumentException if {@code fallback} is not an absolute path
     */
    public Publisher fallback(final String fallback) {

        if ( fallback == null ) {
            throw new NullPointerException("null fallback path");
        }

        if ( !fallback.startsWith("/") ) {
            throw new NullPointerException("relative fallback path");
        }

        this.fallback=fallback;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Response assets(final Request request, final Function<Request, Response> forward) { // !!! handle HEAD
        if ( assets != null && request.method().equals(GET) && (fallback == null || !request.route()) ) {

            return variants(request.path())

                    .map(variant -> assets.getRoot().relativize(assets.getFileSystem().getPath(variant)))
                    .map(assets::resolve)
                    .map(Path::normalize) // prevent tree walking attacks

                    .filter(Files::exists)
                    .filter(Files::isRegularFile)

                    .findFirst()

                    .map(file -> request.reply().map(response -> {

                        try {

                            final String mime=mime(file.getFileName().toString());
                            final String length=String.valueOf(Files.size(file));
                            final String etag=format("\"%s\"", Files.getLastModifiedTime(file).toMillis());

                            return request.headers("If-None-Match").anyMatch(etag::equals)

                                    ? response.status(NotModified)

                                    : request.method().equals(HEAD)

                                    ? response.status(OK)
                                    .header("Content-Type", mime)
                                    .header("ETag", etag)

                                    : response.status(OK)
                                    .header("Content-Type", mime)
                                    .header("Content-Length", length)
                                    .header("ETag", etag)
                                    .output(output -> {
                                        try {
                                            Files.copy(file, output);
                                        } catch ( final IOException e ) {
                                            throw new UncheckedIOException(e);
                                        }
                                    });

                        } catch ( final IOException e ) {

                            throw new UncheckedIOException(e);

                        }

                    }))

                    .orElseGet(() -> forward.apply(request));

        } else {

            return forward.apply(request);

        }
    }

    private Response fallback(final Request request, final Function<Request, Response> forward) { // !!! handle HEAD
        if ( fallback != null && request.method().equals(GET) && request.route() && !request.path().equals(fallback) ) {

            // retrieve fallback with a new request to support static content delivery on dedicated server

            return new Request()

                    .method(request.method())
                    .base(request.base())
                    .path(fallback)

                    .headers(request.headers())

                    // disable conditional requests

                    .header("If-None-Match", "")
                    .header("If-Modified-Since", "")

                    .map(fetcher)

                    // convert incoming into outgoing response

                    .map(response -> request.reply(response.status())

                            .header("Content-Type", response.header("Content-Type").orElse(""))
                            .header("Cache-Control", "no-store") // don't shadow future API responses

                            .output(output -> {

                                try ( final InputStream input=response.input().get() ) {

                                    input.transferTo(output);

                                } catch ( final IOException e ) {

                                    throw new UncheckedIOException(e);

                                }

                            })

                    );

        } else {

            return forward.apply(request);

        }
    }

}
