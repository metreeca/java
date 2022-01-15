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

package com.metreeca.rdf.formats;

public final class RDFFormatTestService {

	private final org.eclipse.rdf4j.rio.RDFFormat format;


	RDFFormatTestService(final org.eclipse.rdf4j.rio.RDFFormat format) {
		this.format=format;
	}


	org.eclipse.rdf4j.rio.RDFFormat getFormat() {
		return format;
	}


	@Override public String toString() {
		return format.getDefaultMIMEType();
	}

}