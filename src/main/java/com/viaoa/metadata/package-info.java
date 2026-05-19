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
package com.viaoa.metadata;


/* CODEX Invariants

1. Metadata Runtime Contracts

  META-RUNTIME-001 — Metadata Is Runtime Truth
  Contract statement: OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, and OAMethodInfo define authoritative
  runtime semantics for OAObjects. Runtime services must not infer conflicting semantics from reflection, datasource
  state, or object instances once metadata is initialized.
  Rationale: Graph routing, Hub relationships, path traversal, serialization, datasource behavior, sync, and
  replication all depend on metadata being the single semantic source.
  Source locations: OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, OAMethodInfo, OAObjectInfoService,
  OAObjectAnnotationService
  Known related CODEX findings: none observed.
  Suggested unit tests: testMetadataIsAuthoritativeForPropertyLookup(), testRuntimeUsesMetadataLinkSemantics()
  Spec target section: Metadata Runtime / Runtime Truth

  META-RUNTIME-002 — Optional Metadata Absence Is Safe
  Contract statement: Optional metadata arrays, flags, paths, and annotation-derived values must behave as absent/
  false/empty when unset; presence-check APIs must not throw.
  Rationale: Optional metadata is common in generated models. Runtime callers must safely ask whether import-match,
  context, view, POJO, or mapping metadata exists.
  Source locations: OAObjectInfo.hasImportMatchProperties, getImportMatchPropertyNames, getViewDependentProperties,
  getContextDependentProperties
  Known related CODEX findings: fixed null import-match presence check.
  Suggested unit tests: testUnsetOptionalMetadataPresenceChecksReturnFalse(),
  testUnsetOptionalMetadataArraysAreSafeToQuery()
  Spec target section: Metadata Runtime / Optional Metadata

  2. OAObjectInfo Contracts

  META-OBJECTINFO-001 — OAObjectInfo Identifies Exactly One OA Class
  Contract statement: Every usable OAObjectInfo must be associated with its owning OAObject class through
  getForClass() / setForClass(), and combined inherited metadata must still route to the concrete class.
  Rationale: Graph lookup, datasource routing, cache ownership, POJO generation, path parsing, triggers, and sync role
  behavior all depend on class identity.
  Source locations: OAObjectInfo, OAObjectInfoService._getOAObjectInfo, createCombinedObjectInfo
  Known related CODEX findings: none observed.
  Suggested unit tests: testCombinedObjectInfoUsesConcreteClass(),
  testObjectInfoClassIdentitySurvivesInheritanceMerge()
  Spec target section: Metadata Runtime / ObjectInfo Identity

  META-OBJECTINFO-002 — Display Metadata Defaults Must Not Be Suppressed By Empty Annotation Values
  Contract statement: Empty annotation defaults must not override computed defaults. Display name defaults to simple
  class name; plural name defaults to pluralized simple class name unless explicitly set.
  Rationale: Empty display metadata silently breaks generated UI, POJO labels, diagnostics, and model-designer output.
  Source locations: OAObjectInfo.getDisplayName, getPluralName, OAObjectAnnotationService,
  OAObjectInfoService.createCombinedObjectInfo
  Known related CODEX findings: fixed empty annotation display/plural behavior and plural fallback.
  Suggested unit tests: testEmptyOAClassDisplayNameUsesDefault(), testPluralNameUsesSimpleClassNameFallback()
  Spec target section: Metadata Runtime / Display Metadata

  3. ID / Key Property Contracts

  META-ID-001 — ID Property Names Define Object Key Semantics
  Contract statement: OAObjectInfo.idProperties and OAPropertyInfo.id/key flags must agree for identity, object-key c
  onstruction, cache lookup, datasource matching, serialization identity, and replication identity.
  Rationale: Any mismatch can create duplicate objects, stale cache entries, failed select matching, or replication
  divergence.
  Source locations: OAObjectInfo.getIdProperties, isIdProperty, getKeyProperties, OAPropertyInfo.getId, getKey,
  OAObjectInfoService.initialize
  Known related CODEX findings: none observed.
  Suggested unit tests: testIdPropertiesMarkPropertyInfoAsId(), testKeyPropertiesMatchObjectKeyConstruction()
  Spec target section: Metadata Runtime / Identity Metadata

  META-ID-002 — POJO Key Metadata Must Match OA Key Semantics
  Contract statement: POJO key positions must represent OA primary keys, import-match keys, or declared unique-link
  keys consistently and deterministically.
  Rationale: Deserialization/import needs stable matching to existing OAObjects without creating duplicates.
  Source locations: OAObjectPojoLoader.markAllPojoPropertyKeys, PojoDelegate.hasPkey, hasImportMatchKey,
  hasLinkUniqueKey
  Known related CODEX findings: accepted POJO-loader path noted by owner.
  Suggested unit tests: testPojoPrimaryKeyMetadataMatchesObjectInfoKey(),
  testPojoImportMatchKeyMetadataIsDeterministic()
  Spec target section: Metadata Runtime / POJO Key Semantics

  4. Property Metadata Contracts

  META-PROPERTY-001 — Property Name Lookup Is Case-Insensitive And Cache-Consistent
  Contract statement: Property metadata lookup by name must be case-insensitive, and lookup caches must be reset
  whenever property metadata is added or replaced through supported paths.
  Rationale: Path, filter, query, reflect, serialize, and datasource code depend on stable name resolution.
  Source locations: OAObjectInfo.getPropertyInfo, addPropertyInfo, resetPropertyInfo, OAPropertyInfo.getName,
  getLowerName
  Known related CODEX findings: fixed inheritance merge to use addPropertyInfo.
  Suggested unit tests: testPropertyLookupIsCaseInsensitive(), testPropertyLookupCacheResetsAfterAdd()
  Spec target section: Metadata Runtime / Property Lookup

  META-PROPERTY-002 — Primitive Null Semantics Must Be Metadata-Driven
  Contract statement: Primitive class type, trackPrimitiveNull, and primitive-property arrays must consistently define
  which primitive fields participate in OA primitive-null tracking.
  Rationale: OA primitive-null semantics affect object property reads, serialization, datasource writes, and equality/
  filter behavior.
  Source locations: OAPropertyInfo.setClassType, getTrackPrimitiveNull, OAObjectInfo.getPrimitiveProperties,
  OAObjectInfoService.initialize
  Known related CODEX findings: none observed.
  Suggested unit tests: testPrimitiveNullTrackedForConfiguredPrimitiveProperty(),
  testPrimitivePropertyListIsStableAcrossInheritance()
  Spec target section: Metadata Runtime / Primitive Null Semantics

  5. Link / Reverse-Link Contracts

  META-LINK-001 — Link Metadata Defines Cardinality And Target Type
  Contract statement: Every OALinkInfo must accurately define name, target class, and cardinality (TYPE_ONE or
  TYPE_MANY). These values drive Hub/detail behavior, path traversal, datasource reference loading, and serialization.
  Rationale: Wrong cardinality or target class corrupts object graph shape.
  Source locations: OALinkInfo.getName, getToClass, getType, isOne, isMany, OAObjectInfo.getLinkInfo
  Known related CODEX findings: none observed.
  Suggested unit tests: testLinkInfoCardinalityDrivesPathReturnType(), testLinkInfoTargetClassMatchesMetadataLookup()
  Spec target section: Metadata Runtime / Link Semantics

  META-LINK-002 — Reverse-Link Resolution Must Be Stable
  Contract statement: If reverseName is defined, getReverseLinkInfo() must resolve to the target object’s matching
  link metadata and remain consistent after metadata initialization.
  Rationale: Reverse links drive Hub membership, ownership, cascade, trigger reverse traversal, path reversal, and
  sync propagation.
  Source locations: OALinkInfo.getReverseName, setReverseName, getReverseLinkInfo, OAObjectInfoService reverse-link
  setup
  Known related CODEX findings: none observed.
  Suggested unit tests: testReverseLinkResolvesByNameIgnoringCase(), testSyntheticReverseLinkCreatedForOneSidedMany()
  Spec target section: Metadata Runtime / Reverse Link Semantics

  6. Ownership / Cascade Contracts

  META-OWNERSHIP-001 — Ownership Metadata Controls Lifecycle Semantics
  Contract statement: OALinkInfo.owner, reverse-owner state, and owned-link caches must consistently define object
  ownership and one-owner relationships.
  Rationale: Save/delete cascade, referenceability, delete restrictions, and graph ownership depend on this metadata.
  Source locations: OALinkInfo.getOwner, setOwner, OAObjectInfo.getOwnedLinkInfos, getOwnedByOne,
  isOwnedAndNoReverseMany
  Known related CODEX findings: none observed.
  Suggested unit tests: testOwnedLinkInfosReflectOwnerLinks(), testOwnedByOneUsesReverseOwnerMetadata()
  Spec target section: Metadata Runtime / Ownership Semantics

  META-CASCADE-001 — Cascade Metadata Must Match Save/Delete Runtime Behavior
  Contract statement: cascadeSave, cascadeDelete, mustBeEmptyForDelete, and ownership flags must be interpreted
  consistently by object and Hub save/delete services.
  Rationale: Cascade metadata defines whether related objects are saved, deleted, blocked, or left untouched.
  Source locations: OALinkInfo.getCascadeSave, getCascadeDelete, getMustBeEmptyForDelete, object save/delete services
  Known related CODEX findings: none observed.
  Suggested unit tests: testCascadeSaveFollowsLinkMetadata(), testMustBeEmptyForDeleteBlocksDeleteWhenLinkHasMembers()
  Spec target section: Metadata Runtime / Cascade Semantics

  7. Calculated Property Contracts

  META-CALC-001 — Calculated Property Metadata Must Include Dependencies
  Contract statement: Every calculated property must expose a stable name, return type, dependency paths, and hub-
  calculation flag when applicable.
  Rationale: Triggers, UI refresh, serialization, path traversal, and reflection depend on calculated-property
  metadata.
  Source locations: OACalcInfo, OAObjectInfo.addCalcInfo, getCalcInfo, OAObjectAnnotationService calculated-property
  loading
  Known related CODEX findings: fixed hub-calc index consistency.
  Suggested unit tests: testCalculatedPropertyDependenciesAreLoadedFromAnnotation(),
  testHubCalcInfoIndexUpdatedForAllAddPaths()
  Spec target section: Metadata Runtime / Calculated Property Semantics

  META-CALC-002 — Hub Calculated Properties Must Be Indexed Consistently
  Contract statement: If an OACalcInfo is marked isForHub, OAObjectInfo.isHubCalcInfo(name) must return true for the
  same property name.
  Rationale: Runtime reflection chooses different invocation semantics for Hub calculations.
  Source locations: OAObjectInfo.addCalcInfo, isHubCalcInfo, OAObjectInfoService.createCombinedObjectInfo,
  OAObjectReflectService
  Known related CODEX findings: fixed direct calc list additions bypassing hub-calc index.
  Suggested unit tests: testServiceAddCalcInfoUpdatesHubCalcIndex(), testInheritedHubCalcInfoIsRecognized()
  Spec target section: Metadata Runtime / Hub Calculated Properties

  8. Method Metadata Contracts

  META-METHOD-001 — Method Metadata Lookup Must Be Case-Insensitive And Cache-Consistent
  Contract statement: OAMethodInfo lookup by name must be case-insensitive and must reflect supported method
  additions.
  Rationale: Model-driven invocation, callbacks, trigger methods, and generated UI actions use method metadata.
  Source locations: OAObjectInfo.getMethodInfo, addMethod, addMethodInfo, OAMethodInfo
  Known related CODEX findings: none observed.
  Suggested unit tests: testMethodInfoLookupIsCaseInsensitive(), testMethodInfoCacheResetsAfterAdd()
  Spec target section: Metadata Runtime / Method Metadata

  META-METHOD-002 — Callback Metadata Must Be Attached To The Correct Runtime Element
  Contract statement: Object, property, link, method, and calculated-property callbacks must attach to the matching
  metadata element by name.
  Rationale: Incorrect callback routing creates silent missing behavior in generated applications.
  Source locations: OAObjectInfo.addObjectCallbackMethod, getObjectCallbackMethod, OAObjectAnnotationService callback
  processing
  Known related CODEX findings: none observed.
  Suggested unit tests: testPropertyCallbackAttachesToPropertyInfo(), testLinkCallbackAttachesToLinkInfo()
  Spec target section: Metadata Runtime / Callback Semantics

  9. Datasource Mapping Contracts

  META-DATASOURCE-001 — Datasource Flags Must Be Class-Level Runtime Truth
  Contract statement: useDataSource, localOnly, addToCache, and initialization flags on OAObjectInfo must define
  datasource/cache/runtime participation for that object type.
  Rationale: Datasource routing, local-only behavior, object cache registration, sync, and replication use these
  flags.
  Source locations: OAObjectInfo.getUseDataSource, getLocalOnly, getAddToCache, getInitializeNewObjects,
  OAObjectAnnotationService, OAObjectInfoService
  Known related CODEX findings: none observed.
  Suggested unit tests: testAnnotationDatasourceFlagsLoadIntoObjectInfo(),
  testCombinedObjectInfoPreservesDatasourceFlags()
  Spec target section: Metadata Runtime / Datasource Mapping

  META-DATASOURCE-002 — Property Column Metadata Must Remain Attached To Property Metadata
  Contract statement: OAPropertyInfo must retain column, length, format, blob, timestamp, timezone, encrypted, and
  hash metadata for datasource and serialization consumers.
  Rationale: Persistence and serialization need metadata-consistent value handling.
  Source locations: OAPropertyInfo, OAColumn, OAProperty, OAObjectAnnotationService property annotation loading
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAColumnMetadataLoadsIntoPropertyInfo(), testBlobTimestampAndFormatMetadataSurviveLookup()
  Spec target section: Metadata Runtime / Column Mapping

  10. Annotation / POJO Loading Contracts

  META-ANNOTATION-001 — Annotation Defaults Must Preserve OA Defaults
  Contract statement: Annotation default values such as empty strings or empty arrays must not override computed OA
  metadata defaults unless explicitly meaningful.
  Rationale: Generated models commonly rely on annotation defaults. Empty default metadata must not become silent
  wrong metadata.
  Source locations: OAObjectAnnotationService, OAObjectInfoService.createCombinedObjectInfo, OAObjectInfo default
  getters
  Known related CODEX findings: fixed empty display/plural annotation behavior.
  Suggested unit tests: testAnnotationEmptyStringDoesNotOverrideObjectInfoDefault(),
  testAnnotationLowerNameDefaultIsComputed()
  Spec target section: Metadata Runtime / Annotation Loading

  META-POJO-001 — POJO Metadata Must Preserve OA Match Semantics
  Contract statement: POJO metadata generated from OAObjectInfo must preserve regular properties, link-one fkeys,
  import-match fields, link-many presence, and unique-link matching rules.
  Rationale: JSON/Jackson/import layers depend on POJO metadata to match or create correct OAObjects.
  Source locations: OAObjectPojoLoader, Pojo, PojoDelegate, PojoLinkOneDelegate, PojoProperty
  Known related CODEX findings: accepted nested POJO import-match loader behavior per owner.
  Suggested unit tests: testPojoLoaderIncludesRegularPropertiesAndLinks(),
  testPojoLoaderPreservesImportMatchAndFkeySemantics()
  Spec target section: Metadata Runtime / POJO Loading

  11. Metadata Mutability / Consistency Contracts

  META-MUTABILITY-001 — Supported Metadata Mutation Paths Must Reset Derived Caches
  Contract statement: Adding properties, links, methods, or calc infos through supported APIs must update or
  invalidate derived caches and indexes.
  Rationale: Stale metadata caches cause silent wrong path/property/link/method resolution.
  Source locations: OAObjectInfo.addPropertyInfo, addLinkInfo, addMethodInfo, addCalcInfo, resetPropertyInfo, link
  list reset hook
  Known related CODEX findings: fixed inheritance merge paths to use supported add APIs.
  Suggested unit tests: testLinkCacheResetsAfterAddLinkInfo(), testPropertyCacheResetsAfterAddPropertyInfo()
  Spec target section: Metadata Runtime / Metadata Consistency

  META-MUTABILITY-002 — Metadata Should Be Treated As Stable After Initialization
  Contract statement: Once metadata is initialized and consumed by runtime services, arbitrary mutation of live
  metadata lists or mutable metadata fields must be avoided or must explicitly reset dependent caches.
  Rationale: Runtime services cache object/link/property decisions for performance and determinism.
  Source locations: OAObjectInfo.getPropertyInfos, getLinkInfos, getCalcInfos, getMethodInfos, cached lookup maps
  Known related CODEX findings: none observed beyond fixed supported mutation paths.
  Suggested unit tests: testSupportedMutationInvalidatesLookupCaches(),
  testMetadataInitializationProducesStableLookups()
  Spec target section: Metadata Runtime / Freeze Expectations

  12. Cross-Runtime Consumer Contracts

  META-CONSUMER-001 — Path/Filter/Query Must Resolve Through Metadata
  Contract statement: Path traversal, filter/query parsing, and reflective property access must resolve properties,
  links, and calculated properties from metadata with consistent case-insensitive behavior.
  Rationale: OAPath, filters, selects, serializers, and generated UI must agree on graph shape.
  Source locations: OAObjectInfo.getPropertyInfo, getLinkInfo, getCalcInfo, OALinkInfo, OAPropertyInfo, OAPath
  consumers
  Known related CODEX findings: none observed.
  Suggested unit tests: testOAPathResolvesPropertyLinkAndCalcFromMetadata(),
  testQueryFilterUsesMetadataCaseInsensitiveLookup()
  Spec target section: Metadata Runtime / Cross-Runtime Resolution

  META-CONSUMER-002 — Trigger Metadata Must Honor Sync Role Semantics
  Contract statement: Trigger metadata marked server-side-only must suppress clients but must still run in SingleUser
  mode unless explicitly actual-server-only.
  Rationale: SingleUser is local runtime, not a client. Silent trigger suppression breaks standalone apps and
  generated model behavior.
  Source locations: OAObjectInfo.createTrigger, _onChange, _onChange2, OATrigger, OATriggerMethod
  Known related CODEX findings: fixed isServer() gates to isClient() gates for trigger execution.
  Suggested unit tests: testServerSideOnlyTriggerRunsInSingleUser(), testServerSideOnlyTriggerDoesNotRunOnClient()
  Spec target section: Metadata Runtime / Trigger Role Semantics

  13. Failure / Silent Wrong-Metadata Contracts

  META-FAILURE-001 — Metadata Presence Checks Must Not Produce False Success Or NPE
  Contract statement: Metadata query APIs that answer “has” or lookup questions must return false/null for absence and
  must not throw for normal unset metadata.
  Rationale: Runtime services use metadata checks to choose behavior. Exceptions or false positives cause wrong
  routing or startup failure.
  Source locations: hasImportMatchProperties, getPropertyInfo, getLinkInfo, getMethodInfo, getCalcInfo
  Known related CODEX findings: fixed import-match NPE.
  Suggested unit tests: testMetadataLookupReturnsNullForMissingName(), testMetadataPresenceChecksAreNullSafe()
  Spec target section: Metadata Runtime / Failure Semantics

  META-FAILURE-002 — Removing Metadata-Backed Runtime Hooks Must Remove All Registrations
  Contract statement: Removing a trigger or metadata-backed runtime hook must remove every registration created for
  that hook and keep counters consistent.
  Rationale: Stale trigger metadata causes callbacks after removal and corrupts trigger accounting.
  Source locations: OAObjectInfo.removeTrigger, _removeTrigger, hmTriggerInfo, trigger counters
  Known related CODEX findings: fixed _removeTrigger removing only first matching registration.
  Suggested unit tests: testRemoveTriggerRemovesAllRegistrationsForSameTrigger(),
  testTriggerCountersMatchRegisteredTriggerInfosAfterRemove()
  Spec target section: Metadata Runtime / Hook Cleanup

  14. Test Coverage Matrix

  OAObjectInfo:
  testObjectInfoClassIdentitySurvivesInheritanceMerge, testUnsetOptionalMetadataPresenceChecksReturnFalse,
  testSupportedMutationInvalidatesLookupCaches

  ID / key metadata:
  testIdPropertiesMarkPropertyInfoAsId, testKeyPropertiesMatchObjectKeyConstruction,
  testPojoPrimaryKeyMetadataMatchesObjectInfoKey

  OAPropertyInfo:
  testPropertyLookupIsCaseInsensitive, testPrimitiveNullTrackedForConfiguredPrimitiveProperty,
  testOAColumnMetadataLoadsIntoPropertyInfo

  OALinkInfo:
  testReverseLinkResolvesByNameIgnoringCase, testLinkInfoCardinalityDrivesPathReturnType,
  testOwnedLinkInfosReflectOwnerLinks

  Ownership / cascade:
  testCascadeSaveFollowsLinkMetadata, testMustBeEmptyForDeleteBlocksDeleteWhenLinkHasMembers

  OACalcInfo:
  testCalculatedPropertyDependenciesAreLoadedFromAnnotation, testHubCalcInfoIndexUpdatedForAllAddPaths,
  testInheritedHubCalcInfoIsRecognized

  OAMethodInfo / callbacks:
  testMethodInfoLookupIsCaseInsensitive, testPropertyCallbackAttachesToPropertyInfo,
  testLinkCallbackAttachesToLinkInfo

  Annotation / POJO:
  testAnnotationEmptyStringDoesNotOverrideObjectInfoDefault, testPojoLoaderIncludesRegularPropertiesAndLinks,
  testPojoLoaderPreservesImportMatchAndFkeySemantics

  Cross-runtime:
  testOAPathResolvesPropertyLinkAndCalcFromMetadata, testServerSideOnlyTriggerRunsInSingleUser,
  testServerSideOnlyTriggerDoesNotRunOnClient

  Failure / cleanup:
  testMetadataPresenceChecksAreNullSafe, testRemoveTriggerRemovesAllRegistrationsForSameTrigger,
  testTriggerCountersMatchRegisteredTriggerInfosAfterRemove


*/


