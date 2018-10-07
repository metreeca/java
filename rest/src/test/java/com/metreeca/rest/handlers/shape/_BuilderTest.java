/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;


final class _BuilderTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(small()))
				.exec(task)
				.clear();
	}


	private String virtual() {
		return sparql(""
				+"construct {\n"
				+"\n"
				+"\t$this a :Employee ;\n"
				+"\t\trdfs:label 'Tino Faussone' ;\n"
				+"\t\t:code '1234' ;\n"
				+"\t\t:surname 'Faussone' ;\n"
				+"\t\t:forename 'Tino' ;\n"
				+"\t\t:email 'tfaussone@classicmodelcars.com' ;\n"
				+"\t\t:title 'Sales Rep' ;\n"
				+"\t\t:supervisor <employees/1102> ;\n"
				+"\t\t:seniority '1'^^xsd:integer .\n"
				+"\n"
				+"} where {}"
		);
	}


	private Request direct() {
		return new Request()
				.method(Request.GET)
				.base(Base)
				.path("/virtual");
	}

	private Request driven() {
		return direct()
				.roles(Manager)
				.body(shape()).set(Employee);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testBuild() {
		exec(() -> new _Builder().query(virtual())

				.handle(driven())

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBodyThat(rdf())
						.isIsomorphicTo(decode("<virtual> a :Employee ;\n"
								+"\trdfs:label 'Tino Faussone' ;\n"
								+"\t:code '1234' ;\n"
								+"\t:surname 'Faussone' ;\n"
								+"\t:forename 'Tino' ;\n"
								+"\t:email 'tfaussone@classicmodelcars.com' ;\n"
								+"\t:title 'Sales Rep' ;\n"
								+"\t:supervisor <employees/1102> ;\n"
								+"\t:seniority '1'^^xsd:integer .\n"))
				)

		);
	}

	@Test void testBuildLimited() {
		exec(() -> new _Builder().query(virtual())

				.handle(driven().roles(Salesman))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBodyThat(rdf())
						.isIsomorphicTo(decode("<virtual> a :Employee ;\n"
								+"\trdfs:label 'Tino Faussone' ;\n"
								+"\t:code '1234' ;\n"
								+"\t:surname 'Faussone' ;\n"
								+"\t:forename 'Tino' ;\n"
								+"\t:email 'tfaussone@classicmodelcars.com' ;\n"
								+"\t:title 'Sales Rep' .\n"))
				)

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenUnautorized() {
		exec(() -> new _Builder()

				.handle(driven().roles(Form.none))

				.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

		);
	}

	@Test void testDrivenForbidden() {
		exec(() -> new _Builder()

				.handle(driven().body(shape()).set(or()))

				.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

		);
	}

	@Test void testDrivenUnknownOnEmptyModel() {
		exec(() -> new _Builder()

				.handle(driven())

				.accept(response -> assertThat(response).hasStatus(Response.NotFound))

		);
	}

}
