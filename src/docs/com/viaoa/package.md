# com.viaoa

## Purpose

Root package for shared OA core runtime support packages.

## Architectural Role

This package groups the core OA runtime, object model, Hub, metadata, path, datasource, synchronization, replication, and utility packages. Most architectural contracts are owned by subpackages rather than by the root package itself.

## Primary Responsibilities

- Provide the namespace root for OA core.
- Keep public package boundaries stable for generated applications and OA runtime services.
- Separate core runtime concerns into package-specific APIs and services.

## Package Boundary

The root package should not become a dumping ground for behavior. New runtime capabilities should live in the narrowest package that owns the concern.

## Key Types

The root package contains package-level source declarations and delegates substantive runtime behavior to subpackages.

## Package-Level Invariants

### INV-PKG-COM-VIAOA-001: Subpackages own runtime concerns

**Contract**

Behavioral contracts should be defined in the package that owns the concern: object state in `com.viaoa.object`, Hub behavior in `com.viaoa.hub`, OA runtime access in `com.viaoa.oa`, metadata in `com.viaoa.metadata`, and path behavior in `com.viaoa.path`.

**Rationale**

Keeping ownership localized prevents cross-package service leaks and makes generated application behavior easier to reason about.

**Evidence**

Production source is organized into focused subpackages under `src/main/java/com/viaoa`.

**Test implications**

Invariant tests should target the owning package rather than broad root-package behavior.

**Confidence**

High

## Cross-Package Contracts

Subpackages may collaborate through OA runtime services, metadata, OAObject, Hub, callbacks, and paths. These collaborations should preserve the public/internal service boundary documented in the relevant package docs.

## Required Invariant Tests

Use subpackage-specific invariant tests.
