package com.metreeca.core;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * Format utilities.
 */
public final class Formats {

    /**
     * A SQL timestamp formatter ({@code yyyy-MM-dd hh:mm:ss}).
     */
    public static final DateTimeFormatter SQL_TIMESTAMP=new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .parseStrict()
            .toFormatter();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Formats() { }

}
