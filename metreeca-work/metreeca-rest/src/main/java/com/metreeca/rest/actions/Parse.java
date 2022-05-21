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

package com.metreeca.rest.actions;

import com.metreeca.http.*;
import com.metreeca.http.services.Logger;

import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;

import static java.lang.String.format;


/**
 * Message body parsing.
 *
 * <p>Extracts a specific body representation from a message.</p>
 *
 * @param <R> the type of the message body to be extracted
 */
public final class Parse<R> implements Function<Message<?>, Optional<R>> {

    private final Codec<R> codec;

    private final Logger logger=service(Logger.logger());


    /**
     * Creates a new message body parser.
     *
     * @param codec the codec for the message body to be extracted
     *
     * @throws NullPointerException if {@code codec} is null
     */
    public Parse(final Codec<R> codec) {

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        this.codec=codec;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses a message body representation.
     *
     * @param message the message whose body representation is to be parsed
     *
     * @return an optional body representation of the required format, if {@code message} was not null and its body
     * representation successfully pased; an empty optional, otherwise, logging an error to the {@linkplain
     * Logger#logger() shared event logger}
     */
    @Override public Optional<R> apply(final Message<?> message) {

        if ( message == null ) { return Optional.empty(); } else {

            try {

                final Optional<R> value=codec.decode(message);

                if ( value.isEmpty() ) {

                    logger.warning(this,
                            format("no <%s> message body", codec.getClass().getSimpleName())
                    );

                }

                return value;


            } catch ( final CodecException error ) {

                // !!! review formatting >> avoid newlines in log

                logger.error(this,
                        format("unable to parse message body as <%s>", codec.getClass().getSimpleName()),
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
