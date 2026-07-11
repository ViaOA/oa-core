# com.viaoa.oa.service

## Purpose

Owns model-level runtime services such as config, model user, session user, sync, replication, and triggers.

## Architectural Role

OA runtime services

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OAConfigService, OAModelUserService, OAReplicationService, OASessionUserService, OASyncService, OATriggerService

## Dependencies

- com.viaoa.cascade
- com.viaoa.converter
- com.viaoa.converter.internal
- com.viaoa.datasource.clientserver
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.oa.api
- com.viaoa.oa.api.internal
- com.viaoa.object
- com.viaoa.remote.info
- com.viaoa.replication
- com.viaoa.runtime
- com.viaoa.runtime.thread

## Package-Level Invariants

### INV-PKG-OA-SERVICE-001: Services and internal boundaries remain explicit

**Contract**

Application-facing operations must be exposed through service facades, while implementation-only object/Hub operations remain under internal APIs.

**Rationale**

The OA 4.0 service/internal split prevents generated applications and UI code from depending on runtime internals.

**Evidence**

OA#services(), OA#internal(), com.viaoa.oa.api.services.*, com.viaoa.oa.api.internal.*

**Test implications**

Compile representative callers against services only; verify internal implementations are reached through OA wiring.

**Confidence**

Medium

### INV-PKG-OA-SERVICE-002: ModelUser and SessionUser remain separate

**Contract**

Model-level permissions must use ModelUser state; session actor scoping must use SessionUser/OASessionAccess state.

**Rationale**

Conflating these identities can either overgrant model permissions or apply session boundaries to generated permission roots.

**Evidence**

OAModelUserService, OASessionUserService, OASessionAccess, OAObjectRulesService

**Test implications**

Set different model/session users and verify rules combine rather than overwrite each other.

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
