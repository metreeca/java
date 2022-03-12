package com.metreeca.core;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * String utilities.
 */
public final class Strings {

    private static final Pattern SpacePattern=Pattern.compile("[ \\p{Cntrl}]+");


    /**
     * Normalizes spaces.
     *
     * @param text the text to be normalized; may be null
     *
     * @return a copy of {@code text} where leading and trailing sequences of control and space characters are removed
     * and other sequences replaced with a single space character
     */
    public static String normalize(final String text) {
        return text == null || text.isEmpty() ? text : SpacePattern.matcher(text.trim()).replaceAll(" ");
    }

    /**
     * Normalize to lower case.
     *
     * @param text the text to be normalized; may be null
     *
     * @return a copy of {@code text} converted to lower case according to the {@link Locale#ROOT root locale}.
     */
    public static String lower(final String text) {
        return text == null || text.isEmpty() ? text : text.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalize to upper case.
     *
     * @param text the text to be normalized; may be null
     *
     * @return a copy of {@code text} converted to upper case according to the {@link Locale#ROOT root locale}.
     */
    public static String upper(final String text) {
        return text == null || text.isEmpty() ? text : text.toUpperCase(Locale.ROOT);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Strings() { }

}
