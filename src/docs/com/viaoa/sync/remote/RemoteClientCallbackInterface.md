# com.viaoa.sync.remote.RemoteClientCallbackInterface

## Purpose

Callback interface implemented by the client and invoked by the server to deliver out-of-band notifications. Methods are invoked outside of the normal sync queue, using direct socket writes. Typical uses include: terminating the client connection with a message, simple connectivity pings, dumping server thread stacks for diagnostics. All methods are routed through the multiplexer using non-queued or low-latency semantics to avoid ordering delays relative to ordinary sync messages.

## Architectural Role

RemoteClientCallbackInterface is a interface in the real-time synchronization area. Its invariants should be interpreted through the package role: Coordinates client/server live runtime synchronization.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.remote.multiplexer.annotation

## Public Contract

Public/protected methods reviewed: performThreadDump.

Annotations present: OARemoteInterface, OARemoteMethod.

## Invariants

### INV-REMOTECLIENTCALLBACKINTERFACE-001: Public behavior is deterministic

**Contract**

RemoteClientCallbackInterface public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/sync/remote/RemoteClientCallbackInterface.java, methods: performThreadDump

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-REMOTECLIENTCALLBACKINTERFACE-002: Invalid inputs fail predictably

**Contract**

RemoteClientCallbackInterface should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/sync/remote/RemoteClientCallbackInterface.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

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

- Source file: `src/main/java/com/viaoa/sync/remote/RemoteClientCallbackInterface.java`
- Package: `com.viaoa.sync.remote`
- Type kind: `interface`
- Methods/constructors referenced by invariant review: `performThreadDump`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
