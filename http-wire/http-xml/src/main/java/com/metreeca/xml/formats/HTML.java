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

package com.metreeca.xml.formats;

import com.metreeca.http.*;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static java.util.regex.Pattern.compile;


/**
 * HTML message format.
 */
public final class HTML implements Format<Document> {

    /**
     * The default MIME type for HTML messages ({@value}).
     */
    public static final String MIME="text/html";

    /**
     * A pattern matching the HTML MIME type.
     */
    public static final Pattern MIMEPattern=compile("(?i)^text/html(?:\\s*;.*)?$");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses an HTML document.
     *
     * <p><strong>Warning</strong> / The {@code .getElementById()} method of the parsed document will return always
     * {@code null}, as HTML id attributes aren't recognized as ID XML attributes.</p>
     *
     * @param html the source of the HTML document to be parsed
     * @param base the possibly null base URL for the HTML document to be parsed
     *
     * @return the HTML document parsed from {@code input}
     *
     * @throws NullPointerException if either {@code html} or {@code base} is null
     * @throws FormatException       if {@code input} contains a malformed document
     */
    public static Document html(
            final String html, final String base
    ) throws FormatException {

        if ( html == null ) {
            throw new NullPointerException("null html");
        }

        if ( base == null ) {
            throw new NullPointerException("null base URL");
        }

        final Document document=W3CDom.convert(Jsoup.parse(html, base));

        document.normalize();

        return document;
    }

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
     * @return the HTML document parsed from {@code input}
     *
     * @throws NullPointerException if any parameter is null
     * @throws FormatException       if {@code input} contains a malformed document
     */
    public static Document html(
            final InputStream input, final Charset charset, final String base
    ) throws FormatException {

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

            final Document document=W3CDom.convert(Jsoup.parse(input, charset.name(), base));

            document.normalize();

            return document;

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
			final O output, final Charset charset, final String base, final Node node
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

    /**
     * @return {@value MIME}
     */
    @Override public String mime() {
        return MIME;
    }

    @Override public Class<Document> type() {
        return Document.class;
    }


    /**
     * @param message
     *
     * @return the HTML payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account
     * the {@code message} {@linkplain Message#charset() charset} or an empty optional if the {@code "Content-Type"}
     * {@code
     * message} header is not empty and is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<Document> decode(final Message<?> message) {
        return message

		        .header("Content-Type")
		        .or(() -> Optional.of(MIME))
		        .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try ( final InputStream input=message.input().get() ) {

                        return html(input, message.charset(), message.item());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the HTML
	 * {@code value}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Document value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> html(output, message.charset(), message.item(), value));
	}

}
