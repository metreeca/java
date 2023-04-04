/*
 * Copyright © 2020-2022 EC2U Alliance
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

package com.metreeca.csv.formats;

import com.metreeca.http.*;

import org.apache.commons.csv.*;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.metreeca.http.Response.BadRequest;

/**
 * CSV message format.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc4180">RFC 4180 - Common Format and MIME Type for Comma-Separated
 * Values (CSV) Files</a>
 */
public final class CSV implements Format<List<CSVRecord>> {

    /**
     * The default MIME type for CSV messages ({@value}).
     */
    public static final String MIME="text/csv";


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final CSVFormat format;


    public CSV(final CSVFormat format) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        this.format=format;
    }


    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<List<CSVRecord>> type() {
        return (Class<List<CSVRecord>>)(Object)List.class;
    }


    /**
     * @return the CSV payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty optional if
     * the {@code "Content-Type"} {@code message} header is not empty and is not equal to {@value MIME}
     */
    @Override public Optional<List<CSVRecord>> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(MIME))
                .filter(MIME::equals)

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset());
                            final CSVParser parser=format.parse(reader)
                    ) {

                        return parser.getRecords();

                    } catch ( final UnsupportedEncodingException e ) {

                        throw new FormatException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });

    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the CSV {@code
     * value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final List<CSVRecord> value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {
                    try (
                            final Writer writer=new OutputStreamWriter(output, message.charset());
                            final CSVPrinter printer=format.print(writer);
                    ) {


                        Optional.ofNullable(format.getHeader())
                                .filter(strings -> strings.length > 0)
                                .ifPresent((strings -> {

                                    try {
                                        printer.printRecord((Object[])strings);
                                    } catch ( final IOException e ) {
                                        throw new UncheckedIOException(e);
                                    }

                                }));

                        printer.printRecords(value);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }
                });
    }

}
