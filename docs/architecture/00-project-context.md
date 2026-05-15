# Project Context

## Purpose

This project is being organized early to avoid uncontrolled growth, inconsistent patterns and duplicated logic.

The goal is to create a clear architectural baseline that can guide developers and AI agents during implementation, review and refactoring.

## Current Situation

The system already has working features, but some parts evolved without consistent structure.

The main problems to avoid are:

- duplicated logic
- inconsistent naming
- business rules spread across layers
- components or classes that work only in specific contexts
- unclear responsibility boundaries
- unsafe broad refactors
- architecture decisions without documentation

## Reference Project Usage

A reference project may exist in the workspace.

The reference project must be used only as inspiration for:

- folder organization
- separation of responsibilities
- domain-oriented structure
- validation strategy
- mapping strategy
- testing discipline
- incremental refactoring approach

The reference project must not be copied blindly.

Names, entities, methods, business flows and domain-specific classes from the reference project are not automatically part of this project.

## Working Strategy

The project must evolve incrementally.

**Refactoring sequence:** after the baseline is in place, follow **`04-incremental-refactoring-roadmap.md`** (Phases **1–5**; Phase **0** is already done in this repository — do not repeat as mandatory checklist).

Before changing production code:

1. understand the current structure;
2. identify the existing patterns;
3. compare them with the documented architecture principles;
4. propose a small and safe change;
5. apply the change only after the scope is clear;
6. run or update tests when relevant;
7. update documentation only when a real architectural decision changes.

Do not rewrite the whole project at once.

Do not introduce new patterns silently.