/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.codecs;

import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static com.metreeca.form.shapes.Alias.alias;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Default.dflt;
import static com.metreeca.form.shapes.Group.group;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Notes.notes;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.Placeholder.placeholder;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Values.literal;

import static org.junit.Assert.assertEquals;


public final class ShapeCodecTest {

	@Test public void testAnnotations() {

		assertCoded("alias", alias("alias"));
		assertCoded("label", Label.label("label"));
		assertCoded("notes", notes("notes"));
		assertCoded("placeholder", placeholder("placeholder"));
		assertCoded("default", dflt(Values.literal("default")));
		assertCoded("default", Hint.hint(RDF.NIL));
		assertCoded("group", group(MinCount.minCount(10)));

	}

	@Test public void testGlobals() {

		assertCoded("minCount", MinCount.minCount(10));
		assertCoded("maxCount", maxCount(10));
		assertCoded("universal", All.all(RDF.VALUE));
		assertCoded("existential", any(RDF.VALUE));
		assertCoded("range", In.in(RDF.VALUE));

	}

	@Test public void testLocals() {

		assertCoded("type", Datatype.datatype(RDF.NIL));
		assertCoded("class", Clazz.clazz(RDF.NIL));
		assertCoded("minExclusive", MinExclusive.minExclusive(Values.literal(10)));
		assertCoded("maxExclusive", maxExclusive(Values.literal(10)));
		assertCoded("minInclusive", MinInclusive.minInclusive(Values.literal(10)));
		assertCoded("maxInclusive", maxInclusive(Values.literal(10)));
		assertCoded("pattern", pattern("expression"));
		assertCoded("pattern with flags", pattern("expression", "flags"));
		assertCoded("like", like("keywords"));
		assertCoded("minLength", MinLength.minLength(10));
		assertCoded("maxLength", MaxLength.maxLength(10));

	}

	@Test public void testStructurals() {

		assertCoded("direct trait", trait(RDF.VALUE));
		assertCoded("inverse trait", trait(Step.step(RDF.VALUE, true)));
		assertCoded("shaped trait", trait(RDF.VALUE, MinCount.minCount(10)));

		assertCoded("virtual trait", virtual(trait(RDF.VALUE, MinCount.minCount(10)), Step.step(RDF.NIL)));

	}

	@Test public void testLogicals() {

		assertCoded("empty conjunction", and());
		assertCoded("singleton conjunction", and(MinCount.minCount(1)));
		assertCoded("proper conjunction", and(MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("empty disjunction", or());
		assertCoded("singleton disjunction", or(MinCount.minCount(1)));
		assertCoded("proper disjunction", or(MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("one-way option", com.metreeca.form.shapes.Test.test(and(), MinCount.minCount(1)));
		assertCoded("two-way option", com.metreeca.form.shapes.Test.test(and(), MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("singleton condition", When.when(RDF.VALUE, Values.literal(1)));
		assertCoded("proper condition", When.when(RDF.VALUE, Values.literal(1), Values.literal(10)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertCoded(final String message, final Shape shape) {

		final ShapeCodec codec=new ShapeCodec();
		final Collection<Statement> model=new ArrayList<>();

		try {

			assertEquals(message, shape, codec.decode(codec.encode(shape, model), model));

		} finally {
			Rio.write(model, System.out, RDFFormat.TURTLE);
		}
	}

}