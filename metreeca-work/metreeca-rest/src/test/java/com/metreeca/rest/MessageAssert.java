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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.io.*;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.fail;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


public abstract class MessageAssert<A extends MessageAssert<A, T>, T extends Message<T>> extends AbstractAssert<A, T> {

    @SuppressWarnings("unchecked") public static <T extends Message<T>> MessageAssert<?, ?> assertThat(final Message<?> message) {

        final class WorkAssert extends MessageAssert<WorkAssert, T> {

            private WorkAssert(final T actual) { super(actual, WorkAssert.class); }

        }

        return new WorkAssert((T)message);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected MessageAssert(final T actual, final Class<A> type) {
        super(actual, type);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public A hasItem(final String item) {

        isNotNull();

        if ( !Objects.equals(actual.item(), item) ) {
            failWithMessage("expected message to have <%s> item but has <%s>", item, actual.item());
        }

        return myself;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <V> A hasAttribute(final Class<V> key, final Consumer<V> assertions) {

        if ( key == null ) {
            throw new NullPointerException("null key");
        }

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        actual.attribute(key).ifPresentOrElse(assertions, () ->
                failWithMessage("expected message to have <%s> attribute", key)
        );


        return myself;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public A hasHeader(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        isNotNull();

        final Collection<String> values=actual.headers(name).collect(toList());

        if ( values.isEmpty() ) {
            failWithMessage("expected message to have <%s> headers but has none", name);
        }

        return myself;
    }

    public A hasHeader(final String name, final String value) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        isNotNull();

        final String found=actual.header(name).orElse(null);

        if ( !value.equals(found) ) {
            failWithMessage(
                    "expected response to have <%s> header with value <%s> but found <%s>",
                    name, value, found
            );
        }

        return myself;
    }

    public A hasHeader(final String name, final Consumer<String> assertions) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        final String value=actual.header(name).orElse(null);

        if ( value == null ) {
            failWithMessage("expected message to have <%s> headers but has none", name);
        }

        assertions.accept(value);

        return myself;
    }

    public A doesNotHaveHeader(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        isNotNull();

        final Collection<String> values=actual.headers(name).collect(toList());

        if ( !values.isEmpty() ) {
            failWithMessage("expected response to have no <%s> headers but has <%s>", name, values);
        }

        return myself;
    }


    public A hasHeaders(final String name, final String... values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        isNotNull();

        Assertions.assertThat(actual.headers(name))
                .as("<%s> message headers", name)
                .containsExactly(values);

        return myself;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <V> A hasDataInput(final Consumer<byte[]> assertions) {

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

        try ( final InputStream input=actual.input().get() ) {

            input.transferTo(buffer);

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }

        actual.input(() -> new ByteArrayInputStream(buffer.toByteArray()));

        assertions.accept(buffer.toByteArray());

        return myself;

    }

    public <V> A hasTextInput(final Consumer<String> assertions) {

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

        try ( final InputStream input=actual.input().get() ) {

            input.transferTo(buffer);

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }

        actual.input(() -> new ByteArrayInputStream(buffer.toByteArray()));

        assertions.accept(buffer.toString(actual.charset()));

        return myself;

    }


    public <V> A hasDataOutput(final Consumer<byte[]> assertions) {

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

        actual.output().accept(buffer);

        assertions.accept(buffer.toByteArray());

        return myself;

    }

    public <V> A hasTextOutput(final Consumer<String> assertions) {

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

        actual.output().accept(buffer);

        assertions.accept(buffer.toString(actual.charset()));

        return myself;

    }


    public A hasBody() {

        isNotNull();

        final ByteArrayOutputStream output=new ByteArrayOutputStream();

        actual.output().accept(output);

        final byte[] data=output.toByteArray();

        if ( data.length == 0 ) {
            failWithMessage("expected body but had none");
        }

        return myself;
    }

    public A hasBody(final Codec<?> codec) {

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        return hasBody(codec, body -> { });
    }

    public <V> A hasBody(final Codec<V> codec, final Consumer<V> assertions) {

        if ( codec == null ) {
            throw new NullPointerException("null body");
        }

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        try {

            assertions.accept(actual.body(codec));

        } catch ( final CodecException error ) {

            fail(
                    "expected message to have a <%s> body but was unable to retrieve one (%s)",
                    codec.getClass().getSimpleName(), error
            );

        }

        return myself;
    }


    public A doesNotHaveBody() {

        isNotNull();

        final ByteArrayOutputStream output=new ByteArrayOutputStream();

        actual.output().accept(output);

        final byte[] data=output.toByteArray();

        if ( data.length > 0 ) {
            failWithMessage("expected empty body but had binary body of length <%d>", data.length);
        }

        return myself;
    }

    public A doesNotHaveBody(final Codec<?> codec) {

        if ( codec == null ) {
            throw new NullPointerException("null codec");
        }

        isNotNull();

        codec.decode(actual).ifPresent(value ->
                fail(format("expected message to have no <%s> body but has one", codec.getClass().getSimpleName()))
        );

        return myself;
    }


    //// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    public A hasBody(final _Format<?> format) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        return hasBody(format, body -> { });
    }

    public <V> A hasBody(final _Format<V> body, final Consumer<V> assertions) {

        if ( body == null ) {
            throw new NullPointerException("null body");
        }

        if ( assertions == null ) {
            throw new NullPointerException("null assertions");
        }

        isNotNull();

        return actual.body(body).fold(

                error -> fail(
                        "expected message to have a <%s> body but was unable to retrieve one (%s)",
                        body.getClass().getSimpleName(), error
                ),

                value -> {

                    assertions.accept(value);

                    return myself;

                }

        );
    }

    public A doesNotHaveBody(final _Format<?> body) {

        if ( body == null ) {
            throw new NullPointerException("null body");
        }

        isNotNull();

        return actual.body(body).fold(
                error -> myself, value -> fail("expected message to have no <%s> body but has one")
        );
    }

}
