# com.viaoa.filter.OAEqualPathFilter

## Purpose

Filter that compares the values of two {@link OAPath} expressions on the same object (or located target object) for equality. Both property paths are resolved and the resulting values are compared using standard OA equality rules. This filter supports deep property traversal: if either property path crosses a many-relationship, an {@link OAFinder} is automatically created to locate the referenced object before evaluating equality. Typical usage: verify that two properties on an object match each

## Architectural Role

OAEqualPathFilter is a class in the filtering predicates area. Its invariants should be interpreted through the package role: Defines reusable predicates for values, paths, selects, and composite conditions.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.compare
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.object
- com.viaoa.path
- com.viaoa.runtime
- com.viaoa.select

## Public Contract

Public/protected methods reviewed: setup, getPath, isUsed, updateSelect.

Annotations present: Override.

Type declaration relationship: implements OAFilter.

## Invariants

### INV-OAEQUALPATHFILTER-001: Compiled path state matches path string and root class

**Contract**

OAEqualPathFilter must resolve property/link/filter segments consistently for its root context.

**Rationale**

Incorrect compiled path state leads to wrong values, wrong permissions, or datasource query errors.

**Evidence**

src/main/java/com/viaoa/filter/OAEqualPathFilter.java, setup/value methods

**Test implications**

Resolve simple, nested, Hub, null-intermediate, and invalid paths.

**Confidence**

Medium

### INV-OAEQUALPATHFILTER-002: Traversal handles null intermediates deterministically

**Contract**

Path evaluation should return null or documented default behavior for null roots/intermediates rather than corrupting state.

**Rationale**

Generated UI, filters, and rules evaluate paths frequently against partial object graphs.

**Evidence**

setup, getPath, isUsed, updateSelect

**Test implications**

Evaluate nested paths where each intermediate is null.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Identity Rules

Identity must be scoped by the relevant OA concept: OA runtime, object class, OAObject key/GUID, Hub instance, path root type, remote request, or datasource key. Cross-scope identity leakage should be treated as a defect.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

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

- Source file: `src/main/java/com/viaoa/filter/OAEqualPathFilter.java`
- Package: `com.viaoa.filter`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setup`, `getPath`, `isUsed`, `updateSelect`.
- Declaration relationship: `implements OAFilter`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
