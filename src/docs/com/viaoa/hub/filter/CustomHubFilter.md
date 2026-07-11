# com.viaoa.hub.filter.CustomHubFilter

## Purpose

Defines a customizable filter interface for Hub data sets. This interface extends {@link com.viaoa.filter.OAFilter} to allow additional Hub-aware logic, providing access to the underlying {@link HubFilter} that owns or delegates filtering decisions. Purpose Enables compound or decorator filters that can evaluate both object attributes and Hub context. Allows injection of dynamic filter behavior (e.g., for UI or cascading filters) without subclassing {@code HubFilter} itself. Usage Example {@code

## Architectural Role

CustomHubFilter is a interface in the hub observable collection core area. Its invariants should be interpreted through the package role: Defines active-object collection semantics, listener/event contracts, and master-detail relationship behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.filter
- com.viaoa.object

## Public Contract

No substantial public/protected members were detected beyond construction or type identity; invariant coverage is conservative.

## Invariants

### INV-CUSTOMHUBFILTER-001: Hub helper preserves Hub invariants

**Contract**

CustomHubFilter must preserve Hub object class, AO, membership, and listener/event contracts when it transforms or observes Hubs.

**Rationale**

Hub helpers compose core Hub behavior; breaking invariants affects UI, sync, rules, and generated apps.

**Evidence**

src/main/java/com/viaoa/hub/filter/CustomHubFilter.java, Hub-related methods

**Test implications**

Exercise helper with add/remove/AO changes and verify events and membership.

**Confidence**

Medium

### INV-CUSTOMHUBFILTER-002: Invalid inputs fail predictably

**Contract**

CustomHubFilter should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/hub/filter/CustomHubFilter.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/hub/filter/CustomHubFilter.java`
- Package: `com.viaoa.hub.filter`
- Type kind: `interface`

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
