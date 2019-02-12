/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.rest.Engine;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.rest.engines.Descriptions.description;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


abstract class GraphEntity implements Engine {

	Shape redact(final Shape shape, final IRI task, final IRI view) {
		return shape.map(new Redactor(map(
				entry(Form.task, set(task)),
				entry(Form.view, set(view)),
				entry(Form.role, set(Form.any))
		))).map(new Optimizer());
	}


	Optional<Resource> reserve(final RepositoryConnection connection, final Resource resource) {
		return Optional.ofNullable(connection.hasStatement(resource, null, null, true)
				|| connection.hasStatement(null, null, resource, true) ? null : resource);
	}


	Focus validate(final Resource resource, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(resource, false, model);

		return focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside description envelope "+outlier))
				.collect(toList())
		);
	}

	Focus validate(final RepositoryConnection connection, final IRI resource, final Shape shape, final Collection<Statement> model) {

		// !!! make sure the validator use updated resource state in 'model' rather than current state in the store

		// validate against shape (disable if not transactional) // !!! just downgrade

		final boolean unsafe=!connection.getIsolationLevel().isCompatibleWith(IsolationLevels.SNAPSHOT);

		final Focus focus=new ShapedValidator().validate(connection, resource, unsafe ? and() : shape);

		// validate shape envelope

		final Collection<Statement> envelope=focus.outline().collect(toSet());

		final Collection<Statement> outliers=unsafe ? emptySet() : model.stream()
				.filter(statement -> !envelope.contains(statement))
				.collect(toList());

		// extend validation report with statements outside shape envelope

		return outliers.isEmpty() ? focus : focus(concat(focus.getIssues(), outliers.stream()
				.map(outlier -> issue(Issue.Level.Error, "statement outside shape envelope "+outlier))
				.collect(toList())
		), focus.getFrames());

	}

}
