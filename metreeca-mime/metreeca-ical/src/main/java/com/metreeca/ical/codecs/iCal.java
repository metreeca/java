/*
 * Copyright © 2021-2022 EC2U Consortium
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

package com.metreeca.ical.codecs;

import com.metreeca.http.*;

import net.fortuna.ical4j.data.*;
import net.fortuna.ical4j.model.Calendar;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * iCalendar message format.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc5545.html">RFC 5545 - Internet Calendaring and Scheduling Core Object
 * Specification (iCalendar)</a>
 */
public final class iCal implements Codec<Calendar> {

    /**
     * The default MIME type for iCalendar message bodies ({@value}).
     */
    public static final String MIME="text/calendar";

    /**
     * A pattern matching iCalendar-based MIME types.
     */
    public static final Pattern MIMEPattern=Pattern.compile(
            "(?i:^(text/calendar)(?:\\s*;.*)?$)"
    );


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return the default MIME type for XML messages ({@value MIME})
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<Calendar> type() {
        return Calendar.class;
    }


    /**
     * @return the iCalendar payload decoded from the raw {@code message} {@linkplain Message#input()} or an empty
     * optional if the {@code "Content-Type"} {@code message} header is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<Calendar> decode(final Message<?> message) throws CodecException, UncheckedIOException {
        return message

                .header("Content-Type")

                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset())
                    ) {

                        return new CalendarBuilder().build(input);

                    } catch ( final UnsupportedEncodingException|ParserException e ) {

                        throw new CodecException(Response.BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the iCalendar
     * {@code value}
     */

    @Override public <M extends Message<M>> M encode(final M message, final Calendar value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {
                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        new CalendarOutputter().output(value, writer);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }
                });
    }

}
