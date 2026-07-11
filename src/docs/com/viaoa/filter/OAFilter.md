# com.viaoa.filter.OAFilter

## Purpose

Functional filtering interface used throughout OA to evaluate whether an object should be included in a collection, Hub, or query result. An {@code OAFilter} provides a lightweight, deterministic predicate that can be applied locally in memory or, when possible, pushed down into a datasource during query execution. Typical usage includes: Filtering the contents of a {@code Hub} or derived Hub. Constraining results returned by an {@link com.viaoa.select.OASelect}. Filtering child collections in m

## Architectural Role

OAFilter is a interface in the filtering predicates area. Its invariants should be interpreted through the package role: Defines reusable predicates for values, paths, selects, and composite conditions.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.object
- com.viaoa.select

## Public Contract

Annotations present: FunctionalInterface.

## Invariants

### INV-OAFILTER-001: Filter result depends only on configured predicate state and input

**Contract**

Filter implementations should not mutate the tested object or hidden global state during evaluation.

**Rationale**

Filters are reused by Hubs, queries, selects, and rules.

**Evidence**

src/main/java/com/viaoa/filter/OAFilter.java, isUsed/isMatch methods

**Test implications**

Run filter twice against same input and verify same result and no mutation.

**Confidence**

Medium

### INV-OAFILTER-002: Invalid inputs fail predictably

**Contract**

OAFilter should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/filter/OAFilter.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/filter/OAFilter.java`
- Package: `com.viaoa.filter`
- Type kind: `interface`

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
