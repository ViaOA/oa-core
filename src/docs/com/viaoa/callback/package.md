# com.viaoa.callback

## Purpose

Defines request/response callback objects and extension callbacks used by rules, copy, serialization, and UI layers.

## Architectural Role

Rule and callback carriers

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OACallback, OACallbackLabel, OACopyCallback, OAObjectCallback, OAObjectSerializerCallback

## Dependencies

- com.viaoa.converter
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.object
- com.viaoa.serialize

## Package-Level Invariants

### INV-PKG-CALLBACK-001: Callback objects carry rule question and result

**Contract**

OAObjectCallback instances must keep Type, context, active CheckTypes, allowed, response, and throwable together for one rule evaluation.

**Rationale**

The rules engine, object callback methods, Hub listeners, and UI/controller code share this carrier.

**Evidence**

OAObjectCallback fields, Type, CheckType, CategoryType

**Test implications**

Create callbacks for add/delete/property change and verify fields survive processing.

**Confidence**

Medium

### INV-PKG-CALLBACK-002: Callback extensions must not own runtime policy

**Contract**

Callback classes may supply decisions or transformed values, but OA services own storage, identity, and policy orchestration.

**Rationale**

Keeps extension hooks composable with metadata, ModelUser, SessionAccess, and Hub listeners.

**Evidence**

OACallback, OACopyCallback, OAObjectCallback, OAObjectRulesService

**Test implications**

Use callback overrides and verify service-owned rule stages still run when enabled.

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
