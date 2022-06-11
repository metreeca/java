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

import com.metreeca.link.shapes.*;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.function.Predicate;

import static com.metreeca.link.Trace.trace;
import static com.metreeca.link.Values.format;
import static com.metreeca.link.Values.is;
import static com.metreeca.link.Values.lang;
import static com.metreeca.link.Values.traverse;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;


final class ShapeValidator extends Shape.Probe<Trace> {

    static Optional<Trace> validate(final Shape shape, final Value focus, final Collection<Statement> model) {
        return Optional.of(shape

                .redact(Guard.Role)
                .redact(Guard.Task)
                .redact(Guard.View)
                .redact(Guard.Mode, Guard.Convey) // remove internal filtering shapes

                .map(new ShapeValidator(focus, Set.of(focus),
                        model.stream().collect(groupingBy(Statement::getPredicate)) // optimize lookup by predicate
                ))

        ).filter(not(Trace::isEmpty));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Value focus;

    private final Set<Value> group;
    private final Map<IRI, List<Statement>> model;


    private ShapeValidator(final Value focus, final Set<Value> group, final Map<IRI, List<Statement>> model) {

        this.focus=focus;

        this.group=group;
        this.model=model;
    }


    private Set<Value> resolve(final Collection<Value> values) {
        return values.stream().map(this::resolve).collect(toSet());
    }

    private Value resolve(final Value value) {
        return value instanceof Focus ? ((Focus)value).resolve(focus) : value;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Trace probe(final Guard guard) {
        throw new UnsupportedOperationException(guard.toString());
    }


    @Override public Trace probe(final Datatype datatype) {

        final IRI iri=datatype.iri();

        return trace(group.stream()
                .filter(not(value -> is(value, iri)))
                .map(value -> format("%s is not of datatype %s", format(value), format(iri)))
        );
    }


    @Override public Trace probe(final Range range) {

        final Set<Value> values=resolve(range.values());

        return trace(group.stream()
                .filter(value -> !values.contains(value))
                .map(value -> format("%s is not in the expected value range {%s}", format(value), format(values)))
        );
    }

    @Override public Trace probe(final Lang lang) {

        final Set<String> tags=lang.tags();

        final Predicate<Value> predicate=tags.isEmpty()
                ? value -> !lang(value).isEmpty()
                : value -> tags.contains(lang(value));

        return trace(group.stream()

                .filter(not(predicate))

                .map(value -> format(
                        "%s is not in the expected language set {%s}", format(value), join(", ", tags)
                ))
        );
    }


    @Override public Trace probe(final MinExclusive minExclusive) {

        final Value limit=resolve(minExclusive.limit());

        return trace(group.stream()
                .filter(not(value -> Values.compare(value, limit) > 0))
                .map(value -> format("%s is not strictly greater than %s", format(value), format(limit)))
        );
    }

    @Override public Trace probe(final MaxExclusive maxExclusive) {

        final Value limit=resolve(maxExclusive.limit());

        return trace(group.stream()
                .filter(not(value -> Values.compare(value, limit) < 0))
                .map(value -> format("%s is not strictly less than %s", format(value), format(limit)))
        );
    }

    @Override public Trace probe(final MinInclusive minInclusive) {

        final Value limit=resolve(minInclusive.limit());

        return trace(group.stream()
                .filter(not(value -> Values.compare(value, limit) >= 0))
                .map(value -> format("%s is not greater than or equal to %s", format(value),
                        format(limit)))
        );
    }

    @Override public Trace probe(final MaxInclusive maxInclusive) {

        final Value limit=resolve(maxInclusive.limit());

        return trace(group.stream()
                .filter(not(value -> Values.compare(value, limit) <= 0))
                .map(value -> format("%s is not less than or equal to %s", format(value), format(limit)))
        );
    }


    @Override public Trace probe(final MinLength minLength) {

        final int limit=minLength.limit();

        return trace(group.stream()
                .filter(not(value -> Values.text(value).length() >= limit))
                .map(value -> format("%s length is not greater than or equal to %s", format(value), limit))
        );
    }

    @Override public Trace probe(final MaxLength maxLength) {

        final int limit=maxLength.limit();

        return trace(group.stream()
                .filter(not(value -> Values.text(value).length() <= limit))
                .map(value -> format("%s length is not less than or equal to %s", format(value), limit))
        );
    }

    @Override public Trace probe(final Pattern pattern) {

        final String expression=pattern.expression();
        final String flags=pattern.flags();

        final java.util.regex.Pattern compiled=java.util.regex.Pattern
                .compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

        // match the whole string: don't use compiled.asPredicate() (implemented using .find())

        return trace(group.stream()
                .filter(not(value -> compiled.matcher(Values.text(value)).matches()))
                .map(value -> format("%s textual value doesn't match <%s> pattern", format(value), compiled.pattern()))
        );
    }

    @Override public Trace probe(final Like like) {

        final String expression=like.toExpression();

        final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

        return trace(group.stream()
                .filter(not(value -> predicate.test(Values.text(value))))
                .map(value -> format("%s textual value doesn't match <%s> keywords", format(value), like.keywords()))
        );
    }

    @Override public Trace probe(final Stem stem) {

        final String prefix=stem.prefix();

        final Predicate<String> predicate=lexical -> lexical.startsWith(prefix);

        return trace(group.stream()
                .filter(not(value -> predicate.test(Values.text(value))))
                .map(value -> format("%s textual value has not stem <%s>", format(value), prefix))
        );
    }


    @Override public Trace probe(final MinCount minCount) {

        final int count=group.size();
        final int limit=minCount.limit();

        return count >= limit ? trace() : trace(format(
                "value count is not greater than or equal to %s", limit
        ));
    }

    @Override public Trace probe(final MaxCount maxCount) {

        final int count=group.size();
        final int limit=maxCount.limit();

        return count <= limit ? trace() : trace(format(
                "value count is not less than or equal to %s", limit
        ));
    }

    @Override public Trace probe(final All all) {

        final Set<Value> values=resolve(all.values());

        return group.containsAll(values) ? trace() : trace(format(
                "values don't include all the expected set {%s}", format(values)
        ));
    }

    @Override public Trace probe(final Any any) {

        final Set<Value> values=resolve(any.values());

        return values.stream().anyMatch(group::contains) ? trace() : trace(format(
                "values don't include at least one of the expected set {%s}", format(values)
        ));
    }

    @Override public Trace probe(final Localized localized) {
        return trace(group.stream()

                .collect(groupingBy(Values::lang, toList()))

                .entrySet().stream()

                .filter(not(entry -> entry.getValue().size() <= 1))

                .map(entry -> format("multiple values for <%s> language tag", entry.getKey()))
        );
    }


    @Override public Trace probe(final Link link) {
        return link.shape().map(this);
    }

    @Override public Trace probe(final Field field) {
        return group.stream().map(value -> {

            final IRI iri=field.iri();
            final Shape shape=field.shape();

            final Set<Value> values=traverse(iri,

                    recto -> model.getOrDefault(recto, List.of()).stream()
                            .filter(s -> s.getSubject().equals(value))
                            .map(Statement::getObject),

                    verso -> model.getOrDefault(verso, List.of()).stream()
                            .filter(s -> s.getObject().equals(value))
                            .map(Statement::getSubject)

            ).collect(toSet());

            final Trace trace=shape.map(new ShapeValidator(focus, values, model));

            return trace.isEmpty() ? trace : trace(iri.toString(), trace);

        }).reduce(trace(), Trace::trace);
    }


    @Override public Trace probe(final When when) {
        return when.test().map(this).isEmpty()
                ? when.pass().map(this)
                : when.fail().map(this);
    }

    @Override public Trace probe(final And and) {
        return and.shapes().stream()

                .map(s -> s.map(this))

                .reduce(trace(), Trace::trace);
    }

    @Override public Trace probe(final Or or) {

        final List<Trace> traces=or.shapes().stream()
                .map(s -> s.map(this))
                .collect(toList());

        if ( traces.stream().anyMatch(Trace::isEmpty) ) { return trace(); } else {

            final Collection<Trace> alternatives=new ArrayList<>();

            for (int i=0; i < traces.size(); i++) {
                alternatives.add(trace(format("#%d", i), traces.get(i)));
            }

            return trace("values don't match any alternative",
                    alternatives.stream().reduce(trace(), Trace::trace)
            );

        }
    }


    @Override public Trace probe(final Shape shape) {
        return trace();
    }

}