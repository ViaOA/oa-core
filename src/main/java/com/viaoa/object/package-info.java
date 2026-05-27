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
 * Core object framework classes that define OA's runtime identity, event, and lifecycle model.
 * <p>
 * This package contains {@link com.viaoa.object.OAObject}, the base class for all persistent
 * and transient domain entities, along with its internal helper classes that together form
 * the core of the Object Graph runtime.
 * <p>
 * Classes prefixed with {@code OAObject*Delegate}, {@code OAObject*Helper}, or
 * {@code OAObject*Cache} are internal support classes used by {@link com.viaoa.object.OAObject}
 * to manage state, identity, event propagation, caching, cascading, and synchronization.
 * These helpers rely on package-level access to OAObject internals to maintain performance
 * and strict encapsulation—avoiding reflection or public exposure of internal fields.
 * <p>
 * External applications should interact with {@link com.viaoa.object.OAObject} and its
 * related APIs (such as {@link com.viaoa.hub.Hub}) rather than calling delegate classes
 * directly. The delegate layer is part of OA's internal implementation contract and may
 * evolve independently of the public API.
 * <p>
 * <b>Design goals:</b>
 * <ul>
 *   <li>High-performance, reflection-free internal architecture.</li>
 *   <li>Consistent object identity and referential integrity across the graph.</li>
 *   <li>Thread-safe mutation and deterministic event ordering.</li>
 *   <li>DataSource-agnostic persistence and distributed synchronization.</li>
 * </ul>
 *
 * @see com.viaoa.object.OAObject
 * @see com.viaoa.hub.Hub
 * @see com.viaoa.datasource.OADataSource
 */
package com.viaoa.object;

//CODEX unit tests <todo>

/* CODEX Invariants

OBJ-IDENTITY-001 — OAObject Runtime Identity Is Graph-Scoped
Contract statement: An OAObject instance represents one runtime identity within its owning OAGraph and must not be
treated as interchangeable with another instance unless graph identity/cache resolution defines that relationship.
Rationale: Hubs, references, cache, sync, replication, serialization, datasource merging, and equality behavior
depend on stable graph-scoped identity.
Source scope: OAObject, OAObjectKey, OAObject.getGuid, OAObject.getObjectKey, OAObject.getGraph, OAObject.equals,
OAObject.hashCode, OAObject.compareTo.
Related CODEX findings: Identity/cache drift findings from graph and object-service scans.
Suggested unit tests: testSameGraphSameKeyResolvesSameObject(),
testDifferentInstancesWithSameKeyMergeThroughCache(), testObjectEqualityUsesGraphIdentityContract()
Spec target section: Object Runtime / Identity Semantics

OBJ-IDENTITY-002 — GUID And Persistent Key Are Distinct Identity Concepts
Contract statement: GUID/runtime identity and object key/persistent identity must remain distinct; creating,
assigning, changing, serializing, or comparing one must not silently corrupt the other.
Rationale: New unsaved objects require runtime identity before persistent keys exist, while persisted/datasource
identity depends on key semantics.
Source scope: OAObject.getGuid, OAObjectKey, OAObject.getObjectKey, key/guid friend access.
Related CODEX findings: GUID/key/cache transition risks covered in graph and object-service reviews.
Suggested unit tests: testNewObjectHasGuidBeforePersistentKey(), testPersistentKeyAssignmentDoesNotChangeGuid(),
testGuidAndKeyComparisonRemainDistinct()
Spec target section: Object Runtime / GUID And Key Semantics

OBJ-KEY-001 — Object Key Reflects Current Configured ID Properties
Contract statement: An OAObjectKey must represent the current configured object ID values and GUID according to OA
metadata, including composite-key and id-only forms.
Rationale: Datasource lookup, cache lookup, serialization, sync, replication, and id-only reference behavior route
by key.
Source scope: OAObjectKey constructors/getObjectIds/getGuid/hasValidObjectIds/equals/hashCode/compareTo/toString;
OAObject.getObjectKey.
Related CODEX findings: Cache/key drift and id-only identity findings.
Suggested unit tests: testObjectKeyMatchesIdProperties(), testCompositeKeyUsesAllIdProperties(),
testIdOnlyKeyComparisonIsDeterministic()
Spec target section: Object Runtime / Key Semantics

OBJ-KEY-002 — Primary-Key Mutation Is Controlled And Reindexed
Contract statement: Primary-key changes must occur only through authorized object/datasource/runtime paths and must
update graph identity indexes consistently or fail visibly.
Rationale: Uncontrolled key changes create duplicate, unreachable, or incorrectly merged objects.
Source scope: OAObject.setProperty for ID fields, OAObject.setFkeyProperty, OAObject.getFkeyProperty,
OAObject.getObjectKey, graph key/cache services.
Related CODEX findings: Assign-id and key/cache transition findings from graph object-service review.
Suggested unit tests: testAssignIdAllowsPrimaryKeySet(), testUnauthorizedIdChangeRejectedOrReindexed(),
testFkeyPropertyMutationUpdatesReferenceKeySemantics()
Spec target section: Object Runtime / Key Mutation

OBJ-LIFECYCLE-001 — New Changed Deleted Flags Describe Completed Semantic State
Contract statement: new, changed, deleted, submitted, loading, and related lifecycle flags must describe completed
semantic object state, not attempted or failed work.
Rationale: Save/delete retry, UI state, validation, sync, replication, and datasource coordination depend on
truthful lifecycle flags.
Source scope: OAObject.isNew/getNew/setNew, isChanged/getChanged/setChanged, isDeleted/getDeleted/wasDeleted/setDel
eted, isLoading, isSubmitted/_isSubmitted.
Related CODEX findings: False-success lifecycle findings from graph object-service scans.
Suggested unit tests: testSuccessfulInsertClearsNew(), testFailedSaveDoesNotClearChanged(),
testFailedDeleteDoesNotMarkDeletedComplete()
Spec target section: Object Runtime / Lifecycle Semantics

OBJ-LIFECYCLE-002 — Deleted Objects Stop Being Authoritative Persistent Members
Contract statement: Once delete is completed, the object must not remain authoritative in cache, Hubs, links, or
datasource-facing runtime state as an active persistent object.
Rationale: Completed delete must not leave ghost cache entries, stale Hub/detail references, or sync-visible live
objects.
Source scope: OAObject.delete, OAObject.afterDelete, OAObject.setDeleted, OAObject.isDeleted, Hub/link interactions
through graph services.
Related CODEX findings: Delete/cache/Hub coordination findings from graph and object-service scans.
Suggested unit tests: testCompletedDeleteRemovesFromCache(), testCompletedDeleteRemovesFromHubs(),
testCompletedDeleteClearsAuthoritativeLinkState()
Spec target section: Object Runtime / Delete Semantics

OBJ-LIFECYCLE-003 — Undelete And State Restoration Must Validate Identity Before Publishing
Contract statement: Undelete or lifecycle restoration must validate identity/cache constraints before publishing
active state, events, or Hub membership.
Rationale: Restoring an object with conflicting key/cache state can create duplicate authoritative objects or
contradictory lifecycle state.
Source scope: OAObject.setDeleted, OAObject.wasDeleted, OAObject.getObjectKey, graph cache/key service interactions.
Related CODEX findings: setDeleted(false) conflicting-key failure path noted in object-service review.
Suggested unit tests: testUndeleteWithConflictingKeyFailsBeforePublishingActiveState(),
testFailedUndeletePreservesDeletedState()
Spec target section: Object Runtime / Lifecycle Restoration

OBJ-PROPERTY-001 — Property Get And Set Use OA Runtime Semantics
Contract statement: Property get/set must respect OA metadata, graph services, primitive-null state, reference
state, validation callbacks, change tracking, and event/sync behavior.
Rationale: OAObject properties are runtime-managed semantic values, not plain Java fields.
Source scope: OAObject.setProperty overloads, getProperty, getPropertyAsString overloads, removeProperty, setNull,
getNull, isNull.
Related CODEX findings: Primitive-null, reflection, and property mutation findings from graph/object-service scans.
Suggested unit tests: testSetPropertyStoresValueAndMarksChanged(), testGetPropertyRespectsPrimitiveNull(),
testRemovePropertyUsesOASemantics()
Spec target section: Object Runtime / Property Semantics

OBJ-PROPERTY-002 — Primitive Null Is Distinct From Primitive Default
Contract statement: Primitive-null state must distinguish unset/null from Java primitive defaults such as 0, false,
0L, and 0.0.
Rationale: Database nulls, UI display, queries, filters, validation, and serialization require this distinction.
Source scope: OAObject.nulls, setNull, getNull, isNull, primitive setProperty overloads, FriendAccess nulls methods.
Related CODEX findings: Primitive-null behavior reviewed in runtime/object-service scans.
Suggested unit tests: testPrimitiveNullDistinctFromZero(), testSetPrimitiveValueClearsPrimitiveNull(),
testSetNullForPrimitivePropertyRestoresNullState()
Spec target section: Object Runtime / Primitive Null Semantics

OBJ-PROPERTY-003 — Old/New Value Tracking Matches Completed Mutation
Contract statement: Property old/new values used for validation, callbacks, events, change tracking, and compare-
and-swap must correspond to the completed mutation being published.
Rationale: Listeners, triggers, UI binding, undo, sync, and validation depend on accurate old/new values.
Source scope: OAObject.isValidPropertyChange, getIsValidPropertyChangeObjectCallback, fireBeforePropertyChange,
firePropertyChange, fireLocalPropertyChange, compareAndSwap.
Related CODEX findings: Property after-event and reverse-link failure findings from graph object-service scans.
Suggested unit tests: testPropertyChangeEventReceivesCorrectOldNewValues(),
testRejectedPropertyChangeDoesNotPublishNewValue(), testCompareAndSwapUsesExpectedOldValue()
Spec target section: Object Runtime / Property Change Tracking

OBJ-VALIDATION-001 — Validation And Enablement Callbacks Reflect Current Object Context
Contract statement: Validation, enablement, visibility, command verification, allow-submit, and verify-save
callbacks must evaluate against the current object state, graph context, and user/access rules.
Rationale: Generated apps and UI policy depend on object-level callback decisions being deterministic and context-
correct.
Source scope: OAObject.isValidPropertyChange, isEnabled, isVisible, verifyCommand, getAllowSubmit,
getVerifySaveObjectCallback, OAObjectCallback return paths.
Related CODEX findings: Callback/context behavior reviewed in graph context and callback scans.
Suggested unit tests: testPropertyValidationUsesCurrentObjectState(), testVisibilityCallbackUsesActiveContext(),
testVerifyCommandReturnsDeterministicCallbackResult()
Spec target section: Object Runtime / Validation And Callback Semantics

OBJ-REF-001 — Reference State Distinguishes Null Unloaded And Resolved
Contract statement: Object references must distinguish confirmed null, unloaded/unresolved, id-only/key-only, loaded
object, loaded Hub, and failed-load states.
Rationale: Lazy loading, retry, serialization, sync, replication, and path traversal require explicit reference
state.
Source scope: OAObject.getObject, getHub overloads, isReferenceObjectNull, isReferenceNull, getReferenceObjectKey,
isLoaded, isPropertyLoaded, isHubLoaded, loadReferences, refresh.
Related CODEX findings: Lazy-load/reference-state findings from graph, load, find, and serialization scans.
Suggested unit tests: testUnloadedReferenceIsNotNull(), testFailedReferenceLoadRemainsRetryable(),
testReferenceObjectKeyPreservesIdOnlyState()
Spec target section: Object Runtime / Reference Semantics

OBJ-REF-002 — Bidirectional Links Stay Metadata-Consistent
Contract statement: Updating a link/reference or link-bound Hub must update corresponding reverse link, Hub
membership, master/detail state, and ownership semantics when metadata defines them.
Rationale: Object graph consistency depends on both sides of relationships agreeing.
Source scope: OAObject.setProperty for link properties, getHub, setHub, getObject, setFkeyProperty, getFkeyProperty,
Hub integration.
Related CODEX findings: Reverse-link and Hub/link consistency findings from graph and object-service reviews.
Suggested unit tests: testSetReferenceUpdatesReverseHub(), testRemoveFromHubClearsReverseReference(),
testFkeyPropertyUpdateMaintainsLinkConsistency()
Spec target section: Object Runtime / Link Semantics

OBJ-HUB-001 — Object Hub Access Must Follow Metadata And Load Semantics
Contract statement: Hub access from an OAObject must return the Hub for the declared link property, respect sort/
match/sequence options, and preserve loaded/unloaded/detail semantics according to metadata.
Rationale: Object-owned Hubs drive traversal, UI detail views, cascade save/delete, serialization, and sync
behavior.
Source scope: OAObject.getHub overloads, setHub, isHubLoaded, loadReferences, refresh(String).
Related CODEX findings: Hub/detail and metadata link findings from graph service scans.
Suggested unit tests: testGetHubReturnsMetadataLinkHub(), testGetHubSortOrderUsesDocumentedOrdering(),
testIsHubLoadedDistinguishesUnloadedFromLoadedEmpty()
Spec target section: Object Runtime / Hub Reference Semantics

OBJ-EVENT-001 — Property Change Events Represent Completed Authoritative Value Changes
Contract statement: Property change listeners must observe the new authoritative value and must not receive after-
events for rejected or failed changes.
Rationale: UI binding, triggers, sync, replication, undo, and generated logic rely on event truth.
Source scope: OAObject.fireBeforePropertyChange overloads, firePropertyChange overloads, fireLocalPropertyChange
overloads, fireNewList.
Related CODEX findings: After-event ordering and failed mutation findings from graph scans.
Suggested unit tests: testPropertyChangeEventSeesNewValue(), testRejectedPropertyChangeDoesNotFireAfterEvent(),
testLocalPropertyChangeDoesNotEmitRemoteSideEffects()
Spec target section: Object Runtime / Property Events

OBJ-EVENT-002 — Before Events Are Participants And After Events Are Observers
Contract statement: Before events/callbacks may validate or cancel according to contract; after events must be
published only after completed state and must not hide state-changing failures.
Rationale: OA event semantics define temporal correctness for UI, triggers, sync, and application listeners.
Source scope: OAObject.fireBeforePropertyChange, firePropertyChange, isValidPropertyChange, callback accessors.
Related CODEX findings: Listener/callback stage semantics from transaction, trigger, graph, and callback reviews.
Suggested unit tests: testBeforePropertyChangeCanCancelByContract(),
testAfterPropertyChangeFiresOnlyAfterCompletedSet(), testListenerFailureDoesNotPublishFalseSuccess()
Spec target section: Object Runtime / Event Stage Semantics

OBJ-EVENT-003 — Event Publication Preserves Runtime Context
Contract statement: Object event publication must not leak loading, saving, deleting, sync-suppression, admin,
remote-thread, or context flags.
Rationale: Object events run inside larger graph operations and must not contaminate later operations.
Source scope: OAObject.fireBeforePropertyChange, firePropertyChange, fireLocalPropertyChange, runtime ThreadLocal
integration.
Related CODEX findings: ThreadLocal restoration findings from runtime and graph scans.
Suggested unit tests: testPropertyEventRestoresThreadLocalFlags(), testListenerExceptionDoesNotLeakEventState(),
testSyncSuppressionRestoredAfterPropertyEvent()
Spec target section: Object Runtime / Event Context

OBJ-CACHE-001 — Cache Lookup By GUID And Key Remains Consistent
Contract statement: Cache lookup by GUID, persistent key, id-only key, and object reference must remain consistent
with current object identity state.
Rationale: Cache is the graph-level identity authority for OAObjects.
Source scope: OAObject.getGuid, getObjectKey, equals/hashCode/compareTo, serialization readResolve, graph cache/key
service integration.
Related CODEX findings: Identity/cache drift findings from graph and object-service scans.
Suggested unit tests: testCacheLookupByGuidReturnsObject(), testCacheLookupByKeyReturnsSameObject(),
testIdOnlyReferenceResolvesToCachedObject()
Spec target section: Object Runtime / Cache Semantics

OBJ-CACHE-002 — Cache Removal Follows Completed Delete Or Explicit Eviction
Contract statement: Objects must leave cache authority only after completed delete or explicit eviction semantics;
failed delete or failed lifecycle transition must not evict silently.
Rationale: Premature cache removal can create duplicate identities, unresolved references, and sync/serialization
drift.
Source scope: OAObject.delete, setDeleted, afterDelete, graph cache/delete service integration.
Related CODEX findings: Delete/cache coordination findings from graph scans.
Suggested unit tests: testFailedDeleteDoesNotEvictObject(), testCompletedDeleteEvictsByContract(),
testExplicitEvictionRemovesCacheEntry()
Spec target section: Object Runtime / Cache Removal

OBJ-GRAPH-001 — Object Operations Resolve Through Owning Graph
Contract statement: OAObject operations requiring metadata, cache, datasource, sync, Hub, serialization, trigger, or
callback behavior must resolve through the object’s owning graph.
Rationale: Multi-graph correctness depends on graph-scoped services and authority.
Source scope: OAObject.getGraph, save, delete, refresh, getObjectKey, getHub, getObject, remote/callRemote, role
helpers.
Related CODEX findings: Graph ownership and routing findings from graph/runtime scans.
Suggested unit tests: testObjectUsesOwningGraphServices(), testCrossGraphObjectDoesNotUseDefaultGraphAccidentally(),
testObjectGraphLookupIsStable()
Spec target section: Object Runtime / Graph Ownership

OBJ-SERIAL-001 — Serialization Preserves Identity Semantics
Contract statement: Serialized OAObjects must deserialize through OA identity resolution so duplicate runtime
identities are not created for existing graph keys.
Rationale: Sync, replication, storage, remote calls, and cache load require identity merge.
Source scope: OAObject.readResolve, OAObjectKey, OAObjectInternalBridge serializer friend access, graph
serialization/cache integration.
Related CODEX findings: Serialization/reference duplicate risks from graph and serialize scans.
Suggested unit tests: testDeserializeExistingKeyReturnsCachedIdentity(),
testSerializedReferencePreservesObjectKey(), testReadResolveDoesNotCreateDuplicateAuthoritativeInstance()
Spec target section: Object Runtime / Serialization Identity

OBJ-SERIAL-002 — Serialization Preserves Reference Load State
Contract statement: Serialization must preserve enough state to distinguish loaded references, unloaded references,
null references, id/key references, and failed/incomplete load state according to contract.
Rationale: Remote/sync deserialization must not accidentally materialize null, loaded-empty, or duplicate reference
state.
Source scope: OAObject.readResolve, properties array, getReferenceObjectKey, isLoaded, isReferenceNull,
serialization friend access.
Related CODEX findings: Lazy/reference serialization findings from graph/load/serialize scans.
Suggested unit tests: testSerializeUnloadedReferenceRemainsUnloaded(), testSerializeNullReferenceRemainsNull(),
testSerializeReferenceObjectKeyRoundTrips()
Spec target section: Object Runtime / Serialization Reference State

OBJ-SAVE-001 — Save Routes Through Graph Datasource Authority
Contract statement: OAObject save/saveAll must route through the owning graph’s object and datasource services and
must honor cascade rule, graph role, lifecycle flags, callbacks, and validation semantics.
Rationale: Persistence must coordinate object state, datasource identity, Hub links, events, sync, and retry
behavior.
Source scope: OAObject.save, save(int), saveAll, canSave, afterSave, getVerifySaveObjectCallback.
Related CODEX findings: Save failure and role-routing findings from graph object-service scans.
Suggested unit tests: testObjectSaveUsesOwningGraphDatasource(), testSaveAllHonorsCascadeRuleByContract(),
testFailedSaveLeavesObjectRetryable()
Spec target section: Object Runtime / Save Semantics

OBJ-DELETE-001 — Delete Routes Through Graph Lifecycle Authority
Contract statement: OAObject delete must route through the owning graph’s delete lifecycle, datasource, Hub, cache,
event, sync, and relationship coordination.
Rationale: Delete is a graph mutation and must not be reduced to a local flag change.
Source scope: OAObject.delete, canDelete, afterDelete, setDeleted, isDeleted, wasDeleted.
Related CODEX findings: Delete/cache/Hub/cascade partial-progress findings from graph object-service scans.
Suggested unit tests: testObjectDeleteUsesOwningGraphServices(), testCompletedDeleteRemovesFromCacheAndHubs(),
testFailedDeleteDoesNotPublishCompletedState()
Spec target section: Object Runtime / Delete Semantics

OBJ-REMOTE-001 — Remote Calls Preserve Object And Hub Runtime Semantics
Contract statement: Object remote calls must use the intended graph/Hub/remote runtime context and must not appear
successful when remote execution is unavailable or failed.
Rationale: Remote object behavior can affect datasource, sync, validation, and application commands.
Source scope: OAObject.callRemote, remote, isRemoteAvailable overloads, isRemoteThread, role helpers.
Related CODEX findings: Remote false-success and role findings from remote/graph scans.
Suggested unit tests: testRemoteCallUsesOwningRuntimeContext(), testRemoteUnavailableFailsOrFallbackByContract(),
testRemoteCallDoesNotUseForeignHubContext()
Spec target section: Object Runtime / Remote Semantics

OBJ-SYNC-001 — Object Mutations Emit Sync Only For Completed Semantic Changes
Contract statement: Object property/lifecycle changes must emit sync/replication messages only when the semantic
change is authoritative and completed.
Rationale: Prevents client/server and replication divergence.
Source scope: OAObject.setProperty, setDeleted, save, delete, sendMessages, startServerOnly/endServerOnly/
runOnServerOnly, role helpers.
Related CODEX findings: Sync message false-success and send-sync restoration findings from graph/runtime scans.
Suggested unit tests: testFailedPropertySetDoesNotEmitSync(), testCompletedPropertySetEmitsSyncWhenEnabled(),
testSendMessagesScopeRestoresPriorValue()
Spec target section: Object Runtime / Sync Semantics

OBJ-SYNC-002 — Client Server Object State Respects Runtime Authority
Contract statement: Client-side object changes requiring server authority must not locally claim authoritative
completion before server acceptance.
Rationale: Prevents silent local/server divergence in distributed OA runtimes.
Source scope: OAObject.isServer, isSingleUser, isClient, remote, save, delete, refresh, setProperty, role-aware
graph services.
Related CODEX findings: Client/server authority ordering findings from graph and remote scans.
Suggested unit tests: testClientRejectedPropertyChangeDoesNotPersistLocalSuccess(),
testServerAcceptedChangeUpdatesClientState(), testSingleUserDoesNotUseClientServerRouting()
Spec target section: Object Runtime / Client Server Authority

OBJ-TL-001 — Object Behavior Respects Runtime ThreadLocal Flags
Contract statement: Object property, lifecycle, save/delete, load/refresh, event, remote, and sync behavior must
respect current OAThreadLocal flags for loading, saving, deleting, server-only, remote-thread, sync suppression, and
context state.
Rationale: ThreadLocal state prevents recursive saves, unwanted events, sync loops, and access/context leakage.
Source scope: OAObject.isLoading, isRemoteThread, sendMessages, startServerOnly, endServerOnly, runOnServerOnly,
save/delete/refresh/loadReferences paths.
Related CODEX findings: ThreadLocal restoration and role flag findings from runtime and graph scans.
Suggested unit tests: testLoadingFlagSuppressesUserChangeSemantics(), testDeletingFlagRestoredAfterDeleteFailure(),
testServerOnlyScopeRestoresAfterException()
Spec target section: Object Runtime / ThreadLocal Semantics

OBJ-CONTEXT-001 — Context And User Access Apply Consistently To Object Behavior
Contract statement: Object access, visibility, enablement, command verification, validation, and mutation behavior
that depends on context/user state must use the active graph/runtime context.
Rationale: Generated apps require consistent user-specific behavior across object APIs.
Source scope: OAObject.isEnabled, isVisible, verifyCommand, getAllowSubmit, validation callbacks, context/user-
access integration.
Related CODEX findings: Context/user-access behavior reviewed in graph context and secure scans.
Suggested unit tests: testUserAccessBlocksUnauthorizedPropertyChange(),
testContextDoesNotLeakAcrossObjectOperations(), testVisibilityUsesActiveContext()
Spec target section: Object Runtime / Context Semantics

OBJ-LOCK-001 — Object Lock State Is Observable And Releasable
Contract statement: Object lock/unlock/isLocked and property-lock behavior must reflect graph/runtime lock authority
and must release state on success, failure, timeout, or interruption according to contract.
Rationale: Locks protect object mutation, save/delete, and concurrent graph consistency.
Source scope: OAObject.lock, unlock, isLocked, isPropertyLocked, compareAndSwap with distributed lock option.
Related CODEX findings: Lock/concurrency risks from graph object-service scans.
Suggested unit tests: testObjectLockUnlockRoundTrip(), testObjectLockReleasedAfterException(),
testCompareAndSwapWithDistributedLockUsesLockContract()
Spec target section: Object Runtime / Lock Semantics

OBJ-COPY-001 — Copy And Clone Behavior Preserve Defined Object Semantics
Contract statement: Object copy helpers must define whether identity, keys, GUID, lifecycle flags, primitive-null
state, loaded references, Hubs, and excluded properties are copied or reset.
Rationale: Copy behavior can accidentally duplicate identity or carry runtime state into a new object.
Source scope: OAObject.createCopy, createCopy(String[]), copyInto, setObjectDefaults.
Related CODEX findings: No direct CODEX finding observed; source/API behavior implies contract.
Suggested unit tests: testCreateCopyDoesNotDuplicateRuntimeIdentityByContract(),
testCopyIntoCopiesExpectedPropertiesOnly(), testCreateCopyHonorsExcludedProperties()
Spec target section: Object Runtime / Copy Semantics

OBJ-FIND-001 — Object Find And Hierarchical Search Use OA Path Semantics
Contract statement: Object find, findAll, and hierarchical find must traverse according to OA path, link, Hub,
reference, filter, and load semantics without returning false matches.
Rationale: Object-level search is used by generated apps and runtime logic across object graphs.
Source scope: OAObject.find, findAll, hierFind.
Related CODEX findings: Finder/path traversal findings from find and graph scans.
Suggested unit tests: testObjectFindUsesOAPathSemantics(), testFindAllReturnsAllMatchingReachableObjects(),
testHierFindUsesHierarchyPathByContract()
Spec target section: Object Runtime / Find Semantics

OBJ-LOAD-001 — Reference Loading Is Bounded And Metadata-Driven
Contract statement: loadReferences must load references according to metadata, one/many flags, include-calc flag,
depth limits, owned-level limits, and max-reference limits.
Rationale: Bulk reference loading must avoid infinite traversal, unexpected eager loading, and missed required
references.
Source scope: OAObject.loadReferences overloads, refresh, refresh(String), isLoaded, isPropertyLoaded.
Related CODEX findings: Recursive traversal/lazy-load findings from graph/load/find scans.
Suggested unit tests: testLoadReferencesHonorsOneManyFlags(), testLoadReferencesHonorsDepthLimits(),
testLoadReferencesIncludesCalcOnlyWhenRequested()
Spec target section: Object Runtime / Reference Loading

OBJ-UNIQUE-001 — Unique Lookup Uses Metadata And Graph Scope
Contract statement: Unique lookup and uniqueness checks must resolve values using owning graph, metadata, cache,
datasource, and property semantics without returning the wrong object.
Rationale: Unique-object behavior prevents duplicates and drives import/match logic.
Source scope: OAObject.isUnique, getUniqueInstance.
Related CODEX findings: Import/match and unique identity concerns from object-service scans.
Suggested unit tests: testIsUniqueUsesOwningGraphScope(), testGetUniqueInstanceFindsExistingObjectByProperty(),
testAmbiguousUniqueLookupFailsByContract()
Spec target section: Object Runtime / Unique Object Semantics

OBJ-FKEY-001 — Foreign-Key Property Access Preserves Link Semantics
Contract statement: Foreign-key property get/set helpers must update or read the corresponding link/key relationship
according to metadata and must not silently detach key state from reference state.
Rationale: FK helpers bridge datasource fields and object links, and drift corrupts lazy loading, save, query, and
serialization.
Source scope: OAObject.setFkeyProperty overloads, getFkeyProperty overloads, getReferenceObjectKey.
Related CODEX findings: Invalid foreign-key metadata findings from graph object-service scan.
Suggested unit tests: testSetFkeyPropertyUpdatesReferenceKeyByContract(), testGetFkeyPropertyReadsLinkedObjectKey(),
testInvalidFkeyPropertyFailsVisibly()
Spec target section: Object Runtime / Foreign Key Semantics

OBJ-ENUM-001 — Name Value Metadata Access Is Deterministic
Contract statement: Name/value metadata access must return deterministic values for the requested property according
to object metadata and active graph context.
Rationale: UI choices, validation, and generated forms rely on stable name/value metadata.
Source scope: OAObject.getNameValues, enum/name-value metadata integration.
Related CODEX findings: Metadata-derived behavior reviewed in annotation/object-service scans.
Suggested unit tests: testGetNameValuesUsesPropertyMetadata(),
testGetNameValuesUnknownPropertyUsesDefinedFallback(), testNameValuesAreDeterministicForSameMetadata()
Spec target section: Object Runtime / Name Value Semantics

OBJ-BRIDGE-001 — Internal Bridge Access Does Not Become Public Object Semantics
Contract statement: FriendAccess and internal bridge APIs may expose package/runtime internals to infrastructure but
must not define application-facing OAObject behavior.
Rationale: Internal access preserves performance and encapsulation while keeping the public OAObject contract
stable.
Source scope: OAObjectInternalBridge, OAObject.FriendAccess, metadata/serializer friend accessors.
Related CODEX findings: Internal surface and package-boundary findings from graph reviews.
Suggested unit tests: testApplicationSurfaceDoesNotRequireFriendAccess(),
testFriendAccessMutatesOnlyDocumentedInternalState(), testInternalBridgeDoesNotBypassGraphAuthority()
Spec target section: Object Runtime / Internal API Boundary

OBJ-LOCAL-001 — Local Objects Have Explicit Non-Persistent Runtime Semantics
Contract statement: OAObjectLocal instances must define local/transient metadata, identity, save/delete, cache, and
graph behavior explicitly and must not be mistaken for normal persistent objects.
Rationale: Local helper objects can participate in UI/runtime behavior without datasource persistence semantics.
Source scope: OAObjectLocal, OAObjectLocal.getOAObjectInfo.
Related CODEX findings: No direct CODEX finding observed; source/API behavior implies contract.
Suggested unit tests: testOAObjectLocalHasLocalObjectInfo(),
testOAObjectLocalDoesNotUsePersistentDatasourceByContract(), testOAObjectLocalLifecycleFlagsBehaveByContract()
Spec target section: Object Runtime / Local Object Semantics

OBJ-FAILURE-001 — Visible Failure Means Object Operation Is Incomplete
Contract statement: If an object operation throws or returns visible failure, callers may treat the operation as
incomplete; object state must not falsely claim successful completion.
Rationale: OA allows partial progress, but false success breaks retry, UI state, sync, and reconciliation.
Source scope: OAObject setProperty/save/delete/refresh/loadReferences/remote/find operations and graph service
delegation.
Related CODEX findings: Partial-progress semantics clarified in graph/object-service scans.
Suggested unit tests: testFailedPropertySetDoesNotFireCompletionEvent(), testFailedSaveLeavesObjectRetryable(),
testFailedRefreshDoesNotMarkReferenceLoaded()
Spec target section: Object Runtime / Failure Semantics

OBJ-FAILURE-002 — Retry Remains Possible After Caller-Visible Failure
Contract statement: After caller-visible failure, object state must remain retryable, refreshable, or explicitly
terminal according to contract.
Rationale: Applications can retry, refresh, reconcile, or use transaction boundaries after failures.
Source scope: OAObject save/delete/refresh/loadReferences/setProperty/remote and lifecycle state.
Related CODEX findings: Retry correctness findings from graph/object/load scans.
Suggested unit tests: testFailedLazyReferenceLoadCanRetry(), testFailedDeleteCanRetryOrReconcile(),
testFailedRemoteCallDoesNotPoisonObjectState()
Spec target section: Object Runtime / Retry Semantics

OBJ-DETERMINISM-001 — Same Object State And Metadata Produce Same Observable Behavior
Contract statement: For the same graph, metadata, object state, context, ThreadLocal state, datasource result, and
callback outcome, OAObject APIs must produce deterministic property, lifecycle, event, key, Hub, serialization, and
sync behavior.
Rationale: Deterministic object behavior is required for generated apps, tests, debugging, sync/replication, and
datasource consistency.
Source scope: OAObject public/protected APIs, OAObjectKey, OAObjectLocal, OAObjectInternalBridge boundaries.
Related CODEX findings: Package-wide identity, lifecycle, property, graph, serialization, and failure findings.
Suggested unit tests: testSamePropertySetScenarioProducesSameChangedStateAndEvents(),
testSameSaveScenarioProducesSameLifecycleAndKeyState(),
testSameSerializationScenarioProducesSameIdentityResolution()
Spec target section: Object Runtime / Deterministic Object Semantics

*/


