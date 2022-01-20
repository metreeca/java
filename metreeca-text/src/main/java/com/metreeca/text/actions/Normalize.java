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

package com.metreeca.text.actions;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.function.UnaryOperator;

/**
 * Text cleaning.
 *
 * <p>Cleans and normalizes text values.</p>
 */
public final class Normalize implements UnaryOperator<String> {

	private boolean space;
	private boolean marks;
	private boolean smart;
	private boolean lower;


	/**
	 * Configures space normalization (defaults to {@code false}).
	 *
	 * @param space if {@code true}, leading and trailing sequences of control and separator characters are removed and
	 *              other sequences replaced with a single space character
	 *
	 * @return this action
	 */
	public Normalize space(final boolean space) {

		this.space=space;

		return this;
	}

	/**
	 * Configures marks normalization (defaults to {@code false}).
	 *
	 * @param marks if {@code true}, mark characters are removed
	 *
	 * @return this action
	 */
	public Normalize marks(final boolean marks) {

		this.marks=marks;

		return this;
	}

	/**
	 * Configures typographical cleaning (defaults to {@code false}).
	 *
	 * @param smart if {@code true}, typographical characters (curly quotes, guillemets, …) are replaced with equivalent
	 *              plain ASCII characters
	 *
	 * @return this action
	 */
	public Normalize smart(final boolean smart) {

		this.smart=smart;

		return this;
	}

	/**
	 * Configures lowercase normalization (defaults to {@code false}).
	 *
	 * @param lower if {@code true}, all characters are converted to lower case
	 *
	 * @return this action
	 */
	public Normalize lower(final boolean lower) {

		this.lower=lower;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Clean text values.
	 *
	 * @param text the text value to be cleaned; unmodified if null or empty
	 *
	 * @return the clean version of {@code text}
	 */
	@Override public String apply(final String text) {
		if ( text == null || text.isEmpty() ) { return text; } else {

			final char[] chars=Normalizer.normalize(text, marks ? Form.NFD : Form.NFC).toCharArray();

			int r=0;
			int w=0;

			boolean s=false;

			while ( r < chars.length ) {

				final char n=chars[r++];
				final char c=lower ? Character.toLowerCase(n) : n;

				switch ( Character.getType(c) ) {

					case Character.CONTROL:
					case Character.SPACE_SEPARATOR:
					case Character.LINE_SEPARATOR:
					case Character.PARAGRAPH_SEPARATOR:

						if ( space ) {
							s=(w > 0);
						} else {
							chars[w++]=smart ? plain(c) : c;
						}

						break;

					case Character.ENCLOSING_MARK:
					case Character.NON_SPACING_MARK:
					case Character.COMBINING_SPACING_MARK:

						if ( !marks ) { chars[w++]=smart ? plain(c) : c; }

						break;

					default:

						if ( s ) {
							chars[w++]=' ';
							s=false;
						}

						chars[w++]=smart ? plain(c) : c;

						break;

				}
			}

			return new String(chars, 0, w);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private char plain(final char c) {
		switch ( c ) {

			case '\u00A0': // no-break space
			case '\u2002': // en space
			case '\u2003': // em space

				return ' ';

			case '\u2012': // figure dash
			case '\u2013': // en dash
			case '\u2014': // em dash

				return '-';

			case '\u2018': // single smart opening quote
			case '\u2019': // single smart closing quote

				return '\'';

			case '\u201C': // double smart opening quote
			case '\u201D': // double smart closing quote

				return '"';

			case '\u2039': // single opening guillemet
			case '\u00AB': // double opening guillemet

				return '<';

			case '\u203A': // single closing guillemet
			case '\u00BB': // double closing guillemet

				return '>';

			default:

				return c;

		}
	}

}
