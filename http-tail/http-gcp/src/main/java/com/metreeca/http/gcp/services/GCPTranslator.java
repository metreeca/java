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

package com.metreeca.http.gcp.services;

import com.metreeca.http.gcp.GCPServer;
import com.metreeca.http.services.Translator;

import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslationServiceClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static java.util.function.Predicate.not;

/**
 * Google Cloud text translator.
 *
 * <p>Translates texts using the Google Cloud Platform Translation API.</p>
 *
 * @see <a href="https://cloud.google.com/translate/docs">Google Cloud Plaform - Cloud Translation</a>
 */
public final class GCPTranslator implements Translator, AutoCloseable {

    private static final LocationName parent=LocationName.of(GCPServer.project(), "global");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final TranslationServiceClient client;


    /**
     * Creates a new Google Cloud text translator.
     */
    public GCPTranslator() {
        try {

            this.client=TranslationServiceClient.create();

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    @Override public Optional<String> translate(final String target, final String source, final String text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        if ( target == null ) {
            throw new NullPointerException("null target");
        }

        if ( source == null ) {
            throw new NullPointerException("null source");
        }

        return text.isBlank() ? Optional.of(text) : Optional.of(client

                .translateText(TranslateTextRequest.newBuilder()
                        .setParent(parent.toString())
                        .setMimeType("text/plain")
                        .setSourceLanguageCode(source)
                        .setTargetLanguageCode(target)
                        .addContents(text)
                        .build()
                )

                .getTranslations(0)
                .getTranslatedText()

        ).filter(not(String::isEmpty));
    }

    @Override public void close() {
        client.close();
    }

}
