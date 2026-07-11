# com.viaoa.oa.service.facade.ServicesOpsImpl

## Purpose

Default implementation of the top-level {@link ServicesOps} facade. This class groups the curated OA service nouns exposed through {@code oa.services()} and returns the object, Hub, trigger, and rules operation facades supplied by the owning OA runtime.

## Architectural Role

ServicesOpsImpl is a class in the oa runtime services area. Its invariants should be interpreted through the package role: Owns model-level runtime services such as config, model user, session user, sync, replication, and triggers.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.oa.api.services

## Public Contract

Public/protected methods reviewed: objects, hubs, triggers, rules.

Annotations present: Override.

Type declaration relationship: implements ServicesOps.

## Invariants

### INV-SERVICESOPSIMPL-001: Service delegates preserve runtime ownership

**Contract**

ServicesOpsImpl must operate on OAObject/Hub state through the owning OA runtime and must not create unrelated global ownership.

**Rationale**

OA 4.0 service classes are runtime-scoped coordination points; hidden global state would break multi-model execution.

**Evidence**

src/main/java/com/viaoa/oa/service/facade/ServicesOpsImpl.java, public/protected service methods

**Test implications**

Use two OA runtimes or model classes and verify service state does not leak.

**Confidence**

Medium

### INV-SERVICESOPSIMPL-002: Null and class context are explicit

**Contract**

Service entry points must either reject null context deterministically or derive class/object/Hub context using documented runtime rules.

**Rationale**

Rules, metadata, and datasource operations need stable class resolution.

**Evidence**

objects, hubs, triggers, rules

**Test implications**

Call public methods with null/missing Hub/object context and verify deterministic behavior.

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

- Source file: `src/main/java/com/viaoa/oa/service/facade/ServicesOpsImpl.java`
- Package: `com.viaoa.oa.service.facade`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `objects`, `hubs`, `triggers`, `rules`.
- Declaration relationship: `implements ServicesOps`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
