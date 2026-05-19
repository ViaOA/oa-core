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


/* CODEX Invariants

 1. Path Parsing Contracts

  PATH-PARSE-001 — Property Path Segments Are Parsed In Order
  Contract statement: An OAPath string must be parsed into ordered segments that preserve the exact traversal
  sequence.
  Rationale: Path order defines graph traversal and query/filter behavior.
  Source locations: OAPath, path parser/compiler helpers.
  Known related CODEX findings: path parsing edge cases were reviewed.
  Suggested unit tests: testParseSimplePropertyPathSegmentsInOrder(), testParseNestedPropertyPathSegmentsInOrder()
  Spec target section: Path Runtime / Parsing Semantics

  PATH-PARSE-002 — Empty Path Segments Must Not Become Valid Properties
  Contract statement: Empty segments from leading, trailing, or repeated delimiters must be rejected or handled by a
  defined fallback; they must not silently resolve to a wrong property.
  Rationale: Silent wrong-path behavior is worse than visible invalid-path failure.
  Source locations: OAPath parsing/validation methods.
  Known related CODEX findings: empty segment/path parsing issues reviewed.
  Suggested unit tests: testLeadingDotPathRejectedByContract(), testRepeatedDotPathDoesNotResolveWrongProperty()
  Spec target section: Path Runtime / Path Validation

  PATH-PARSE-003 — Cast Syntax Must Apply Only To The Intended Segment
  Contract statement: Path cast syntax must affect only the segment/class transition it is written for and must not
  shift delimiter parsing.
  Rationale: OA uses casts for model inheritance and subtype traversal.
  Source locations: OAPath, cast parsing logic, metadata resolution.
  Known related CODEX findings: cast syntax/index issues reviewed.
  Suggested unit tests: testPathCastResolvesSubtypeSegment(), testCastSyntaxDoesNotConsumeFollowingProperty()
  Spec target section: Path Runtime / Cast Parsing

  2. Metadata / Property Resolution Contracts

  PATH-METADATA-001 — Path Resolution Uses OA Metadata First
  Contract statement: OAPath must resolve properties and links through OA metadata/object info before using fallback
  reflection behavior.
  Rationale: OA metadata defines semantic graph structure, calculated properties, links, and Hub relationships.
  Source locations: OAPath, metadata lookup services, object info/link info/property info.
  Known related CODEX findings: metadata lookup issues reviewed.
  Suggested unit tests: testPathResolvesMetadataProperty(), testPathResolvesMetadataLink()
  Spec target section: Path Runtime / Metadata Resolution

  PATH-METADATA-002 — Property Name Resolution Must Be Deterministic
  Contract statement: Case handling and property-name matching must be consistent and must not choose different
  properties depending on reflection order.
  Rationale: Generated apps and filters require stable path behavior.
  Source locations: OAPath, OAReflect/property lookup helpers.
  Known related CODEX findings: case-insensitive ambiguity reviewed in reflect/path scans.
  Suggested unit tests: testPathPropertyResolutionCaseByContract(),
  testAmbiguousCasePropertyFailsOrUsesDefinedRule()
  Spec target section: Path Runtime / Property Name Resolution

  PATH-METADATA-003 — Link Metadata Determines Traversal Type
  Contract statement: Link metadata must determine whether a segment traverses to one object, many objects, a Hub,
  or a calculated reference.
  Rationale: Return type and traversal continuation depend on link cardinality.
  Source locations: OAPath, OALinkInfo, OAObjectInfo.
  Known related CODEX findings: link traversal issues reviewed.
  Suggested unit tests: testOneLinkPathReturnsObject(), testManyLinkPathReturnsHubOrCollectionByContract()
  Spec target section: Path Runtime / Link Semantics

  3. Object Traversal Contracts

  PATH-TRAVERSE-001 — Object Traversal Must Follow Segment Order
  Contract statement: Evaluating a path against an object must apply each segment to the current traversal value in
  sequence.
  Rationale: Path traversal is the foundation for UI binding, filters, finders, queries, and serialization.
  Source locations: OAPath.getValue(...), traversal helpers.
  Known related CODEX findings: traversal correctness issues reviewed.
  Suggested unit tests: testObjectTraversalSimplePath(), testObjectTraversalNestedPath()
  Spec target section: Path Runtime / Object Traversal

  PATH-TRAVERSE-002 — Traversal Must Stop On Null According To Contract
  Contract statement: If an intermediate object is null, traversal must return the defined null/empty result and
  must not continue into unrelated state.
  Rationale: Prevents NPEs and wrong values in UI/query paths.
  Source locations: OAPath.getValue(...), null traversal logic.
  Known related CODEX findings: null path traversal risks reviewed.
  Suggested unit tests: testTraversalStopsOnNullIntermediate(), testNullIntermediateReturnsDefinedValue()
  Spec target section: Path Runtime / Null Traversal

  PATH-TRAVERSE-003 — Object Return Semantics Must Match Final Segment Type
  Contract statement: The value returned by a path must match the final segment’s semantic type: scalar, object,
  Hub/collection, or calculated value.
  Rationale: Callers use OAPath for typed UI fields, filters, sorting, serialization, and query generation.
  Source locations: OAPath, return-type helpers.
  Known related CODEX findings: object vs Hub return semantics reviewed.
  Suggested unit tests: testScalarFinalSegmentReturnsScalar(), testObjectFinalSegmentReturnsObject(),
  testHubFinalSegmentReturnsHubByContract()
  Spec target section: Path Runtime / Return Semantics

  4. Hub / Detail Traversal Contracts

  PATH-HUB-001 — Hub Traversal Uses Active Object Or Collection Semantics By Contract
  Contract statement: When a path traverses through a Hub, OAPath must use the documented behavior: active object,
  all members, first member, or collection traversal depending on API.
  Rationale: Hub traversal ambiguity can produce wrong UI/filter/query results.
  Source locations: OAPath, Hub-aware traversal methods, finder/filter integration.
  Known related CODEX findings: Hub traversal behavior reviewed.
  Suggested unit tests: testHubPathUsesActiveObjectWhenContractRequires(),
  testHubPathCollectionTraversalWhenContractRequires()
  Spec target section: Path Runtime / Hub Traversal

  PATH-DETAIL-001 — Detail Path Traversal Must Respect Master/Detail Scope
  Contract statement: Paths that traverse detail links must remain scoped to the current master/object chain.
  Rationale: Detail traversal must not accidentally broaden to unrelated graph objects.
  Source locations: OAPath, OALinkInfo, Hub detail services.
  Known related CODEX findings: detail/path scope issues reviewed in datasource/graph scans.
  Suggested unit tests: testDetailPathReturnsOnlyCurrentMasterDetail(),
  testDetailPathNullMasterReturnsEmptyByContract()
  Spec target section: Path Runtime / Detail Traversal

  5. Calculated Property Contracts

  PATH-CALC-001 — Calculated Properties Are First-Class Path Segments
  Contract statement: Calculated properties defined by OA metadata must be resolvable and traversable like regular
  scalar/object properties when their return type supports continuation.
  Rationale: Generated models often expose calculated domain values.
  Source locations: OAPath, calculated property metadata, reflection helpers.
  Known related CODEX findings: calculated traversal risks reviewed.
  Suggested unit tests: testPathResolvesCalculatedScalarProperty(),
  testPathContinuesThroughCalculatedObjectProperty()
  Spec target section: Path Runtime / Calculated Properties

  PATH-CALC-002 — Calculated Property Failure Must Not Produce Wrong Value
  Contract statement: If calculated property evaluation fails, OAPath must propagate failure or return a defined
  fallback; it must not silently substitute unrelated values.
  Rationale: Calculated values are often used in UI, query-like filters, and reports.
  Source locations: OAPath, reflection invocation helpers.
  Known related CODEX findings: swallowed exception/wrong output risks reviewed.
  Suggested unit tests: testCalculatedPropertyExceptionPropagatesOrFallbackByContract(),
  testCalculatedPropertyFailureDoesNotUsePreviousValue()
  Spec target section: Path Runtime / Calculated Property Failure

  6. Null / Empty / Unresolved Reference Contracts

  PATH-NULL-001 — Null Root Has Defined Result
  Contract statement: Evaluating a path against a null root must return the documented null/empty result or fail
  with a defined exception.
  Rationale: UI bindings and filters often evaluate paths against optional objects.
  Source locations: OAPath.getValue(...), static path helpers.
  Known related CODEX findings: null handling reviewed.
  Suggested unit tests: testNullRootPathReturnsDefinedResult(), testNullRootPathDoesNotThrowUnexpectedNPE()
  Spec target section: Path Runtime / Null Root Semantics

  PATH-UNRESOLVED-001 — Unresolved References Must Not Become Authoritative Nulls
  Contract statement: Traversal over unresolved object-key references must preserve unresolved/loadable semantics
  unless authoritative resolution confirms null.
  Rationale: Lazy loading and sync references must remain retryable.
  Source locations: OAPath, object property/reference services.
  Known related CODEX findings: unresolved reference risks reviewed in graph/path scans.
  Suggested unit tests: testUnresolvedReferencePathDoesNotBecomeNull(), testUnresolvedReferencePathCanResolveLater()
  Spec target section: Path Runtime / Unresolved Reference Semantics

  PATH-EMPTY-001 — Empty Path Has Explicit Identity Or Invalid Semantics
  Contract statement: An empty path must either mean “the root object itself” or be invalid, according to the API
  contract; it must not behave inconsistently.
  Rationale: Empty paths appear in filters, finders, and generated helpers.
  Source locations: OAPath constructors/evaluation methods.
  Known related CODEX findings: empty path behavior reviewed.
  Suggested unit tests: testEmptyPathReturnsRootWhenContractRequires(), testEmptyPathInvalidWhenContractRequires()
  Spec target section: Path Runtime / Empty Path Semantics

  7. Invalid Path / Error Contracts

  PATH-ERROR-001 — Invalid Paths Must Fail Clearly Or Return Defined Fallback
  Contract statement: Invalid property/link paths must not silently resolve to a wrong property or unrelated value.
  Rationale: Silent wrong-path behavior corrupts UI binding, filters, queries, and serialization.
  Source locations: OAPath, metadata/reflection resolution.
  Known related CODEX findings: invalid path behavior was a scan focus.
  Suggested unit tests: testInvalidPropertyPathThrowsOrFallbackByContract(),
  testInvalidNestedPathDoesNotReturnWrongValue()
  Spec target section: Path Runtime / Invalid Path Semantics

  PATH-ERROR-002 — Failure Mode Must Respect Throw/No-Throw API Variant
  Contract statement: APIs that expose throw/no-throw behavior must honor that choice consistently.
  Rationale: Callers choose strict or permissive path handling based on context.
  Source locations: OAPath, path helper methods with exception flags.
  Known related CODEX findings: no-throw invalid path behavior reviewed.
  Suggested unit tests: testStrictInvalidPathThrows(), testLenientInvalidPathReturnsDefinedFallback()
  Spec target section: Path Runtime / Error Mode Semantics

  8. Path Caching / Reuse Contracts

  PATH-CACHE-001 — Compiled Path Reuse Must Be Immutable Or Thread-Safe
  Contract statement: Once compiled, path metadata/traversal arrays must not be mutated in a way that changes
  behavior across callers.
  Rationale: OAPath is reused in filters, sorting, UI binding, and generated code.
  Source locations: OAPath, cached method/link/property arrays.
  Known related CODEX findings: caching/compiled path behavior reviewed.
  Suggested unit tests: testCompiledPathRepeatedEvaluationSameResult(), testCompiledPathConcurrentEvaluationStable()
  Spec target section: Path Runtime / Compiled Path Reuse

  PATH-CACHE-002 — Cached Path Must Be Bound To Correct Root Class
  Contract statement: A compiled/cached path must be used only with compatible root classes or must re-resolve by
  class.
  Rationale: Reusing metadata from a different class can traverse wrong methods/properties.
  Source locations: OAPath constructors/cache state.
  Known related CODEX findings: substitute/root class risks reviewed.
  Suggested unit tests: testCompiledPathRejectsIncompatibleRootClass(), testSubclassRootUsesCompatibleCompiledPath()
  Spec target section: Path Runtime / Root Class Binding

  9. Query / Filter / Finder Integration Contracts

  PATH-FILTER-001 — Filters/Finder Path Evaluation Must Match Direct Path Evaluation
  Contract statement: A property path used by filters/finders must evaluate the same values as direct OAPath
  traversal for the same root object.
  Rationale: Query/filter logic must match UI/property binding semantics.
  Source locations: OAPath, OAFinder, OAFilter integration.
  Known related CODEX findings: filter/query integration reviewed.
  Suggested unit tests: testFilterPathMatchesDirectPathValue(), testFinderPathMatchesDirectTraversal()
  Spec target section: Path Runtime / Filter Integration

  PATH-QUERY-001 — Query Path Translation Must Preserve Path Semantics
  Contract statement: When OAPath is used to build datasource/query/filter expressions, translated paths must
  preserve traversal scope and link direction.
  Rationale: Query results must match in-memory traversal expectations.
  Source locations: OAPath, datasource/object-cache query integration.
  Known related CODEX findings: reverse path/detail query issues reviewed.
  Suggested unit tests: testReversePropertyPathMatchesDetailTraversal(),
  testQueryPathTranslationPreservesLinkDirection()
  Spec target section: Path Runtime / Query Integration

  10. Serialization / UI Binding Contracts

  PATH-UI-001 — UI Binding Paths Must Be Stable And Observable
  Contract statement: Paths used for UI binding must resolve consistently and must produce observable change
  dependencies where supported.
  Rationale: Generated UI depends on property paths for fields, columns, and detail views.
  Source locations: OAPath, UI binding integration, property path dependency helpers.
  Known related CODEX findings: none observed.
  Suggested unit tests: testUiBindingPathResolvesValue(), testUiBindingPathDependencyFiresOnNestedChange()
  Spec target section: Path Runtime / UI Binding

  PATH-SERIAL-001 — Serialization Paths Must Not Force Unintended Lazy Loads
  Contract statement: Paths used by serialization/security/export must not force lazy loading unless explicitly
  configured.
  Rationale: Serialization can otherwise expand large graphs unexpectedly or change loaded state.
  Source locations: OAPath, serializer integration, reference traversal options.
  Known related CODEX findings: serialization/lazy-load behavior reviewed.
  Suggested unit tests: testSerializationPathDoesNotForceLazyLoadByDefault(),
  testSerializationPathForcesLoadWhenConfigured()
  Spec target section: Path Runtime / Serialization Traversal

  11. Failure / Silent Wrong-Path Contracts

  PATH-FAILURE-001 — Wrong Path Must Not Produce Plausible Wrong Value
  Contract statement: If path resolution/traversal cannot satisfy the requested path, it must fail visibly or return
  a clearly defined fallback, not a plausible value from another property.
  Rationale: Silent wrong values are dangerous in generated UI, security rules, filters, and serialization.
  Source locations: OAPath, metadata/reflection resolution, calculated property invocation.
  Known related CODEX findings: false-success/wrong-path bugs reviewed.
  Suggested unit tests: testWrongPathDoesNotReturnSimilarlyNamedProperty(), testWrongPathFailureModeByContract()
  Spec target section: Path Runtime / Silent Wrong-Path Prevention

  PATH-FAILURE-002 — Traversal Failure Must Not Mark References Loaded
  Contract statement: A failed path traversal through lazy references must not mark the reference or Hub as loaded/
  empty.
  Rationale: Path evaluation should not corrupt lazy-load retry state.
  Source locations: OAPath, reference/object property services, Hub detail/select services.
  Known related CODEX findings: lazy-load state corruption issues reviewed.
  Suggested unit tests: testFailedPathTraversalDoesNotMarkReferenceLoaded(),
  testFailedPathTraversalDoesNotMarkHubEmpty()
  Spec target section: Path Runtime / Traversal Failure Semantics

  12. Test Coverage Matrix

  Parsing:

  - testParseSimplePropertyPathSegmentsInOrder
  - testParseNestedPropertyPathSegmentsInOrder
  - testLeadingDotPathRejectedByContract
  - testRepeatedDotPathDoesNotResolveWrongProperty
  - testPathCastResolvesSubtypeSegment
  - testCastSyntaxDoesNotConsumeFollowingProperty

  Metadata/property resolution:

  - testPathResolvesMetadataProperty
  - testPathResolvesMetadataLink
  - testPathPropertyResolutionCaseByContract
  - testAmbiguousCasePropertyFailsOrUsesDefinedRule
  - testOneLinkPathReturnsObject
  - testManyLinkPathReturnsHubOrCollectionByContract

  Traversal:

  - testObjectTraversalSimplePath
  - testObjectTraversalNestedPath
  - testTraversalStopsOnNullIntermediate
  - testScalarFinalSegmentReturnsScalar
  - testObjectFinalSegmentReturnsObject
  - testHubFinalSegmentReturnsHubByContract

  Hub/detail/calculated:

  - testHubPathUsesActiveObjectWhenContractRequires
  - testHubPathCollectionTraversalWhenContractRequires
  - testDetailPathReturnsOnlyCurrentMasterDetail
  - testPathResolvesCalculatedScalarProperty
  - testPathContinuesThroughCalculatedObjectProperty
  - testCalculatedPropertyExceptionPropagatesOrFallbackByContract

  Null/unresolved/invalid:

  - testNullRootPathReturnsDefinedResult
  - testUnresolvedReferencePathDoesNotBecomeNull
  - testUnresolvedReferencePathCanResolveLater
  - testEmptyPathReturnsRootWhenContractRequires
  - testStrictInvalidPathThrows
  - testLenientInvalidPathReturnsDefinedFallback

  Caching/reuse:

  - testCompiledPathRepeatedEvaluationSameResult
  - testCompiledPathConcurrentEvaluationStable
  - testCompiledPathRejectsIncompatibleRootClass
  - testSubclassRootUsesCompatibleCompiledPath

  Filter/query/UI/serialization:

  - testFilterPathMatchesDirectPathValue
  - testFinderPathMatchesDirectTraversal
  - testReversePropertyPathMatchesDetailTraversal
  - testQueryPathTranslationPreservesLinkDirection
  - testUiBindingPathResolvesValue
  - testUiBindingPathDependencyFiresOnNestedChange
  - testSerializationPathDoesNotForceLazyLoadByDefault

  Failure:

  - testWrongPathDoesNotReturnSimilarlyNamedProperty
  - testWrongPathFailureModeByContract
  - testFailedPathTraversalDoesNotMarkReferenceLoaded
  - testFailedPathTraversalDoesNotMarkHubEmpty


*/


