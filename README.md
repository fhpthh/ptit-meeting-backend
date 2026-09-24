# PTIT Meeting Backend

Spring Boot backend for the PTIT meeting system.

Architecture and implementation conventions are documented in [docs/README.md](docs/README.md).
Developers and AI agents must read these documents before adding a new module.

## Source structure

The project uses the same lightweight package-by-feature structure as the QLCC backend:

```text
org.ptit.meeting
|-- common
|   |-- base
|   |-- constant
|   |-- dto
|   |   |-- request
|   |   `-- response
|   `-- wrapper
|-- config
|-- exception
|-- integration
|-- modules
|   `-- meeting
|       |-- constant
|       |-- controller
|       |-- dto
|       |   |-- request
|       |   `-- response
|       |-- entity
|       |-- mapper
|       |-- repository
|       `-- service
|           `-- impl
`-- utils
```

## Module convention

Create each business feature under `modules/<feature>` and keep its controller, service,
repository, entity, mapper and DTOs together. Shared technical code belongs in `common`,
global Spring configuration in `config`, and external-system adapters in `integration`.

- Entities extend `BaseEntity` when the standard ID and audit fields are suitable.
- Controllers return `BaseResponse` and use `PageResponseDTO` for paginated data.
- Business errors use `BusinessException` with `StatusCode` and a feature-specific message key constant.
- Service interfaces live in `service`; implementations live in `service.impl`.
- Feature code must not be added back under global `domain`, `application` or `infrastructure` packages.

## Run locally

```powershell
mvn spring-boot:run
```
