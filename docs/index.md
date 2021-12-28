---
title: Overview nav_order: 1
---

Metreeca/Base is modular Java framework for rapid model-driven REST/JSON-LD server development.

Its engines automatically convert high-level declarative JSON-LD models into extended REST APIs supporting CRUD
operations, faceted search, data validation and fine‑grained role‑based access control, relieving backend developers from
low-level chores and completely shielding frontend developers from linked data technicalities.

While collectively providing a floor-to-ceiling JSON-LD server development solution, its modules are loosely coupled and
may be easily plugged as a specialized component into your framework of choice.

|    area | javadocs                                                     | description                             |
| ------: | :----------------------------------------------------------- | :-------------------------------------- |
|    core | [metreeca‑json](https://javadoc.io/doc/com.metreeca/metreeca-json) | shape-based JSON modelling framework    |
|         | [metreeca‑rest](https://javadoc.io/doc/com.metreeca/metreeca-rest) | model-driven REST publishing framework  |
|    data | [metreeca‑xml](https://javadoc.io/doc/com.metreeca/metreeca-xml) | XML/HTML codecs and utilities           |
|         | [metreeca‑rdf](https://javadoc.io/doc/com.metreeca/metreeca-rdf) | RDF codecs and utilities                |
|  server | [metreeca‑jse](https://javadoc.io/doc/com.metreeca/metreeca-jse) | Java SE HTTP server connector          |
|         | [metreeca‑jee](https://javadoc.io/doc/com.metreeca/metreeca-jee) | Servlet 3.1 containers connector        |
| storage | [metreeca‑rdf4j](https://javadoc.io/doc/com.metreeca/metreeca-rdf4j) | RDF4J-based SPARQL repository connector |