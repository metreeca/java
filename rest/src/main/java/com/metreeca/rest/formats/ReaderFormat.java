/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.formats;

import com.metreeca.rest.Format;

import java.io.Reader;
import java.util.function.Supplier;


/**
 * Textual input body format.
 */
public final class ReaderFormat implements Format<Supplier<Reader>> {

	private static final ReaderFormat Instance=new ReaderFormat();

	/**
	 * Retrieves the textual input body format.
	 *
	 * @return the singleton textual input body format instance
	 */
	public static ReaderFormat reader() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ReaderFormat() {}

}
