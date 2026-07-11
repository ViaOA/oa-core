# com.viaoa.comm.multiplexer.io.VirtualSocket

## Purpose

A logical socket connection that is multiplexed over a single physical TCP connection. All read and write operations on a VirtualSocket are delegated through the owning {@link MultiplexerSocketController}, allowing many independent channels to share the same underlying real socket. Each VirtualSocket behaves like a normal {@link Socket}: it exposes input/output streams, supports blocking reads, and maintains independent close and timeout behavior. The multiplexer assigns each virtual channel a u

## Architectural Role

VirtualSocket is a class in the io area. Its invariants should be interpreted through the package role: Provides OA runtime support types for the com.viaoa.comm.multiplexer.io package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: createInputStream, read, getInputStream, createOutputStream, write, getOutputStream, getConnectionId, getId, getServerSocketName, close, setTimeoutSeconds, getTimeoutSeconds.

Public/protected fields/constants reviewed: _connectionId, _id, _serverSocketName, _lockObject.

Annotations present: Override.

Type declaration relationship: extends Socket.

## Invariants

### INV-VIRTUALSOCKET-001: Public behavior is deterministic

**Contract**

VirtualSocket public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/comm/multiplexer/io/VirtualSocket.java, methods: createInputStream, read, getInputStream, createOutputStream, write, getOutputStream, getConnectionId, getId, getServerSocketName, close

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-VIRTUALSOCKET-002: Invalid inputs fail predictably

**Contract**

VirtualSocket should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/comm/multiplexer/io/VirtualSocket.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/comm/multiplexer/io/VirtualSocket.java`
- Package: `com.viaoa.comm.multiplexer.io`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `createInputStream`, `read`, `getInputStream`, `createOutputStream`, `write`, `getOutputStream`, `getConnectionId`, `getId`, `getServerSocketName`, `close`, `setTimeoutSeconds`, `getTimeoutSeconds`.
- Fields/constants referenced by invariant review: `_connectionId`, `_id`, `_serverSocketName`, `_lockObject`.
- Declaration relationship: `extends Socket`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
