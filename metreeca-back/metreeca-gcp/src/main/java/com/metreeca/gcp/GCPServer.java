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

package com.metreeca.gcp;

import com.metreeca.core.Locator;
import com.metreeca.gcp.services.GCPStore;
import com.metreeca.gcp.services.GCPVault;
import com.metreeca.http.Handler;
import com.metreeca.jse.JSEServer;

import com.google.cloud.ServiceOptions;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.Locator.path;
import static com.metreeca.core.services.Store.store;
import static com.metreeca.core.services.Vault.vault;
import static com.metreeca.http.Response.Forbidden;

import static java.lang.String.format;

/**
 * Google Cloud Platform server connector.
 *
 * <p>Delegates a {@link JSEServer} pre-configured with Google Cloud Platform {@linkplain com.metreeca.gcp.services
 * services}.</p>
 */
public final class GCPServer {

    private static final String ServiceVariable="GAE_SERVICE";
    private static final String VersionVariable="GAE_VERSION";
    private static final String AddressVariable="PORT";

    private static final String Default="default";


    /**
     * Checks if running in the development environment
     *
     * @return {@code true} if running in the development environment; {@code false}, otherwise
     */
    public static boolean development() {
		return Objects.isNull(System.getenv(AddressVariable));
    }

    /**
     * Checks if running in the production environment
     *
     * @return {@code true} if running in the production environment; {@code false}, otherwise
     */
    public static boolean production() {
        return !development();
    }


    /**
     * Retrieves the project name.
     *
     * @return the Google Cloud Platform project name or null if unknown
     */
    public static String project() {
        return ServiceOptions.getDefaultProjectId();
    }

    /**
     * Retrieves the service name.
     *
     * @return the Google App Engine service name
     */
    public static String service() {
        return System.getenv().getOrDefault(ServiceVariable, Default);
    }

    /**
     * Retrieves the service version.
     *
     * @return the Google App Engine service version
     */
    public static String version() {
        return System.getenv().getOrDefault(VersionVariable, Default);
    }


    /**
     * Restricts access to a cron handler.
     *
     * @param handler the cron handler
     *
     * @return an access control handler restricting {@code handler} to requests issued by the Google App Engine cron
     * service
     *
     * @throws NullPointerException if {@code handler} is null
     */
    public static Handler cron(final Handler handler) {

        if ( handler == null ) {
            throw new NullPointerException("null handler");
        }

        return (request, next) -> request.headers("X-Appengine-Cron").anyMatch("true"::equals)
                ? handler.handle(request, next)
                : request.reply(Forbidden);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final JSEServer delegate=new JSEServer()

            .context(Optional.of(service())
                    .filter(service -> !service.equals(Default))
                    .map(service -> format("/%s/", service))
                    .orElse("/")
            );


    public GCPServer context(final String context) {

        if ( context == null ) {
            throw new NullPointerException("null context");
        }

        delegate.context(context);

        return this;
    }

    public GCPServer delegate(final Function<Locator, Handler> factory) {

        if ( factory == null ) {
            throw new NullPointerException("null factory");
        }

        delegate.delegate(context -> factory.apply(context

                .set(path(), () -> Paths.get("/tmp"))

                .set(vault(), production() ? GCPVault::new : vault())
                .set(store(), production() ? GCPStore::new : store())

        ));

        return this;
    }


    public void start() {
        delegate.start();
    }

}
