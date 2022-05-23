

/*
 * Copyright © 2013-2022 Metreeca srl
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

package com.metreeca.jsonld.actions;

import com.metreeca.http.services.Logger;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.services.Logger.logger;
import static com.metreeca.link.Frame.frame;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Shape-based validation.
 *
 * <p>Validates {@linkplain Frame frames} against a {@linkplain Shape shape}.</p>
 */
public final class Validate implements Function<Frame, Optional<Frame>> {

	private final Shape shape;

	private final Logger logger=service(logger());


	/**
	 * Creates a shape-based validation action.
	 *
	 * @param shape the shape frames are to be validated against
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Validate(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;
	}


	@Override public Optional<Frame> apply(final Frame frame) {

		return shape.validate(frame.focus(), frame.model().collect(toList())).fold(

				trace -> {

					logger.warning(this, () -> format("%s %s", frame.focus(), trace));

					return Optional.empty();

				},

				model -> Optional.of(frame(frame.focus(), model))

		);
	}

}
