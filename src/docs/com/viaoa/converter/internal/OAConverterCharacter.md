# com.viaoa.converter.internal.OAConverterCharacter

## Purpose

Converter for transforming a value into a {@link Character}, and formatting {@link Character} values as {@link String} text. Conversion to {@code Character} The following input types are supported: {@link Character} — returned directly {@link String} — only if length is exactly 1 (first character used) {@link Boolean} — {@code true → 'T'}, {@code false → 'F'} {@link Number} — integer value within valid {@code char} range If the input cannot be converted, {@code null} is returned. Formatting from

## Architectural Role

OAConverterCharacter is a class in the type conversion area. Its invariants should be interpreted through the package role: Defines deterministic value conversion across Java/OA primitive, date/time, enum, and string types.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.converter
- com.viaoa.lang

## Public Contract

Public/protected methods reviewed: convert, convertToString.

Annotations present: Override.

Type declaration relationship: implements OAConverterInterface<Character>.

## Invariants

### INV-OACONVERTERCHARACTER-001: Conversions are deterministic and side-effect free

**Contract**

OAConverterCharacter must convert values without mutating input objects or global conversion state except documented caches.

**Rationale**

Conversion is used throughout property setting, query, UI, and serialization paths.

**Evidence**

src/main/java/com/viaoa/converter/internal/OAConverterCharacter.java, convert methods

**Test implications**

Convert representative values repeatedly and verify same result.

**Confidence**

Medium

### INV-OACONVERTERCHARACTER-002: Invalid inputs fail predictably

**Contract**

OAConverterCharacter should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/converter/internal/OAConverterCharacter.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/converter/internal/OAConverterCharacter.java`
- Package: `com.viaoa.converter.internal`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `convert`, `convertToString`.
- Declaration relationship: `implements OAConverterInterface<Character>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
