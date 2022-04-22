package com.metreeca.rest.handlers;

import com.metreeca.rest.*;

import java.util.function.Function;

/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, possibly assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

    private Handler delegate=(request, next) -> request.reply();


    /**
     * Configures the delegate handler.
     *
     * @param delegate the handler request processing is delegated to
     *
     * @return this handler
     *
     * @throws NullPointerException if {@code delegate} is null
     */
    protected com.metreeca.rest.handlers.Delegator delegate(final Handler delegate) {

        if ( delegate == null ) {
            throw new NullPointerException("null delegate");
        }

        this.delegate=delegate;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Handler with(final Wrapper wrapper) { return delegate.with(wrapper); }

    @Override public Response handle(final Request request, final Function<Request, Response> forward) {
        return delegate.handle(request, forward);
    }

}
