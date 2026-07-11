# com.viaoa.datetime.OATime

## Purpose

Time-only OA value. OATime is a specialization of {@link OADateTime} whose date portion is always normalized to {@code 1970-01-01}. It represents a time of day rather than a full date/time or a precise business timestamp. Semantics OATime uses {@link DateTimeType#Floating} semantics. The business meaning is the local time-of-day. Internally, the underlying epoch-millisecond value is derived from {@code 1970-01-01} plus the time fields in the active/default OA zone. Timezone behavior A timezone c

## Architectural Role

OATime is a class in the oa date/time values area. Its invariants should be interpreted through the package role: Defines OA-specific date, time, datetime, timezone, and range cache behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.lang

## Public Contract

Public/protected methods reviewed: createUtil, timeValue, valueOf, parseTime, setGlobalOutputFormat, getGlobalOutputFormat, addGlobalParseFormat, removeGlobalParseFormat, toString.

Public/protected fields/constants reviewed: Format1, Format2, Format3, Format4, Format5, Format6, timeOutputFormat, JsonFormat, JsonFormatTZ, JdbcFormat.

Annotations present: Override.

Type declaration relationship: extends OADateTime.

## Invariants

### INV-OATIME-001: Date/time value semantics are immutable enough for comparison

**Contract**

Date/time wrappers must preserve intended instant/date/time fields across parsing, formatting, comparison, and serialization.

**Rationale**

Scheduling, filters, converters, and datasource mapping rely on stable value semantics.

**Evidence**

src/main/java/com/viaoa/datetime/OATime.java, constructors/compare/format methods

**Test implications**

Round-trip parse/format/serialize and compare boundary dates/times.

**Confidence**

Medium

### INV-OATIME-002: Invalid inputs fail predictably

**Contract**

OATime should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/datetime/OATime.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/datetime/OATime.java`
- Package: `com.viaoa.datetime`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `createUtil`, `timeValue`, `valueOf`, `parseTime`, `setGlobalOutputFormat`, `getGlobalOutputFormat`, `addGlobalParseFormat`, `removeGlobalParseFormat`, `toString`.
- Fields/constants referenced by invariant review: `Format1`, `Format2`, `Format3`, `Format4`, `Format5`, `Format6`, `timeOutputFormat`, `JsonFormat`, `JsonFormatTZ`, `JdbcFormat`.
- Declaration relationship: `extends OADateTime`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
