# com.viaoa.oa.api.ReplicationOps

## Purpose

Public OA replication operations. Replication is a runtime role used to coordinate persisted changes between a master and one or more clients. This interface exposes the caller-visible lifecycle and configuration state for that replication role.

## Architectural Role

ReplicationOps is a interface in the oa runtime root area. Its invariants should be interpreted through the package role: Defines the OA runtime instance, facade, and service boundary for a model package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.sync

## Public Contract

Public/protected methods reviewed: createMaster, createClient, isMaster, isClient, start, stop, getStatus, getLogFileName, getMasterHostName, getMasterPort.

## Invariants

### INV-REPLICATIONOPS-001: API describes behavior, implementation supplies policy

**Contract**

ReplicationOps must remain a contract surface and not encode implementation storage assumptions.

**Rationale**

Interfaces separate public/internal contracts from concrete services and preserve OA 4.0 layering.

**Evidence**

src/main/java/com/viaoa/oa/api/ReplicationOps.java interface methods

**Test implications**

Compile implementations against the interface and verify no callers require implementation classes.

**Confidence**

Medium

### INV-REPLICATIONOPS-002: Method names remain semantic

**Contract**

ReplicationOps methods must describe OA operations at the correct boundary level.

**Rationale**

Generated applications and runtime services rely on stable API names.

**Evidence**

createMaster, createClient, isMaster, isClient, start, stop, getStatus, getLogFileName, getMasterHostName, getMasterPort

**Test implications**

Create API compatibility tests for representative callers.

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

- Source file: `src/main/java/com/viaoa/oa/api/ReplicationOps.java`
- Package: `com.viaoa.oa.api`
- Type kind: `interface`
- Methods/constructors referenced by invariant review: `createMaster`, `createClient`, `isMaster`, `isClient`, `start`, `stop`, `getStatus`, `getLogFileName`, `getMasterHostName`, `getMasterPort`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
