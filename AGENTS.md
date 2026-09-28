# PTIT Meeting Backend - Agent Instructions

These instructions apply to the entire repository.

Before creating or changing code under `src`, read `docs/README.md` and every document
listed in its "Required reading" section. The documents in `docs/` are the source of
truth for project structure and coding conventions.

When implementing a new business module:

1. Follow the package-by-feature structure under `org.ptit.meeting.modules.<feature>`.
   Every module must contain `<feature>.constant.<Feature>ErrorConstants` for its message keys.
2. Follow the dependency, naming, error-handling and testing rules in `docs/`.
3. Inspect existing shared classes before introducing a new base class or abstraction.
4. Do not create global `domain`, `application`, `infrastructure` or `layer` packages.
5. Do not introduce `ErrorCode` or `CommonErrorCode`; use `StatusCode`, string error
   constants and `BusinessException` as documented.
6. Keep business logic in services, not controllers, repositories, mappers or utilities.
7. Run `mvn test` before considering an implementation complete.
8. Update `docs/` in the same change when a deliberate architecture convention changes.

If a user request conflicts with these conventions, follow the user request and document
the intentional exception instead of silently creating a second convention.
