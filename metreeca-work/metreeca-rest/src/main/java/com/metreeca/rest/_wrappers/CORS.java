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

package com.metreeca.rest._wrappers;


import com.metreeca.http.Request;
import com.metreeca.rest.Handler;
import com.metreeca.rest._Wrapper;

import static java.lang.String.join;


/**
 * CORS filter.
 *
 * <p>Manages CORS HTTP requests.</p>
 *
 * <p><strong>Warning</strong> Don't use in production / Provisional implementation with unsafe shortcuts.</p>
 *
 * @see <a href="https://fetch.spec.whatwg.org/#cors-protocol">Fetch - § 3.2 CORS protocol</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">Cross-Origin Resource Sharing (CORS) @ MDN</a>
 */
public final class CORS implements _Wrapper {

	// !!! https://www.html5rocks.com/static/images/cors_server_flowchart.png

	@Override public Handler wrap(final Handler handler) {
		return (request, next) -> handler.handle(request, next).map(response -> response

                .header("Access-Control-Allow-Origin", request.header("Origin").orElse("*"))
                .header("Access-Control-Allow-Credentials", "true")

                .header("Access-Control-Allow-Methods", join(", ",
                        Request.OPTIONS,
                        Request.GET,
                        Request.HEAD,
                        Request.POST,
                        Request.PUT,
						Request.DELETE
				))

				.header("Access-Control-Allow-Headers", join(", ",
						"Origin",
						"Accept",
						"Content-Type",
						"Authorization",
						"X-Requested-With",
						"Access-Control-Allow-Header",
						"Access-Control-Request-Method",
						"Access-Control-Request-Header"
				))

		);
	}

}
