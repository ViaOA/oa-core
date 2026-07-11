# com.viaoa.hub.link

## Purpose

Defines active-object collection semantics, listener/event contracts, and master-detail relationship behavior.

## Architectural Role

Hub observable collection core

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

HubLink, HubLinkEventListener

## Dependencies

- com.viaoa
- com.viaoa.hub
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.oa.service.object
- com.viaoa.runtime

## Package-Level Invariants

### INV-PKG-HUB-LINK-001: Hub membership changes are observable

**Contract**

Add, remove, AO, sort, filter, and detail changes must publish Hub events through the Hub event path.

**Rationale**

OA UI, generated apps, sync, and dependent Hubs rely on Hub as the observable collection root.

**Evidence**

Hub, HubEvent, HubListener, HubEventService and related service interfaces

**Test implications**

Attach listeners and verify event order for add/remove/AO/detail changes.

**Confidence**

Medium

### INV-PKG-HUB-LINK-002: Active object is independent collection state

**Contract**

A Hub may contain many objects but has at most one active object for binding/navigation at a time.

**Rationale**

AO is central to detail Hubs, UI binding, and ModelUser Hub semantics.

**Evidence**

HubDataActive, HubAOService, Hub#setAO/getAO methods

**Test implications**

Change AO without changing membership; verify detail Hubs and listeners update predictably.

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
