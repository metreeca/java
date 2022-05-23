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

package com.metreeca.jsonld.handlers;

import com.metreeca.http.Locator;
import com.metreeca.jsonld.services.Engine;
import com.metreeca.link.*;

import java.util.Optional;
import java.util.function.Predicate;



final class OperatorTest {

	private OperatorTest() {}


	static void exec(final Predicate<Frame> success, final Runnable task) {
        new Locator()
                .set(Engine.engine(), () -> new MockEngine(success))
				.exec(task)
				.clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MockEngine implements Engine {

		private final Predicate<Frame> success;


		private MockEngine(final Predicate<Frame> success) {
			this.success=success;
		}


		@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
			return Optional.of(frame).filter(success);
		}

		@Override public Optional<Frame> relate(final Frame frame, final Query query) {
			return Optional.of(frame).filter(success);
		}

		@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
			return Optional.of(frame).filter(success);
		}

		@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
			return Optional.of(frame).filter(success);
		}

	}

}
