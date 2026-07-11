# com.viaoa.callback.OAObjectCallback

## Purpose

Request/response carrier used by the OA object rules engine. An {@code OAObjectCallback} describes one model-rule question being asked by OA and carries both the evaluation context and the resulting answer. It is processed by {@code OAObjectRulesService} and is also the shared carrier used by OAObject callback methods, Hub listeners, and UI/controller code. Core Contract {@link #getType() type} defines the semantic question, such as {@link Type#AllowDelete}, {@link Type#VerifySave}, {@link Type#

## Architectural Role

OAObjectCallback is a class in the rule and callback carriers area. Its invariants should be interpreted through the package role: Defines request/response callback objects and extension callbacks used by rules, copy, serialization, and UI layers.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.converter
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.object

## Public Contract

Public/protected methods reviewed: has, isUsed, getCheckTypes, getCheckTypesExcept, getAllCheckTypesButProcessed, getAllCheckTypesExcept, getCallbackOnlyCheckType, getCalcClass, setType, getType, setHub, getHub, getObject, setObject, ack, setAcknownledged, getAcknownledged, getPropertyName, setPropertyName, setOldValue, getOldValue, setValue, getValue, setResponse.

## Invariants

### INV-OAOBJECTCALLBACK-001: Type defines the semantic question

**Contract**

Every callback must have exactly one Type that describes the rule/UI/copy/confirmation question being evaluated.

**Rationale**

The rules engine uses Type to choose default CheckTypes and categories.

**Evidence**

OAObjectCallback.Type enum, constructors, getType

**Test implications**

Create callbacks for all Type values and verify expected defaults.

**Confidence**

Medium

### INV-OAOBJECTCALLBACK-002: CheckTypes define active processing stages

**Contract**

A callback may narrow active rule stages, but processing must use cb.isUsed(CheckType.X) semantics.

**Rationale**

This enables callback-only, hub-listener-only, and full-pipeline rule evaluation without duplicate logic.

**Evidence**

CheckType enum, hmOnlyCheckTypes, isUsed(CheckType)

**Test implications**

Narrow callbacks and verify disabled stages do not run.

**Confidence**

Medium

### INV-OAOBJECTCALLBACK-003: Result fields travel with context

**Contract**

allowed, response, throwable, confirm, tooltip, format, and label values belong to the same request context.

**Rationale**

Later rule stages and UI/controller callers need one carrier for both question and result.

**Evidence**

allowed/response/throwable/context fields

**Test implications**

Process callbacks through methods/listeners and verify result fields are preserved or intentionally overridden.

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

- Source file: `src/main/java/com/viaoa/callback/OAObjectCallback.java`
- Package: `com.viaoa.callback`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `has`, `isUsed`, `getCheckTypes`, `getCheckTypesExcept`, `getAllCheckTypesButProcessed`, `getAllCheckTypesExcept`, `getCallbackOnlyCheckType`, `getCalcClass`, `setType`, `getType`, `setHub`, `getHub`, `getObject`, `setObject`, `ack`, `setAcknownledged`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
