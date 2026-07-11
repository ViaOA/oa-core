# com.viaoa.filter.OAEqualFilter

## Purpose

Filter that evaluates equality between a property value and a comparison value. Supports multiple comparison modes, including: direct object equality, string equality (with optional ignore-case), decimal-place comparison for floating-point values, Hub membership when the property value is a {@link Hub}. A property path may be supplied to read nested values, and if the path traverses a multi-valued reference, an {@link OAFinder} is generated so that the comparison is applied to the located target

## Architectural Role

OAEqualFilter is a class in the filtering predicates area. Its invariants should be interpreted through the package role: Defines reusable predicates for values, paths, selects, and composite conditions.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.compare
- com.viaoa.filter.OAFilterDelegate
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.object
- com.viaoa.path
- com.viaoa.reflect

## Public Contract

Public/protected methods reviewed: setIgnoreCase, setDeciPlaces, getDeciPlaces, isUsed, getPropertyValue.

Annotations present: Override.

Type declaration relationship: implements OAFilter.

## Invariants

### INV-OAEQUALFILTER-001: Filter result depends only on configured predicate state and input

**Contract**

Filter implementations should not mutate the tested object or hidden global state during evaluation.

**Rationale**

Filters are reused by Hubs, queries, selects, and rules.

**Evidence**

src/main/java/com/viaoa/filter/OAEqualFilter.java, isUsed/isMatch methods

**Test implications**

Run filter twice against same input and verify same result and no mutation.

**Confidence**

Medium

### INV-OAEQUALFILTER-002: Invalid inputs fail predictably

**Contract**

OAEqualFilter should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/filter/OAEqualFilter.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/filter/OAEqualFilter.java`
- Package: `com.viaoa.filter`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setIgnoreCase`, `setDeciPlaces`, `getDeciPlaces`, `isUsed`, `getPropertyValue`.
- Declaration relationship: `implements OAFilter`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
