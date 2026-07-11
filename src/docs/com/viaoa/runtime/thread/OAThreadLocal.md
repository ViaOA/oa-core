# com.viaoa.runtime.thread.OAThreadLocal

## Purpose

Thread-scoped state container used internally by OA to manage execution context and operational flags on a per-thread basis. This holds lightweight, mutable metadata including: Object OA model loading and deletion state Serialization modes User/session context and admin privileges Hub event traversal depth & suppression flags Undo/calc change batching Distributed sync and process tracking Thread participation in object locking Instances are automatically created on-demand by {@link OAThreadLocal

## Architectural Role

OAThreadLocal is a class in the runtime registry and thread services area. Its invariants should be interpreted through the package role: Locates OA runtimes and owns thread-local, remote-thread, datasource, and thread execution services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa
- com.viaoa.lang
- com.viaoa.oa
- com.viaoa.oa.sibling
- com.viaoa.object
- com.viaoa.process
- com.viaoa.remote.info
- com.viaoa.serialize
- com.viaoa.session
- com.viaoa.transaction

## Public Contract

Public/protected methods reviewed: getTime, setTime, getTransaction, setTransaction, getLoading, setLoading, getCacheAddMode, setCacheAddMode, getObjectSerializers, addObjectSerializer, removeObjectSerializer, getSendSyncMessages, setSendSyncMessages, getSendSyncMessagesHold, setSendSyncMessagesHold, incStartServerOnly, decStartServerOnly, getDeleting, setDeleting, getFlags, setFlags, getLocks, setLocks, getWaitingOnLock.

Public/protected fields/constants reviewed: transaction, sendingEvent, loading, sendSyncMessages, cntStartServerOnly, sendSyncMessagesHold, oaSyncEventCount, alSiblingHelper, cntGetSiblingCalled, alHubEvent, hmModelUser, isAdmin, dontAdjustHubs, fastLoadingHub, process, hubMergerCallback.

Annotations present: SuppressWarnings.

## Invariants

### INV-OATHREADLOCAL-001: Runtime state is scoped by OA and thread where applicable

**Contract**

OAThreadLocal must keep thread-local/model-specific state separated by OA runtime instance where the API requires it.

**Rationale**

OA 4.0 supports multiple OA/model runtimes on one thread.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThreadLocal.java, OARuntime/OAThreadLocal methods

**Test implications**

Set per-OA thread-local state and verify isolation.

**Confidence**

Medium

### INV-OATHREADLOCAL-002: Invalid inputs fail predictably

**Contract**

OAThreadLocal should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThreadLocal.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Lifecycle

Lifecycle-sensitive operations should leave state valid after success, failure, cancellation, and repeated invocation. Any global, static, thread-local, listener, remote, or cache registration requires cleanup tests.

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

- Source file: `src/main/java/com/viaoa/runtime/thread/OAThreadLocal.java`
- Package: `com.viaoa.runtime.thread`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getTime`, `setTime`, `getTransaction`, `setTransaction`, `getLoading`, `setLoading`, `getCacheAddMode`, `setCacheAddMode`, `getObjectSerializers`, `addObjectSerializer`, `removeObjectSerializer`, `getSendSyncMessages`, `setSendSyncMessages`, `getSendSyncMessagesHold`, `setSendSyncMessagesHold`, `incStartServerOnly`.
- Fields/constants referenced by invariant review: `transaction`, `sendingEvent`, `loading`, `sendSyncMessages`, `cntStartServerOnly`, `sendSyncMessagesHold`, `oaSyncEventCount`, `alSiblingHelper`, `cntGetSiblingCalled`, `alHubEvent`, `hmModelUser`, `isAdmin`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
