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

package com.metreeca.rdf4j;

import com.metreeca.core.toolkits.Snippets;
import com.metreeca.link.Values;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.metreeca.core.toolkits.Snippets.*;
import static com.metreeca.core.toolkits.Strings.quote;
import static com.metreeca.link.Values.format;
import static com.metreeca.link.Values.traverse;

import static java.util.Arrays.stream;

/**
 * SPARQL query composer.
 */
public final class SPARQL {

    public static Consumer<Appendable> comment(final String text) {
        return space(text("# %s", text));
    }


    public static Consumer<Appendable> base(final String base) {
        return space(text("base <%s>", base));
    }

    public static Consumer<Appendable> prefix(final Namespace namespace) {
        return prefix(namespace.getPrefix(), namespace.getName());
    }

    public static Consumer<Appendable> prefix(final String prefix, final String name) {
        return line(text("prefix %s: <%s>", prefix, name));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs public static Consumer<Appendable> select(final Consumer<Appendable>... vars) {
        return select(false, vars);
    }

    @SafeVarargs public static Consumer<Appendable> select(final boolean distinct, final Consumer<Appendable>... vars) {
        return list(text("\rselect"),
                distinct ? text(" distinct") : nothing(),
                vars.length == 0 ? text(" *") : list(vars)
        );
    }


    @SafeVarargs public static Consumer<Appendable> construct(final Consumer<Appendable>... patterns) {
        return list(text("\rconstruct"), list(patterns));
    }


    @SafeVarargs public static Consumer<Appendable> where(final Consumer<Appendable>... pattern) {
        return list(text("\rwhere"), block(pattern));
    }


    @SafeVarargs public static Consumer<Appendable> group(final Consumer<Appendable>... expressions) {
        return list(text(" group by"), list(expressions));
    }

    @SafeVarargs public static Consumer<Appendable> having(final Consumer<Appendable>... expressions) {
        return list(text(" having ( "), list(expressions), text(" )"));
    }

    @SafeVarargs public static Consumer<Appendable> order(final Consumer<Appendable>... expressions) {
        return list(text(" order by"), list(expressions));
    }

    public static Consumer<Appendable> sort(final boolean inverse, final Consumer<Appendable> expression) {
        return inverse ? desc(expression) : asc(expression);
    }

    public static Consumer<Appendable> asc(final Consumer<Appendable> expression) {
        return list(text(" asc("), expression, text(")"));
    }

    public static Consumer<Appendable> desc(final Consumer<Appendable> expression) {
        return list(text(" desc("), expression, text(")"));
    }

    public static Consumer<Appendable> offset(final int offset) {
        return offset > 0 ? text(" offset %d", offset) : nothing();
    }

    public static Consumer<Appendable> limit(final int limit) {
        return limit(limit, 0);
    }

    public static Consumer<Appendable> limit(final int limit, final int sampling) {
        return limit > 0 ? text(" limit %d", sampling > 0 ? Math.min(limit, sampling) : limit)
                : sampling > 0 ? text(" limit %d", sampling)
                : nothing();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs public static Consumer<Appendable> union(final Consumer<Appendable>... patterns) {
        return union(stream(patterns));
    }

    public static Consumer<Appendable> union(final Collection<Consumer<Appendable>> patterns) {
        return union(patterns.stream());
    }

    public static Consumer<Appendable> union(final Stream<Consumer<Appendable>> patterns) {
        return list(patterns.flatMap(pattern -> Stream.of(text(" union "), pattern)).skip(1));
    }

    @SafeVarargs public static Consumer<Appendable> optional(final Consumer<Appendable>... pattern) {
        return list(text("optional"), block(pattern));
    }

    public static Consumer<Appendable> values(final Consumer<Appendable> var, final Collection<Value> values) {
        return list(text("\rvalues"), var, block(list(
                values.stream().map(Values::format).map(Snippets::text).map(Snippets::line)
        )));
    }

    @SafeVarargs public static Consumer<Appendable> filter(final Consumer<Appendable>... expressions) {
        return list(text(" filter ( "), list(expressions), text(" )"));
    }

    public static Consumer<Appendable> bind(final String id, final Consumer<Appendable> expression) {
        return line(list(text(" bind"), as(id, expression)));
    }

    public static Consumer<Appendable> as(final String id, final Consumer<Appendable> expression) {
        return list(text(" ("), expression, text(" as "), var(id), text(')'));
    }

    public static Consumer<Appendable> var(final String id) {
        return text(" ?%s", id);
    }

    public static Consumer<Appendable> string(final String text) {
        return text(quote(text));
    }


    public static Consumer<Appendable> edge(final Consumer<Appendable> source, final String path,
            final Consumer<Appendable> target) {
        return edge(source, text(path), target);
    }

    public static Consumer<Appendable> edge(final Consumer<Appendable> source, final IRI path,
            final Consumer<Appendable> target) {
        return traverse(path,
                iri -> edge(source, value(iri), target),
                iri -> edge(target, value(iri), source)
        );
    }

    public static Consumer<Appendable> edge(final Consumer<Appendable> source,
            final Consumer<Appendable> path, final Consumer<Appendable> target) {
        return list(text(' '), source, text(' '), path, text(' '), target, text(" ."));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Consumer<Appendable> min(final Consumer<Appendable> expression) {
        return list(text(" min("), expression, text(")"));
    }

    public static Consumer<Appendable> max(final Consumer<Appendable> expression) {
        return list(text(" max("), expression, text(")"));
    }

    public static Consumer<Appendable> count(final Consumer<Appendable> expression) {
        return count(false, expression);
    }

    public static Consumer<Appendable> count(final boolean distinct, final Consumer<Appendable> expression) {
        return list(text(" count("), distinct ? text("distinct ") : nothing(), expression, text(")"));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Consumer<Appendable> is(final Consumer<Appendable> test,
            final Consumer<Appendable> pass, final Consumer<Appendable> fail) {
        return list(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
    }

    public static Consumer<Appendable> isBlank(final Consumer<Appendable> expression) {
        return function("isBlank", expression);
    }

    public static Consumer<Appendable> isIRI(final Consumer<Appendable> expression) {
        return function("isIRI", expression);
    }

    public static Consumer<Appendable> isLiteral(final Consumer<Appendable> expression) {
        return function("isLiteral", expression);
    }

    public static Consumer<Appendable> bound(final Consumer<Appendable> expression) {
        return function("bound", expression);
    }

    public static Consumer<Appendable> lang(final Consumer<Appendable> expression) {
        return function("lang", expression);
    }

    public static Consumer<Appendable> datatype(final Consumer<Appendable> expression) {
        return function("datatype", expression);
    }

    public static Consumer<Appendable> str(final Consumer<Appendable> expression) {
        return function("str", expression);
    }

    public static Consumer<Appendable> strlen(final Consumer<Appendable> expression) {
        return function("strlen", expression);
    }

    public static Consumer<Appendable> strstarts(final Consumer<Appendable> expression,
            final Consumer<Appendable> prefix) {
        return function("strstarts", expression, prefix);
    }

    public static Consumer<Appendable> regex(final Consumer<Appendable> expression,
            final Consumer<Appendable> pattern) {
        return function("regex", expression, pattern);
    }

    public static Consumer<Appendable> regex(final Consumer<Appendable> expression,
            final Consumer<Appendable> pattern, final Consumer<Appendable> flags) {
        return function("regex", expression, pattern, flags);
    }


    @SafeVarargs public static Consumer<Appendable> or(final Consumer<Appendable>... expressions) {
        return list(" || ", expressions);
    }

    @SafeVarargs public static Consumer<Appendable> and(final Consumer<Appendable>... expressions) {
        return list(" && ", expressions);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Consumer<Appendable> eq(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, "=", y);
    }

    public static Consumer<Appendable> neq(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, "!=", y);
    }

    public static Consumer<Appendable> lt(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, "<", y);
    }

    public static Consumer<Appendable> gt(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, ">", y);
    }

    public static Consumer<Appendable> lte(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, "<=", y);
    }

    public static Consumer<Appendable> gte(final Consumer<Appendable> x, final Consumer<Appendable> y) {
        return op(x, ">=", y);
    }

    public static Consumer<Appendable> in(final Consumer<Appendable> expression,
            final Stream<Consumer<Appendable>> expressions) {
        return list(expression, text(" in ("), list(", ", expressions), text(')'));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SafeVarargs private static Consumer<Appendable> function(final String name, final Consumer<Appendable>... args) {
        return list(text(' '), text(name), text('('), list(", ", args), text(')'));
    }

    private static Consumer<Appendable> op(final Consumer<Appendable> x, final String name,
            final Consumer<Appendable> y) {
        return list(x, text(' '), text(name), text(' '), y);
    }

    public static Consumer<Appendable> value(final Value value) {

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return text(format(value));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private SPARQL() { }

}
