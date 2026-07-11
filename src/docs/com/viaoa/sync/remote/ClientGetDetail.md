# com.viaoa.sync.remote.ClientGetDetail

## Purpose

Server-side helper used during {@code RemoteClient.getDetail(...)} operations. {@code ClientGetDetail} is responsible for constructing the OA model that should be returned to a client when it requests the value of a reference property or hub (detail) from a master object. It determines: the master object and its siblings, the requested property value, which additional related objects must be sent so that the client receives a consistent subset of the OA model, how much reference depth should be 

## Architectural Role

ClientGetDetail is a class in the real-time synchronization area. Its invariants should be interpreted through the package role: Coordinates client/server live runtime synchronization.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.compare.match
- com.viaoa.datasource
- com.viaoa.hub
- com.viaoa.oa
- com.viaoa.oa.sibling
- com.viaoa.object
- com.viaoa.performance
- com.viaoa.runtime
- com.viaoa.serialize

## Public Contract

Public/protected methods reviewed: removeGuid, addGuid, close, getDetail, getSerializedDetail, loadDataInBackground, afterSerialize, beforeSerialize, getReferenceValueToSend, shouldSerializeReference.

Annotations present: Override.

## Invariants

### INV-CLIENTGETDETAIL-001: Distributed calls preserve identity and ordering

**Contract**

ClientGetDetail must preserve object/session/request identity through serialization and remote invocation.

**Rationale**

Sync/replication requires convergence and deterministic remote side effects.

**Evidence**

src/main/java/com/viaoa/sync/remote/ClientGetDetail.java, remote/client/server methods

**Test implications**

Simulate ordered remote events and verify client/server state convergence.

**Confidence**

Medium

### INV-CLIENTGETDETAIL-002: Invalid inputs fail predictably

**Contract**

ClientGetDetail should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/sync/remote/ClientGetDetail.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/sync/remote/ClientGetDetail.java`
- Package: `com.viaoa.sync.remote`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `removeGuid`, `addGuid`, `close`, `getDetail`, `getSerializedDetail`, `loadDataInBackground`, `afterSerialize`, `beforeSerialize`, `getReferenceValueToSend`, `shouldSerializeReference`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
