[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/http.svg)](https://central.sonatype.com/artifact/com.metreeca/http/3.0.0/versions)

# Metreeca/HTTP

Metreeca/HTTP is a lightweight Java framework for rapid REST service development.

# Documentation
- **[Tutorials](tutorials/index.md)**
- **[How-To](how-to/index.md)**

# Modules

|              area | javadocs                                                     | description                                   |
| ----------------: | :----------------------------------------------------------- | :-------------------------------------------- |
|         framework | [http‑core](https://javadoc.io/doc/com.metreeca/http-core)   | HTTP processing framework                     |
|       data codecs | [http-json](https://javadoc.io/doc/com.metreeca/http-json)   | JSON codecs and utilities                     |
|                   | [http-jsonld](https://javadoc.io/doc/com.metreeca/http-jsonld) | JSON-LD codecs and model-driven REST handlers |
|                   | [http‑xml](https://javadoc.io/doc/com.metreeca/http-xml)     | XML/HTML codecs and utilities                 |
|                   | [http‑rdf](https://javadoc.io/doc/com.metreeca/http-rdf)     | RDF codecs and utilities                      |
|                   | [http‑csv](https://javadoc.io/doc/com.metreeca/http-csv)     | CSV codecs and utilities                      |
|                   | [http‑ical](https://javadoc.io/doc/com.metreeca/http-ical)   | iCalendar codecs and utilities                |
| server connectors | [http‑jse](https://javadoc.io/doc/com.metreeca/http-jse)     | Java SE HTTP server connector                 |
|                   | [http‑jee](https://javadoc.io/doc/com.metreeca/http-jee)     | Java EE Servlet 3.1 containers connector      |
|   data connectors | [http‑open](https://javadoc.io/doc/com.metreeca/http-open)   | linked open data connector                    |
|                   | [http‑rdf4j](https://javadoc.io/doc/com.metreeca/http-rdf4j) | RDF4J-based SPARQL repository connector       |
|                   | [http‑gcp](https://javadoc.io/doc/com.metreeca/http-gcp)     | Google Cloud services connector               |

# Getting Started

1. Add the framework to your Maven configuration

```xml 
<project>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}</artifactId>
                <version>${project.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency> <!-- server connector -->
            <groupId>com.metreeca</groupId>
            <artifactId>http-jse</artifactId>
        </dependency>

        <dependency> <!-- storage connector -->
            <groupId>com.metreeca</groupId>
            <artifactId>http-rdf4j</artifactId>
        </dependency>

    </dependencies>

</project>
```

2. Write your first server and launch it

```java
import com.metreeca.http.jse.JSEServer;

import static com.metreeca.link.Response.OK;
import static com.metreeca.link._formats.TextFormat.text;

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

4. Delve into the [docs](https://metreeca.github.io/http/) to learn how
   to [publish](http://metreeca.github.io/http/tutorials/publishing-jsonld-apis)
   and [consume](https://metreeca.github.io/http/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD
   APIs…

# Support

- open an [issue](https://github.com/metreeca/http/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/http/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/http/blob/main/LICENSE)
file for details.
