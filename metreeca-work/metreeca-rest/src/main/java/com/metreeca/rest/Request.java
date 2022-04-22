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

package com.metreeca.rest;

import java.util.*;
import java.util.regex.Pattern;

import static com.metreeca.core.Identifiers.AbsoluteIRIPattern;
import static com.metreeca.core.Identifiers.IRIPattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;


/**
 * HTTP request.
 */
public final class Request extends Message<Request> {

    public static final String GET="GET"; // https://tools.ietf.org/html/rfc7231#section-4.3.1
    public static final String HEAD="HEAD"; // https://tools.ietf.org/html/rfc7231#section-4.3.2
    public static final String POST="POST"; // https://tools.ietf.org/html/rfc7231#section-4.3.3
    public static final String PUT="PUT"; // https://tools.ietf.org/html/rfc7231#section-4.3.4
    public static final String PATCH="PATCH"; // https://tools.ietf.org/html/rfc5789#section-2
    public static final String DELETE="DELETE"; // https://tools.ietf.org/html/rfc7231#section-4.3.5
    public static final String CONNECT="CONNECT"; // https://tools.ietf.org/html/rfc7231#section-4.3.6
    public static final String OPTIONS="OPTIONS"; // https://tools.ietf.org/html/rfc7231#section-4.3.7
    public static final String TRACE="TRACE"; // https://tools.ietf.org/html/rfc7231#section-4.3.8


    private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");
    private static final Pattern FilePattern=Pattern.compile("\\.\\w+$");

    private static final Collection<String> Safe=new HashSet<>(asList(
            GET, HEAD, OPTIONS, TRACE // https://tools.ietf.org/html/rfc7231#section-4.2.1
    ));

    private static final Collection<String> Remote=new HashSet<>(asList(
            "feed:", "ftp:", "http:", "https", "imap", "ldap:", "ldaps", "message:", "pop:", "s3:"
    ));


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Object user;
    private Set<Object> roles=emptySet();

    private String method=GET;
    private String base="app:/";
    private String path="/";
    private String item=base;
    private String query="";

    private final Map<String, List<String>> parameters=new LinkedHashMap<>();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the focus IRI of this request.
     *
     * @return the absolute IRI obtained by concatenating {@linkplain #base() base} and {@linkplain #path() path} for
     * this request
     */
    @Override public String item() {
        return item;
    }

    /**
     * Retrieves the originating request for this request.
     *
     * @return this request
     */
    @Override public Request request() {
        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a response for this request.
     *
     * @return a new empty response for this request
     */
    public Response reply() {
        return new Response(this);
    }

    /**
     * Creates a response for this request.
     *
     * @param status the {@linkplain Response#status(int) status} code of the response
     *
     * @return a new response for this request initialized with {@code status}
     *
     * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
     */
    public Response reply(final int status) {
        return new Response(this).status(status);
    }

    /**
     * Creates a response for this request.
     *
     * @param status   the {@linkplain Response#status(int) status} code of the response
     * @param location the {@code Location} {@linkplain Response#header(String, String) header} of the response
     *
     * @return a new response for this request initialized with {@code status} and {@code location}
     *
     * @throws IllegalArgumentException if {@code response } is less than 100 or greater than 599
     * @throws IllegalArgumentException if {@code location} is not a well-formed relative IRI
     * @throws NullPointerException     if {@code location} is null
     */
    public Response reply(final int status, final String location) {

        if ( location == null ) {
            throw new NullPointerException("null location");
        }

        if ( !IRIPattern.matcher(location).matches() ) {
            throw new IllegalArgumentException(format("malformed location IRI <%s>", location));
        }

        return new Response(this).status(status).header("Location", location);
    }


    //// Checks ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Checks if this request is safe.
     *
     * @return {@code true} if this request is <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">safe</a> ,
     * that is if it's not expected to cause any state change on the origin server ; {@code false} otherwise
     */
    public boolean safe() {
        return Safe.contains(method);
    }

    /**
     * Checks if this request is remote.
     *
     * @return {@code true} if the {@link #item() focus IRI} of this request is based on a remote dereferenceable
     * <a href="https://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml#uri-schemes-1">URI scheme</a>; {@code
     * false} otherwise
     */
    public boolean remote() {
        return Remote.stream().anyMatch(item::startsWith);
    }

    /**
     * Checks if this request targets a collection.
     *
     * @return {@code true} if the {@link #path()} of this request includes a trailing slash; {@code false} otherwise
     *
     * @see <a href="https://www.w3.org/TR/ldp-bp/#include-a-trailing-slash-in-container-uris">Linked Data Platform Best
     * Practices and Guidelines - § 2.6 Include a trailing slash in container URIs</a>
     */
    public boolean collection() {
        return path.endsWith("/");
    }

    /**
     * Checks if this request targets a browser route.
     *
     * @return {@code true} if the {@linkplain #method() method} of this request is {@link #safe() safe} and its {@code
     * Accept} header includes a MIME type usually associated with an interactive browser-managed HTTP request (e.g.
     * {@code text/html} and its {@link #path() path} doesn't contain a filename extension (e.g. {@code .html}); {@code
     * false}, otherwise
     */
    public boolean route() {
        return safe() && (!FilePattern.matcher(path).find()
                && headers("Accept").anyMatch(value -> HTMLPattern.matcher(value).find())
        );
    }

    /**
     * Checks if this request targets a browser asset.
     *
     * @return {@code true} if the {@linkplain #method() method} of this request is {@link #safe() safe} and its {@code
     * Accept} header includes a MIME type usually associated with an interactive browser-managed HTTP request (e.g.
     * {@code text/html} or its {@link #path() path} contains a filename extension (e.g. {@code .html}); {@code false},
     * otherwise
     */
    public boolean asset() {
        return safe() && (FilePattern.matcher(path).find()
                || headers("Accept").anyMatch(value -> HTMLPattern.matcher(value).find())
        );
    }


    /**
     * Checks if this request if performed by a user in a target set of roles.
     *
     * @param roles the target set if roles to be checked
     *
     * @return {@code true} if this request is performed by a {@linkplain #user() user} in one of the given {@code
     * roles}, that is if {@code roles} and  {@linkplain #roles() request roles} are not disjoint
     */
    public boolean role(final Object... roles) {
        return role(asList(roles));
    }

    /**
     * Checks if this request if performed by a user in a target set of roles.
     *
     * @param roles the target set if roles to be checked
     *
     * @return {@code true} if this request is performed by a {@linkplain #user() user} in one of the given {@code
     * roles}, that is if {@code roles} and  {@linkplain #roles() request roles} are not disjoint
     */
    public boolean role(final Collection<Object> roles) {

        if ( roles == null ) {
            throw new NullPointerException("null roles");
        }

        return !disjoint(this.roles, roles);
    }


    //// Actor ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the profile of the request user.
     *
     * @return an optional profile for the user performing this request or the empty optional if no user is authenticated
     */
    public Optional<Object> user() {
        return Optional.ofNullable(user);
    }

    /**
     * Retrieves the profile of the request user.
     *
     * @param <T>  the expected profile type
     * @param type the expected profile type
     *
     * @return an optional typed profile for the user performing this request or the empty optional if either no user is
     * authenticated or the {@linkplain #user(Object) current profile} is not an instance of {@code type}
     *
     * @throws NullPointerException if {@code type} is null
     */
    public <T> Optional<T> user(final Class<T> type) {

        if ( type == null ) {
            throw new NullPointerException("null type");
        }

        return Optional.ofNullable(user).filter(type::isInstance).map(type::cast);
    }

    /**
     * Configures the profile of the request user.
     *
     * @param user a profile for the user performing this request or {@code null} if no user is authenticated
     *
     * @return this request
     */
    public Request user(final Object user) {

        this.user=user;

        return this;
    }


    /**
     * Retrieves the roles attributed to the request user.
     *
     * @return a set of values uniquely identifying the roles attributed to the request {@linkplain #user() user}
     */
    public Set<Object> roles() { return unmodifiableSet(roles); }

    /**
     * Configures the roles attributed to the request user.
     *
     * @param roles a collection of values uniquely identifying the roles assigned to the request {@linkplain #user()
     *              user}
     *
     * @return this request
     *
     * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
     */
    public Request roles(final Object... roles) {
        return roles(asList(roles));
    }

    /**
     * Configures the roles attributed to the request user.
     *
     * @param roles a collection of IRIs uniquely identifying the roles assigned to the request {@linkplain #user()
     *              user}
     *
     * @return this request
     *
     * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
     */
    public Request roles(final Collection<Object> roles) {

        if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null roles");
        }

        this.roles=new LinkedHashSet<>(roles);

        return this;
    }


    //// Action ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the HTTP method of this request.
     *
     * @return the HTTP method of this request; in upper case
     */
    public String method() {
        return method;
    }

    /**
     * Configures the HTTP method of this request.
     *
     * @param method the HTTP method for this request; will be automatically converted to upper case
     *
     * @return this request
     *
     * @throws NullPointerException if {@code method} is null
     */
    public Request method(final String method) {

        if ( method == null ) {
            throw new NullPointerException("null method");
        }

        this.method=method.toUpperCase(Locale.ROOT);

        return this;
    }


    /**
     * Retrieves the base IRI of this request.
     *
     * @return the base IRI of this request, that is the base IRI if the linked data server handling the request;
     * includes a trailing slash
     */
    public String base() {
        return base;
    }

    /**
     * Configures the base IRI of this request.
     *
     * @param base the base IRI for this request, that is the base IRI if the linked data server handling the request
     *
     * @return this request
     *
     * @throws NullPointerException     if {@code base} is null
     * @throws IllegalArgumentException if {@code base} is not an absolute IRI or if it doesn't include a trailing slash
     */
    public Request base(final String base) {

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        if ( !AbsoluteIRIPattern.matcher(base).matches() ) {
            throw new IllegalArgumentException("not an absolute base IRI");
        }

        if ( !base.endsWith("/") ) {
            throw new IllegalArgumentException("missing trailing slash in base IRI");
        }

        this.base=base;
        this.item=base+path.substring(1);

        return this;
    }


    /**
     * Retrieves the resource path of this request.
     *
     * @return the resource path of this request, that is the absolute server path of the linked data resources this
     * request refers to; includes a leading slash
     */
    public String path() {
        return path;
    }

    /**
     * Configures the resource path of this request.
     *
     * @param path the resource path of this request, that is the absolute server path of the linked data resources this
     *             request refers to
     *
     * @return this request
     *
     * @throws NullPointerException     if {@code path} is null
     * @throws IllegalArgumentException if {@code path} doesn't include a leading slash
     */
    public Request path(final String path) {

        if ( path == null ) {
            throw new NullPointerException("null resource path");
        }

        if ( !path.startsWith("/") ) {
            throw new IllegalArgumentException("missing leading / in resource path");
        }

        this.path=path;
        this.item=base+path.substring(1);

        return this;
    }


    /**
     * Retrieves the query of this request.
     *
     * @return the query this request; doesn't include a leading question mark
     */
    public String query() {
        return query;
    }

    /**
     * Configures the query of this request.
     *
     * @param query the query of this request; doesn't include a leading question mark
     *
     * @return this request
     *
     * @throws NullPointerException if {@code query} is null
     */
    public Request query(final String query) {

        if ( query == null ) {
            throw new NullPointerException("null query");
        }

        this.query=query;

        return this;
    }


    /**
     * Retrieves the target resource of this request.
     *
     * @return the full URL of the target resource of this request, including {@link #base() base}, {@link #path() path}
     * and optional {@link #query() query}
     */
    public String resource() {
        return query.isEmpty() ? item() : item()+"?"+query;
    }


    //// Parameters ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves request accepted languages.
     *
     * @return a list of language tags included in the {@code Accept-Language} header of this request or an empty list if
     * no such header is included; may include a wildcard tag ({@code *})
     */
    public List<String> langs() {
        return header("Accept-Language")
                .map(Format::langs)
                .orElse(emptyList());
    }


    /**
     * Retrieves request query parameters.
     *
     * @return an immutable and possibly empty map from query parameters names to collections of values
     */
    public Map<String, List<String>> parameters() {
        return unmodifiableMap(parameters);
    }

    /**
     * Configures request query parameters.
     *
     * <p>Existing values are overwritten.</p>
     *
     * @param parameters a map from parameter names to lists of values
     *
     * @return this message
     *
     * @throws NullPointerException if {@code parameters} is null or contains either null keys or null values
     */
    public Request parameters(final Map<String, ? extends Collection<String>> parameters) {

        if ( parameters == null ) {
            throw new NullPointerException("null parameters");
        }

        parameters.forEach((name, value) -> { // ;( parameters.containsKey()/ContainsValue() can throw NPE

            if ( name == null ) {
                throw new NullPointerException("null parameter name");
            }

            if ( value == null ) {
                throw new NullPointerException("null parameter value");
            }

        });

        this.parameters.clear();

        parameters.forEach(this::parameters);

        return this;
    }


    /**
     * Retrieves request query parameter value.
     *
     * @param name the name of the query parameter whose value is to be retrieved
     *
     * @return an optional value containing the first value among those returned by {@link #parameters(String)}, if one
     * is present; an empty optional otherwise
     *
     * @throws NullPointerException if {@code name} is null
     */
    public Optional<String> parameter(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return parameters(name).stream().findFirst();
    }

    /**
     * Configures request query parameter value.
     *
     * <p>Existing values are overwritten.</p>
     *
     * @param name  the name of the query parameter whose value is to be configured
     * @param value the new value for {@code name}
     *
     * @return this message
     *
     * @throws NullPointerException if either {@code name} or {@code value} is null
     */
    public Request parameter(final String name, final String value) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        return parameters(name, value);
    }


    /**
     * Retrieves request query parameter values.
     *
     * @param name the name of the query parameter whose values are to be retrieved
     *
     * @return an immutable and possibly empty collection of values
     */
    public List<String> parameters(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return unmodifiableList(parameters.getOrDefault(name, emptyList()));
    }

    /**
     * Configures request query parameter values.
     *
     * <p>Existing values are overwritten.</p>
     *
     * @param name   the name of the query parameter whose values are to be configured
     * @param values a possibly empty collection of values
     *
     * @return this message
     *
     * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
     *                              {@code null} value
     */
    public Request parameters(final String name, final String... values) {
        return parameters(name, asList(values));
    }

    /**
     * Configures request query parameter values.
     *
     * <p>Existing values are overwritten.</p>
     *
     * @param name   the name of the query parameter whose values are to be configured
     * @param values a possibly empty collection of values
     *
     * @return this message
     *
     * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
     *                              {@code null} value
     */
    public Request parameters(final String name, final Collection<String> values) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( values == null ) {
            throw new NullPointerException("null values");
        }

        if ( values.contains(null) ) {
            throw new NullPointerException("null value");
        }

        if ( values.isEmpty() ) {

            parameters.remove(name);

        } else {

            parameters.put(name, unmodifiableList(new ArrayList<>(values)));

        }

        return this;
    }

}
