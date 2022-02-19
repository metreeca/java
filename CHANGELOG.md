# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased](https://github.com/metreeca/base/compare/v1.0.2...HEAD)

### Added

- REST / Resource retrieval action
- REST / Shape-based frame validation action
- REST / JSONPath processing action
- RDF / Default text localization action
- RDF / Value normalization action
- RDF / Schema.org namespace support
- RDF4J / HTML microdata parsing action

### Improved

RDF4J / Upgraded to RDF4J 3.7.4

### Fixed

RDF4J / Handling of union shapes in GraphEngine: now fields are matched inside SPARQL unions


## [1.0.2](https://github.com/metreeca/base/compare/v1.0.1...v1.0.2)+2022-01-12

This is a patch release fixing the lowing issues.


### Fixed

- Base / Broken version numbers in maven BOM

## [1.0.1](https://github.com/metreeca/base/compare/v1.0.1...v0.55.0)+2021-12-28

This is a major release marking the initial public availability of the framework ;).

### Broken ;-(

- JSON / Simplified and generalized `Frame` API
- JSON / Restricted `Frame.focus()` to `Resource` values
- JSON / Migrated `Frame` API to focus `Shift` operators
- JSON / Reorganized and extended `Values` converter methods
- REST / Migrated `JSONLDFormat` to `Frame` payload to improve usability
- REST / Renamed `Context.asset()` to `Toolbox.service()` to avoid widespread conflicts with other concepts (RDF
  statement context, JSON-LD context, web app context, web app asset, …)
- REST / Factored configurable option mgmt to `Setup`
- REST / Merged `Engine.browse()/relate()` methods and removed `Browser` handler
- REST / Migrated `Creator` slug generator configuration fomr constructor to setter method
- REST / Factored request handling code to CRUD handlers and simplified `Engine` API
- REST / Removed transaction mgmt from `Engine` API
- REST / Migrated shape-based access control from `Engine.throttler()` to `Wrapper.keeper()`
- REST / Renamed `Gateway` wrapper to `Server`
- JSE / Merge `JSE.context(String/IRI)` setters to simplify API
- RDF4J / Simplified txn mgtm `Graph` API
- RDF4J / Migrated `Graph` SPARQL processors to `Frame`

### Added

- JSON / Extended `Frame` API with typed getters/setters
- JSON / Lay down focus `Shift` operators (predicate paths, value mappings, aggregates, …)
- REST / Added `Handler.route()/asset()` conditional handler factories
- REST / Added `Format.mime()` default MIME getter
- REST / Added `Store.has()` test methods
- REST / Added `Packer` development server proxy

### Improved

- JSON / Ignored `rdf:type` inverse links in `Frame` construction traversal
- REST / Added request IRI placeholder to `status(code, details)` response generator method
- REST / Extended the default secret `vault()` implementation to retrieve parameters also from environment variables
- REST / Extended `Query` action to handle `jar:` URLs
- REST / Reduced default items query limit to 100
- XML / Migrated HTML parsing to JSoup

### Fixed

- REST / `JSONLDFormat.query()` now correctly resolve aliased JSON-LD keywords
- REST / Hardened `Vault.get()` method against empty parameter identifiers
- Head / Hardened `JSEServer` and `JEEServer` against empty header names