/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rest.codecs;

/**
 * JSON message codec.
 *
 * @see <a href="https://github.com/google/gson">Gson</a>
 */
public final class JSON {

    ///**
    // * The default MIME type for JSON messages ({@value}).
    // */
    //public static final String MIME="application/json";
    //
    ///**
    // * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
    // */
    //public static final Pattern MIMEPattern=Pattern.compile(
    //        "(?i:^(text/json|application/(?:.*\\+)?json)(?:\\s*;.*)?$)"
    //);
    //
    //
    //private static final Class<JsonElement> Type=JsonElement.class;
    //
    //
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //@Override public <M extends Message<M>> M _encode(final M message, final JsonElement payload) {
    //    throw new UnsupportedOperationException(";(  be implemented"); // !!!
    //}
    //
    //@Override protected <M extends Message<M>> Optional<M> encode(final M message) {
    //    return message.payload(Type).map(element -> message
    //
    //            .header("Content-Type", MIME)
    //
    //            .payload(Output.class, output -> {
    //
    //                try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {
    //
    //                    final Gson gson=new Gson(); // !!! service
    //
    //                    gson.toJson(element, writer);
    //
    //                } catch ( final JsonIOException e ) {
    //
    //                    throw new UncheckedIOException(new IOException(e));
    //
    //                } catch ( final IOException e ) {
    //
    //                    throw new UncheckedIOException(e);
    //
    //                }
    //
    //            })
    //
    //    );
    //}
    //
    //
    //
    //@Override public <M extends Message<M>> Optional<JsonElement> _decode(final M message) {
    //    throw new UnsupportedOperationException(";(  be implemented"); // !!!
    //}
    //
    //@Override protected <M extends Message<M>> Optional<M> decode(final M message) throws IllegalArgumentException {
    //    return message.header("Content-Type").filter(MIMEPattern.asPredicate()).map(type -> {
    //
    //        try (
    //                final InputStream input=message.payload(Input.class).orElseGet(() -> Feeds::input).get();
    //                final Reader reader=new InputStreamReader(input, message.charset())
    //        ) {
    //
    //            final JsonElement element=JsonParser.parseReader(reader);
    //
    //            return message.payload(Type, element);
    //
    //        } catch ( final JsonSyntaxException e ) {
    //
    //            throw new IllegalArgumentException(e);
    //
    //        } catch ( final JsonIOException e ) {
    //
    //            throw new UncheckedIOException(new IOException(e));
    //
    //        } catch ( final IOException e ) {
    //
    //            throw new UncheckedIOException(e);
    //
    //        }
    //
    //    });
    //}

}
