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

package com.metreeca.rdf.formats;


import com.metreeca.rest.formats.JSONFormat;

import javax.json.Json;
import javax.json.JsonObject;


final class RDFJSONCodecTest {

	static final JsonObject Context=Json.createObjectBuilder()
			.add("id", JSONFormat.id)
			.add("value", JSONFormat.value)
			.add("type", JSONFormat.type)
			.add("language", JSONFormat.language)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RDFJSONCodecTest() {}

}