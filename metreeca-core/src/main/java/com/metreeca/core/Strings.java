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
    public static final int LineLengthLimit=80;


    private static final Pattern SpacePattern=Pattern.compile("[ \\p{Cntrl}]+");
    private static final Pattern NewlinePattern=Pattern.compile("\n");


    /**
     * Normalizes spaces.
     *
     * @param string the (possibly null) string to be normalized
     *
     * @return a copy of {@code string} where leading and trailing sequences of control and space characters are removed
     * and other sequences replaced with a single space character, or {@code null} if {@code string} is null
     */
    public static String normalize(final String string) {
        return string == null || string.isEmpty() ? string : SpacePattern.matcher(string.trim()).replaceAll(" ");
    }

    /**
     * Normalize to lower case.
     *
     * @param string the (possibly null) string to be normalized
     *
     * @return a copy of {@code string} converted to lower case according to the {@link Locale#ROOT root locale}, or
     * {@code null} if {@code string} is null
     */
    public static String lower(final String string) {
        return string == null || string.isEmpty() ? string : string.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalize to upper case.
     *
     * @param string the (possibly null) string to be normalized
     *
     * @return a copy of {@code string} converted to upper case according to the {@link Locale#ROOT root locale}, or
     * {@code null} if {@code string} is null
     */
    public static String upper(final String string) {
        return string == null || string.isEmpty() ? string : string.toUpperCase(Locale.ROOT);
    }


    /**
     * Limits length.
     *
     * @param string the (possibly null) string to be clipped
     *
     * @return the input {@code string} {@linkplain #clip(String, int) clipped} to the default line length limit ({@value
     * #LineLengthLimit}), or {@code null} if {@code string} is null
     */
    public static String clip(final String string) {
        return clip(string, LineLengthLimit);
    }

    /**
     * Limits length.
     *
     * <p>Limits string length by replacing exceeding trailing content with a horizontal ellipsis
     * character ('{@code …}').</p>
     *
     * @param string the (possibly null) string to be clipped
     * @param length the length limit; values less than or equal to {@code 0} disable clipping
     *
     * @return the input {@code string} clipped to the {@code length} limit, or {@code null} if {@code string} is null
     */
    public static String clip(final String string, final int length) {
        return string == null || length <= 0 || string.length() <= length ? string
                : string.substring(0, length-1)+"…";
    }


    /**
     * Limits length.
     *
     * @param string the (possibly null) string to be folded
     *
     * @return the input {@code string} {@linkplain #fold(String, int) folded} to the default line length limit ({@value
     * #LineLengthLimit}), or {@code null} if {@code string} is null
     */
    public static String fold(final String string) {
        return clip(string, LineLengthLimit);
    }

    /**
     * Limits length.
     *
     * <p>Limits string length by replacing exceeding central content with a horizontal ellipsis
     * character ('{@code …}').</p>
     *
     * @param string the (possibly null) string to be folded
     * @param length the length limit; values less than or equal to {@code 0} disable folding
     *
     * @return the input {@code string} folded to the {@code length} limit, or {@code null} if {@code string} is null
     */
    public static String fold(final String string, final int length) {
        return string == null || length <= 0 || string.length() <= length ? string
                : string.substring(0, (length-1)/2)+"…"+string.substring(string.length()-(length-1)/2);
    }


    /**
     * Extracts an excerpt.
     *
     * @param string the (possibly null) string to be excerpted
     *
     * @return the first non-empty {@link #normalize(String) normalized} line of the input {@code string}, or {@code
     * null} if {@code string} is null
     */
    public static String excerpt(final String string) {
        return string == null ? null : Arrays.stream(string.split(NewlinePattern.pattern()))
                .map(Strings::normalize)
                .filter(not(String::isEmpty))
                .findFirst()
                .orElse("");
    }


    /**
     * Quotes and escapes content.
     *
     * @param string the (possibly null) string to be quoted
     *
     * @return a copy of {@code string} surrounded by single quotes and with control characters replaced by escape
     * control sequences, or {@code null} if {@code string} is null; the quoted string may be safely embedded within
     * Java/JSON strings
     */
    public static String quote(final String string) {

        if ( string == null ) { return null; } else {

            final StringBuilder builder=new StringBuilder(string.length()+string.length()/10);

            builder.append('\''); // use single quote to support embedding within Java/JSON string

            for (int i=0, n=string.length(); i < n; ++i) {
                switch ( string.charAt(i) ) {
                    case '\\':
                        builder.append("\\\\");
                        break;
                    case '\'':
                        builder.append("\\'");
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
                        builder.append(string.charAt(i));
                        break;
                }
            }

            builder.append('\'');

            return builder.toString();

        }

    }

    /**
     * Indents lines.
     *
     * @param string the (possibly null) string to be indented
     *
     * @return a copy of {@code string} where each line is prefixed with a tab character, or {@code null} if {@code
     * string} is null
     */
    public static String indent(final String string) { return prefix(string, "\t"); }

    /**
     * Prefixes lines.
     *
     * @param string the (possibly null) string to be prefixed
     * @param prefix the prefix to be inserted before each line
     *
     * @return a copy of {@code string} where each line is prefixed with {@code prefix}, or {@code null} if {@code
     * string} is null
     *
     * @throws NullPointerException if {@code prefix} is null
     */
    public static String prefix(final String string, final String prefix) {

        if ( prefix == null ) {
            throw new NullPointerException("null prefix");
        }

        return string == null ? null : NewlinePattern.matcher(string)
                .replaceAll("\0"+Matcher.quoteReplacement(prefix));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Strings() { }

}
