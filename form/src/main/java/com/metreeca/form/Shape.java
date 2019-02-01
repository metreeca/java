/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form;

import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.Value;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.Or.or;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	public static final Shape Pass=and();
	public static final Shape Fail=or();


	public static Shape pass() {
		return Pass;
	}

	public static boolean pass(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Pass.equals(shape);
	}


	public static Shape fail() {
		return Fail;
	}

	public static boolean fail(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Fail.equals(shape);
	}


	public static boolean empty(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return pass(shape) || fail(shape);
	}


	//// Shorthands ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Value... values) { return and(all(values), in(values)); }


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Guard role(final Value... roles) { return guard(Form.role, roles); }

	public static Guard task(final Value... tasks) { return guard(Form.task, tasks); }

	public static Guard view(final Value... views) { return guard(Form.view, views); }

	public static Guard mode(final Value... modes) { return guard(Form.mode, modes); }


	public static Guard create() { return task(Form.create); }

	public static Guard relate() { return task(Form.relate); }

	public static Guard update() { return task(Form.update); }

	public static Guard delete() { return task(Form.delete); }


	/**
	 * Marks shapes as server-defined read-only.
	 */
	public static Guard server() { return task(Form.relate, Form.delete); }

	/**
	 * Marks shapes as client-defined write-once.
	 */
	public static Guard client() { return task(Form.create, Form.relate, Form.delete); }


	public static Guard digest() { return view(Form.digest); }

	public static Guard detail() { return view(Form.detail); }


	public static Guard verify() { return mode(Form.verify); }

	public static Guard filter() { return mode(Form.filter); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V map(final Probe<V> probe);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Shape probe.
	 *
	 * <p>Generates a result by probing shapes.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public static interface Probe<V> {

		//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Meta meta);

		public V probe(final Guard guard);


		//// Term Constraints //////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Datatype datatype);

		public V probe(final Clazz clazz);


		public V probe(final MinExclusive minExclusive);

		public V probe(final MaxExclusive maxExclusive);

		public V probe(final MinInclusive minInclusive);

		public V probe(final MaxInclusive maxInclusive);


		public V probe(final MinLength minLength);

		public V probe(final MaxLength maxLength);

		public V probe(final Pattern pattern);

		public V probe(final Like like);


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount);

		public V probe(final MaxCount maxCount);

		public V probe(final In in);

		public V probe(final All all);

		public V probe(final Any any);


		//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Field field);


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and);

		public V probe(final Or or);

		public V probe(final When when);

	}

}
