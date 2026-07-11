# com.viaoa.hub.Hub

## Purpose

Core observable collection for OAObject OA models. The Hub acts as an enhanced, observable {@link java.util.List} that maintains a single "active object" (AO) and propagates all object and structural changes throughout linked, shared, and detail Hubs. It is the foundation of OA’s object-OA model synchronization, event dispatch, and master-detail wiring. Core Responsibilities Maintain ordered membership of domain objects with optional sorting and filtering. Track and broadcast the current active 

## Architectural Role

Hub is a class in the hub observable collection core area. Its invariants should be interpreted through the package role: Defines active-object collection semantics, listener/event contracts, and master-detail relationship behavior.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.cascade
- com.viaoa.filter
- com.viaoa.hub.auto
- com.viaoa.hub.filter
- com.viaoa.lang

## Public Contract

Public/protected methods reviewed: ensureCapacity, resizeToFit, readResolve, setProperty, getProperty, removeProperty, toString, setRefresh, getRefresh, getChanged, setChanged, copyInto, toArray, toList, getObjectClass, finalize, isMoreData, loadAllData, getCurrentSize, getSize, size, getLoadedSize, saveAll, deleteAll.

Public/protected fields/constants reviewed: data, datau, dataa, datam, DEBUG.

Annotations present: Override, SuppressWarnings.

Type declaration relationship: <TYPE extends OAObject> implements Serializable, List<TYPE>, Cloneable, Comparable<Hub<?>>, Iterable<TYPE>.

## Invariants

### INV-HUB-001: Hub object class constrains membership

**Contract**

A Hub either has no class before use or contains objects compatible with its object class.

**Rationale**

Typed Hubs, detail Hubs, datasource selects, and UI binding depend on homogeneous membership.

**Evidence**

Hub constructors, HubData object class, add paths

**Test implications**

Add compatible/incompatible objects and verify rejection or class assignment behavior.

**Confidence**

Medium

### INV-HUB-002: AO is separate from membership

**Contract**

The active object must be one object reference selected for navigation and can change independently from collection contents.

**Rationale**

Detail Hubs, UI binding, ModelUser Hub, and listeners depend on AO semantics.

**Evidence**

HubDataActive, HubAOService, getAO/setAO methods

**Test implications**

Change AO in populated/empty Hubs and verify membership size is unchanged.

**Confidence**

Medium

### INV-HUB-003: Shared Hubs share data but not all view state

**Contract**

Shared Hub relationships must preserve shared membership while allowing view-specific active object/state where designed.

**Rationale**

Shared data vectors let UI/controllers observe the same collection without duplicating objects.

**Evidence**

HubShareService, HubDataUnique sharedHub references

**Test implications**

Create shared Hubs and verify membership updates are shared and AO behavior follows contract.

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

- Source file: `src/main/java/com/viaoa/hub/Hub.java`
- Package: `com.viaoa.hub`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `ensureCapacity`, `resizeToFit`, `readResolve`, `setProperty`, `getProperty`, `removeProperty`, `toString`, `setRefresh`, `getRefresh`, `getChanged`, `setChanged`, `copyInto`, `toArray`, `toList`, `getObjectClass`, `finalize`.
- Fields/constants referenced by invariant review: `data`, `datau`, `dataa`, `datam`, `DEBUG`.
- Declaration relationship: `<TYPE extends OAObject> implements Serializable, List<TYPE>, Cloneable, Comparable<Hub<?>>, Iterable<TYPE>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
