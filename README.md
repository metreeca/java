[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/metreeca-base.svg)](https://search.maven.org/artifact/com.metreeca/metreeca-base/)

# Metreeca/Base

Metreeca/Base is modular Java framework for rapid model-driven REST/JSON-LD server development.

Its engines automatically convert high-level declarative JSON-LD models into extended REST APIs supporting CRUD
operations, faceted search, data validation and fine‑grained role‑based access control, relieving backend developers from
low-level chores and completely shielding frontend developers from linked data technicalities.

While collectively providing a floor-to-ceiling JSON-LD server development solution, its modules are loosely coupled and
may be easily plugged as a specialized component into your framework of choice.

# Documentation

- **[Tutorials](https://metreeca.github.io/base/tutorials/)**
- **[How-To](https://metreeca.github.io/base/how-to/)**
- **[Reference](https://metreeca.github.io/base/reference/)**

# Modules

|    area | javadocs                                                     | description                             |
| ------: | :----------------------------------------------------------- | :-------------------------------------- |
|    core | [metreeca‑json](https://javadoc.io/doc/com.metreeca/metreeca-json) | shape-based JSON modelling framework    |
|         | [metreeca‑rest](https://javadoc.io/doc/com.metreeca/metreeca-rest) | model-driven REST publishing framework  |
|    data | [metreeca‑xml](https://javadoc.io/doc/com.metreeca/metreeca-xml) | XML/HTML codecs and utilities           |
|         | [metreeca‑rdf](https://javadoc.io/doc/com.metreeca/metreeca-rdf) | RDF codecs and utilities                |
|  server | [metreeca‑jse](https://javadoc.io/doc/com.metreeca/metreeca-jse) | Java SE HTTP server connector          |
|         | [metreeca‑jee](https://javadoc.io/doc/com.metreeca/metreeca-jee) | Servlet 3.1 containers connector        |
| storage | [metreeca‑rdf4j](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j) | RDF4J-based SPARQL repository connector |

# Getting Started

1. Add the framework to your Maven configuration

```xml

<project>

	<dependencyManagement>
		<dependencies>

       <dependency>
          <groupId>${project.group}</groupId>
          <artifactId>metreeca-base</artifactId>
          <version>1.0.2</version>
          <type>pom</type>
				<scope>import</scope>
			</dependency>

		</dependencies>
	</dependencyManagement>

	<dependencies>

		<dependency> <!-- server connector -->
			<groupId>com.metreeca</groupId>
			<artifactId>metreeca-jse</artifactId>
		</dependency>

		<dependency> <!-- storage connector -->
			<groupId>com.metreeca</groupId>
			<artifactId>metreeca-rdf4j</artifactId>
		</dependency>

	</dependencies>

</project>
```

2. Write your first server and launch it

```java
import com.metreeca.jse.JSEServer;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.TextFormat.text;

public final class Hello {

	public static void main(final String... args) {
		new JSEServer()

				.delegate(context -> request ->
						request.reply(response -> response
								.status(OK)
								.body(text(), "Hello world!")
						)
				)

				.start();
	}

}
```

3. Access you API

```shell
% curl -i http://localhost:8080/

HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 12

Hello world!
```

4. Delve into the the [docs](https://metreeca.github.io/base/) to learn how
   to [publish](http://metreeca.github.io/base/tutorials/publishing-jsonld-apis)
   and [consume](https://metreeca.github.io/base/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD
   APIs…

# Support

- open an [issue](https://github.com/metreeca/base/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/base/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/base/blob/main/LICENSE)
file for details.

