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

package com.metreeca.xml.codecs;

import com.metreeca.rest.*;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import static com.metreeca.rest.Response.BadRequest;

import static java.util.regex.Pattern.compile;


/**
 * XML message codec.
 */
public final class XML implements Codec<Document> {

    /**
     * The default MIME type for XML message bodies ({@value}).
     */
    public static final String MIME="application/xml";

    /**
     * A pattern matching XML-based MIME types, for instance {@code application/rss+xml}.
     */
    public static final Pattern MIMEPattern=compile("(?i)^.*/(?:.*\\+)?xml(?:\\s*;.*)?$");


	private static DocumentBuilder builder() {
		try {

			return DocumentBuilderFactory.newInstance().newDocumentBuilder();

		} catch ( final ParserConfigurationException e ) {

			throw new RuntimeException("unable to create document builder", e);

		}
	}

	private static Transformer transformer() {
		try {

			return TransformerFactory.newInstance().newTransformer();

		} catch ( final TransformerConfigurationException e ) {

			throw new RuntimeException("unable to create transformer", e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Parses an XML document.
     *
     * @param input the input stream the XML document is to be parsed from
     *
     * @return the XML document parsed from {@code input}
     *
     * @throws NullPointerException if {@code input} is null
     * @throws CodecException       if {@code input} contains a malformed document
     */
    public static Document xml(final InputStream input) throws CodecException {

        if ( input == null ) {
            throw new NullPointerException("null input");
        }

        return xml(input, null);
    }

    /**
     * Parses an XML document.
     *
     * @param input the input stream the XML document is to be parsed from
     * @param base  the possibly null base URL for the XML document to be parsed
     *
     * @return either a parsing exception or the XML document parsed from {@code input}
     *
     * @throws NullPointerException if {@code input} is null
     * @throws CodecException       if {@code input} contains a malformed document
     */
    public static Document xml(final InputStream input, final String base) throws CodecException {

        if ( input == null ) {
            throw new NullPointerException("null input");
        }

        final InputSource source=new InputSource();

        source.setSystemId(base);
        source.setByteStream(input);

        return xml(new SAXSource(source));
    }

    /**
     * Parses an XML document.
     *
     * @param source the source the XML document is to be parsed from
     *
     * @return either a parsing exception or the XML document parsed from {@code source}
     *
     * @throws NullPointerException if {@code source} is null
     * @throws CodecException       if {@code source} contains a malformed document
     */
    public static Document xml(final Source source) throws CodecException {

        if ( source == null ) {
            throw new NullPointerException("null source");
        }

        try {

            final Document document=builder().newDocument();

			document.setDocumentURI(source.getSystemId());

			transformer().transform(source, new DOMResult(document));

            return document;

        } catch ( final TransformerException e ) {

            throw new CodecException(BadRequest, e.getMessage());

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final XMLReader parser;


    /**
     * Creates an XML message codec.
     */
    public XML() {
        this.parser=null;
    }

    /**
     * Creates an XML message format using a custom SAX parser.
     *
     * @param parser the custom SAX parser
     *
     * @throws NullPointerException if {@code parser} is null
     */
    public XML(final XMLReader parser) {

        if ( parser == null ) {
            throw new NullPointerException("null parser");
        }

        this.parser=parser;
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
     * @return the XML payload decoded from the raw {@code message} {@linkplain Message#input()} taking into account the
     * {@code message} {@linkplain Message#charset() charset} or an empty optional if the {@code "Content-Type"} {@code
     * message} header is not matched by {@link #MIMEPattern}
     */
    @Override public Optional<Document> decode(final Message<?> message) {
        return message

                .header("Content-Type")
                .filter(MIMEPattern.asPredicate())

                .map(type -> {

                    try ( final InputStream input=message.input().get() ) {

                        final InputSource inputSource=new InputSource();

                        inputSource.setSystemId(message.item());
                        inputSource.setByteStream(input);
                        inputSource.setEncoding(message.charset().name());

                        final SAXSource saxSource=(parser != null)
								? new SAXSource(parser, inputSource)
                                : new SAXSource(inputSource);

                        saxSource.setSystemId(message.item());

                        return xml(saxSource);

                    } catch ( final UnsupportedEncodingException e ) {

                        throw new CodecException(BadRequest, e.getMessage());

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                });
    }

    /**
     * @return the target {@code message} with its {@code "Content-Type"} header configured to {@value #MIME}, unless
     * already defined, and its raw {@linkplain Message#output(Consumer) output} configured to return the XML
     * {@code value}
     */
    @Override public <M extends Message<M>> M encode(final M message, final Document value) {
        return message

                .header("Content-Type", message.header("Content-Type").orElse(MIME))

                .output(output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        final Source source=new DOMSource(value);
                        final javax.xml.transform.Result result=new StreamResult(writer);

                        source.setSystemId(message.item());
						result.setSystemId(message.item());

						transformer().transform(source, result);

					} catch ( final TransformerException unexpected ) {

						throw new RuntimeException(unexpected);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				});
	}

}
