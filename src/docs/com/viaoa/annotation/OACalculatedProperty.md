# com.viaoa.annotation.OACalculatedProperty

## Purpose

Declares a read-only calculated property on an {@link OAObject}, including its formatting, dependent property paths, and UI characteristics. These properties are evaluated dynamically (often via {@code getXxx()} methods containing custom logic) and have no storage in the underlying datasource. Key Responsibilities Define dependent properties for automatic recalculation. Specify formatting and display hints (lengths, tooltips, enum mappings). Provide type hints (email, url, xml, timestamp, curren

## Architectural Role

OACalculatedProperty is a annotation in the oa annotation contract area. Its invariants should be interpreted through the package role: Defines the annotations that generated and hand-written model classes use to declare metadata.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Annotations present: Documented, Retention, Target.

## Invariants

### INV-OACALCULATEDPROPERTY-001: Annotation values are metadata source of truth

**Contract**

@OACalculatedProperty values must be readable at runtime and map to OA metadata fields.

**Rationale**

Generated and hand-written model classes declare runtime metadata through annotations.

**Evidence**

src/main/java/com/viaoa/annotation/OACalculatedProperty.java, annotation methods

**Test implications**

Reflect annotated test model classes and verify metadata loader captures each element.

**Confidence**

Medium

### INV-OACALCULATEDPROPERTY-002: Invalid inputs fail predictably

**Contract**

OACalculatedProperty should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/annotation/OACalculatedProperty.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

**Confidence**

Medium

## Threading and Concurrency

Unless explicitly documented or implemented as thread-safe, mutable instances should be considered single-owner or configure-before-publish. Thread-local state must be cleared or restored by the caller/service that sets it.

## Failure and Exception Behavior

Failures should be deterministic: invalid inputs should either return documented default values or throw documented exceptions without leaving partially updated shared state.

## Prohibited States or Operations

- Use current OA 4.0 runtime terminology and service boundaries.
- Do not bypass OA runtime services for identity, metadata, relationship, rule, cache, or synchronization behavior unless the type explicitly owns that concern.
- Do not mutate configure-before-publish structures concurrently with evaluation unless tests prove it is safe.

## Required Invariant Tests

- Add focused tests for each invariant listed above.
- Include representative OA model objects, Hubs, metadata, callbacks, and paths when this type participates in runtime behavior.
- Verify null, boundary, invalid, repeated, and exceptional execution paths.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/annotation/OACalculatedProperty.java`
- Package: `com.viaoa.annotation`
- Type kind: `annotation`

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
