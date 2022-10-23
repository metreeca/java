

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

package com.metreeca.gcp.services;


import com.metreeca.core.services.Logger;
import com.metreeca.core.services.Store;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.sail.*;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.metreeca.core.Locator.service;
import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.core.services.Logger.time;
import static com.metreeca.core.services.Store.store;

/**
 * Google Cloud RDF4J storage SAIL.
 *
 * <p>Manages an RDF4J MemoryStore backed by a persistent blob-based storage.</p>
 *
 * @see GCPStore
 * @see <a href="https://cloud.google.com/storage/docs">Google Cloud Plaform - Storage</a>
 */
public final class GCPMemoryStore implements Sail {

    private final String storage;
    private final MemoryStore delegate;

    private RDFFormat format=RDFFormat.BINARY;

    private final Store store=service(store());
    private final Logger logger=service(logger());


    /**
     * Creates a Google Cloud repository.
     *
     * @param storage the id of the backing Goggle Cloud Storage blob
     *
     * @throws NullPointerException if {@code storage} is null
     * @see GCPMemoryStore
     */
    public GCPMemoryStore(final String storage) {

        final MemoryStore memory=new MemoryStore();

        memory.addSailChangedListener(event -> {
            if ( event.statementsAdded() || event.statementsRemoved() ) {
                try ( final SailConnection connection=event.getSail().getConnection() ) {
                    dump(connection);
                }
            }
        });

        this.storage=storage;
        this.delegate=memory;
    }


    /**
     * Configures the RDF format for the backing storage.
     *
     * @param format the RDF format for archiving repository content to the backing storage
     *
     * @return this repository
     *
     * @throws NullPointerException if {@code format} is null
     */
    public GCPMemoryStore format(final RDFFormat format) {

        if ( format == null ) {
            throw new NullPointerException("null format");
        }

        this.format=format;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public void init() throws RepositoryException {

        delegate.init();

        load();

    }

    @Override public void shutDown() throws RepositoryException {
        delegate.shutDown();
    }


    @Override public boolean isWritable() throws RepositoryException {
        return delegate.isWritable();
    }


    @Override public List<IsolationLevel> getSupportedIsolationLevels() {
        return delegate.getSupportedIsolationLevels();
    }

    @Override public IsolationLevel getDefaultIsolationLevel() {
        return delegate.getDefaultIsolationLevel();
    }


    @Override public File getDataDir() {
        return delegate.getDataDir();
    }

    @Override public void setDataDir(final File dataDir) {
        delegate.setDataDir(dataDir);
    }


    @Override public ValueFactory getValueFactory() {
        return delegate.getValueFactory();
    }

    @Override public SailConnection getConnection() throws RepositoryException {
        return delegate.getConnection();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private String blob() {
        return String.format("%s.%s.gz",
                storage, Optional.ofNullable(format.getDefaultFileExtension()).orElse("rdf")
        );
    }

    private void load() {
        try {

            final String blob=blob();

            if ( store.has(blob) ) {
                try (
                        final SailConnection connection=delegate.getConnection();
                        final InputStream input=new GZIPInputStream(store.read(blob))
                ) {

                    time(() -> {

                        try {

                            final RDFParser parser=Rio.createParser(format, delegate.getValueFactory());

                            parser.setRDFHandler(new LoadHandler(connection));

                            connection.begin();

                            parser.parse(input);

                            connection.commit();

                        } catch ( final IOException e ) {

                            connection.rollback();

                            throw new UncheckedIOException(e);

                        }

                    }).apply(t -> logger.info(GCPMemoryStore.class, String.format(

                            "loaded <%,d> statements in <%,d> ms", connection.size(), t

                    )));


                }
            }

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    private void dump(final SailConnection connection) {
        try (
                final OutputStream output=new GZIPOutputStream(store.write(blob()));
                final CloseableIteration<? extends Statement, SailException> statements=
                        connection.getStatements(null, null, null, true)
        ) {

            time(() ->

                    Rio.write(() -> statements.stream().map(Statement.class::cast).iterator(), output, format)

            ).apply(t -> logger.info(GCPMemoryStore.class, String.format(

                    "dumped <%,d> statements in <%,d> ms", connection.size(), t

            )));

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class LoadHandler extends AbstractRDFHandler {

        private final SailConnection connection;

        private LoadHandler(final SailConnection connection) { this.connection=connection; }

        @Override public void handleStatement(final Statement st) throws RDFHandlerException {
            connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
        }

    }
}
