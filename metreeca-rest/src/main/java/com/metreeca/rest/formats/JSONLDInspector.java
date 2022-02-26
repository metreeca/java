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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.metreeca.json.shapes.Guard.*;

import static java.util.stream.Collectors.toSet;

abstract class JSONLDInspector<V> extends Shape.Probe<Stream<V>> {

    static Shape driver(final Shape shape) { // !!! caching
        return shape

                .redact(Role)
                .redact(Task)
                .redact(View)
                .redact(Mode, Convey) // remove internal filtering shapes

                .expand(); // add inferred constraints to drive json shorthands
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static boolean tagged(final Shape shape) {
        return datatype(shape).filter(RDF.LANGSTRING::equals).isPresent();
    }

    static boolean localized(final Shape shape) {
        return shape != null && shape.map(new JSONLDInspector<>() {

            @Override public Stream<Object> probe(final Localized localized) { return Stream.of(localized); }

        }).findAny().isPresent();
    }


    static Optional<IRI> datatype(final Shape shape) {
        return shape == null ? Optional.empty() : Optional

                .of(shape.map(new JSONLDInspector<IRI>() {

                    @Override public Stream<IRI> probe(final Datatype datatype) { return Stream.of(datatype.iri()); }

                }).collect(toSet()))


                .filter(datatypes -> datatypes.size() == 1)
                .map(datatypes -> datatypes.iterator().next());
    }

    static Set<String> langs(final Shape shape) {
        return shape == null ? Set.of() : shape.map(new JSONLDInspector<String>() {

            @Override public Stream<String> probe(final Lang lang) { return lang.tags().stream(); }

        }).collect(toSet());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override public Stream<V> probe(final Link link) {
        return link.shape().map(this);
    }


    @Override public Stream<V> probe(final When when) {
        return Stream.of(when.pass(), when.fail()).flatMap(shape -> shape.map(this));
    }

    @Override public Stream<V> probe(final And and) {
        return and.shapes().stream().flatMap(shape -> shape.map(this));
    }

    @Override public Stream<V> probe(final Or or) {
        return or.shapes().stream().flatMap(shape -> shape.map(this));
    }


    @Override protected Stream<V> probe(final Shape shape) {
        return Stream.empty();
    }

}
