/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.link.shapes;

import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.Value;

import static com.metreeca.link.Values.format;


/**
 * Inclusive maximum value constraint.
 *
 * <p>States that each value in the focus set is less than or equal to a given maximum value, according to <a
 * href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules.</p>
 */
public final class MaxInclusive extends Shape {

    public static Shape maxInclusive(final Value limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        return new MaxInclusive(limit);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Value limit;


    private MaxInclusive(final Value limit) {
        this.limit=limit;
    }


    public Value limit() {
        return limit;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public <T> T map(final Probe<T> probe) {

        if ( probe == null ) {
            throw new NullPointerException("null probe");
        }

        return probe.probe(this);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public boolean equals(final Object object) {
        return this == object || object instanceof MaxInclusive
                && limit.equals(((MaxInclusive)object).limit);
    }

    @Override public int hashCode() {
        return limit.hashCode();
    }

    @Override public String toString() {
        return "maxInclusive("+format(limit)+")";
    }

}
