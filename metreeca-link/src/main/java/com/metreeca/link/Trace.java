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

package com.metreeca.link;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.metreeca.core.toolkits.Strings.indent;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;


/**
 * Shape validation trace.
 */
public final class Trace {

    private static final Trace EmptyTrace=new Trace(Stream.empty(), Stream.empty());


    public static Trace trace() {
        return EmptyTrace;
    }

    public static Trace trace(final Trace x, final Trace y) {
        return x.isEmpty() ? y : y.isEmpty() ? x : new Trace(
                Stream.of(x, y).flatMap(trace -> trace.issues().stream()),
                Stream.of(x, y).flatMap(trace -> trace.fields().entrySet().stream())
        );
    }

    public static Trace trace(final Trace... traces) {
        return new Trace(
                Arrays.stream(traces).flatMap(trace -> trace.issues().stream()),
                Arrays.stream(traces).flatMap(trace -> trace.fields().entrySet().stream())
        );
    }

    public static Trace trace(final String... issues) {
        return new Trace(Arrays.stream(issues), Stream.empty());
    }

    public static Trace trace(final Stream<String> issues) {
        return new Trace(issues, Stream.empty());
    }

    public static Trace trace(final String field, final Trace trace) {
        return new Trace(Stream.empty(), Stream.of(new AbstractMap.SimpleImmutableEntry<>(field, trace)));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Collection<String> issues;
    private final Map<String, Trace> fields;


    private Trace(final Stream<String> issues, final Stream<Entry<String, Trace>> fields) {

        this.issues=issues
                .filter(issue -> !issue.isEmpty())
                .collect(toCollection(LinkedHashSet::new));

        this.fields=fields
                .filter(field -> !field.getValue().isEmpty())
                .collect(toMap(Entry::getKey, Entry::getValue, Trace::trace, LinkedHashMap::new));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isEmpty() {
        return issues.isEmpty() && fields.isEmpty();
    }


    public Collection<String> issues() {
        return unmodifiableCollection(issues);
    }

    public Map<String, Trace> fields() {
        return unmodifiableMap(fields);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return Stream

                .of(

                        issues.stream()
                                .map(issue -> format("\t%s", issue))
                                .collect(joining("\n")),

                        fields.entrySet().stream()
                                .map(entry -> format("\t%s: %s", entry.getKey(), indent(entry.getValue().toString())))
                                .collect(joining("\n"))

                )

                .filter(not(String::isEmpty))

                .collect(joining("\n", "{\n", "\n}"));

    }

}
