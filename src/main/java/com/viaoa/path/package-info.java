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
 * 
  * </p>
 */
package com.viaoa.path;

//CODEX unit tests <todo>

/* CODEX Invariants

PATH-PARSE-001 — Deterministic Path Text Interpretation
Contract statement:
For a given path string, root class, metadata model, strict/lenient mode, substitute class, and private-link policy,
OAPath must produce one deterministic interpretation of ordered segments, casts, filters, methods, classes, links,
and terminal value metadata.
Rationale:
OA paths are semantic navigation expressions used by graph traversal, Hubs, filters, finders, bindings, listeners,
serializers, projections, generated code, and runtime metadata interpretation. Nondeterministic parsing changes
executable graph behavior.
Source scope:
OAPath constructors, OAPath.setup overloads, getPropertyPath, getProperties, getCastNames, getFilterNames,
getFilterParams, getFilterParamValues, getMethods, getClasses, getLinkInfos.
Related CODEX findings:
OAPath source CODEX note identifies the 4.0 package move/renaming context; existing package invariants note path
parsing edge cases.
Suggested unit tests:
testParseSimplePropertyPathSegmentsInOrder, testParseNestedPropertyPathSegmentsInOrder,
testRepeatedParseProducesSameCompiledMetadata, testPathWithCastAndFilterParsesDeterministically.
Spec target section:
Path Runtime / Parsing Semantics

PATH-PARSE-002 — Segment Boundary Semantics
Contract statement:
Path delimiters, segment order, casts, and embedded filters must apply only to their intended segment. Leading,
trailing, repeated, or malformed delimiters must not create valid hidden segments or shift interpretation of later
segments.
Rationale:
Segment boundaries define graph navigation. Shifting one boundary can cause traversal through the wrong property,
link, subtype, or filter.
Source scope:
OAPath parsing/setup logic, getProperties, getCastNames, getFilterNames, getFilterConstructors, getClasses.
Related CODEX findings:
Existing package invariants note empty segment/path parsing and cast syntax/index issues.
Suggested unit tests:
testLeadingDotPathRejectedByContract, testRepeatedDotPathDoesNotResolveWrongProperty,
testPathCastResolvesSubtypeSegment, testCastSyntaxDoesNotConsumeFollowingProperty.
Spec target section:
Path Runtime / Segment Semantics

PATH-EMPTY-001 — Empty Path Contract
Contract statement:
An empty or blank path must have one explicit API-level meaning: either the root object itself or invalid path
input. That meaning must remain consistent across setup, evaluation, filters, finders, bindings, and generated
helpers.
Rationale:
Empty paths appear in dynamic runtime code. Ambiguous empty-path behavior can accidentally bypass traversal or
return the wrong object.
Source scope:
OAPath constructors, setup overloads, getValue, getValueAsString, getFirstPropertyName, getLastPropertyName.
Related CODEX findings:
Existing package invariants note empty path behavior reviewed.
Suggested unit tests:
testEmptyPathReturnsRootWhenContractRequires, testEmptyPathInvalidWhenContractRequires,
testBlankPathDoesNotResolveWrongProperty.
Spec target section:
Path Runtime / Empty Path Semantics

PATH-METADATA-001 — Metadata Is Path Authority
Contract statement:
Path resolution must use OA metadata as the authority for properties, links, calculated values, private links,
recursive links, annotations, and generated blueprint structure before fallback reflection behavior.
Rationale:
OA paths navigate executable blueprint metadata, not arbitrary JavaBean strings. OAObjectInfo, OAPropertyInfo,
OALinkInfo, and OACalcInfo define graph semantics.
Source scope:
OAPath.setup, getEndPropertyInfo, getEndCalcInfo, getEndLinkInfo, getOAPropertyAnnotation,
getOACalculatedPropertyAnnotation, getOAOneAnnotation, getLinkInfos, getRecursiveLinkInfos.
Related CODEX findings:
Existing package invariants note metadata lookup issues reviewed.
Suggested unit tests:
testPathResolvesMetadataProperty, testPathResolvesMetadataLink, testPathResolvesCalculatedMetadataProperty,
testPathUsesMetadataBeforeFallbackReflection.
Spec target section:
Path Runtime / Metadata-Driven Resolution

PATH-METADATA-002 — Stable Property Name Resolution
Contract statement:
Property, link, calculated-property, and method-style segment names must resolve using a defined case-sensitivity
and ambiguity rule. Resolution must not depend on reflection order, generated method order, JVM behavior, or
similarly named properties.
Rationale:
Generated model classes can contain related property names. Path interpretation must be stable across runtime
environments and code generation output.
Source scope:
OAPath.setup, property/method resolution, getMethods, getProperties, OAPathDelegate.createRootPropertyPath.
Related CODEX findings:
Existing package invariants note case-insensitive ambiguity reviewed in reflect/path scans.
Suggested unit tests:
testPathPropertyResolutionCaseByContract, testAmbiguousCasePropertyFailsOrUsesDefinedRule,
testPropertyResolutionDoesNotDependOnReflectionOrder.
Spec target section:
Path Runtime / Property Name Resolution

PATH-LINK-001 — Relationship Cardinality Controls Traversal
Contract statement:
OALinkInfo and related metadata must determine whether each relationship segment traverses one object, many objects,
a Hub, a recursive relationship, a private link, or a calculated reference.
Rationale:
Cardinality controls continuation, result type, finder/filter behavior, query translation, Hub/detail behavior, and
graph scope.
Source scope:
OAPath.getLinkInfos, getRecursiveLinkInfos, hasLinks, hasPrivateLink, getEndLinkInfo, getHasHubProperty,
getDoesLastMethodHasHubParam.
Related CODEX findings:
Existing package invariants note link traversal issues reviewed.
Suggested unit tests:
testOneLinkPathReturnsObject, testManyLinkPathReturnsHubOrCollectionByContract, testRecursiveLinkInfoDetected,
testPrivateLinkPolicyControlsResolution.
Spec target section:
Path Runtime / Link Semantics

PATH-TRAVERSE-001 — Ordered Object Graph Navigation
Contract statement:
Path evaluation against an object must apply each resolved segment to the current traversal value in sequence.
Traversal must not skip, repeat, reorder, or reinterpret segments after setup.
Rationale:
Path traversal is the foundation for object graph navigation, binding, filtering, finding, serialization,
projection, and digital twin runtime semantics.
Source scope:
OAPath.getValue(TYPE), getValue(OAObject,int), getValue(Hub,TYPE), getValue(Hub,TYPE,boolean), getLastLinkValue.
Related CODEX findings:
Existing package invariants note traversal correctness issues reviewed.
Suggested unit tests:
testObjectTraversalSimplePath, testObjectTraversalNestedPath, testHubTraversalDoesNotSkipSegment,
testRepeatedEvaluationUsesSameSegmentOrder.
Spec target section:
Path Runtime / Traversal Semantics

PATH-RETURN-001 — Terminal Value Type Semantics
Contract statement:
A valid path’s returned value must match the semantic type of the final segment: scalar property, OAObject
reference, Hub/collection relationship, calculated value, or formatted display string for string APIs.
Rationale:
Consumers use path results as typed runtime values. Returning a plausible value from another segment or previous
traversal corrupts UI, filter, query, serializer, and projection behavior.
Source scope:
OAPath.getValue, getValueAsString, getLastLinkValue, getEndPropertyInfo, getEndCalcInfo, getEndLinkInfo, getFormat.
Related CODEX findings:
Existing package invariants note object vs Hub return semantics reviewed.
Suggested unit tests:
testScalarFinalSegmentReturnsScalar, testObjectFinalSegmentReturnsObject, testHubFinalSegmentReturnsHubByContract,
testGetValueAsStringFormatsResolvedTerminalValue.
Spec target section:
Path Runtime / Return Semantics

PATH-HUB-001 — Hub Traversal Mode Is Explicit
Contract statement:
When a path is evaluated against or through a Hub, the API contract must determine whether traversal uses the active
object, the supplied from-object, links-only traversal, a Hub-valued terminal result, or all-member traversal
delegated to finder/filter logic.
Rationale:
Hub traversal ambiguity can produce wrong binding values, wrong filter membership, wrong query fallback, or
accidental graph broadening.
Source scope:
OAPath.getValue(Hub,TYPE), getValue(Hub,TYPE,boolean), getValueAsString(Hub,TYPE), getHasHubProperty,
getDoesLastMethodHasHubParam, finder/filter integration.
Related CODEX findings:
Existing package invariants note Hub traversal behavior reviewed.
Suggested unit tests:
testHubPathUsesActiveObjectWhenContractRequires, testHubPathUsesSuppliedFromObjectWhenProvided,
testHubPathLinksOnlyStopsAtLastLink, testHubFinalSegmentReturnsHubByContract.
Spec target section:
Path Runtime / Hub Traversal

PATH-DETAIL-001 — Master/Detail Scope Preservation
Contract statement:
Paths traversing detail links must remain scoped to the current master/object chain. Reverse paths and detail paths
must preserve link direction and must not broaden traversal to unrelated graph objects.
Rationale:
Master/detail scope is required for correct Hub contents, generated UI, graph traversal, query translation,
serialization, and projection semantics.
Source scope:
OAPath.getLinkInfos, getReversePropertyPath, getReversePropertyPath(boolean), getPropertyPathLinksOnly,
OAPathDelegate.getPropertyPathforClasses.
Related CODEX findings:
Existing package invariants note detail/path scope and reverse path query issues reviewed.
Suggested unit tests:
testDetailPathReturnsOnlyCurrentMasterDetail, testDetailPathNullMasterReturnsEmptyByContract,
testReversePropertyPathMatchesDetailTraversal, testReversePathPreservesLinkDirection.
Spec target section:
Path Runtime / Master Detail Semantics

PATH-CALC-001 — Calculated Properties Are First-Class Segments
Contract statement:
Calculated properties defined by OA metadata must be resolvable as path segments. They may terminate traversal as
scalar values or continue traversal only when their declared return type supports continuation.
Rationale:
Generated OA models expose calculated values as executable metadata. Bindings, filters, serializers, and projections
must treat them consistently with other path segments.
Source scope:
OAPath.getEndCalcInfo, getOACalculatedPropertyAnnotation, getMethods, getClasses, getValue, getValueAsString.
Related CODEX findings:
Existing package invariants note calculated traversal risks reviewed.
Suggested unit tests:
testPathResolvesCalculatedScalarProperty, testPathContinuesThroughCalculatedObjectProperty,
testCalculatedHubPropertyReturnSemanticsByContract.
Spec target section:
Path Runtime / Calculated Property Semantics

PATH-CALC-002 — Calculated Property Failure Boundary
Contract statement:
If calculated property invocation fails, OAPath must propagate the failure or return the documented fallback for the
active API mode. It must not reuse stale values, substitute unrelated values, or report successful traversal.
Rationale:
Calculated properties can drive UI, filters, listeners, serialization, and projections. Silent calculated-value
false success corrupts runtime state.
Source scope:
OAPath.getValue, getValueAsString, reflection invocation paths, strict/ignore-error setup and evaluation modes.
Related CODEX findings:
Existing package invariants note swallowed exception and wrong-output risks reviewed.
Suggested unit tests:
testCalculatedPropertyExceptionPropagatesOrFallbackByContract, testCalculatedPropertyFailureDoesNotUsePreviousValue,
testCalculatedPropertyFailureDoesNotReturnSiblingProperty.
Spec target section:
Path Runtime / Calculated Property Failure Semantics

PATH-NULL-001 — Null Traversal Semantics
Contract statement:
A null root or null intermediate value must produce the documented null/empty result or strict-mode failure. Null
traversal must stop only the affected branch and must not continue into stale, unrelated, or previously cached
object state.
Rationale:
Optional references are normal in OA graphs. Null behavior must be predictable for UI, filters, finders,
serializers, and generated runtime logic.
Source scope:
OAPath.getValue(TYPE), getValue(Hub,TYPE), getValue(OAObject,int), getValueAsString, getLastLinkValue.
Related CODEX findings:
Existing package invariants note null path traversal risks reviewed.
Suggested unit tests:
testNullRootPathReturnsDefinedResult, testNullRootPathDoesNotThrowUnexpectedNPE,
testTraversalStopsOnNullIntermediate, testNullIntermediateDoesNotUsePreviousTraversalValue.
Spec target section:
Path Runtime / Null Semantics

PATH-LOAD-001 — Unresolved And Lazy Reference Semantics
Contract statement:
Traversal over unresolved object-key references or unloaded lazy links must preserve unresolved/loadable state
unless resolution succeeds authoritatively. Path traversal must not convert unresolved state into authoritative null
or loaded-empty state.
Rationale:
OA lazy loading, sync, serialization, and graph retry behavior require unresolved references to remain retryable.
Source scope:
OAPath.getValue, getLastLinkValue, link traversal through OAObject references, Hub/detail traversal, object/
reference services.
Related CODEX findings:
Existing package invariants note unresolved reference and lazy-load state corruption risks reviewed.
Suggested unit tests:
testUnresolvedReferencePathDoesNotBecomeNull, testUnresolvedReferencePathCanResolveLater,
testFailedPathTraversalDoesNotMarkReferenceLoaded, testFailedPathTraversalDoesNotMarkHubEmpty.
Spec target section:
Path Runtime / Load-State Semantics

PATH-FAIL-001 — Invalid Path Failure Visibility
Contract statement:
Invalid syntax, missing properties, missing links, incompatible continuation types, invalid casts, unresolved
embedded filters, incompatible root classes, and malformed traversal structures must fail visibly or return the
documented lenient fallback. They must not silently resolve to a plausible wrong value.
Rationale:
Silent wrong-path success is dangerous in generated UI, security, filters, queries, serialization, projections, and
graph services.
Source scope:
OAPath constructors, setup overloads, getValue, getValueAsString, OAPathDelegate.createRootPropertyPath.
Related CODEX findings:
Existing package invariants note invalid path, false-success, and wrong-path behavior reviewed.
Suggested unit tests:
testInvalidPropertyPathThrowsOrFallbackByContract, testInvalidNestedPathDoesNotReturnWrongValue,
testWrongPathDoesNotReturnSimilarlyNamedProperty, testWrongPathFailureModeByContract.
Spec target section:
Path Runtime / Invalid Path Semantics

PATH-MODE-001 — Strict And Lenient Mode Consistency
Contract statement:
APIs that expose strict, ignore-error, or lenient behavior must honor that mode consistently during parsing, setup,
metadata resolution, dependency extraction, and evaluation.
Rationale:
Callers choose strict mode for contract enforcement and lenient mode for optional runtime paths. Mode drift either
hides contract violations or raises unexpected runtime failures.
Source scope:
OAPath(Class,String,boolean), setup(Class,boolean), setup(Hub,Class,boolean), setup(Hub,Class,Class,boolean),
getNeedsDataToVerify.
Related CODEX findings:
Existing package invariants note no-throw invalid path behavior reviewed.
Suggested unit tests:
testStrictInvalidPathThrows, testLenientInvalidPathReturnsDefinedFallback,
testIgnoreErrorModeDoesNotReturnWrongProperty, testNeedsDataToVerifyReportedByContract.
Spec target section:
Path Runtime / Error Mode Semantics

PATH-STATE-001 — No Partial Commit On Setup Or Traversal Failure
Contract statement:
Failed parsing, setup, metadata resolution, calculated property invocation, lazy resolution, or traversal must not
leave the OAPath instance or referenced graph/load state partially committed as if traversal succeeded.
Rationale:
OA paths are reused and can participate in live graph operations. Partial-progress false success causes stale
compiled metadata, corrupted load-state, and wrong later evaluation.
Source scope:
OAPath.setup overloads, getValue, getValueAsString, getLastLinkValue, getReversePropertyPath, lazy/reference
traversal services.
Related CODEX findings:
Existing package invariants note traversal failure must not mark references loaded or Hubs empty.
Suggested unit tests:
testSetupFailureDoesNotPublishPartialCompiledPath, testTraversalFailureDoesNotChangeCompiledMetadata,
testFailedPathTraversalDoesNotMarkReferenceLoaded, testFailedPathTraversalDoesNotMarkHubEmpty.
Spec target section:
Path Runtime / Partial Progress Semantics

PATH-REUSE-001 — Parsed Path Reuse Stability
Contract statement:
After successful setup, a parsed/resolved OAPath must be stable for repeated evaluation against compatible roots.
Compiled arrays and metadata must not mutate in ways that change behavior across evaluations.
Rationale:
OAPath instances are reused by filters, finders, sorters, bindings, listeners, serializers, projections, and
generated code.
Source scope:
OAPath compiled fields and accessors: getProperties, getMethods, getClasses, getLinkInfos, getRecursiveLinkInfos,
getFilterConstructors, getEndPropertyInfo, getEndLinkInfo, getEndCalcInfo.
Related CODEX findings:
Existing package invariants note caching/compiled path behavior reviewed.
Suggested unit tests:
testCompiledPathRepeatedEvaluationSameResult, testCompiledPathMetadataArraysRemainStableAfterEvaluation,
testRepeatedEvaluationDoesNotMutateCompiledPath.
Spec target section:
Path Runtime / Compiled Path Reuse

PATH-ROOT-001 — Root Class Compatibility
Contract statement:
A compiled path must be bound to a compatible root class or re-resolved for the actual root class. Metadata from one
generated model class must not be reused to traverse an incompatible class with similar property names.
Rationale:
Generated blueprint classes can share names while encoding different relationships. Cross-class path reuse corrupts
graph navigation and runtime metadata interpretation.
Source scope:
OAPath.fromClass, getFromClass, setup(Class), setup(Hub,Class,Class,boolean), substituteClass handling,
OAPathDelegate.
Related CODEX findings:
Existing package invariants note substitute/root class risks reviewed.
Suggested unit tests:
testCompiledPathRejectsIncompatibleRootClass, testSubclassRootUsesCompatibleCompiledPath,
testSubstituteClassPathResolvesAgainstCorrectMetadata.
Spec target section:
Path Runtime / Root Class Binding

PATH-THREAD-001 — Shared Path Evaluation Safety
Contract statement:
A successfully setup OAPath must either be safe for concurrent read-only evaluation or document caller
synchronization requirements. Concurrent evaluation must not corrupt compiled metadata, cached traversal state, or
runtime graph state.
Rationale:
Shared path definitions are used by Hubs, filters, finders, bindings, serializers, listeners, projections, and
generated code across runtime threads.
Source scope:
OAPath getValue/getValueAsString accessors, compiled metadata accessors, setup/evaluation state, OAPathDelegate
helper methods.
Related CODEX findings:
Existing package invariants note compiled path concurrent evaluation stability.
Suggested unit tests:
testCompiledPathConcurrentEvaluationStable, testConcurrentEvaluationDoesNotMutateCompiledMetadata,
testConcurrentSetupAndEvaluationRequiresDocumentedOwnership.
Spec target section:
Path Runtime / Thread Safety Semantics

PATH-REVERSE-001 — Reverse Path Metadata Semantics
Contract statement:
Reverse property path calculation must preserve OA metadata link direction, master/detail ownership, private-link
policy, and traversal compatibility.
Rationale:
Reverse paths are used by query translation, detail traversal, filters, datasource relationships, and graph
services. Wrong reverse paths invert or broaden relationship semantics.
Source scope:
OAPath.getReversePropertyPath, getReversePropertyPath(boolean), getPropertyPathLinksOnly,
OAPathDelegate.getPropertyPathforClasses.
Related CODEX findings:
Existing package invariants note reverse path/detail query issues reviewed.
Suggested unit tests:
testReversePropertyPathMatchesDetailTraversal, testReversePropertyPathHonorsPrivateLinkPolicy,
testReversePathRoundTripPreservesLinkScope.
Spec target section:
Path Runtime / Reverse Path Semantics

PATH-OBSERVE-001 — Dependent Path Semantics
Contract statement:
Paths used for bindings, listeners, filters, projections, and observable graph behavior must expose or preserve the
dependency chain needed to observe relevant root, link, calculated, and terminal property changes where OA supports
live updates.
Rationale:
OA paths are executable semantic contracts for live graph behavior. Dependent path drift causes stale UI, stale
projections, missed listener updates, and incorrect filter refreshes.
Source scope:
OAPath.getProperties, getLinkInfos, getEndPropertyInfo, getEndCalcInfo, getFormat, calculated property metadata,
listener/binding/projection integration.
Related CODEX findings:
Existing package invariants note UI binding dependency behavior.
Suggested unit tests:
testUiBindingPathResolvesValue, testUiBindingPathDependencyFiresOnNestedChange,
testCalculatedPropertyDependencyChainIsAvailable, testProjectionPathDependencyMatchesDirectTraversal.
Spec target section:
Path Runtime / Observable Graph Semantics

PATH-INTEGRATION-001 — Finder And Filter Alignment
Contract statement:
A path used by finders or filters must represent the same terminal values as direct OAPath evaluation for the same
root object, metadata, traversal mode, and lazy-load policy.
Rationale:
Filter and finder packages may optimize traversal, but they must not change path meaning. Divergence causes
mismatched Hub contents and graph results.
Source scope:
OAPath, OAFinder integration, OAFilter path-aware filters, getValue, getLinkInfos, getFilterConstructors.
Related CODEX findings:
Existing package invariants note filter/query integration reviewed.
Suggested unit tests:
testFilterPathMatchesDirectPathValue, testFinderPathMatchesDirectTraversal,
testPathFilterWithManyLinkMatchesFinderTraversal.
Spec target section:
Path Runtime / Finder and Filter Integration

PATH-INTEGRATION-002 — Query And Projection Translation Alignment
Contract statement:
When OA paths are translated for datasource queries, object-cache queries, projections, or generated runtime code,
translation must preserve root scope, link direction, detail scope, terminal property meaning, and metadata-defined
relationship semantics.
Rationale:
Query/projection results must match in-memory graph traversal expectations. Path translation must not silently
broaden, narrow, or invert graph scope.
Source scope:
OAPath metadata/link accessors, getReversePropertyPath, getPropertyPathLinksOnly, datasource/query/projection
integration.
Related CODEX findings:
Existing package invariants note reverse path/detail query issues reviewed.
Suggested unit tests:
testQueryPathTranslationPreservesLinkDirection, testQueryPathTranslationPreservesDetailScope,
testQueryPathTranslationMatchesDirectTraversal, testProjectionPathMatchesDirectPathValue.
Spec target section:
Path Runtime / Query and Projection Integration

PATH-INTEGRATION-003 — Serialization And Security Load Boundaries
Contract statement:
Paths used by serialization, security, export, or projection code must preserve path semantics without forcing
unintended lazy loads or expanding object graphs unless explicitly configured.
Rationale:
Serialization and security path traversal can expose data, alter load state, or expand large graphs. Path semantics
must respect runtime load boundaries.
Source scope:
OAPath.getValue/getLastLinkValue, link traversal, Hub/detail traversal, serializer/security/export integration.
Related CODEX findings:
Existing package invariants note serialization/lazy-load behavior reviewed.
Suggested unit tests:
testSerializationPathDoesNotForceLazyLoadByDefault, testSerializationPathForcesLoadWhenConfigured,
testSecurityPathResolvesSamePropertyAsDirectPath.
Spec target section:
Path Runtime / Serialization and Security Integration

PATH-DETERMINISM-001 — Same Inputs Produce Same Path Result
Contract statement:
For the same path text, root class, metadata, object/Hub state, strict/lenient mode, lazy-load policy, format,
private-link policy, and runtime context, OAPath must produce the same value or the same visible failure.
Rationale:
OA paths are AI-readable and runtime-readable semantic contracts over executable blueprints. Determinism is required
for digital twin runtime behavior, generated code, tests, and graph services.
Source scope:
All public/protected behavior in OAPath and OAPathDelegate.
Related CODEX findings:
Existing package invariants note false-success/wrong-path, caching, traversal, and lazy-load state risks reviewed.
Suggested unit tests:
testRepeatedPathEvaluationSameResult, testSamePathSameMetadataSameFailure,
testPathEvaluationDeterministicAcrossRepeatedSetup.
Spec target section:
Path Runtime / Determinism

PATH-AUTHORITY-001 — Path Package Is Semantic Authority
Contract statement:
com.viaoa.path is the package authority for parsing, resolving, and evaluating OA path semantics. Other packages may
optimize, wrap, or translate paths, but they must preserve OAPath meaning unless their own contract explicitly
narrows behavior.
Rationale:
Path semantics sit at the boundary between generated blueprint metadata and live object graph behavior. Central
authority prevents drift across finders, filters, queries, bindings, serializers, projections, security, and graph
services.
Source scope:
OAPath, OAPathDelegate, package integration with metadata, object, hub, finder, filter, query, datasource,
serialization, security, graph, and runtime services.
Related CODEX findings:
Existing package invariants note graph/path, query, serialization/lazy-load, and false-success boundary risks
reviewed.
Suggested unit tests:
testPathMatchesOAObjectMetadataSemantics, testPathMatchesHubDetailSemantics,
testPathDoesNotViolateSerializationLazyLoadContract, testPathSecurityAndFilterResolveSameProperty.
Spec target section:
Path Runtime / Cross-Package Authority

*/
