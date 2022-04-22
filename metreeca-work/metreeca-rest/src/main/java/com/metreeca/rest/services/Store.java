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

package com.metreeca.rest.services;

import com.metreeca.http.Toolbox;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.function.Supplier;

import static com.metreeca.http.Toolbox.service;
import static com.metreeca.http.Toolbox.storage;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;


/**
 * Blob store.
 *
 * <p>Provides access to a dedicated system store for binary data blobs.</p>
 */
public interface Store {

	/**
	 * Retrieves the default store factory.
	 *
	 * @return the default store factory, which creates {@link FileStore} instances
	 */
	public static Supplier<Store> store() {
		return FileStore::new;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks a data blob.
	 *
	 * @param id the identifier of the data blob to be checked
	 *
	 * @return {@code true} if a data blob identified by {@code id} is present; {@code false}, otherwise
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid data blob identifier for the backing blob store
	 * @throws IOException              if an I/O exception occurred while operating on the store
	 */
	public boolean has(final String id) throws IOException;

	/**
	 * Reads a data blob.
	 *
	 * @param id the identifier of the data blob to be read
	 *
	 * @return an input stream for reading from the data blob identified by {@code id}
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid data blob identifier for the backing blob store
	 * @throws NoSuchFileException      if {@code id} is not the identifier of an existing data blob
	 * @throws IOException              if an I/O exception occurred while operating on the store
	 */
	public InputStream read(final String id) throws IOException;

	/**
	 * Writes a data blob.
	 *
	 * <p>Write operations must be atomic, that is:</p>
	 *
	 * <ul>
	 *
	 *  <li>mltiple write operatinos on the same blob must be serialized;</li>
	 *
	 *  <li>existing data must be made available for {@linkplain #read(String) reading} until the operation is
	 *  completed by closing the output stream.</li>
	 *
	 * </ul>
	 *
	 * @param id the identifier of the data blob to be written
	 *
	 * @return an output stream for writing to the data blob identified by {@code id}
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid data blob identifier for the backing blob store
	 * @throws IOException              if an I/O exception occurred while operating on the store
	 */
	public OutputStream write(final String id) throws IOException;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Storage blob store.
	 *
	 * <p>Stores data blobs in the {@code store} folder of the system file {@linkplain Toolbox#storage storage}.</p>
	 */
	public static class FileStore implements Store {

		private final Path path=service(storage()).resolve("store");

		@Override public boolean has(final String id) throws IOException {
			return Files.exists(path.resolve(id));
		}

		@Override public InputStream read(final String id) throws IOException {

			if ( id == null ) {
				throw new NullPointerException("null id");
			}

			return Files.newInputStream(path.resolve(id), READ);
		}

		@Override public OutputStream write(final String id) throws IOException {

			if ( id == null ) {
				throw new NullPointerException("null id");
			}

			final Path blob=path.resolve(UUID.randomUUID().toString());

			return new FilterOutputStream(Files.newOutputStream(blob, CREATE_NEW, WRITE)) {

				@Override public void close() throws IOException {
					try {

						super.close();

						Files.move(blob, path.resolve(id), REPLACE_EXISTING);

					} catch ( final Throwable e ) {

						Files.deleteIfExists(blob);

						throw e;

					}
				}

			};

		}

	}

}
