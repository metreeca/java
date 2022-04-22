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

import com.metreeca.http.Either;
import com.metreeca.rest.formats.MultipartFormat;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.http.Either.Right;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;


/**
 * HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP messages and message parts.</p>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 */
public abstract class Message<T extends Message<T>> extends Setup<T> {

    private static final String SetCookie=normalize("Set-Cookie");

    private static final Pattern SplitPattern=Pattern.compile("\\s*,\\s*");
    private static final Pattern CharsetPattern=Pattern.compile(";\\s*charset\\s*=\\s*(?<charset>[-\\w]+)\\b");


    private static String normalize(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, String> headers=new LinkedHashMap<>();
    private final Map<Format<?>, Either<MessageException, ?>> bodies=new HashMap<>();


    /**
     * Maps this message.
     *
     * @param mapper the message mapping function; must return a non-null value
     * @param <R>    the type of the value returned by {@code mapper}
     *
     * @return a non-null value obtained by applying {@code mapper} to this message
     *
     * @throws NullPointerException if {@code mapper} is null or returns a null value
     */
    public <R> R map(final Function<T, R> mapper) {

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        return requireNonNull(mapper.apply(self()), "null mapper return value");
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the focus item of this message.
     *
     * @return an absolute IRI identifying the focus item of this message
     */
    public abstract String item();

    /**
     * Retrieves the originating request for this message.
     *
     * @return the originating request for this message
     */
    public abstract Request request();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the charset of this message.
     *
     * <p><strong>Warning</strong> / The {@code Accept-Charset} header or the originating request is
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Charset">ignored</a>.</p>
     *
     * @return the charset defined in the {@code Content-Type} header of this message, defaulting to {@code UTF-8} if no
     * charset is explicitly defined
     */
    public String charset() {
        return header("Content-Type")

                .map(CharsetPattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group("charset"))

                .orElse(UTF_8.name());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves header values.
     *
     * @return an immutable map from header names to headers values
     */
    public Map<String, String> headers() {
        return unmodifiableMap(headers);
    }

    /**
     * Configures header values.
     *
     * <p><strong>Warning</strong> / The {@code Expires} attribute of the {@code Set-Cookie} header is not compliant
     * with combined multiple header value as specified by RFC 7230 and will cause an exception to be thrown: replace its
     * usages with {@code Max-Age} attributes.</p>
     *
     * @param headers the new header values; blank values are ignored
     *
     * @return this message
     *
     * @throws NullPointerException     if {@code headers} is null or contains null values
     * @throws IllegalArgumentException if {@code headers} includes a {@code Set-Cookie} with a value containing a comma
     *                                  ('{@code ,}')
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.2">RFC 7230 - 3.2.2 Field Order</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">Set-Cookie</a>
     */
    public T headers(final Map<String, String> headers) {

        if ( headers == null || headers.entrySet().stream().anyMatch(entry ->
                isNull(entry.getKey()) || isNull(entry.getValue())
        ) ) {
            throw new NullPointerException("null headers");
        }

        this.headers.clear();

        headers.forEach(this::header);

        return self();
    }


    /**
     * Retrieves header values.
     *
     * @param name the name of the header whose values are to be retrieved
     *
     * @return a possibly empty stream of header values
     *
     * @throws NullPointerException if {@code name} is null
     */
    public Stream<String> headers(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return header(name).map(SplitPattern::split).stream().flatMap(Arrays::stream);
    }

    /**
     * Configures header values.
     *
     * <p>Provided values are combined by joining them with a comma ('{@code ,}').</p>
     *
     * <p><strong>Warning</strong> / The {@code Expires} attribute of the {@code Set-Cookie} header is not compliant
     * with combined multiple header value as specified by RFC 7230 and will cause an exception to be thrown: replace its
     * usages with {@code Max-Age} attributes.</p>
     *
     * @param name   the name of the header whose values are to be appended
     * @param values the new values for {@code name}; blank values are ignored; empty arrays cause the header to be
     *               removed
     *
     * @return this message
     *
     * @throws NullPointerException     if either {@code name} or {@code values} is null or {@code values} contains null
     *                                  items
     * @throws IllegalArgumentException if {@code name} is {@code Set-Cookie} and {@code values} contains a value
     *                                  containing a comma ('{@code ,}')
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.2">RFC 7230 - 3.2.2 Field Order</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">Set-Cookie</a>
     */
    public T headers(final String name, final String... values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        return headers(name, Arrays.asList(values));
    }

    /**
     * Configures header values.
     *
     * <p>Provided values are combined by joining them with a comma ('{@code ,}').</p>
     *
     * <p><strong>Warning</strong> / The {@code Expires} attribute of the {@code Set-Cookie} header is not compliant
     * with combined multiple header value as specified by RFC 7230 and will cause an exception to be thrown: replace its
     * usages with {@code Max-Age} attributes.</p>
     *
     * @param name   the name of the header whose values are to be appended
     * @param values the bew values for {@code name}; blank values are ignored; empty collections cause the header to be
     *               removed
     *
     * @return this message
     *
     * @throws NullPointerException     if either {@code name} or {@code values} is null
     * @throws NullPointerException     if {@code values} contains null values
     * @throws IllegalArgumentException if {@code name} is {@code Set-Cookie} and {@code values} includes a value
     *                                  containing a comma ('{@code ,}')
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.2">RFC 7230 - 3.2.2 Field Order</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">Set-Cookie</a>
     */
    public T headers(final String name, final Collection<String> values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null values");
        }

        return header(name, values.stream().filter(not(String::isBlank)).collect(joining(", ")));
    }


    /**
     * Retrieves header value.
     *
     * @param name the name of the header whose value is to be retrieved
     *
     * @return an optional value containing the header value, if one is present; an empty optional otherwise
     *
     * @throws NullPointerException if {@code name} is null
     */
    public Optional<String> header(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return Optional.ofNullable(headers.get(normalize(name)));
    }

    /**
     * Configures header value.
     *
     * <p><strong>Warning</strong> / The {@code Expires} attribute of the {@code Set-Cookie} header is not compliant
     * with combined multiple header value as specified by RFC 7230 and will cause an exception to be thrown: replace its
     * usages with {@code Max-Age} attributes.</p>
     *
     * @param name  the name of the header whose value is to be configured
     * @param value the new value for {@code name}; blank values cause the header to be removed
     *
     * @return this message
     *
     * @throws NullPointerException     if either {@code name} or {@code value} is null
     * @throws IllegalArgumentException if {@code name} is {@code Set-Cookie} and {@code value} includes a value
     *                                  containing a comma ('{@code ,}')
     * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.2">RFC 7230 - 3.2.2 Field Order</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">Set-Cookie</a>
     */
    public T header(final String name, final String value) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        final String normalized=normalize(name);

        if ( normalized.equals(SetCookie) && value.contains(",") ) {
            throw new IllegalArgumentException("null values");
        }

        if ( value.isBlank() ) {

            headers.remove(normalized);

        } else {

            headers.put(normalized, value);

        }

        return self();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Decodes message body.
     *
     * @param format the expected body format
     * @param <V>    the type of the body to be decoded
     *
     * @return either a message exception reporting a decoding issue or the message body {@linkplain
     * Format#decode(Message) decoded} by {@code format}
     *
     * @throws NullPointerException if {@code format} is null
     */
    @SuppressWarnings("unchecked")
    public <V> Either<MessageException, V> body(final Format<V> format) {

        if ( format == null ) {
            throw new NullPointerException("null body");
        }

        // ;( don't use bodies.computeIfAbsent() to prevent concurrent modification exceptions

        Either<MessageException, ?> body=bodies.get(format);

        if ( body == null ) {
            bodies.put(format, body=format.decode(this));
        }

        return (Either<MessageException, V>)body;
    }

    /**
     * Encodes message body.
     *
     * <p>Subsequent calls to {@link #body(Format)} with the same body format will return the specified value.</p>
     *
     * @param format the body format
     * @param value  the body to be encoded
     * @param <V>    the type of the body to be encoded
     *
     * @return this message with the {@code value} {@linkplain Format#encode(Message, Object) encoded} by {@code format}
     * as body
     *
     * @throws NullPointerException if either {@code format} or {@code value} is null
     */
    public <V> T body(final Format<? super V> format, final V value) {

        if ( format == null ) {
            throw new NullPointerException("null body");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        bodies.put(format, Right(value));

        return format.encode(self(), value);
    }


    /**
     * Updates message body.
     *
     * @param format the body format
     * @param mapper a function mapping from the current to the updated value of the {@code format} body; must return a
     *               non-null value
     * @param <V>    the type of the body to be updated
     *
     * @return this message
     *
     * @throws NullPointerException if either {@code name} or {@code value} is null
     */
    @SuppressWarnings("unchecked")
    public <V> T map(final Format<? super V> format, final UnaryOperator<V> mapper) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        if ( mapper == null ) {
            throw new NullPointerException("null mapper");
        }

        // ;( don't use bodies.computeIfPresent() to prevent concurrent modification exceptions

        final Either<MessageException, ?> value=bodies.get(format);

        if ( value != null ) {

            final Either<MessageException, V> mapped=value.map(body -> requireNonNull(

                    mapper.apply((V)body), "null mapper return value"

            ));

            bodies.put(format, mapped);

            mapped.map(body ->

                    format.encode(self(), body)

            );

        }

        return self();
    }


    //// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a message part.
     *
     * @param item the (possibly relative)) IRI identifying the {@linkplain #item() focus item} of the new message part;
     *             will be resolved against the message {@linkplain #item() item} IRI
     *
     * @return a new message part with a focus item identified by {@code item} and the same {@linkplain #request()
     * originating request} as this message
     *
     * @throws NullPointerException     if {@code item} is null
     * @throws IllegalArgumentException if {@code item} is not a legal (possibly relative) IRI
     */
    public Message<?> part(final String item) {

        if ( item == null ) {
            throw new NullPointerException("null item");
        }

        return new Part(URI.create(item()).resolve(item).toString(), this);
    }

    /**
     * Lift a message part into this message.
     *
     * <p>Mainly intended to be used inside wrappers to lift the main message part in {@linkplain MultipartFormat
     * multipart} requests for further downstream processing, as for instance in:</p>
     *
     * <pre>{@code handler -> request -> request.body(multipart(1000, 10_000)).fold(
     *
     *     parts -> Optional.ofNullable(parts.get("main"))
     *
     *         .map(main -> {
     *
     *           ... // process ancillary body parts
     *
     *           return handler.handle(request.lift(main));
     *
     *         })
     *
     *         .orElseGet(() -> request.reply(new Failure()
     *             .status(BadRequest)
     *             .cause("missing main body part")
     *         )),
     *
     *     request::reply
     *
     * )}</pre>
     *
     * @param message the message part to be lifted into this message
     *
     * @return this message modified as follows:
     * <ul>
     * <li>source {@code message} headers are copied to this message overriding existing matching values;</li>
     * <li>source {@code message} body representations are copied to this message replacing all existing values.</li>
     * </ul>
     *
     * @throws NullPointerException if {@code message} is null
     */
    public T lift(final Message<?> message) {

        if ( message == null ) {
            throw new NullPointerException("null message");
        }

        headers.putAll(message.headers); // value lists are read-only

        bodies.clear();
        bodies.putAll(message.bodies);

        return self();
    }

    private static final class Part extends Message<Part> {

        private final String item;
        private final Request request;


        private Part(final String item, final Message<?> message) {
            this.item=item;
            this.request=message.request();
        }


        @Override public String item() {
            return item;
        }

        @Override public Request request() {
            return request;
        }

    }

}