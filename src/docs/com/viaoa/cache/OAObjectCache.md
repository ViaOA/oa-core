# com.viaoa.cache.OAObjectCache

## Purpose

Maintains weak-reference identity cache entries for OAObject instances, keyed primarily by OA class and GUID with secondary lookup support through OAObjectKey.

## Architectural Role

OAObjectCache is the runtime object-identity cache for OA objects in the current JVM. It supports the architectural rule that a loaded OAObject identity should resolve to a canonical in-memory instance when it is still reachable.

## Responsibilities

- Store OAObject instances by class and GUID using weak references.
- Maintain secondary object-key indexes for business-key lookup.
- Remove cache entries whose weak references have been cleared.
- Notify cache listeners and triggers through the cache infrastructure where applicable.

## Collaborators

- `OAObject`
- `OAObjectKey`
- `OAObjectIndex`
- `OAWeakRef`
- `OARuntime`
- `OA`

## Public Contract

Public methods expose class inventory, cache counts, clear operations, GUID/key lookup, cache update, listener registration, object enumeration, callback iteration, and trigger/filter management.

## Invariants

### INV-OAOBJECTCACHE-001: Class and GUID identify cached object identity

**Contract**

For a non-reclaimed cached object, lookup by the object's OA class and GUID must return the same in-memory OAObject instance.

**Rationale**

OA object identity, relationship synchronization, remote notifications, and generated application behavior rely on stable object identity within a JVM.

**Evidence**

`OAObjectCache#getObject(Class, UUID)`, `OAObjectCache#updateObject(OAObject)`, `OAObjectCache#updateObject(OAObject, OAObjectKey, Class)`, `OAObjectIndex`.

**Test implications**

Cache an object, retrieve it by GUID, update it with an equivalent key, and verify the same instance is returned while strongly reachable.

**Confidence**

High

### INV-OAOBJECTCACHE-002: Weak references do not define object lifetime

**Contract**

The cache must not prevent otherwise unreachable OAObject instances from being garbage collected.

**Rationale**

The cache preserves identity only while objects remain reachable elsewhere; it must not become an unbounded strong-reference object store.

**Evidence**

`OAObjectCache` stores `OAWeakRef` values and uses a `ReferenceQueue<OAObject>` for cleanup.

**Test implications**

Use focused cache tests that avoid depending on exact GC timing, but verify cleared references are removed when the reference queue is processed.

**Confidence**

Medium

### INV-OAOBJECTCACHE-003: Secondary key lookup resolves through canonical GUID lookup

**Contract**

Lookup by `OAObjectKey` or ID values must resolve to a GUID and then use the GUID cache path.

**Rationale**

Business-key lookup must not create a second identity path that can disagree with GUID identity.

**Evidence**

`OAObjectCache#getObject(Class, Object[])`, `OAObjectCache#getObject(Class, OAObjectKey)`, `OAObjectIndex#lookupGuid(...)`.

**Test implications**

Update an object with a key, retrieve it by key, and verify the result matches GUID lookup.

**Confidence**

High

## State Model

The primary state is a concurrent map from OAObject class to GUID weak-reference maps, plus a secondary object-key index and listener/filter/trigger state.

## Threading and Concurrency

The primary maps are concurrent, but listener/filter/trigger mutation should still be tested under representative access patterns before being treated as fully concurrent application API.

## Failure and Exception Behavior

Null class, key, GUID, or object inputs should return a deterministic default or no-op result where the current public method contract supports it.

## Required Invariant Tests

- Canonical lookup by class and GUID.
- Secondary key lookup returning the same canonical instance.
- Clear-by-class and clear-all behavior.
- Null input behavior for lookup/update methods.
- Weak-reference cleanup behavior without relying on arbitrary sleeps.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/cache/OAObjectCache.java`
- Package: `com.viaoa.cache`

## Open Questions or Unclear Contracts

Exact listener/filter/trigger ordering should be verified against the current implementation before documenting as a strict ordering guarantee.
