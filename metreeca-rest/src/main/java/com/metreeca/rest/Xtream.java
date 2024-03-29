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

package com.metreeca.rest;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;


/**
 * Extended data processing stream.
 *
 * @param <T> the type of the extended stream elements
 */
public final class Xtream<T> implements Stream<T> {

	/**
	 * Creates an empty sequential extended stream.
	 *
	 * @param <T> the type of stream elements
	 *
	 * @return an empty sequential extended stream
	 */
	public static <T> Xtream<T> empty() {
		return from(Stream.empty());
	}


	/**
	 * Creates a sequential extended stream containing a single element.
	 *
	 * @param element the single element to be included in the new extended stream
	 * @param <T>     the type of stream elements
	 *
	 * @return a singleton sequential extended stream
	 */
	public static <T> Xtream<T> of(final T element) {
		return from(Stream.of(element));
	}

	/**
	 * Creates a sequential ordered extended stream containing the specified elements.
	 *
	 * @param <T>      the type of stream elements
	 * @param elements the elements to be included in the new extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code elements} is null
	 */
	@SafeVarargs public static <T> Xtream<T> of(final T... elements) {

		if ( elements == null ) {
			throw new NullPointerException("null elements");
		}

		return from(Stream.of(elements));
	}


	/**
	 * Creates a sequential ordered extended stream containing the elements of the specified collection.
	 *
	 * @param <T>        the type of stream elements
	 * @param collection the collection whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code collection} is null
	 */
	public static <T> Xtream<T> from(final Collection<T> collection) {

		if ( collection == null ) {
			throw new NullPointerException("null collection");
		}

		return from(collection.stream());
	}

	/**
	 * Creates a sequential ordered extended stream containing the elements of the specified collections.
	 *
	 * @param <T>         the type of stream elements
	 * @param collections the collections whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code collections} is null or contains null elements
	 */
	@SafeVarargs public static <T> Xtream<T> from(final Collection<T>... collections) {

		if ( collections == null || Arrays.stream(collections).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null collections");
		}

		return from(Arrays.stream(collections).flatMap(Collection::stream));
	}


	/**
	 * Creates a extended stream containing the elements of the specified stream.
	 *
	 * @param <T>    the type of stream elements
	 * @param stream the stream whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code stream} is null
	 */
	public static <T> Xtream<T> from(final Stream<T> stream) {

		if ( stream == null ) {
			throw new NullPointerException("null stream");
		}

		return stream instanceof Xtream ? (Xtream<T>)stream : new Xtream<>(stream);
	}

	/**
	 * Creates a extended stream containing the elements of the specified streams.
	 *
	 * @param <T>     the type of stream elements
	 * @param streams the streams whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code stream} is null or contains null elements
	 */
	@SafeVarargs public static <T> Xtream<T> from(final Stream<T>... streams) {

		if ( streams == null || Arrays.stream(streams).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null streams");
		}

		return from(Arrays.stream(streams).flatMap(identity()));
	}


	//// Functional Utilities //////////////////////////////////////////////////////////////////////////////////////////

	public static <K, V> Map<K, V> map() {
		return emptyMap();
	}

	@SafeVarargs public static <K, V> Map<K, V> map(final Entry<K, V>... entries) {
		return Arrays.stream(entries).filter(Objects::nonNull).collect(toMap(Entry::getKey, Entry::getValue));
	}

	public static <K, V> Entry<K, V> entry(final K key, final V value) {
		return new SimpleImmutableEntry<>(key, value);
	}


	/**
	 * Converts a consumer into a function.
	 *
	 * @param consumer the consumer to be converted
	 * @param <V>      the type of the value accepted by {@code consumer}
	 * @param <R>      the type returned by the generated function
	 *
	 * @return a function forwarding its input value to {@code supplier} and returning a null value
	 *
	 * @throws NullPointerException if consumer is null
	 */
	public static <V, R> Function<V, R> task(final Consumer<V> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		return value -> {

			consumer.accept(value);

			return null;

		};
	}

	/**
	 * Creates a guarded function.
	 *
	 * @param function the function to be wrapped
	 * @param <V>      the type of the {@code function} input value
	 * @param <R>      the type of the {@code function} return value
	 *
	 * @return a function returning the value produced by applying {@code function} to its input value, if the input
	 * value is not null and no exception is thrown in the process, or {@code null}, otherwise
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V, R> Function<V, R> guarded(final Function<? super V, ? extends R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return value -> {
			try {

				return value == null ? null : function.apply(value);

			} catch ( final RuntimeException e ) {

				return null;

			}
		};

	}

	/**
	 * Creates an autocloseable function.
	 *
	 * @param function the function to be wrapped
	 * @param <V>      the type of the {@code function} input value
	 * @param <R>      the type of the {@code function} return value
	 *
	 * @return a function returning the value produced by applying {@code function} to its input value and closing it
	 * after processing
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V extends AutoCloseable, R> Function<V, R> closing(final Function<? super V, ? extends R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return value -> {

			try ( final V c=value ) {

				return function.apply(c);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final Exception e ) {

				throw new RuntimeException(e);

			}

		};
	}


	//// IO Utilities //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * URL-encode a string.
	 *
	 * @param string the string to be encoded
	 *
	 * @return the encoded version of {@code string}
	 *
	 * @throws NullPointerException if {@code string} is null
	 */
	public static String encode(final String string) {

		if ( string == null ) {
			throw new NullPointerException("null string");
		}

		try {
			return URLEncoder.encode(string, UTF_8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}

	/**
	 * URL-decode a string.
	 *
	 * @param string the string to be decoded
	 *
	 * @return the decoded version of {@code string}
	 *
	 * @throws NullPointerException if {@code string} is null
	 */
	public static String decode(final String string) {

		if ( string == null ) {
			throw new NullPointerException("null string");
		}

		try {
			return URLDecoder.decode(string, UTF_8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}


	/**
	 * Creates an empty input stream.
	 *
	 * @return a new empty input stream
	 */
	public static InputStream input() {
		return new ByteArrayInputStream(new byte[0]);
	}

	/**
	 * Creates an empty reader.
	 *
	 * @return a new empty reader
	 */
	public static Reader reader() {
		return new StringReader("");
	}


	/**
	 * Reads data from an input stream.
	 *
	 * @param input the input stream to be read
	 *
	 * @return the data content read from {@code input}
	 *
	 * @throws NullPointerException if {@code input} is null
	 */
	public static byte[] data(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

			return data(output, input).toByteArray();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	/**
	 * Writes data to an output stream.
	 *
	 * @param <O>    the type of the output stream
	 * @param output the output stream
	 * @param data   the data content to be written to {@code output}
	 *
	 * @return the {@code output} stream
	 *
	 * @throws NullPointerException if either {@code output} or {@code data} is null
	 */
	public static <O extends OutputStream> O data(final O output, final byte[] data) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		try {

			output.write(data);
			output.flush();

			return output;

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	/**
	 * Copies an input stream to an output stream.
	 *
	 * @param <O>    the type of the output stream
	 * @param output the output stream
	 * @param input  the input stream
	 *
	 * @return the {@code output} stream
	 *
	 * @throws NullPointerException if either {@code output} ir {@code input} is null
	 */
	public static <O extends OutputStream> O data(final O output, final InputStream input) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try {

			final byte[] buffer=new byte[1024];

			for (int n; (n=input.read(buffer)) >= 0; output.write(buffer, 0, n)) { }

			output.flush();

			return output;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * Reads text from a reader.
	 *
	 * @param reader the reader to be read
	 *
	 * @return the text content read from {@code reader}
	 *
	 * @throws NullPointerException if {@code reader} is null
	 */
	public static String text(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try ( final StringWriter writer=new StringWriter() ) {

			return text(writer, reader).toString();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	/**
	 * Writes text to writer.
	 *
	 * @param <W>    the type of the writer
	 * @param writer the writer
	 * @param text   the text content to be written to {@code writer}
	 *
	 * @return the {@code writer}
	 *
	 * @throws NullPointerException if either {@code writer} or {@code text} is null
	 */
	public static <W extends Writer> W text(final W writer, final String text) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try {

			writer.write(text);
			writer.flush();

			return writer;

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	/**
	 * Copies a reader to writer.
	 *
	 * @param <W>    the type of the writer
	 * @param writer the writer
	 * @param reader the reader
	 *
	 * @return the {@code writer}
	 *
	 * @throws NullPointerException if either {@code writer} ir {@code reader} is null
	 */
	public static <W extends Writer> W text(final W writer, final Reader reader) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try {

			final char[] buffer=new char[1024];

			for (int n; (n=reader.read(buffer)) >= 0; writer.write(buffer, 0, n)) { }

			writer.flush();

			return writer;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Stream<T> stream; // the delegate plain stream


	private Xtream(final Stream<T> stream) {
		this.stream=stream;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Maps elements to optional values.
	 *
	 * @param mapper a function mapping elements to optional values
	 * @param <R>    the type of the optional value returned by {@code mapper}
	 *
	 * @return an extended stream  produced by applying {@code mapper} to each element of this extended stream and
	 * replacing it with the value of non-null and non-empty optionals
	 *
	 * @throws NullPointerException if {@code mapper} is null
	 */
	public <R> Xtream<R> optMap(final Function<? super T, Optional<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper)
				.filter(Objects::nonNull)
				.filter(Optional::isPresent)
				.map(Optional::get)
		);
	}

	/**
	 * Maps elements to collection of values.
	 *
	 * @param mapper a function mapping elements to collection of values
	 * @param <R>    the type of the value in the collection returned by {@code mapper}
	 *
	 * @return an extended stream produced by applying {@code mapper} to each element of this extended stream and
	 * replacing it with the values of non-null collections
	 *
	 * @throws NullPointerException if {@code mapper} is null
	 */
	public <R> Xtream<R> bagMap(final Function<? super T, ? extends Collection<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
		);
	}


	@SafeVarargs public final <R> Xtream<R> bagMap(final Function<? super T, ? extends Collection<R>>... mappers) {

		if ( mappers == null || Arrays.stream(mappers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null mapper");
		}

		return flatMap(t -> Arrays.stream(mappers)
				.map(mapper -> mapper.apply(t))
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
		);

	}

	@SafeVarargs public final <R> Xtream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>>... mappers) {

		if ( mappers == null || Arrays.stream(mappers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null mappers");
		}

		return flatMap(t -> Arrays.stream(mappers)
				.flatMap(mapper -> mapper.apply(t))
		);
	}


	/**
	 * Removes incompatible elements.
	 *
	 * @param clash a binary predicate returning {@code true} if its arguments are mutually incompatible or {@code
	 * false}
	 *              otherwise
	 *
	 * @return an extended stream produced by removing from this extended stream all elements not compatible with
	 * previously processed elements according to {@code clash}
	 *
	 * @throws NullPointerException if {@code clash} is null
	 */
	public Xtream<T> prune(final BiPredicate<T, T> clash) {

		if ( clash == null ) {
			throw new NullPointerException("null clash");
		}

		return filter(new Predicate<T>() {

			private final Collection<T> matches=new ArrayList<>();

			@Override public boolean test(final T x) {
				synchronized ( matches ) {
					return matches.stream().noneMatch(y -> clash.test(x, y)) && matches.add(x);
				}
			}

		});
	}


	/**
	 * Groups elements.
	 *
	 * @param classifier a function mapping elements to a grouping key
	 * @param <K>        the type of the key returned by {@code classifier}
	 *
	 * @return an extended stream produced by applying {@code classifier} to each element of this extended stream and
	 * returning a stream of entries mapping each grouping key returned by {@code classifier} to the list of the
	 * elements
	 * matching the grouping key
	 *
	 * @throws NullPointerException if {@code classifier} is null
	 */
	public <K> Xtream<Entry<K, List<T>>> groupBy(final Function<T, K> classifier) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		return from(collect(groupingBy(classifier)).entrySet().stream());
	}

	/**
	 * Groups and collects elements.
	 *
	 * @param classifier a function mapping elements to their grouping key
	 * @param downstream a collector for sub-streams of this extended stream
	 * @param <K>        the type of the key returned by {@code classifier}
	 * @param <V>        the type of the value returned by the {@code downstream} collector
	 *
	 * @return an extended stream produced by applying {@code classifier} to each element of this extended stream and
	 * returning a stream of entries mapping each grouping key returned by {@code classifier} to the value produced by
	 * collection the stream of the elements matching the grouping key using the {@code downstream} collector
	 *
	 * @throws NullPointerException if either {@code classifier} or {@code downstream} is null
	 */
	public <K, V> Xtream<Entry<K, V>> groupBy(final Function<T, K> classifier, final Collector<T, ?, V> downstream) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		if ( downstream == null ) {
			throw new NullPointerException("null downstream collector");
		}

		return from(collect(groupingBy(classifier, downstream)).entrySet().stream());
	}


	/**
	 * Batches elements.
	 *
	 * @param size the batch size limit (0 for no limits)
	 *
	 * @return an extended stream produced by collecting the elements of this extended stream in batches of at most
	 * {@code size} elements if {@code size} is greater than 0 or in a single batch otherwise
	 *
	 * @throws IllegalArgumentException if {@code size} is negative
	 */
	public Xtream<Collection<T>> batch(final int size) {

		if ( size < 0 ) {
			throw new IllegalArgumentException("negative batch size");
		}

		return size == 0 ? of(stream.collect(Collectors.toList()))
				: from(StreamSupport.stream(new BatchSpliterator<>(size, stream.spliterator()), stream.isParallel()));
	}

	/**
	 * Batches elements.
	 *
	 * @param collector a stream collector
	 * @param <C>       the type of the collected element
	 *
	 * @return an extended stream containing a single element produced by collecting the elements of this extended
	 * stream
	 * using {@code collector}
	 *
	 * @throws NullPointerException if {@code collector} is null
	 */
	public <C> Xtream<C> batch(final Collector<T, ?, C> collector) {

		if ( collector == null ) {
			throw new NullPointerException("null collector");
		}

		return of(collect(collector));
	}


	/**
	 * Recursively expands this extended stream.
	 *
	 * @param mapper a function mapping elements to streams of elements of the same type
	 *
	 * @return an extended stream produced by recursively applying {@code mapper} to this extended stream and expanding
	 * it with the elements of the returned streams until no new elements are generated; null returned streams are
	 * considered to be empty
	 *
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public Xtream<T> loop(final Function<? super T, ? extends Stream<T>> mapper) { // !!! lazy

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		final Collection<T> visited=new LinkedHashSet<>(); // preserve order

		for (

				Set<T> pending=stream
						.collect(toCollection(LinkedHashSet::new));

				!pending.isEmpty();

				pending=pending
						.stream()
						.flatMap(mapper)
						.filter(value -> !visited.contains(value))
						.collect(toCollection(LinkedHashSet::new))

		) {

			visited.addAll(pending);

		}

		return from(visited.stream());
	}

	/**
	 * Iteratively expands this extended stream.
	 *
	 * @param steps  the number of expansion steps to be performed
	 * @param mapper a function mapping elements to streams of elements of the same type
	 *
	 * @return an extended stream produced by iteratively applying {@code mapper} this this extended stream and
	 * replacing
	 * it with with the elements of the returned streams until {@code steps} cycles are performed; null returned streams
	 * are considered to be empty
	 *
	 * @throws IllegalArgumentException if {@code steps} is negative
	 * @throws NullPointerException     if {@code mapper} is {@code null}
	 */
	public Xtream<T> iter(final int steps, final Function<? super T, ? extends Stream<T>> mapper) { // !!! lazy

		if ( steps < 0 ) {
			throw new IllegalArgumentException("negative steps count");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		Xtream<T> iter=this;

		for (int n=0; n < steps; ++n) {
			iter=iter.flatMap(mapper);
		}

		return iter;
	}


	/**
	 * Processes this extended stream.
	 *
	 * @param mapper a function mapping from/to extended streams
	 * @param <R>    the type of the elements of the extended stream returned by {@code mapper}
	 *
	 * @return an extended stream produced by applying {@code mapper} to this extended stream or an empty extended
	 * stream
	 * if {@code mapper} returns a null value
	 *
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public <R> Xtream<R> pipe(final Function<? super Xtream<T>, ? extends Stream<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return Optional
				.of(this)
				.map(mapper)
				.map(Xtream::from)
				.orElseGet(Xtream::empty);
	}

	/**
	 * Processes this extended stream.
	 *
	 * @param consumer a consumer of extended streams of this type
	 *
	 * @throws NullPointerException if {@code consumer} is {@code null}
	 */
	public void sink(final Consumer<? super Xtream<T>> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Xtream<T> filter(final Predicate<? super T> predicate) { return from(stream.filter(predicate)); }

	@Override public <R> Xtream<R> map(final Function<? super T, ? extends R> mapper) { return from(stream.map(mapper)); }

	@Override public IntStream mapToInt(final ToIntFunction<? super T> mapper) { return stream.mapToInt(mapper); }

	@Override public LongStream mapToLong(final ToLongFunction<? super T> mapper) { return stream.mapToLong(mapper); }

	@Override public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) { return stream.mapToDouble(mapper); }

	@Override public <R> Xtream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) { return from(stream.flatMap(mapper)); }

	@Override public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) { return stream.flatMapToInt(mapper); }

	@Override public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) { return stream.flatMapToLong(mapper); }

	@Override public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) { return stream.flatMapToDouble(mapper); }

	@Override public Xtream<T> distinct() { return from(stream.distinct()); }

	@Override public Xtream<T> sorted() { return from(stream.sorted()); }

	@Override public Xtream<T> sorted(final Comparator<? super T> comparator) { return from(stream.sorted(comparator)); }

	@Override public Xtream<T> peek(final Consumer<? super T> action) { return from(stream.peek(action)); }

	@Override public Xtream<T> limit(final long maxSize) { return from(stream.limit(maxSize)); }

	@Override public Xtream<T> skip(final long n) { return from(stream.skip(n)); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void forEach(final Consumer<? super T> action) { stream.forEach(action); }

	@Override public void forEachOrdered(final Consumer<? super T> action) { stream.forEachOrdered(action); }

	@Override public Object[] toArray() { return stream.toArray(); }

	@Override public <A> A[] toArray(final IntFunction<A[]> generator) { return stream.toArray(generator); }

	@Override public T reduce(final T identity, final BinaryOperator<T> accumulator) {
		return stream.reduce(identity, accumulator);
	}

	@Override public Optional<T> reduce(final BinaryOperator<T> accumulator) { return stream.reduce(accumulator); }

	@Override public <U> U reduce(
			final U identity, final BiFunction<U, ? super T, U> accumulator, final BinaryOperator<U> combiner) { return stream.reduce(identity, accumulator, combiner); }

	@Override public <R> R collect(
			final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator, final BiConsumer<R, R> combiner) { return stream.collect(supplier, accumulator, combiner); }

	@Override public <R, A> R collect(final Collector<? super T, A, R> collector) { return stream.collect(collector); }

	@Override public Optional<T> min(final Comparator<? super T> comparator) { return stream.min(comparator); }

	@Override public Optional<T> max(final Comparator<? super T> comparator) { return stream.max(comparator); }

	@Override public long count() { return stream.count(); }

	@Override public boolean anyMatch(final Predicate<? super T> predicate) { return stream.anyMatch(predicate); }

	@Override public boolean allMatch(final Predicate<? super T> predicate) { return stream.allMatch(predicate); }

	@Override public boolean noneMatch(final Predicate<? super T> predicate) { return stream.noneMatch(predicate); }

	@Override public Optional<T> findFirst() { return stream.findFirst(); }

	@Override public Optional<T> findAny() { return stream.findAny(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Iterator<T> iterator() { return stream.iterator(); }

	@Override public Spliterator<T> spliterator() { return stream.spliterator(); }

	@Override public boolean isParallel() { return stream.isParallel(); }

	@Override public Xtream<T> sequential() { return from(stream.sequential()); }

	@Override public Xtream<T> parallel() { return from(stream.parallel()); }

	@Override public Xtream<T> unordered() { return from(stream.unordered()); }

	@Override public Xtream<T> onClose(final Runnable closeHandler) { return from(stream.onClose(closeHandler)); }

	@Override public void close() { stream.close(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// !!! https://www.airpair.com/java/posts/parallel-processing-of-io-based-data-with-java-streams
	// !!! https://stackoverflow.com/questions/28210775/28211518#28211518

	private static final class BatchSpliterator<T> implements Spliterator<Collection<T>> {

		private final int size;
		private final Spliterator<T> base;


		private BatchSpliterator(final int size, final Spliterator<T> base) {
			this.size=size;
			this.base=base;
		}


		@Override public boolean tryAdvance(final Consumer<? super Collection<T>> action) {

			final Collection<T> batch=new ArrayList<>(size);

			for (int n=0; n < size && base.tryAdvance(batch::add); ++n) { }

			if ( batch.isEmpty() ) {

				return false;

			} else {

				action.accept(batch);

				return true;

			}

		}

		@Override public Spliterator<Collection<T>> trySplit() {
			return base.estimateSize() <= size ? null : Optional
					.ofNullable(base.trySplit())
					.map(spliterator -> new BatchSpliterator<>(size, spliterator))
					.orElse(null);
		}

		@Override public long estimateSize() {
			return (base.estimateSize()+size-1)/size;
		}

		@Override public int characteristics() {
			return base.characteristics();
		}

	}

}
