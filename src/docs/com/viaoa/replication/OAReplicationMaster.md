# com.viaoa.replication.OAReplicationMaster

## Purpose

Replication master that receives client messages and forwards master sync messages to registered clients. The master maintains master transaction-log records, exposes remote registration/session interfaces, and processes connected client sessions for catch-up and live forwarding.

## Architectural Role

OAReplicationMaster is a class in the replication area. Its invariants should be interpreted through the package role: Coordinates transaction-log and master/client replication behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.concurrent
- com.viaoa.datetime
- com.viaoa.io

## Public Contract

Public/protected methods reviewed: start, _start, registerClient, run, stop, runProcessClients, processMessage, getLastReceivedClientSeq, getLastProcessedClientSeq, getLastReceivedMasterSeq, getLastProcessedMasterSeq, setEnabled, getEnabled, setLastReceivedMasterSeq, onClientDisconnected, onNewSyncMessage, loadTLogFile, openTLogFile, _openTLogFile, writeStreamHeader, createNewTLogFile, writeTLog, addTLog.

Public/protected fields/constants reviewed: ReplicationMasterLookupName, tlogFileName.

Annotations present: Override.

Type declaration relationship: extends OAReplicationBase.

## Invariants

### INV-OAREPLICATIONMASTER-001: Distributed calls preserve identity and ordering

**Contract**

OAReplicationMaster must preserve object/session/request identity through serialization and remote invocation.

**Rationale**

Sync/replication requires convergence and deterministic remote side effects.

**Evidence**

src/main/java/com/viaoa/replication/OAReplicationMaster.java, remote/client/server methods

**Test implications**

Simulate ordered remote events and verify client/server state convergence.

**Confidence**

Medium

### INV-OAREPLICATIONMASTER-002: Invalid inputs fail predictably

**Contract**

OAReplicationMaster should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/replication/OAReplicationMaster.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/replication/OAReplicationMaster.java`
- Package: `com.viaoa.replication`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `start`, `_start`, `registerClient`, `run`, `stop`, `runProcessClients`, `processMessage`, `getLastReceivedClientSeq`, `getLastProcessedClientSeq`, `getLastReceivedMasterSeq`, `getLastProcessedMasterSeq`, `setEnabled`, `getEnabled`, `setLastReceivedMasterSeq`, `onClientDisconnected`, `onNewSyncMessage`.
- Fields/constants referenced by invariant review: `ReplicationMasterLookupName`, `tlogFileName`.
- Declaration relationship: `extends OAReplicationBase`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
