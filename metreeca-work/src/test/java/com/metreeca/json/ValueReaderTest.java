package com.metreeca.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.text.ParseException;
import java.util.List;

import static com.metreeca.json.Value.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Map.entry;

final class ValueReaderTest {

    @Test void testReportEmptyDocument() {
        assertThatThrownBy(() -> read("")).isInstanceOf(ParseException.class);
    }

    @Test void testReportTrailingGarbage() {
        assertThatThrownBy(() -> read("true false")).isInstanceOf(ParseException.class);
    }


    @Nested final class Objects {

        @Test void testReadEmptyObject() throws ParseException {
            assertThat(read("{ }"))
                    .isEqualTo(object(map()));
        }

        @Test void testReadSingletonObject() throws ParseException {
            assertThat(read("{ \"uno\": 1 }"))
                    .isEqualTo(object(map(entry("uno", integer(1)))));
        }

        @Test void testReadObject() throws ParseException {
            assertThat(read("{\n\t\"uno\": 1,\n\t\"due\": 2\n}"))
                    .isEqualTo(object(map(entry("uno", integer(1)), entry("due", integer(2)))));
        }

        @Test void testNestedValues() throws ParseException {
            assertThat(read("{\n"
                    +"\t\"object\": {},\n"
                    +"\t\"array\": [],\n"
                    +"\t\"string\": \"\",\n"
                    +"\t\"number\": 0,\n"
                    +"\t\"boolean\": false,\n"
                    +"\t\"null\": null\n"
                    +"}"
            )).isEqualTo(object(map(
                    entry("object", object(map())),
                    entry("array", array(List.of())),
                    entry("string", string("")),
                    entry("number", integer(0)),
                    entry("boolean", bool(false)),
                    entry("null", nil())
            )));
        }

        @Test void testReportTrailingComma() {
            assertThatThrownBy(() -> read("{ \"uno\": 1, }")).isInstanceOf(ParseException.class);
        }

    }

    @Nested final class Arrays {

        @Test void testReadEmptyArray() throws ParseException {
            assertThat(read("[ ]"))
                    .isEqualTo(array(List.of()));
        }

        @Test void testReadSingletonArray() throws ParseException {
            assertThat(read("[1]"))
                    .isEqualTo(array(List.of(integer(1))));
        }

        @Test void testReadArray() throws ParseException {
            assertThat(read("[1, 2]"))
                    .isEqualTo(array(List.of(integer(1), integer(2))));
        }

        @Test void testNestedValues() throws ParseException {
            assertThat(read("[\n"
                    +"\t{},\n"
                    +"\t[],\n"
                    +"\t\"\",\n"
                    +"\t0,\n"
                    +"\tfalse,\n"
                    +"\tnull\n"
                    +"]"
            )).isEqualTo(array(List.of(
                    object(map()),
                    array(List.of()),
                    string(""),
                    integer(0),
                    bool(false),
                    nil()
            )));
        }

        @Test void testReportTrailingComma() {
            assertThatThrownBy(() -> read("[1,]")).isInstanceOf(ParseException.class);
        }

    }


    @Nested final class Strings {

        @Test void testReadEmptyString() throws ParseException {
            assertThat(read("\"\""))
                    .isEqualTo(string(""));
        }

        @Test void testReadString() throws ParseException {
            assertThat(read("\"abc\\\\\\\"\\/\\b\\f\\n\\r\\t\\u1234\""))
                    .isEqualTo(string("abc\\\"/\b\f\n\r\t\u1234"));
        }

        @Test void testReportControlChars() {
            assertThatThrownBy(() -> read("\"\u0000\""))
                    .isInstanceOf(ParseException.class);
        }

    }

    @Nested final class Numbers {

        @Test void testReadIntegers() throws ParseException {
            assertThat(read("0")).isEqualTo(integer(0));
            assertThat(read("-0")).isEqualTo(integer(0));
            assertThat(read("10")).isEqualTo(integer(10));
            assertThat(read("-10")).isEqualTo(integer(-10));
        }

        @Test void testReportLeadingZero() {
            assertThatThrownBy(() -> read("01")).isInstanceOf(ParseException.class);
        }

        @Test void testReportLeadingPlus() {
            assertThatThrownBy(() -> read("+1")).isInstanceOf(ParseException.class);
        }

        @Test void testReadDecimals() throws ParseException {
            assertThat(read("0.0")).isEqualTo(decimal(0.0));
            assertThat(read("0.1")).isEqualTo(decimal(0.1));
            assertThat(read("-0.1")).isEqualTo(decimal(-0.1));
            assertThat(read("10.0")).isEqualTo(decimal(10.0));
            assertThat(read("-10.0")).isEqualTo(decimal(-10.0));
        }

        @Test void testReadFloatings() throws ParseException {
            assertThat(read("10e+2")).isEqualTo(floating(1000));
            assertThat(read("-1.0E-2")).isEqualTo(floating(-0.01));
        }

        @Test void testReportLeadingDot() {
            assertThatThrownBy(() -> read(".1")).isInstanceOf(ParseException.class);
        }

        @Test void testReportTrailingDot() {
            assertThatThrownBy(() -> read("1.")).isInstanceOf(ParseException.class);
        }

    }

    @Nested final class Booleans {

        @Test void testReadBooleans() throws ParseException {
            assertThat(read("true")).isEqualTo(bool(true));
            assertThat(read("false")).isEqualTo(bool(false));
        }

        @Test void testReportMixedCase() {
            assertThatThrownBy(() -> read("True")).isInstanceOf(ParseException.class);
        }

    }

    @Nested final class Nulls {

        @Test void testReadNulls() throws ParseException {
            assertThat(read("null")).isEqualTo(nil());
        }

        @Test void testReportMixedCase() {
            assertThatThrownBy(() -> read("Null")).isInstanceOf(ParseException.class);
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Value read(final String json) throws ParseException {
        try ( final StringReader reader=new StringReader(json) ) {

            return new ValueReader(reader).read();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

}
