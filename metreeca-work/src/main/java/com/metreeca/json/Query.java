package com.metreeca.json;

import com.metreeca.json.queries.*;

import java.util.function.Function;

public abstract class Query {

    public abstract <V> V accept(final Visitor<V> visitor);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Query visitor.
     *
     * <p>Generates a result by visiting queries.</p>
     *
     * @param <V> the type of the generated result value
     */
    public abstract static class Visitor<V> implements Function<Query, V> {

        @Override public final V apply(final Query query) {

            if ( query == null ) {
                throw new NullPointerException("null query");
            }

            return query.accept(this);
        }


        public abstract V visit(final ItemsQuery query);

        public abstract V visit(final SlotsQuery query);

        public abstract V visit(final TermsQuery query);

        public abstract V visit(final StatsQuery query);

    }

}
