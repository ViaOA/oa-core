# com.viaoa.metadata.pojo.PojoDelegate

## Purpose

Utility methods for querying {@link Pojo} metadata and interpreting which POJO properties participate in key-matching logic. This delegate provides: simple lookups for {@link PojoProperty}, {@link PojoRegularProperty} and {@link PojoLink} by name, helpers to retrieve all {@link PojoProperty} instances or only those that are marked as key parts (via {@code keyPos}), and {@link com.viaoa.metadata.OAObjectInfo}-aware helpers to determine whether a type has: a primary-key based POJO key, an import-m

## Architectural Role

PojoDelegate is a class in the oa metadata model area. Its invariants should be interpreted through the package role: Stores annotation-derived class, property, link, calc, method, and model metadata used by runtime services.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.lang
- com.viaoa.metadata

## Public Contract

Public/protected methods reviewed: getPojoProperty, getPojoRegularProperty, getPojoLink, hasKey, hasCompoundKey, getPojoProperties, getPojoPropertyKeys, hasPkey, hasImportMatchKey, hasLinkUniqueKey.

## Invariants

### INV-POJODELEGATE-001: Metadata objects represent model descriptors

**Contract**

PojoDelegate instances must describe model classes/members and not runtime object state.

**Rationale**

Rules, paths, datasource, generated UI, and annotation loading rely on metadata being descriptive and reusable.

**Evidence**

src/main/java/com/viaoa/metadata/pojo/PojoDelegate.java, fields and getters/setters

**Test implications**

Load metadata for a model and verify repeated lookup returns equivalent descriptors.

**Confidence**

Medium

### INV-POJODELEGATE-002: Names and reverse relationships are stable after setup

**Contract**

Property/link/method names and reverse names should remain stable once runtime services use them.

**Rationale**

Path resolution and relationship maintenance depend on stable descriptor names.

**Evidence**

getPojoProperty, getPojoRegularProperty, getPojoLink, hasKey, hasCompoundKey, getPojoProperties, getPojoPropertyKeys, hasPkey, hasImportMatchKey, hasLinkUniqueKey

**Test implications**

Mutate only during setup; verify runtime lookups stay consistent afterward.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Identity Rules

Identity must be scoped by the relevant OA concept: OA runtime, object class, OAObject key/GUID, Hub instance, path root type, remote request, or datasource key. Cross-scope identity leakage should be treated as a defect.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

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

- Source file: `src/main/java/com/viaoa/metadata/pojo/PojoDelegate.java`
- Package: `com.viaoa.metadata.pojo`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getPojoProperty`, `getPojoRegularProperty`, `getPojoLink`, `hasKey`, `hasCompoundKey`, `getPojoProperties`, `getPojoPropertyKeys`, `hasPkey`, `hasImportMatchKey`, `hasLinkUniqueKey`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
