# com.viaoa.comm

## Purpose

Root communication package for OA core networking and transport support.

## Architectural Role

`com.viaoa.comm` groups lower-level communication helpers used by remote, sync, replication, and multiplexer packages.

## Primary Responsibilities

- Provide transport support types used by OA communication layers.
- Keep low-level IO/socket behavior separate from model, Hub, metadata, and rules behavior.
- Support deterministic failure behavior for communication setup and teardown.

## Package Boundary

This package should not own OA model identity, rule evaluation, or persistence semantics. It provides communication primitives and delegates OA-level behavior to higher layers.

## Key Types

Subpackages contain IO, SSL, and multiplexer implementations.

## Package-Level Invariants

### INV-PKG-COMM-001: Communication helpers do not own OA model semantics

**Contract**

Communication types may transport OA-related data, but they must not define OA object identity, metadata, Hub membership, or rules semantics.

**Rationale**

Keeping transport separate from model semantics prevents remote/sync code from diverging from local OA runtime behavior.

**Evidence**

Communication code is organized under `com.viaoa.comm.*`, while model behavior lives under `com.viaoa.object`, `com.viaoa.hub`, `com.viaoa.metadata`, and `com.viaoa.oa`.

**Test implications**

Transport tests should verify IO behavior independently from OA rule/model tests.

**Confidence**

High

## Error and Failure Behavior

Communication failures should surface deterministically through return values or exceptions without corrupting higher-level OA runtime state.

## Required Invariant Tests

- Multiplexer connection lifecycle tests.
- SSL/client/server setup failure tests.
- IO stream close/error propagation tests.
