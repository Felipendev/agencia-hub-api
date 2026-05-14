---
alwaysApply: true
---

# Cursor Rules

## Agent Behavior

Act as a senior technical agent responsible for analyzing, implementing, reviewing and evolving this software project.

Prioritize:

- strict adherence to the requested scope
- respect for the real project patterns
- safe execution without assumptions
- technical clarity
- architectural consistency
- complete and usable deliveries

## Rule Precedence

When rules conflict, follow this order:

1. explicit user scope
2. architecture documentation in /docs/architecture
3. rules in /.cursor/rules.md
4. real existing project patterns
5. safety and absence of assumptions
6. best practices compatible with the current context

## General

- Always read /docs/architecture before changing code.
- Follow the documented architecture.
- Do not copy reference projects blindly.
- Use reference projects only as inspiration for structure, organization and rules.
- Do not introduce new patterns without updating architecture documentation or creating an ADR when relevant.
- Do not refactor the whole project unless explicitly requested.
- Prefer small, safe and incremental changes.
- If the task is analysis-only, do not modify files.
- If the task asks for documentation-only, do not modify production code.
- If the task asks for one feature, do not refactor unrelated features.
- Keep naming descriptive.
- Avoid unclear abbreviations.
- Avoid duplicated logic.
- Do not add extra features, unrelated refactors or files outside the requested scope.

## Backend Architecture

- Keep clear boundaries between API, Application, Domain and Infrastructure responsibilities.
- The exact package names may vary according to the project.
- Domain must not depend on HTTP, database, JPA entities, external API models or infrastructure implementations.
- Controllers must only adapt external requests and responses.
- Application services or use cases must orchestrate behavior.
- Domain entities, Value Objects or domain services must own business rules.
- Infrastructure must implement persistence, integrations and framework-specific details.
- Avoid exposing persistence entities directly as API responses.

## Domain

- Use factory methods when object creation has validation, normalization or business meaning.
- Avoid redundant names such as createNew, createNewEntity or updateEntityData.
- Prefer simple method names inside the class context, such as create, update, activate, deactivate, approve, reject or cancel.
- Use a specific reconstruction method only when the project separates new object creation from persistence reconstruction.
- Do not add reconstruction methods mechanically.
- Fields with relevant validation, formatting, normalization or business rules should become Value Objects.
- Do not create Value Objects without a real rule or benefit.
- Mutation methods must validate important preconditions before changing state.
- Mutation methods should update audit fields when they exist and are relevant.
- Mutation methods should validate final state when the entity has invariants.
- Domain entities must protect important business invariants.

## Validation

- Use self-validation only when an entity or Value Object has meaningful invariants.
- Do not add selfValidate mechanically to simple data structures.
- Do not spread domain validation across controllers or infrastructure.
- Request validation may use Bean Validation or the framework validation tool.
- Business validation belongs in the domain or domain services when appropriate.

## Time

- Do not use real current time directly in business logic when time affects behavior or test stability.
- Prefer TimeProvider, Clock or an equivalent abstraction when needed.
- Tests must use fixed time when validating date/time behavior.
- Do not compare expected dates using the real current processing time.

## Clean Code

- Avoid nested if statements.
- Prefer guard clauses.
- Extract complex conditions to private methods with clear names.
- Prefer readable code over clever code.
- Keep methods small.
- Keep classes cohesive.
- Before creating a new method, check if similar logic already exists.
- If two methods differ only by one or two parameters, consider one parameterized method with clear arguments.
- Do not add code comments unless they explain non-obvious business context, external constraints or architectural decisions.
- Do not add TODO comments unless explicitly requested.

## Mappers

- Use dedicated mappers when conversion logic becomes large, duplicated or distracts from the class responsibility.
- Do not put large conversion logic inside controllers, use cases or domain entities.
- If a request or response builder has more than 8 fields, consider creating a mapper.
- Use dedicated mappers for API, persistence and integration conversions when needed.
- Mapper method names must make origin and destination clear.
- Prefer names such as toDomain, toEntity, toResponse or fromEntity when clear.
- Use more specific names when generic names become ambiguous.
- Avoid vague names when conversion is not obvious, such as parse, convert or toDTO.

## Tests

- Unit tests should focus on business behavior and application orchestration.
- Integration tests should validate relevant framework, API and persistence behavior.
- Test builders are allowed when they improve readability.
- Test data must be clear and explicit.
- Avoid relying on real current time in assertions.
- Use fixed time when testing date/time behavior.

## Encoding

- When modifying existing files, preserve the original encoding.
- Prefer UTF-8.
- Avoid tools or commands that may corrupt accented characters.
- After batch replacements, verify that accented characters were preserved correctly.