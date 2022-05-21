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

package com.metreeca.rdf4j;

import com.metreeca.link.Values;
import com.metreeca.rest._Scribe;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.core.Strings.quote;
import static com.metreeca.link.Values.traverse;
import static com.metreeca.rest._Scribe.*;

import static java.util.Arrays.stream;

/**
 * SPARQL query composer.
 */
public final class SPARQLScribe {

	public static _Scribe comment(final String text) {
		return space(text("# %s", text));
	}


	public static _Scribe base(final String base) {
		return space(text("base <%s>", base));
	}

	public static _Scribe prefix(final Namespace namespace) {
		return prefix(namespace.getPrefix(), namespace.getName());
	}

	public static _Scribe prefix(final String prefix, final String name) {
		return line(text("prefix %s: <%s>", prefix, name));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static _Scribe select(final _Scribe... vars) {
		return select(false, vars);
	}

	public static _Scribe select(final boolean distinct, final _Scribe... vars) {
		return list(text("\rselect"),
				distinct ? text(" distinct") : nothing(),
				vars.length == 0 ? text(" *") : list(vars)
		);
	}


	public static _Scribe construct(final _Scribe... patterns) {
		return list(text("\rconstruct"), list(patterns));
	}


	public static _Scribe where(final _Scribe... pattern) {
		return list(text("\rwhere"), block(pattern));
	}


	public static _Scribe group(final _Scribe... expressions) {
		return list(text(" group by"), list(expressions));
	}

	public static _Scribe having(final _Scribe... expressions) {
		return list(text(" having ( "), list(expressions), text(" )"));
	}

	public static _Scribe order(final _Scribe... expressions) {
		return list(text(" order by"), list(expressions));
	}

	public static _Scribe sort(final boolean inverse, final _Scribe expression) {
		return inverse ? desc(expression) : asc(expression);
	}

	public static _Scribe asc(final _Scribe expression) {
		return list(text(" asc("), expression, text(")"));
	}

	public static _Scribe desc(final _Scribe expression) {
		return list(text(" desc("), expression, text(")"));
	}

	public static _Scribe offset(final int offset) {
		return offset > 0 ? text(" offset %d", offset) : nothing();
	}

	public static _Scribe limit(final int limit) {
		return limit(limit, 0);
	}

	public static _Scribe limit(final int limit, final int sampling) {
		return limit > 0 ? text(" limit %d", sampling > 0 ? Math.min(limit, sampling) : limit)
				: sampling > 0 ? text(" limit %d", sampling)
				: nothing();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static _Scribe union(final _Scribe... patterns) {
		return union(stream(patterns));
	}

	public static _Scribe union(final Collection<_Scribe> patterns) {
		return union(patterns.stream());
	}

	public static _Scribe union(final Stream<_Scribe> patterns) {
		return list(patterns.flatMap(pattern -> Stream.of(text(" union "), pattern)).skip(1));
	}

	public static _Scribe optional(final _Scribe... pattern) {
		return list(text("optional"), block(pattern));
	}

	public static _Scribe values(final _Scribe var, final Collection<Value> values) {
		return list(text("\rvalues"), var, block(list(
				values.stream().map(Values::format).map(_Scribe::text).map(_Scribe::line)
		)));
	}

	public static _Scribe filter(final _Scribe... expressions) {
		return list(text(" filter ( "), list(expressions), text(" )"));
	}

	public static _Scribe bind(final String id, final _Scribe expression) {
		return line(list(text(" bind"), as(id, expression)));
	}

	public static _Scribe as(final String id, final _Scribe expression) {
		return list(text(" ("), expression, text(" as "), var(id), text(')'));
	}

	public static _Scribe var(final String id) {
		return text(" ?%s", id);
	}

	public static _Scribe string(final String text) {
		return text(quote(text));
	}


	public static _Scribe edge(final _Scribe source, final String path, final _Scribe target) {
		return edge(source, text(path), target);
	}

	public static _Scribe edge(final _Scribe source, final IRI path, final _Scribe target) {
		return traverse(path,
				iri -> edge(source, text(iri), target),
				iri -> edge(target, text(iri), source)
		);
	}

	public static _Scribe edge(final _Scribe source, final _Scribe path, final _Scribe target) {
		return list(text(' '), source, text(' '), path, text(' '), target, text(" ."));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static _Scribe min(final _Scribe expression) {
		return list(text(" min("), expression, text(")"));
	}

	public static _Scribe max(final _Scribe expression) {
		return list(text(" max("), expression, text(")"));
	}

	public static _Scribe count(final _Scribe expression) {
		return count(false, expression);
	}

	public static _Scribe count(final boolean distinct, final _Scribe expression) {
		return list(text(" count("), distinct ? text("distinct ") : nothing(), expression, text(")"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static _Scribe is(final _Scribe test, final _Scribe pass, final _Scribe fail) {
		return list(text(" if("), test, text(", "), pass, text(", "), fail, text(")"));
	}

	public static _Scribe isBlank(final _Scribe expression) {
		return function("isBlank", expression);
	}

	public static _Scribe isIRI(final _Scribe expression) {
		return function("isIRI", expression);
	}

	public static _Scribe isLiteral(final _Scribe expression) {
		return function("isLiteral", expression);
	}

	public static _Scribe bound(final _Scribe expression) {
		return function("bound", expression);
	}

	public static _Scribe lang(final _Scribe expression) {
		return function("lang", expression);
	}

	public static _Scribe datatype(final _Scribe expression) {
		return function("datatype", expression);
	}

	public static _Scribe str(final _Scribe expression) {
		return function("str", expression);
	}

	public static _Scribe strlen(final _Scribe expression) {
		return function("strlen", expression);
	}

	public static _Scribe strstarts(final _Scribe expression, final _Scribe prefix) {
		return function("strstarts", expression, prefix);
	}

	public static _Scribe regex(final _Scribe expression, final _Scribe pattern) {
		return function("regex", expression, pattern);
	}

	public static _Scribe regex(final _Scribe expression, final _Scribe pattern, final _Scribe flags) {
		return function("regex", expression, pattern, flags);
	}


	public static _Scribe or(final _Scribe... expressions) {
		return list(" || ", expressions);
	}

	public static _Scribe and(final _Scribe... expressions) {
		return list(" && ", expressions);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static _Scribe eq(final _Scribe x, final _Scribe y) {
		return op(x, "=", y);
	}

	public static _Scribe neq(final _Scribe x, final _Scribe y) {
		return op(x, "!=", y);
	}

	public static _Scribe lt(final _Scribe x, final _Scribe y) {
		return op(x, "<", y);
	}

	public static _Scribe gt(final _Scribe x, final _Scribe y) {
		return op(x, ">", y);
	}

	public static _Scribe lte(final _Scribe x, final _Scribe y) {
		return op(x, "<=", y);
	}

	public static _Scribe gte(final _Scribe x, final _Scribe y) {
		return op(x, ">=", y);
	}

	public static _Scribe in(final _Scribe expression, final Stream<_Scribe> expressions) {
		return list(expression, text(" in ("), list(", ", expressions), text(')'));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static _Scribe function(final String name, final _Scribe... args) {
		return list(text(' '), text(name), text('('), list(", ", args), text(')'));
	}

	private static _Scribe op(final _Scribe x, final String name, final _Scribe y) {
		return list(x, text(' '), text(name), text(' '), y);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private SPARQLScribe() {}

}
