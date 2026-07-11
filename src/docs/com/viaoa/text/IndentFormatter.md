# com.viaoa.text.IndentFormatter

## Purpose

{@code IndentFormatter} is a custom {@link java.util.logging.Formatter} that produces indented log output to visually represent call nesting and execution flow. This formatter is designed to work with {@link java.util.logging.Logger#entering} and {@link java.util.logging.Logger#exiting}, as well as with explicit log messages that signal entry and exit semantics. Recognized entry markers include {@code "ENTRY"}, {@code "BEGIN"}, and {@code ">"}. Recognized exit markers include {@code "RETURN"}, {

## Architectural Role

IndentFormatter is a class in the text utilities area. Its invariants should be interpreted through the package role: Defines text formatting, escaping, comparison, generation, tokenizing, and sanitizing utilities.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: format.

Public/protected fields/constants reviewed: NL.

Type declaration relationship: extends SimpleFormatter.

## Invariants

### INV-INDENTFORMATTER-001: Public behavior is deterministic

**Contract**

IndentFormatter public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/text/IndentFormatter.java, methods: format

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-INDENTFORMATTER-002: Invalid inputs fail predictably

**Contract**

IndentFormatter should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/text/IndentFormatter.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## Threading and Concurrency

Unless explicitly documented or implemented as thread-safe, mutable instances should be considered single-owner or configure-before-publish. Thread-local state must be cleared or restored by the caller/service that sets it.

## Failure and Exception Behavior

Failures should be deterministic: invalid inputs should either return documented default values or throw documented exceptions without leaving partially updated shared state.

## Extension and Override Contracts

Subclasses and implementations must preserve the invariants above. Overrides should call super where the current implementation or Javadocs require event firing, state cleanup, or service delegation.

## Prohibited States or Operations

- Use current OA 4.0 runtime terminology and service boundaries.
- Do not bypass OA runtime services for identity, metadata, relationship, rule, cache, or synchronization behavior unless the type explicitly owns that concern.
- Do not mutate configure-before-publish structures concurrently with evaluation unless tests prove it is safe.

## Required Invariant Tests

- Add focused tests for each invariant listed above.
- Include representative OA model objects, Hubs, metadata, callbacks, and paths when this type participates in runtime behavior.
- Verify null, boundary, invalid, repeated, and exceptional execution paths.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/text/IndentFormatter.java`
- Package: `com.viaoa.text`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `format`.
- Fields/constants referenced by invariant review: `NL`.
- Declaration relationship: `extends SimpleFormatter`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
