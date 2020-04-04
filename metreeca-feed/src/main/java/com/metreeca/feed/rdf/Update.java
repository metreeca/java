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

package com.metreeca.feed.rdf;

import java.util.function.Consumer;

import static com.metreeca.rest.services.Logger.time;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


public final class Update extends Operation<Update> implements Consumer<String> {

	@Override public void accept(final String update) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		graph().exec(connection -> {
			return time(() ->

					configure(connection.prepareUpdate(SPARQL, update)).execute()

			).apply(t -> logger().info(this, String.format("executed in <%,d> ms", t)));
		});
	}

}
