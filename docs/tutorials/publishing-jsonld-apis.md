---
title: "Publishing Model‑Driven REST/JSON-LD APIs"
trail: "Tutorials"
---

[comment]: <> "excerpt:    Hands-on guided tour of model-driven REST/JSON-LD APIs publishing"

This example-driven tutorial introduces the main building blocks of the Metreeca/Java model-driven REST/JSON framework.
Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts
and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to use the framework to publish linked data resources through REST/JSON-LD
APIs that automatically support CRUD operations, faceted search, data validation and fine‑grained role‑based access
control.

In the tutorial we will work with a linked data version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample
dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries.

The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *
order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models: we will walk through the REST
API development process focusing on the task of publishing
the [Product](https://demo.metreeca.com/self/#endpoint=https://demo.metreeca.com/toys/sparql&collection=https://demo.metreeca.com/toys/terms#Product)
catalog.

You may try out the examples using your favorite API testing tool or working from the command line with tools like `curl`
or `wget`.

# Getting Started

To get started, set up a Maven Java 1.8 project, importing the BOM module for Metreeca/Java:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<groupId>com.example</groupId>
	<artifactId>sample</artifactId>
	<version>1.0</version>

	<properties>

		<maven.compiler.target>1.8</maven.compiler.target>
		<maven.compiler.source>1.8</maven.compiler.source>

	</properties>

	<dependencyManagement>

		<dependencies>

			<dependency>
				<groupId>com.metreeca</groupId>
				<artifactId>metreeca-java</artifactId>
				<version>${project.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>

		</dependencies>

	</dependencyManagement>

</project>
```

Then, add the required dependencies for the Metreeca/Java [connectors](../#modules) for the target deployment server and
the target graph storage option; in this tutorial we’ll use an RDF4J Memory store deploying either to an embedded server
or to a Servlet 3.1 container.

Note that the Metreeca/Java BOM module re-exports the BOM module for the target RDF4J version, so we don't need to
specify version numbers explicitly.

## Embedded Server

Add the following dependencies to your Maven project:

```xml

<dependencies>

	<dependency>
		<groupId>com.metreeca</groupId>
		<artifactId>metreeca-jse</artifactId>
	</dependency>

	<dependency>
		<groupId>com.metreeca</groupId>
		<artifactId>metreeca-rdf4j</artifactId>
	</dependency>

	<dependency>
		<groupId>org.eclipse.rdf4j</groupId>
		<artifactId>rdf4j-repository-sail</artifactId>
	</dependency>

	<dependency>
		<groupId>org.eclipse.rdf4j</groupId>
		<artifactId>rdf4j-sail-memory</artifactId>
	</dependency>

</dependencies>
```

Then, define in the `src/main/java` folder a minimal server stub like:

```java
import com.metreeca.jse.JSEServer;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.wrappers.Server.server;

public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator.get(() ->

						server().wrap(request -> request.reply(response ->
								response.status(OK)
						))

				))

				.start();
	}

}
```

Compile and launch the application.

## Servlet Filter

Add the following definitions and dependencies to your Maven project:

```xml

<project>

	<packaging>war</packaging>

	<dependencies>

		<dependency>
			<groupId>com.metreeca</groupId>
			<artifactId>metreeca-jee</artifactId>
		</dependency>

		<dependency>
			<groupId>com.metreeca</groupId>
			<artifactId>metreeca-rdf4j</artifactId>
		</dependency>

		<dependency>
			<groupId>org.eclipse.rdf4j</groupId>
			<artifactId>rdf4j-repository-sail</artifactId>
		</dependency>

		<dependency>
			<groupId>org.eclipse.rdf4j</groupId>
			<artifactId>rdf4j-sail-memory</artifactId>
		</dependency>

		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<version>3.1.0</version>
			<scope>provided</scope>
		</dependency>

	</dependencies>

</project>
```

Copy [`web.xml`](toys/web.xml) to the `src/main/webapp` folder.

```xml

<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

</web-app>
```

Then, define a minimal server stub like:

```java
import com.metreeca.jee.JEEServer;

import javax.servlet.annotation.WebFilter;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.wrappers.Server.server;

@WebFilter(urlPatterns="/*")
public final class Server extends JEEServer {

	public Server() {
		delegate(locator -> locator.get(() ->

				server().wrap(request -> request.reply(response ->
						response.status(OK)
				))

		));
	}

}
```

Compile and deploy the web app to your favorite servlet container.

## Test Connection

Both stubs configure the application to handle any resource using a
barebone [handler](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Handler.html) always
replying to incoming [requests](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Request.html)
with a [response](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Response.html) including
a `200` HTTP status code. The
standard [Server](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/wrappers/Server.html)
wrapper provides default pre/postprocessing services and shared error handling.

The server should now be up and running and you can try your first request:

```shell
% curl --include 'http://localhost:8080/'

HTTP/1.1 200
```

From now on, we will extend the embedded server version, but the code applies with minor tweaks to the servlet versions
as well.

## Sample Data

The [locator](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/http/Locator.html) argument handled
to the app loader lambda manages the shared system-provided services and can be used to customize them and to run app
initialization tasks. Copy [`toys.ttl`](toys/toys.ttl)
to the `src/main/resources/` folder, extend the stub as follows and relaunch the application:

```java
import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.http.Locator.service;
import static com.metreeca.rest.wrappers.Server.server;

public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator

						.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

						.exec(() -> service(graph()).update(connection -> {
							try {

								connection.add(
										Toys.class.getResourceAsStream("toys.ttl"),
										"https://example.com/", RDFFormat.TURTLE
								);

								return null;

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						}))

						.get(() ->
								server().wrap(request -> request.reply(response ->
										response.status(OK)
								))

						))

				.start();
	}

}
```

Here we are customizing the shared
system-wide [graph](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j/latest/com/metreeca/rdf4j/services/Graph.html)
database as an ephemeral memory-based RDF4J store, initializing it on demand with the BIRT dataset.

The static [Locator.service()](https://javadoc.io/static/com.metreeca/metreeca-rest/1.0.1/com/metreeca/http/Locator.
html#service(java.util.function.Supplier))
locator method provides access to shared services.

Complex initialization tasks can be easily factored to a dedicated loader class:

```java
import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.wrappers.Server.server;

public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator

						.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

						.exec(new Toys())

						.get(() ->
								server().wrap(request -> request.reply(response ->
										response.status(OK)
								))

						))

				.start();
	}

}
```

```java
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.http.Locator.service;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

public final class Toys implements Runnable {

	public static final String Base="https://example.com/";
	public static final String Namespace=Base+"terms#";


	@Override
	public void run() {

		service(graph()).update(connection -> {
			if ( !connection.hasStatement(null, null, null, false) ) {
				try {

					connection.setNamespace("toys", Namespace);
					connection.add(getClass().getResourceAsStream("toys.ttl"), Base, TURTLE);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}

			return this;

		});
	}

}
```

# Handling Requests

Requests are dispatched to their final handlers through a hierarchy of wrappers and delegating handlers.

```java
import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.http.Locator.service;
import static com.metreeca.rest.Wrapper.preprocessor;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.wrappers.Server.server;

import static org.eclipse.rdf4j.common.iteration.Iterations.asList;
import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.stream.Collectors.toList;

public final class Server {

	public static void main(final String... args) {
      new JSEServer()

              .delegate(locator -> locator

                      .set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

						.exec(new Toys())

						.get(() -> server()

								.with(preprocessor(request -> request.base(Toys.Base)))

								.wrap(router()

										.path("/products/*", router()

												.path("/", router().get(request -> request.reply(response -> response
														.status(OK)
														.body(rdf(), service(graph()).query(connection ->
																stream(connection.getStatements(
																		null, RDF.TYPE, iri(Toys.Namespace, "Product")
																))
																		.map(Statement::getSubject)
																		.map(p -> statement(
																				iri(request.item()), LDP.CONTAINS, p)
																		)
																		.collect(toList())
														))
												)))

												.path("/{code}",
														router().get(request -> request.reply(response -> response
																.status(OK)
																.body(rdf(), service(graph()).query(connection ->
																		asList(connection.getStatements(
																						iri(request.item()), null, null
																				)
																		))
																))
														)
												)

										)

								)

						)
				)

				.start();
	}

}
```

[Wrappers](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Wrapper.html) inspect and possibly
alter incoming requests and outgoing responses before they are forwarded to wrapped handlers and returned to wrapping
containers.

The preprocessor rebases RDF payloads from the external network-visible server base (`http://localhost:8080/`) to an
internal canonical base (`https://demo.metreeca.com/`), ensuring data portability between development and production
environments and making it possible to load the static RDF dataset during server initialization, while the external and
possibly request-dependent base is not yet known. When external and internal bases match, as in production, rewriting is
effectively disabled avoiding any performance hit.

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: text/turtle;charset=UTF-8

@base <https://example.com/products/S18_4409> .

<> a </terms#Product>;
  </terms#buy> 43.26;
  </terms#code> "S18_4409";
  </terms#scale> "1:18";
  </terms#sell> 92.03;
  </terms#stock> 6553;
  </terms#vendor> "Exoto Designs";
  <http://www.w3.org/2000/01/rdf-schema#comment> "This 1:18 scale precision die cast replica …";
  <http://www.w3.org/2000/01/rdf-schema#label> "1932 Alfa Romeo 8C2300 Spider Sport"
  ⋮
```

## Request Routing

[Routers](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/handlers/Router.html) dispatch
requests on the basis of
the [request path](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Request.html#path()) and
the [request method](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Request.html##method()),
ignoring leading path segments possibly already matched by wrapping routers.

Requests are forwarded to a registered handler if their path is matched by an associated pattern defined by a sequence of
steps according to the following rules:

| pattern step | matching path step   | definition                                                   |
| ------------ | -------------------- | ------------------------------------------------------------ |
| `/`          | `/`                  | empty / matches only the empty step                          |
| `/<step>`    | `/<step>`            | literal / matches step verbatim                              |
| `/{}`        | `/<step>`            | wildcard / matches a single step                             |
| `/{<key>}`   | `/<step>`            | placeholder / match a single path step, adding the matched `<key>`/`<step>` entry to request [parameters](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Request.html#parameters()); the matched `<step>` name is URL-decoded before use |
| `/*`         | `/<step>[/<step>/…]` | prefix / matches one or more trailing steps                  |

Registered path patterns are tested in order of definition.

If the router doesn't contain a matching handler, no action is performed giving the container adapter a fall-back
opportunity to handle the request.

## Combo Handlers

Again, complex handlers can be easily factored to dedicated classes:

```java
import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.services.Graph;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Wrapper.preprocessor;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.wrappers.Server.server;

public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator

						.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

						.exec(new Toys())

						.get(() -> server()

								.with(preprocessor(request -> request.base(Toys.Base)))

								.wrap(router()

										.path("/products/*", new Products())

								)

						)
				)

				.start();
	}

}
```

```java
import com.metreeca.rest.Handler.Base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.http.Locator.service;
import static com.metreeca.rest.handlers.Router.router;

import static org.eclipse.rdf4j.common.iteration.Iterations.asList;
import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.stream.Collectors.toList;

public final class Products extends Delegator {

	public static final IRI Product=iri(Toys.Namespace, "Product");


	public Products() {
		delegate(router()

				.path("/", router().get(request -> request.reply(response -> response
						.status(OK)
						.body(rdf(), service(graph()).query(connection ->
								stream(connection.getStatements(
										null, RDF.TYPE, Product
								))
										.map(Statement::getSubject)
										.map(p -> statement(
												iri(request.item()), LDP.CONTAINS, p)
										)
										.collect(toList())
						))
				)))

				.path("/{code}",
						router().get(request -> request.reply(response -> response
								.status(OK)
								.body(rdf(), service(graph()).query(connection ->
										asList(connection.getStatements(
														iri(request.item()), null, null
												)
										))
								))
						)
				)
		);
	}
}
```

The [Delegator](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/handlers/Delegator.html)
abstract handler provides a convenient way of packaging complex handlers assembled as a combination of other handlers and
wrappers.

# Model-Driven Handlers

Standard resource action handlers can be defined using high-level declarative models that drive automatic fine‑grained
role‑based read/write access control, faceted search, incoming data validation and bidirectional conversion between RDF
and idiomatic [compacted/framed](../references/jsonld -format) JSON-LD payloads, as demonstrated in
the [REST APIs interaction tutorial](consuming-jsonld-apis.md).

Actors provide default shape-driven implementations for CRUD actions on resources and containers identified by the
request [focus item](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Request.html#item()).

| actor                                                        | action                                                       |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Relator](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/operators/Relator.html) | resource retrieval / retrieves the detailed RDF description of the target resource; supports extended collection [faceted search](consuming-jsonld-apis.md#faceted-search), sorting and pagination |
| [Creator](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/operators/Creator.html) | container resource creation / uploads the detailed RDF description of a new resource to be inserted into the target container |
| [Updater](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/operators/Updater.html) | resource updating / updates the detailed RDF description of the target resource |
| [Deleter](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/operators/Deleter.html) | resource deletion / deletes the detailed RDF description of the target resource |

```java
import com.metreeca.jse.JSEServer;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rdf4j.services.GraphEngine;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Wrapper.preprocessor;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.services.Engine.engine;
import static com.metreeca.rest.wrappers.Server.server;

public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator

						.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
						.set(engine(), GraphEngine::new) // <<< add this line <<<

						.exec(new Toys())

						.get(() -> server()

								.with(preprocessor(request -> request.base(Toys.Base)))

								.wrap(router()

										.path("/products/*", new Products())

								)

						)
				)

				.start();
	}

}
```

Actors delegate transaction management, data validation and trimming and CRUD operations to a customizable engine.

CRUD perations are performed on the graph neighbourhood of the target target item(s)  matched by
the  [shape](https://javadoc.io/doc/com.metreeca/metreeca-json/latest/com/metreeca/json/Shape.html)
model associated to the request, after redaction according to the request user roles and to actor-specific task, area and
mode parameters.

## Defining Models

Let's start by defining a barebone model stating that all resources of class `Product` are to be published as container
items exposing only `rdf:type`, `rdfs:label`  and `rdfs:comment` properties.

```java
import com.metreeca.rest.Handler.Base;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Creator.creator;
import static com.metreeca.rest.operators.Deleter.deleter;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.operators.Updater.updater;
import static com.metreeca.rest.wrappers.Driver.driver;

public final class Products extends Delegator {

	public Products() {
		delegate(driver(

				field(RDF.TYPE),
				field(RDFS.LABEL),
				field(RDFS.COMMENT)

		).wrap(router()

				.path("/", router()
						.get(relator())
						.post(creator())
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

The [Driver](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/wrappers/Driver.html) wrapper
associated a linked data model to incoming requests, driving the operations of nested actors and other model-aware
handlers.

Linked data models are defined with a shape-based [specification language](../reference/spec-language.md), assembling
shape [building blocks](../reference/spec-language.md#shapes) using a simple Java DSL.

As soon as the server is redeployed, the updated REST API exposes only the data specified in the driving model.

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "/products/S18_4409",
    "type": [
        "/terms#Product"
    ],
    "label": [
        "1932 Alfa Romeo 8C2300 Spider Sport"
    ],
    "comment": [
        "This 1:18 scale precision die cast replica features the 6 front headlights…"
    ]
}
```

We'll now refine the initial barebone model, exposing more properties and detailing properties roles and constraints.

```java
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.json.Values.iri;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.http.Locator.service;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;

public final class Toys implements Runnable {

	public static final String Base="https://example.com/";
	public static final String Namespace=Base+"terms#";

	public static final IRI staff=toys("staff");

	public static final IRI Order=toys("Order");
	public static final IRI Product=toys("Product");
	public static final IRI ProductLine=toys("ProductLine");

	public static final IRI amount=toys("amount");
	public static final IRI buy=toys("buy");
	public static final IRI code=toys("code");
	public static final IRI customer=toys("customer");
	public static final IRI line=toys("line");
	public static final IRI product=toys("product");
	public static final IRI order=toys("order");
	public static final IRI scale=toys("scale");
	public static final IRI sell=toys("sell");
	public static final IRI size=toys("size");
	public static final IRI status=toys("status");
	public static final IRI stock=toys("stock");
	public static final IRI vendor=toys("vendor");


	private static IRI toys(final String name) {
		return iri(Namespace, name);
	}

	@Override
	public void run() {

		service(graph()).update(connection -> {
			if ( !connection.hasStatement(null, null, null, false) ) {
				try {

					connection.setNamespace("toys", Namespace);
					connection.add(getClass().getResourceAsStream("toys.ttl"), Base, TURTLE);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}

			return this;

		});
	}

}
```

```java
import com.metreeca.rest.Handler.Base;

import org.eclipse.rdf4j.model.vocabulary.*;

import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Creator.creator;
import static com.metreeca.rest.operators.Deleter.deleter;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.operators.Updater.updater;
import static com.metreeca.rest.wrappers.Driver.driver;

public final class Products extends Delegator {

	public Products() {
		delegate(driver(or(relate(), role(Toys.staff)).then(

				filter(clazz(Toys.Product)),

				field(RDF.TYPE, exactly(Toys.Product)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(500)),

				server(field(Toys.code, required())),

				field(Toys.line, required(), convey(clazz(Toys.ProductLine)),

						relate(field(RDFS.LABEL, required()))

				),

				field(Toys.scale, required(),
						datatype(XSD.STRING),
						pattern("1:[1-9][0-9]{1,2}")
				),

				field(Toys.vendor, required(),
						datatype(XSD.STRING),
						maxLength(50)
				),

				field("price", Toys.sell, required(),
						datatype(XSD.DECIMAL),
						minExclusive(literal(0.0)),
						maxExclusive(literal(1_000.0))
				),

				role(Toys.staff).then(field(Toys.buy, required(),
						datatype(XSD.DECIMAL),
						minInclusive(literal(0.0)),
						maxInclusive(literal(1_000.0))
				)),

				server().then(field(Toys.stock, required(),
						datatype(XSD.INTEGER),
						minInclusive(literal(0)),
						maxExclusive(literal(10_000))
				))

		)).wrap(router()

				.path("/", router()
						.get(relator())
						.post(creator())
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

The `filter` section states that this model describes a container whose member are the instances of the `toys:Product`
class.

The extended resource model makes use of a number of additional blocks to precisely define the expected shape of the RDF
description of the member resources, for instance including `required` and `datatype` and `pattern` constraints to state
that `rdfs:label`, `rdfs:comment`, `toys:code`, `toys:scale` and `toys:vendor` values are expected:

- to occur exactly once for each resource;
- to be RDF literals of `xsd:string` datatype;
- to possibly match a specific regular expression pattern.

```shell
% curl --include 'http://localhost:8080/products/S18_4409'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "/products/S18_4409",
    "type": "/terms#Product",
    "label": "1932 Alfa Romeo 8C2300 Spider Sport",
    "comment": "This 1:18 scale precision die cast replica features the 6 front headlights …",
    "code": "S18_4409",
    "stock": 6553,
    "line": {
        "@id": "/product-lines/vintage-cars",
        "label": "Vintage Cars"
    },
    "scale": "1:18",
    "vendor": "Exoto Designs",
    "price": 92.03
}
```

The constraints in the extended model are leveraged by the engine in a number of ways, for instance to optimize the JSON
representation of the RDF description in order to make it more usable for front-end development, omitting nested arrays
where a property is known to occur at most once and so on.

## Parameterizing Models

The`filter()` and `server()` guards in the extended model also introduce the concept
of [parametric](../reference/spec-language.md#parameters) model.

The `filter` guard states that nested constraints ae to be used only selecting existing resources to be exposed as
container members and not for extracting outgoing data and validating incoming data.

The `convey` guard states that nested constraints are to be used only for extracting outgoing data and validating
incoming data and not for selecting existing resources to be exposed as container members.

The `server` guard states that guarded properties are server-managed and will be considered only when retrieving or
deleting resources, but won't be accepted as valid content on resource creation and updating.

In the most general form, models may be parameterized on for different [axes](../reference/spec-language.md#parameters).
Constraints specified outside parametric sections are unconditionally enabled.

## Controlling Access

Parametric models support the definition of resource-level task/role-based access control rules.

```java
or(relate(),role(Toys.staff)).then(…)
```

This guard states that the product catalog and the contained resources are accessible only if the request **either** has
a `GET` method **or** is performed by a user in the `toys:staff` role.

Parametric models support also the definition of fine-grained access control rules and role-dependent read/write resource
views.

```java
role(Toys.staff).then(field(Toys.buy,required(),
		datatype(XSD.DECIMAL),
		minInclusive(literal(0.0)),
		maxInclusive(literal(1_000.0))
		))
```

This `role` guard states that the `toys:buy` price will be visible only if the request is performed by a user in
the `toys:staff` role.

User roles are usually granted to requests by authentication/authorization wrappers, like in the following naive sample:

```java
public final class Server {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(locator -> locator

						.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
						.set(engine(), GraphEngine::new)

						.exec(new Toys())

						.get(() -> server()

								.with(preprocessor(request -> request.base(Toys.Base)))

								.with(bearer("secret", Toys.staff)) // <<< add this line <<<

								.wrap(router()

										.path("/products/*", new Products())

								)

						)
				)

				.start();
	}

}
```

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "price": 92.03,
}
```

```shell
% curl --include \
    --header 'Authorization: Bearer secret' \
    'http://localhost:8080/products/S18_4409'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "price": 92.03,
    "buy": 43.26 # << buy price included
}
```

```shell
% curl --include --request DELETE \
    'http://localhost:8080/products/S18_4409'
    
HTTP/1.1  401 Unauthorized # << user not authenticated in the `toys:staff` role

```

# Pre/Postprocessing

We'll now complete the product catalog, adding:

- a slug generator for assigning meaningful names to new resources;
- postprocessing scripts for updating server-managed properties and perform other housekeeping tasks when resources are
  created or modified.

Copy [`ProductsCreate.ql`](toys/ProductsCreate.ql)
to the `src/main/resources/` directory and extend `Products` as follows:

```sparql
prefix toys: <terms#>

prefix owl: <http://www.w3.org/2002/07/owl#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>

#### assign the unique scale-based product code generated by the slug function ####

insert { $this toys:code $name } where {};


#### initialize stock #############################################################

insert { $this toys:stock 0 } where {};
```

```java
import com.metreeca.http.Locator;
import com.metreeca.rest.Handler.Base;

import org.eclipse.rdf4j.model.vocabulary.*;

import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rdf4j.services.Graph.update;
import static com.metreeca.http.Locator.text;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Creator.creator;
import static com.metreeca.rest.operators.Deleter.deleter;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.operators.Updater.updater;
import static com.metreeca.rest.wrappers.Driver.driver;

public final class Products extends Delegator {

	public Products() {
		delegate(driver(or(relate(), role(Toys.staff)).then(

				filter(clazz(Toys.Product)),

				field(RDF.TYPE, exactly(Toys.Product)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(500)),

				server(field(Toys.code, required())),

				field(Toys.line, required(), convey(clazz(Toys.ProductLine)),

						relate(field(RDFS.LABEL, required()))

				),

				field(Toys.scale, required(),
						datatype(XSD.STRING),
						pattern("1:[1-9][0-9]{1,2}")
				),

				field(Toys.vendor, required(),
						datatype(XSD.STRING),
						maxLength(50)
				),

				field("price", Toys.sell, required(),
						datatype(XSD.DECIMAL),
						minExclusive(literal(0.0)),
						maxExclusive(literal(1_000.0))
				),

				role(Toys.staff).then(field(Toys.buy, required(),
						datatype(XSD.DECIMAL),
						minInclusive(literal(0.0)),
						maxInclusive(literal(1_000.0))
				)),

				server().then(field(Toys.stock, required(),
						datatype(XSD.INTEGER),
						minInclusive(literal(0)),
						maxExclusive(literal(10_000))
				))

		)).wrap(router()

				.path("/", router()
						.get(relator())
						.post(creator()
								.slug(new ProductsSlug())
								.with(postprocessor(update(text(Products.class, "ProductsCreate.ql"))))
						))

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

```java
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.function.Function;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.http.Locator.service;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;

import static org.eclipse.rdf4j.model.util.Values.literal;

public final class ProductsSlug implements Function<Request, String> {

	private final Graph graph=service(graph());

	@Override
	public String apply(final Request request) {
		return graph.update(connection -> {

			final Value scale=literal(request.body(jsonld()).get()
					.flatMap(frame -> frame.string(Toys.scale))
					.orElse("1:1")
			);

			int serial=0;

			try ( final RepositoryResult<Statement> matches=connection.getStatements(
					null, Toys.scale, scale
			) ) {
				for (; matches.hasNext(); matches.next()) { ++serial; }
			}

			String code="";

			do {
				code=String.format("S%s_%d", scale.stringValue().substring(2), serial);
			} while ( connection.hasStatement(
					null, Toys.code, literal(code), true
			) );

			return code;

		});
	}

}
```

The slug generator assigns newly created resources a unique identifier based on their scale.

> :warning:  Real-world slug generators depending on shared state would take care that operations are synchronized
> among different transactions in order to prevent the creation of duplicate identifiers.

The *ProductsCreate.ql* SPARQL Update postprocessing script updates server-managed `toys:code` and `toys:stock`
properties after a new product is added to the catalog.

SPARQL Update postprocessing scripts are executed after the state-mutating HTTP request is successfully completed, with
some [pre-defined bindings](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j/latest/com/metreeca/rdf4j/services/Graph.html#configure(M,O,java.util.function.BiConsumer...))
like the `$this` variable holding the IRI of the targe resource either as derived from the HTTP request or as defined by
the `Location` HTTP header after a POST request.

Request and response RDF payloads may also
be [pre](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Wrapper.html#preprocessor(java.util.function.Function))
and [post](https://javadoc.io/doc/com.metreeca/metreeca-rest/latest/com/metreeca/rest/Wrapper.html#postprocessor(java.util.function.Function))
using custom filtering functions.

# Localization

Multi-lingual content retrieval is fully supported,
with [compact rendering](https://www.w3.org/TR/json-ld11/#language-indexing) for shapes including
either [`localized()`](https://javadoc.io/doc/com.metreeca/metreeca-json/latest/com/metreeca/json/shapes/Localized.html)
or  [`lang()`](https://javadoc.io/doc/com.metreeca/metreeca-json/latest/com/metreeca/json/shapes/Lang.html) constraints.

Retrieved localizations may be limited to a predefined set of language tags using a `convey` language constraint, like
for instance:

```java
convey(lang("en","it","de"))
```

Additional language constraints may be introduced at query time using the `Accept-Language` HTTP header, like for
instance:

```http request
GET http://localhost:8080/products/S18_3140
Accept-Language: en
```

# Next Steps

To complete your tour of the framework:

- walk through the [consuming tutorial](consuming-jsonld-apis.md) to learn how to interact with model-driven REST APIs to
  power client apps like the demo [online product catalog](https://demo.metreeca.com/toys/);
- explore the [framework](../#modules) to learn how to develop your own custom wrappers and handlers and to extend your
  server with additional services.
