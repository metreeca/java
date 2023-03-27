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

package com.metreeca.link;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.link.Values.literal;

import static java.util.function.Predicate.not;

public final class Frames {

    private static final Pattern SeparatorPattern=Pattern.compile("[,;\n\r]+");
    private static final Pattern SpacePattern=Pattern.compile("[ \\p{Cntrl}]+");


    private static String normalize(final String text) {
        return text == null || text.isEmpty() ? text : SpacePattern.matcher(text.trim()).replaceAll(" ");
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<IRI> iri(final Optional<String> string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.map(Values::iri);
    }


    public static Literal string(final String string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return literal(string);
    }

    public static Optional<Literal> string(final Optional<String> string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.map(Values::literal);
    }

    public static Stream<Literal> strings(final Stream<String> string) {

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.map(Values::literal);
    }


    public static Stream<String> values(final String value) {
        return Arrays.stream(SeparatorPattern.split(value)).map(Frames::normalize).filter(not(String::isEmpty));
    }

    public static Optional<String> value(final String value) {
        return Optional.of(value).map(Frames::normalize).filter(not(String::isEmpty));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Frames() { }

}
