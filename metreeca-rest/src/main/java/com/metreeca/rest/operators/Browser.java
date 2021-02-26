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

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.IRI;

import javax.json.JsonObject;

import static com.metreeca.json.shapes.Guard.Digest;
import static com.metreeca.json.shapes.Guard.Relate;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Engine.*;


/**
 * Model-driven container browser.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Engine#throttler(Object, Object) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Relate} task and {@linkplain Guard#Digest} view;</li>
 *
 * <li>engine assisted container {@linkplain Engine#browse(Request) browsing};</li>
 *
 * <li>engine-assisted response payload {@linkplain JSONLDFormat#trim(IRI, Shape, JsonObject) trimming}, considering
 * shapes as above.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine engine transaction}.</p>
 */
public final class Browser extends Delegator {

	/**
	 * Creates a container browser.
	 *
	 * @return a new container browser
	 */
	public static Browser browser() {
		return new Browser();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Browser() {

		final Engine engine=asset(engine());

		delegate(((Handler)engine::browse)

				.with(engine)
				.with(trimmer())
				.with(throttler(Relate, Digest))

		);
	}

}
