# com.viaoa.datasource.clientserver

## Purpose

Defines persistence/select/save/delete contracts independent of specific storage implementations.

## Architectural Role

Datasource abstraction

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OADataSourceClient

## Dependencies

- com.viaoa.datasource
- com.viaoa.datasource.objectcache
- com.viaoa.filter
- com.viaoa.hub
- com.viaoa.oa
- com.viaoa.oa.sibling
- com.viaoa.object
- com.viaoa.runtime
- com.viaoa.sync.remote

## Package-Level Invariants

### INV-PKG-DATASOURCE-CLIENTSERVER-001: Datasource operations are model-class scoped

**Contract**

Select, save, delete, and iterator operations must be scoped to the OAObject class and metadata involved.

**Rationale**

Class scoping prevents cross-model persistence and key lookup ambiguity.

**Evidence**

OADataSource, OASelect, OADataSourceIterator, OADataSourceObjectCache

**Test implications**

Select two model classes with overlapping keys and verify no cross-class leakage.

**Confidence**

Medium

### INV-PKG-DATASOURCE-CLIENTSERVER-002: Iterators must release datasource resources

**Contract**

Datasource iterators must be closed or exhausted without leaking remote/database resources.

**Rationale**

Select operations may hold result sets, remote cursors, or cache views.

**Evidence**

OADataSourceIterator and concrete iterator classes

**Test implications**

Open/cancel/exhaust select iterators and assert close/cancel behavior.

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
