

/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.http.jsonld.actions;

import com.metreeca.http.services.Logger;
import com.metreeca.link.Frame;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.http.Locator.service;
import static com.metreeca.http.services.Logger.logger;
import static com.metreeca.link.Frame.frame;

import static java.lang.String.format;

/**
 * Model-based validation.
 *
 * <p>{@linkplain Frame#validate() Validates} objects against their expected shape.</p>
 *
 * @param <T> the type of the object to be validated
 */
public final class Validate<T> implements Function<T, Optional<T>> {

    private final Logger logger=service(logger());


    @Override public Optional<T> apply(final T object) {

        final Frame<T> frame=frame(object);

        return frame.validate()

                .map(trace -> {

                    logger.warning(this, () -> format("%s %s", object, trace));

                    return Optional.<T>empty();

                })

                .orElseGet(() -> {

                    logger.debug(this, () -> format("%s {}", frame.id()));

                    return Optional.of(frame.value());

                });
    }

}
