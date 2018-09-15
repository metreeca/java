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

package com.metreeca.next.formats;

import com.metreeca.next.Format;
import com.metreeca.next.Message;

import java.io.Writer;
import java.util.function.Consumer;


/**
 * Inbound raw body format.
 */
public final class _Writer implements Format<Consumer<Writer>> {

	/**
	 * The singleton inbound raw body format.
	 */
	public static final _Writer Format=new _Writer();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Writer() {} // singleton


	@Override public void set(final Message<?> message, final Consumer<Writer> value) {
		if ( !message.header("content-type").isPresent() ) {
			message.header("content-type", "text/plain");
		}
	}

}
