# com.viaoa.serialize.OAObjectSerializer

## Purpose

Serializes and deserializes OAObject state for caching, messaging, and distributed synchronization. This serializer transfers identity and data in a form that preserves lazy loading and metadata-driven resolution on the destination side. Object references are represented using OAObjectKey so that related objects do not need to be materialized during serialization. This enables efficient graph projection for remote clients and reduces network payload size. Hub properties and collections are handl

## Architectural Role

OAObjectSerializer is a class in the serialization area. Its invariants should be interpreted through the package role: Defines OA serialization contexts, readers, writers, serializers, and deserializers.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.callback
- com.viaoa.comm.io
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.metadata

## Public Contract

Public/protected methods reviewed: setId, getId, setClientId, getClientId, getIncludeBlobs, setIncludeBlobs, setExcludedReferences, excludedClasses, getReferenceValueToSend, setMax, getMax, getTotalObjectsWritten, setMaxSize, getMaxSize, includeProperties, excludeProperties, includeAllProperties, excludeAllProperties, getStackSize, getPreviousObject, getStackObject, getLevelsDeep, shouldSerializeReference, hasReachedMax.

Public/protected fields/constants reviewed: newCount, dupCount.

Type declaration relationship: <TYPE> implements Serializable.

## Invariants

### INV-OAOBJECTSERIALIZER-001: Public behavior is deterministic

**Contract**

OAObjectSerializer public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/serialize/OAObjectSerializer.java, methods: setId, getId, setClientId, getClientId, getIncludeBlobs, setIncludeBlobs, setExcludedReferences, excludedClasses, getReferenceValueToSend, setMax

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OAOBJECTSERIALIZER-002: Invalid inputs fail predictably

**Contract**

OAObjectSerializer should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/serialize/OAObjectSerializer.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Lifecycle

Lifecycle-sensitive operations should leave state valid after success, failure, cancellation, and repeated invocation. Any global, static, thread-local, listener, remote, or cache registration requires cleanup tests.

## Identity Rules

Identity must be scoped by the relevant OA concept: OA runtime, object class, OAObject key/GUID, Hub instance, path root type, remote request, or datasource key. Cross-scope identity leakage should be treated as a defect.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

## Threading and Concurrency

Unless explicitly documented or implemented as thread-safe, mutable instances should be considered single-owner or configure-before-publish. Thread-local state must be cleared or restored by the caller/service that sets it.

## Event and Callback Ordering

Callbacks/listeners/events should run in the order defined by the owning service or Hub. Later stages may override earlier responses only where the rule contract explicitly allows it.

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

- Source file: `src/main/java/com/viaoa/serialize/OAObjectSerializer.java`
- Package: `com.viaoa.serialize`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setId`, `getId`, `setClientId`, `getClientId`, `getIncludeBlobs`, `setIncludeBlobs`, `setExcludedReferences`, `excludedClasses`, `getReferenceValueToSend`, `setMax`, `getMax`, `getTotalObjectsWritten`, `setMaxSize`, `getMaxSize`, `includeProperties`, `excludeProperties`.
- Fields/constants referenced by invariant review: `newCount`, `dupCount`.
- Declaration relationship: `<TYPE> implements Serializable`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
