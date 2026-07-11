# com.viaoa.datasource.objectcache.OADataSourceObjectCache

## Purpose

In-memory implementation of {@link com.viaoa.datasource.OADataSource} backed by the OA object cache. {@code OADataSourceObjectCache} allows all OAObjects of each class to be stored, queried, and serialized directly in memory without a database. It is primarily used for testing, client-side caching, or fully in-memory applications. Features Thread-safe storage using {@link java.util.concurrent.ConcurrentHashMap} and {@link java.util.concurrent.locks.ReentrantReadWriteLock}. Supports {@link com.vi

## Architectural Role

OADataSourceObjectCache is a class in the datasource abstraction area. Its invariants should be interpreted through the package role: Defines persistence/select/save/delete contracts independent of specific storage implementations.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa
- com.viaoa.callback
- com.viaoa.comm.io
- com.viaoa.compare
- com.viaoa.datasource.autonumber
- com.viaoa.filter
- com.viaoa.find

## Public Contract

Public/protected methods reviewed: select, isUsed, selectPassthru, getSupportsPreCount, insert, insertWithoutReferences, saveToStorageFile, _saveToStorageFile, updateObject, loadFromStorageFile, resolveObject, _loadFromStorageFile.

Annotations present: Override.

Type declaration relationship: extends OADataSourceAuto.

## Invariants

### INV-OADATASOURCEOBJECTCACHE-001: Cache lookup is class/key scoped

**Contract**

Cache/index lookups must not return objects for the wrong OAObject class or key.

**Rationale**

Canonical object identity depends on class-scoped cache keys.

**Evidence**

src/main/java/com/viaoa/datasource/objectcache/OADataSourceObjectCache.java, cache/index methods

**Test implications**

Cache two classes with same key values and verify distinct lookups.

**Confidence**

Medium

### INV-OADATASOURCEOBJECTCACHE-002: Invalid inputs fail predictably

**Contract**

OADataSourceObjectCache should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/datasource/objectcache/OADataSourceObjectCache.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/datasource/objectcache/OADataSourceObjectCache.java`
- Package: `com.viaoa.datasource.objectcache`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `select`, `isUsed`, `selectPassthru`, `getSupportsPreCount`, `insert`, `insertWithoutReferences`, `saveToStorageFile`, `_saveToStorageFile`, `updateObject`, `loadFromStorageFile`, `resolveObject`, `_loadFromStorageFile`.
- Declaration relationship: `extends OADataSourceAuto`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
