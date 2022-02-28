# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unpublished](https://github.com/metreeca/base/compare/v1.0.2...HEAD)

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

- RDF / Factor RDFFormat static parsing method
- RDF4J / Upgrade to RDF4J 3.7.4

### Fixed

RDF4J / Handling of union shapes in GraphEngine: now fields are matched inside SPARQL unions




## [1.0.2+20220112](https://github.com/metreeca/base/compare/v1.0.1...v1.0.2)

This is a patch release fixing the following issues.

### Fixed

- Base / Broken version numbers in maven BOM

## [1.0.1+20211228](https://github.com/metreeca/base/compare/v1.0.1...v0.55.0)

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
