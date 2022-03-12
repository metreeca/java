package com.metreeca.core;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;

/**
 * Identifier utilities.
 */
public final class Identifiers {

    private static final String IRIScheme="(?<schemeall>(?<scheme>[-+\\w]+):)";
    private static final String IRIHost="(?<hostall>//(?<host>[^/?#]*))";
    private static final String IRIQuery="(?<queryall>\\?(?<query>[^#]*))";
    private static final String IRIFragment="(?<fragmentall>#(?<fragment>.*))";
    private static final String IRIPath="(?<pathall>(?<path>[^?#]*)"+IRIQuery+"?"+IRIFragment+"?)";

    private static final char[] HexDigits="0123456789abcdef".toCharArray();


    /**
     * A pattern matching IRI components.
     *
     * <p>On successful matching, will bind the following {@linkplain Matcher#group(String) named groups} to the
     * corresponding (possibly empty) part of the matched IRI:</p>
     *
     * <ul>
     *     <li>{@code scheme}</li>
     *     <li>{@code schemeall} / {@code scheme} plus trailing colon ('{@code :}')</li>
     *     <li>{@code host}</li>
     *     <li>{@code hostall} / {@code host} plus leading slashes ('{@code //}')</li>
     *     <li>{@code path}</li>
     *     <li>{@code pathall} / {@code path} plus trailing {@code query} and {@code fragment}</li>
     *     <li>{@code query}</li>
     *     <li>{@code queryall} / {@code query} plus leading question mark ('{@code ?}')</li>
     *     <li>{@code fragment}</li>
     *     <li>{@code fragmentall} / {@code fragment} plus leading hash mark ('{@code #}')</li>
     * </ul>
     *
     * @see <a href="https://tools.ietf.org/html/rfc3986#appendix-B">RFC 3986 Uniform Resource Identifier (URI): Generic
     * Syntax - Appendix B.  Parsing a URI Reference with a Regular Expression</a>
     */
    public static final Pattern IRIPattern=Pattern.compile("^"+IRIScheme+"?"+IRIHost+"?"+IRIPath+"$");

    /**
     * A pattern matching absolute IRIs.
     *
     * <p>Will match only IRIs including a {@code scheme}.</p>
     *
     * <p>On successful matching, will bind the same {@linkplain Matcher#group(String) named groups} bound by the
     * {@linkplain #IRIPattern base IRI pattern}.</p>
     */
    public static final Pattern AbsoluteIRIPattern=Pattern.compile("^"+IRIScheme+IRIHost+"?"+IRIPath+"$");


    /**
     * Generates a random UUID.
     *
     * @return a random
     * <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_4_(random)">Version 4
     * UUID</a> generated using a cryptographically strong pseudo random number generator
     */
    public static String uuid() {
        return randomUUID().toString();
    }

    /**
     * Generates a name-based UUID.
     *
     * @param text the content to be hashed in the generated UUID
     *
     * @return a name-based
     * <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Versions_3_and_5_(namespace_name-based)">Version 3
     * UUID</a> based on {@code text}
     *
     * @throws NullPointerException if {@code text} is null
     */
    public static String uuid(final String text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        return uuid(text.getBytes(UTF_8));
    }

    /**
     * Generates a name-based UUID.
     *
     * @param data the content to be hashed in the generated UUID
     *
     * @return a name-based
     * <a href="https://en.wikipedia.org/wiki/Universally_unique_identifier#Versions_3_and_5_(namespace_name-based)">Version 3
     * UUID</a> based on {@code data}
     *
     * @throws NullPointerException if {@code data} is null
     */
    public static String uuid(final byte[] data) {

        if ( data == null ) {
            throw new NullPointerException("null data");
        }

        return nameUUIDFromBytes(data).toString();
    }


    /**
     * Generates a random MD5 hash.
     *
     * <p><strong>Warning</strong> / This function is not intended to generate cryptographically strong identifiers.</p>
     *
     * @return a random <a href="https://en.wikipedia.org/wiki/MD5">MD5</a>
     */
    public static String md5() {

        final byte[] bytes=new byte[16];

        ThreadLocalRandom.current().nextBytes(bytes);

        return md5(bytes);
    }

    /**
     * Generates an MD5 hash.
     *
     * <p><strong>Warning</strong> / This function is not intended to generate cryptographically strong identifiers.</p>
     *
     * @param text the content to be hashed
     *
     * @return an <a href="https://en.wikipedia.org/wiki/MD5">MD5</a> hash based on {@code text}
     *
     * @throws NullPointerException if {@code text} is null
     */
    public static String md5(final String text) {

        if ( text == null ) {
            throw new NullPointerException("null text");
        }

        return md5(text.getBytes(UTF_8));
    }

    /**
     * Generates an MD5 hash.
     *
     * <p><strong>Warning</strong> / This function is not intended to generate cryptographically strong identifiers.</p>
     *
     * @param data the content to be hashed
     *
     * @return an <a href="https://en.wikipedia.org/wiki/MD5">MD5</a> hash based on {@code data}
     *
     * @throws NullPointerException if {@code data} is null
     */
    public static String md5(final byte[] data) {

        if ( data == null ) {
            throw new NullPointerException("null data");
        }

        try {

            final byte[] bytes=MessageDigest.getInstance("MD5").digest(data);

            if ( bytes == null ) { return null; } else {

                final char[] hex=new char[bytes.length*2];

                for (int i=0, l=bytes.length; i < l; ++i) {

                    final int b=bytes[i]&0xFF;

                    hex[2*i]=HexDigits[b >>> 4];
                    hex[2*i+1]=HexDigits[b&0x0F];
                }

                return new String(hex);
            }

        } catch ( final NoSuchAlgorithmException unexpected ) {
            throw new InternalError(unexpected);
        }
    }


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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Identifiers() { }

}
