/*
 * Copyright © 2020-2022 EC2U Alliance
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

package com.metreeca.http.services;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Text translator.
 *
 * <p>Manages automated text translation.</p>
 */
public interface Translator {

    /**
     * Retrieves the default translator factory.
     *
     * @return the default translator factory, which throws an exception reporting the service as undefined
     */
    public static Supplier<Translator> translator() {
        return () -> { throw new IllegalStateException("undefined translator service"); };
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Translates text.
     *
     * @param target the target language BCP-47 tag for the translated text
     * @param source the source language BCP-47 tag of the {@code  text} to be translated or an empty string if the
     *               source language is expected to be auto-detected
     * @param text   the text to be translated
     *
     * @return an optional containing the input {@code text} translated from {@code source} to {@code target} language;
     * an empty optional, if a translation error occurred, for instance if the underlying translation engine does not
     * support either the {@code target} or the {@code source} language
     *
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if either {@code target} or {@code source} is not a valid language tag according
     *                                  to RFC 5646
     * @see <a href="https://www.rfc-editor.org/rfc/rfc5646.html">RFC 5646 - Tags for Identifying Languages</a>
     */
    public Optional<String> translate(final String target, final String source, final String text);

}
