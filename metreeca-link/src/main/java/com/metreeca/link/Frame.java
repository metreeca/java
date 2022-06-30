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

import com.metreeca.core.Strings;
import com.metreeca.link.shifts.Path;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.core.Identifiers.md5;
import static com.metreeca.link.Values.*;
import static com.metreeca.link.shifts.Alt.alt;
import static com.metreeca.link.shifts.Seq.seq;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.*;

/**
 * Linked data frame.
 *
 * <p>Describes a linked data graph centered on a focus value.</p>
 */
public final class Frame {

    private static final Path Labels=alt(
            RDFS.LABEL, DC.TITLE, DCTERMS.TITLE, iri("http://schema.org/", "name")
    );

    private static final Path Briefs=alt(
            RDFS.COMMENT, DC.DESCRIPTION, DCTERMS.DESCRIPTION, iri("http://schema.org/", "description")
    );


    public static Frame frame(final Value focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        return new Frame(focus, Set.of());
    }

    public static Frame frame(final Value focus, final Collection<Statement> model) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null model or model statement");
        }

        final Queue<Value> pending=new ArrayDeque<>(Set.of(focus));
        final Collection<Value> visited=new HashSet<>();
        final Set<Statement> reachable=new LinkedHashSet<>();

        for (Value value; (value=pending.poll()) != null; ) {
            if ( visited.add(value) ) {
                for (final Statement statement : model) {

                    if ( statement.getSubject().equals(value) ) {
                        pending.add(statement.getObject());
                        reachable.add(statement);
                    }

                    if ( statement.getObject().equals(value) ) {
                        pending.add(statement.getSubject());
                        reachable.add(statement);
                    }

                }
            }
        }

        return new Frame(focus, reachable);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Value focus;
    private final Set<Statement> model;


    private Frame(final Value focus, final Set<Statement> model) {
        this.focus=focus;
        this.model=model;
    }


    public boolean isEmpty() {
        return model.isEmpty();
    }


    public Optional<String> label() {
        return string(Labels);
    }

    public Optional<String> notes() {
        return string(Briefs);
    }


    /**
     * Retrieves the frame focus.
     *
     * @return the frame focus value.
     */
    public Value focus() {
        return focus;
    }

    public Set<Statement> model() {
        return unmodifiableSet(model);
    }

    public Stream<Statement> stream() {
        return model.stream();
    }


    public String skolemize() {
        return md5(focus.stringValue());
    }

    public String skolemize(final IRI... traits) {

        if ( traits == null || Arrays.stream(traits).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null traits");
        }

        return md5(Arrays.stream(traits)
                .flatMap(this::values)
                .map(Value::stringValue)
                .collect(joining("\n"))
        );
    }

    public String skolemize(final Shift... shifts) {

        if ( shifts == null || Arrays.stream(shifts).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null shifts");
        }

        return md5(Arrays.stream(shifts)
                .flatMap(this::values)
                .map(Value::stringValue)
                .collect(joining("\n"))
        );
    }


    public Frame refocus(final Value focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        return new Frame(focus, model.stream()

                .map(statement -> {

                    final Resource subject=statement.getSubject();

                    final IRI predicate=statement.getPredicate();
                    final Value object=statement.getObject();
                    final Resource context=statement.getContext();

                    return statement(
                            focus.isResource() && subject.equals(this.focus) ? (Resource)focus : subject,
                            focus.isIRI() && predicate.equals(this.focus) ? (IRI)focus : predicate,
                            object.equals(this.focus) ? focus : object,
                            context.isResource() && context.equals(this.focus) ? (Resource)focus : context
                    );
                })

                .collect(toSet())
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Boolean> bool(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return bool(seq(predicate));
    }

    public Optional<Boolean> bool(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return value(shift).flatMap(Values::bool);
    }


    public Frame bool(final IRI predicate, final Boolean bool) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return bool == null ? this : value(predicate, literal(bool));
    }

    public Frame bool(final IRI predicate, final Optional<Boolean> bool) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( bool == null ) {
            throw new NullPointerException("null bool");
        }

        return bool.map(object -> value(predicate, literal(object))).orElse(this);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BigInteger> integer(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return integer(seq(predicate));
    }

    public Optional<BigInteger> integer(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return value(shift).flatMap(Values::integer);
    }


    public Stream<BigInteger> integers(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return integers(seq(predicate));
    }

    public Stream<BigInteger> integers(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return values(shift).map(Values::integer).filter(Optional::isPresent).map(Optional::get);
    }


    public Frame integer(final IRI predicate, final Number integer) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return integer == null ? this : integers(predicate, Stream.of(integer));
    }

    public Frame integer(final IRI predicate, final Optional<Number> integer) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( integer == null ) {
            throw new NullPointerException("null integer");
        }

        return integer.map(object -> integers(predicate, Stream.of(object))).orElse(this);
    }


    public Frame integers(final IRI predicate, final Number... integers) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( integers == null ) {
            throw new NullPointerException("null integers");
        }

        return integers.length == 0 ? this : integers(predicate, Arrays.stream(integers));
    }

    public Frame integers(final IRI predicate, final Collection<Number> integers) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( integers == null ) {
            throw new NullPointerException("null integers");
        }

        return integers.isEmpty() ? this : integers(predicate, integers.stream());
    }

    public Frame integers(final IRI predicate, final Stream<Number> integers) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( integers == null ) {
            throw new NullPointerException("null integers");
        }

        return values(predicate, integers.map(value

                -> value instanceof BigInteger ? (BigInteger)value
                : value instanceof BigDecimal ? ((BigDecimal)value).toBigInteger()
                : BigInteger.valueOf(value.longValue())

        ).map(Values::literal));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<BigDecimal> decimal(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return decimal(seq(predicate));
    }

    public Optional<BigDecimal> decimal(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return value(shift).flatMap(Values::decimal);
    }


    public Stream<BigDecimal> decimals(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return decimals(seq(predicate));
    }

    public Stream<BigDecimal> decimals(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return values(shift).map(Values::decimal).filter(Optional::isPresent).map(Optional::get);
    }


    public Frame decimal(final IRI predicate, final Number decimal) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return decimal == null ? this : decimals(predicate, Stream.of(decimal));
    }

    public Frame decimal(final IRI predicate, final Optional<Number> decimal) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( decimal == null ) {
            throw new NullPointerException("null decimal");
        }

        return decimal.map(object -> decimals(predicate, Stream.of(object))).orElse(this);
    }


    public Frame decimals(final IRI predicate, final Number... decimals) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( decimals == null ) {
            throw new NullPointerException("null decimals");
        }

        return decimals.length == 0 ? this : decimals(predicate, Arrays.stream(decimals));
    }

    public Frame decimals(final IRI predicate, final Collection<Number> decimals) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( decimals == null ) {
            throw new NullPointerException("null decimals");
        }

        return decimals.isEmpty() ? this : decimals(predicate, decimals.stream());
    }

    public Frame decimals(final IRI predicate, final Stream<Number> decimals) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( decimals == null ) {
            throw new NullPointerException("null decimals");
        }


        return values(predicate, decimals.map(value

                -> value instanceof BigDecimal ? (BigDecimal)value
                : value instanceof BigInteger ? new BigDecimal((BigInteger)value)
                : BigDecimal.valueOf(value.doubleValue())

        ).map(Values::literal));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<String> string(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return string(seq(predicate));
    }

    public Optional<String> string(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return value(shift).map(Value::stringValue);
    }


    public Stream<String> strings(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return strings(seq(predicate));
    }

    public Stream<String> strings(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return values(shift).map(Value::stringValue);
    }


    public Frame string(final IRI predicate, final String string) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return string == null ? this : strings(predicate, Stream.of(string));
    }

    public Frame string(final IRI predicate, final Optional<String> string) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( string == null ) {
            throw new NullPointerException("null string");
        }

        return string.map(object -> strings(predicate, Stream.of(object))).orElse(this);
    }


    public Frame strings(final IRI predicate, final String... strings) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( strings == null ) {
            throw new NullPointerException("null strings");
        }

        return strings.length == 0 ? this : strings(predicate, Arrays.stream(strings));
    }

    public Frame strings(final IRI predicate, final Collection<String> strings) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( strings == null ) {
            throw new NullPointerException("null strings");
        }

        return strings.isEmpty() ? this : strings(predicate, strings.stream());
    }

    public Frame strings(final IRI predicate, final Stream<String> strings) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( strings == null ) {
            throw new NullPointerException("null strings");
        }

        return values(predicate, strings.map(Values::literal));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Value> value(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return values(predicate).findFirst();
    }

    public Optional<Value> value(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return values(shift).findFirst();
    }


    public Stream<Value> values(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return frames(predicate).map(frame -> frame.focus);
    }

    public Stream<Value> values(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return frames(shift).map(frame -> frame.focus);
    }


    public Frame value(final IRI predicate, final Value value) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return value == null ? this : values(predicate, Stream.of(value));
    }

    public Frame value(final IRI predicate, final Optional<? extends Value> value) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return value.map(object -> values(predicate, Stream.of(object))).orElse(this);
    }


    public Frame values(final IRI predicate, final Value... values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return values.length == 0 ? this : values(predicate, Arrays.stream(values));
    }

    public Frame values(final IRI predicate, final Collection<? extends Value> values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return values.isEmpty() ? this : values(predicate, values.stream());
    }

    public Frame values(final IRI predicate, final Stream<? extends Value> values) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return frames(predicate, values.map(value -> new Frame(value, Set.of())));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Frame> frame(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return frame(seq(predicate));
    }

    public Optional<Frame> frame(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return frames(shift).findFirst();
    }


    public Stream<Frame> frames(final IRI predicate) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return traverse(predicate,

                direct -> model.stream()
                        .filter(statement -> statement.getSubject().equals(focus))
                        .filter(statement -> statement.getPredicate().equals(direct))
                        .map(Statement::getObject),

                inverse -> model.stream()
                        .filter(statement -> statement.getObject().equals(focus))
                        .filter(statement -> statement.getPredicate().equals(inverse))
                        .map(Statement::getSubject)

        ).map(value -> new Frame(value, model));
    }

    public Stream<Frame> frames(final Shift shift) {

        if ( shift == null ) {
            throw new NullPointerException("null shift");
        }

        return shift.map(new ShiftEvaluator(this));
    }


    public Frame frame(final IRI predicate, final Frame frame) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        return frame == null ? this : frames(predicate, Stream.of(frame));
    }

    public Frame frame(final IRI predicate, final Optional<Frame> frame) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( frame == null ) {
            throw new NullPointerException("null frame");
        }

        return frame.map(object -> frames(predicate, Stream.of(object))).orElse(this);
    }


    public Frame frames(final IRI predicate, final Frame... frames) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( frames == null ) {
            throw new NullPointerException("null frames");
        }

        return frames.length == 0 ? this : frames(predicate, Arrays.stream(frames));
    }

    public Frame frames(final IRI predicate, final Collection<Frame> frames) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( frames == null ) {
            throw new NullPointerException("null frames");
        }

        return frames.isEmpty() ? this : frames(predicate, frames.stream());
    }

    public Frame frames(final IRI predicate, final Stream<Frame> frames) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( frames == null ) {
            throw new NullPointerException("null frames");
        }

        return new Frame(focus, Stream

                .concat(model.stream(), frames.flatMap(frame -> traverse(predicate,

                        direct -> focus.isResource() ? Stream.concat(
                                Stream.of(statement((Resource)focus, direct, frame.focus)),
                                frame.model.stream()
                        ) : Stream.empty(),

                        inverse -> frame.focus.isResource() ? Stream.concat(
                                Stream.of(statement((Resource)frame.focus, inverse, focus)),
                                frame.model.stream()
                        ) : Stream.empty()

                )))

                .collect(toCollection(LinkedHashSet::new))

        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public boolean equals(final Object object) {
        return this == object || object instanceof Frame
                && focus.equals(((Frame)object).focus)
                && model.equals(((Frame)object).model);
    }

    @Override public int hashCode() {
        return focus.hashCode()
                ^model.hashCode();
    }

    @Override public String toString() {
        return format(focus)
                +label().map(l -> String.format(" : %s", Strings.fold(Strings.excerpt(l)))).orElse("")
                +notes().map(n -> String.format(" / %s", Strings.fold(Strings.excerpt(n)))).orElse("")
                +String.format(" [%d]", model.size());
    }

}
