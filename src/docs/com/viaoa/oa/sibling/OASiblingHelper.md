# com.viaoa.oa.sibling.OASiblingHelper

## Purpose

Learns and resolves property-paths from a root {@link Hub} so that "sibling" data can be located efficiently. As references are accessed (via {@code OAObject.getObject(...)} / {@code getHub(...)}), this helper records the traversed link steps as a small tree of nodes. Later, given an {@link OAObject} and a link/property name, it can reconstruct the property path back to the root hub. Paths are discovered in two ways: Explicitly via {@link #add(String)} using a property path starting at the hub's

## Architectural Role

OASiblingHelper is a class in the oa runtime root area. Its invariants should be interpreted through the package role: Defines the OA runtime instance, facade, and service boundary for a model package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.annotation
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.object
- com.viaoa.path

## Public Contract

Public/protected methods reviewed: getHub, setUseSameThread, getUseSameThread, add, onGetReference, getPath.

Type declaration relationship: <TYPE extends OAObject>.

## Invariants

### INV-OASIBLINGHELPER-001: Public behavior is deterministic

**Contract**

OASiblingHelper public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/oa/sibling/OASiblingHelper.java, methods: getHub, setUseSameThread, getUseSameThread, add, onGetReference, getPath

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OASIBLINGHELPER-002: Invalid inputs fail predictably

**Contract**

OASiblingHelper should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/oa/sibling/OASiblingHelper.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/oa/sibling/OASiblingHelper.java`
- Package: `com.viaoa.oa.sibling`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getHub`, `setUseSameThread`, `getUseSameThread`, `add`, `onGetReference`, `getPath`.
- Declaration relationship: `<TYPE extends OAObject>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
