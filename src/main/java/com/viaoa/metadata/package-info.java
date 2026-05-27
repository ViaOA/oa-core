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

//CODEX unit tests <todo>

/* CODEX Invariants

META-RUNTIME-001 — Executable Runtime Metadata
Contract statement:
com.viaoa.metadata defines the authoritative executable metadata model for OAObject classes, properties, links,
calculated values, methods, callbacks, triggers, POJO mapping, datasource behavior, serialization, sync,
replication, and graph runtime interpretation.
Rationale:
OA metadata is the semantic bridge between generated blueprints, annotations, Java class structure, and live Object
Graph behavior. Runtime services must not infer conflicting semantics once metadata is initialized.
Source scope:
OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, OAMethodInfo, OAFkeyInfo, OAObjectModel, POJO metadata
classes, metadata-consuming graph/object/hub/runtime services.
Related CODEX findings:
Existing package-info identifies metadata as runtime truth and notes optional metadata, inheritance/defaults, cache
invalidation, trigger, and POJO semantics.
Suggested unit tests:
testMetadataIsAuthoritativeForPropertyLookup(), testRuntimeUsesMetadataLinkSemantics(),
testAnnotationBlueprintBuildsEquivalentMetadata()
Spec target section:
Metadata Runtime / Core Responsibility

META-CLASS-001 — ObjectInfo Class Authority
Contract statement:
Every usable OAObjectInfo must identify exactly one owning OAObject class, and inherited/combined metadata must
resolve to the intended concrete runtime class.
Rationale:
Graph lookup, datasource routing, cache authority, POJO generation, path parsing, triggers, serialization, sync, and
replication depend on stable class identity.
Source scope:
OAObjectInfo.getForClass(), setForClass(...), metadata construction and combination services.
Related CODEX findings:
Existing package-info notes combined inherited metadata must still route to the concrete class.
Suggested unit tests:
testObjectInfoClassIdentitySetForRuntimeClass(), testCombinedObjectInfoUsesConcreteClass(),
testObjectInfoClassIdentitySurvivesInheritanceMerge()
Spec target section:
Metadata Runtime / Class Identity

META-CONSTRUCT-001 — Deterministic Metadata Construction
Contract statement:
For the same generated class, annotations, inheritance chain, and runtime configuration, metadata construction must
produce deterministic OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, and OAMethodInfo results independent of
reflection ordering.
Rationale:
Metadata drives executable graph behavior. Nondeterministic construction changes identity, links, callbacks,
triggers, and persistence behavior across JVM runs.
Source scope:
OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, OAMethodInfo, annotation/reflect metadata loading services.
Related CODEX findings:
Annotation package notes reflection order risks; metadata package notes cache-consistent supported mutation paths.
Suggested unit tests:
testMetadataConstructionDeterministicAcrossRepeatedLoads(),
testReflectionOrderDoesNotChangePropertyOrLinkMetadata(), testInheritedMetadataMergeIsDeterministic()
Spec target section:
Metadata Runtime / Construction Semantics

META-LOOKUP-001 — Case-Insensitive Metadata Lookup
Contract statement:
Property, link, calculated property, and method lookup by name must follow OA’s case-insensitive metadata lookup
rules and must be cache-consistent after supported metadata additions.
Rationale:
Path, query, filter, reflect, serialize, datasource, trigger, and UI code depend on stable name resolution.
Source scope:
OAObjectInfo.getPropertyInfo(...), addPropertyInfo(...), resetPropertyInfo(), getLinkInfo(...), addLinkInfo(...),
getCalcInfo(...), addCalcInfo(...), getMethodInfo(...), addMethodInfo(...).
Related CODEX findings:
Existing package-info notes property lookup cache reset fixes and method lookup cache consistency expectations.
Suggested unit tests:
testPropertyLookupIsCaseInsensitive(), testLinkLookupIsCaseInsensitive(), testMethodInfoLookupIsCaseInsensitive(),
testLookupCacheResetsAfterSupportedAdd()
Spec target section:
Metadata Runtime / Lookup Semantics

META-OPTIONAL-001 — Safe Optional Metadata
Contract statement:
Optional metadata arrays, flags, paths, callback fields, import-match state, POJO fields, view/context dependencies,
and display/UI metadata must behave as absent, false, empty, or null when unset; presence-check APIs must not throw
for normal absence.
Rationale:
Generated models commonly omit optional metadata. Runtime callers must safely query optional capabilities without
startup or runtime failures.
Source scope:
OAObjectInfo.hasImportMatchProperties(), getImportMatchPropertyNames(), getImportMatchPropertyPaths(),
getViewDependentProperties(), getContextDependentProperties(), OAPropertyInfo, OALinkInfo, OAMethodInfo optional
fields.
Related CODEX findings:
Existing package-info notes fixed null import-match presence check.
Suggested unit tests:
testUnsetOptionalMetadataPresenceChecksReturnFalse(), testUnsetOptionalMetadataArraysAreSafeToQuery(),
testMissingMetadataLookupReturnsNullNotException()
Spec target section:
Metadata Runtime / Optional Metadata

META-DEFAULT-001 — Annotation Defaults Preserve OA Defaults
Contract statement:
Empty annotation defaults and missing generated metadata must not suppress computed OA defaults unless the empty
value is explicitly meaningful.
Rationale:
Generated classes rely on default display names, plural names, lower names, and metadata fallbacks. Empty strings
becoming runtime truth creates misleading UI, diagnostics, and tooling output.
Source scope:
OAObjectInfo.getDisplayName(), getPluralName(), getLowerName(), setDisplayName(...), setPluralName(...),
setLowerName(...), annotation metadata loading.
Related CODEX findings:
Existing package-info notes fixed empty annotation display/plural behavior and plural fallback.
Suggested unit tests:
testEmptyOAClassDisplayNameUsesDefault(), testPluralNameUsesSimpleClassNameFallback(),
testAnnotationEmptyStringDoesNotOverrideObjectInfoDefault()
Spec target section:
Metadata Runtime / Default Metadata

META-ID-001 — Identity Metadata Consistency
Contract statement:
OAObjectInfo idProperties, keyProperties, OAPropertyInfo id/key/autoAssign flags, and object-key construction
semantics must agree for each object type.
Rationale:
Identity metadata drives cache lookup, datasource matching, serialization identity, sync, replication, equality, and
duplicate-object prevention.
Source scope:
OAObjectInfo.getIdProperties(), getKeyProperties(), isIdProperty(...), isKeyProperty(...), OAPropertyInfo.getId(),
getKey(), getAutoAssign(), OAFkeyInfo.
Related CODEX findings:
Existing package-info notes ID/key metadata as a core object-key invariant.
Suggested unit tests:
testIdPropertiesMarkPropertyInfoAsId(), testKeyPropertiesMatchObjectKeyConstruction(),
testAutoAssignIdMetadataMatchesGeneratedKeyContract()
Spec target section:
Metadata Runtime / Identity Metadata

META-PROPERTY-001 — Scalar Property Semantics
Contract statement:
OAPropertyInfo must accurately describe scalar property name, type, primitive/null tracking, identity/key flags,
validation/display fields, formatting, datasource column fields, blob/encrypted/hash/timestamp flags, import/export,
POJO, and sensitive-data semantics.
Rationale:
Scalar metadata feeds object property access, datasource writes, validation, serialization, formatting, path/query
resolution, sync, and replication.
Source scope:
OAPropertyInfo getters/setters, OAObjectInfo property list/lookup, annotation-derived property metadata.
Related CODEX findings:
Existing package-info notes primitive null semantics and column metadata retention.
Suggested unit tests:
testPropertyInfoStoresNameTypeAndDisplayMetadata(), testPrimitiveNullTrackedForConfiguredPrimitiveProperty(),
testBlobTimestampFormatAndColumnMetadataSurviveLookup()
Spec target section:
Metadata Runtime / Property Metadata

META-LINK-001 — Link Cardinality and Target Semantics
Contract statement:
Each OALinkInfo must define stable link name, target class, cardinality, reverse name, ownership, cascade flags,
Hub/detail behavior, relationship flags, calculated/transient status, and default/select/equal path metadata
according to OA graph semantics.
Rationale:
Link metadata defines object graph shape. Wrong link metadata corrupts Hub/detail behavior, traversal, save/delete,
serialization, sync, replication, and path/query results.
Source scope:
OALinkInfo, OAObjectInfo.getLinkInfo(...), addLinkInfo(...), OAFkeyInfo, link metadata consumers.
Related CODEX findings:
Existing package-info notes cardinality, reverse-link, ownership, and cascade metadata as runtime contracts.
Suggested unit tests:
testLinkInfoCardinalityDrivesPathReturnType(), testLinkInfoTargetClassMatchesMetadataLookup(),
testLinkInfoRelationshipFlagsPreserved()
Spec target section:
Metadata Runtime / Link Metadata

META-REVERSE-001 — Reverse-Link Resolution
Contract statement:
When reverseName is defined, OALinkInfo.getReverseLinkInfo() must resolve to the target class’s matching reverse
link metadata and remain stable after metadata initialization.
Rationale:
Reverse links drive Hub membership, ownership, cascade, path reversal, trigger traversal, sync propagation, and
serialization relationships.
Source scope:
OALinkInfo.getReverseName(), setReverseName(...), getReverseLinkInfo(), OAObjectInfo link metadata.
Related CODEX findings:
Existing package-info identifies reverse-link resolution as stable metadata behavior.
Suggested unit tests:
testReverseLinkResolvesByNameIgnoringCase(), testSyntheticReverseLinkCreatedForOneSidedManyWhenContracted(),
testReverseLinkStableAfterMetadataInitialization()
Spec target section:
Metadata Runtime / Reverse-Link Semantics

META-OWNERSHIP-001 — Ownership and Cascade Semantics
Contract statement:
OALinkInfo owner, reverse-owner, owned-link caches, cascadeSave, cascadeDelete, mustBeEmptyForDelete, and ownership-
derived state must consistently define graph lifecycle ownership.
Rationale:
Save/delete cascade, referenceability, delete restrictions, cache ownership, and graph traversal depend on ownership
metadata.
Source scope:
OALinkInfo.getOwner(), setOwner(...), getCascadeSave(), getCascadeDelete(), getMustBeEmptyForDelete(),
OAObjectInfo.getOwnedLinkInfos(), getOwnedByOne(), isOwnedAndNoReverseMany().
Related CODEX findings:
Existing package-info notes ownership and cascade metadata contracts.
Suggested unit tests:
testOwnedLinkInfosReflectOwnerLinks(), testOwnedByOneUsesReverseOwnerMetadata(),
testCascadeSaveFollowsLinkMetadata(), testMustBeEmptyForDeleteBlocksDeleteWhenLinkHasMembers()
Spec target section:
Metadata Runtime / Ownership and Cascade

META-CALC-001 — Calculated Metadata Semantics
Contract statement:
OACalcInfo must expose stable calculated property name, return type, dependency paths, hub-calculation flag, and
invocation metadata needed by runtime invalidation, reflection, path traversal, serialization, and UI refresh.
Rationale:
Calculated properties are executable semantic metadata and must not become stale or invokable through the wrong
runtime path.
Source scope:
OACalcInfo, OAObjectInfo.addCalcInfo(...), getCalcInfo(...), isHubCalcInfo(...), calculated property consumers.
Related CODEX findings:
Existing package-info notes fixed hub-calc index consistency.
Suggested unit tests:
testCalculatedPropertyDependenciesAreLoadedFromMetadata(), testHubCalcInfoIndexUpdatedForAllAddPaths(),
testInheritedHubCalcInfoIsRecognized()
Spec target section:
Metadata Runtime / Calculated Metadata

META-METHOD-001 — Method Metadata Semantics
Contract statement:
OAMethodInfo must define stable method/action metadata, callback dependencies, visibility/enabled/context fields,
and method annotations in a way that lookup and invocation consumers can resolve deterministically.
Rationale:
Generated UI actions, callbacks, triggers, and runtime callable graph behavior depend on method metadata.
Source scope:
OAMethodInfo, OAObjectInfo.addMethodInfo(...), addMethod(...), getMethodInfo(...), object callback method APIs.
Related CODEX findings:
Existing package-info notes method lookup and callback attachment consistency.
Suggested unit tests:
testMethodInfoLookupIsCaseInsensitive(), testMethodInfoCacheResetsAfterAdd(),
testMethodCallbackMetadataAttachesToMethodInfo()
Spec target section:
Metadata Runtime / Method Metadata

META-CALLBACK-001 — Callback Metadata Attachment
Contract statement:
Object, property, link, method, and calculated-property callback metadata must attach to the matching metadata
element by name and must remain discoverable by runtime callback consumers.
Rationale:
Incorrect callback routing creates silent missing behavior in generated applications and callable graph flows.
Source scope:
OAObjectInfo.addObjectCallbackMethod(...), getObjectCallbackMethod(...), OAPropertyInfo callback fields, OALinkInfo
callback fields, OAMethodInfo callback fields.
Related CODEX findings:
Existing package-info identifies callback metadata attachment as a runtime contract.
Suggested unit tests:
testPropertyCallbackAttachesToPropertyInfo(), testLinkCallbackAttachesToLinkInfo(),
testObjectCallbackMethodLookupUsesExpectedName()
Spec target section:
Metadata Runtime / Callback Metadata

META-DATASOURCE-001 — Datasource Participation Metadata
Contract statement:
useDataSource, localOnly, addToCache, initializeNewObjects, supportsStorage, lookup, singleton, preSelect,
processed, and related class-level metadata must define runtime datasource/cache participation for the object type.
Rationale:
Datasource routing, local-only behavior, object cache registration, object initialization, sync, replication, and
selection behavior depend on class-level metadata.
Source scope:
OAObjectInfo.getUseDataSource(), getLocalOnly(), getAddToCache(), getInitializeNewObjects(), getSupportsStorage(),
getLookup(), getSingleton(), getPreSelect(), getProcessed().
Related CODEX findings:
Existing package-info notes datasource flags must be class-level runtime truth.
Suggested unit tests:
testAnnotationDatasourceFlagsLoadIntoObjectInfo(), testCombinedObjectInfoPreservesDatasourceFlags(),
testLocalOnlyObjectInfoSkipsExternalDatasourceByContract()
Spec target section:
Metadata Runtime / Datasource Metadata

META-POJO-001 — POJO Metadata Alignment
Contract statement:
POJO metadata generated from OAObjectInfo must preserve regular properties, one-link foreign keys, import-match
fields, many-link presence, primary keys, and unique-link matching rules consistently with OA object identity
semantics.
Rationale:
JSON/Jackson/import tooling must match or create correct OAObjects without duplicate identity or relationship drift.
Source scope:
OAObjectInfo.getPojo(), POJO metadata classes, OAPropertyInfo noPojo/pojoKeyPos/importMatch, OALinkInfo import/
equal/select metadata.
Related CODEX findings:
Existing package-info notes accepted nested POJO import-match loader behavior.
Suggested unit tests:
testPojoLoaderIncludesRegularPropertiesAndLinks(), testPojoLoaderPreservesImportMatchAndFkeySemantics(),
testPojoPrimaryKeyMetadataMatchesObjectInfoKey()
Spec target section:
Metadata Runtime / POJO Metadata

META-PATH-001 — Metadata Resolution for Path and Query
Contract statement:
Path traversal, query parsing, filter evaluation, reflective property access, serializer traversal, and generated UI
behavior must resolve properties, links, calculated properties, and methods through metadata with consistent case-
insensitive behavior.
Rationale:
Runtime packages must agree on graph shape and callable fields.
Source scope:
OAObjectInfo.getPropertyInfo(...), getLinkInfo(...), getCalcInfo(...), getMethodInfo(...), OAPropertyInfo,
OALinkInfo, OACalcInfo, OAMethodInfo.
Related CODEX findings:
Existing package-info identifies path/filter/query metadata resolution as a cross-runtime consumer contract.
Suggested unit tests:
testOAPathResolvesPropertyLinkAndCalcFromMetadata(), testQueryFilterUsesMetadataCaseInsensitiveLookup(),
testSerializerUsesMetadataForPropertyAndLinkTraversal()
Spec target section:
Metadata Runtime / Cross-Runtime Resolution

META-TRIGGER-001 — Trigger Metadata Semantics
Contract statement:
Metadata-backed triggers must preserve dependent property paths, execution role, registration counters, removal
semantics, and graph visibility according to trigger/runtime contracts.
Rationale:
Triggers are metadata-driven reactive behavior. Incorrect registration or role handling can suppress standalone
behavior, duplicate execution, or leak trigger hooks.
Source scope:
OAObjectInfo.createTrigger(...), removeTrigger(...), getTriggers(...), getHasTriggers(), onChange(...), TriggerInfo
state.
Related CODEX findings:
Existing package-info notes fixed server-side trigger gates and removal/counter consistency concerns.
Suggested unit tests:
testServerSideOnlyTriggerRunsInSingleUser(), testServerSideOnlyTriggerDoesNotRunOnClient(),
testRemoveTriggerRemovesAllRegistrationsAndCounters()
Spec target section:
Metadata Runtime / Trigger Metadata

META-MUTATION-001 — Supported Mutation Invalidates Derived State
Contract statement:
Supported metadata mutation APIs that add or replace properties, links, calculated properties, methods, callbacks,
or object-level flags must update or invalidate derived caches and indexes before later lookup.
Rationale:
Stale metadata caches cause silent wrong path, property, link, method, ownership, trigger, or calculated-property
resolution.
Source scope:
OAObjectInfo.addPropertyInfo(...), resetPropertyInfo(), addLinkInfo(...), getLinkInfos() custom list hooks,
addCalcInfo(...), addMethodInfo(...), addObjectCallbackMethod(...).
Related CODEX findings:
Existing package-info notes fixed inherited merge paths to use supported add APIs and hub-calc index updates.
Suggested unit tests:
testPropertyCacheResetsAfterAddPropertyInfo(), testLinkCacheResetsAfterAddLinkInfo(),
testServiceAddCalcInfoUpdatesHubCalcIndex()
Spec target section:
Metadata Runtime / Metadata Mutation

META-STABILITY-001 — Post-Initialization Stability
Contract statement:
Once metadata is initialized and consumed by runtime services, it must be treated as stable unless an explicit
supported mutation/rebuild path resets all dependent caches and consumers.
Rationale:
Runtime services cache metadata decisions for performance, identity, graph traversal, path/query execution,
serialization, sync, and replication.
Source scope:
OAObjectInfo property/link/calc/method lists, lookup maps, owned-link caches, hub-calc indexes, trigger registration
state.
Related CODEX findings:
Existing package-info notes supported mutation paths and freeze expectations.
Suggested unit tests:
testMetadataInitializationProducesStableLookups(), testSupportedMutationInvalidatesLookupCaches(),
testArbitraryListMutationRequiresOwnerDecision()
Spec target section:
Metadata Runtime / Metadata Stability

META-FAIL-001 — Invalid Metadata Visibility
Contract statement:
Invalid, contradictory, missing, or incomplete metadata required for runtime behavior must fail visibly during
construction, verification, lookup, or first semantic use; metadata APIs must not produce false success.
Rationale:
False metadata success can corrupt persistence, graph traversal, identity, Hub relationships, serialization, sync,
replication, and generated UI/tooling.
Source scope:
OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, OAMethodInfo, metadata lookup and construction paths.
Related CODEX findings:
Existing package-info notes null-safe presence checks, optional metadata behavior, verifier-alignment risks from
annotation package, and wrong-metadata prevention.
Suggested unit tests:
testInvalidLinkMetadataFailsBeforeRuntimeUse(), testMetadataPresenceChecksAreNullSafe(),
testMissingRuntimeRequiredMetadataFailsVisibly()
Spec target section:
Metadata Runtime / Failure Semantics

META-CONCURRENT-001 — Shared Metadata Thread-Safety Boundary
Contract statement:
Shared metadata structures must be safely published after construction, and concurrent reads must see stable
metadata; concurrent mutation is allowed only through supported paths with defined cache invalidation behavior.
Rationale:
Metadata is read by many runtime threads across object, Hub, path, query, serialization, sync, replication, UI, and
datasource services.
Source scope:
OAObjectInfo lookup caches and lists, concurrent trigger maps, OAPropertyInfo/OALinkInfo/OACalcInfo/OAMethodInfo
metadata fields.
Related CODEX findings:
Existing package-info identifies metadata stability and supported mutation cache reset as key contracts.
Suggested unit tests:
testConcurrentMetadataLookupsSeeStablePropertyInfo(), testConcurrentTriggerLookupUsesStableRegistrationState(),
testConcurrentSupportedMetadataAddInvalidatesCachesSafely()
Spec target section:
Metadata Runtime / Concurrency

META-COMPAT-001 — Blueprint and Version Compatibility
Contract statement:
Metadata must remain compatible with generated blueprint classes and annotation evolution; changes to metadata
defaults, field meaning, or construction rules must preserve existing generated model behavior or require explicit
migration/validation.
Rationale:
OA applications depend on generated model classes remaining semantically stable across OA 4.0 metadata evolution.
Source scope:
OAObjectInfo, OAPropertyInfo, OALinkInfo, OACalcInfo, OAMethodInfo, OAObjectModel, annotation-derived metadata, POJO
metadata.
Related CODEX findings:
Annotation package notes compatibility/default ownership; metadata package notes defaults and generated model
behavior.
Suggested unit tests:
testMetadataDefaultsRemainCompatibleWithExistingGeneratedModels(),
testAnnotationDefaultChangesRequireMetadataValidation(), testBlueprintMetadataRoundTripPreservesSemantics()
Spec target section:
Metadata Runtime / Compatibility

META-MODEL-001 — UI/Model Policy Metadata
Contract statement:
OAObjectModel metadata must represent UI/tooling policy state separately from core runtime object identity/link
semantics, and defaultAll must set only its documented model-policy flags.
Rationale:
Object model metadata can guide UI/tooling behavior without becoming core graph identity or persistence truth.
Source scope:
OAObjectModel, display/plural names, allow flags, viewOnly, createUI, table/filter/sorting/download/move/refresh
flags.
Related CODEX findings:
No direct source CODEX finding; package context includes display/UI metadata boundaries.
Suggested unit tests:
testObjectModelDefaultAllSetsDocumentedFlags(), testObjectModelDisplayNamesAreIndependentOfObjectInfoIdentity(),
testObjectModelPolicyDoesNotMutateCoreObjectInfo()
Spec target section:
Metadata Runtime / Model Policy Metadata

META-INTEGRATION-001 — Cross-Package Metadata Compatibility
Contract statement:
Metadata behavior must remain compatible with annotation, reflect, object, Hub, graph, datasource, path, query,
select, filter, find, serialization, sync, replication, validation, callback, trigger, template, and codegen/tooling
contracts.
Rationale:
Metadata is the executable semantic contract over OA blueprints and live graph behavior; nearly every runtime
package depends on its authority and stability.
Source scope:
com.viaoa.metadata.*, metadata consumers across OA runtime packages.
Related CODEX findings:
Existing package-info maps metadata to runtime truth, datasource, path/query, triggers, POJO, annotation defaults,
cache, and graph services.
Suggested unit tests:
testMetadataAnnotationReflectRuntimeAlignment(), testMetadataPathQueryDatasourceSerializationIntegration(),
testMetadataSyncReplicationIdentityIntegration()
Spec target section:
Metadata Runtime / Cross-Package Integration

*/
