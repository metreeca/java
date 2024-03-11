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

import org.eclipse.rdf4j.model.IRI;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.metreeca.http.jsonld.formats.JSONLD.store;
import static com.metreeca.link.Frame.frame;

final class OperatorTest {

    private OperatorTest() { }


    static void exec(final BiPredicate<IRI, Frame> success, final Runnable task) {
        new Locator()
                .set(store(), () -> new MockStore(success))
                .exec(task)
                .clear();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class MockStore implements Store {

        private final BiPredicate<IRI, Frame> success;


        private MockStore(final BiPredicate<IRI, Frame> success) {
            this.success=success;
        }


        @Override public Optional<Frame> retrieve(final IRI id, final Shape shape, final Frame model, final List<String> langs) {
            return success.test(id, model) ? Optional.of(model) : Optional.empty();
        }

        @Override public boolean create(final IRI id, final Shape shape, final Frame state) {
            return success.test(id, state);
        }

        @Override public boolean update(final IRI id, final Shape shape, final Frame state) {
            return success.test(id, state);
        }

        @Override public boolean delete(final IRI id, final Shape shape) {
            return success.test(id, frame());
        }

    }

}
