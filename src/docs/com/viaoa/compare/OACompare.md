# com.viaoa.compare.OACompare

## Purpose

Utility class that performs flexible comparisons between values of arbitrary types. OACompare extends standard Java comparison semantics by applying OA-style coercion rules: Values of different types (e.g., String "123" vs. Double 123.0) are automatically coerced for comparison. Supports comparisons involving OAObject, OAObjectKey, arrays, and Hub collections. Boolean, numeric, and date/time types are normalized through OAConverter before comparing. Special placeholder objects (e.g., OAMatchNull

## Architectural Role

OACompare is a class in the comparison/matching area. Its invariants should be interpreted through the package role: Defines comparison and match semantics used by filters, queries, and UI rules.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.compare.match
- com.viaoa.converter
- com.viaoa.datetime
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.oa
- com.viaoa.object
- com.viaoa.reflect

## Public Contract

Public/protected methods reviewed: isEqualOrIn, isIn, isLike, isEqualIgnoreCase, isEqual, isNotEqual, isBetween, isEqualOrBetween, isBetweenOrEqual, isGreater, isEqualOrGreater, isGreaterOrEqual, isLess, isEqualOrLess, isLessOrEqual, compare, isNotEmpty, isEmpty, compareNumbers, toBigDecimal, isNonFinite.

## Invariants

### INV-OACOMPARE-001: Comparison semantics are stable for supported types

**Contract**

Comparison/match operations must handle supported null, numeric, string, date, and OA values consistently.

**Rationale**

Filters, query parsing, UI sorting, and rules depend on stable comparisons.

**Evidence**

src/main/java/com/viaoa/compare/OACompare.java, compare/match methods

**Test implications**

Test nulls, mixed numeric types, strings, and unsupported values.

**Confidence**

Medium

### INV-OACOMPARE-002: Invalid inputs fail predictably

**Contract**

OACompare should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/compare/OACompare.java, constructors and public methods

**Test implications**

Call constructors/public methods with boundary inputs and assert return values or exceptions.

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

- Source file: `src/main/java/com/viaoa/compare/OACompare.java`
- Package: `com.viaoa.compare`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `isEqualOrIn`, `isIn`, `isLike`, `isEqualIgnoreCase`, `isEqual`, `isNotEqual`, `isBetween`, `isEqualOrBetween`, `isBetweenOrEqual`, `isGreater`, `isEqualOrGreater`, `isGreaterOrEqual`, `isLess`, `isEqualOrLess`, `isLessOrEqual`, `compare`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
