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
 * Provides classes that implement lightweight autonumber assignment for
 * {@link com.viaoa.object.OAObject} instances.
 * <p>
 * This package contains a minimal {@link com.viaoa.datasource.OADataSource}
 * implementation whose sole responsibility is assigning sequential numeric
 * identifiers to objects. It does not support persistence, selection, query
 * execution, or storage operations.
 * <p>
 * Autonumber assignment can operate in two modes:
 * <ul>
 *   <li><b>Global mode:</b> a shared Hub of {@code NextNumber} sequences is used
 *       by all datasource instances.</li>
 *   <li><b>Local mode:</b> callers may supply their own Hub instance, allowing
 *       autonumber sequences to be isolated per usage context.</li>
 * </ul>
 * <p>
 * The package-level functionality supports:
 * <ul>
 *   <li>auto-assigning IDs during object construction or insertion, depending
 *       on configuration</li>
 *   <li>lazy creation of per-class {@code NextNumber} sequences</li>
 *   <li>class-level filtering that determines whether autonumber values may be
 *       assigned to a given type</li>
 * </ul>
 */
package com.viaoa.datasource.autonumber;


/* CODEX Invariants

AUTONUM-RUNTIME-001 — Generated Identity Authority
Contract statement:
com.viaoa.datasource.autonumber defines lightweight OA generated-identity assignment for OAObjects when a full
persistence datasource is not responsible for assigning numeric IDs.
Rationale:
Auto-number assignment participates in OA object identity, cache keys, object lifecycle, and graph consistency even
though this datasource does not provide storage, selection, query, update, or delete persistence.
Source scope:
OADataSourceAuto, NextNumber, OADataSource integration, OAObject identity metadata, OARuntime graph/object services.
Related CODEX findings:
OADataSourceAuto source notes local/negative-number identity question and concurrent startNextNumber assignment
risk.
Suggested unit tests:
testAutoNumberAssignsGeneratedIdForAutoAssignProperty(), testAutoNumberDatasourceDoesNotClaimStorageSupport(),
testGeneratedIdentityMatchesObjectInfoIdMetadata()
Spec target section:
Datasource Autonumber / Core Responsibility

AUTONUM-SCOPE-001 — Global and Local Sequence Scope
Contract statement:
Auto-number sequences must be scoped deterministically: global mode uses the shared global NextNumber Hub, while
local mode uses the caller-supplied NextNumber Hub and must not share sequence state across unrelated local
contexts.
Rationale:
Sequence scope determines whether two datasource instances allocate from the same identity space or isolated
identity spaces.
Source scope:
OADataSourceAuto.getGlobalNextNumbers(), setGlobalNextNumbers(...), getNextNumbers(),
OADataSourceAuto(Hub<NextNumber>, boolean), OADataSourceAuto(Hub).
Related CODEX findings:
None directly beyond package-info documenting global and local modes.
Suggested unit tests:
testGlobalAutoNumberInstancesShareSequenceHub(), testLocalAutoNumberInstanceUsesSuppliedSequenceHub(),
testLocalAndGlobalSequencesRemainIsolated()
Spec target section:
Datasource Autonumber / Sequence Scope

AUTONUM-SUPPORT-001 — Class Support Semantics
Contract statement:
Auto-number assignment must occur only for classes supported by OADataSourceAuto and only when a NextNumber sequence
can resolve an auto-assign ID property for the class.
Rationale:
Generated identity must not be assigned to classes or properties that are not part of the datasource’s declared
authority.
Source scope:
OADataSourceAuto.isClassSupported(...), getSupportAllClasses(), setSupportAllClasses(...), getNextNumber(...),
NextNumber.getProperty().
Related CODEX findings:
None observed.
Suggested unit tests:
testSupportAllClassesAllowsLazySequenceCreation(), testUnsupportedClassDoesNotReceiveGeneratedId(),
testClassWithoutAutoAssignIdDoesNotReceiveGeneratedId()
Spec target section:
Datasource Autonumber / Class Support

AUTONUM-METADATA-001 — Metadata-Driven ID Property Selection
Contract statement:
Auto-number property selection must be driven by OA runtime metadata: the selected property must be one of the class
ID properties and must be marked auto-assign.
Rationale:
Assigning generated values to a non-ID or non-auto-assign property corrupts object identity and cache-key behavior.
Source scope:
OADataSourceAuto._getNextNumber(...), OARuntime.graph(clazz), OAObjectInfo.getIdProperties(),
OAPropertyInfo.getAutoAssign(), NextNumber.setProperty(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testAutoNumberSelectsAutoAssignIdProperty(), testAutoNumberIgnoresNonAutoAssignIdProperty(),
testAutoNumberDoesNotAssignWhenNoIdMetadataExists()
Spec target section:
Datasource Autonumber / Metadata Semantics

AUTONUM-LIFECYCLE-001 — Assignment Timing
Contract statement:
Generated IDs must be assigned at the configured lifecycle boundary: on object creation when assignIdOnCreate is
enabled by the datasource chain, or during insert/insertWithoutReferences when assignIdOnCreate is disabled.
Rationale:
OA object lifecycle state, cache visibility, and datasource save behavior depend on predictable timing for
persistent key assignment.
Source scope:
OADataSourceAuto.assignId(...), insert(...), insertWithoutReferences(...), OADataSource.getAssignIdOnCreate().
Related CODEX findings:
None observed.
Suggested unit tests:
testAssignIdOnCreateAssignsBeforeInsertBoundary(), testInsertAssignsIdWhenAssignOnCreateDisabled(),
testInsertWithoutReferencesAssignsIdWhenAssignOnCreateDisabled()
Spec target section:
Datasource Autonumber / Identity Lifecycle Timing

AUTONUM-SEQUENCE-001 — Per-Class Sequence Identity
Contract statement:
Each NextNumber sequence must identify exactly one class and one selected auto-number property; repeated lookup for
the same class must return the same sequence within the datasource scope.
Rationale:
Stable class-to-sequence mapping prevents duplicate or conflicting key allocation for the same model class.
Source scope:
NextNumber.getId(), setId(...), getProperty(), setProperty(...), OADataSourceAuto.hmClassNextNumber,
OADataSourceAuto._getNextNumber(...), Hub<NextNumber>.find(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testNextNumberSequenceCreatedOncePerClass(), testSequenceIdUsesClassName(),
testRepeatedLookupReturnsSameNextNumber()
Spec target section:
Datasource Autonumber / Sequence Identity

AUTONUM-ALLOC-001 — Unique Allocation Within Sequence
Contract statement:
For a given class sequence, each successful generated ID allocation must reserve a unique numeric value and advance
the sequence so that the same value is not allocated twice within the sequence scope.
Rationale:
Generated IDs are object identity keys. Duplicate allocation corrupts cache, datasource, sync, replication, and
graph semantics.
Source scope:
OADataSourceAuto.assignId(...), OADataSourceAuto.getNextNumber(...), NextNumber.getNext(), NextNumber.setNext(...).
Related CODEX findings:
Concurrent startNextNumber adjustment can reset NextNumber.next outside synchronized(nn), creating duplicate
assigned IDs.
Suggested unit tests:
testSequentialAssignIdProducesUniqueIds(), testConcurrentAssignIdProducesNoDuplicateIds(),
testStartingNextNumberConcurrentAssignmentProducesNoDuplicateIds()
Spec target section:
Datasource Autonumber / Allocation Semantics

AUTONUM-START-001 — Starting Number Boundary
Contract statement:
Configured starting next number must be applied atomically with sequence allocation and must never move an active
sequence backward below an already allocated value.
Rationale:
Starting-number configuration is an identity boundary. Applying it outside the allocation critical section can
create duplicates or regressions.
Source scope:
OADataSourceAuto.setStartingNextNumber(...), getStartingNextNumber(), getNextNumber(...), assignId(...),
NextNumber.getNext(), NextNumber.setNext(...).
Related CODEX findings:
getNextNumber adjusts nn.next outside synchronized(nn) while assignId increments inside synchronized(nn).
Suggested unit tests:
testStartingNextNumberRaisesInitialSequence(), testStartingNextNumberDoesNotDecreaseExistingSequence(),
testStartingNextNumberAdjustmentIsAtomicWithAllocation()
Spec target section:
Datasource Autonumber / Starting Sequence Semantics

AUTONUM-CACHE-001 — Cache Collision Avoidance
Contract statement:
Before assigning a generated ID, the datasource must avoid assigning a value already present in the active graph/
cache for the target class, and must continue allocation until an unused value is found or fail visibly.
Rationale:
Object cache identity is authoritative at runtime; generated IDs must not create duplicate cached objects for the
same class/key.
Source scope:
OADataSourceAuto.assignId(...), OARuntime.graph(oaObj), callObjectCacheGetObject(...), OAObject.setProperty(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testAssignIdSkipsExistingCachedKey(), testAssignIdContinuesUntilUnusedCacheKey(),
testAssignIdCollisionFailureDoesNotAssignDuplicateKey()
Spec target section:
Datasource Autonumber / Cache Identity Consistency

AUTONUM-ASSIGN-001 — Assignment Guarding
Contract statement:
Setting an auto-generated ID on an OAObject must occur inside the datasource assigning-ID guard and must restore
that guard with try/finally on success, failure, and early exit after the guard is set.
Rationale:
OA object services need to distinguish datasource-generated identity assignment from normal user property mutation.
Source scope:
OADataSourceAuto.assignId(...), OARuntime.graph(oaObj), callObjectDSSetAssigningId(...), OAObject.setProperty(...).
Related CODEX findings:
Current assignment path uses try/finally around the assigning-ID guard.
Suggested unit tests:
testAssignIdSetsAndClearsAssigningIdGuard(), testAssignIdRestoresAssigningIdGuardWhenSetPropertyThrows(),
testGeneratedIdPropertyChangeMarkedAsDatasourceAssignment()
Spec target section:
Datasource Autonumber / Assignment Guard Semantics

AUTONUM-FAIL-001 — Failure Visibility
Contract statement:
Failure to resolve metadata, create or find a sequence, allocate a unique value, update sequence state, update
cache-consistent object identity, or set the generated property must be visible to the caller or owning datasource
operation; it must not silently appear as successful identity assignment.
Rationale:
Generated identity failure can leave a new object without a persistent key or with an inconsistent cache key while
the save/insert path appears successful.
Source scope:
OADataSourceAuto.assignId(...), getNextNumber(...), _getNextNumber(...), insert(...), insertWithoutReferences(...),
NextNumber setters.
Related CODEX findings:
Concurrent duplicate assignment risk; local/negative identity behavior question indicates unresolved identity
boundary.
Suggested unit tests:
testAssignIdMetadataFailureIsVisible(), testAssignIdSetPropertyFailureDoesNotAppearSuccessful(),
testInsertFailureDuringAutoNumberAssignmentIsVisible()
Spec target section:
Datasource Autonumber / Failure Semantics

AUTONUM-PARTIAL-001 — Partial Assignment Boundary
Contract statement:
An object must not be treated as having a committed generated persistent identity until the sequence reservation,
cache-collision check, assigning-ID guard, and property set have all completed according to contract.
Rationale:
Partial progress around identity assignment can create objects whose lifecycle state, cache key, or sync/
serialization identity does not match their actual property state.
Source scope:
OADataSourceAuto.assignId(...), insert(...), insertWithoutReferences(...), OARuntime graph object services.
Related CODEX findings:
None directly beyond duplicate assignment risk.
Suggested unit tests:
testFailedPropertySetDoesNotCommitGeneratedIdentity(), testSequenceAdvancedButAssignmentFailedIsVisible(),
testPartialAutoNumberAssignmentDoesNotCorruptObjectCache()
Spec target section:
Datasource Autonumber / Partial Progress

AUTONUM-CONCURRENT-001 — Concurrent Allocation Safety
Contract statement:
Concurrent allocation for the same class sequence must serialize all reads, adjustments, collision retries,
increments, and assignment decisions needed to prevent duplicate IDs and sequence regression.
Rationale:
Auto-number allocation is shared identity authority; concurrent object creation must not produce duplicate keys.
Source scope:
OADataSourceAuto.getNextNumber(...), _getNextNumber(...), assignId(...), hmClassNextNumber, LOCK, synchronized(nn),
NextNumber.next.
Related CODEX findings:
startNextNumber adjustment outside synchronized(nn) can race with assignment and create duplicate assigned IDs.
Suggested unit tests:
testConcurrentAssignIdForSameClassProducesUniqueIds(), testConcurrentSequenceCreationCreatesOneNextNumber(),
testConcurrentStartingNumberAdjustmentDoesNotRegressSequence()
Spec target section:
Datasource Autonumber / Concurrency

AUTONUM-NO-STORAGE-001 — Non-Persistence Boundary
Contract statement:
OADataSourceAuto must not imply storage, query, count, update, delete, blob, or many-to-many persistence support;
unsupported operations must return documented unsupported/no-op results without masquerading as persisted state.
Rationale:
This package only assigns generated identity. Treating no-op datasource methods as persistence success would corrupt
save/delete/query semantics.
Source scope:
OADataSourceAuto.supportsStorage(), updateMany2ManyLinks(...), update(...), delete(...), execute(...),
getPropertyBlobValue(...), count(...), countPassthru(...), select(...), selectPassthru(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testAutoNumberDatasourceSupportsStorageFalse(), testUnsupportedSelectReturnsNullByContract(),
testUnsupportedUpdateDeleteDoNotClaimPersistence()
Spec target section:
Datasource Autonumber / Non-Persistence Boundary

AUTONUM-NULL-001 — Null and Unsupported Input Semantics
Contract statement:
Null objects, null classes, null property names, unsupported classes, and classes without an auto-assign ID property
must have deterministic no-assignment behavior and must not create misleading sequence or object state.
Rationale:
Auto-number assignment is optional for unsupported metadata and must not mutate state accidentally when input is
unavailable.
Source scope:
OADataSourceAuto.isClassSupported(...), assignId(...), willCreatePropertyValue(...), getNextNumber(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testAssignIdNullObjectIsNoOp(), testIsClassSupportedNullReturnsFalse(),
testWillCreatePropertyValueNullInputsReturnFalse(), testUnsupportedClassDoesNotCreateSequenceWhenSupportAllFalse()
Spec target section:
Datasource Autonumber / Null and Unsupported Semantics

AUTONUM-DISTRIBUTED-001 — Distributed Identity Boundary
Contract statement:
Auto-number generated IDs are local datasource identity assignments unless a higher-level datasource, sync,
replication, or graph authority explicitly defines distributed uniqueness, temporary identity, negative/local
identity, or reconciliation semantics.
Rationale:
Distributed OA systems need clear separation between local generated IDs and globally authoritative persistent
identity to avoid cross-site duplicate keys.
Source scope:
OADataSourceAuto, NextNumber, integration with OARuntime graph, cache, sync, replication, and datasource packages.
Related CODEX findings:
Source comment asks “what about local (negative numbers)”, indicating unresolved local/distributed identity
semantics.
Suggested unit tests:
testAutoNumberDoesNotClaimDistributedUniquenessByDefault(),
testLocalNegativeNumberPolicyRequiresExplicitOwnerDecision(),
testSyncReplicationIdentityUsesExplicitAuthorityBoundary()
Spec target section:
Datasource Autonumber / Distributed Identity Boundary

AUTONUM-METRICS-001 — Sequence Object Semantics
Contract statement:
NextNumber objects must remain local-only, non-datasource, non-initialized OAObjects whose Id, next value, and
property name represent sequence state only and not persisted domain model state.
Rationale:
NextNumber is internal sequence metadata. Persisting or syncing it as normal domain data would leak allocation state
and corrupt identity authority boundaries.
Source scope:
NextNumber @OAClass(localOnly=true, useDataSource=false, initialize=false), getId(), setId(...), getNext(),
setNext(...), getProperty(), setProperty(...).
Related CODEX findings:
None observed.
Suggested unit tests:
testNextNumberClassMetadataIsLocalOnlyAndNoDatasource(), testNextNumberPropertyChangesAreSequenceStateOnly(),
testNextNumberIdIsUniqueClassName()
Spec target section:
Datasource Autonumber / Sequence Object Semantics

AUTONUM-INTEGRATION-001 — Cross-Package Identity Compatibility
Contract statement:
Auto-number behavior must remain compatible with OAObjectInfo ID metadata, OAObject property mutation semantics,
graph/cache key authority, datasource save/insert lifecycle, transaction boundaries, serialization, sync,
replication, and generated blueprint annotations.
Rationale:
Generated identity is cross-cutting runtime state. Assignment must not bypass object metadata, cache indexes,
transaction semantics, or distributed identity assumptions.
Source scope:
OADataSourceAuto, NextNumber, OAObjectInfo, OAPropertyInfo, OARuntime graph object services,
OAObject.setProperty(...), cache/datasource/sync/replication integration.
Related CODEX findings:
Concurrent duplicate assignment risk and local/negative identity question both map to identity/cache/datasource
integration boundaries.
Suggested unit tests:
testGeneratedIdUpdatesObjectCacheKeyConsistently(),
testAutoNumberAssignmentCompatibleWithDatasourceInsertLifecycle(),
testAutoNumberIdentityCompatibleWithSerializationSyncReplicationContracts()
Spec target section:
Datasource Autonumber / Cross-Package Integration

*/


