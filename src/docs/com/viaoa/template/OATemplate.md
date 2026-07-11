# com.viaoa.template.OATemplate

## Purpose

A lightweight, high-performance template engine used throughout OA for generating dynamic strings, HTML fragments, and code-generation output. OATemplate processes a template string that contains zero or more variable placeholders of the form ${name} (configurable start/end tokens). During evaluation, each placeholder is resolved using either a callback interface or a supplied map of variable values. The resulting output is generated in a single forward pass with minimal allocations, making it s

## Architectural Role

OATemplate is a class in the template area. Its invariants should be interpreted through the package role: Provides OA runtime support types for the com.viaoa.template package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa
- com.viaoa.config
- com.viaoa.converter
- com.viaoa.datetime
- com.viaoa.find
- com.viaoa.hub
- com.viaoa.lang
- com.viaoa.lang.oa
- com.viaoa.metadata
- com.viaoa.oa.sibling
- com.viaoa.path
- com.viaoa.runtime

## Public Contract

Public/protected methods reviewed: setTemplate, getTemplate, process, stopProcessing, setProperty, createTree, removeRowTags, _removeRowTags, _removeRowTagsBefore, _removeRowTagsAfter, preprocess, getIncludeText, parse, getHasParseError, hasEndToken, parseTokens, generate, _generate, createMatrix, getOutputText, setOutputTextConversion, setHiliteOutputText, getValue, getProperty.

Public/protected fields/constants reviewed: hiliteText.

Annotations present: Override.

Type declaration relationship: <F extends OAObject>.

## Invariants

### INV-OATEMPLATE-001: Public behavior is deterministic

**Contract**

OATemplate public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/template/OATemplate.java, methods: setTemplate, getTemplate, process, stopProcessing, setProperty, createTree, removeRowTags, _removeRowTags, _removeRowTagsBefore, _removeRowTagsAfter

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OATEMPLATE-002: Invalid inputs fail predictably

**Contract**

OATemplate should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/template/OATemplate.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/template/OATemplate.java`
- Package: `com.viaoa.template`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `setTemplate`, `getTemplate`, `process`, `stopProcessing`, `setProperty`, `createTree`, `removeRowTags`, `_removeRowTags`, `_removeRowTagsBefore`, `_removeRowTagsAfter`, `preprocess`, `getIncludeText`, `parse`, `getHasParseError`, `hasEndToken`, `parseTokens`.
- Fields/constants referenced by invariant review: `hiliteText`.
- Declaration relationship: `<F extends OAObject>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
