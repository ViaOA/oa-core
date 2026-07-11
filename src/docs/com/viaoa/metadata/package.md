# com.viaoa.metadata

## Purpose

Stores annotation-derived class, property, link, calc, method, and model metadata used by runtime services.

## Architectural Role

OA metadata model

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OACalcInfo, OAFkeyInfo, OALinkInfo, OAMethodInfo, OAObjectInfo, OAObjectModel, OAPropertyInfo

## Dependencies

- com.viaoa.annotation
- com.viaoa.compare
- com.viaoa.datasource
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.lang.oa
- com.viaoa.oa
- com.viaoa.oa.service.object
- com.viaoa.object
- com.viaoa.runtime

## Package-Level Invariants

### INV-PKG-METADATA-001: Metadata names are canonical runtime names

**Contract**

Annotation-derived names must map consistently to metadata fields and generated property constants.

**Rationale**

Rules, paths, datasource mapping, and UI generation all depend on stable metadata names.

**Evidence**

OAClass/OAProperty/OAOne/OAMany annotations and OAObjectInfo/OAPropertyInfo/OALinkInfo

**Test implications**

Load annotated model classes and verify metadata lookup by class/property/link name.

**Confidence**

Medium

### INV-PKG-METADATA-002: Metadata is configured before runtime use

**Contract**

Metadata objects should be treated as stable descriptors once used by OA runtime services.

**Rationale**

Runtime services cache and share metadata; mutation during evaluation would make rules/path/datasource behavior non-deterministic.

**Evidence**

OAObjectInfo, OALinkInfo, OAPropertyInfo, OAMethodInfo

**Test implications**

Build metadata, run concurrent path/rule lookup, and verify no mutation-dependent failures.

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
