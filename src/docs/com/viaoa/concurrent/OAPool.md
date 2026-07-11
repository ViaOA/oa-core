# com.viaoa.concurrent.OAPool

## Purpose

Generic thread-safe object pool that maintains a configurable minimum and maximum number of pooled instances. The pool grows on demand, blocks callers when all objects are in use and the maximum size has been reached, and gradually shrinks when usage decreases. Instances are created and released using the {@link #create()} and {@link #removed(Object)} callback methods. The pool tracks active usage counts, high-water marks, and decay thresholds to determine when idle resources may be released. Al

## Architectural Role

OAPool is a class in the concurrency utilities area. Its invariants should be interpreted through the package role: Defines executor, pool, throttle, and scheduling wrappers used by OA services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: setHighMarkTimeLimit, setMinimum, getMinimum, setMaximum, getMaximum, getCurrentSize, getCurrentUsed, loadMinimum, get, remove, release, getAllItems, add, create, removed.

Type declaration relationship: <TYPE>.

## Invariants

### INV-OAPOOL-001: Public behavior is deterministic

**Contract**

OAPool public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/concurrent/OAPool.java, methods: setHighMarkTimeLimit, setMinimum, getMinimum, setMaximum, getMaximum, getCurrentSize, getCurrentUsed, loadMinimum, get, remove

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OAPOOL-002: Invalid inputs fail predictably

**Contract**

OAPool should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/concurrent/OAPool.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/concurrent/OAPool.java`
- Package: `com.viaoa.concurrent`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setHighMarkTimeLimit`, `setMinimum`, `getMinimum`, `setMaximum`, `getMaximum`, `getCurrentSize`, `getCurrentUsed`, `loadMinimum`, `get`, `remove`, `release`, `getAllItems`, `add`, `create`, `removed`.
- Declaration relationship: `<TYPE>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
