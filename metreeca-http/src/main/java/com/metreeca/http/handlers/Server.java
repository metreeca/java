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

package com.metreeca.http.handlers;

import com.metreeca.http.*;
import com.metreeca.http.codecs.Text;
import com.metreeca.http.services.Logger;

import java.io.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.core.Feeds.text;
import static com.metreeca.http.Locator.service;
import static com.metreeca.http.Request.*;
import static com.metreeca.http.Response.InternalServerError;
import static com.metreeca.http.services.Logger.Level.*;
import static com.metreeca.http.services.Logger.logger;

import static java.lang.String.format;


/**
 * API server.
 *
 * <p>Provides default resource pre/postprocessing and error handling; mainly intended to factor default server
 * connector functionalities.</p>
 */
public final class Server extends Delegator {

    private static final Pattern HostPattern=Pattern.compile("\\bhost\\s*=\\s*(?<host>[^;]+)");
    private static final Pattern ProtoPattern=Pattern.compile("\\bproto\\s*=\\s*(?<proto>[^;]+)");

    private static final Pattern TextualPattern=Pattern.compile("(?i:^text/.+|.+/.*\bjson$)");
    private static final Pattern URLEncodedPattern=Pattern.compile("application/x-www-form-urlencoded\\b");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Logger logger=service(logger());


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public Server() {
        delegate((request, forward) -> {
            try {

                return request

                        .map(this::base)
                        .map(this::query)
                        .map(this::form)

                        .map(forward)

                        .map(this::logging)
                        .map(this::charset);

            } catch ( final CodecException e ) {

                final String method=request.method();
                final String item=request.item();

                logger.entry(warning, this, () -> format("%s %s > %d", method, item, e.getStatus()), e);

                return e.getStatus() < 500
                        ? request.reply(e.getStatus()).body(new Text(), e.getMessage())
                        : request.reply(e.getStatus());

            } catch ( final RuntimeException e ) { // try to send a new response

                final String method=request.method();
                final String item=request.item();

                logger.entry(error, this, () -> format("%s %s > %d", method, item, InternalServerError), e);

                return request.reply(InternalServerError);


            }
        });
    }


    //// Pre-Processing ///////////////////////////////////////////////////////////////////////////////////////////////

    private Request base(final Request request) { // reconstruct public base from proxy forward headers

        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded

        final Optional<String> proto=request.header("Forwarded")

                .map(ProtoPattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group("proto"))

                .or(() -> request.header("X-Forwarded-Proto"));

        final Optional<String> host=request.header("Forwarded")

                .map(HostPattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group("host"))

                .or(() -> request.header("X-Forwarded-Host"));

        return proto

                .flatMap(_proto -> host.map(_host -> request.base(format("%s://%s/", _proto, _host))))

                .orElse(request);
    }

    private Request query(final Request request) { // parse parameters from query string, if not already set
        return request.parameters().isEmpty() && request.method().equals(GET)
                ? request.parameters(params(request.query()))
                : request;
    }

    private Request form(final Request request) { // parse parameters from encoded form body, ignoring charset
        if ( request.parameters().isEmpty()
                && request.method().equals(POST)
                && URLEncodedPattern.matcher(request.header("Content-Type").orElse("")).lookingAt()
        ) {

            try (
                    final InputStream input=request.input().get();
                    final Reader reader=new InputStreamReader(input, request.charset());
            ) {

                return request.parameters(params(text(reader)));

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }

        } else {

            return request;

        }
    }


    //// Post-Processing //////////////////////////////////////////////////////////////////////////////////////////////

    private Response logging(final Response response) { // log request outcome

        final Request request=response.request();
        final String method=request.method();
        final String item=request.item();

        final int status=response.status();

        final Logger.Level level=(status < 400) ? info
                : (status < 500) ? warning
                : error;

        logger.entry(level, this, () -> format("%s %s > %d", method, item, status), null);

        return response;
    }

    private Response charset(final Response response) { // ;( prevent the container from adding its default charset…

        response.header("Content-Type")
                .filter(type -> TextualPattern.matcher(type).matches()) // textual content with no charset
                .ifPresent(type -> response.header("Content-Type", type+"; charset=UTF-8"));

        return response;
    }

}