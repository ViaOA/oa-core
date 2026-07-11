# com.viaoa.oa.service.object.OAObjectRulesService

## Purpose

OA 4.0 object rules engine. {@code OAObjectRulesService} evaluates model-rule questions carried by {@link OAObjectCallback}. The callback {@link Type} defines the semantic question being asked, and {@link CheckType} values define which rules-engine stages are active through {@link OAObjectCallback#isUsed(CheckType)}. The primary processing order is: Session checks Metadata and object-state checks Object callback methods Hub listeners SuperAdmin override Later stages may intentionally refine or o

## Architectural Role

OAObjectRulesService is a class in the object runtime services area. Its invariants should be interpreted through the package role: Implements OAObject behavior such as metadata, identity, rules, persistence, property state, locking, and events.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.callback.OAObjectCallback
- com.viaoa.cascade
- com.viaoa.compare.match
- com.viaoa.converter
- com.viaoa.hub
- com.viaoa.hub.listener
- com.viaoa.lang
- com.viaoa.metadata

## Public Contract

Public/protected methods reviewed: getAllowVisible, getAllowVisibleObjectCallback, getVerifyPropertyChange, getVerifyPropertyChangeCallbackOnly, getVerifyPropertyChangeObjectCallback, getVerifyPropertyChangeCallbackOnlyObjectCallback, getAllowEnabled, getAllowEnabledCallbackOnly, getAllowEnabledObjectCallback, getAllowCopy, getCopy, getAllowAdd, getAllowAddIgnoreProcessed, getVerifyAdd, getAllowRemove, getAllowRemoveCallbackOnly, getAllowRemoveIgnoreProcessed, getVerifyRemove, getVerifyRemoveCallbackOnly, getVerifyRemoveIgnoreProcessed, getAllowRemoveAll, getVerifyRemoveAll, getAllowDelete, getVerifyDelete.

Annotations present: SuppressWarnings.

## Invariants

### INV-OAOBJECTRULESSERVICE-001: Rule processing is stage-driven

**Contract**

_processObjectCallback must run only the stages enabled by OAObjectCallback.CheckType.

**Rationale**

The Type/CheckType contract is the central OA 4.0 rules architecture.

**Evidence**

processObjectCallback, _processObjectCallback, CheckType checks

**Test implications**

For each CheckType, verify enabling/disabling changes only that stage.

**Confidence**

Medium

### INV-OAOBJECTRULESSERVICE-002: Object callback methods and Hub listeners can refine earlier results

**Contract**

Later callback/listener stages may intentionally override metadata/session/model-user results unless a failure is final by contract.

**Rationale**

Generated model code and UI/controller Hubs need extension points for contextual rules and messages.

**Evidence**

_processObjectCallback_2, _processObjectCallback_3, processObjectCallbackForHubListeners

**Test implications**

Set earlier denial, then callback/listener override, and verify response/throwable semantics.

**Confidence**

Medium

### INV-OAOBJECTRULESSERVICE-003: Owner hierarchy is a lightweight visible/enabled gate

**Contract**

Owner processing must check owner visibility for AllowVisible and owner enabled for other rule types, without running the full original rule on every owner.

**Rationale**

Owner traversal prevents child operations when containing objects are inaccessible without duplicating validation/deletion pipelines.

**Evidence**

ownerHierProcess and related helper methods

**Test implications**

Verify delete/property-change child checks only evaluate owner AllowEnabled/AllowVisible semantics.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Lifecycle

Lifecycle-sensitive operations should leave state valid after success, failure, cancellation, and repeated invocation. Any global, static, thread-local, listener, remote, or cache registration requires cleanup tests.

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

- Source file: `src/main/java/com/viaoa/oa/service/object/OAObjectRulesService.java`
- Package: `com.viaoa.oa.service.object`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getAllowVisible`, `getAllowVisibleObjectCallback`, `getVerifyPropertyChange`, `getVerifyPropertyChangeCallbackOnly`, `getVerifyPropertyChangeObjectCallback`, `getVerifyPropertyChangeCallbackOnlyObjectCallback`, `getAllowEnabled`, `getAllowEnabledCallbackOnly`, `getAllowEnabledObjectCallback`, `getAllowCopy`, `getCopy`, `getAllowAdd`, `getAllowAddIgnoreProcessed`, `getVerifyAdd`, `getAllowRemove`, `getAllowRemoveCallbackOnly`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
