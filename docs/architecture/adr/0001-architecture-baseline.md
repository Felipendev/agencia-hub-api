# ADR 0001 - Architecture Baseline

## Status

Accepted

## Context

The project needs a clear architectural baseline before it grows further.

The current goal is not to copy a reference project, but to extract useful structural practices from it and adapt them to this project's own domain and constraints.

Without a documented baseline, future changes may introduce inconsistent patterns, duplicated rules and unclear responsibility boundaries.

## Decision

The project will use a layered, responsibility-oriented architecture.

The main conceptual areas are:

- API or external interface
- Application or use cases
- Domain or business rules
- Infrastructure or adapters

The exact package names may vary, but responsibility boundaries must remain clear.

Reference projects may be used as inspiration for structure and discipline, but their domain-specific names, classes and flows must not be copied automatically.

## Consequences

### Positive

- Clearer responsibility boundaries.
- Safer incremental refactoring.
- Better consistency for developers and AI agents.
- Easier testing of business rules.
- Less duplicated logic across layers.

### Negative

- Some additional structure may be needed.
- Existing code may require gradual migration.
- New patterns must be documented before becoming project conventions.
- The team must avoid copying reference implementations blindly.

## Follow-up

Detailed rules are documented in:

- `docs/architecture/01-architecture-principles.md`
- `docs/architecture/02-backend-architecture.md`
- `docs/architecture/03-domain-rules.md`
- `docs/architecture/05-package-refactoring-and-class-responsibilities.md`
- `docs/architecture/06-clean-architecture-use-case-driven.md`