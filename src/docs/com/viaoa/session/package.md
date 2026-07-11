# com.viaoa.session

## Purpose

Defines session user/access scope that can narrow model-level rules for actors and sessions.

## Architectural Role

Session access boundary

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OASessionAccess, OASessionUser

## Dependencies

- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.object
- com.viaoa.path
- com.viaoa.select

## Package-Level Invariants

### INV-PKG-SESSION-001: SessionAccess narrows session actor scope

**Contract**

OASessionAccess rules describe visible/enabled slices for a session actor and must not replace ModelUser permissions.

**Rationale**

Session actor boundaries are an additional runtime scope, combined with generated model permissions.

**Evidence**

OASessionAccess, OASessionUser, OAObjectRulesService session checks

**Test implications**

Set session rules that deny access while ModelUser allows; verify final rule result is denied unless an explicit override is intended.

**Confidence**

Medium

### INV-PKG-SESSION-002: Session access rules are configure-before-publish

**Contract**

Mutable access rule collections should be configured before being shared across request/session threads.

**Rationale**

Current rule storage uses mutable collections; concurrent mutation can make evaluations unstable.

**Evidence**

OASessionAccess rule lists/maps/sets

**Test implications**

Run access evaluation while mutating rules; document or enforce stable behavior.

**Confidence**

Medium

## Lifecycle and State Rules

Package state should be initialized before runtime use and cleaned up when lifecycle APIs expose cleanup or cancellation. Static or shared state must be treated as runtime-wide unless the API explicitly scopes it by OA instance, object class, Hub, or thread.

## Threading and Concurrency Rules

Unless a type explicitly documents thread safety, callers should treat mutable instances as single-owner or configure-before-publish. Listener, callback, cache, executor, remote, and thread-local types need focused tests for leak-free cleanup.

## Cross-Package Contracts

This package participates in OA runtime contracts through metadata, OAObject, Hub, runtime services, callbacks, paths, datasource, sync, or utility APIs as indicated by its dependencies.

## Required Invariant Tests

- Verify the package-level invariants above with representative model classes and real OA runtime services where practical.
- Include null/boundary behavior, lifecycle cleanup, and cross-package integration paths.

## Open Questions or Unclear Contracts

Some invariants are inferred from current implementation and existing Javadocs. Where confidence is Medium or Low, tests should lock the intended behavior before relying on it as a public architectural guarantee.
