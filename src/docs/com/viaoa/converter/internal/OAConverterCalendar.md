# com.viaoa.converter.internal.OAConverterCalendar

## Purpose

Converter for transforming values into {@link Calendar} instances and formatting {@link Calendar} values into display-friendly {@link String} representations. Conversion Rules Supported input types when converting to {@code Calendar}: {@code null} → {@code null} {@link Calendar} — returned as a defensive clone Any type supported by {@link OAConverterOADateTime} (e.g., String, {@link java.util.Date}, {@link java.time.LocalDateTime}, {@link java.time.ZonedDateTime}, etc.) Semantics: Conversions ar

## Architectural Role

OAConverterCalendar is a class in the type conversion area. Its invariants should be interpreted through the package role: Defines deterministic value conversion across Java/OA primitive, date/time, enum, and string types.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.datetime

## Public Contract

Public/protected methods reviewed: convert, convertToString.

Annotations present: Override.

Type declaration relationship: implements OAConverterInterface<Calendar>.

## Invariants

### INV-OACONVERTERCALENDAR-001: Conversions are deterministic and side-effect free

**Contract**

OAConverterCalendar must convert values without mutating input objects or global conversion state except documented caches.

**Rationale**

Conversion is used throughout property setting, query, UI, and serialization paths.

**Evidence**

src/main/java/com/viaoa/converter/internal/OAConverterCalendar.java, convert methods

**Test implications**

Convert representative values repeatedly and verify same result.

**Confidence**

Medium

### INV-OACONVERTERCALENDAR-002: Invalid inputs fail predictably

**Contract**

OAConverterCalendar should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/converter/internal/OAConverterCalendar.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/converter/internal/OAConverterCalendar.java`
- Package: `com.viaoa.converter.internal`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `convert`, `convertToString`.
- Declaration relationship: `implements OAConverterInterface<Calendar>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
