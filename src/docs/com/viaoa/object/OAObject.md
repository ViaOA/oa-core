# com.viaoa.object.OAObject

## Purpose

Base class for OA model objects. {@code OAObject} supplies the runtime behavior that lets generated and hand written model classes participate in the OA model: property storage, change notification, identity, lifecycle flags, lazy loading, rule/callback checks, persistence hooks, synchronization, serialization, and Hub relationship support. Application entities normally extend {@code OAObject}. The object itself does not contain datasource-specific code; persistence, metadata, rules, cache, sync

## Architectural Role

OAObject is a class in the oaobject model base area. Its invariants should be interpreted through the package role: Defines the object identity, state, property, persistence, and relationship contract for OA model classes.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.callback.OAObjectCallback
- com.viaoa.compare
- com.viaoa.compare.match
- com.viaoa.converter
- com.viaoa.datasource
- com.viaoa.datetime
- com.viaoa.find
- com.viaoa.hub

## Public Contract

Public/protected methods reviewed: getOAVersion, readResolve, setProperty, setNull, setPrimitiveNull, getProperty, getPropertyAsString, removeProperty, isValidPropertyChange, getIsValidPropertyChangeObjectCallback, isEnabled, getIsEnabledObjectCallback, isVisible, getIsVisibleObjectCallback, verifyCommand, getVerifyCommand, getAllowSubmit, getVerifySaveObjectCallback, getNew, isNew, setNew, getDeleted, wasDeleted, isDeleted.

Public/protected fields/constants reviewed: OALOG, guid, changedFlag, newFlag, nulls, deletedFlag, weakhubs, properties, CASCADE_NONE, CASCADE_LINK_RULES, CASCADE_OWNED_LINKS, CASCADE_ALL_LINKS, cntNew.

Annotations present: Override.

Type declaration relationship: implements java.io.Serializable, Comparable<Object>.

## Invariants

### INV-OAOBJECT-001: GUID identity is assigned during initialization

**Contract**

A constructed OAObject must receive stable identity through OAObjectInitializeService before normal use.

**Rationale**

Hub membership, cache canonicalization, serialization, sync, and replication rely on stable identity.

**Evidence**

OAObject constructor, guid field, OAObjectGuidService

**Test implications**

Construct objects and verify GUID remains unchanged across property changes/save.

**Confidence**

Medium

### INV-OAOBJECT-002: State flags represent lifecycle

**Contract**

new, changed, deleted, and primitive-null flags must reflect lifecycle and property state transitions.

**Rationale**

Persistence, rules, UI, and sync need accurate dirty/deleted/null-state information.

**Evidence**

newFlag, changedFlag, deletedFlag, nulls, OAObjectStateService

**Test implications**

Save/delete/change primitive properties and verify flags transition as expected.

**Confidence**

Medium

### INV-OAOBJECT-003: Relationship property access is service-mediated

**Contract**

Object references and Hub references must be obtained/mutated through OAObject services, preserving reverse links and lazy-load behavior.

**Rationale**

Direct storage mutation would break metadata-driven relationship invariants.

**Evidence**

properties array, getObject/getHub/setProperty call paths

**Test implications**

Set one/many links and verify reverse links, lazy loading, and Hub membership.

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

- Source file: `src/main/java/com/viaoa/object/OAObject.java`
- Package: `com.viaoa.object`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getOAVersion`, `readResolve`, `setProperty`, `setNull`, `setPrimitiveNull`, `getProperty`, `getPropertyAsString`, `removeProperty`, `isValidPropertyChange`, `getIsValidPropertyChangeObjectCallback`, `isEnabled`, `getIsEnabledObjectCallback`, `isVisible`, `getIsVisibleObjectCallback`, `verifyCommand`, `getVerifyCommand`.
- Fields/constants referenced by invariant review: `OALOG`, `guid`, `changedFlag`, `newFlag`, `nulls`, `deletedFlag`, `weakhubs`, `properties`, `CASCADE_NONE`, `CASCADE_LINK_RULES`, `CASCADE_OWNED_LINKS`, `CASCADE_ALL_LINKS`.
- Declaration relationship: `implements java.io.Serializable, Comparable<Object>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
