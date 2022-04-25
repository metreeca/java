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

package com.metreeca.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.function.Predicate.not;

/**
 * String utilities.
 */
public final class Strings {

    /**
     * The default maximum line length limit ({@value}).
     */
    public static final int LineLength=80;

    /**
     * The default maximum small text length limit ({@value}).
     */
    public static final int TextLength=400;


    private static final Pattern SpacePattern=Pattern.compile("[ \\p{Cntrl}]+");
    private static final Pattern NewlinePattern=Pattern.compile("\r?\n");


    /**
     * Normalizes spaces.
     *
     * @param string the string to be normalized
     *
     * @return a copy of {@code string} where leading and trailing sequences of control and space characters are removed
     * and other sequences replaced with a single space character
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String normalize(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : SpacePattern.matcher(string.trim()).replaceAll(" ");
    }

    /**
     * Normalize to lower case.
     *
     * @param string the string to be normalized
     *
     * @return a copy of {@code string} converted to lower case according to the {@link Locale#ROOT root locale}
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String lower(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : string.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalize to upper case.
     *
     * @param string the string to be normalized
     *
     * @return a copy of {@code string} converted to upper case according to the {@link Locale#ROOT root locale}
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String upper(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : string.toUpperCase(Locale.ROOT);
    }


    /**
     * Limits length.
     *
     * @param string the string to be clipped
     *
     * @return the input {@code string} {@linkplain #clip(String, int) clipped} to the default line length limit ({@value
     * #LineLength})
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String clip(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : clip(string, LineLength);
    }

    /**
     * Limits length.
     *
     * <p>Limits string length by replacing exceeding trailing content with a horizontal ellipsis
     * character ('{@code …}').</p>
     *
     * @param string the string to be clipped
     * @param length the length limit; values less than or equal to {@code 0} disable clipping
     *
     * @return the input {@code string} clipped to the {@code length} limit
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String clip(final String string, final int length) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return length <= 0 || string.length() <= length ? string
                : string.substring(0, length-1)+"…";
    }


    /**
     * Limits length.
     *
     * @param string the string to be folded
     *
     * @return the input {@code string} {@linkplain #fold(String, int) folded} to the default line length limit ({@value
     * #LineLength})
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String fold(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : clip(string, LineLength);
    }

    /**
     * Limits length.
     *
     * <p>Limits string length by replacing exceeding central content with a horizontal ellipsis
     * character ('{@code …}').</p>
     *
     * @param string the string to be folded
     * @param length the length limit; values less than or equal to {@code 0} disable folding
     *
     * @return the input {@code string} folded to the {@code length} limit
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String fold(final String string, final int length) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return length <= 0 || string.length() <= length ? string
                : string.substring(0, (length-1)/2)+"…"+string.substring(string.length()-(length-1)/2);
    }


    /**
     * Extracts an excerpt.
     *
     * @param string the string to be excerpted
     *
     * @return the first non-empty {@link #normalize(String) normalized} line of the input {@code string}
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String excerpt(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : Arrays.stream(string.split(NewlinePattern.pattern()))
                .map(Strings::normalize)
                .filter(not(String::isEmpty))
                .findFirst()
                .orElse("");
    }


    /**
     * Quotes and escapes content.
     *
     * @param string the string to be quoted
     *
     * @return a copy of {@code string} surrounded by single quotes and with control characters replaced by escape
     * control sequences; the quoted string may be safely embedded within Java/JSON strings
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String quote(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return quote(string, '\''); // use single quote to support embedding within Java/JSON string
    }

    /**
     * Quotes and escapes content.
     *
     * @param string the string to be quoted
     * @param quote  the quote character
     *
     * @return a copy of {@code string} surrounded by single quotes and with control characters replaced by escape
     * control sequences; the quoted string may be safely embedded within Java/JSON strings
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String quote(final String string, final char quote) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        final StringBuilder builder=new StringBuilder(string.length()+string.length()/10);

        builder.append(quote);

        for (int i=0, n=string.length(); i < n; ++i) {

            final char c=string.charAt(i);

            switch ( c ) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\'':
                    builder.append(c == quote ? "\\'" : c);
                    break;
                case '\"':
                    builder.append(c == quote ? "\\\"" : c);
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }

        builder.append(quote);

        return builder.toString();

    }

    /**
     * Indents lines.
     *
     * @param string the string to be indented
     *
     * @return a copy of {@code string} where each line is prefixed with a tab character
     *
     * @throws NullPointerException if {@code string} is null
     */
    public static String indent(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.isEmpty() ? string : prefix(string, "\t");
    }

    /**
     * Prefixes lines.
     *
     * @param string the string to be prefixed
     * @param prefix the prefix to be inserted before each line
     *
     * @return a copy of {@code string} where each line is prefixed with {@code prefix}
     *
     * @throws NullPointerException if either {@code string} or {@code prefix} is null
     */
    public static String prefix(final String string, final String prefix) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        if ( prefix == null ) {
            throw new NullPointerException("null prefix");
        }

        return string.isEmpty() ? string
                : NewlinePattern.matcher(string).replaceAll("\0"+Matcher.quoteReplacement(prefix));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Strings() { }

}
