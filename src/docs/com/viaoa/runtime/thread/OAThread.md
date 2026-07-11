# com.viaoa.runtime.thread.OAThread

## Purpose

Thread implementation that automatically propagates OA thread context from the creating thread into the newly created thread. The constructor snapshots the current context from {@link com.viaoa.object.OAThreadLocalDelegate#getContext()}, restores it at the beginning of {@link #run()}, and clears it when the task completes. This ensures that background work has access to the same OA model, user/session information, or other context bound to the originating thread. Also provides utility sleep meth

## Architectural Role

OAThread is a class in the runtime registry and thread services area. Its invariants should be interpreted through the package role: Locates OA runtimes and owns thread-local, remote-thread, datasource, and thread execution services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.datetime
- com.viaoa.runtime
- com.viaoa.session

## Public Contract

Public/protected methods reviewed: run, yield, delay, sleepSeconds, sleep, sleepUntil.

Annotations present: Override.

Type declaration relationship: extends Thread.

## Invariants

### INV-OATHREAD-001: Runtime state is scoped by OA and thread where applicable

**Contract**

OAThread must keep thread-local/model-specific state separated by OA runtime instance where the API requires it.

**Rationale**

OA 4.0 supports multiple OA/model runtimes on one thread.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThread.java, OARuntime/OAThreadLocal methods

**Test implications**

Set per-OA thread-local state and verify isolation.

**Confidence**

Medium

### INV-OATHREAD-002: Invalid inputs fail predictably

**Contract**

OAThread should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThread.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/runtime/thread/OAThread.java`
- Package: `com.viaoa.runtime.thread`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `run`, `yield`, `delay`, `sleepSeconds`, `sleep`, `sleepUntil`.
- Declaration relationship: `extends Thread`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
