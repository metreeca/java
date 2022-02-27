package com.metreeca.json.queries;

import com.metreeca.json.Query;

public final class StatsQuery extends Query {

    @Override public <V> V accept(final Visitor<V> visitor) {

        if ( visitor == null ) {
            throw new NullPointerException("null visitor");
        }

        return visitor.visit(this);
    }

}
