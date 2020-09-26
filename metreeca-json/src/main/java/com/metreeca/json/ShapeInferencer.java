/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Set;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class ShapeInferencer extends Shape.Probe<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Meta meta) {
		return meta.label().equals("hint") ? and(meta, datatype(ResourceType)) : meta;
	}


	@Override public Shape probe(final Datatype datatype) {
		return datatype.iri().equals(XSD.BOOLEAN) ? and(datatype,
				range(literal(false), literal(true)), maxCount(1)
		) : datatype;
	}

	@Override public Shape probe(final Clazz clazz) {
		return and(clazz, datatype(ResourceType));
	}

	@Override public Shape probe(final Range range) {

		final Set<Value> values=range.values();
		final Set<IRI> types=values.stream().map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		final Shape type=types.size() == 1 ? datatype(types.iterator().next()) : and();

		return and(range, count, type);
	}


	@Override public Shape probe(final All all) {
		return and(all, minCount(all.values().size()));
	}

	@Override public Shape probe(final Any any) {
		return and(any, minCount(1));
	}


	@Override public Shape probe(final Field field) {

		final IRI iri=field.name();
		final Shape shape=field.shape().map(this);

		return iri.equals(RDF.TYPE) ? and(field(iri, and(shape, datatype(ResourceType))), datatype(IRIType))
				: direct(iri) ? and(field(iri, shape), datatype(ResourceType))
				: field(iri, and(shape, datatype(ResourceType)));
	}


	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final When when) {
		return when(
				when.test().map(this),
				when.pass().map(this),
				when.fail().map(this)
		);
	}

}
