# com.viaoa.runtime.thread.OAThreadLocalHubMergerCallback

## Purpose

Callback interface used internally by OA’s Hub-merge process to execute custom logic within the same thread-local merge context. When OA merges one Hub’s contents into another, it temporarily installs a thread-local context so that recursive updates and event propagation can be suppressed or controlled. Implementations of this interface are invoked inside that protected block to perform additional merge-aware operations. Typical usage: {@code HubMerger.merge(hubA, hubB, () -> { /* custom post-me

## Architectural Role

OAThreadLocalHubMergerCallback is a interface in the runtime registry and thread services area. Its invariants should be interpreted through the package role: Locates OA runtimes and owns thread-local, remote-thread, datasource, and thread execution services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: callback.

## Invariants

### INV-OATHREADLOCALHUBMERGERCALLBACK-001: Public behavior is deterministic

**Contract**

OAThreadLocalHubMergerCallback public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThreadLocalHubMergerCallback.java, methods: callback

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OATHREADLOCALHUBMERGERCALLBACK-002: Invalid inputs fail predictably

**Contract**

OAThreadLocalHubMergerCallback should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/runtime/thread/OAThreadLocalHubMergerCallback.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/runtime/thread/OAThreadLocalHubMergerCallback.java`
- Package: `com.viaoa.runtime.thread`
- Type kind: `interface`
- Methods/constructors referenced by invariant review: `callback`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
