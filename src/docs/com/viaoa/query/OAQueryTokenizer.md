# com.viaoa.query.OAQueryTokenizer

## Purpose

Parses OA object queries into a list of {@link OAQueryToken} elements. {@code OAQueryTokenizer} performs recursive-descent parsing of textual query expressions (e.g., {@code "id in (1,2,3) and status = 'A'"}) into structured token sequences that can be later transformed into a DataSource-specific native query (such as SQL, REST filter, or distributed call). Features Supports nested parentheses, AND/OR precedence, and comparison operators. Handles quoted string literals, escaped characters, and p

## Architectural Role

OAQueryTokenizer is a class in the query parsing area. Its invariants should be interpreted through the package role: Defines query tokens, tokenizer, and query representation for datasource/filter use.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Public Contract

Public/protected methods reviewed: convertToTokens, evaluate, evaluateA, evaluateB, evaluateB2, evaluateC, evaluateC2, evaluateD, evaluateE, evaluateF, nextToken, main2, main.

Type declaration relationship: implements OAQueryTokenType.

## Invariants

### INV-OAQUERYTOKENIZER-001: Public behavior is deterministic

**Contract**

OAQueryTokenizer public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/query/OAQueryTokenizer.java, methods: convertToTokens, evaluate, evaluateA, evaluateB, evaluateB2, evaluateC, evaluateC2, evaluateD, evaluateE, evaluateF

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OAQUERYTOKENIZER-002: Invalid inputs fail predictably

**Contract**

OAQueryTokenizer should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/query/OAQueryTokenizer.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/query/OAQueryTokenizer.java`
- Package: `com.viaoa.query`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `convertToTokens`, `evaluate`, `evaluateA`, `evaluateB`, `evaluateB2`, `evaluateC`, `evaluateC2`, `evaluateD`, `evaluateE`, `evaluateF`, `nextToken`, `main2`, `main`.
- Declaration relationship: `implements OAQueryTokenType`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
