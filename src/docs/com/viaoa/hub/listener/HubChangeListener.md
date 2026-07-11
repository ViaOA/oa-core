# com.viaoa.hub.listener.HubChangeListener

## Purpose

Rule-based, multi-hub condition monitor that aggregates checks over one or more {@link Hub}s (and optional property paths) and evaluates them as a single boolean via {@link #getValue()}. Use the {@code add*} methods to compose conditions (hub validity/emptiness, AO null/new, property null/empty/not-empty, object-callback enabled/visible, custom {@link com.viaoa.filter.OAFilter}, etc.). The listener internals will attach a shared {@link HubListener} per (hub,path) and re-use it across rules to mi

## Architectural Role

HubChangeListener is a class in the hub observable collection core area. Its invariants should be interpreted through the package role: Defines active-object collection semantics, listener/event contracts, and master-detail relationship behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.compare
- com.viaoa.compare.match
- com.viaoa.converter
- com.viaoa.filter
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.oa.service.object

## Public Contract

Public/protected methods reviewed: useAoOnly, add, addHubValid, addHubNotValid, addHubEmpty, addHubNotEmpty, addAoNew, addAoNotNew, addAoNull, addAoNotNull, addAlwaysTrue, addAlwaysFalse, addOnlySuperAdmin, addPropertyNull, addPropertyNotNull, addPropertyEmpty, addPropertyNotEmpty, addPropertyChange, addAddEnabled, isUsed, addNewEnabled, addDeleteEnabled, addRemoveEnabled, addSaveEnabled.

Public/protected fields/constants reviewed: hubProps, DEBUG, hub, path, listenToPropertyName, props, hubListener, compareValue, bUseCompareValue, filter, bAoOnly, bIgnore, failureReason, description, alHubChangeListener.

Annotations present: Override, SuppressWarnings.

## Invariants

### INV-HUBCHANGELISTENER-001: Hub helper preserves Hub invariants

**Contract**

HubChangeListener must preserve Hub object class, AO, membership, and listener/event contracts when it transforms or observes Hubs.

**Rationale**

Hub helpers compose core Hub behavior; breaking invariants affects UI, sync, rules, and generated apps.

**Evidence**

src/main/java/com/viaoa/hub/listener/HubChangeListener.java, Hub-related methods

**Test implications**

Exercise helper with add/remove/AO changes and verify events and membership.

**Confidence**

Medium

### INV-HUBCHANGELISTENER-002: Invalid inputs fail predictably

**Contract**

HubChangeListener should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/hub/listener/HubChangeListener.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/hub/listener/HubChangeListener.java`
- Package: `com.viaoa.hub.listener`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `useAoOnly`, `add`, `addHubValid`, `addHubNotValid`, `addHubEmpty`, `addHubNotEmpty`, `addAoNew`, `addAoNotNew`, `addAoNull`, `addAoNotNull`, `addAlwaysTrue`, `addAlwaysFalse`, `addOnlySuperAdmin`, `addPropertyNull`, `addPropertyNotNull`, `addPropertyEmpty`.
- Fields/constants referenced by invariant review: `hubProps`, `DEBUG`, `hub`, `path`, `listenToPropertyName`, `props`, `hubListener`, `compareValue`, `bUseCompareValue`, `filter`, `bAoOnly`, `bIgnore`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
