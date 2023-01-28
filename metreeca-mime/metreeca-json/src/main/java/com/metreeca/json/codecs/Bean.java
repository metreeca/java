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

package com.metreeca.json.codecs;

import com.metreeca.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.Optional;

import static com.metreeca.http.Response.BadRequest;
import static com.metreeca.json.codecs.GsonConnector.GsonBuilder;


public final class Bean<T> implements Codec<T> {

    private final Class<T> clazz;

    private final Gson json=GsonBuilder().create();


    public Bean(final Class<T> clazz) {

        if ( clazz == null ) {
            throw new NullPointerException("null clazz");
        }

        this.clazz=clazz;
    }


    /**
     * @return {@value JSON#MIME}
     */
    @Override public String mime() {
        return JSON.MIME;
    }

    @Override public Class<T> type() {
        return clazz;
    }


    @Override public Optional<T> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .or(() -> Optional.of(JSON.MIME))
                .filter(JSON.MIMEPattern.asPredicate())

                .map(type -> {

                    try (
                            final InputStream input=message.input().get();
                            final Reader reader=new InputStreamReader(input, message.charset())
                    ) {

                        return json.fromJson(reader, clazz);

                    } catch ( final UnsupportedEncodingException|JsonSyntaxException e ) {

                        throw new CodecException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });

    }

    @Override public <M extends Message<M>> M encode(final M message, final T value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(JSON.MIME))

                .output(output -> {
                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        json.toJson(value, writer);

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }
                });
    }

}
