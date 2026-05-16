# Architecture Principles

## Main Principle

Architecture exists to make the system easier to understand, change, test and evolve.

The project must prefer clear boundaries, explicit responsibilities and incremental evolution.

## Reference Projects

Reference projects are examples, not templates to be copied blindly.

Use reference projects to extract:

- organizational patterns
- naming discipline
- responsibility separation
- testing strategies
- validation strategies
- mapping strategies
- architecture boundaries

Do not copy:

- domain names
- business flows
- entity names
- method names
- package names
- implementation details that do not fit this project

## Incremental Evolution

Prefer small and safe changes.

Avoid broad rewrites unless explicitly approved.

When refactoring, migrate one feature, module or flow at a time.

A migrated feature can become the internal reference for the next migrations.

## Responsibility Boundaries

Each part of the system must have a clear responsibility.

Avoid mixing:

- HTTP concerns with business rules
- persistence concerns with domain rules
- validation rules with transport DTOs when they are business-specific
- mapping logic with orchestration logic
- infrastructure details with domain behavior

## Pattern Introduction

Do not introduce a new pattern just because it is generally considered good.

Introduce a pattern only when it solves a real problem in this project.

When a new architectural pattern is introduced, document why it exists and where it should be used.

## Naming

Names must describe intent clearly.

Avoid redundant names when the surrounding context already explains the subject.

Avoid abbreviations that reduce readability.

Prefer consistency over personal preference.

## Documentation

Documentation must be practical.

Avoid documenting generic theory.

Document:

- decisions
- project-specific conventions
- recurring patterns
- architecture boundaries
- examples that help future changes

Avoid excessive repetition between documents.