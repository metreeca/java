/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.birt;

import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.vocabulary.*;

import static com.metreeca.json.Shape.exactly;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rdf4j.assets.Graph.query;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Browser.browser;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.wrappers.Driver.driver;


public final class ProductLines extends Delegator {

	public ProductLines() {
		delegate(driver(or(relate(), role(BIRT.staff)).then(

				filter(clazz(BIRT.ProductLine)),

				field(RDF.TYPE, exactly(BIRT.ProductLine)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(750)),

				field(iri("http://schema.org/image"), optional()),

				digest(
						field(BIRT.size, required(), datatype(XSD.INTEGER))
				)

		)).wrap(router()

				.path("/", router()
						.get(browser()
								.with(postprocessor(jsonld(), query(""
										+"prefix : <terms#>\n"
										+"\n"
										+"construct { ?line :size ?size } where {\n"
										+"\n"
										+"\t{ select ?line (count(distinct ?product) as ?size) {\n"
										+"\t\n"
										+"\t\t?line a :ProductLine; :product ?product;\n"
										+"\t\t\n"
										+"\t} group by ?line }\n"
										+"\t\n"
										+"}"
								)))
						))

				.path("/{}", router()
						.get(relator())
				)

		));
	}

}
