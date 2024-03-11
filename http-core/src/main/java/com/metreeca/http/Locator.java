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

package com.metreeca.http;


import com.metreeca.http.services.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;


/**
 * Service locator {thread-safe}.
 *
 * <p>Manages the lifecycle of shared services.</p>
 */
@SuppressWarnings("unchecked") public final class Locator {

    private static final ThreadLocal<Locator> scope=new ThreadLocal<>();


    /**
     * Retrieves the default base path factory.
     *
     * @return the default base path factory, which returns the path of the current working directory of the process in
     * the host filesystem
     */
    public static Supplier<Path> path() {
        return () -> Paths.get("").toAbsolutePath();
    }


    /**
     * {@linkplain #get(Supplier) Retrieves} a shared service from the current locator.
     *
     * @param factory the factory responsible for creating the required shared service; must return a non-null and
     *                thread-safe object
     * @param <T>     the type of the shared service created by {@code factory}
     *
     * @return the shared service created by {@code factory} or by its plugin replacement if one was
     * {@linkplain #set(Supplier, Supplier) specified}
     *
     * @throws IllegalArgumentException if {@code factory} is null
     * @throws IllegalStateException    if called outside an active locator or a circular service dependency is detected
     */
    public static <T> T service(final Supplier<T> factory) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        return locator().get(factory);
    }

    /**
     * {@linkplain #get(Supplier, Supplier) Retrieves} a shared assert from the active locator.
     *
     * @param factory  the factory responsible for creating the required shared service; must return a non-null and
     *                 thread-safe object
     * @param delegate the factory responsible for creating a fallback delegate service if a circular service dependency
     *                 is detected
     * @param <T>      the type of the shared service created by {@code factory} and {@code delegate}
     *
     * @return the shared service created by {@code factory} or by its plugin replacement if one was
     * {@linkplain #set(Supplier, Supplier) specified}
     *
     * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
     * @throws IllegalStateException    if called outside an active locator
     */
    public static <T> T service(final Supplier<T> factory, final Supplier<? extends T> delegate) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        if ( delegate == null ) {
            throw new NullPointerException("null delegate");
        }

        return locator().get(factory, delegate);
    }


    private static Locator locator() {

        final Locator locator=scope.get();

        if ( locator == null ) {
            throw new IllegalStateException("not running inside a service locator");
        }

        return locator;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Supplier<?>, Supplier<?>> factories=new HashMap<>();
    private final Map<Supplier<?>, Object> services=new LinkedHashMap<>(); // preserve initialization order

    private final Object pending=new Object(); // placeholder for detecting circular dependencies


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the shared service created by a factory.
     *
     * <p>The new service is cached so that further calls for the same factory are idempotent.</p>
     *
     * <p>During object construction, nested shared service dependencies may be retrieved from this locator through
     * the static {@linkplain  #service(Supplier) service locator} method of the locator class. The locator used by the
     * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
     * constructors executed on a different thread.</p>
     *
     * @param factory the factory responsible for creating the required shared service; must return a non-null and
     *                thread-safe object
     * @param <T>     the type of the shared service created by {@code factory}
     *
     * @return the shared service created by {@code factory} or by its plugin replacement if one was
     * {@linkplain #set(Supplier, Supplier) specified}
     *
     * @throws IllegalArgumentException if {@code factory} is null
     * @throws IllegalStateException    if a circular service dependency is detected
     */
    public <T> T get(final Supplier<T> factory) {
        return get(factory, () -> { throw new IllegalStateException("circular service dependency ["+factory+"]"); });
    }

    /**
     * Retrieves the shared service created by a factory.
     *
     * <p>The new service is cached so that further calls for the same factory are idempotent.</p>
     *
     * <p>During object construction, nested shared service dependencies may be retrieved from this locator through
     * the static {@linkplain  #service(Supplier) service locator} method of the locator class. The locator used by the
     * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
     * constructors executed on a different thread.</p>
     *
     * @param factory  the factory responsible for creating the required shared service; must return a non-null and
     *                 thread-safe object
     * @param delegate the factory responsible for creating a fallback delegate service if a circular service dependency
     *                 is detected
     * @param <T>      the type of the shared service created by {@code factory} and {@code delegate}
     *
     * @return the shared service created by {@code factory} or by its plugin replacement if one was
     * {@linkplain #set(Supplier, Supplier) specified}
     *
     * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
     */
    public <T> T get(final Supplier<T> factory, final Supplier<? extends T> delegate) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        if ( delegate == null ) {
            throw new NullPointerException("null delegate");
        }

        synchronized ( services ) {

            final T cached=(T)services.get(factory);

            if ( pending.equals(cached) ) { return delegate.get(); } else {

                return cached != null ? cached : locator(() -> {
                    try {

                        services.put(factory, pending); // mark factory as being acquired

                        final T acquired=((Supplier<T>)factories.getOrDefault(factory, factory)).get();

                        services.put(factory, acquired); // cache actual resource

                        return acquired;

                    } catch ( final Throwable e ) {

                        services.remove(factory); // roll back acquisition marker

                        throw e;

                    }
                });
            }

        }
    }

    /**
     * Replaces an service factory with a plugin.
     *
     * <p>Subsequent calls to {@link #get(Supplier)} using {@code factory} as key will return the shared service
     * created by {@code plugin}.</p>
     *
     * @param <T>     the type of the shared service created by {@code factory}
     * @param factory the factory to be replaced
     * @param plugin  the replacing factory; must return a non-null and thread-safe object
     *
     * @return this locator
     *
     * @throws IllegalArgumentException if either {@code factory} or {@code plugin} is null
     * @throws IllegalStateException    if {@code factory} service was already retrieved
     */
    public <T> Locator set(final Supplier<T> factory, final Supplier<T> plugin) throws IllegalStateException {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        if ( plugin == null ) {
            throw new NullPointerException("null plugin");
        }

        synchronized ( services ) {

            if ( services.containsKey(factory) ) {
                throw new IllegalStateException("factory already in use");
            }

            factories.put(factory, plugin);

            return this;

        }
    }


    /**
     * Clears this locator.
     *
     * <p>All {@linkplain #get(Supplier) cached} service are purged. {@linkplain AutoCloseable Auto-closeable}
     * service are closed in inverse creation order before purging.</p>
     *
     * @return this locator
     */
    public Locator clear() {
        synchronized ( services ) {
            try {

                final Logger logger=get(Logger.logger()); // !!! make sure logger is not released before other services

                for (final Map.Entry<Supplier<?>, Object> entry : services.entrySet()) {

                    final Supplier<Object> factory=(Supplier<Object>)entry.getKey();
                    final Object service=entry.getValue();

                    try {

                        if ( service instanceof AutoCloseable ) {
                            ((AutoCloseable)service).close();
                        }

                    } catch ( final Exception t ) {

                        logger.error(this,
                                format("error during service deletion [%s/%s]", factory, service), t);

                    }
                }

                return this;

            } finally {

                factories.clear();
                services.clear();

            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Executes a set of tasks using shared services managed by this locator.
     *
     * <p>During task execution, shared service may be retrieved from this locator through the static {@linkplain
     * #service(Supplier) service locator} method of the locator class. The locator used by the service locator method is
     * managed through a {@link ThreadLocal} variable, so it won't be available to methods executed on a different
     * thread.</p>
     *
     * @param tasks the tasks to be executed
     *
     * @return this locator
     *
     * @throws NullPointerException if {@code task} is null or contains null items
     */
    public Locator exec(final Runnable... tasks) {

        if ( tasks == null ) {
            throw new NullPointerException("null tasks");
        }

        return locator(() -> {

            for (final Runnable task : tasks) {

                if ( task == null ) {
                    throw new NullPointerException("null task");
                }

                task.run();
            }

            return this;

        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private <V> V locator(final Supplier<V> task) {

        final Locator current=scope.get();

        try {

            scope.set(this);

            return task.get();

        } finally {

            scope.set(current);

        }

    }

}