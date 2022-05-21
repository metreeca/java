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

package com.metreeca.http;

/**
 * Codec exception.
 *
 * <p>Thrown to report message encoding/decoding issues.</p>
 */
public final class CodecException extends RuntimeException {

    private static final long serialVersionUID=6385340424276867964L;


    private final int status;


    /**
     * Creates a codec exception.
     *
     * @param status  a client error HTTP status code ({@code 4xx})
     * @param message the exception message
     *
     * @throws IllegalArgumentException if {@code status} is outside the client error message
     * @throws NullPointerException     if {@code message} is null
     */
    public CodecException(final int status, final String message) {

        super(message);

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        if ( status/100 != 4 ) {
            throw new IllegalArgumentException("status outside client error range");
        }

        this.status=status;
    }


    /**
     * Retrieves the exception client error HTTP status code
     *
     * @return an HTTP status code in the {@code 4xx} range
     */
    public int getStatus() {
        return status;
    }

}
