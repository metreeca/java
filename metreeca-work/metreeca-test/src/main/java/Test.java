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
import com.metreeca.rest.codecs.TextCodec;

import static com.metreeca.rest.Handler.handler;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.codecs.TextCodec.Text;

public final class Test {

    public static void main(final String... args) {
        new JSEServer()

                .delegate(locator -> locator.get(() -> handler(

                        new TextCodec(),

                        (request, forward) -> request.payload(Text)

                                .map(text -> request.reply(OK)

                                        .payload(Text, "ciao "+text+"!!!")

                                )

                                .orElseGet(() -> request.reply(BadRequest))

                )))

                .start();
    }

}
