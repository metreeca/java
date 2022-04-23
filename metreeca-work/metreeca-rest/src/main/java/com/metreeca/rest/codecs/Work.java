package com.metreeca.rest.codecs;

import com.metreeca.rest.*;

import java.util.function.BiFunction;

public final class Work {

    private Handler handler=handler(Object.class, (request, object) ->

            request.reply().payload(Object.class, object)

    );

    public static <T> Handler handler(final Class<T> type,
            final BiFunction<? super Request, ? super T, Response> mapper) {
        return (request, forward) -> request.payload(type)
                .map(value -> mapper.apply(request, value))
                .orElseGet(() -> forward.apply(request));
    }

}
