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

package com.metreeca.core;

import java.io.*;

/**
 * I/O Utilities.
 */
public final class Feeds {

    /**
     * Reads data from an input stream.
     *
     * @param input the input stream to be read
     *
     * @return the data content read from {@code input}
     *
     * @throws NullPointerException if {@code input} is null
     */
    public static byte[] data(final InputStream input) {

        if ( input == null ) {
            throw new NullPointerException("null input");
        }

        try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

            return data(output, input).toByteArray();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    /**
     * Writes data to an output stream.
     *
     * @param <O>    the type of the output stream
     * @param output the output stream
     * @param data   the data content to be written to {@code output}
     *
     * @return the {@code output} stream
     *
     * @throws NullPointerException if either {@code output} or {@code data} is null
     */
    public static <O extends OutputStream> O data(final O output, final byte[] data) {

        if ( output == null ) {
            throw new NullPointerException("null output");
        }

        if ( data == null ) {
            throw new NullPointerException("null data");
        }

        try {

            output.write(data);
            output.flush();

            return output;

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    /**
     * Copies an input stream to an output stream.
     *
     * @param <O>    the type of the output stream
     * @param output the output stream
     * @param input  the input stream
     *
     * @return the {@code output} stream
     *
     * @throws NullPointerException if either {@code output} ir {@code input} is null
     */
    public static <O extends OutputStream> O data(final O output, final InputStream input) {

        if ( output == null ) {
            throw new NullPointerException("null output");
        }

        if ( input == null ) {
            throw new NullPointerException("null input");
        }

        try {

            final byte[] buffer=new byte[1024];

            for (int n; (n=input.read(buffer)) >= 0; output.write(buffer, 0, n)) { }

            output.flush();

            return output;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Reads text from a reader.
     *
     * @param reader the reader to be read
     *
     * @return the text content read from {@code reader}
     *
     * @throws NullPointerException if {@code reader} is null
     */
    public static String text(final Reader reader) {

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        try ( final StringWriter writer=new StringWriter() ) {

            return text(writer, reader).toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    /**
     * Writes text to writer.
     *
     * @param <W>    the type of the writer
     * @param writer the writer
     * @param text   the text content to be written to {@code writer}
     *
     * @return the {@code writer}
     *
     * @throws NullPointerException if either {@code writer} or {@code text} is null
     */
    public static <W extends Writer> W text(final W writer, final String text) {

        if ( writer == null ) {
            throw new NullPointerException("null writer");
        }

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        try {

            writer.write(text);
            writer.flush();

            return writer;

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    /**
     * Copies a reader to writer.
     *
     * @param <W>    the type of the writer
     * @param writer the writer
     * @param reader the reader
     *
     * @return the {@code writer}
     *
     * @throws NullPointerException if either {@code writer} or {@code reader} is null
     */
    public static <W extends Writer> W text(final W writer, final Reader reader) {

        if ( writer == null ) {
            throw new NullPointerException("null writer");
        }

        if ( reader == null ) {
            throw new NullPointerException("null reader");
        }

        try {

            final char[] buffer=new char[1024];

            for (int n; (n=reader.read(buffer)) >= 0; writer.write(buffer, 0, n)) { }

            writer.flush();

            return writer;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Feeds() { }

}
