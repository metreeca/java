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

package com.metreeca.http.toolkits;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Resource utilities.
 */
public final class Resources {

    /**
     * Locates a network resource.
     *
     * @param resource the URL of the resource to be located
     *
     * @return the URL of the located {@code resource}
     *
     * @throws NullPointerException if {@code resource} is null
     * @throws IllegalArgumentException if {@code resource} is malformed
     */
    public static URL resource(final String resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        try {

            return new URL(resource);

        } catch ( final MalformedURLException e ) {

            throw new IllegalArgumentException(e.getMessage());

        }
    }

    /**
     * Locates a class resource.
     *
     * @param master   the target class or an instance of the target class for the resource to be located
     * @param resource the path of the resource to be located, relative to the target class
     *
     * @return the URL of the located {@code resource}
     *
     * @throws NullPointerException if either {@code master} or {@code resource} is null
     * @throws MissingResourceException if {@code resource} is not available
     */
    public static URL resource(final Object master, final String resource) {

        if ( master == null ) {
            throw new NullPointerException("null master");
        }

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        final Class<?> clazz=master instanceof Class ? (Class<?>)master : master.getClass();

        final URL url=clazz.getResource(resource.startsWith(".") ? clazz.getSimpleName()+resource : resource);

        if ( url == null ) {
            throw new MissingResourceException(
                    format("unknown resource <%s:%s>", clazz.getName(), resource),
                    clazz.getName(),
                    resource
            );
        }

        return url;
    }


    /**
     * Retrieves the input stream for a resource.
     *
     * @param resource the resource to be retrieved
     * @return the input stream for the given {@code resource}
     *
     * @throws NullPointerException if {@code resource} is null
     */
    public static InputStream input(final URL resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        try {

            final InputStream input=resource.openStream();

            return resource.toExternalForm().endsWith(".gz") ? new GZIPInputStream(input) : input;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Retrieves the reader for a resource.
     *
     * @param resource the resource to be retrieved
     *
     * @return the {@code UTF-8} reader for the given {@code resource}
     *
     * @throws NullPointerException if {@code resource} is null
     */
    public static Reader reader(final URL resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        return new InputStreamReader(input(resource), UTF_8);
    }

    /**
     * Retrieves the textual content of a resource.
     *
     * @param resource the resource to be retrieved
     *
     * @return the textual content of the given {@code resource}, read using the {@code UTF-8} charset
     *
     * @throws NullPointerException if {@code resource} is null
     */
    public static String text(final URL resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        try ( final Reader reader=reader(resource) ) {

            return Feeds.text(reader);

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Retrieves the binary content of a resource.
     *
     * @param resource the resource to be retrieved
     *
     * @return the binary content of the given {@code resource}
     *
     * @throws NullPointerException if {@code resource} is null
     */
    public static byte[] data(final URL resource) {

        if ( resource == null ) {
            throw new NullPointerException("null resource");
        }

        try (
                final InputStream input=input(resource);
                final ByteArrayOutputStream output=new ByteArrayOutputStream()
        ) {

            return Feeds.data(output, input).toByteArray();

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Resources() { }

}
