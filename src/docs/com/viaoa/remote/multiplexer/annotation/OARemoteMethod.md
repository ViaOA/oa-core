# com.viaoa.remote.multiplexer.annotation.OARemoteMethod

## Purpose

Defines remoting behavior for a specific method on a remote interface. This annotation must be placed on the interface method (never on the implementation class). The values control how the client and server handle: compression of return values, whether a return value is transmitted at all, whether the method call or its return value should bypass the asynchronous message queue, whether the server should execute the method inside an {@link OARemoteThread} (useful for broadcast or fan-out behavio

## Architectural Role

OARemoteMethod is a annotation in the remote invocation area. Its invariants should be interpreted through the package role: Defines remote call metadata, streams, interfaces, and multiplexer-backed invocation.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Annotations present: Documented, Retention, Target.

## Invariants

### INV-OAREMOTEMETHOD-001: Annotation values are metadata source of truth

**Contract**

@OARemoteMethod values must be readable at runtime and map to OA metadata fields.

**Rationale**

Generated and hand-written model classes declare runtime metadata through annotations.

**Evidence**

src/main/java/com/viaoa/remote/multiplexer/annotation/OARemoteMethod.java, annotation methods

**Test implications**

Reflect annotated test model classes and verify metadata loader captures each element.

**Confidence**

Medium

### INV-OAREMOTEMETHOD-002: Invalid inputs fail predictably

**Contract**

OARemoteMethod should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/remote/multiplexer/annotation/OARemoteMethod.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## Threading and Concurrency

Unless explicitly documented or implemented as thread-safe, mutable instances should be considered single-owner or configure-before-publish. Thread-local state must be cleared or restored by the caller/service that sets it.

## Failure and Exception Behavior

Failures should be deterministic: invalid inputs should either return documented default values or throw documented exceptions without leaving partially updated shared state.

## Prohibited States or Operations

- Use current OA 4.0 runtime terminology and service boundaries.
- Do not bypass OA runtime services for identity, metadata, relationship, rule, cache, or synchronization behavior unless the type explicitly owns that concern.
- Do not mutate configure-before-publish structures concurrently with evaluation unless tests prove it is safe.

## Required Invariant Tests

- Add focused tests for each invariant listed above.
- Include representative OA model objects, Hubs, metadata, callbacks, and paths when this type participates in runtime behavior.
- Verify null, boundary, invalid, repeated, and exceptional execution paths.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/remote/multiplexer/annotation/OARemoteMethod.java`
- Package: `com.viaoa.remote.multiplexer.annotation`
- Type kind: `annotation`

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
