/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.assets;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * Resource loader.
 *
 * <p>Loads shared resources from a system-specific source.</p>
 */
@FunctionalInterface public interface Loader {

	/**
	 * Retrieves the default loader factory.
	 *
	 * @return the default loader factory, which retrieves system resources from the classpath through {@link
	 * ClassLoader#getResourceAsStream(String)}
	 */
	public static Supplier<Loader> loader() {
		return () -> path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return Optional.ofNullable(Loader.class.getClassLoader().getResourceAsStream(path));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Loads a shared resource.
	 *
	 * @param path the path the system resource should be loaded from; path syntax is source dependent, but a
	 *             filesystem-like slash-separated hierarchical structure is recommended
	 *
	 * @return an optional input stream for reading the required resource, if one is available at {@code path};  an
	 * empty optional, otherwise
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} syntax is illegal according to source-specific rules
	 */
	public Optional<InputStream> load(final String path);

}