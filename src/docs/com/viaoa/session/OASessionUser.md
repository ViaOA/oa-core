# com.viaoa.session.OASessionUser

## Purpose

Class in com.viaoa.session that participates in the session access boundary package contract.

## Architectural Role

OASessionUser is a class in the session access boundary area. Its invariants should be interpreted through the package role: Defines session user/access scope that can narrow model-level rules for actors and sessions.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.hub
- com.viaoa.object

## Public Contract

Public/protected methods reviewed: getCalcUserObject, getUserHub, getUserObject, getSessionAccess, setSessionAccess.

Type declaration relationship: <T extends OAObject>.

## Invariants

### INV-OASESSIONUSER-001: Session user/access is not ModelUser

**Contract**

OASessionUser must represent session actor state/scope and not the generated model permission user.

**Rationale**

OA rules combine ModelUser permission with SessionAccess scope.

**Evidence**

src/main/java/com/viaoa/session/OASessionUser.java, OASessionUser/OASessionAccess usage

**Test implications**

Set different session and model users and verify independent results.

**Confidence**

Medium

### INV-OASESSIONUSER-002: Access decisions are deterministic for a fixed rule set

**Contract**

Visible/enabled results must be stable while rule collections are not being mutated.

**Rationale**

REST/UI/session boundaries must not vary unexpectedly during one request.

**Evidence**

getCalcUserObject, getUserHub, getUserObject, getSessionAccess, setSessionAccess

**Test implications**

Configure rules once and repeat enabled/visible checks.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

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

- Source file: `src/main/java/com/viaoa/session/OASessionUser.java`
- Package: `com.viaoa.session`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getCalcUserObject`, `getUserHub`, `getUserObject`, `getSessionAccess`, `setSessionAccess`.
- Declaration relationship: `<T extends OAObject>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
