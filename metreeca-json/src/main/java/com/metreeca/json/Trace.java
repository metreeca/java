/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.json;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;


/**
 * Shape validation trace.
 */
public final class Trace {

	private static final Trace EmptyTrace=new Trace(Stream.empty(), Stream.empty());


	public static Trace trace() {
		return EmptyTrace;
	}

	public static Trace trace(final Trace... traces) {return trace(asList(traces));}

	public static Trace trace(final Collection<Trace> traces) {

		if ( traces == null || traces.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null traces");
		}

		return new Trace(
				traces.stream().flatMap(trace -> trace.issues.entrySet().stream()),
				traces.stream().flatMap(trace -> trace.fields.entrySet().stream())
		);
	}

	public static Trace trace(final String issue, final JsonValue... values) {
		return trace(issue, asList(values));
	}

	public static Trace trace(final String issue, final Collection<JsonValue> values) {
		return new Trace(singletonMap(issue, values).entrySet().stream(), Stream.empty());
	}

	public static Trace trace(final Map<String, Collection<JsonValue>> issues) {
		return new Trace(issues.entrySet().stream(), Stream.empty());
	}

	public static Trace trace(final Map<String, Collection<JsonValue>> issues, final Map<String, Trace> fields) {
		return new Trace(issues.entrySet().stream(), fields.entrySet().stream());
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Collection<JsonValue>> issues;
	private final Map<String, Trace> fields;


	private Trace(
			final Stream<Map.Entry<String, Collection<JsonValue>>> issues,
			final Stream<Map.Entry<String, Trace>> fields
	) {

		this.issues=issues
				.filter(issue -> !issue.getKey().isEmpty())
				.collect(toMap(
						Map.Entry::getKey, entry -> unmodifiableSet(new LinkedHashSet<>(entry.getValue())),
						(x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toCollection(LinkedHashSet::new)),
						LinkedHashMap::new
				));

		this.fields=fields
				.filter(field -> !field.getValue().empty())
				.collect(toMap(
						Map.Entry::getKey, Map.Entry::getValue,
						Trace::trace,
						LinkedHashMap::new
				));
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean empty() {
		return issues.isEmpty() && fields.isEmpty();
	}


	public Map<String, Collection<JsonValue>> issues() {
		return unmodifiableMap(issues);
	}

	public Map<String, Trace> fields() {
		return unmodifiableMap(fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public JsonObject toJSON() {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		if ( !issues().isEmpty() ) {

			final JsonObjectBuilder errors=Json.createObjectBuilder();

			issues().forEach((detail, values) -> {

				final JsonArrayBuilder objects=Json.createArrayBuilder();

				values.forEach(value -> {

					if ( value != null ) { objects.add(value); }

				});

				errors.add(detail, objects.build());

			});

			builder.add("", errors);
		}

		fields().forEach((name, nested) -> {

			if ( !nested.empty() ) {
				builder.add(name, nested.toJSON());
			}

		});

		return builder.build();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		try ( final StringWriter writer=new StringWriter() ) {

			Json
					.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true))
					.createWriter(writer)
					.write(toJSON());

			return writer.toString();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

}
