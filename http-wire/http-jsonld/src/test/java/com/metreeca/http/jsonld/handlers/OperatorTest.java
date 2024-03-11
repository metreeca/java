/*
 * Copyright © 2013-2024 Metreeca srl
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

package com.metreeca.http.jsonld.handlers;

import com.metreeca.http.Locator;
import com.metreeca.link.Frame;
import com.metreeca.link.Shape;
import com.metreeca.link.Store;

import java.util.Optional;
import java.util.function.Predicate;

import static com.metreeca.http.jsonld.formats.JSONLD.store;

final class OperatorTest {

    private OperatorTest() { }


    static void exec(final Predicate<Frame> success, final Runnable task) {
        new Locator()
                .set(store(), () -> new MockStore(success))
                .exec(task)
                .clear();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class MockStore implements Store {

        private final Predicate<Frame> success;


        private MockStore(final Predicate<Frame> success) {
            this.success=success;
        }


        @Override public Optional<Frame> retrieve(final Shape shape, final Frame model) {
            return Optional.of(model).filter(success);
        }

        @Override public boolean create(final Shape shape, final Frame frame) {
            return success.test(frame);
        }

        @Override public boolean update(final Shape shape, final Frame frame) {
            return success.test(frame);
        }

        @Override public boolean delete(final Shape shape, final Frame frame) {
            return success.test(frame);
        }

    }

}
