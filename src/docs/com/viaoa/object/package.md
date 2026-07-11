# com.viaoa.object

## Purpose

Defines the object identity, state, property, persistence, and relationship contract for OA model classes.

## Architectural Role

OAObject model base

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OAObject, OAObjectInternalBridge, OAObjectKey, OAObjectLocal

## Dependencies

- com.viaoa.callback
- com.viaoa.callback.OAObjectCallback
- com.viaoa.compare
- com.viaoa.compare.match
- com.viaoa.converter
- com.viaoa.datasource
- com.viaoa.datetime
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.metadata
- com.viaoa.serialize

## Package-Level Invariants

### INV-PKG-OBJECT-001: OAObject identity is stable after initialization

**Contract**

Each OAObject must keep a stable GUID and object identity through property changes, Hub membership, save, and serialization boundaries.

**Rationale**

Caches, Hubs, sync, replication, and object-key lookup depend on stable identity.

**Evidence**

OAObject#guid, OAObjectGuidService, OAObjectKeyService, cache services

**Test implications**

Create, save, serialize, and cache objects; verify identity and key lookup remain stable.

**Confidence**

Medium

### INV-PKG-OBJECT-002: Property mutations go through OAObject services

**Contract**

Public property mutation paths must notify before/after change services and preserve changed/new/deleted state rules.

**Rationale**

Bypassing services would skip validation, rules, listeners, sync, undo, and dirty-state tracking.

**Evidence**

OAObject#setProperty/firePropertyChange, OAObjectPropertyService, OAObjectChangeService

**Test implications**

Change scalar/link properties and assert callbacks, listeners, changed flags, and reverse links.

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
