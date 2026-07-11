# com.viaoa.hub.util.HubSample

## Purpose

Maintains a {@link Hub} containing the first N elements of another Hub. This helper automatically mirrors the top portion of a master Hub’s contents in a smaller “sample” Hub, updating it whenever the master Hub changes. It is often used for UI previews, dashboards, or analytics views that only require a subset of the full list. Usage Example {@code Hub hubAll = new Hub<>(Customer.class); Hub hubTop5 = new Hub<>(Customer.class); new HubSample<>(hubAll, hubTop5, 5); } The {@code hubTop5} list wil

## Architectural Role

HubSample is a class in the hub observable collection core area. Its invariants should be interpreted through the package role: Defines active-object collection semantics, listener/event contracts, and master-detail relationship behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.hub
- com.viaoa.object

## Public Contract

Public/protected methods reviewed: setup, afterAdd, afterInsert, afterNewList, afterRemove, afterRemoveAll, afterSort, refresh, close, finalize.

Public/protected fields/constants reviewed: hubMaster, hubSample, amtSample, hubListener.

Annotations present: Override.

Type declaration relationship: <T extends OAObject>.

## Invariants

### INV-HUBSAMPLE-001: Hub helper preserves Hub invariants

**Contract**

HubSample must preserve Hub object class, AO, membership, and listener/event contracts when it transforms or observes Hubs.

**Rationale**

Hub helpers compose core Hub behavior; breaking invariants affects UI, sync, rules, and generated apps.

**Evidence**

src/main/java/com/viaoa/hub/util/HubSample.java, Hub-related methods

**Test implications**

Exercise helper with add/remove/AO changes and verify events and membership.

**Confidence**

Medium

### INV-HUBSAMPLE-002: Invalid inputs fail predictably

**Contract**

HubSample should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/hub/util/HubSample.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/hub/util/HubSample.java`
- Package: `com.viaoa.hub.util`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setup`, `afterAdd`, `afterInsert`, `afterNewList`, `afterRemove`, `afterRemoveAll`, `afterSort`, `refresh`, `close`, `finalize`.
- Fields/constants referenced by invariant review: `hubMaster`, `hubSample`, `amtSample`, `hubListener`.
- Declaration relationship: `<T extends OAObject>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
