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

package com.metreeca.xml.formats;

import com.metreeca.rest.*;
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.OutputFormat;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.util.regex.Pattern;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.util.regex.Pattern.compile;


/**
 * HTML message format.
 */
public final class HTMLFormat extends Format<Document> {

	/**
	 * The default MIME type for HTML messages ({@value}).
	 */
	public static final String MIME="text/html";

	/**
	 * A pattern matching the HTML MIME type.
	 */
	public static final Pattern MIMEPattern=compile("(?i)^text/html(?:\\s*;.*)?$");


	/**
	 * Creates an HTML message format.
	 *
	 * @return a new HTML message format
	 */
	public static HTMLFormat html() { return new HTMLFormat(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses an HTML document.
	 *
	 * <p><strong>Warning</strong> / The {@code .getElementById()} method of the parsed document will return always
	 * {@code null}, as HTML id attributes aren't recognized as ID XML attributes.</p>
	 *
	 * @param input   the input the HTML document is to be parsed from
	 * @param charset the charset used to decode {@code input}
	 * @param base    the possibly null base URL for the HTML document to be parsed
	 *
	 * @return either a parsing exception or the HTML document parsed from {@code input}
	 *
	 * @throws NullPointerException if either {@code input} or {@code charset} is null
	 */
	public static Either<TransformerException, Document> html(
			final InputStream input, final String charset, final String base
	) {

		if ( input == null ) {
			throw new NullPointerException("null input stream");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		if ( base == null ) {
			throw new NullPointerException("null base URL");
		}

		try {

			final Document document=W3CDom.convert(Jsoup.parse(input, charset, base));

			document.normalize();

			return Right(document);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Writes an HTML node.
	 *
	 * @param <O>     the type of the {@code output} the HTML node is to be written to
	 * @param output  the output the HTML node is to be written to
	 * @param base    the possibly null base URL for the HTML node to be written
	 * @param charset the charset used to encode {@code output}
	 * @param node    the HTML node to be written
	 *
	 * @return the target {@code output}
	 *
	 * @throws NullPointerException if {@code output} or {@code charset} or {@code node} is null
	 */
	public static <O extends OutputStream> O html(
			final O output, final String charset, final String base, final Node node
	) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( node == null ) {
			throw new NullPointerException("null node");
		}

		try ( final Writer writer=new OutputStreamWriter(output, charset) ) {

			transformer().transform(
					new DOMSource(node, base),
					new StreamResult(writer)
			);

			return output;

		} catch ( final TransformerException unexpected ) {
			throw new RuntimeException(unexpected);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Transformer transformer() {
		try {

			final TransformerFactory factory=TransformerFactory.newInstance();

			factory.setAttribute("indent-number", 4);

			final Transformer transformer=factory.newTransformer();

			transformer.setOutputProperty(OutputKeys.METHOD, "html");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			return transformer;

		} catch ( final TransformerConfigurationException e ) {

			throw new RuntimeException("unable to create transformer", e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private HTMLFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the default MIME type for HTML messages ({@value MIME})
	 */
	@Override public String mime() {
		return MIME;
	}


	/**
	 * Decodes the HTML {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available and the {@code message} {@code Content-Type} header is matched by {@link #MIMEPattern},
	 * taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public Either<MessageException, Document> decode(final Message<?> message) {
		return message.header("Content-Type").filter(MIMEPattern.asPredicate())

				.map(type -> message.body(input()).flatMap(source -> {

					try ( final InputStream input=source.get() ) {

						return html(input, message.charset(), message.item()).fold(e -> Left(status(BadRequest, e)),
								Either::Right);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no HTML body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes the
	 * HTML {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body, taking into
	 * account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Document value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> html(output, message.charset(), message.item(), value));
	}

}
