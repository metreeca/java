package com.metreeca.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URI;
import java.time.*;
import java.util.List;

import static com.metreeca.json.Value.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Map.entry;

final class ValueWriterTest {


    @Nested final class Objects {

        @Test void testWriteEmptyObject() {
            assertThat(write(object(map())))
                    .isEqualTo("{}");
        }

        @Test void testWriteSingletonObject() {
            assertThat(write(object(map(entry("uno", integer(1))))))
                    .isEqualTo(indent("{\n\t\"uno\": 1\n}"));
        }

        @Test void testWriteObject() {
            assertThat(write(object(map(entry("uno", integer(1)), entry("due", integer(2))))))
                    .isEqualTo(indent("{\n\t\"uno\": 1,\n\t\"due\": 2\n}"));
        }

        @Test void testNestedValues() {
            assertThat(write(object(map(
                    entry("object", object(map())),
                    entry("array", array(list())),
                    entry("string", string("")),
                    entry("number", integer(0)),
                    entry("boolean", bool(false)),
                    entry("null", nil())
            ))))
                    .isEqualTo(indent("{\n"
                            +"\t\"object\": {},\n"
                            +"\t\"array\": [],\n"
                            +"\t\"string\": \"\",\n"
                            +"\t\"number\": 0,\n"
                            +"\t\"boolean\": false,\n"
                            +"\t\"null\": null\n"
                            +"}"
                    ));
        }

    }

    @Nested final class Arrays {

        @Test void testWriteEmptyArray() {
            assertThat(write(array(list())))
                    .isEqualTo("[]");
        }

        @Test void testWriteSingletonArray() {
            assertThat(write(array(List.of(integer(1)))))
                    .isEqualTo(indent("[\n\t1\n]"));
        }

        @Test void testWriteArray() {
            assertThat(write(array(List.of(integer(1), integer(2)))))
                    .isEqualTo(indent("[\n\t1,\n\t2\n]"));
        }

        @Test void testNestedValues() {
            assertThat(write(array(List.of(
                    object(map()),
                    array(list()),
                    string(""),
                    integer(0),
                    bool(false),
                    nil()
            ))))
                    .isEqualTo(indent("[\n"
                            +"\t{},\n"
                            +"\t[],\n"
                            +"\t\"\",\n"
                            +"\t0,\n"
                            +"\tfalse,\n"
                            +"\tnull\n"
                            +"]"
                    ));
        }

    }


    @Nested final class URIs {

        @Test void testWriteURIs() {
            assertThat(write(id(URI.create("https://example.com/")))).isEqualTo("\"https://example.com/\"");
        }

    }

    @Nested final class LocalDateTimes {

        @Test void testWriteLocalDateTimes() {
            assertThat(write(datetime(LocalDateTime.of(
                    2021, 12, 31,
                    1, 2, 3, 4_000_000
            ))))
                    .isEqualTo("\"2021-12-31T01:02:03.004Z\"");
        }

    }

    @Nested final class LocalDates {

        @Test void testWriteLocalDates() {
            assertThat(write(date(LocalDate.of(2021, 12, 31)))).isEqualTo("\"2021-12-31Z\"");
        }

    }

    @Nested final class LocalTimes {

        @Test void testWriteLocalTimes() {
            assertThat(write(time(LocalTime.of(1, 2, 3, 4_000_000)))).isEqualTo("\"01:02:03.004Z\"");
        }

    }


    @Nested final class Strings {

        @Test void testWriteEmptyString() {
            assertThat(write(string("")))
                    .isEqualTo("\"\"");
        }

        @Test void testWriteString() {
            assertThat(write(string("abcè\\\"/\b\f\n\r\t\u007F")))
                    .isEqualTo("\"abcè\\\\\\\"/\\b\\f\\n\\r\\t\\u007F\"");
        }

    }

    @Nested final class Numbers {

        @Test void testWriteIntegers() {
            assertThat(write(integer(0))).isEqualTo("0");
            assertThat(write(integer(10))).isEqualTo("10");
            assertThat(write(integer(-10))).isEqualTo("-10");
        }

        @Test void testWriteDecimals() {
            assertThat(write(decimal(0))).isEqualTo("0.0");
            assertThat(write(decimal(0.1))).isEqualTo("0.1");
            assertThat(write(decimal(-0.1))).isEqualTo("-0.1");
            assertThat(write(decimal(10))).isEqualTo("10.0");
            assertThat(write(decimal(-10))).isEqualTo("-10.0");
        }

        @Test void testWriteFloatings() {
            assertThat(write(floating(1.2E3))).isEqualTo("1.2E3");
            assertThat(write(floating(-1.2E-3))).isEqualTo("-1.2E-3");
        }

    }

    @Nested final class Booleans {

        @Test void testWriteBooleans() {
            assertThat(write(bool(true))).isEqualTo("true");
            assertThat(write(bool(false))).isEqualTo("false");
        }

    }

    @Nested final class Nulls {

        @Test void testWriteNulls() {
            assertThat(write(nil())).isEqualTo("null");
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String write(final Value value) {
        try ( final StringWriter writer=new StringWriter() ) {

            new ValueWriter(writer).write(value);

            return writer.toString();

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    private String indent(final String string) {
        return string.replace("\t", "    ");
    }

}
