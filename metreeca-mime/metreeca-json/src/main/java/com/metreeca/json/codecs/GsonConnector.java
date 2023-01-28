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

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.*;

/**
 * !!! Remove after integrating to metreeca/kona
 */
public final class GsonConnector {

    public static GsonBuilder GsonBuilder() {
        return new GsonBuilder()

                .registerTypeAdapter(Instant.class, new JSONInstant().nullSafe())

                .registerTypeAdapter(OffsetDateTime.class, new JSONOffsetDateTime().nullSafe())
                .registerTypeAdapter(OffsetTime.class, new JSONOffsetTime().nullSafe())

                .registerTypeAdapter(LocalDateTime.class, new JSONLocalDateTime().nullSafe())
                .registerTypeAdapter(LocalDate.class, new JSONLocalDate().nullSafe())
                .registerTypeAdapter(LocalTime.class, new JSONLocalTime().nullSafe())

                .registerTypeAdapter(Year.class, new JSONYear().nullSafe())

                .registerTypeAdapter(Period.class, new JSONPeriod().nullSafe())
                .registerTypeAdapter(Duration.class, new JSONDuration().nullSafe())

                .setPrettyPrinting();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private GsonConnector() { }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // !!! sneak
    // !!! textual

    private static final class JSONInstant extends TypeAdapter<Instant> {

        @Override public Instant read(final JsonReader reader) throws IOException {
            return Instant.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final Instant value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONOffsetDateTime extends TypeAdapter<OffsetDateTime> {

        @Override public OffsetDateTime read(final JsonReader reader) throws IOException {
            return OffsetDateTime.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final OffsetDateTime value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONOffsetTime extends TypeAdapter<OffsetTime> {

        @Override public OffsetTime read(final JsonReader reader) throws IOException {
            return OffsetTime.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final OffsetTime value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONLocalDateTime extends TypeAdapter<LocalDateTime> {

        @Override public LocalDateTime read(final JsonReader reader) throws IOException {
            return LocalDateTime.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final LocalDateTime value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONLocalDate extends TypeAdapter<LocalDate> {

        @Override public LocalDate read(final JsonReader reader) throws IOException {
            return LocalDate.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final LocalDate value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONLocalTime extends TypeAdapter<LocalTime> {

        @Override public LocalTime read(final JsonReader reader) throws IOException {
            return LocalTime.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final LocalTime value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONYear extends TypeAdapter<Year> {

        @Override public Year read(final JsonReader reader) throws IOException {
            return Year.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final Year value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONPeriod extends TypeAdapter<Period> {

        @Override public Period read(final JsonReader reader) throws IOException {
            return Period.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final Period value) throws IOException {
            writer.value(value.toString());
        }

    }

    private static final class JSONDuration extends TypeAdapter<Duration> {

        @Override public Duration read(final JsonReader reader) throws IOException {
            return Duration.parse(reader.nextString());
        }

        @Override public void write(final JsonWriter writer, final Duration value) throws IOException {
            writer.value(value.toString());
        }

    }

}
