# com.viaoa.remote

## Purpose

Root package for OA remote invocation and remote metadata support.

## Architectural Role

`com.viaoa.remote` supports remote method invocation infrastructure used by synchronization, replication, and distributed OA runtime components.

## Primary Responsibilities

- Define and support remote invocation contracts.
- Preserve serializable request/response identity across process boundaries.
- Keep remote transport details separate from OA model rule evaluation.

## Package Boundary

Remote infrastructure may call OA services, but it should not replace local OA identity, metadata, Hub, or rules contracts.

## Key Types

Subpackages provide remote invocation metadata, multiplexer integration, annotations, and IO support.

## Package-Level Invariants

### INV-PKG-REMOTE-001: Remote calls preserve local OA contracts

**Contract**

Remote invocation must preserve OA identity and operation semantics expected by local services; transport boundaries must not introduce alternate object, Hub, metadata, or rules behavior.

**Rationale**

Generated applications must behave consistently whether an operation is local or remote.

**Evidence**

Remote support is separated from core object, Hub, metadata, and OA service packages.

**Test implications**

Add integration tests comparing representative local and remote operations where practical.

**Confidence**

Medium

### INV-PKG-REMOTE-002: Remote metadata is descriptive, not policy-owning

**Contract**

Remote method and binding metadata should describe invocation behavior without becoming the owner of OA model permission or session-access policy.

**Rationale**

Permission and session scope belong to OA rules/session services, not transport metadata.

**Evidence**

Remote info types are under `com.viaoa.remote.info`; rules and callbacks are under `com.viaoa.oa.service.object` and `com.viaoa.callback`.

**Test implications**

Verify remote metadata construction does not bypass OAObjectRulesService when operations require rule checks.

**Confidence**

Medium

## Error and Failure Behavior

Remote failures should preserve enough detail for callers to distinguish transport failure from OA rule or validation denial.

## Required Invariant Tests

- Remote metadata construction tests.
- Remote invocation serialization tests.
- Failure propagation tests for representative remote methods.
