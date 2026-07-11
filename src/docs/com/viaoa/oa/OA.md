# com.viaoa.oa.OA

## Purpose

OA runtime for a model package. {@code OA} is the executable runtime for a model-defined blueprint. It operates as a layer above the data source, allowing applications to work with live objects and collections instead of directly interacting with persistence or transport mechanisms. The model defines the blueprint. {@code OA} executes that blueprint by creating and wiring live {@link OAObject} instances and {@link Hub} collections together based on model-defined relationships. Through its verbs,

## Architectural Role

OA is a interface in the oa runtime root area. Its invariants should be interpreted through the package role: Defines the OA runtime instance, facade, and service boundary for a model package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.find
- com.viaoa.hub
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.oa.api.internal
- com.viaoa.oa.api.services
- com.viaoa.object
- com.viaoa.select

## Public Contract

Public/protected methods reviewed: getPackageName, sync, replication.

## Invariants

### INV-OA-001: OA instance is bound to one model package

**Contract**

An OA runtime instance must represent one model package name and expose services/internal operations for that model only.

**Rationale**

Model metadata, ModelUser state, rules, sync, replication, and object creation are package-scoped.

**Evidence**

OA#getPackageName(), OARuntime#oa(...), OAImpl service fields

**Test implications**

Create OA runtimes for two packages and verify service state and metadata do not cross.

**Confidence**

Medium

### INV-OA-002: Services/internal API split is preserved

**Contract**

Application-level callers use services/modelUser/sessionUser/config/sync/replication, while runtime internals use internal().

**Rationale**

This protects generated applications from implementation-only object/Hub service contracts.

**Evidence**

OA#services(), OA#internal(), OA#modelUser(), OA#sessionUser()

**Test implications**

Compile and exercise public service calls without reaching internal interfaces.

**Confidence**

Medium

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

- Source file: `src/main/java/com/viaoa/oa/OA.java`
- Package: `com.viaoa.oa`
- Type kind: `interface`
- Methods/constructors referenced by invariant review: `getPackageName`, `sync`, `replication`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
