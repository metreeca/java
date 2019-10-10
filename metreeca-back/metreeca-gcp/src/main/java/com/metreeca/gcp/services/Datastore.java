/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gcp.services;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import java.util.Date;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.cloud.datastore.ValueType.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;


/**
 * Google Cloud Datastore.
 *
 * <p>Manages task execution on Cloud Datastore.</p>
 *
 * <p>Nested task executions on the datastore from the same thread will share the same transaction through a {@link
 * ThreadLocal} context variable.</p>
 */
public final class Datastore {

	private static final ThreadLocal<Transaction> context=new ThreadLocal<>();


	/**
	 * Retrieves the default datastore factory.
	 *
	 * @return the default datastore factory, which creates datastores with the default configuration
	 */
	public static Supplier<Datastore> datastore() {
		return () -> new Datastore(DatastoreOptions.getDefaultInstance());
	}


	//// Sorting ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Compares values.
	 *
	 * <p>Comparison is consistent with Google Datastore <a href="https://cloud.google.com/datastore/docs/concepts/entities#value_type_ordering">sorting
	 * rules</a>, with the following deterministic ordering for mixed value types:</p>
	 *
	 * <ul>
	 *     <li>null values;</li>
	 *     <li>integer values;</li>
	 *     <li>timestamp values;</li>
	 *     <li>boolean values;</li>
	 *     <li>blob values;</li>
	 *     <li>string values;</li>
	 *     <li>double values;</li>
	 *     <li>point values;</li>
	 *     <li>key values;</li>
	 *     <li>entity values;</li>
	 *     <li>other values (system-dependent order).</li>
	 * </ul>
	 *
	 * @param x the first object to be compared.
	 * @param y the second object to be compared.
	 *
	 * @return a negative integer, zero, or a positive integer as the {@code x} is less than, equal to, or greater than
	 * {@code y}
	 */
	static int compare(final Value<?> x, final Value<?> y) {

		if ( x == null ) {
			throw new NullPointerException("null x");
		}

		if ( y == null ) {
			throw new NullPointerException("null y");
		}

		final ValueType xtype=x.getType();
		final ValueType ytype=y.getType();

		return xtype == NULL ? ytype == NULL ? 0 : -1 : ytype == NULL ? 1

				: xtype == LONG ? ytype == LONG ? compare((LongValue)x, (LongValue)y) : -1 : ytype == LONG ? 1

				: xtype == TIMESTAMP ? ytype == TIMESTAMP ? compare((TimestampValue)x, (TimestampValue)y) : -1 : ytype == TIMESTAMP ? 1

				: xtype == BOOLEAN ? ytype == BOOLEAN ? compare((BooleanValue)x, (BooleanValue)y) : -1 : ytype == BOOLEAN ? 1

				// !!! blobs

				: xtype == STRING ? ytype == STRING ? compare((StringValue)x, (StringValue)y) : -1 : ytype == STRING ? 1

				: xtype == DOUBLE ? ytype == DOUBLE ? compare((DoubleValue)x, (DoubleValue)y) : -1 : ytype == DOUBLE ? 1

				// !!! points

				: xtype == KEY ? ytype == KEY ? compare((KeyValue)x, (KeyValue)y) : -1 : ytype == KEY ? 1

				: xtype == ENTITY ? ytype == ENTITY ? compare((EntityValue)x, (EntityValue)y) : -1 : ytype == ENTITY ? 1

				: 0;
	}


	private static int compare(final LongValue x, final LongValue y) {
		return Long.compare(x.get(), y.get());
	}

	private static int compare(final TimestampValue x, final TimestampValue y) {
		return x.get().compareTo(y.get());
	}

	private static int compare(final BooleanValue x, final BooleanValue y) {
		return Boolean.compare(x.get(), y.get());
	}

	private static int compare(final StringValue x, final StringValue y) {
		return x.get().compareTo(y.get());
	}

	private static int compare(final DoubleValue x, final DoubleValue y) {
		return Double.compare(x.get(), y.get());
	}

	private static int compare(final KeyValue x, final KeyValue y) {
		return compare(x.get(), y.get());
	}

	private static int compare(final EntityValue x, final EntityValue y) {
		return compare(x.get().getKey(), y.get().getKey());
	}

	private static int compare(final IncompleteKey x, final IncompleteKey y) {
		return (x == null ? "" : x.toString()).compareTo(y == null ? "" : y.toString()); // !!! review
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final com.google.cloud.datastore.Datastore datastore;


	/**
	 * Create a new datastore.
	 *
	 * @param options the datastore options
	 *
	 * @throws NullPointerException if {@code options} is null
	 */
	public Datastore(final DatastoreOptions options) {

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.datastore=options.getService();
	}


	/**
	 * Creates a key factory for this datastore.
	 *
	 * @return a new key factory for this datastore.
	 */
	public KeyFactory newKeyFactory() {
		return datastore.newKeyFactory();
	}


	/**
	 * Executes a task inside a transaction on this datastore.
	 *
	 * <p>If a transaction is not already active on the datastore, begins one and commits it on successful task
	 * completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in either
	 * case no action is taken if the transaction was already closed inside the task.</p>
	 *
	 * @param task the task to be executed; takes as argument a datastore service
	 * @param <V>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <V> V exec(final Function<com.google.cloud.datastore.Datastore, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( context.get() != null ) {

			return requireNonNull(task.apply(datastore), "null task return value");

		} else {

			final Transaction txn=datastore.newTransaction();

			context.set(txn);

			try {

				final V value=requireNonNull(task.apply(datastore), "null task return value");

				if ( txn.isActive() ) { txn.commit(); }

				return value;

			} catch ( final Throwable t ) {

				if ( txn.isActive() ) { txn.rollback(); }

				throw t;

			} finally {

				context.remove();

			}

		}

	}

}
