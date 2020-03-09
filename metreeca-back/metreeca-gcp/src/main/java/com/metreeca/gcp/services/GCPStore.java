/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.rest.services.Store;

import com.google.cloud.storage.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.NoSuchFileException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Google Cloud blob store.
 *
 * <p>Retrieves blobs managed by the Google Cloud Storage service.</p>
 *
 * <p>For both {@linkplain #read(String) read} and {@linkplain #write(String) write} operations, blob identifiers not
 * matching a full GCS URI (i.e. {@code gs://{bucket}/{object}}) or link URL (i.e. {@code
 * https://storage.cloud.google.com/{bucket}/{object}) are interpreted as object names in the default project bucket
 * (i.e. {@code {project}.appspot.com}}.</p>
 *
 * @see <a href="https://cloud.google.com/storage/docs">Google Cloud Plaform - Storage</a>
 */
public final class GCPStore implements Store {

	private static final Pattern IdPattern=Pattern
			.compile("(?:https://storage\\.cloud\\.google\\.com/|gs://)(?<bucket>[^/]+)/(?<object>[^/]+)");


	@FunctionalInterface private static interface Task<R> {

		public R exec(final String bucket, final String object) throws IOException;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Storage storage;


	public GCPStore() {
		this(StorageOptions.getDefaultInstance());
	}

	public GCPStore(final StorageOptions options) {

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.storage=options.getService();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public InputStream read(final String id) throws IOException {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::read);
	}

	@Override public OutputStream write(final String id) throws IOException {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return exec(id, this::write);
	}


	private <V> V exec(final String id, final Task<V> task) throws IOException {

		final Matcher matcher=IdPattern.matcher(id);

		return matcher.matches()
				? task.exec(matcher.group("bucket"), matcher.group("object"))
				: task.exec(GCP.Project+".appspot.com", id);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public InputStream read(final String bucket, final String object) throws IOException {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		try {

			final Blob blob=storage.get(BlobId.of(bucket, object));

			if ( blob == null ) {
				throw new NoSuchFileException(String.format("gs://%s/%s", bucket, object));
			}

			return Channels.newInputStream(blob.reader());

		} catch ( final StorageException e ) {
			throw new IOException(e);
		}
	}

	public OutputStream write(final String bucket, final String object) throws IOException {

		if ( bucket == null ) {
			throw new NullPointerException("null bucket");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		try {

			return Channels.newOutputStream(storage.create(BlobInfo.newBuilder(bucket, object).build()).writer());

		} catch ( final StorageException e ) {
			throw new IOException(e);
		}
	}

}
