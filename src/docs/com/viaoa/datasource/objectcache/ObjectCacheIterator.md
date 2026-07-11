# com.viaoa.datasource.objectcache.ObjectCacheIterator

## Purpose

Iterator over objects stored in the OA object cache. {@code ObjectCacheIterator} retrieves batches of objects from {@link com.viaoa.object.OAObjectCacheDelegate} that match a given {@link com.viaoa.filter.OAFilter}. It is used by {@link OADataSourceObjectCache#select} to perform in-memory queries. Features Fetches objects incrementally (default batch = 100). Applies optional filter predicates during iteration. Supports {@link #setMax(int)} to cap the number of returned objects. Thread-safe for s

## Architectural Role

ObjectCacheIterator is a class in the datasource abstraction area. Its invariants should be interpreted through the package role: Defines persistence/select/save/delete contracts independent of specific storage implementations.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.datasource
- com.viaoa.filter
- com.viaoa.oa
- com.viaoa.oa.service.object
- com.viaoa.object
- com.viaoa.runtime

## Public Contract

Public/protected methods reviewed: next, _next, hasNext, setMax, getMax.

Public/protected fields/constants reviewed: clazz, filter, lastFetchObject, alFetchObjects, posFetchObjects, bFetchIsDone, max.

Type declaration relationship: <T extends OAObject> implements OADataSourceIterator.

## Invariants

### INV-OBJECTCACHEITERATOR-001: Cache lookup is class/key scoped

**Contract**

Cache/index lookups must not return objects for the wrong OAObject class or key.

**Rationale**

Canonical object identity depends on class-scoped cache keys.

**Evidence**

src/main/java/com/viaoa/datasource/objectcache/ObjectCacheIterator.java, cache/index methods

**Test implications**

Cache two classes with same key values and verify distinct lookups.

**Confidence**

Medium

### INV-OBJECTCACHEITERATOR-002: Invalid inputs fail predictably

**Contract**

ObjectCacheIterator should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/datasource/objectcache/ObjectCacheIterator.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Lifecycle

Lifecycle-sensitive operations should leave state valid after success, failure, cancellation, and repeated invocation. Any global, static, thread-local, listener, remote, or cache registration requires cleanup tests.

## Identity Rules

Identity must be scoped by the relevant OA concept: OA runtime, object class, OAObject key/GUID, Hub instance, path root type, remote request, or datasource key. Cross-scope identity leakage should be treated as a defect.

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

- Source file: `src/main/java/com/viaoa/datasource/objectcache/ObjectCacheIterator.java`
- Package: `com.viaoa.datasource.objectcache`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `next`, `_next`, `hasNext`, `setMax`, `getMax`.
- Fields/constants referenced by invariant review: `clazz`, `filter`, `lastFetchObject`, `alFetchObjects`, `posFetchObjects`, `bFetchIsDone`, `max`.
- Declaration relationship: `<T extends OAObject> implements OADataSourceIterator`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
