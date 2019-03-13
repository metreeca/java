/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gate.rosters;

import com.metreeca.gate.Roster;


public final class KeyRoster implements Roster {  // !!! hardwired super-user

	@Override public Roster.Permit profile(final String alias) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Permit refresh(final String alias) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Permit acquire(final String alias, final String secret) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Permit acquire(final String alias, final String secret, final String update) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Permit release(final String alias) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
