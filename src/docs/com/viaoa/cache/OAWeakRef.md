# com.viaoa.cache.OAWeakRef

## Purpose

Wraps a cached OAObject in a weak reference while retaining the object's class and GUID needed to remove the cache entry after the referent is garbage collected.

## Architectural Role

OAWeakRef is an internal cache-support type used by `OAObjectCache`. It exists to connect Java weak-reference cleanup with OA object identity cleanup.

## Responsibilities

- Hold a weak reference to an OAObject.
- Retain enough identity data to remove the corresponding class/GUID cache entry after GC.
- Avoid extending object lifetime through a strong reference to the OAObject.

## Collaborators

- `OAObject`
- `OAObjectCache`
- `ReferenceQueue`
- `UUID`

## Public Contract

This package-private type is not a public OA API. Its contract is defined by `OAObjectCache` cleanup behavior.

## Invariants

### INV-OAWEAKREF-001: Weak reference does not retain OAObject

**Contract**

OAWeakRef must not keep a strong reference to its OAObject referent.

**Rationale**

The OA object cache must preserve canonical identity while allowing unused objects to be reclaimed.

**Evidence**

`OAWeakRef` extends `WeakReference<T>` and is used by `OAObjectCache` maps.

**Test implications**

Cache cleanup tests should verify that cleared references can be detected and removed without depending on exact GC timing.

**Confidence**

High

### INV-OAWEAKREF-002: Cleanup identity is retained independently of referent

**Contract**

The cache class and GUID needed for removal must remain available even after the referent has been cleared.

**Rationale**

Reference-queue cleanup cannot ask a cleared OAObject for its identity.

**Evidence**

`OAWeakRef` stores identity fields alongside the weak referent and is processed by `OAObjectCache#checkReferenceQueue()`.

**Test implications**

Verify that a cleared reference can be removed from the correct class/GUID map.

**Confidence**

High

## State Model

State is immutable identity data plus the weak referent managed by `WeakReference`.

## Threading and Concurrency

Threading behavior is owned by `OAObjectCache`; OAWeakRef itself should remain a passive value/reference holder.

## Required Invariant Tests

- Weak reference cleanup removes the correct class/GUID entry.
- Cleared references do not corrupt unrelated class maps.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/cache/OAObjectCache.java`
- Nested/package-private type: `OAWeakRef`

## Open Questions or Unclear Contracts

No separate public contract is intended for OAWeakRef outside `OAObjectCache`.
