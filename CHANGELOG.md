# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased](https://github.com/metreec/java/compare/v1.0.2...HEAD)

### Changed

- REST / Removed support for streaming operations and simplified Handler signature
- REST / Merged development server support from Packer into Publisher

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
