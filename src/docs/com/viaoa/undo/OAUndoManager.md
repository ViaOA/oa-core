# com.viaoa.undo.OAUndoManager

## Purpose

OA-specific extension of {@link javax.swing.undo.UndoManager} providing undo/redo support for Hub and OAObject operations. {@code OAUndoManager} centralizes undo logic for OA-based applications and supplies additional features on top of Swing's {@code UndoManager}: global, shared undo manager via {@link #createUndoManager()}, compound edits for grouping multiple operations, integration with {@code OAThreadLocalDelegate} for automatic property-change capture, thread-local ignore counters to suppr

## Architectural Role

OAUndoManager is a class in the undo area. Its invariants should be interpreted through the package role: Provides OA runtime support types for the com.viaoa.undo package.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.oa
- com.viaoa.runtime

## Public Contract

Public/protected methods reviewed: createUndoManager, getUndoManager, setVerbose, getVerbose, startCompoundEdit, startCompoundEditForPropertyChanges, endCompoundEditForPropertyChanges, getPresentationName, getUndoPresentationName, getRedoPresentationName, canRedo, endCompoundEdit, isInCompoundEdit, cancelCompoundEdit, add, addEdit, setIgnore, ignore, getIgnore, setIgnoreAll, undo.

Public/protected fields/constants reviewed: hmThreadCounter, undoManager, bVerbose, bIgnoreAll.

Annotations present: Override.

Type declaration relationship: extends UndoManager.

## Invariants

### INV-OAUNDOMANAGER-001: Public behavior is deterministic

**Contract**

OAUndoManager public/protected methods should return deterministic results for the same inputs and documented state.

**Rationale**

This type participates in OA runtime utility behavior and is likely reused across services.

**Evidence**

src/main/java/com/viaoa/undo/OAUndoManager.java, methods: createUndoManager, getUndoManager, setVerbose, getVerbose, startCompoundEdit, startCompoundEditForPropertyChanges, endCompoundEditForPropertyChanges, getPresentationName, getUndoPresentationName, getRedoPresentationName

**Test implications**

Run normal, null, boundary, and invalid-input tests.

**Confidence**

Medium

### INV-OAUNDOMANAGER-002: Invalid inputs fail predictably

**Contract**

OAUndoManager should handle null, empty, invalid, or unsupported inputs according to a stable contract.

**Rationale**

Predictable failure behavior prevents hidden runtime state corruption and simplifies generated-app error handling.

**Evidence**

src/main/java/com/viaoa/undo/OAUndoManager.java, constructors and public methods

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

- Source file: `src/main/java/com/viaoa/undo/OAUndoManager.java`
- Package: `com.viaoa.undo`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `createUndoManager`, `getUndoManager`, `setVerbose`, `getVerbose`, `startCompoundEdit`, `startCompoundEditForPropertyChanges`, `endCompoundEditForPropertyChanges`, `getPresentationName`, `getUndoPresentationName`, `getRedoPresentationName`, `canRedo`, `endCompoundEdit`, `isInCompoundEdit`, `cancelCompoundEdit`, `add`, `addEdit`.
- Fields/constants referenced by invariant review: `hmThreadCounter`, `undoManager`, `bVerbose`, `bIgnoreAll`.
- Declaration relationship: `extends UndoManager`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
