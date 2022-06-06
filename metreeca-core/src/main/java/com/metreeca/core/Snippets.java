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

package com.metreeca.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Source code utilities.
 */
public final class Snippets {

    private static final Consumer<Appendable> Nothing=code -> { };


    @SafeVarargs public static String code(final Consumer<Appendable>... snippets) {

        if ( snippets == null || stream(snippets).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null snippets");
        }

        final Formatter formatter=new Formatter(new StringBuilder(1000));

        list(snippets).accept(formatter);

        return formatter.toString();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs public static Consumer<Appendable> block(final Consumer<Appendable>... snippets) {
        return list(text("\r{ "), list(snippets), text(" }"));
    }

    @SafeVarargs public static Consumer<Appendable> parens(final Consumer<Appendable>... snippets) {
        return list(text("("), list(snippets), text(")"));
    }

    @SafeVarargs public static Consumer<Appendable> line(final Consumer<Appendable>... snippets) {
        return list(text('\n'), list(snippets), text('\n'));
    }


    @SafeVarargs public static Consumer<Appendable> space(final Consumer<Appendable>... snippets) {
        return list(text('\f'), list(snippets), text('\f'));
    }

    @SafeVarargs public static Consumer<Appendable> indent(final Consumer<Appendable>... snippets) {
        return list(text('\t'), list(snippets), text('\b'));
    }


    @SafeVarargs public static Consumer<Appendable> list(final Consumer<Appendable>... items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        return list(stream(items));
    }

    public static Consumer<Appendable> list(final Stream<Consumer<Appendable>> items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        return list(items.collect(toList())); // memoize to enable reuse
    }

    public static Consumer<Appendable> list(final Iterable<Consumer<Appendable>> items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        return code -> items.forEach(item -> item.accept(code));
    }


    @SafeVarargs public static Consumer<Appendable> list(
            final CharSequence separator, final Consumer<Appendable>... items
    ) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        if ( separator == null ) {
            throw new NullPointerException("null separator");
        }

        return list(separator, asList(items));
    }

    public static Consumer<Appendable> list(final CharSequence separator,
            final Stream<Consumer<Appendable>> items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        if ( separator == null ) {
            throw new NullPointerException("null separator");
        }

        return list(separator, items.collect(toList())); // memoize to enable reuse
    }

    public static Consumer<Appendable> list(final CharSequence separator,
            final Collection<Consumer<Appendable>> items) {

        if ( items == null ) {
            throw new NullPointerException("null items");
        }

        if ( separator == null ) {
            throw new NullPointerException("null separator");
        }

        return code -> items.stream()
                .flatMap(item -> Stream.of(text(separator), item))
                .skip(1)
                .forEach(item -> item.accept(code));
    }


    public static Consumer<Appendable> text(final Object value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return text(valueOf(value));
    }

    public static Consumer<Appendable> text(final char c) {
        return code -> {
            try {

                code.append(c);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }
        };
    }

    public static Consumer<Appendable> text(final CharSequence text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        return code -> {
            try {

                code.append(text);

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }
        };
    }

    public static Consumer<Appendable> text(final String format, final Object... args) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        return text(String.format(format, args));
    }


    public static Consumer<Appendable> nothing() {
        return Nothing;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Snippets() { }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Formatter implements Appendable {

        private final Appendable code; // output target

        private int indent; // indent level

        private char last; // last output
        private char wait; // pending optional whitespace


        private Formatter(final Appendable code) {
            this.code=code;
        }


        @Override public Appendable append(final CharSequence sequence) {

            for (int i=0, n=sequence.length(); i < n; ++i) { append(sequence.charAt(i)); }

            return this;
        }

        @Override public Appendable append(final CharSequence sequence, final int start, final int end) {

            for (int i=start; i < end; ++i) { append(sequence.charAt(i)); }

            return this;
        }

        @Override public Appendable append(final char c) {
            switch ( c ) {

                case '\f': return feed();
                case '\r': return fold();
                case '\n': return newline();
                case ' ': return space();

                case '\t': return indent();
                case '\b': return outdent();

                default: return other(c);

            }
        }


        @Override public String toString() {
            return code.toString();
        }


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private Formatter feed() {

            if ( last != '\0' ) { wait='\f'; }

            return this;
        }

        private Formatter fold() {

            if ( last != '\0' && wait != '\f' ) { wait=(wait == '\n') ? '\f' : ' '; }

            return this;
        }

        private Formatter newline() {

            if ( last != '\0' && wait != '\f' ) { wait='\n'; }

            return this;
        }

        private Formatter space() {

            if ( last != '\0' && wait != '\f' && wait != '\n' ) { wait=' '; }

            return this;
        }


        private Formatter indent() {

            if ( last != '\0' ) { ++indent; }

            return this;
        }

        private Formatter outdent() {

            if ( indent > 0 ) { --indent; }

            return this;
        }


        private Formatter other(final char c) {
            try {

                if ( wait == '\f' || wait == '\n' ) {

                    if ( last == '{' ) { ++indent; }

                    if ( c == '}' && indent > 0 ) { --indent; }

                    code.append(wait == '\f' ? "\n\n" : "\n");

                    for (int i=4*indent; i > 0; --i) { code.append(' '); }

                } else if ( wait == ' ' && last != '(' && c != ')' && last != '[' && c != ']' ) {

                    code.append(' ');

                }

                code.append(c);

                return this;

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            } finally {

                last=c;
                wait='\0';

            }
        }

    }

}
