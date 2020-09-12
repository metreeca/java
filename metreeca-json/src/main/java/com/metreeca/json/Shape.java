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

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Guard.guard;
import static com.metreeca.json.shapes.In.in;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.When.when;
import static java.util.Arrays.asList;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	/**
	 * Retrieves the default shape asset factory.
	 *
	 * @return the default shape factory, which returns an {@linkplain Or#or() empty disjunction}, that is a shape
	 * the always fail to validate
	 */
	public static Supplier<Shape> shape() {
		return Or::or;
	}


	//// Parametric Axes and Values ////////////////////////////////////////////////////////////////////////////////////

	public static final String Role="role";
	public static final String Task="task";
	public static final String Area="area";
	public static final String Mode="mode";

	public static final String Create="create";
	public static final String Relate="relate";
	public static final String Update="update";
	public static final String Delete="delete";

	public static final String Target="target";
	public static final String Digest="digest";
	public static final String Detail="detail";

	public static final String Convey="convey";
	public static final String Filter="filter";


	//// Shape Shorthands //////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Value... values) { return and(all(values), in(values)); }


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape role(final Object... roles) { return guard(Role, roles); }

	public static Shape task(final Object... tasks) { return guard(Task, tasks); }

	public static Shape area(final Object... areas) { return guard(Area, areas); }

	public static Shape mode(final Object... modes) { return guard(Mode, modes); }


	public static Shape create() { return task(Create); }

	public static Shape relate() { return task(Relate); }

	public static Shape update() { return task(Update); }

	public static Shape delete() { return task(Delete); }


	/*
	 * Marks shapes as server-defined internal.
	 */
	public static Shape hidden() { return task(Delete); }

	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server() { return task(Relate, Delete); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client() { return task(Create, Relate, Delete); }


	public static Shape target() { return area(Target); }

	public static Shape member() { return area(Digest, Detail); }

	public static Shape digest() { return area(Digest); }

	public static Shape detail() { return area(Detail); }


	public static Shape convey() { return mode(Convey); }

	public static Shape filter() { return mode(Filter); }


	//// Evaluation ///////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests if a shape is always matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal to an {@linkplain And#and() empty conjunction}, ignoring
	 * {@linkplain Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean pass(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.equals(and());
	}

	/**
	 * Tests if a shape is never matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal to an {@linkplain Or#or() empty disjunction}, ignoring
	 * {@linkplain
	 * Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean fail(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Boolean.FALSE.equals(shape.map(Evaluator.Instance));
	}

	/**
	 * Tests if a shape is empty.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal either to an {@linkplain And#and() empty conjunction} or to an
	 * {@linkplain Or#or() empty disjunction}, ignoring {@linkplain Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean empty(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(Evaluator.Instance) != null;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V map(final Probe<V> probe);

	public default <V> V map(final Function<Shape, V> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Use this shape as a test condition.
	 *
	 * @param shapes the shapes this shape is to be applied as a test condition
	 *
	 * @return a {@linkplain When#when(Shape, Shape) conditional} shape applying this shape as test condition to {@code
	 * shapes}
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public default Shape then(final Shape... shapes) {
		return then(asList(shapes));
	}

	/**
	 * Use this shape as a test condition.
	 *
	 * @param shapes the shapes this shape is to be applied as a test condition
	 *
	 * @return a {@linkplain When#when(Shape, Shape) conditional} shape applying this shape as test condition to {@code
	 * shapes}
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public default Shape then(final Collection<Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		return when(this, shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Shape probe.
	 *
	 * <p>Generates a result by probing shapes.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public abstract static class Probe<V> implements Function<Shape, V> {

		public V apply(final Shape shape) {
			return shape == null ? null : shape.map(this);
		}


		//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Meta meta) { return probe((Shape)meta); }

		public V probe(final Guard guard) { return probe((Shape)guard); }


		//// Value Constraints /////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Datatype datatype) { return probe((Shape)datatype); }

		public V probe(final Clazz clazz) { return probe((Shape)clazz); }


		public V probe(final MinExclusive minExclusive) { return probe((Shape)minExclusive); }

		public V probe(final MaxExclusive maxExclusive) { return probe((Shape)maxExclusive); }

		public V probe(final MinInclusive minInclusive) { return probe((Shape)minInclusive); }

		public V probe(final MaxInclusive maxInclusive) { return probe((Shape)maxInclusive); }


		public V probe(final MinLength minLength) { return probe((Shape)minLength); }

		public V probe(final MaxLength maxLength) { return probe((Shape)maxLength); }

		public V probe(final Pattern pattern) { return probe((Shape)pattern); }

		public V probe(final Like like) { return probe((Shape)like); }


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount) { return probe((Shape)minCount); }

		public V probe(final MaxCount maxCount) { return probe((Shape)maxCount); }

		public V probe(final In in) { return probe((Shape)in); }

		public V probe(final All all) { return probe((Shape)all); }

		public V probe(final Any any) { return probe((Shape)any); }


		//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Field field) { return probe((Shape)field); }


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and) { return probe((Shape)and); }

		public V probe(final Or or) { return probe((Shape)or); }

		public V probe(final When when) { return probe((Shape)when); }


		//// Fallback //////////////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Probes a generic shape.
		 *
		 * @param shape the generic shape to be probed
		 *
		 * @return the result generated by probing {@code shape}; by default {@code null}
		 */
		public V probe(final Shape shape) { return null; }

	}

}
