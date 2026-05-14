# Domain Rules

## Purpose

Domain rules protect the business behavior of the system.

They should prevent invalid states, reduce duplicated validation and keep business logic away from transport or persistence details.

## Entities

Use entities when an object has identity and lifecycle.

Entities may contain:

- business state
- state transitions
- invariant validation
- behavior that changes internal state

Entities should not depend on:

- controllers
- HTTP
- database
- JPA-specific annotations
- external API models

## Value Objects

Use Value Objects when a field or concept has its own rules.

A field is a good Value Object candidate when it has:

- validation
- formatting
- normalization
- comparison rules
- calculation rules
- business meaning beyond a primitive type

Examples of possible Value Objects:

- Email
- Document
- PhoneNumber
- Money
- Address
- DateRange
- TimeRange
- Percentage
- Slug

These are examples only. Do not create Value Objects without a real rule or benefit.

Avoid unnecessary wrappers around simple fields that have no behavior.

## Factory Methods

Factory methods may be used when object creation requires validation, normalization or business rules.

Prefer clear factory names.

Common examples:

```java
create(...)
of(...)
from(...)
```

Use project consistency to choose the best naming convention.

Avoid redundant names such as:

```java
createNewEntity(...)
createNewUser(...)
updateEntityData(...)
```

Prefer names that are clear inside the class context:

```java
create(...)
update(...)
activate(...)
deactivate(...)
approve(...)
reject(...)
cancel(...)
```

## Reconstructing Objects From Persistence

When the project separates domain objects from persistence entities, it may be useful to have a specific creation path for rebuilding existing objects from storage.

Possible names:

```java
rebuild(...)
restore(...)
fromPersistence(...)
```

This is optional.

Do not add `rebuild` automatically if the project does not need it.

Use this pattern only when it helps distinguish:

- creating a new object with business rules;
- reconstructing an existing object already persisted.

## Mutations

Methods that mutate business state should protect invariants.

A mutation method should generally:

1. validate preconditions;
2. change state;
3. update audit fields when they exist and are relevant;
4. validate the final state when the entity has invariants;
5. emit domain events when the project uses domain events.

Examples of mutation methods:

```java
update(...)
activate(...)
deactivate(...)
approve(...)
reject(...)
cancel(...)
publish(...)
archive(...)
```

Do not force these methods into every entity.

Use them only when the domain behavior exists.

## Self Validation

A self-validation method can be used when an entity or value object has invariants that must always be true.

Possible names:

```java
selfValidate()
validate()
validateState()
```

This is optional.

Use it when:

- the object has multiple rules;
- invalid internal state would be dangerous;
- the same validation is needed after creation and mutation.

Do not add self-validation mechanically to simple data structures.

## Time Handling

Avoid using the real current time directly inside business logic when time affects behavior or tests.

Prefer an abstraction when time is part of the rule.

Possible approaches:

- TimeProvider
- Clock
- injected date/time service
- fixed Clock in tests

Use this especially when:

- updating audit fields;
- validating expiration;
- scheduling;
- calculating deadlines;
- comparing dates in tests.

Tests should avoid assertions based on `now()` at execution time.

Prefer fixed time in tests.

## Validation

Separate input validation from business validation.

API request validation may use Bean Validation or equivalent tools.

Business validation belongs in the domain or in domain services when appropriate.

The controller should not decide business validity.

The application layer may coordinate the action, but the domain should protect important invariants.

## Avoid Nested Conditionals

Avoid deeply nested `if` statements.

Prefer:

- guard clauses
- extracted methods with clear names
- value objects
- strategy pattern when behavior varies
- state pattern when lifecycle becomes complex

Example:

```java
if (!entity.canBeApproved()) {
    throw new DomainException("Entity cannot be approved.");
}

entity.approve();
```

## Mappers

Use mappers when conversion logic becomes large, duplicated or distracts from the responsibility of the class.

Create dedicated mappers for conversions between:

- API requests and application commands
- application results and API responses
- domain objects and persistence entities
- external integration models and internal models

As a practical rule, if a builder has more than 8 fields, consider extracting it to a mapper.

Do not force mappers for trivial one-line conversions.

Mapper method names should make the conversion clear.

Examples:

```java
toDomain(...)
toEntity(...)
toResponse(...)
fromEntity(...)
```

Use more specific names when generic names become ambiguous.

Avoid vague names when the conversion is not obvious:

```java
parse(...)
convert(...)
toDTO(...)
```

## Duplication

Before creating a new method, check whether similar logic already exists.

If two methods differ only by one or two values, consider parameterizing the difference.

Avoid duplicated business rules across:

- controllers
- use cases
- entities
- validators
- mappers
- tests

## Comments

Do not add comments to explain obvious code.

Prefer clear names and small methods.

Comments are acceptable when they explain non-obvious business context, external constraints or architectural decisions.

Do not add TODO comments unless explicitly requested.