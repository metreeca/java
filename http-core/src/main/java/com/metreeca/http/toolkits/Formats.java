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

package com.metreeca.http.toolkits;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static java.time.temporal.ChronoField.*;

/**
 * Format utilities.
 */
public final class Formats {

    /**
     * A SQL timestamp formatter ({@code yyyy-MM-dd hh:mm:ss}).
     */
    public static final DateTimeFormatter SQL_TIMESTAMP=new DateTimeFormatterBuilder()

            .parseCaseInsensitive()
            .parseStrict()

            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)

            .toFormatter(Locale.ROOT);

    /**
     * A compact ISO local date formatter ({@code yyyyMMdd}).
     */
    public static final DateTimeFormatter ISO_LOCAL_DATE_COMPACT=new DateTimeFormatterBuilder()

            .parseStrict()

            .appendValue(YEAR, 4)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)

            .toFormatter(Locale.ROOT);

    /**
     * A compact ISO local datetime formatter ({@code yyyyMMddThhmmss}).
     */
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME_COMPACT=new DateTimeFormatterBuilder()

            .parseStrict()

            .appendValue(YEAR, 4)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral("T")
            .appendValue(HOUR_OF_DAY, 2)
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendValue(SECOND_OF_MINUTE, 2)

            .toFormatter(Locale.ROOT);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Formats() { }

}
