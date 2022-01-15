/*
 * Copyright © 2021 EC2U Consortium. All rights reserved.
 */

package com.metreeca.rest.actions;

import com.metreeca.rest.Xtream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.*;

import static java.lang.Integer.parseInt;
import static java.util.Map.entry;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * JSONPath-based JSON value processing.
 *
 * <p>Maps JSON values to values produced by a function taking as argument a value-targeted JSONPath processor.</p>
 *
 * @param <R> the type of the value returned by the processing action
 */
public final class JSONPath<R> implements Function<JsonValue, R> {

	private static final String Wildcard="*";


	private static Optional<JsonObject> object(final JsonValue value) {
		return Optional.ofNullable(value)
				.filter(JsonObject.class::isInstance)
				.map(JsonObject.class::cast);
	}

	private static Optional<JsonArray> array(final JsonValue value) {
		return Optional.ofNullable(value)
				.filter(JsonArray.class::isInstance)
				.map(JsonArray.class::cast);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Processor, R> query;


	/**
	 * Creates an JSONPath-based JSON value processing action.
	 *
	 * @param query a function taking as argument a processor and returning a value
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public JSONPath(final Function<Processor, R> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.query=query;
	}


	@Override public R apply(final JsonValue value) {
		return query.apply(new Processor(value));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * JSONPath processor.
	 *
	 * <p>Applies JSONPath expression to a target JSON value.</p>
	 */
	public static final class Processor {

		private final JsonValue target;


		/**
		 * Creates a JSONPath processor.
		 *
		 * @param target the target JSON value for the processor
		 *
		 * @throws NullPointerException if {@code value} is null
		 */
		public Processor(final JsonValue target) {

			if ( target == null ) {
				throw new NullPointerException("null node");
			}

			this.target=target;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		public Optional<Boolean> bool(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return bools(path).findFirst();
		}

		public Xtream<Boolean> bools(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return values(path)
					.map(v -> v == JsonValue.TRUE ? Boolean.TRUE : v == JsonValue.FALSE ? Boolean.FALSE : null)
					.filter(Objects::nonNull);
		}


		public Optional<BigInteger> integer(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return integers(path).findFirst();
		}

		public Xtream<BigInteger> integers(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return values(path)
					.filter(JsonNumber.class::isInstance)
					.map(JsonNumber.class::cast)
					.map(JsonNumber::bigIntegerValue);
		}


		public Optional<BigDecimal> decimal(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return decimals(path).findFirst();
		}

		public Xtream<BigDecimal> decimals(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return values(path)
					.filter(JsonNumber.class::isInstance)
					.map(JsonNumber.class::cast)
					.map(JsonNumber::bigDecimalValue);
		}


		/**
		 * Retrieves a string from the target value.
		 *
		 * @param path the JSONPath expression to be evaluated against the target value
		 *
		 * @return an optional non-empty string produced by evaluating {@code path} against the target value of, if one
		 * was available and not empty; an empty optional, otherwise
		 *
		 * @throws NullPointerException if {@code path} is null
		 */
		public Optional<String> string(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null XPath expression");
			}

			return strings(path).findFirst();
		}

		/**
		 * Retrieves strings from the target value.
		 *
		 * @param path the JSONPath expression to be evaluated against the target value
		 *
		 * @return a stream of non-empty strings produced by evaluating {@code path} against the target value
		 *
		 * @throws NullPointerException if {@code path} is null
		 */
		public Xtream<String> strings(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null XPath expression");
			}

			return values(path)
					.map(v -> v instanceof JsonString ? ((JsonString)v).getString() : "")
					.filter(s -> !s.isEmpty());
		}


		public Xtream<Map.Entry<String, Processor>> entries(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return values(path).optMap(JSONPath::object).flatMap(object -> object.entrySet().stream()).map(entry ->
					entry(entry.getKey(), new Processor(entry.getValue()))
			);
		}


		/**
		 * Retrieves a JSON value from the target value.
		 *
		 * @param path the JSONPath expression to be evaluated against the target node
		 *
		 * @return an optional JSON value produced by evaluating {@code path} against the target value, if one was
		 * available; an empty optional, otherwise
		 *
		 * @throws NullPointerException if {@code path} is null
		 */
		public Optional<JsonValue> value(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null XPath expression");
			}

			return values(path).findFirst();
		}

		/**
		 * Retrieves JSON values from the target value.
		 *
		 * @param path the JSONPath expression to be evaluated against the target value
		 *
		 * @return a stream of JSON values produced by evaluating {@code path} against the target node
		 *
		 * @throws NullPointerException if {@code path} is null
		 */
		public Xtream<JsonValue> values(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null XPath expression");
			}

			Xtream<JsonValue> values=Xtream.of(target);

			for (final String step : Arrays
					.stream(path.split("\\."))
					.map(String::trim)
					.filter(not(String::isEmpty))
					.collect(toList())
			) {
				values=values.flatMap(value ->

						object(value).map(object ->

								step.equals(Wildcard) ? object.values().stream()
										: Stream.of(object.get(step))

						).or(() -> array(value).map(array ->

								step.equals(Wildcard) ? array.stream()
										: step.matches("\\d+") ? Stream.of(array.get(parseInt(step))) // !!! bounds
										: Stream.<JsonValue>empty()

						)).orElse(

								Xtream.empty()

						)

				).filter(Objects::nonNull);
			}

			return values;
		}

	}

}
