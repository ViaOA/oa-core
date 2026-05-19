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


/* CODEX Invariants

  1. Object Identity Contracts

  OBJ-IDENTITY-001 — OAObject Runtime Identity Is Graph-Scoped
  Contract statement: An OAObject instance represents one graph-scoped runtime identity and must not be treated as
  interchangeable with another instance unless graph identity/cache resolution says so.
  Rationale: Hubs, references, sync, serialization, and datasource merging depend on stable object identity.
  Source locations: OAObject, OAObjectKey, graph object cache/key/guid services, object delegate entry points.
  Known related CODEX findings: identity/cache drift issues were handled in graph scans.
  Suggested unit tests: testSameGraphSameKeyResolvesSameObject(),
  testDifferentInstancesWithSameKeyMergeThroughCache()
  Spec target section: Object Runtime / Identity Semantics

  OBJ-IDENTITY-002 — GUID Is Runtime Identity, Key Is Persistent Identity
  Contract statement: GUID/runtime identity and object key/persistent identity must remain distinct concepts;
  changing one must not silently corrupt the other.
  Rationale: New unsaved objects need runtime identity before persistent keys exist.
  Source locations: OAObject, OAObjectKey, guid/key services.
  Known related CODEX findings: key/cache transition risks covered in graph review.
  Suggested unit tests: testNewObjectHasRuntimeIdentityBeforeKey(), testPersistentKeyChangeUpdatesCacheIndex()
  Spec target section: Object Runtime / GUID vs Key

  2. Object Key / Primary-Key Contracts

  OBJ-KEY-001 — Object Key Must Reflect Current Primary-Key Properties
  Contract statement: An object key must represent the current values of the object’s configured ID properties.
  Rationale: Datasource lookup, cache lookup, sync, and replication route by key.
  Source locations: OAObjectKey, OAObject, key service, metadata OAObjectInfo.
  Known related CODEX findings: cache/key drift issues mapped here.
  Suggested unit tests: testObjectKeyMatchesIdProperties(), testCompositeKeyUsesAllIdProperties()
  Spec target section: Object Runtime / Primary-Key Semantics

  OBJ-KEY-002 — Primary-Key Mutation Must Be Controlled
  Contract statement: Primary-key changes must be allowed only through authorized datasource/object-service paths
  and must update identity indexes consistently.
  Rationale: Uncontrolled ID changes create duplicate or unreachable objects.
  Source locations: OAObject, property set paths, datasource assign-id paths, key/cache services.
  Known related CODEX findings: assign-id and key transition risks noted.
  Suggested unit tests: testAssignIdAllowsPrimaryKeySet(), testUnauthorizedIdChangeRejectedOrReindexed()
  Spec target section: Object Runtime / Key Mutation

  3. Object Lifecycle Contracts

  OBJ-LIFECYCLE-001 — New/Changed/Deleted Flags Must Describe Semantic State
  Contract statement: new, changed, and deleted state must reflect completed semantic lifecycle, not attempted or
  failed work.
  Rationale: Save/delete, retry, UI state, and replication depend on truthful lifecycle flags.
  Source locations: OAObject, lifecycle delegate/service methods, save/delete services.
  Known related CODEX findings: false-success lifecycle bugs were found/fixed in graph scans.
  Suggested unit tests: testFailedSaveDoesNotClearChanged(), testSuccessfulInsertClearsNew(),
  testFailedDeleteDoesNotMarkDeletedComplete()
  Spec target section: Object Runtime / Lifecycle Semantics

  OBJ-LIFECYCLE-002 — Deleted Objects Must Not Remain Authoritative Members
  Contract statement: Once delete is completed, the object must not remain authoritative in cache/Hubs as an active
  persistent object.
  Rationale: Prevents ghost objects and stale Hub/detail references.
  Source locations: OAObject, object delete service, cache service, Hub remove/delete services.
  Known related CODEX findings: delete/cache/Hub coordination issues covered in graph scans.
  Suggested unit tests: testCompletedDeleteRemovesFromCache(), testCompletedDeleteRemovesFromHubs()
  Spec target section: Object Runtime / Delete Semantics

  4. Property Storage Contracts

  OBJ-PROPERTY-001 — Property Values Must Be Retrieved Through OA Semantics
  Contract statement: Property get/set must respect OA metadata, primitive-null state, reference state, events, and
  graph services.
  Rationale: Direct field semantics are insufficient for OAObject runtime behavior.
  Source locations: OAObject, property delegate/service, reflection service.
  Known related CODEX findings: primitive-null and reflection issues reviewed earlier.
  Suggested unit tests: testSetPropertyStoresValueAndMarksChanged(), testGetPropertyRespectsPrimitiveNull()
  Spec target section: Object Runtime / Property Semantics

  OBJ-PROPERTY-002 — Primitive Null Is Distinct From Primitive Default
  Contract statement: OA primitive-null state must distinguish “unset/null” from Java primitive default values like
  0 or false.
  Rationale: Database nulls, UI state, and query/filter behavior require this distinction.
  Source locations: OAObject, primitive-null helpers, reflect/property services.
  Known related CODEX findings: OAReflect primitive-null behavior reviewed.
  Suggested unit tests: testPrimitiveNullDistinctFromZero(), testSetPrimitiveValueClearsPrimitiveNull()
  Spec target section: Object Runtime / Primitive Null Semantics

  5. Reference / Link State Contracts

  OBJ-REF-001 — Reference State Must Distinguish Null, Unloaded, And Resolved
  Contract statement: A reference must not collapse unresolved/unloaded state into null unless authoritative loading
  confirms absence.
  Rationale: Lazy load and sync references must remain retryable.
  Source locations: OAObject, reference/link property services, object reflect service.
  Known related CODEX findings: lazy-load/reference state bugs covered in graph scan.
  Suggested unit tests: testUnloadedReferenceIsNotNull(), testFailedReferenceLoadRemainsRetryable()
  Spec target section: Object Runtime / Reference Semantics

  OBJ-REF-002 — Bidirectional Links Must Stay Consistent
  Contract statement: Updating a link/reference must update the corresponding reverse link or Hub membership when
  metadata defines one.
  Rationale: Object graph consistency depends on both sides of relationships agreeing.
  Source locations: OAObject, link service, Hub link/detail services.
  Known related CODEX findings: Hub/link consistency findings mapped here.
  Suggested unit tests: testSetReferenceUpdatesReverseHub(), testRemoveFromHubClearsReverseReference()
  Spec target section: Object Runtime / Link Semantics

  6. Property Change / Event Contracts

  OBJ-EVENT-001 — Property Change Events Fire After Authoritative Value Change
  Contract statement: Property change listeners must observe the new authoritative value and must not receive after-
  events for rejected changes.
  Rationale: UI binding, triggers, sync, and generated logic rely on event truth.
  Source locations: OAObject, property event services, trigger/sync hooks.
  Known related CODEX findings: after-event ordering issues covered in graph scans.
  Suggested unit tests: testPropertyChangeEventSeesNewValue(), testRejectedPropertyChangeDoesNotFireAfterEvent()
  Spec target section: Object Runtime / Property Events

  OBJ-EVENT-002 — Event Publication Must Preserve ThreadLocal State
  Contract statement: Property/object event publication must not leak loading/saving/deleting/sync-suppression
  flags.
  Rationale: Event code runs inside larger graph operations.
  Source locations: OAObject, event service, runtime thread-local service.
  Known related CODEX findings: thread-local restoration issues covered in runtime/graph scans.
  Suggested unit tests: testPropertyEventRestoresThreadLocalFlags(), testListenerExceptionDoesNotLeakEventState()
  Spec target section: Object Runtime / Event Context

  7. Cache Interaction Contracts

  OBJ-CACHE-001 — Cache Indexes Must Track Object Key And GUID
  Contract statement: Object cache lookup by GUID and persistent key must remain consistent with current object
  identity state.
  Rationale: Cache is the graph identity authority.
  Source locations: OAObject, cache service, key/guid services.
  Known related CODEX findings: identity/cache drift issues found in graph scans.
  Suggested unit tests: testCacheLookupByGuidReturnsObject(), testCacheLookupByKeyReturnsSameObject()
  Spec target section: Object Runtime / Cache Semantics

  OBJ-CACHE-002 — Cache Removal Must Follow Completed Delete Or Explicit Eviction
  Contract statement: Objects must be removed from cache only after completed delete or explicit cache eviction
  semantics.
  Rationale: Premature removal can create duplicate identities or unresolved references.
  Source locations: OAObject, cache service, delete service.
  Known related CODEX findings: delete/cache coordination reviewed.
  Suggested unit tests: testFailedDeleteDoesNotEvictObject(), testExplicitEvictionRemovesCacheEntry()
  Spec target section: Object Runtime / Cache Removal

  8. Graph Ownership Contracts

  OBJ-GRAPH-001 — Object Operations Resolve Through Owning Graph
  Contract statement: OAObject operations requiring metadata, cache, datasource, sync, or Hub behavior must resolve
  through the object’s graph.
  Rationale: Multi-graph/runtime correctness depends on graph-scoped services.
  Source locations: OAObject, OARuntime.graph(object), graph object services.
  Known related CODEX findings: graph ownership/routing findings mapped here.
  Suggested unit tests: testObjectUsesOwningGraphServices(),
  testCrossGraphObjectDoesNotUseDefaultGraphAccidentally()
  Spec target section: Object Runtime / Graph Ownership

  9. Serialization Contracts

  OBJ-SERIAL-001 — Serialization Preserves Identity Semantics
  Contract statement: Serialized OAObjects must deserialize through OA identity resolution so duplicate runtime
  identities are not created for existing graph keys.
  Rationale: Sync, replication, storage, and object-cache load depend on identity merge.
  Source locations: OAObject, serialization hooks, OAObjectSerializer, object cache service.
  Known related CODEX findings: serialization/reference duplicate risks covered in graph/serialize scans.
  Suggested unit tests: testDeserializeExistingKeyReturnsCachedIdentity(),
  testSerializedReferencePreservesObjectKey()
  Spec target section: Object Runtime / Serialization Identity

  OBJ-SERIAL-002 — Serialization Must Preserve Reference Load State
  Contract statement: Serialization must preserve enough state to distinguish loaded references, unloaded
  references, null references, and object-key references.
  Rationale: Remote/sync deserialization must not accidentally materialize null or loaded-empty state.
  Source locations: OAObject, serialization hooks, reference services.
  Known related CODEX findings: lazy/reference serialization risks noted.
  Suggested unit tests: testSerializeUnloadedReferenceRemainsUnloaded(), testSerializeNullReferenceRemainsNull()
  Spec target section: Object Runtime / Serialization Reference State

  10. Sync / Replication Interaction Contracts

  OBJ-SYNC-001 — Object Mutations Emit Sync Only For Completed Semantic Changes
  Contract statement: Object property/lifecycle changes must emit sync/replication messages only when the semantic
  change is authoritative and completed.
  Rationale: Prevents client/server and replication divergence.
  Source locations: OAObject, property service, sync/replication services.
  Known related CODEX findings: sync message false-success findings covered in graph scans.
  Suggested unit tests: testFailedPropertySetDoesNotEmitSync(), testCompletedPropertySetEmitsSyncWhenEnabled()
  Spec target section: Object Runtime / Sync Semantics

  OBJ-SYNC-002 — Client/Server Object State Must Respect Role Authority
  Contract statement: Client object changes requiring server authority must not locally claim authoritative
  completion before server acceptance.
  Rationale: Prevents silent local/server divergence.
  Source locations: OAObject, sync services, datasource client/server paths.
  Known related CODEX findings: CS authority ordering issues reviewed.
  Suggested unit tests: testClientRejectedPropertyChangeDoesNotPersistLocalSuccess(),
  testServerAcceptedChangeUpdatesClientState()
  Spec target section: Object Runtime / Client-Server Authority

  11. ThreadLocal / Context-Dependent Contracts

  OBJ-TL-001 — Object Behavior Must Respect Loading/Saving/Deleting Flags
  Contract statement: Object property/lifecycle behavior must respect current OAThreadLocal flags for loading,
  saving, deleting, and sync suppression.
  Rationale: Avoids recursive saves, unwanted events, and sync loops.
  Source locations: OAObject, property service, DS service, runtime thread-local service.
  Known related CODEX findings: thread-local flag restoration issues covered.
  Suggested unit tests: testLoadingFlagSuppressesUserChangeSemantics(), testDeletingFlagRestoredAfterDeleteFailure()
  Spec target section: Object Runtime / ThreadLocal Semantics

  OBJ-CONTEXT-001 — Context/User Access Must Be Applied Consistently
  Contract statement: Object access and mutation rules that depend on context/user state must use the active graph/
  runtime context.
  Rationale: Generated apps need consistent user-specific behavior.
  Source locations: OAObject, context/user-access services, property callbacks.
  Known related CODEX findings: context/user-access behavior reviewed in graph context pass.
  Suggested unit tests: testUserAccessBlocksUnauthorizedPropertyChange(),
  testContextDoesNotLeakAcrossObjectOperations()
  Spec target section: Object Runtime / Context Semantics

  12. Failure / Retry Contracts

  OBJ-FAILURE-001 — Visible Failure Means Operation Is Incomplete
  Contract statement: If an object operation throws, caller must be able to treat it as incomplete; object state
  must not falsely claim successful completion.
  Rationale: OA allows partial progress, but false success breaks retry and reconciliation.
  Source locations: OAObject, save/delete/property services.
  Known related CODEX findings: partial-progress semantics clarified in graph scans.
  Suggested unit tests: testFailedPropertySetDoesNotFireCompletionEvent(), testFailedSaveLeavesObjectRetryable()
  Spec target section: Object Runtime / Failure Semantics

  OBJ-FAILURE-002 — Retry Must Remain Possible After Caller-Visible Failure
  Contract statement: After caller-visible failure, object state must remain retryable unless the operation
  explicitly documents terminal state.
  Rationale: Applications can retry, refresh, reconcile, or use transaction boundaries.
  Source locations: OAObject, lifecycle services, datasource service, reference loading.
  Known related CODEX findings: retry correctness findings covered in graph/object scans.
  Suggested unit tests: testFailedLazyReferenceLoadCanRetry(), testFailedDeleteCanRetryOrReconcile()
  Spec target section: Object Runtime / Retry Semantics

  13. Test Coverage Matrix

  Identity/key:

  - testSameGraphSameKeyResolvesSameObject
  - testNewObjectHasRuntimeIdentityBeforeKey
  - testObjectKeyMatchesIdProperties
  - testPrimaryKeyMutationUpdatesCacheIndex

  Lifecycle:

  - testSuccessfulInsertClearsNew
  - testFailedSaveDoesNotClearChanged
  - testFailedDeleteDoesNotMarkDeletedComplete
  - testCompletedDeleteRemovesFromCacheAndHubs

  Property/reference:

  - testSetPropertyStoresValueAndMarksChanged
  - testPrimitiveNullDistinctFromZero
  - testUnloadedReferenceIsNotNull
  - testFailedReferenceLoadRemainsRetryable
  - testSetReferenceUpdatesReverseHub

  Events:

  - testPropertyChangeEventSeesNewValue
  - testRejectedPropertyChangeDoesNotFireAfterEvent
  - testPropertyEventRestoresThreadLocalFlags

  Cache/graph:

  - testCacheLookupByGuidReturnsObject
  - testCacheLookupByKeyReturnsSameObject
  - testObjectUsesOwningGraphServices
  - testCrossGraphObjectDoesNotUseDefaultGraphAccidentally

  Serialization:

  - testDeserializeExistingKeyReturnsCachedIdentity
  - testSerializedReferencePreservesObjectKey
  - testSerializeUnloadedReferenceRemainsUnloaded

  Sync/context/failure:

  - testCompletedPropertySetEmitsSyncWhenEnabled
  - testFailedPropertySetDoesNotEmitSync
  - testClientRejectedPropertyChangeDoesNotPersistLocalSuccess
  - testLoadingFlagSuppressesUserChangeSemantics
  - testUserAccessBlocksUnauthorizedPropertyChange
  - testFailedSaveLeavesObjectRetryable


*/



