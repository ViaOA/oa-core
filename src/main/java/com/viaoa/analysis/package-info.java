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
/**
 * <p>
 */
package com.viaoa.analysis;

/* CODEX Invariants

ANALYSIS-SCOPE-001 — Explicit Analysis Scope
Contract statement:
Analysis results must explicitly represent the analyzed scope: graph-scoped, package-scoped, class-scoped, object-
scoped, cache-scoped, or runtime-wide.
Rationale:
OA analysis can drive codegen, invariant docs, architecture reports, diagnostics, unit-test planning, and AI-
readable runtime interpretation; a partial scan that appears global creates false confidence.
Source scope:
OAObjectAnalyzer.load(); package-level com.viaoa.analysis; future analysis result/report APIs.
Related CODEX findings:
default-graph-only scan can omit non-default graph caches while appearing complete.
Suggested unit tests:
analysisReportsExplicitGraphScope(), analysisDoesNotClaimGlobalCompletenessForDefaultGraphOnlyScan(),
analysisVisitsAllConfiguredGraphsWhenGlobalScanRequested().
Spec target section:
Analysis Runtime / Scope and Completeness Semantics.

ANALYSIS-GRAPH-001 — Graph Ownership Semantics
Contract statement:
Object, Hub, cache, and metadata analysis must respect OARuntime graph ownership and package/class routing
semantics.
Rationale:
Object Graph correctness depends on graph authority; mixing default and package-owned graph state can produce
incorrect dependency, cache, Hub, or lifecycle reports.
Source scope:
OAObjectAnalyzer.load(); OARuntime.graph(); OARuntime.graph(Class<?>); OAGraphInternal.objectsInternal(); OAObject
cache traversal.
Related CODEX findings:
default graph class enumeration skips classes cached only in package graphs.
Suggested unit tests:
analysisUsesOwningGraphForClassCache(), analysisDoesNotMixDefaultAndPackageGraphMemberships(),
analysisScansPackageGraphObjects().
Spec target section:
Analysis Runtime / Graph Ownership Semantics.

ANALYSIS-METADATA-001 — Metadata as Runtime Truth
Contract statement:
Analysis that interprets OA classes, properties, links, ownership, cardinality, calculated values, persistence
behavior, serialization behavior, or distributed behavior must use OA metadata services as runtime truth.
Rationale:
Metadata is the executable semantic bridge for OAObject, Hub, path, datasource, serialization, trigger, sync,
replication, and graph behavior.
Source scope:
com.viaoa.analysis package; future metadata-aware analyzers; integration with metadata, annotation, reflect, object,
hub, graph, path, query, datasource, serialization, sync, and replication packages.
Related CODEX findings:
none observed in current implementation.
Suggested unit tests:
analysisUsesOAObjectInfoForClassSemantics(), analysisUsesOALinkInfoForRelationshipCardinality(),
analysisUsesCalcInfoForCalculatedDependencies().
Spec target section:
Analysis Runtime / Metadata Semantics.

ANALYSIS-PATH-001 — Path and Dependency Discovery Semantics
Contract statement:
Path/property/dependency analysis must follow OAPath and metadata semantics, including property resolution, link
traversal, Hub/detail traversal, calculated properties, null/unresolved references, and invalid-path behavior.
Rationale:
Analysis output can drive generated guidance and tests; wrong path interpretation creates false dependencies or
misses real runtime dependencies.
Source scope:
com.viaoa.analysis package; future path/dependency analyzers; integration with path, metadata, object, hub, query,
filter, find, and template packages.
Related CODEX findings:
none observed in current implementation.
Suggested unit tests:
analysisResolvesPathUsingOAPathSemantics(), analysisRejectsInvalidPathVisibly(),
analysisHandlesCalculatedPropertyPathDependencies().
Spec target section:
Analysis Runtime / Path and Dependency Semantics.

ANALYSIS-TRAVERSE-001 — Object and Hub Traversal Semantics
Contract statement:
Object and Hub traversal during analysis must preserve OA identity, Hub membership, detail/master links, shared-Hub
semantics, ownership, ordering where analyzed, and active-object semantics where reported.
Rationale:
Analysis is useful only when it describes the graph shape and relationships that OA runtime services actually
observe.
Source scope:
OAObjectAnalyzer.load(); OAGraphInternal.internal().objects().hub().GetHubReferences; OAObject; Hub.
Related CODEX findings:
none observed beyond graph-scope completeness.
Suggested unit tests:
analysisCountsOnlyCurrentHubReferences(), analysisPreservesDetailHubRelationshipSemantics(),
analysisReportsSharedHubMembershipsWithoutDuplicates().
Spec target section:
Analysis Runtime / Object and Hub Traversal Semantics.

ANALYSIS-RECURSIVE-001 — Recursive Traversal Protection
Contract statement:
Recursive or cyclic graph analysis must use visited-state tracking that prevents infinite traversal without
suppressing legitimate distinct reachable nodes.
Rationale:
OA graphs commonly contain reverse links, parent/child relationships, shared Hubs, and recursive models; production
graph analysis must terminate safely.
Source scope:
com.viaoa.analysis package; future recursive analyzers; integration with graph, find, path, cascade, object, and hub
traversal contracts.
Related CODEX findings:
none observed in current implementation.
Suggested unit tests:
analysisTerminatesOnCyclicObjectGraph(), analysisStillVisitsDistinctReachableObjectsInCycle(),
analysisBoundsRecursiveDetailTraversal().
Spec target section:
Analysis Runtime / Recursive Traversal Semantics.

ANALYSIS-CACHE-001 — Cache and Identity Analysis
Contract statement:
Cache analysis must distinguish live cache contents, stale weak-reference artifacts, unloaded references, duplicate
identity candidates, graph-owned cache state, GUID identity, object-key identity, and business-key identity.
Rationale:
OA cache state is central to identity and graph correctness; analysis must not confuse stale or graph-local cache
state with authoritative live object identity.
Source scope:
OAObjectAnalyzer.load(); OAObjectCacheService.getClasses/getTotal/callback behavior; OAObjectCache; graph/cache int
egration boundaries.
Related CODEX findings:
graph-scope scan affects cache completeness.
Suggested unit tests:
analysisIgnoresClearedWeakCacheEntries(), analysisReportsGraphSpecificCacheTotals(),
analysisDoesNotConflateGuidAndBusinessKeyIdentity().
Spec target section:
Analysis Runtime / Cache and Identity Semantics.

ANALYSIS-RESULT-001 — Per-Run Result Isolation
Contract statement:
Analysis result state must be isolated per run unless explicit cumulative behavior is documented and surfaced in the
result model.
Rationale:
Stale analysis state creates false positives, false negatives, and misleading diagnostics across repeated runs.
Source scope:
OAObjectAnalyzer.hsHub; OAObjectAnalyzer.load(); future analysis result containers.
Related CODEX findings:
hsHub is an instance field and is not cleared by load().
Suggested unit tests:
analysisClearsRunStateBeforeLoad(), repeatedAnalysisDoesNotRetainRemovedHubReferences(),
concurrentAnalyzerInstancesDoNotShareResults().
Spec target section:
Analysis Runtime / Result State Isolation.

ANALYSIS-STATE-001 — Temporary State Cleanup
Contract statement:
Temporary analysis state must be cleaned up on success and failure and must not retain strong references to
OAObjects, Hubs, graphs, metadata, callbacks, or caches longer than the analysis contract requires.
Rationale:
Analysis can run over large live graphs; retained diagnostic state can become a memory leak or keep stale runtime
objects alive.
Source scope:
OAObjectAnalyzer.hsHub; OAObjectAnalyzer.load(); future analyzers and report builders.
Related CODEX findings:
retained hsHub can keep old Hub references across scans.
Suggested unit tests:
analysisDoesNotRetainTemporaryHubStateAfterRun(), analysisCleanupRunsAfterCallbackException(),
analysisCanReleaseLargeGraphAfterRun().
Spec target section:
Analysis Runtime / Temporary State Cleanup Semantics.

ANALYSIS-FAIL-001 — Analysis Failure Visibility
Contract statement:
Analysis failures must be caller-visible or explicitly recorded in the analysis result; failed traversal, metadata
lookup, path resolution, callback execution, graph inspection, or cache inspection must not silently produce a
complete-looking report.
Rationale:
Silent false-success in analysis can encode wrong assumptions into generated specs, tests, code, diagnostics, and AI
reasoning.
Source scope:
OAObjectAnalyzer.load(); com.viaoa.analysis package; future report/result APIs.
Related CODEX findings:
default-graph-only behavior creates silent incompleteness; no structured result currently records scan scope or
failures.
Suggested unit tests:
analysisFailureIsReportedInResult(), callbackExceptionDoesNotProduceCompleteReport(),
missingMetadataIsReportedNotIgnored().
Spec target section:
Analysis Runtime / Failure and False-Success Prevention.

ANALYSIS-PARTIAL-001 — Partial Progress Visibility
Contract statement:
If analysis inspects only part of the requested scope, the result must mark partial progress and identify unvisited,
failed, inaccessible, or unsupported portions.
Rationale:
Partial reports are useful only when downstream consumers know what was and was not inspected.
Source scope:
OAObjectAnalyzer.load(); future graph/cache/path/metadata analyzers; report/result APIs.
Related CODEX findings:
default graph scan can omit package graphs without marking partial scope.
Suggested unit tests:
analysisPartialGraphScanMarksIncomplete(), analysisUnsupportedInspectionIsReported(),
analysisResultListsSkippedScopeElements().
Spec target section:
Analysis Runtime / Partial Progress Semantics.

ANALYSIS-UNCERTAIN-001 — Uncertainty and False Finding Control
Contract statement:
Analysis must distinguish confirmed inconsistency, missing data, inaccessible state, unsupported analysis, and
uncertain inference; uncertain conclusions must not be reported as facts.
Rationale:
OA analysis feeds hardening, test generation, and semantic documentation, where false positives and false negatives
can misdirect engineering work.
Source scope:
com.viaoa.analysis package; future model/report/result APIs.
Related CODEX findings:
stale/cross-run result risk can produce false positives or false negatives.
Suggested unit tests:
analysisMarksIncompletePathAsUncertain(), analysisDoesNotReportRemovedHubAsCurrent(),
analysisDoesNotHideUnresolvedMetadata().
Spec target section:
Analysis Runtime / Report Correctness Semantics.

ANALYSIS-OBSERVE-001 — Read-Only Runtime Inspection
Contract statement:
Analysis must observe runtime state without mutating OAObjects, Hubs, graph ownership, cache identity, lifecycle
flags, datasource state, sync state, or replication state unless a diagnostic side effect is explicitly documented.
Rationale:
Analysis should describe OA runtime truth, not change it; mutation during inspection can create production
correctness drift.
Source scope:
OAObjectAnalyzer.load(); OARuntime; OAGraphInternal; OAObject; Hub; cache traversal APIs.
Related CODEX findings:
none observed for mutation; current code reads Hub references and cache contents.
Suggested unit tests:
analysisDoesNotMutateHubMembership(), analysisDoesNotTriggerLazyLoadUnlessConfigured(),
analysisDoesNotChangeObjectLifecycleFlags().
Spec target section:
Analysis Runtime / Read-Only Inspection Semantics.

ANALYSIS-CONSISTENCY-001 — Consistency Check Interpretation
Contract statement:
Consistency checks must define whether a finding means violated invariant, suspicious state, missing data,
inaccessible state, unsupported check, or informational diagnostic.
Rationale:
Analysis findings are used for runtime verification and hardening; result meaning must be precise enough for tests
and owner decisions.
Source scope:
com.viaoa.analysis package; future consistency-check/report APIs; integration with metadata, graph, object, hub,
cache, datasource, serialization, sync, and replication packages.
Related CODEX findings:
none observed directly.
Suggested unit tests:
analysisFindingSeverityIsExplicit(), analysisMissingDataIsNotReportedAsViolation(),
analysisUnsupportedCheckIsReportedSeparately().
Spec target section:
Analysis Runtime / Consistency Check Semantics.

ANALYSIS-OUTPUT-001 — Deterministic Diagnostic Output
Contract statement:
Analysis output must be deterministic for the same runtime state, metadata state, graph scope, and analysis options.
Rationale:
Diagnostics, generated docs, invariant extraction, unit-test planning, and AI/MCP semantic reasoning require stable
output.
Source scope:
OAObjectAnalyzer.load() console output; future structured result/report APIs.
Related CODEX findings:
console-only output amplifies silent-incomplete analysis risk.
Suggested unit tests:
analysisOutputIsDeterministicForStableGraph(), analysisReportOrderingIsStable(),
analysisStructuredResultIsStableAcrossRuns().
Spec target section:
Analysis Runtime / Diagnostic Output Semantics.

ANALYSIS-TOOLING-001 — Structured Tooling Result Contract
Contract statement:
Analysis APIs used by codegen, specs, invariant extraction, unit-test generation, Javadocs, MCP/OAi reasoning, or
runtime verification must provide structured results with scope, findings, failures, and completeness metadata.
Rationale:
Tooling needs machine-checkable results, not only ad hoc console diagnostics.
Source scope:
OAObjectAnalyzer.load() currently prints to System.out; com.viaoa.analysis package; future tooling APIs.
Related CODEX findings:
console-only output is not a correctness bug alone, but it worsens silent-incomplete analysis risk.
Suggested unit tests:
analysisReturnsStructuredResultWithScope(), analysisResultIncludesFailures(),
analysisResultIncludesCompletenessMetadata().
Spec target section:
Analysis Runtime / Tooling and Verification Semantics.

ANALYSIS-CONCURRENT-001 — Concurrent Analysis Safety
Contract statement:
Concurrent analysis runs must not corrupt shared state, race mutable result containers, or expose partially
accumulated results as complete; analyzers must be thread-safe, externally synchronized, or explicitly single-use.
Rationale:
Analysis may run in diagnostics, background monitoring, tests, or tooling while the runtime is active.
Source scope:
OAObjectAnalyzer.hsHub; OAObjectAnalyzer.load(); runtime graph/cache traversal APIs; future shared analyzers.
Related CODEX findings:
instance-level mutable HashSet<Hub> is not synchronized and is mutated during callback traversal.
Suggested unit tests:
concurrentAnalysisLoadCallsDoNotCorruptAnalyzerState(), analyzerResultSnapshotStableDuringConcurrentGraphMutation(),
analyzerRejectsConcurrentReuseIfNotThreadSafe().
Spec target section:
Analysis Runtime / Concurrency Semantics.

ANALYSIS-TL-001 — Runtime Context Restoration
Contract statement:
Any analysis code that sets ThreadLocal, graph context, security context, datasource context, sync/replication
context, or runtime inspection mode must restore prior state with try/finally.
Rationale:
Analysis may run on shared worker or tooling threads; leaked context can affect later Object Graph operations.
Source scope:
com.viaoa.analysis package; future analyzers that use runtime context; integration boundaries with runtime, graph,
datasource, security, sync, and replication packages.
Related CODEX findings:
none observed in current implementation.
Suggested unit tests:
analysisThreadLocalRestoredAfterSuccess(), analysisThreadLocalRestoredAfterFailure(),
analysisInspectionContextDoesNotLeakToRuntimeOperations().
Spec target section:
Analysis Runtime / ThreadLocal Context Semantics.

ANALYSIS-BOUNDARY-001 — Analysis Success Versus Object Graph Correctness
Contract statement:
Successful analysis means the requested inspection completed according to its scope and options; it must not imply
that the Object Graph, metadata, datasource, serialization, sync, replication, or runtime state is semantically
correct unless specific checks verified that correctness.
Rationale:
Analysis is an inspection boundary, not the authority for all runtime correctness.
Source scope:
OAObjectAnalyzer; com.viaoa.analysis package; cross-package boundaries with metadata, object, hub, graph,
datasource, cache, serialization, sync, replication, validation, and runtime.
Related CODEX findings:
none observed beyond false-completeness findings.
Suggested unit tests:
analysisSuccessDoesNotImplyGraphIsValid(), analysisSuccessDoesNotImplyMetadataConsistency(),
analysisSuccessDoesNotImplyDatasourceCorrectness().
Spec target section:
Analysis Runtime / Runtime Boundary Semantics.

ANALYSIS-INTEGRATION-001 — Cross-Package Semantic Compatibility
Contract statement:
Analysis behavior must remain compatible with metadata, annotation, reflect, path, query, object, Hub, graph,
runtime, cache, find, filter, select, datasource, serialization, sync, replication, validation, and codegen
contracts.
Rationale:
Analysis must inspect OA runtime truth using the same semantic authorities as the runtime, or its findings will not
match actual Object Graph behavior.
Source scope:
OAObjectAnalyzer; com.viaoa.analysis package; cross-package analysis integrations.
Related CODEX findings:
graph-scope and stale-result findings illustrate cross-package completeness risk.
Suggested unit tests:
analysisUsesMetadataPathAndGraphContractsConsistently(), analysisCacheAndHubReportsMatchRuntimeState(),
analysisCodegenDependencyDiscoveryMatchesRuntimeMetadata().
Spec target section:
Analysis Runtime / Cross-Package Integration Semantics.

*/

