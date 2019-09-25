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

package com.metreeca.gcloud.services;

import com.metreeca.gcloud.GCloud;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.gcloud.services.Datastore.compare;
import static com.metreeca.gcloud.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;

import static com.google.cloud.Timestamp.now;
import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptyList;


final class DatastoreTest extends DatastoreTestBase {

	@Nested final class Compare {

		@Test void testNulls() {

			assertThat(compare(NullValue.of(), NullValue.of())).isEqualTo(0);

			assertThat(compare((Value<?>)NullValue.of(), LongValue.of(1))).isLessThan(0);
			assertThat(compare(LongValue.of(1), (Value<?>)NullValue.of())).isGreaterThan(0);
		}

		@Test void testLongs() {

			assertThat(compare(LongValue.of(1), LongValue.of(2))).isLessThan(0);
			assertThat(compare(LongValue.of(1), LongValue.of(1))).isEqualTo(0);
			assertThat(compare(LongValue.of(1), LongValue.of(1))).isEqualTo(0);
			assertThat(compare(LongValue.of(1), LongValue.of(1))).isEqualTo(0);
			assertThat(compare(LongValue.of(2), LongValue.of(1))).isGreaterThan(0);

			assertThat(compare(LongValue.of(1), TimestampValue.of(now()))).isLessThan(0);
			assertThat(compare(TimestampValue.of(now()), LongValue.of(1))).isGreaterThan(0);
		}

		@Test void testTimestamps() {

			final TimestampValue one=TimestampValue.of(Timestamp.ofTimeMicroseconds(1));
			final TimestampValue two=TimestampValue.of(Timestamp.ofTimeMicroseconds(2));

			assertThat(compare(one, two)).isLessThan(0);
			assertThat(compare(one, one)).isEqualTo(0);
			assertThat(compare(two, one)).isGreaterThan(0);

			assertThat(compare(TimestampValue.of(now()), BooleanValue.of(true))).isLessThan(0);
			assertThat(compare(BooleanValue.of(true), TimestampValue.of(now()))).isGreaterThan(0);
		}

		@Test void testBooleans() {

			assertThat(compare(BooleanValue.of(false), BooleanValue.of(true))).isLessThan(0);
			assertThat(compare(BooleanValue.of(false), BooleanValue.of(false))).isEqualTo(0);
			assertThat(compare(BooleanValue.of(true), BooleanValue.of(false))).isGreaterThan(0);

			assertThat(compare(BooleanValue.of(true), StringValue.of(""))).isLessThan(0);
			assertThat(compare(StringValue.of(""), BooleanValue.of(true))).isGreaterThan(0);
		}

		@Test void testStrings() {

			assertThat(compare(StringValue.of("x"), StringValue.of("y"))).isLessThan(0);
			assertThat(compare(StringValue.of("x"), StringValue.of("x"))).isEqualTo(0);
			assertThat(compare(StringValue.of("y"), StringValue.of("x"))).isGreaterThan(0);

			assertThat(compare(StringValue.of(""), DoubleValue.of(1.0))).isLessThan(0);
			assertThat(compare(DoubleValue.of(1.0), StringValue.of(""))).isGreaterThan(0);
		}

		@Test void testDoubles() {

			assertThat(compare(DoubleValue.of(1.0), DoubleValue.of(2.0))).isLessThan(0);
			assertThat(compare(DoubleValue.of(1.0), DoubleValue.of(1.0))).isEqualTo(0);
			assertThat(compare(DoubleValue.of(1.0), DoubleValue.of(1.0))).isEqualTo(0);
			assertThat(compare(DoubleValue.of(1.0), DoubleValue.of(1.0))).isEqualTo(0);
			assertThat(compare(DoubleValue.of(2.0), DoubleValue.of(1.0))).isGreaterThan(0);

			assertThat(compare(DoubleValue.of(1.0), ListValue.of(emptyList()))).isLessThan(0);
			assertThat(compare(ListValue.of(emptyList()), DoubleValue.of(1.0))).isGreaterThan(0);
		}

		@Test void testOthers() {
			assertThat(compare(ListValue.of(emptyList()), ListValue.of(emptyList()))).isEqualTo(0);
		}

	}

	@Nested final class Keys {

		@Test void testHierarchy() {
			exec(() -> {

				final Datastore datastore=service(datastore());

				assertThat(datastore.key("Entity", "/"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/")
						));

				assertThat(datastore.key("Entity", "/container/"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/container/")
						));

				assertThat(datastore.key("Entity", "/resource"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/resource")
						));

				assertThat(datastore.key("Entity", "/container/resource"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.addAncestor(PathElement.of(GCloud.Resource, "/container/"))
								.setKind("Entity")
								.newKey("/container/resource")
						));

				assertThat(datastore.key("Entity", "/container/container/resource"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.addAncestor(PathElement.of(GCloud.Resource, "/container/"))
								.addAncestor(PathElement.of(GCloud.Resource, "/container/container/"))
								.setKind("Entity")
								.newKey("/container/container/resource")
						));

			});

		}

		@Test void testTyping() {
			exec(() -> {

				final Datastore datastore=service(datastore());


				assertThat(datastore.key("Class", "/resource"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("/resource")
						));

				assertThat(datastore.key("Class", "/resource"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("/resource")
						));

			});
		}


		@Test void testExternal() {
			exec(() -> {

				final Datastore datastore=service(datastore());

				assertThat(datastore.key("Class", "http://example.com/path"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("http://example.com/path")
						));

			});

		}

	}

}
