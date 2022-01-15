

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

package com.metreeca.rdf.schemes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.multiple;
import static com.metreeca.json.Shape.optional;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Or.or;

/**
 * Schema.org RDF vocabulary.
 *
 * @see <a href="https://schema.org/">Schema.org</a>
 */
public final class Schema {

	public static final String Namespace="https://schema.org/";
	public static final String NamespaceLegacy="http://schema.org/";


	/**
	 * Creates a term in the schema.org namespace.
	 *
	 * @param id the identifer of the term to be created
	 *
	 * @return the schema.org term identified by {@code id}
	 *
	 * @throws NullPointerException if {@code id} is null
	 */
	public static IRI term(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return iri(Namespace, id);
	}


	//// Things ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Thing=term("Thing");

	public static final IRI url=term("url");
	public static final IRI name=term("name");
	public static final IRI logo=term("logo"); // ;( formally not a Thing property
	public static final IRI image=term("image");
	public static final IRI description=term("description");
	public static final IRI disambiguatingDescription=term("disambiguatingDescription");


	/**
	 * Creates a thing shape.
	 *
	 * @param labels additional constraints for textual labels (e.g. localized names)
	 *
	 * @return a thing shape including {@code labels} constraints for textual labels
	 *
	 * @throws NullPointerException if {@code labels} is nul or contains null elements
	 */
	public static Shape Thing(final Shape... labels) {

		if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}

		return and(

				field(url, optional(), datatype(IRIType)),
				field(name, labels),
				field(logo, multiple(), datatype(IRIType)),
				field(image, multiple(), datatype(IRIType)),
				field(description, labels),
				field(disambiguatingDescription, labels)

		);
	}


	//// Events ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public enum EventStatus {EventScheduled, EventMovedOnline, EventPostponed, EventRescheduled, EventCancelled}

	public static final IRI Event=term("Event");

	public static final IRI organizer=term("organizer");
	public static final IRI isAccessibleForFree=term("isAccessibleForFree");
	public static final IRI eventStatus=term("eventStatus");
	public static final IRI location=term("location");
	public static final IRI eventAttendanceMode=term("eventAttendanceMode");
	public static final IRI inLanguage=term("inLanguage");
	public static final IRI audience=term("audience");
	public static final IRI typicalAgeRange=term("typicalAgeRange");
	public static final IRI startDate=term("startDate");
	public static final IRI endDate=term("endDate");


	/**
	 * Creates an event shape.
	 *
	 * @param labels additional constraints for textual labels (e.g. localized names)
	 *
	 * @return an event shape including {@code labels} constraints for textual labels
	 *
	 * @throws NullPointerException if {@code labels} is nul or contains null elements
	 */
	public static Shape Event(final Shape... labels) {

		if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}

		return and(Thing(labels),

				field(organizer, optional(),
						field(RDFS.LABEL, labels)
				),

				field(isAccessibleForFree, optional(), datatype(XSD.BOOLEAN)),
				field(eventStatus, optional(), datatype(IRIType)),

				field(location, optional(), or(

						Place(labels),
						PostalAddress(labels),
						VirtualLocation(labels)

				)),

				field(eventAttendanceMode, multiple(), datatype(IRIType)),

				field(audience, multiple(),
						field(RDFS.LABEL, labels)
				),

				field(inLanguage, multiple(), datatype(XSD.STRING)),
				field(typicalAgeRange, multiple(), datatype(XSD.STRING)),

				field(startDate, optional(), datatype(XSD.DATETIME)),
				field(endDate, optional(), datatype(XSD.DATETIME)));
	}


	//// Places ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Place=term("Place");
	public static final IRI PostalAddress=term("PostalAddress");
	public static final IRI VirtualLocation=term("VirtualLocation");


	public static IRI address=term("address");

	public static IRI latitude=term("latitude");
	public static IRI longitude=term("longitude");

	public static IRI addressCountry=term("addressCountry");
	public static IRI addressRegion=term("addressRegion");
	public static IRI addressLocality=term("addressLocality");

	public static IRI postalCode=term("vpostalCode");
	public static IRI streetAddress=term("streetAddress");


	/**
	 * Creates a place shape.
	 *
	 * @param labels additional constraints for textual labels (e.g. localized names)
	 *
	 * @return a place shape including {@code labels} constraints for textual labels
	 *
	 * @throws NullPointerException if {@code labels} is nul or contains null elements
	 */
	public static Shape Place(final Shape... labels) {

		if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}
		return and(Thing(labels),

				field(address, optional(), PostalAddress(labels)),

				field(latitude, optional(), datatype(XSD.DECIMAL)),
				field(longitude, optional(), datatype(XSD.DECIMAL))

		);
	}

	/**
	 * Creates a postal address shape.
	 *
	 * @param labels additional constraints for textual labels (e.g. localized names)
	 *
	 * @return a postal address shape including {@code labels} constraints for textual labels
	 *
	 * @throws NullPointerException if {@code labels} is nul or contains null elements
	 */
	public static Shape PostalAddress(final Shape... labels) {

		if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}

		return and(Thing(labels),

				field(addressCountry, datatype(XSD.DECIMAL)),
				field(addressRegion, datatype(XSD.STRING)),
				field(addressLocality, datatype(XSD.STRING)),
				field(postalCode, datatype(XSD.STRING)),
				field(streetAddress, datatype(XSD.STRING))

		);
	}

	/**
	 * Creates a virtual location shape.
	 *
	 * @param labels additional constraints for textual labels (e.g. localized names)
	 *
	 * @return a virtual location shape including {@code labels} constraints for textual labels
	 *
	 * @throws NullPointerException if {@code labels} is nul or contains null elements
	 */
	public static Shape VirtualLocation(final Shape... labels) {

		if ( labels == null || Arrays.stream(labels).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null labels");
		}

		return and(Thing(labels));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Upgrades legacy {@value #NamespaceLegacy} references.
	 *
	 * @param model the RDF model to be normalized
	 *
	 * @return a normalized {@code model} where legacy {@value #NamespaceLegacy} references are upgraded to  {@value
	 * #Namespace}
	 *
	 * @throws NullPointerException if {@code model} is null or contains null elements
	 */
	public static Stream<Statement> normalize(final Stream<Statement> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return model.map(Schema::normalize);
	}

	/**
	 * Upgrades legacy {@value #NamespaceLegacy} references.
	 *
	 * @param statement the RDF statement to be normalized
	 *
	 * @return a normalized {@code statement} where legacy {@value #NamespaceLegacy} references are upgraded to  {@value
	 * #Namespace}
	 *
	 * @throws NullPointerException if {@code statement} is null
	 */
	public static Statement normalize(final Statement statement) {

		if ( statement == null ) {
			throw new NullPointerException("null statement");
		}

		return statement(
				normalize(statement.getSubject()),
				normalize(statement.getPredicate()),
				normalize(statement.getObject()),
				statement.getContext()
		);
	}

	/**
	 * Upgrades legacy {@value #NamespaceLegacy} references.
	 *
	 * @param value the RDF value to be normalized
	 *
	 * @return a normalized {@code value} where legacy {@value #NamespaceLegacy} references are upgraded to  {@value
	 * #Namespace}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Value normalize(final Value value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return value.isIRI() ? normalize((IRI)value) : value;
	}

	/**
	 * Upgrades legacy {@value #NamespaceLegacy} references.
	 *
	 * @param resource the RDF resource to be normalized
	 *
	 * @return a normalized {@code resource} where legacy {@value #NamespaceLegacy} references are upgraded to  {@value
	 * #Namespace}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Resource normalize(final Resource resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return resource.isIRI() ? normalize((IRI)resource) : resource;
	}

	/**
	 * Upgrades legacy {@value #NamespaceLegacy} references.
	 *
	 * @param iri the RDF resource to be normalized
	 *
	 * @return a normalized {@code iri} where legacy {@value #NamespaceLegacy} references are upgraded to  {@value
	 * #Namespace}
	 *
	 * @throws NullPointerException if {@code iri} is null
	 */
	public static IRI normalize(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return iri.getNamespace().equals(NamespaceLegacy) ? iri(Namespace, iri.getLocalName()) : iri;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Schema() { }

}
