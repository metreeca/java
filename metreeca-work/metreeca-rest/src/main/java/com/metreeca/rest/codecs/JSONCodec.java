package com.metreeca.rest.codecs;

import com.metreeca.core.Feeds;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.gson.*;

import java.io.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.metreeca.rest.Response.BadRequest;

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


    @Override public Response handle(final Request request, final Function<Request, Response> forward) {

        return request.header("Content-Type").filter(MIMEPattern.asPredicate()).map(type -> {

                    try (
                            final InputStream input=request.payload(Input.class).orElseGet(() -> Feeds::input).get();
                            final Reader reader=new InputStreamReader(input, request.charset())
                    ) {

                        final JsonElement element=JsonParser.parseReader(reader);

                        return forward.apply(request.payload(JsonElement.class, element));

                    } catch ( final UnsupportedEncodingException|JsonSyntaxException e ) {

                        return request.reply(BadRequest).cause(e); // !!! payload

                    } catch ( final JsonIOException e ) {

                        throw new UncheckedIOException(new IOException(e));

                    } catch ( final IOException e ) {

                        throw new UncheckedIOException(e);

                    }

                })

                .orElseGet(() -> forward.apply(request).map(response -> response.payload(JsonElement.class).map(element -> response

                                        .header("Content-Type", MIME)

                                        .payload(Output.class, output -> {
                                            try ( final Writer writer=new OutputStreamWriter(output,
                                                    response.charset()) ) {

                                                final Gson gson=new Gson(); // !!! service

                                                gson.toJson(element, writer);

                                                //} catch ( final UnsupportedEncodingException|JsonSyntaxException e ) {
                                                //
                                                //    return request.reply(BadRequest).cause(e); // !!! payload

                                            } catch ( final JsonIOException e ) {

                                                throw new UncheckedIOException(new IOException(e));

                                            } catch ( final IOException e ) {

                                                throw new UncheckedIOException(e);

                                            }

                                        })

                                )

                                .orElse(response)

                ));
    }

}
