# com.viaoa.remote.multiplexer.io.RemoteObjectInputStream

## Purpose

Customized {@link ObjectInputStream} used by OA's remoting system. This implementation eliminates the standard Java stream header and replaces Java's class-descriptor mechanism with a compact ID-based protocol. Features: Reads class descriptors using small integer IDs shared between client and server. Caches known {@link ObjectStreamClass} instances in a {@link java.util.concurrent.ConcurrentHashMap}. Allows nested streams (e.g., compressed or embedded remote objects) to reuse the parent class-d

## Architectural Role

RemoteObjectInputStream is a class in the remote invocation area. Its invariants should be interpreted through the package role: Defines remote call metadata, streams, interfaces, and multiplexer-backed invocation.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: readStreamHeader, readClassDescriptor, readAsciiString.

Annotations present: Override.

Type declaration relationship: extends ObjectInputStream.

## Invariants

### INV-REMOTEOBJECTINPUTSTREAM-001: Distributed calls preserve identity and ordering

**Contract**

RemoteObjectInputStream must preserve object/session/request identity through serialization and remote invocation.

**Rationale**

Sync/replication requires convergence and deterministic remote side effects.

**Evidence**

src/main/java/com/viaoa/remote/multiplexer/io/RemoteObjectInputStream.java, remote/client/server methods

**Test implications**

Simulate ordered remote events and verify client/server state convergence.

**Confidence**

Medium

### INV-REMOTEOBJECTINPUTSTREAM-002: Invalid inputs fail predictably

**Contract**

RemoteObjectInputStream should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/remote/multiplexer/io/RemoteObjectInputStream.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/remote/multiplexer/io/RemoteObjectInputStream.java`
- Package: `com.viaoa.remote.multiplexer.io`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `readStreamHeader`, `readClassDescriptor`, `readAsciiString`.
- Declaration relationship: `extends ObjectInputStream`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
