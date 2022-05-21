
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

package com.metreeca.rdf.actions;

import com.metreeca.link.Values;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Values.literal;
import static com.metreeca.link.Values.statement;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.stream;

/**
 * RDF value normalization.
 *
 * <p>Normalizes RDF object values.</p>
 */
public final class Normalize implements Function<Statement, Statement> {

	private final List<Function<Value, Optional<Value>>> normalizers;


	/**
	 * Creates an RDF value normalization action.
	 *
	 * @param normalizers the normaliser to be iteratively applied
	 *
	 * @throws NullPointerException if {@code normalizers} is null or contains null elements
	 */
	@SafeVarargs public Normalize(final Function<Value, Optional<Value>>... normalizers) {

		if ( normalizers == null || stream(normalizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null normalizers");
		}

		this.normalizers=List.of(normalizers);

	}

	/**
	 * Creates an RDF value normalization action.
	 *
	 * @param normalizers the normaliser to be iteratively applied
	 *
	 * @throws NullPointerException if {@code normalizers} is null or contains null elements
	 */
	public Normalize(final Collection<Function<Value, Optional<Value>>> normalizers) {

		if ( normalizers == null || normalizers.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null normalizers");
		}

		this.normalizers=List.copyOf(normalizers);
	}


	@Override public Statement apply(final Statement statement) {

		Value object=statement.getObject();

		for (final Function<Value, Optional<Value>> normalizer : normalizers) {
			object=normalizer.apply(object).orElse(object);
		}

		return statement(statement.getSubject(), statement.getPredicate(), object);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * String to date normalization.
	 *
	 * <p>Converts string values matching a given regular expression into {@code xsd:date} values.</p>
	 */
	public static final class StringToDate implements Function<Value, Optional<Value>> {

		private static final String Date="\\d{1,2}/\\d{1,2}/\\d{4}";

		private static final String EUFormat="d/M/yyyy";
		private static final String USFormat="M/d/yyyy";

		private final Pattern DatePattern;
		private final DateTimeFormatter DateFormatter;


		/**
		 * Creates a string to EU date {@value #EUFormat}) normalizer.
		 */
		public StringToDate() {
			this(false);
		}

		/**
		 * Creates a string to date normalizer.
		 *
		 * @param US if {@code true}, converts textual US dates({@value #USFormat}); if {@code false}, converts textual
		 *           EU dates ({@value #EUFormat})
		 */
		public StringToDate(final boolean US) {
			this(Date, US ? USFormat : EUFormat);
		}

		/**
		 * Creates a string to date normalizer.
		 *
		 * @param pattern the pattern used to identify textual dates
		 * @param format  the {@linkplain DateTimeFormatter date format} used to parse strings matching {@code pattern}
		 *                into date value
		 *
		 * @throws NullPointerException     if either {@code pattern} or {@code format} is nul
		 * @throws IllegalArgumentException if either {@code pattern} or {@code format} is malformed
		 */
		public StringToDate(final String pattern, final String format) {

			if ( pattern == null ) {
				throw new NullPointerException("null pattern");
			}

			if ( format == null ) {
				throw new NullPointerException("null format");
			}

			DatePattern=Pattern.compile(pattern);
			DateFormatter=DateTimeFormatter.ofPattern(format);
		}


		@Override public Optional<Value> apply(final Value value) {
			return literal(value)

					.filter(object -> object.getDatatype().equals(XSD.STRING))

					.map(Value::stringValue)
					.map(DatePattern::matcher)
					.filter(Matcher::matches)

					.map(Matcher::group)
					.map(date -> LocalDate.parse(date, DateFormatter))
					.map(Values::literal);
		}
	}

	/**
	 * Date to date-time normalization.
	 *
	 * <p>Converts {@code xsd:date} values into {@link ZoneOffset#UTC} {@code xsd:dateTime} values.</p>
	 */
	public static final class DateToDateTime implements Function<Value, Optional<Value>> {

		public Optional<Value> apply(final Value value) {
			return literal(value)

					.filter(object -> object.getDatatype().equals(XSD.DATE))

					.flatMap(Values::temporalAccessor)
					.map(date -> LocalDate.from(date).atStartOfDay(UTC))
					.map(Values::literal);
		}

	}

}
