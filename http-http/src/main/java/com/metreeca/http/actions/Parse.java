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

package com.metreeca.http.actions;

import com.metreeca.core.services.Logger;
import com.metreeca.http.*;

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.Locator.service;

import static java.lang.String.format;


/**
 * Message body parsing.
 *
 * <p>Extracts a specific body representation from a message.</p>
 *
 * @param <R> the type of the message body to be extracted
 */
public final class Parse<R> implements Function<Message<?>, Optional<R>> {

    private final Format<R> format;

    private final Logger logger=service(Logger.logger());


    /**
     * Creates a new message body parser.
     *
     * @param format the format for the message body to be extracted
     *
     * @throws NullPointerException if {@code format} is null
     */
    public Parse(final Format<R> format) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        this.format=format;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses a message body representation.
     *
     * @param message the message whose body representation is to be parsed
     *
     * @return an optional body representation of the required format, if {@code message} was not null and its body
     * representation successfully pased; an empty optional, otherwise, logging an error to the
     * {@linkplain Logger#logger() shared event logger}
     */
    @Override public Optional<R> apply(final Message<?> message) {

        if ( message == null ) { return Optional.empty(); } else {

            try {

                final Optional<R> value=format.decode(message);

                if ( value.isEmpty() ) {

                    logger.warning(this,
                            format("no <%s> message body", format.getClass().getSimpleName())
                    );

                }

                return value;


            } catch ( final FormatException error ) {

                // !!! review formatting >> avoid newlines in log

                logger.error(this,
                        format("unable to parse message body as <%s>", format.getClass().getSimpleName()),
                        new RuntimeException(error.toString(), error)
                );

                return Optional.empty();


            } catch ( final UncheckedIOException e ) {

                // !!! review formatting >> avoid newlines in log

                logger.error(this,
                        "unable to read message body",
                        new RuntimeException(e.toString(), e)
                );

                return Optional.empty();

            }
        }

    }

}
