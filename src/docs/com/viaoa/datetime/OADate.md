# com.viaoa.datetime.OADate

## Purpose

Date-only OA value. OADate is a specialization of {@link OADateTime} whose time portion is always normalized to {@code 00:00:00.000}. It represents a calendar date rather than a precise timestamp. Semantics OADate always uses {@link DateTimeType#Floating} semantics. The business meaning of an OADate is the local calendar date. During creation or deserialization, the local date fields are resolved using the effective OA zone and converted into a canonical internal epoch-millisecond value inherite

## Architectural Role

OADate is a class in the oa date/time values area. Its invariants should be interpreted through the package role: Defines OA-specific date, time, datetime, timezone, and range cache behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.datetime.OADateTime

## Public Contract

Public/protected methods reviewed: setGlobalOutputFormat, getGlobalOutputFormat, addGlobalParseFormat, removeGlobalParseFormat, setLocale, toString, dateValue, createUtil, valueOf.

Public/protected fields/constants reviewed: Format1, Format2, Format3, Format4, Format5, Format6, JdbcFormat, JsonFormat, dateOutputFormat.

Annotations present: Override.

Type declaration relationship: extends OADateTime.

## Invariants

### INV-OADATE-001: Date/time value semantics are immutable enough for comparison

**Contract**

Date/time wrappers must preserve intended instant/date/time fields across parsing, formatting, comparison, and serialization.

**Rationale**

Scheduling, filters, converters, and datasource mapping rely on stable value semantics.

**Evidence**

src/main/java/com/viaoa/datetime/OADate.java, constructors/compare/format methods

**Test implications**

Round-trip parse/format/serialize and compare boundary dates/times.

**Confidence**

Medium

### INV-OADATE-002: Invalid inputs fail predictably

**Contract**

OADate should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/datetime/OADate.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/datetime/OADate.java`
- Package: `com.viaoa.datetime`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setGlobalOutputFormat`, `getGlobalOutputFormat`, `addGlobalParseFormat`, `removeGlobalParseFormat`, `setLocale`, `toString`, `dateValue`, `createUtil`, `valueOf`.
- Fields/constants referenced by invariant review: `Format1`, `Format2`, `Format3`, `Format4`, `Format5`, `Format6`, `JdbcFormat`, `JsonFormat`, `dateOutputFormat`.
- Declaration relationship: `extends OADateTime`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
