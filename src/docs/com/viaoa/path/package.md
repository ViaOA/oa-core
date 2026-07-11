# com.viaoa.path

## Purpose

Parses and evaluates OA property paths against OAObject, Hub, and metadata structures.

## Architectural Role

Path compiler/evaluator

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OAPath, OAPathDelegate

## Dependencies

- com.viaoa.annotation
- com.viaoa.converter
- com.viaoa.filter
- com.viaoa.hub
- com.viaoa.hub.filter
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.object
- com.viaoa.runtime

## Package-Level Invariants

### INV-PKG-PATH-001: Path setup binds a path string to a root model type

**Contract**

A compiled OAPath must resolve against a compatible from-class or Hub object class before dependable traversal.

**Rationale**

Path evaluation, filters, metadata lookups, and generated apps depend on resolved method/link arrays.

**Evidence**

OAPath constructors, setup methods, OAPathDelegate

**Test implications**

Resolve valid/invalid paths and verify terminal class/value metadata.

**Confidence**

Medium

### INV-PKG-PATH-002: Hub segments traverse through the active object

**Contract**

When a path segment returns a Hub and traversal continues, the next path segment uses the Hub active object.

**Rationale**

This matches OA binding semantics and generated UI assumptions.

**Evidence**

OAPath value traversal methods and Hub#getAO integration

**Test implications**

Create nested Hub paths with null/non-null AO and verify resulting values.

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
