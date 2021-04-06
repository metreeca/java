/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rest.operators;

import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;

import static com.metreeca.json.shapes.Guard.Delete;
import static com.metreeca.json.shapes.Guard.Detail;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Wrapper.keeper;
import static com.metreeca.rest.services.Engine.engine;


/**
 * Model-driven resource deleter.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Wrapper#keeper(Object, Object) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Delete} task and the {@linkplain Guard#Detail} view;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#delete(Request) deletion}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine#transaction() engine transaction}.</p>
 */
public final class Deleter extends Delegator {

	/**
	 * Creates a resource deleter.
	 *
	 * @return a new resource deleter
	 */
	public static Deleter deleter() {
		return new Deleter();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Deleter() {

		final Engine engine=service(engine());

		delegate(((Handler)engine::delete)

				.with(engine.transaction())
				.with(keeper(Delete, Detail))

		);
	}

}
