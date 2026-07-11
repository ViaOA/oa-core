# com.viaoa.converter.internal

## Purpose

Defines deterministic value conversion across Java/OA primitive, date/time, enum, and string types.

## Architectural Role

Type conversion

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

OAConverterBigDecimal, OAConverterBigInteger, OAConverterBoolean, OAConverterCalendar, OAConverterCharacter, OAConverterClass, OAConverterDate, OAConverterEnum, OAConverterInstant, OAConverterInterface, OAConverterLocalDate, OAConverterLocalDateTime, OAConverterLocalTime, OAConverterNumber, OAConverterOADate, OAConverterOADateTime, OAConverterOATime, OAConverterSqlDate

## Dependencies

- com.viaoa.converter
- com.viaoa.datetime
- com.viaoa.lang
- com.viaoa.lang.oa
- com.viaoa.reflect

## Package-Level Invariants

### INV-PKG-CONVERTER-INTERNAL-001: Public contracts remain deterministic

**Contract**

Public methods in this package should return deterministic results for the same inputs and documented mutable state.

**Rationale**

Utility and support packages are used deeply by runtime services and generated applications.

**Evidence**

Package classes: OAConverterBigDecimal, OAConverterBigInteger, OAConverterBoolean, OAConverterCalendar, OAConverterCharacter, OAConverterClass, OAConverterDate, OAConverterEnum, OAConverterInstant, OAConverterInterface

**Test implications**

Run representative normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-PKG-CONVERTER-INTERNAL-002: Shared mutable state is explicit

**Contract**

Any static caches, listeners, registries, pools, or background threads must have clear lifecycle and cleanup behavior.

**Rationale**

Hidden global state causes test pollution, memory leaks, and cross-model interference.

**Evidence**

Static fields, listener registration, executor/pool/cache types where present

**Test implications**

Create and tear down package objects repeatedly; verify no stale state leaks between tests.

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
