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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.*;


/**
 * Multipart body format.
 *
 * @see <a href="https://tools.ietf.org/html/rfc2046#section-5.1">RFC 2046 - Multipurpose Internet Mail Extensions
 * 		(MIME) Part Two: Media Types - § 5.1.  Multipart Media Type</a>
 */
public final class MultipartFormat extends Format<Map<String, Message<?>>> {

	/**
	 * The default MIME type for multipart message bodies ({@value}).
	 */
	public static final String MIME="multipart/mixed";


	private static final byte[] Dashes="--".getBytes(UTF_8);
	private static final byte[] CRLF="\r\n".getBytes(UTF_8);
	private static final byte[] Colon=": ".getBytes(UTF_8);

	private static final byte[] BoundaryChars=
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(UTF_8);

	private static final Pattern BoundaryPattern=Pattern.compile(parameter("boundary"));
	private static final Pattern NamePattern=Pattern.compile(parameter("name"));
	private static final Pattern ItemPattern=Pattern.compile(parameter("filename"));


	private static String parameter(final String name) {
		return format(";\\s*(?i:%s)\\s*=\\s*(?:\"(?<quoted>[^\"]*)\"|(?<simple>[^;\\s]*))", name);
	}

	private String parameter(final Matcher matcher) {
		return Optional.ofNullable(matcher.group("quoted")).orElseGet(() -> matcher.group("simple"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Retrieves a write-only multipart body format.
	 *
	 * @return a write-only multipart body format with part/body size limit set to 0, intended for configuring multipart
	 * 		response bodies
	 */
	public static MultipartFormat multipart() {
		return new MultipartFormat(0, 0);
	}

	/**
	 * Retrieves a multipart body format.
	 *
	 * @param part the size limit for individual message parts; includes boundary and headers and applies also to 
	 *                message
	 *             preamble and epilogue
	 * @param body the size limit for the complete message body
	 *
	 * @return a write-only multipart body format with part/body size limit set to 0, intended for configuring multipart
	 * 		response bodies
	 *
	 * @throws IllegalArgumentException if either {@code part} or {@code body} is less than 0 or if {@code part} is
	 *                                  greater than {@code body}
	 */
	public static MultipartFormat multipart(final int part, final int body) {

		if ( part < 0 ) {
			throw new IllegalArgumentException("negative part size limit");
		}

		if ( body < 0 ) {
			throw new IllegalArgumentException("negative body size limit");
		}

		if ( part > body ) {
			throw new IllegalArgumentException("part size limit greater than body size limit");
		}

		return new MultipartFormat(part, body);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int part;
	private final int body;


	private MultipartFormat(final int part, final int body) {
		this.part=part;
		this.body=body;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<Map<String, Message<?>>, UnaryOperator<Response>> get(final Message<?> message) {
		return message.header("Content-Type")

				.filter(type -> type.startsWith("multipart/"))

				.map(type -> message.body(input()).process(source -> {

					final String boundary=message
							.header("Content-Type")
							.map(BoundaryPattern::matcher)
							.filter(Matcher::find)
							.map(this::parameter)
							.orElse("");

					final Map<String, Message<?>> parts=new LinkedHashMap<>();

					try {

						new MultipartParser(part, body, source.get(), boundary, (headers, content) -> {

							final Optional<String> disposition=headers
									.stream()
									.filter(entry ->
											entry.getKey().equalsIgnoreCase("Content-Disposition")
									)
									.map(Map.Entry::getValue)
									.findFirst();

							final String name=disposition
									.map(NamePattern::matcher)
									.filter(Matcher::find)
									.map(this::parameter)
									.filter(match -> !parts.containsKey(match))
									.orElseGet(() -> format("part%d", parts.size()));

							final String item=disposition
									.map(ItemPattern::matcher)
									.filter(Matcher::find)
									.map(this::parameter)
									.map(s -> "file:"+s)
									.orElseGet(() -> "uuid:"+UUID.randomUUID());

							parts.put(name, message.link(item)

									.headers((Map<String, List<String>>)headers.stream().collect(groupingBy(
											Map.Entry::getKey,
											LinkedHashMap::new,
											mapping(Map.Entry::getValue, toList())
									)))

									.body(input(), () -> content)

							);

						}).parse();

					} catch ( final ParseException e ) {

						final int status=e.getMessage().contains("size limit") ? PayloadTooLarge : BadRequest;
						return Error(status(status, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

					return Result.Value(parts);

				}))

				.orElseGet(() -> Error(status(UnsupportedMediaType)));
	}

	@Override public <M extends Message<M>> M set(final M message, final Map<String, Message<?>> value) {

		final String type=message
				.header("Content-Type") // custom value
				.orElse(MIME); // fallback value

		final byte[] boundary; // compute boundary

		final Matcher matcher=BoundaryPattern.matcher(type);

		if ( matcher.find() ) { // custom boundary set in content-type header

			boundary=parameter(matcher).getBytes(UTF_8);

		} else { // generate random boundary and update content-type definition

			new Random().nextBytes(boundary=new byte[70]);

			for (int i=0; i < boundary.length; i++) {
				boundary[i]=BoundaryChars[(boundary[i]&0xFF)%BoundaryChars.length];
			}

			message.header("Content-Type", format("%s; boundary=\"%s\"", type, new String(boundary, UTF_8)));

		}

		return message.body(output(), output -> {
			try {

				for (final Message<?> part : value.values()) {

					output.write(Dashes);
					output.write(boundary);
					output.write(CRLF);

					for (final Map.Entry<String, List<String>> header : part.headers().entrySet()) {

						final String name=header.getKey();

						for (final String _value : header.getValue()) {
							output.write(name.getBytes(UTF_8));
							output.write(Colon);
							output.write(_value.getBytes(UTF_8));
							output.write(CRLF);
						}
					}

					output.write(CRLF);

					part.body(output()).value().ifPresent(target -> target.accept(output)); // !!! handle errors

					output.write(CRLF);
				}

				output.write(Dashes);
				output.write(boundary);
				output.write(Dashes);
				output.write(CRLF);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
