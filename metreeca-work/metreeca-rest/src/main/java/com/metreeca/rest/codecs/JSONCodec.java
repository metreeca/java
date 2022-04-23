package com.metreeca.rest.codecs;

import com.metreeca.core.Feeds;
import com.metreeca.rest.*;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.InternalServerError;

public final class JSONCodec extends Codec<JsonElement> {

    /**
     * The default MIME type for JSON messages ({@value}).
     */
    public static final String MIME="application/json";

    /**
     * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
     */
    public static final Pattern MIMEPattern=Pattern.compile(
            "(?i:^(text/json|application/(?:.*\\+)?json)(?:\\s*;.*)?$)"
    );


    private static final Class<JsonElement> Type=JsonElement.class;


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        try {

            return forward.apply(process(request)).map(response -> {

                try {

                    return process(response);

                } catch ( final UnsupportedCharsetException|JsonSyntaxException e ) {

                    return request.reply(InternalServerError).cause(e);

                }

            });

        } catch ( final UnsupportedCharsetException|JsonSyntaxException e ) {

            return request.reply(BadRequest).payload(String.class, e.getMessage());

        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <M extends Message<M>> M process(final M message) {
        return encode(message)
                .or(() -> decode(message))
                .orElse(message);
    }

    private <M extends Message<M>> Optional<M> encode(final M message) throws UnsupportedCharsetException {
        return message.payload(Type).map(element -> message

                .header("Content-Type", MIME)

                .payload(Output.class, output -> {

                    try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

                        final Gson gson=new Gson(); // !!! service

                        gson.toJson(element, writer);

                    } catch ( final JsonIOException e ) {

                        throw new UncheckedIOException(new IOException(e));

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                })

        );
    }

    private <M extends Message<M>> Optional<M> decode(final M message) throws UnsupportedCharsetException,
            JsonSyntaxException {
        return message.header("Content-Type").filter(MIMEPattern.asPredicate()).map(type -> {

            try (
                    final InputStream input=message.payload(Input.class).orElseGet(() -> Feeds::input).get();
                    final Reader reader=new InputStreamReader(input, message.charset())
            ) {

                final JsonElement element=JsonParser.parseReader(reader);

                return message.payload(Type, element);

            } catch ( final JsonIOException e ) {

                throw new UncheckedIOException(new IOException(e));

            } catch ( final IOException e ) {

                throw new UncheckedIOException(e);

            }

        });
    }

}
