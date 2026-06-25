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
 * Object-service machinery (sub-services).
 *
 * <p>This package contains internal sub-services that together implement the Object
 * functionality for a single {@code OA} instance.</p>
 *
 * <p>These classes are not intended to be constructed directly. They are created,
 * wired, and managed by the owning coordinator ({@code OAObjectService}) in the
 * parent package.</p>
 *
 * <p>Sub-services may declare required outcomes via protected abstract "dependency hooks".
 * These hooks are implemented by the owner/coordinator and are intentionally used to:
 * <ul>
 *   <li>keep sub-services from depending on other services directly</li>
 *   <li>centralize coordination and lifecycle management</li>
 *   <li>prevent service-locator coupling</li>
 * </ul>
 * </p>
 */
package com.viaoa.oa.service.object;

//CODEX unit tests <todo>

/* CODEX Invariants

OBJ-SERVICE-001 — Object Services Are Graph-Owned Internal Runtime Services
Contract statement: Object service classes are owned, created, wired, and coordinated by the owning OAObjectService/
OA and must not act as independent application-level runtime authorities.
Rationale: Object lifecycle, metadata, identity, cache, datasource, event, sync, and serialization behavior must
remain graph-scoped and centrally coordinated.
Source scope: OAObjectService parent coordinator; all classes in com.viaoa.graph.service.object; dependency hooks
implemented by the owning graph service.
Related CODEX findings: OAObjectParentService child sync hook and parent-role-guard findings.
Suggested unit tests: testObjectSubServicesAreCreatedByParentService(),
testChildServiceDoesNotBypassParentRoleGuards(), testObjectServiceUsesOwningGraphForRuntimeHooks()
Spec target section: OG Object Runtime / Service Ownership

OBJ-ROLE-001 — Single-User, Server, And Client Roles Are Distinct
Contract statement: Object services must distinguish single-user, server, client, and unconfigured sync roles; local
object work must not require remote sync services, and remote/client operations must require the correct role.
Rationale: Save, delete, cache refresh, serialization, and sync hooks behave differently by graph role and must not
silently take the wrong path.
Source scope: OAObjectCSService, OAObjectSaveService, OAObjectDeleteService, OAObjectParentService,
OAObjectSerializeService, OAObjectCacheService.
Related CODEX findings: Local save requiring RemoteSyncInterface; remote delete false-success; child sync hooks
requiring parent role guards.
Suggested unit tests: testSingleUserSaveDoesNotRequireRemoteSync(), testClientRemoteDeleteFalseIsVisible(),
testChildSyncHooksRespectSingleUserServerClientRoles()
Spec target section: OG Object Runtime / Role Semantics

OBJ-METADATA-001 — Annotation Metadata Must Be Valid Before Publication
Contract statement: Annotation-derived object metadata must be complete, validated, and internally consistent before
it is published for runtime use.
Rationale: Metadata drives object identity, links, callbacks, foreign keys, datasource mapping, calculated
properties, cascade behavior, serialization, and generated runtime semantics.
Source scope: OAObjectAnnotationService, OAObjectInfoService.
Related CODEX findings: Class-level OAObjCallback guarded by OAClass processing; duplicate OAId(pos) overwrite;
invalid OAFkey mappings accepted; implicit Hub link creation before OAMany validation; missing-pos handling; guid/id
metadata processing; calc property metadata findings.
Suggested unit tests: testAnnotationMetadataRejectsDuplicateIdPositions(),
testInvalidForeignKeyMappingFailsBeforePublish(), testClassCallbacksProcessedIndependentlyOfOAClass(),
testHubReturningMethodRequiresValidManyMetadata()
Spec target section: OG Object Runtime / Metadata Semantics

OBJ-ID-001 — Live Objects Have Stable Graph Identity After Initialization
Contract statement: After successful initialization, each live OAObject must have stable graph identity, including
GUID/key identity according to metadata and runtime role.
Rationale: Cache lookup, equality, Hub membership, serialization, sync, replication, and object graph traversal
depend on stable identity.
Source scope: OAObjectInitializeService, OAObjectGuidService, OAObjectKeyService, OAObjectCacheService,
OAObjectDSService.
Related CODEX findings: Initialization cache publication before authoritative ID assignment failure; GUID/id
metadata processing findings.
Suggested unit tests: testInitializedObjectHasStableGuid(), testInitializationFailureDoesNotPublishCachedIdentity(),
testGuidAndIdIdentityRemainConsistentAfterCreate()
Spec target section: OG Object Runtime / Identity Semantics

OBJ-KEY-001 — Object Key Comparison Is Deterministic Across Key Forms
Contract statement: Object identity comparison and cache resolution must be deterministic across GUID-only, ID-only,
mixed GUID/id, and datasource-key forms according to metadata.
Rationale: OA commonly compares cached, serialized, deserialized, loaded, and id-only object references before full
materialization.
Source scope: OAObjectKeyService, OAObjectGuidService, OAObjectCacheService, OAObjectSerializeService.
Related CODEX findings: Package-info identity test outline for GUID-only, ID-only, mixed GUID/id keys.
Suggested unit tests: testGuidOnlyAndIdOnlyKeyComparisonByContract(),
testMixedGuidAndIdKeysResolveDeterministically(), testDuplicateCacheAddDoesNotCreateSecondAuthoritativeObject()
Spec target section: OG Object Runtime / Key Semantics

OBJ-CACHE-001 — Cache Add And Resolve Preserve One Authoritative Instance
Contract statement: Object cache add, resolve, refresh, and deserialize operations must not create duplicate
authoritative objects for the same graph/class/key identity.
Rationale: Duplicate live instances corrupt links, Hubs, object equality, save/delete, sync, and serialization
behavior.
Source scope: OAObjectCacheService, OAObjectKeyService, OAObjectInitializeService, OAObjectSerializeService,
OAObjectDSService.
Related CODEX findings: Cache publication before initialization success; cache refresh role matrix; duplicate
authoritative object concerns.
Suggested unit tests: testCacheResolveReturnsSingleAuthoritativeInstance(), testFailedInitializeRollsBackCacheAdd(),
testDeserializeDuplicateMergesWithCachedInstance()
Spec target section: OG Object Runtime / Cache Semantics

OBJ-CACHE-002 — Cache Indexes Must Track Key And Lifecycle Changes
Contract statement: Cache indexes and object-reference indexes must be updated consistently when keys, GUIDs, delete
state, or object reference state changes, or the operation must fail visibly.
Rationale: Stale cache indexes make objects unreachable, duplicated, or incorrectly matched after save/delete/
deserialize/load.
Source scope: OAObjectCacheService, OAObjectKeyService, OAObjectGuidService, OAObjectDeleteService,
OAObjectPropertyService.
Related CODEX findings: Delete cleanup removes object and key references; key/reference cleanup test outline.
Suggested unit tests: testKeyChangeUpdatesCacheIndexes(), testDeleteCleanupRemovesObjectAndKeyReferences(),
testRejectedKeyChangeLeavesCacheIndexesUnchanged()
Spec target section: OG Object Runtime / Cache Index Semantics

OBJ-INIT-001 — Initialization Publishes Runtime State Only After Required Setup Completes
Contract statement: Object initialization may publish cache, GUID, primitive-null, datasource, or sync-visible state
only after required setup has completed, or must roll back/leave visible failure state.
Rationale: Partially initialized objects can corrupt cache identity, primitive-null tracking, datasource identity,
and later save/delete behavior.
Source scope: OAObjectInitializeService, OAObjectGuidService, OAObjectKeyService, OAObjectCacheService,
OAObjectDSService.
Related CODEX findings: Cache add before assignId/create failure; primitive-null initialization test outline.
Suggested unit tests: testInitializeAddsToCacheOnlyAfterAuthoritativeSetup(),
testFailedAssignIdDoesNotLeaveCachedObject(), testPrimitiveNullStateExistsBeforePrimitiveMutation()
Spec target section: OG Object Runtime / Initialization Semantics

OBJ-LOAD-001 — Load And Refresh Must Preserve Unloaded/Loaded/Failed Distinctions
Contract statement: Object load, refresh, and datasource materialization must distinguish unloaded, loading, loaded,
loaded-empty/absent, and failed states where runtime behavior depends on the distinction.
Rationale: Lazy loading, retry, serialization, save/delete, and cache refresh depend on explicit load-state
semantics.
Source scope: OAObjectDSService, OAObjectReflectService, OAObjectPropertyService, OAObjectCacheService,
OAObjectSerializeService.
Related CODEX findings: Package-info cache refresh and role matrix test outline.
Suggested unit tests: testFailedRefreshDoesNotMarkObjectLoaded(),
testDatasourceRefreshKeepsRetryableStateAfterFailure(),
testLoadedAbsentStateOnlyAfterAuthoritativeDatasourceResult()
Spec target section: OG Object Runtime / Load Semantics

OBJ-LAZY-001 — Lazy References Must Not Collapse Unresolved State To Null
Contract statement: Unresolved object references, id-only references, and lazy references must remain
distinguishable from confirmed null/absent references until authoritative resolution completes.
Rationale: Sync, replication, serialization, lazy loading, and finder/path traversal require the distinction between
not-yet-resolved and does-not-exist.
Source scope: OAObjectPropertyService, OAObjectReflectService, OAObjectKeyService, OAObjectDSService,
OAObjectSerializeService.
Related CODEX findings: Package-info identity and serialization test outline for id-only and deserialization forms.
Suggested unit tests: testUnresolvedIdReferenceDoesNotBecomeNullOnCacheMiss(),
testLazyReferenceCanResolveAfterInitialCacheMiss(), testSerializeIdOnlyReferencePreservesResolutionIntent()
Spec target section: OG Object Runtime / Lazy Reference Semantics

OBJ-PROPERTY-001 — Property Mutation Publishes Only Completed Semantic Changes
Contract statement: Object property changes must update primitive-null masks, old/new values, changed state, reverse
links, events, triggers, and sync hooks only according to completed mutation state.
Rationale: Property mutation is the root of object lifecycle, validation, event, trigger, and sync behavior.
Source scope: OAObjectPropertyService, OAObjectChangeService, OAObjectEventService, OAObjectCallbackService,
OAObjectParentService.
Related CODEX findings: Primitive-null mask invariant outline; reverse-link failure findings.
Suggested unit tests: testPrimitiveNullMaskUpdatedBeforePrimitiveNullMutation(),
testFailedPropertyMutationDoesNotPublishAfterEvent(), testPropertyChangedStateMatchesCompletedMutation()
Spec target section: OG Object Runtime / Property Mutation Semantics

OBJ-LINK-001 — Bidirectional Relationship Maintenance Must Complete Or Fail Visibly
Contract statement: Link property transitions must update forward and reverse graph state according to metadata
before publishing after-events or completed mutation state; inverse-link failure must be visible.
Rationale: Broken bidirectional links corrupt object graph traversal, Hub/detail relationships, cascade save/delete,
sync, and serialization.
Source scope: OAObjectEventService, OAObjectPropertyService, OAObjectReflectService, OAObjectParentService.
Related CODEX findings: Reverse Hub getter invalid/non-Hub; reverse update throws; silent inverse-link failure.
Suggested unit tests: testLinkSetterUpdatesReverseHubBeforeAfterEvent(), testReverseLinkFailureIsVisible(),
testFailedReverseLinkUpdateDoesNotPublishCompletedMutation()
Spec target section: OG Object Runtime / Link Semantics

OBJ-HUB-001 — Object/Hub Relationship Discovery Must Match Metadata
Contract statement: Object services that discover or expose Hubs, empty Hubs, auto-add Hubs, or parent/detail Hubs
must follow OA metadata ownership, cardinality, and link semantics.
Rationale: Object graph traversal, detail Hubs, cascade operations, auto-add behavior, and serialization depend on
metadata-correct Hub relationships.
Source scope: OAObjectHubService, OAObjectEmptyHubService, OAObjectAutoAddService, OAObjectReflectService,
OAObjectInfoService, OAObjectAnnotationService.
Related CODEX findings: Hub-returning methods creating link metadata before OAMany validation.
Suggested unit tests: testObjectHubDiscoveryUsesDeclaredMetadata(), testEmptyHubCreationUsesCorrectLinkInfo(),
testAutoAddHonorsOwnershipAndCardinality()
Spec target section: OG Object Runtime / Object-Hub Relationship Semantics

OBJ-SAVE-001 — Save Uses The Authoritative Persistence Path For The Graph Role
Contract statement: Object save must route through the datasource, client/server authority, or local persistence
path required by the owning graph role and object class metadata.
Rationale: Single-user, server, and client modes must persist object state consistently and visibly.
Source scope: OAObjectSaveService, OAObjectDSService, OAObjectCSService, OAObjectParentService.
Related CODEX findings: Local save must not require RemoteSyncInterface; role matrix save tests.
Suggested unit tests: testSingleUserSaveUsesLocalDatasource(), testClientSaveUsesRemoteAuthorityByContract(),
testServerSaveUsesServerDatasourcePath()
Spec target section: OG Object Runtime / Save Routing Semantics

OBJ-SAVE-002 — Failed Save Preserves Retryable Lifecycle State
Contract statement: A failed save must preserve or restore new, changed, deleted, cascade, and reference state
needed for retry, unless failure is explicitly terminal.
Rationale: Save failure that clears dirty/new/deleted state creates silent data loss and prevents retry.
Source scope: OAObjectSaveService, OAObjectChangeService, OAObjectDSService, OAObjectEventService.
Related CODEX findings: saveWithoutReferences clears new state; failed datasource save clears dirty/deleted flags;
shared OACascade state after failure.
Suggested unit tests: testFailedSaveWithoutReferencesPreservesNewState(),
testFailedDatasourceSavePreservesDirtyLifecycleFlags(), testFailedSaveLeavesCascadeRetryable()
Spec target section: OG Object Runtime / Save Failure Semantics

OBJ-SAVE-003 — Save Completion Side Effects Require Authoritative Save Completion
Contract statement: After-save callbacks/events, sync messages, replication hooks, cascade continuation, and
lifecycle cleanup must occur only after the authoritative save stage they represent has completed.
Rationale: After-save side effects are observed by UI, triggers, sync, replication, and downstream cascade behavior
as completed facts.
Source scope: OAObjectSaveService, OAObjectEventService, OAObjectCallbackService, OAObjectParentService.
Related CODEX findings: Failed callDSSave still allowing after-save/cascade continuation.
Suggested unit tests: testAfterSaveNotFiredWhenDatasourceSaveFails(),
testFailedSaveDoesNotContinueCascadeAsSuccess(), testSaveSyncMessageOnlyAfterAuthoritativeSave()
Spec target section: OG Object Runtime / Save Side-Effect Semantics

OBJ-DELETE-001 — Delete Coordinates Lifecycle, Datasource, Links, Cache, And Events
Contract statement: Object delete must coordinate lifecycle flags, datasource/client-server delete, relationship
cleanup, cache/key references, events, sync, and replication according to graph role.
Rationale: Delete is a graph mutation, not only a flag change; partial delete state can leave orphaned children,
stale cache entries, or divergent runtime state.
Source scope: OAObjectDeleteService, OAObjectDSService, OAObjectCSService, OAObjectCacheService,
OAObjectEventService, OAObjectParentService.
Related CODEX findings: Child deletes succeed then parent datasource delete throws; remote delete false result;
delete cleanup reference test outline.
Suggested unit tests: testDeleteRemovesCacheAndKeyReferencesAfterAuthorityCompletes(),
testClientRemoteDeleteFalseDoesNotMarkDeleted(), testDeleteCoordinatesRelationshipCleanup()
Spec target section: OG Object Runtime / Delete Semantics

OBJ-DELETE-002 — Failed Delete Must Not Publish Completed Delete State
Contract statement: A failed delete or undelete operation must not publish completed deleted state, clear retry
state, emit completed after-events, or leave contradictory lifecycle flags.
Rationale: False delete success corrupts object graph consistency and can make retry/reconciliation impossible.
Source scope: OAObjectDeleteService, OAObjectEventService, OAObjectChangeService, OAObjectCacheService.
Related CODEX findings: Cascade delete partial failure; setDeleted(false) conflicting key path; remote delete false
result.
Suggested unit tests: testFailedParentDatasourceDeleteDoesNotSilentlyLoseChildren(),
testFailedUndeletePreservesDeletedState(), testDeleteFailureDoesNotEmitCompletedAfterEvent()
Spec target section: OG Object Runtime / Delete Failure Semantics

OBJ-CASCADE-001 — Recursive Save/Delete Traversal Follows Metadata And Remains Bounded
Contract statement: Recursive save/delete traversal must follow OA metadata ownership, cascade, link, and Hub
semantics while preventing infinite recursion and duplicate invalid processing.
Rationale: Object graph persistence must process required reachable objects without corrupting cycles, shared
references, or ownership boundaries.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectRecurseService, OAObjectReflectService,
OAObjectInfoService.
Related CODEX findings: Partial recursive save/delete and cascade state findings.
Suggested unit tests: testRecursiveSaveFollowsCascadeMetadata(), testRecursiveDeleteHandlesCyclesWithoutLooping(),
testSharedReferenceProcessedAccordingToCascadeContract()
Spec target section: OG Object Runtime / Cascade Traversal Semantics

OBJ-CASCADE-002 — Partial Cascade Progress Must Be Visible And Retryable
Contract statement: If recursive save/delete/cascade work partially succeeds and then fails, the failure must be
caller-visible and remaining graph state must be recoverable or explicitly marked incomplete.
Rationale: Cascades can cross many objects; silent partial success creates graph divergence and data loss.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectRecurseService, OACascade integration.
Related CODEX findings: Parent delete failure after child deletes; failed recursive save shared cascade mutation.
Suggested unit tests: testCascadeDeleteFailureReportsIncompleteState(),
testCascadeSaveFailureDoesNotClearUnprocessedObjects(), testCascadeRetryAfterFailureDoesNotSkipRequiredObjects()
Spec target section: OG Object Runtime / Cascade Failure Semantics

OBJ-EVENT-001 — Object Events Represent Completed Observable State
Contract statement: Object after-events, property-change events, callback notifications, and trigger-facing events
must be published only after the observable object state they describe is valid.
Rationale: UI, listeners, triggers, sync, validation, and replication treat events as semantic runtime facts.
Source scope: OAObjectEventService, OAObjectCallbackService, OAObjectSaveService, OAObjectDeleteService,
OAObjectPropertyService.
Related CODEX findings: Reverse-link failure before after-event; after-save on failed datasource save.
Suggested unit tests: testPropertyAfterEventSeesCompletedState(), testFailedSaveDoesNotFireAfterSaveEvent(),
testFailedReverseLinkUpdateDoesNotFireCompletedPropertyEvent()
Spec target section: OG Object Runtime / Event Semantics

OBJ-CALLBACK-001 — Object Callbacks Participate At The Correct Lifecycle Stage
Contract statement: Before callbacks may fail-fast/cancel according to contract; after/observer callbacks must run
only for completed stages and must not silently hide failures that affect object correctness.
Rationale: Object callbacks implement validation, lifecycle policy, UI feedback, and runtime extension behavior.
Source scope: OAObjectCallbackService, OAObjectAnnotationService, OAObjectSaveService, OAObjectDeleteService,
OAObjectEventService.
Related CODEX findings: Class-level OAObjCallback metadata processing; save/delete listener failure paths.
Suggested unit tests: testBeforeSaveCallbackCanCancelSave(), testAfterCallbackOnlyRunsForCompletedOperation(),
testCallbackFailureIsVisibleWhenItAffectsMutation()
Spec target section: OG Object Runtime / Callback Semantics

OBJ-SYNC-001 — Object Sync Hooks Require The Correct Runtime Authority
Contract statement: Object sync hooks must be invoked only when the required sync service and role exist; absent or
inappropriate sync services must no-op or fail according to contract, never silently corrupt local state.
Rationale: Object save/delete/cache/serialization paths must behave correctly in single-user, server, and client
modes.
Source scope: OAObjectCSService, OAObjectParentService, OAObjectSaveService, OAObjectDeleteService,
OAObjectSerializeService.
Related CODEX findings: OBJ-SYNC package-info invariant; local save requiring RemoteSyncInterface; remote delete
false result; child sync hook role guard.
Suggested unit tests: testObjectSyncHookNoOpsInSingleUserWhenExpected(), testClientSyncHookRequiresClientRole(),
testRemoteObjectDeleteFalseIsVisible()
Spec target section: OG Object Runtime / Sync Hook Semantics

OBJ-SERIALIZE-001 — Deserialization Resolves To The Authoritative Cached Instance
Contract statement: Deserialization must resolve graph/class/key identity to the authoritative cached instance when
one exists, or create/register a new instance only according to graph identity rules.
Rationale: Serialization round trips must not duplicate live objects or drift from graph cache identity.
Source scope: OAObjectSerializeService, OAObjectCacheService, OAObjectKeyService, OAObjectGuidService.
Related CODEX findings: Package-info serialization tests for normal stream and remote stream behavior; duplicate
deserialize merge.
Suggested unit tests: testDeserializeUsesExistingCachedInstance(),
testDeserializeNewObjectRegistersSingleAuthoritativeIdentity(), testRemoteStreamDeserializePreservesGraphIdentity()
Spec target section: OG Object Runtime / Serialization Identity

OBJ-SERIALIZE-002 — Serialization Preserves Load, Reference, And Sync Semantics
Contract statement: Object serialization must preserve reference identity, id-only state, loaded/unloaded
distinctions, and sync/client-server behavior according to stream context.
Rationale: Serialized object graphs are used by remote, sync, persistence, cache, and tooling flows.
Source scope: OAObjectSerializeService, OAObjectReflectService, OAObjectPropertyService, OAObjectCSService.
Related CODEX findings: Package-info serialization tests for normal/remote stream behavior with no sync client, sync
client, and sync server.
Suggested unit tests: testSerializeIdOnlyReferencePreservesReferenceIdentity(),
testSerializationPreservesLoadedStateByContract(), testRemoteSerializationUsesSyncRoleByContract()
Spec target section: OG Object Runtime / Serialization State Semantics

OBJ-FIND-001 — Object Find And Traversal Follow Metadata And Runtime State
Contract statement: Object find, recurse, sibling, and reflection traversal must follow OA metadata, Hub/link
ownership, loaded/reference state, and cycle-prevention semantics.
Rationale: Runtime graph traversal drives save/delete, find, validation, serialization, cascade, and tooling
behavior.
Source scope: OAObjectFindService, OAObjectRecurseService, OAObjectSiblingService, OAObjectReflectService.
Related CODEX findings: Package focus on recursive traversal, cascade behavior, and graph consistency.
Suggested unit tests: testObjectFindFollowsMetadataLinks(),
testRecursePreventsCyclesWithoutSkippingReachableObjects(), testSiblingTraversalUsesExpectedGraphScope()
Spec target section: OG Object Runtime / Traversal Semantics

OBJ-IMPORT-001 — Import/Match Resolves Existing Objects Deterministically
Contract statement: Import and match-key behavior must resolve existing objects deterministically using configured
match, key, metadata, and cache semantics.
Rationale: Importing data must not create duplicates or overwrite the wrong object.
Source scope: OAObjectImportMatchService, OAObjectUniqueService, OAObjectKeyService, OAObjectCacheService.
Related CODEX findings: Package-info object identity and cache duplicate concerns.
Suggested unit tests: testImportMatchFindsExistingObjectByConfiguredKey(),
testImportMatchDoesNotCreateDuplicateWhenCacheHasObject(), testImportMatchAmbiguityFailsVisibly()
Spec target section: OG Object Runtime / Import Match Semantics

OBJ-UNIQUE-001 — Unique Object Constraints Must Be Deterministic And Graph-Scoped
Contract statement: Unique-object lookups and constraints must use graph-scoped metadata, cache, and datasource
rules and must not return a semantically wrong object.
Rationale: Unique constraints often drive object lookup, duplicate prevention, and import behavior.
Source scope: OAObjectUniqueService, OAObjectCacheService, OAObjectDSService, OAObjectInfoService.
Related CODEX findings: Identity/cache duplicate concerns.
Suggested unit tests: testUniqueLookupUsesOwningGraph(), testUniqueLookupRejectsAmbiguousMatch(),
testUniqueCacheAndDatasourceResultsAreConsistent()
Spec target section: OG Object Runtime / Unique Object Semantics

OBJ-LOCK-001 — Object Locking Must Release State On All Completion Paths
Contract statement: Object locks must be acquired, observed, and released according to contract on success, failure,
timeout, interruption, and nested/reentrant operation paths.
Rationale: Stale locks can block save/delete/property mutation and corrupt runtime concurrency behavior.
Source scope: OALock, OAObjectLockService.
Related CODEX findings: none observed.
Suggested unit tests: testObjectLockReleasedAfterException(), testObjectLockTimeoutDoesNotLeaveStaleLock(),
testReentrantLockBehaviorMatchesContract()
Spec target section: OG Object Runtime / Lock Semantics

OBJ-SCHEDULE-001 — Scheduled Object Work Must Preserve Object Runtime Semantics
Contract statement: Object-scheduled work must execute against the intended graph/object state and must not silently
run after cancellation, shutdown, delete, or graph ownership changes unless explicitly contracted.
Rationale: Background object work can otherwise mutate stale or deleted objects and publish wrong events.
Source scope: OAObjectSchedulerService, OAObjectLockService, OAObjectEventService.
Related CODEX findings: none observed.
Suggested unit tests: testScheduledObjectWorkUsesOwningGraph(), testCancelledObjectWorkDoesNotMutateObject(),
testScheduledWorkAfterDeleteUsesDocumentedBehavior()
Spec target section: OG Object Runtime / Scheduled Work Semantics

OBJ-ENUM-001 — Enum And Calculated Metadata Behavior Must Be Deterministic
Contract statement: Enum, calculated, and metadata-derived property behavior must resolve consistently from object
metadata and not silently fall back to wrong property definitions.
Rationale: OA UI, validation, codegen, queries, and serialization depend on deterministic metadata-derived property
semantics.
Source scope: OAObjectEnumService, OAObjectAnnotationService, OAObjectInfoService.
Related CODEX findings: Calculated property metadata and annotation processing findings.
Suggested unit tests: testEnumMetadataResolutionIsDeterministic(),
testCalculatedPropertyMetadataIncludesDependencies(), testInvalidCalculatedMetadataFailsBeforePublish()
Spec target section: OG Object Runtime / Metadata-Derived Property Semantics

OBJ-FAILURE-001 — Object Operations Must Not Publish False Success
Contract statement: Object services must not mark lifecycle state complete, update cache authority, fire completed
events, emit sync/replication hooks, or continue dependent cascade work when an authoritative operation failed.
Rationale: False success creates silent data loss, stale cache state, broken retry, and graph divergence.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectInitializeService, OAObjectEventService,
OAObjectCacheService, OAObjectCSService.
Related CODEX findings: Failed save clears flags/fires events; failed initialize cache publication; remote delete
false; cascade delete partial failure.
Suggested unit tests: testFailedSaveDoesNotClearLifecycleOrFireAfterEvent(),
testFailedInitializeDoesNotPublishCacheSuccess(), testRemoteDeleteFalseDoesNotPublishDeleteSuccess()
Spec target section: OG Object Runtime / Failure Semantics

OBJ-FAILURE-002 — Partial Progress Must Be Caller-Visible And Recoverable
Contract statement: Object operations may make partial progress only when caller-visible failure or observable
incomplete state signals that completion did not occur; retry or reconciliation state must remain valid unless
failure is terminal by contract.
Rationale: OA object runtime commonly performs multi-step operations outside transactions; silent partial progress
is a correctness bug.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectDSService, OAObjectCSService,
OAObjectInitializeService, OAObjectRecurseService.
Related CODEX findings: Partial recursive save/delete, failed datasource save/delete, cache add before failed
initialize.
Suggested unit tests: testPartialSaveFailureIsVisibleAndRetryable(),
testPartialDeleteFailureIsVisibleAndRecoverable(), testFailedDatasourceOperationPreservesRetryState()
Spec target section: OG Object Runtime / Partial Progress Semantics

OBJ-TRANSACTION-001 — Transaction Participation Must Respect Object Lifecycle Stages
Contract statement: When object operations participate in transactions, lifecycle, callbacks, events, datasource
work, and cleanup must align with the current transaction stage and must not publish committed semantics before
commit authority.
Rationale: Transactional object work must not expose committed object graph state during active, failed, or rollback
stages.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectEventService, OAObjectChangeService, OALock
integration.
Related CODEX findings: Package focus on transaction participation semantics; no object-specific CODEX finding
observed.
Suggested unit tests: testObjectSaveDefersCommittedSemanticsUntilTransactionCommit(),
testRollbackPreservesObjectLifecycleStateByContract(), testTransactionFailureDoesNotFireCompletedObjectEvents()
Spec target section: OG Object Runtime / Transaction Semantics

OBJ-TL-001 — Runtime ThreadLocal State Must Be Restored
Contract statement: Object services that set runtime ThreadLocal/context flags for loading, saving, deleting, sync
suppression, callbacks, serialization, or traversal must restore prior state in finally.
Rationale: Leaked ThreadLocal state can suppress events, sync, loading, validation, callbacks, or traversal in
unrelated operations.
Source scope: OAObjectSaveService, OAObjectDeleteService, OAObjectDSService, OAObjectSerializeService,
OAObjectEventService, OAObjectParentService.
Related CODEX findings: Child sync hook role/ThreadLocal risk; package focus on thread-local/runtime-state
assumptions.
Suggested unit tests: testSaveRestoresThreadLocalFlagsAfterException(),
testDeleteRestoresSyncSuppressionAfterFailure(), testSerializationRestoresRuntimeContextAfterException()
Spec target section: OG Object Runtime / ThreadLocal Semantics

OBJ-DETERMINISM-001 — Observable Object Mutations Must Be Deterministic For The Same Graph State
Contract statement: For the same graph state, metadata, role, datasource result, and callback outcome, object
services must produce the same lifecycle state, cache state, events, sync hooks, and relationship mutations.
Rationale: Deterministic runtime behavior is required for testing, debugging, sync/replication, UI binding, and
generated application semantics.
Source scope: All com.viaoa.graph.service.object services.
Related CODEX findings: Package-wide lifecycle, identity, save/delete, metadata, and event findings.
Suggested unit tests: testSameSaveScenarioProducesSameLifecycleAndEvents(),
testSameDeleteScenarioProducesSameCacheAndEventState(), testSameMetadataInputProducesSameObjectInfo()
Spec target section: OG Object Runtime / Deterministic Mutation Semantics

*/

