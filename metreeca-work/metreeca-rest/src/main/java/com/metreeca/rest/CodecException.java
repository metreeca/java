/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.rest;

/**
 * Codec exception.
 *
 * <p>Thrown to report message encoding/decoding issues.</p>
 */
public final class CodecException extends RuntimeException {

	private static final long serialVersionUID=6385340424276867964L;

	public CodecException(final String message) {
		super(message);
	}

}
