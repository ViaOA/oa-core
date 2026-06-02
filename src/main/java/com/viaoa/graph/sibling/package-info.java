/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.graph.sibling;

//CODEX unit tests 20260529

/* CODEX Invariants

GRAPH-SIBLING-001 — Sibling Helper Authority
Contract statement:
com.viaoa.graph.sibling is the semantic authority for discovering and resolving sibling-reference property paths
from a root Hub for lazy-reference prediction and related-reference prefetch behavior.
Rationale:
Sibling behavior is graph-runtime support for efficient related-reference loading and must remain separate from
general load, datasource, cache, object, and Hub contracts.
Source scope:
package-info.java, OASiblingHelper.
Related CODEX findings:
Existing package-info CODEX block is empty; OASiblingHelper CODEX notes same-thread semantic risk.
Suggested unit tests:
Verify OASiblingHelper learns and resolves sibling paths from a root Hub without requiring direct load/datasource
behavior tests.
Spec target section:
Sibling-reference runtime authority.

GRAPH-SIBLING-002 — Root Hub Scope
Contract statement:
Every sibling path learned or resolved by a helper is scoped to the root Hub and its object class; paths must not be
interpreted against unrelated Hub classes or unrelated graph scopes.
Rationale:
Sibling prefetch is meaningful only relative to a root collection and its metadata-defined graph neighborhood.
Source scope:
OASiblingHelper constructor, getHub, add, getPropertyPath.
Related CODEX findings:
None.
Suggested unit tests:
Verify helpers built for different root Hub classes do not resolve each other’s sibling paths.
Spec target section:
Graph-scoped sibling ownership.

GRAPH-SIBLING-003 — Metadata-Driven Discovery
Contract statement:
Sibling path discovery must use OAObjectInfo, OALinkInfo, OAPath, and calculated-property dependency metadata as the
authority for valid relationship segments.
Rationale:
Sibling references are semantic graph paths, not arbitrary strings; metadata determines which references may be
learned or prefetched.
Source scope:
OASiblingHelper.add, _add, onGetReference, _findNode.
Related CODEX findings:
None.
Suggested unit tests:
Verify valid link metadata is learned, invalid links are ignored or fail visibly, and calculated-property
dependencies expand into sibling paths.
Spec target section:
Metadata-driven sibling behavior.

GRAPH-SIBLING-004 — Deterministic Path Learning
Contract statement:
For the same root Hub metadata, explicit added paths, observed reference accesses, and calculated-property
dependencies, the learned sibling tree and resolved property paths must be deterministic.
Rationale:
Prefetch prediction must not produce different semantic paths for the same graph state and access pattern.
Source scope:
OASiblingHelper.add, onGetReference, getPropertyPath.
Related CODEX findings:
None.
Suggested unit tests:
Verify repeated add/reference-access sequences produce identical resolved property paths.
Spec target section:
Deterministic sibling-reference behavior.

GRAPH-SIBLING-005 — Semantic Noninterference
Contract statement:
Sibling discovery and prefetch support may improve access patterns but must not change the semantic result of object
reference resolution, Hub membership, cache identity, datasource results, or observable graph state.
Rationale:
Sibling behavior is an optimization/runtime aid; it must not become a hidden source of different graph meaning.
Source scope:
OASiblingHelper.
Related CODEX findings:
None.
Suggested unit tests:
Compare graph access results with sibling helper enabled and disabled for the same metadata and object graph.
Spec target section:
Lazy-loading/prefetch correctness.

GRAPH-SIBLING-006 — No Forced Lazy Loading During Discovery
Contract statement:
Learning or resolving sibling paths must not itself force unrelated lazy loads; it may record metadata and observed
reference access but must not materialize graph state outside documented prefetch/load boundaries.
Rationale:
Sibling helpers should predict and organize reference loading without causing hidden graph mutation or datasource
access.
Source scope:
OASiblingHelper class documentation, add, onGetReference, getPropertyPath.
Related CODEX findings:
Class documentation states the helper stores link metadata and never forces lazy loading.
Suggested unit tests:
Verify add/onGetReference/getPropertyPath do not trigger datasource loads for unrelated references.
Spec target section:
Lazy-reference discovery boundaries.

GRAPH-SIBLING-007 — Private Link Exclusion
Contract statement:
Sibling discovery must not add or resolve private-method links as public sibling-reference paths.
Rationale:
Private metadata links are not part of the public graph navigation contract for sibling prefetch behavior.
Source scope:
OASiblingHelper._add, _onGetReference, _findNode.
Related CODEX findings:
None.
Suggested unit tests:
Verify private links are ignored during explicit add, learned reference discovery, and path resolution.
Spec target section:
Metadata visibility boundaries.

GRAPH-SIBLING-008 — Calculated Dependency Expansion Boundary
Contract statement:
When a terminal path segment is a calculated property with declared dependencies, sibling discovery must expand
those dependency paths deterministically and with a bounded recursion policy.
Rationale:
Calculated sibling references should follow declared metadata dependencies without unbounded recursive expansion.
Source scope:
OASiblingHelper.add.
Related CODEX findings:
None.
Suggested unit tests:
Verify calculated-property dependencies are added, nested dependency expansion is bounded, and cycles do not cause
unbounded recursion.
Spec target section:
Calculated/dependent sibling path semantics.

GRAPH-SIBLING-009 — Missing Or Invalid Path Behavior
Contract statement:
Empty sibling paths are no-ops, and missing, invalid, private, non-link, or metadata-unresolvable path segments must
not be reported as successfully learned sibling paths.
Rationale:
False path learning would cause callers to prefetch or observe the wrong graph relationship.
Source scope:
OASiblingHelper.add, _add.
Related CODEX findings:
None.
Suggested unit tests:
Verify empty paths, unknown segments, scalar-only paths, private links, and invalid metadata do not produce false
property-path resolutions.
Spec target section:
Invalid sibling path failure semantics.

GRAPH-SIBLING-010 — Reference Access Learning
Contract statement:
When a reference access is reported, the helper may learn a sibling path only when the accessed object class and
property can be matched to root Hub metadata or already learned metadata paths.
Rationale:
Runtime reference observation must be constrained by metadata to avoid learning accidental or unrelated object
paths.
Source scope:
OASiblingHelper.onGetReference, _onGetReference.
Related CODEX findings:
None.
Suggested unit tests:
Verify observed references for reachable classes are learned and references for unrelated classes or null inputs are
ignored.
Spec target section:
Observed reference discovery semantics.

GRAPH-SIBLING-011 — Property Path Resolution Semantics
Contract statement:
getPropertyPath must return the metadata path from the root Hub to the requested object/property when one is known
or discoverable, and must return null when no safe sibling path exists.
Rationale:
Callers need a clear distinction between valid sibling path resolution and absence of a sibling-prefetch candidate.
Source scope:
OASiblingHelper.getPropertyPath.
Related CODEX findings:
None.
Suggested unit tests:
Verify direct, nested, learned, retry-discovered, and unresolved object/property combinations.
Spec target section:
Sibling path resolution behavior.

GRAPH-SIBLING-012 — Last-Found Search Optimization
Contract statement:
The last-found node optimization may affect search priority but must not change the semantic set of valid property
paths that can be resolved.
Rationale:
Search caching should improve performance without creating different graph meanings.
Source scope:
OASiblingHelper.nodeLastFound, getPropertyPath, _findNode.
Related CODEX findings:
SIB-SAME-THREAD-ENFORCED notes nodeLastFound mutation can produce wrong paths under accidental cross-thread reuse.
Suggested unit tests:
Verify bFromLastNode search behavior returns deterministic valid paths and does not hide other valid path
resolutions.
Spec target section:
Deterministic path-resolution caching.

GRAPH-SIBLING-013 — Same-Thread Usage Contract
Contract statement:
If a helper is configured for same-thread use, runtime lookup and use must be limited to the owning thread or must
fail/ignore the helper explicitly; cross-thread use must not silently reuse mutable path-resolution state.
Rationale:
OASiblingHelper mutates learned nodes and last-found state, so same-thread semantics must be enforced or made
explicit.
Source scope:
OASiblingHelper.setUseSameThread, getUseSameThread.
Related CODEX findings:
SIB-SAME-THREAD-ENFORCED notes setUseSameThread(true) is documented but not read anywhere in current source.
Suggested unit tests:
Verify a same-thread helper used from another thread is rejected, ignored, or otherwise documented with
deterministic behavior.
Spec target section:
ThreadLocal/runtime-context restoration and thread ownership.

GRAPH-SIBLING-014 — Concurrent Use Boundary
Contract statement:
A sibling helper is either thread-confined or must provide deterministic synchronization for concurrent add,
onGetReference, and getPropertyPath calls; callers must not observe corrupted learned-path state.
Rationale:
Sibling helpers maintain mutable node trees and cached search state.
Source scope:
OASiblingHelper.
Related CODEX findings:
SIB-SAME-THREAD-ENFORCED identifies cross-thread reuse risk for nodeLastFound and learned nodes.
Suggested unit tests:
Verify supported thread-confined behavior and, if shared use is supported, concurrent learning and resolution remain
deterministic.
Spec target section:
Concurrent sibling-reference correctness.

GRAPH-SIBLING-015 — Identity And Cache Nonduplication
Contract statement:
Sibling prefetch behavior must preserve OA identity/cache authority and must not create duplicate authoritative
objects for the same graph class/key identity.
Rationale:
Prefetching related references is only safe when object identity remains coherent with graph cache semantics.
Source scope:
OASiblingHelper as sibling-prefetch path authority; cache/load behavior as context only.
Related CODEX findings:
None.
Suggested unit tests:
Verify sibling-prefetched references resolve to canonical cached instances in representative graph scenarios.
Spec target section:
Identity/cache authority.

GRAPH-SIBLING-016 — Cache Datasource Boundary
Contract statement:
Sibling path discovery may identify candidate references for cache-first or datasource-backed loading, but success
of sibling path discovery must not imply cache hit, datasource load success, or semantic Object Graph load success.
Rationale:
Sibling discovery, cache lookup, datasource load, and graph semantic success are separate runtime boundaries.
Source scope:
OASiblingHelper.
Related CODEX findings:
None.
Suggested unit tests:
Verify callers can distinguish path discovery success from failed cache lookup or datasource-backed load.
Spec target section:
Datasource/cache boundary correctness.

GRAPH-SIBLING-017 — Null Missing And Unavailable Reference Semantics
Contract statement:
Null objects, null properties, missing metadata, unavailable references, deleted objects, detached objects, or
unloaded links must have deterministic sibling behavior and must not be treated as successfully resolved sibling
paths unless metadata and runtime state support that result.
Rationale:
Sibling prefetch must not turn missing or unavailable graph state into false navigation success.
Source scope:
OASiblingHelper.onGetReference, getPropertyPath, _findNode.
Related CODEX findings:
None.
Suggested unit tests:
Verify null inputs, missing links, deleted/detached objects, and unloaded references return no sibling path or fail
visibly according to contract.
Spec target section:
Null and unavailable sibling-reference behavior.

GRAPH-SIBLING-018 — Partial-Progress Visibility
Contract statement:
If sibling discovery or prefetch coordination fails after learning or loading some references, callers and runtime
services must be able to distinguish fully completed sibling behavior from partial sibling progress.
Rationale:
Sibling prefetch can span multiple related references and must not hide incomplete graph preparation as full
success.
Source scope:
OASiblingHelper and sibling-prefetch integration boundaries.
Related CODEX findings:
None.
Suggested unit tests:
Verify multi-reference sibling scenarios expose partial completion when one reference path fails or cannot be
loaded.
Spec target section:
Partial-progress visibility.

GRAPH-SIBLING-019 — False-Success Prevention
Contract statement:
Sibling discovery, path resolution, or prefetch coordination must not report success when required metadata, graph
scope, object identity, cache state, datasource state, or runtime context cannot safely support the sibling
relationship.
Rationale:
False sibling success can cause wrong related-object access, duplicate objects, or incorrect observable graph state.
Source scope:
OASiblingHelper.
Related CODEX findings:
SIB-SAME-THREAD-ENFORCED identifies an unenforced semantic claim that could produce wrong sibling paths.
Suggested unit tests:
Verify invalid metadata, unrelated object classes, cross-thread helper use, failed load boundaries, and unsupported
references do not produce successful sibling outcomes.
Spec target section:
False-success prevention.

GRAPH-SIBLING-020 — Observable Graph Noncontradiction
Contract statement:
When sibling references become available through prefetch behavior, observable graph state must remain consistent
with ordinary lazy-load and reference-access semantics, including event visibility where such loading is observable.
Rationale:
Sibling prefetch must not create a separate observable behavior model from normal graph navigation.
Source scope:
OASiblingHelper and graph/load integration context.
Related CODEX findings:
None.
Suggested unit tests:
Verify events or observable loaded-state changes caused by sibling prefetch match ordinary load/reference semantics.
Spec target section:
Observable/callable graph semantics.

GRAPH-SIBLING-021 — AI-Readable Sibling Contract
Contract statement:
Sibling-reference behavior must be documented and structured so generated tests, runtime verification, and OAi/MCP
tooling can infer root Hub scope, learned path semantics, invalid path behavior, thread ownership, and prefetch
success boundaries.
Rationale:
Sibling prefetch is part of executable blueprint traversal behavior and must be understandable as a semantic graph-
runtime contract.
Source scope:
package-info.java, OASiblingHelper.
Related CODEX findings:
Existing package-info CODEX block is empty; OASiblingHelper CODEX identifies implicit same-thread semantics.
Suggested unit tests:
Verify invariant coverage maps to root Hub scope, metadata path learning, runtime reference observation, thread
ownership, failure, and prefetch-boundary behavior.
Spec target section:
AI-readable architecture readiness.

*/

