# com.viaoa.runtime.OAThreadLocalService

## Purpose

Central service for OA thread-local execution state. This delegate wraps a thread-local {@link OAThreadLocal} instance and coordinates access to: Object OA model loading and refresh mode indicators Distributed sync and remote invocation state Cache add mode, serialization mode and message suppression Object delete state tracking Undoable property change capture Hub event traversal, dependency resolution & batching Deadlock-aware fine-grained locking coordination OAContext propagation Provides hi

## Architectural Role

OAThreadLocalService is a class in the runtime registry and thread services area. Its invariants should be interpreted through the package role: Locates OA runtimes and owns thread-local, remote-thread, datasource, and thread execution services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.datetime
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.oa
- com.viaoa.oa.sibling
- com.viaoa.object
- com.viaoa.process

## Public Contract

Public/protected methods reviewed: getThreadLocal, clear, setTransaction, getTransaction, isLoading, setLoading, getObjectCacheAddMode, setObjectCacheAddMode, getCurrentObjectSerializer, getObjectSerializers, addObjectSerializer, removeObjectSerializer, getSendSyncMessages, setSendSyncMessages, startServerOnly, endServerOnly, isDeleting, isThreadDeleting, setDeleting, isFlag, setFlag, removeFlag, lock, hasLock.

Public/protected fields/constants reviewed: hmLock, rwLock, openLockCnt, lockCnt, unlockCnt, cntDeadlock, MaxEventDepth.

## Invariants

### INV-OATHREADLOCALSERVICE-001: Service delegates preserve runtime ownership

**Contract**

OAThreadLocalService must operate on OAObject/Hub state through the owning OA runtime and must not create unrelated global ownership.

**Rationale**

OA 4.0 service classes are runtime-scoped coordination points; hidden global state would break multi-model execution.

**Evidence**

src/main/java/com/viaoa/runtime/OAThreadLocalService.java, public/protected service methods

**Test implications**

Use two OA runtimes or model classes and verify service state does not leak.

**Confidence**

Medium

### INV-OATHREADLOCALSERVICE-002: Null and class context are explicit

**Contract**

Service entry points must either reject null context deterministically or derive class/object/Hub context using documented runtime rules.

**Rationale**

Rules, metadata, and datasource operations need stable class resolution.

**Evidence**

getThreadLocal, clear, setTransaction, getTransaction, isLoading, setLoading, getObjectCacheAddMode, setObjectCacheAddMode

**Test implications**

Call public methods with null/missing Hub/object context and verify deterministic behavior.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Lifecycle

Lifecycle-sensitive operations should leave state valid after success, failure, cancellation, and repeated invocation. Any global, static, thread-local, listener, remote, or cache registration requires cleanup tests.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

## Threading and Concurrency

Unless explicitly documented or implemented as thread-safe, mutable instances should be considered single-owner or configure-before-publish. Thread-local state must be cleared or restored by the caller/service that sets it.

## Event and Callback Ordering

Callbacks/listeners/events should run in the order defined by the owning service or Hub. Later stages may override earlier responses only where the rule contract explicitly allows it.

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

- Source file: `src/main/java/com/viaoa/runtime/OAThreadLocalService.java`
- Package: `com.viaoa.runtime`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getThreadLocal`, `clear`, `setTransaction`, `getTransaction`, `isLoading`, `setLoading`, `getObjectCacheAddMode`, `setObjectCacheAddMode`, `getCurrentObjectSerializer`, `getObjectSerializers`, `addObjectSerializer`, `removeObjectSerializer`, `getSendSyncMessages`, `setSendSyncMessages`, `startServerOnly`, `endServerOnly`.
- Fields/constants referenced by invariant review: `hmLock`, `rwLock`, `openLockCnt`, `lockCnt`, `unlockCnt`, `cntDeadlock`, `MaxEventDepth`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
