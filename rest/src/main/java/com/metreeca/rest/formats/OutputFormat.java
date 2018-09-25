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
import com.metreeca.rest.Message;

import java.io.OutputStream;
import java.util.function.Consumer;


/**
 * Binary outbound raw body format.
 */
public final class OutputFormat implements Format<Consumer<OutputStream>> {

	/**
	 * The default MIME type for binary outbound raw message bodies.
	 */
	public static final String MIME="application/octet-stream";

	/**
	 * The singleton binary inbound raw body format.
	 */
	public static final OutputFormat asOutput=new OutputFormat();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private OutputFormat() {} // singleton


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the {@code Content-Type} header of {@code message} to {@value #MIME}, unless already defined
	 */
	@Override public <T extends Message<T>> T set(final T message, final Consumer<OutputStream> value) {
		return message.header("Content-Type", v -> v.orElse(MIME));
	}

}