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

package com.metreeca.jsonld.handlers;

import com.metreeca.http.Locator;
import com.metreeca.link.Engine;

import java.util.Optional;
import java.util.function.Predicate;

import static com.metreeca.jsonld.formats.Bean.engine;


final class _OperatorTest {

    private _OperatorTest() { }


    static void exec(final Predicate<Object> success, final Runnable task) {
        new Locator()
                .set(engine(), () -> new MockEngine(success))
                .exec(task)
                .clear();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class MockEngine implements Engine {

        private final Predicate<Object> success;


        private MockEngine(final Predicate<Object> success) {
            this.success=success;
        }


        @Override public <V> Optional<V> retrieve(final V v) {
            return Optional.of(v).filter(success);
        }

        @Override public <V> Optional<V> create(final V v) {
            return Optional.of(v).filter(success);
        }

        @Override public <V> Optional<V> update(final V v) {
            return Optional.of(v).filter(success);
        }

        @Override public <V> Optional<V> delete(final V v) {
            return Optional.of(v).filter(success);
        }

    }

}
