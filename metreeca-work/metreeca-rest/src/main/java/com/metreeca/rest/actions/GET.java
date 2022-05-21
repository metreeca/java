
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

import com.metreeca.rest.Codec;
import com.metreeca.rest.Request;

import java.util.Optional;
import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * Resource retrieval.
 *
 * <p>Maps textual resource URIs to optional resource bodies.</p>
 *
 * @param <R> the type of the resource body
 */
public final class GET<R> implements Function<String, Optional<R>> {

    private final Codec<R> codec;
    private final Function<Request, Request> customizer;


    /**
     * Creates a resource retriever.
     *
     * @param codec the format of the resource to be retrieved
     *
     * @throws NullPointerException if {@code format} is null
     */
    public GET(final Codec<R> codec) {

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        this.codec=codec;
        this.customizer=identity();
    }

    /**
     * Creates a customized retriever.
     *
     * @param codec      the format of the resource to be retrieved
     * @param customizer the request customizer
     *
     * @throws NullPointerException if either {@code format} or {@code customizer} is null
     */
    public GET(final Codec<R> codec, final Function<Request, Request> customizer) {

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        if ( customizer == null ) {
            throw new NullPointerException("null customizer");
        }

        this.codec=codec;
        this.customizer=customizer;
    }


    @Override public Optional<R> apply(final String url) {
        return Optional.of(url)

                .flatMap(new Query(customizer.compose(request -> request
                        .header("Accept", codec.mime())
                )))

                .flatMap(new Fetch())
                .flatMap(new Parse<>(codec));
    }

}
