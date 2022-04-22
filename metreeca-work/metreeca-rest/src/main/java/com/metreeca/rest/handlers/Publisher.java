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

import com.metreeca.rest.Format;

import java.net.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.core.Lambdas.checked;
import static com.metreeca.http.Locator.service;
import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.Response.NotModified;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

/**
 * Static content publisher.
 */
public final class Publisher extends Delegator {

    private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");


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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a static content publisher.
     *
     * <p><strong>Warning</strong> / Only {@code file:} and {@code jar:} URLs are currently supported.</p>
     *
     * @param root the root URL of the content to be published (e.g. as returned by {@link Class#getResource(String)}) *
     *
     * @throws NullPointerException     if {@code root} is null
     * @throws IllegalArgumentException if {@code root} is malformed
     */
    public Publisher(final URL root) {

        if ( root == null ) {
            throw new NullPointerException("null root");
        }

        final String scheme=root.getProtocol();

        if ( scheme.equals("file") ) {

            try {

                publish(Paths.get(root.toURI()));

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

            final FileSystem filesystem=service(checked(() ->
                    FileSystems.newFileSystem(URI.create(jar), emptyMap())
            ));

            publish(filesystem.getPath(entry));

        } else {

            throw new UnsupportedOperationException(format("unsupported URL scheme <%s>", root));

        }
    }

    /**
     * Creates a static content publisher.
     *
     * @param root the root path of the content to be published
     *
     * @throws NullPointerException if {@code root} is null
     */
    public Publisher(final Path root) {

        if ( root == null ) {
            throw new NullPointerException("null root");
        }

        publish(root);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void publish(final Path root) {
        delegate(new Router()

                .get((request, forward) -> variants(request.path())

                        .map(variant -> root.getRoot().relativize(root.getFileSystem().getPath(variant)))
                        .map(root::resolve)
                        .map(Path::normalize) // prevent tree walking attacks

                        .filter(Files::exists)
                        .filter(Files::isRegularFile)

                        .findFirst()

                        .map(file -> request.reply().map(checked(response -> {

                            final String mime=Format.mime(file.getFileName().toString());
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
                                    .body(output(), checked(output -> { Files.copy(file, output); }));

                        })))

                        .orElseGet(() -> forward.apply(request))

                )

        );
    }

}