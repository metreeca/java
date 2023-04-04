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

package com.metreeca.core;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.core.Locator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class LocatorTest {

    @Test void testReplacesToolsWithPlugins() {

        final Locator locator=new Locator();

        final Supplier<Object> target=() -> "target";
        final Supplier<Object> plugin=() -> "plugin";

        locator.set(target, plugin);

        assertThat(locator.get(target))
                .isEqualTo(plugin.get());

    }

    @Test void testReleaseAutoCloseableResources() {

        final Locator locator=new Locator();

        final class Resource implements AutoCloseable {

            private boolean closed;

            private boolean isClosed() {
                return closed;
            }

            @Override public void close() {
                this.closed=true;
            }

        }

        final Supplier<Resource> service=Resource::new;

        final Resource resource=locator.get(service);

        locator.clear();

        assertThat(resource.isClosed())
                .isTrue();

    }

    @Test void testReleaseDependenciesAfterResource() {

        final Locator locator=new Locator();

        final Collection<Object> released=new ArrayList<>();

        final class Step implements Supplier<AutoCloseable>, AutoCloseable {

            private final Supplier<AutoCloseable> dependency;


            private Step(final Supplier<AutoCloseable> dependency) {
                this.dependency=dependency;
            }


            @Override public AutoCloseable get() {

                if ( dependency != null ) { service(dependency); }

                return this;
            }

            @Override public void close() {
                released.add(this);
            }

        }

        final Step z=new Step(null);
        final Step y=new Step(z);
        final Step x=new Step(y);

        locator.get(x); // load the terminal service with its dependencies
        locator.clear(); // release resources

        assertThat(released)
                .as("dependencies released after relying resources")
                .containsExactly(x, y, z);
    }

    @Test void testPreventToolBindingIfAlreadyInUse() {

        final Locator locator=new Locator();
        final Supplier<Object> service=Object::new;

        assertThatThrownBy(() -> {

            locator.get(service);
            locator.set(service, Object::new);

        })
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void testTrapCircularDependencies() {

        final Locator locator=new Locator();
        final Object delegate=new Object();

        assertThatThrownBy

                (() -> locator.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return locator.get(this);
                    }
                }))

                .isInstanceOf(IllegalStateException.class);

        assertThat

                (locator.get(new Supplier<Object>() {
                    @Override public Object get() {
                        return locator.get(this, () -> delegate);
                    }
                }))

                .isEqualTo(delegate);

    }


    @Test void testHandleExceptionsInFactories() {

        final Locator locator=new Locator();

        final Supplier<Object> service=() -> {
            throw new NoSuchElementException("missing resource");
        };

        assertThatThrownBy(() ->

                locator.get(service)
        )
                .isInstanceOf(NoSuchElementException.class);

    }

}
