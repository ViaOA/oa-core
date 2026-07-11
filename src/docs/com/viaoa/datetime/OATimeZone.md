# com.viaoa.datetime.OATimeZone

## Purpose

Utility for working with Java {@link TimeZone} objects, including: Building and caching all available zones sorted by UTC offset Lookup by ID, abbreviation, or formatted display strings Conversion targets for {@link OADateTime} Efficient repeated lookups via internal caching Thread-safety: Time zone lists are created once and published via safe publication. A background update mechanism exists but currently only refreshes at startup. Display formatting: Each {@code TZ} entry exposes: {@code id} 

## Architectural Role

OATimeZone is a class in the oa date/time values area. Its invariants should be interpreted through the package role: Defines OA-specific date, time, datetime, timezone, and range cache behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.lang

## Public Contract

Public/protected methods reviewed: getDisplay, getTimeZoneUTC, getLocalOATimeZone, getLocalTimeZone, getShortNames, compare, getOATimeZones, _getOATimeZones, getTimeZone, getOATimeZone, getUtcTimeZone, getTimeZoneById.

Public/protected fields/constants reviewed: TZ_Eastern, TZ_NewYork, TZ_Central, TZ_Chicago, TZ_Mountain, TZ_Phoenix, TZ_Pacific, TZ_LosAngeles, TZ_Anchorage, TZ_London, TZ_Tokyo, TZ_HongKong, TZ_GMT, TZ_Zulu, TZ_UTC, id.

Annotations present: Override.

## Invariants

### INV-OATIMEZONE-001: Date/time value semantics are immutable enough for comparison

**Contract**

Date/time wrappers must preserve intended instant/date/time fields across parsing, formatting, comparison, and serialization.

**Rationale**

Scheduling, filters, converters, and datasource mapping rely on stable value semantics.

**Evidence**

src/main/java/com/viaoa/datetime/OATimeZone.java, constructors/compare/format methods

**Test implications**

Round-trip parse/format/serialize and compare boundary dates/times.

**Confidence**

Medium

### INV-OATIMEZONE-002: Invalid inputs fail predictably

**Contract**

OATimeZone should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/datetime/OATimeZone.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/datetime/OATimeZone.java`
- Package: `com.viaoa.datetime`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getDisplay`, `getTimeZoneUTC`, `getLocalOATimeZone`, `getLocalTimeZone`, `getShortNames`, `compare`, `getOATimeZones`, `_getOATimeZones`, `getTimeZone`, `getOATimeZone`, `getUtcTimeZone`, `getTimeZoneById`.
- Fields/constants referenced by invariant review: `TZ_Eastern`, `TZ_NewYork`, `TZ_Central`, `TZ_Chicago`, `TZ_Mountain`, `TZ_Phoenix`, `TZ_Pacific`, `TZ_LosAngeles`, `TZ_Anchorage`, `TZ_London`, `TZ_Tokyo`, `TZ_HongKong`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
