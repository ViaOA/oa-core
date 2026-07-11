# com.viaoa.runtime.OARuntime

## Purpose

Singleton entry point for resolving and creating OA runtime instances. {@code OARuntime} maps model packages, OAObject classes, OAObject instances, and Hubs to the {@link OA} runtime that owns their metadata and services. It also tracks the default OA runtime and the catch-all runtime used when no more specific model package has been registered. The runtime owns shared infrastructure services for datasource registration and thread/runtime state. Application and library code normally use the stat

## Architectural Role

OARuntime is a class in the runtime registry and thread services area. Its invariants should be interpreted through the package role: Locates OA runtimes and owns thread-local, remote-thread, datasource, and thread execution services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.hub
- com.viaoa.oa
- com.viaoa.object

## Public Contract

Public/protected methods reviewed: get, createOA, createDefaultOA, close, oa, defaultOA, catchAllOA, thread, datasource, setUnitTestMode, unitTestReset.

Annotations present: Override.

## Invariants

### INV-OARUNTIME-001: Runtime state is scoped by OA and thread where applicable

**Contract**

OARuntime must keep thread-local/model-specific state separated by OA runtime instance where the API requires it.

**Rationale**

OA 4.0 supports multiple OA/model runtimes on one thread.

**Evidence**

src/main/java/com/viaoa/runtime/OARuntime.java, OARuntime/OAThreadLocal methods

**Test implications**

Set per-OA thread-local state and verify isolation.

**Confidence**

Medium

### INV-OARUNTIME-002: Invalid inputs fail predictably

**Contract**

OARuntime should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/runtime/OARuntime.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/runtime/OARuntime.java`
- Package: `com.viaoa.runtime`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `get`, `createOA`, `createDefaultOA`, `close`, `oa`, `defaultOA`, `catchAllOA`, `thread`, `datasource`, `setUnitTestMode`, `unitTestReset`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
