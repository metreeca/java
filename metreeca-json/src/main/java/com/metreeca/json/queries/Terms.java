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

package com.metreeca.json.queries;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


public final class Terms extends Query {

	public static Terms terms(final Shape shape, final IRI... path) {
		return new Terms(shape, asList(path));
	}

	public static Terms terms(final Shape shape, final List<IRI> path) {
		return new Terms(shape, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<IRI> path;


	private Terms(final Shape shape, final List<IRI> path) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path or path step");
		}

		this.shape=shape;
		this.path=new ArrayList<>(path);
	}


	public Shape shape() {
		return shape;
	}

	public List<IRI> path() {
		return unmodifiableList(path);
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
		return this == object || object instanceof Terms
				&& shape.equals(((Terms)object).shape)
				&& path.equals(((Terms)object).path);
	}

	@Override public int hashCode() {
		return shape.hashCode()^path.hashCode();
	}

	@Override public String toString() {
		return format(
				"terms {\n\tshape: %s\n\tpath: %s\n}",
				shape.toString().replace("\n", "\n\t"), path
		);
	}

}