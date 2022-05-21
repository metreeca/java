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

package com.metreeca.rest;

import com.metreeca.core.Feeds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class ResponseAssert extends MessageAssert<ResponseAssert, Response> {

    public static ResponseAssert assertThat(final Response response) {

        if ( response != null ) {

            // cache output

            final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

            response.output().accept(buffer);

            response.output(output -> Feeds.data(output, buffer.toByteArray())); // cache output
            response.input(() -> new ByteArrayInputStream(buffer.toByteArray())); // expose output to testing


            // log response

            final StringBuilder builder=new StringBuilder(2500);

            builder.append(response.status()).append('\n');

            response.headers().forEach((name, value) ->
                    builder.append(name).append(": ").append(value).append('\n')
            );

            builder.append('\n');

            final String text=buffer.toString(response.charset());

            if ( !text.isEmpty() ) {

                final int limit=builder.capacity();

                builder
                        .append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮")
                        .append("\n\n");
            }

            Logger.getLogger(response.getClass().getName()).log(
                    response.status() < 400 ? Level.INFO : response.status() < 500 ? Level.WARNING : Level.SEVERE,
                    builder.toString()
            );

        }

        return new ResponseAssert(response);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private ResponseAssert(final Response actual) {
        super(actual, ResponseAssert.class);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ResponseAssert isSuccess() {

        isNotNull();

        if ( !actual.success() ) {
            failWithMessage("expected response to be success but was <%d>", actual.status());
        }

        return this;
    }

    public ResponseAssert hasStatus(final int expected) {

        isNotNull();

        if ( actual.status() != expected ) {
            failWithMessage("expected response status to be <%d> was <%d>", expected, actual.status());
        }

        return this;
    }

}
