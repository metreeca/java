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

package com.metreeca.rdf4j.schemas;

import com.metreeca.json.Frame;
import com.metreeca.text.Chunk;
import com.metreeca.text.Match;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.Collection;
import java.util.Set;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;

/**
 * RDF vocabulary for describing textual annotations.
 */
public final class Text {

    /**
     * Default entity labels.
     */
    public static final Collection<IRI> Labels=Set.of(
            RDFS.LABEL,
            SKOS.PREF_LABEL, SKOS.ALT_LABEL, SKOS.HIDDEN_LABEL
    );

    /**
	 * Default languages.
	 */
	public static final Set<String> Languages=Set.of(
			"", "en"
	);


	public static final String Base="app://text.metreeca.com/";
	public static final String Name=Base+"terms#";

	public static final IRI Document=iri(Name, "Document");
	public static final IRI Entity=iri(Name, "Entity");

	public static final IRI Annotation=iri(Name, "Annotation");
	public static final IRI Reference=iri(Name, "Reference");
	public static final IRI Relation=iri(Name, "Relation");

	public static final IRI target=iri(Name, "target");
	public static final IRI source=iri(Name, "source");
	public static final IRI detail=iri(Name, "detail");
	public static final IRI anchor=iri(Name, "anchor");
	public static final IRI offset=iri(Name, "offset");
	public static final IRI length=iri(Name, "length");
	public static final IRI weight=iri(Name, "weight");


	public static Frame reference(
			final IRI iri, final IRI document, final Value detail, final Match<Chunk, Frame> match
	) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( document == null ) {
			throw new NullPointerException("null document");
		}

		if ( detail == null ) {
			throw new NullPointerException("null detail");
		}

		if ( match == null ) {
			throw new NullPointerException("null match");
		}

		final Chunk _source=match.source();
		final Frame _target=match.target();

		return frame(iri)

                .value(RDF.TYPE, Reference)

                .value(source, document)
                .frame(target, _target)

                .value(Text.detail, detail)

				.value(anchor, literal(_source.text()))
				.value(offset, literal(_source.lower()))
				.value(length, literal(_source.length()))

				.value(weight, literal(match.weight()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Text() { }

}
