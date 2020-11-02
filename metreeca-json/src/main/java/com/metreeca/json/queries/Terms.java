/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.json.queries;

import com.metreeca.json.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.List;

import static java.util.Collections.emptyList;


public final class Terms extends Query {

	public static Terms terms(final Shape shape, final List<IRI> path, final int offset, final int limit) {
		return new Terms(shape, path, emptyList(), offset, limit);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Terms(final Shape shape, final List<IRI> path, final List<Order> orders, final int offset, final int limit) {
		super(shape, path, orders, offset, limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}

}
