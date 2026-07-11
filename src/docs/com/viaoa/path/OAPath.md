# com.viaoa.path.OAPath

## Purpose

Parsed OA property path used to navigate OAObject instances, Hubs, links, calculated properties, and filtered relationship segments. A path is a dotted sequence of model member names, optionally including casts and custom Hub filters. Examples: customer.address.city orderItems:OpenItems().product.name (Manager)teamMember.email Setup resolves the path against OA metadata and Java accessors, recording the methods, classes, link information, filter constructors, terminal metadata, and formatting in

## Architectural Role

OAPath is a class in the path compiler/evaluator area. Its invariants should be interpreted through the package role: Parses and evaluates OA property paths against OAObject, Hub, and metadata structures.

## Responsibilities

- Preserve its public/protected API contract.
- Keep state transitions, identity, ownership, callback, and failure behavior deterministic for callers.
- Participate in OA 4.0 terminology and service boundaries without relying on retired graph terminology.

## Collaborators

- com.viaoa.annotation
- com.viaoa.converter
- com.viaoa.filter
- com.viaoa.hub
- com.viaoa.hub.filter
- com.viaoa.lang
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.object

## Public Contract

Public/protected methods reviewed: getPath, getReversePath, getPathLinksOnly, getEndPropertyInfo, getEndCalcInfo, getEndLinkInfo, getOAPropertyAnnotation, getOACalculatedPropertyAnnotation, getOAOneAnnotation, getProperties, getCastNames, getFilterNames, getFilterParams, getFilterParamValues, getMethods, getClasses, getFilterConstructors, getLinkInfos, hasLinks, getRecursiveLinkInfos, getValue, getValueAsString, getLastLinkValue, getLastPropertyName.

Type declaration relationship: <TYPE extends OAObject>.

## Invariants

### INV-OAPATH-001: Compiled path state matches path string and root class

**Contract**

OAPath must resolve property/link/filter segments consistently for its root context.

**Rationale**

Incorrect compiled path state leads to wrong values, wrong permissions, or datasource query errors.

**Evidence**

src/main/java/com/viaoa/path/OAPath.java, setup/value methods

**Test implications**

Resolve simple, nested, Hub, null-intermediate, and invalid paths.

**Confidence**

Medium

### INV-OAPATH-002: Traversal handles null intermediates deterministically

**Contract**

Path evaluation should return null or documented default behavior for null roots/intermediates rather than corrupting state.

**Rationale**

Generated UI, filters, and rules evaluate paths frequently against partial object graphs.

**Evidence**

getPath, getReversePath, getPathLinksOnly, getEndPropertyInfo, getEndCalcInfo, getEndLinkInfo, getOAPropertyAnnotation, getOACalculatedPropertyAnnotation, getOAOneAnnotation, getProperties

**Test implications**

Evaluate nested paths where each intermediate is null.

**Confidence**

Medium

## State Model

State is inferred from fields, constructors, public/protected methods, and package role. Mutable state must remain internally consistent across normal, exceptional, and callback/listener-driven paths.

## Identity Rules

Identity must be scoped by the relevant OA concept: OA runtime, object class, OAObject key/GUID, Hub instance, path root type, remote request, or datasource key. Cross-scope identity leakage should be treated as a defect.

## Ownership and Relationship Rules

Ownership and relationship behavior should follow OA metadata and service boundaries. Direct mutation of internal relationship state should not bypass OAObject, Hub, metadata, or rules services.

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

- Source file: `src/main/java/com/viaoa/path/OAPath.java`
- Package: `com.viaoa.path`
- Type kind: `class`
- Methods/constructors referenced by invariant review: `getPath`, `getReversePath`, `getPathLinksOnly`, `getEndPropertyInfo`, `getEndCalcInfo`, `getEndLinkInfo`, `getOAPropertyAnnotation`, `getOACalculatedPropertyAnnotation`, `getOAOneAnnotation`, `getProperties`, `getCastNames`, `getFilterNames`, `getFilterParams`, `getFilterParamValues`, `getMethods`, `getClasses`.
- Declaration relationship: `<TYPE extends OAObject>`.

## Open Questions or Unclear Contracts

This document records architecture-level invariants inferred from the current implementation. Any Low/Medium confidence invariant should be verified with targeted tests before being treated as a release-blocking public guarantee.
