/*
 * Copyright © 2020-2022 Metreeca srl
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

import com.metreeca.jse.JSEServer;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.codecs.Payload;
import com.metreeca.rest.codecs.Text;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.OK;

import static java.lang.String.format;

public final class Test {

    public static void main(final String... args) {
        new JSEServer()

                .delegate(locator -> locator.get(() -> (request, forward) ->
                        reply(request, new Text(), value ->
                                request.reply(OK).output(new Text(), format("ciao %s!!!", value))
                        )
                ))

                .start();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static <V> Response reply(
            final Request request,
            final Payload<V> payload,
            final Function<? super V, Response> handler
    ) {
        return reply(request,

                payload,

                () -> request.reply(BadRequest, new IllegalArgumentException(format(
                        "missing <%s> body", payload.mime()
                ))),

                handler

        );
    }

    private static <V> Response reply(
            final Request request,
            final Payload<V> payload,
            final Supplier<Response> missing,
            final Function<? super V, Response> value
    ) {
        return request.input(payload).map(

                missing,

                error -> request.reply(BadRequest, error),

                value::apply

        );
    }

}
