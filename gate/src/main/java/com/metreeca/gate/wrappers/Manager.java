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

package com.metreeca.gate.wrappers;

import com.metreeca.gate.*;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.tray.sys.Clock;
import com.metreeca.tray.sys.Trace;

import io.jsonwebtoken.Claims;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.gate.Crypto.crypto;
import static com.metreeca.gate.Notary.notary;
import static com.metreeca.gate.Roster.roster;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Clock.clock;
import static com.metreeca.tray.sys.Trace.trace;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.regex.Pattern.compile;


/**
 * Session manager.
 *
 * <p>Manages user authentication/authorization and session lifecycle using permits issued by shared {@link
 * Roster#roster() roster} tool.</p>
 *
 * <p>Provides a virtual session management handler at the {@linkplain #Manager(String, long, long) provided path}
 * with the methods described below.</p>
 *
 * <p><strong>Warning</strong> / Make sure XSS/XSRF {@linkplain Protector protection} is active when using session
 * managers.</p>
 *
 * <hr>
 *
 * <h3>Session Extension</h3>
 *
 * <p>This is a utility stub for extending existing sessions and/or priming XSRF {@linkplain Protector#cookie(boolean)
 * cookie}-based protection before actual session creation with a POST request.</p>
 *
 * <pre>{@code GET <path>
 * Cookie: ID=<token> // optional}</pre>
 *
 *
 * <p><em>Successful session extension</em> / The current session token, if present, is extended.</p>
 *
 * <pre>{@code 200 OK
 * Set-Cooke: ID=<token>; Path=<base>; HttpOnly; Secure; SameSite=Lax // optional}</pre>
 *
 * <hr>
 *
 * <h3>Session Creation</h3>
 *
 * <pre>{@code POST <path>
 * Content-Type: application/json
 *
 * {
 *     "handle": "<handle>",            // user handle, e.g. an email address
 *     "secret": "<current password>",  // current user password
 *     "update": "<new password>"       // new user password (optional)
 * } }</pre>
 *
 * <p><em>Successful credentials validation</em> / A session is created and reported with a response including a
 * session token in a HTTP only session cookie and a JSON payload containing the session idle timeout in milliseconds
 * and the user profile included in the {@linkplain Permit#profile() permit} returned by the user {@linkplain Roster
 * roster}.</p>
 *
 * <pre>{@code 201 Created
 * Set-Cooke: ID=<token>; Path=<base>; HttpOnly; Secure; SameSite=Lax
 * Content-Type: application/json
 *
 * {
 *     "timeout": 3600000,  // idle session timeout [ms]
 *     "profile": { … }     // user profile
 * }}</pre>
 *
 * <p><em>Failed credentials validation</em> / Reported with a response providing a machine-readable error code among
 * those defined by {@link Roster}.</p>
 *
 * <pre>{@code 403 Forbidden
 * Content-Type: application/json
 *
 * {
 *      "error": "<error>"
 * }}</pre>
 *
 * <p><em>Malformed session ticket</em> / Reported with a response providing a machine-readable error code.</p>
 *
 * <pre>{@code 400 Bad Request
 * Content-Type: application/json
 *
 * {
 *      "error": "ticket-malformed"
 * }}</pre>
 *
 * <hr>
 *
 * <h3>Session Deletion</h3>
 *
 * <pre>{@code POST <path>
 * Cookie: ID=<token> // optional
 * Content-Type: application/json
 *
 * {}}</pre>
 *
 * <p><em>Successful session deletion</em> / The current session is delete and reported with a response including an
 * empty session token in a HTTP only self-expiring session cookie; malformed and unknown session tokens are
 * ignored.</p>
 *
 * <pre>{@code 204 No Content
 * Set-Cooke: ID=; Path=<base>; HttpOnly; Secure; SameSite=Lax; Max-Age=0}</pre>
 *
 * <hr>
 *
 * <h3>Secured Resource Access</h3>
 *
 * <p>Between session creation and deletion, any requests to restricted REST endpoints will automatically include the
 * session token in a cookie header, enabling user authentication and authorization.</p>
 *
 * <pre>{@code <METHOD> <resource>
 * Cookie: ID=<token>
 *
 * …}</pre>
 *
 * <p><em>Successful token validation</em> / The response includes the response generated by the secured handler.</p>
 *
 * <pre>{@code <###> <Status>
 *
 * … }</pre>
 *
 * <p><em>Failed token validation</em> / Due either to session deletion or expiration or to a modified user {@linkplain
 * Permit#id() opaque handle}; no details about the failure are disclosed.</p>
 *
 * <pre>{@code 403 Forbidden
 * Content-Type: application/json
 *
 * {} }</pre>
 */
public final class Manager implements Wrapper {

	public static final String SessionCookie="ID";

	public static final String TicketMalformed="ticket-malformed";


	private static final int TokenIdLength=32; // [bytes]

	private static final Pattern SessionCookiePattern=compile(format("\\b%s\\s*=\\s*(?<value>[^\\s;]+)", SessionCookie));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String path;

	private final long soft;
	private final long hard;


	private final Handler housekeeper=new Worker()
			.get(get())
			.post(post());


	private final Roster roster=tool(roster());
	private final Notary notary=tool(notary());
	private final Crypto crypto=tool(crypto());

	private final Clock clock=tool(clock());
	private final Trace trace=tool(trace());


	/**
	 * Creates a session manager.
	 *
	 * @param path the root relative path of the virtual session handler
	 * @param soft the soft session timeout in milliseconds; on soft timeout session are automatically deleted if no
	 *             activity was registered in the given period
	 * @param hard the hard session timeout in milliseconds; on hard timeout session are unconditionally deleted
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} is not root relative or either {@code idle} or {@code hard} is
	 *                                  less than or equal to 0 or {@code idle} is greater than {@code hard}
	 */
	public Manager(final String path, final long soft, final long hard) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("not a root relative path <"+path+">");
		}

		if ( soft <= 0 || hard <= 0 || soft > hard ) {
			throw new IllegalArgumentException("illegal timeouts <"+soft+"/"+hard+">");
		}

		this.path=path;

		this.soft=soft;
		this.hard=hard;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(challenger())
				.with(gatekeeper())
				.with(housekeeper());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper challenger() { // add authentication challenge, unless already provided by wrapped handlers
		return handler -> request -> handler.handle(request).map(response -> response.status() == Response.Unauthorized
				? response.header("~WWW-Authenticate", format("Session realm=\"%s\"", response.request().base()))
				: response
		);
	}

	private Wrapper gatekeeper() {
		return handler -> request -> cookie(request) // look for session token

				.map(token -> notary.verify(token) // token found >> verify

						.map(claims -> lookup(claims.getSubject()).fold( // valid token >> look for permit

								permit -> handler.handle(request // permit found > authenticate and process request

										.user(permit.user())
										.roles(permit.roles())

								).map(response -> {

									final long now=clock.time();

									final long issued=claims.getIssuedAt().getTime();
									final long expiry=claims.getExpiration().getTime();

									if ( expiry-now < soft*90/100 ) { // extend if residual lease is < 90% soft timeout
										response.header("Set-Cookie",
												cookie(request, permit.id(), issued, min(now+soft, issued+hard))
										);
									}

									return response // disable caching of protected content

											.header("~Cache-Control", "no-store")
											.header("~Pragma", "no-cache");

								}),

								error -> { // permit not found >> reject request

									trace.warning(this, "unknown session token");

									return request.reply(response -> response
											.status(Response.Unauthorized)
									);

								}
						))

						.orElseGet(() -> { // invalid token >> reject request

							trace.warning(this, "invalid session token");

							return request.reply(response -> response
									.status(Response.Unauthorized)
							);

						})

				)

				.orElseGet(() -> handler.handle(request)); // token not found >> process request
	}

	private Wrapper housekeeper() {
		return handler -> request -> request.path().equals(path)
				? housekeeper.handle(request)
				: handler.handle(request);
	}


	private Handler get() {
		return request -> request.reply(response -> response.status(Response.OK));
	}

	private Handler post() {
		return request -> request.reply(response -> request.body(json())

				.process(ticket -> {

					if ( ticket.isEmpty() ) {

						cookie(request)
								.flatMap(notary::verify)
								.map(Claims::getSubject)
								.ifPresent(this::signout);

						return Value(response.status(Response.NoContent)
								.header("Set-Cookie", cookie(request, "", 0, 0))
						);

					} else {

						return signin(ticket).value(permit -> {

							final long now=clock.time();

							return response.status(Response.Created)

									.header("Set-Cookie", cookie(request, permit.id(), now, now+soft))

									.body(json(), Json.createObjectBuilder()
											.add("timeout", soft)
											.add("profile", object(permit.profile()))
											.build()
									);

						});

					}

				})

				.fold(
						identity(),
						response::map
				)

		);
	}


	//// Roster Wrappers ///////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, String> lookup(final String handle) {
		return roster.lookup(handle);
	}

	private Result<Permit, Failure> signin(final JsonObject ticket) {
		return Optional.of(ticket)

				.filter(t -> t.values().stream().allMatch(value -> value instanceof JsonString))
				.filter(t -> set("handle", "secret", "update").containsAll(t.keySet()))

				.flatMap(t -> {

					final String handle=t.getString("handle", null);
					final String secret=t.getString("secret", null);
					final String update=t.getString("update", null);

					return handle == null || secret == null ? Optional.empty()
							: update == null ? Optional.of(roster.signin(handle, secret))
							: Optional.of(roster.signin(handle, secret, update));

				})

				.map(result -> result.<Result<Permit, Failure>>fold(

						permit -> { // log opaque handle to support entry correlation without exposing sensitive info

							trace.info(this, "login "+session(permit));

							return Value(permit);

						},

						error -> { // log only error without exposing sensitive info

							trace.warning(this, "login error "+error);

							return Error(new Failure().status(Response.Forbidden).error(error));

						}

				))

				.orElseGet(() -> { // log only error without exposing sensitive info

					trace.warning(this, "login error "+TicketMalformed);

					return Error(new Failure().status(Response.BadRequest).error(TicketMalformed));

				});
	}

	private void signout(final String handle) {
		roster.signout(handle).use(

				permit -> { // log opaque handle to support entry correlation without exposing sensitive info

					trace.info(this, "logout "+session(permit));

				},

				error -> { // log only error without exposing sensitive info

					trace.warning(this, "logout error "+error);

				}

		);
	}


	private String session(final Permit permit) {
		return crypto.token(permit.id(), permit.user().stringValue());
	}


	//// Cookie Codecs /////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<String> cookie(final Request request) {
		return request.headers("Cookie")
				.stream()
				.map(value -> {

					final Matcher matcher=SessionCookiePattern.matcher(value);

					return matcher.find() ? matcher.group("value") : null;

				})
				.filter(Objects::nonNull)
				.findFirst();
	}

	private String cookie(final Request request, final String handle, final long issued, final long expiry) {
		return format("%s=%s; Path=%s; SameSite=Lax; HttpOnly%s%s",
				SessionCookie,
				handle.isEmpty() ? "" : notary.create(claims -> claims
						.setId(crypto.token(TokenIdLength))
						.setSubject(handle)
						.setIssuedAt(new Date(issued))
						.setExpiration(new Date(expiry))
				),
				request.base(),
				request.base().startsWith("https:") ? "; Secure" : "",
				handle.isEmpty() ? "; Max-Age=0" : ""
		);
	}

}
