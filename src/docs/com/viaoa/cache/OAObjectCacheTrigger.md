# com.viaoa.cache.OAObjectCacheTrigger

## Purpose

Reactive trigger that monitors the global {@link OAObjectCacheDelegate} and invokes {@link #onTrigger(OAObject)} when an object of the target class satisfies specified filter and property-path conditions. OAObjectCacheTrigger generalizes {@link OAObjectCacheFilter} by attaching {@link OATrigger}s to dependent property paths, allowing automatic response when any linked property changes, even across associations. Core Behavior: Registers a class-level {@link OAObjectCacheListener}. Evaluates one o

## Architectural Role

OAObjectCacheTrigger is a class in the object cache/indexing area. Its invariants should be interpreted through the package role: Maintains canonical object cache views, indexes, listeners, and cache-to-Hub adapters.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.filter
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.oa
- com.viaoa.object
- com.viaoa.runtime
- com.viaoa.trigger

## Public Contract

Public/protected methods reviewed: afterPropertyChange, afterAdd, afterRemove, afterLoad, setServerSideOnly, addFilter, refresh, updateObject, addDependentProperty, setupTrigger, onTrigger, isUsed, close, finalize.

Public/protected fields/constants reviewed: bServerSideOnly.

Annotations present: Override, SuppressWarnings.

Type declaration relationship: <T extends OAObject> implements OAFilter<T>.

## Invariants

### INV-OAOBJECTCACHETRIGGER-001: Public behavior is deterministic

**Contract**

OAObjectCacheTrigger public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/cache/OAObjectCacheTrigger.java, methods: afterPropertyChange, afterAdd, afterRemove, afterLoad, setServerSideOnly, addFilter, refresh, updateObject, addDependentProperty, setupTrigger

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OAOBJECTCACHETRIGGER-002: Invalid inputs fail predictably

**Contract**

OAObjectCacheTrigger should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/cache/OAObjectCacheTrigger.java, constructors and public methods

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

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

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

- Source file: `src/main/java/com/viaoa/cache/OAObjectCacheTrigger.java`
- Package: `com.viaoa.cache`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `afterPropertyChange`, `afterAdd`, `afterRemove`, `afterLoad`, `setServerSideOnly`, `addFilter`, `refresh`, `updateObject`, `addDependentProperty`, `setupTrigger`, `onTrigger`, `isUsed`, `close`, `finalize`.
- Fields/constants referenced by invariant review: `bServerSideOnly`.
- Declaration relationship: `<T extends OAObject> implements OAFilter<T>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
