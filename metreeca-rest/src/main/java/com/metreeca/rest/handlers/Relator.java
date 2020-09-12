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

package com.metreeca.rest.handlers;

import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Message;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;

import static com.metreeca.rest.Wrapper.wrapper;


/**
 * Model-driven resource relator.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>shape-based {@linkplain Actor#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Relate} task and {@linkplain Guard#Target}/{@linkplain Guard#Digest} areas, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Guard#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#relate(Request) retrieval};</li>
 *
 * <li>engine-assisted response shape/payload {@linkplain Engine#trim(Message) trimming}, considering shapes as above
 * .</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine#exec(Runnable) engine transaction}.</p>
 */
public final class Relator extends Actor {

	/**
	 * Creates a resource relator.
	 *
	 * @return a new resource relator
	 */
	public static Relator relator() {
		return new Relator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Relator() {
		delegate(_relator()

				.with(connector())
				.with(trimmer())
				.with(wrapper(Request::collection,
						throttler(Guard.Relate, Guard.Target, Guard.Digest),
						throttler(Guard.Relate, Guard.Detail)
				))

		);
	}

}
