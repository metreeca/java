/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.util.*;
import java.util.function.Function;

import javax.json.Json;

import static com.metreeca.rest.Response.UnprocessableEntity;
import static com.metreeca.rest._MessageException.status;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;


/**
 * Validating preprocessor.
 *
 * <p>Applies custom validation {@linkplain #Validator(Function[]) rules} to incoming requests.</p>
 */
public final class Validator implements _Wrapper {

	private final Collection<Function<Request, Collection<String>>> rules;


	/**
	 * Creates a validating preprocessor.
	 *
	 * <p>Validation rules handle a target request and must return a non-null but possibly empty collection of
	 * validation issues; if the collection is not empty, the request fails with a {@link Response#UnprocessableEntity}
	 * status code; otherwise, the request is routed to the wrapped handler.</p>
	 *
	 * @param rules the custom validation rules to be applied to incoming requests
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	@SafeVarargs public Validator(final Function<Request, Collection<String>>... rules) {

		if ( rules == null || stream(rules).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null rules");
		}

		this.rules=new LinkedHashSet<>(asList(rules));
	}

	/**
	 * Creates a validating preprocessor.
	 *
	 * <p>Validation rules handle a target request and must return a non-null but possibly empty collection of
	 * validation issues; if the collection is not empty, the request fails with a {@link Response#UnprocessableEntity}
	 * status code; otherwise, the request is routed to the wrapped handler.</p>
	 *
	 * @param rules the custom validation rules to be applied to incoming requests
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	public Validator(final Collection<Function<Request, Collection<String>>> rules) {

		if ( rules == null || rules.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null rules");
		}

		this.rules=new LinkedHashSet<>(rules);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return (request, next) -> Optional

                .of(rules.stream()
                        .flatMap(rule -> rule.apply(request).stream())
                        .collect(toList())
                )

                .filter(issues -> !issues.isEmpty())

                .map(issues -> Json.createObjectBuilder()
                        .add("", Json.createArrayBuilder(issues)) // !!! align with JSON validator format
                        .build()
                )

				.map(details -> request.reply().map(status(UnprocessableEntity, details)))

                .orElseGet(() -> handler.handle(request, next));
	}

}
