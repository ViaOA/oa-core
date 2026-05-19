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
package com.viaoa.serialize;


/* CODEX Invariants

1. Serialization Runtime Contracts

  SER-RUNTIME-001 — Serialization Is Graph-Aware
  Contract statement: OA serialization must preserve Object Graph semantics, not merely Java object fields.
  Rationale: OAObjects, Hubs, references, keys, GUIDs, and loaded/unloaded state have runtime meaning beyond plain
  Java serialization.
  Source locations: OAObjectSerializer, serializer context classes, graph/object serialization hooks.
  Known related CODEX findings: serialization identity/reference issues were reviewed.
  Suggested unit tests: testSerializerPreservesGraphObjectSemantics(),
  testSerializedObjectCanReenterGraphCorrectly()
  Spec target section: Serialization Runtime / Graph-Aware Serialization

  SER-RUNTIME-002 — Serializer Options Must Have Deterministic Meaning
  Contract statement: Shallow/deep, include blobs, include references, and reference-only options must consistently
  affect the serialized payload according to contract.
  Rationale: Different OA paths use serialization for sync, replication, object-cache storage, and generated
  tooling.
  Source locations: OAObjectSerializer option fields/methods, serialization wrappers.
  Known related CODEX findings: none observed beyond scan findings.
  Suggested unit tests: testShallowSerializationExcludesDeepReferences(),
  testDeepSerializationIncludesConfiguredReferences()
  Spec target section: Serialization Runtime / Serializer Options

  2. Object Identity Preservation Contracts

  SER-IDENTITY-001 — Deserialization Must Preserve OA Identity
  Contract statement: Deserializing an OAObject must resolve through OA identity/cache semantics so an existing
  graph object is reused when appropriate.
  Rationale: Duplicate runtime objects for the same key/GUID corrupt Hubs, references, sync, and replication.
  Source locations: OAObjectSerializer, OAObject read/resolve hooks, object cache service.
  Known related CODEX findings: duplicate/reference identity issues reviewed.
  Suggested unit tests: testDeserializeExistingObjectReusesCachedIdentity(),
  testDeserializeDoesNotCreateDuplicateForSameKey()
  Spec target section: Serialization Runtime / Identity Preservation

  SER-IDENTITY-002 — Serialized Object References Must Resolve To Same Logical Identity
  Contract statement: Multiple references to the same object in one serialized graph must deserialize to the same
  logical object identity.
  Rationale: Object graph topology must not be flattened into duplicate instances.
  Source locations: OAObjectSerializer, object stream resolve hooks.
  Known related CODEX findings: duplicate object/reference risks reviewed.
  Suggested unit tests: testRepeatedObjectReferenceDeserializesToSameInstance(),
  testTwoHubsSharingObjectDeserializeSharedIdentity()
  Spec target section: Serialization Runtime / Shared Identity

  3. GUID / ObjectKey Contracts

  SER-GUID-001 — GUID Must Be Preserved When Required For Runtime Identity
  Contract statement: Serialization paths that need runtime identity must preserve GUIDs or provide an explicit
  identity remapping policy.
  Rationale: Sync/replication and object-cache workflows may need runtime identity continuity.
  Source locations: OAObjectSerializer, OAObject guid hooks, object cache service.
  Known related CODEX findings: GUID/key preservation risks reviewed.
  Suggested unit tests: testSerializationPreservesGuidWhenConfigured(),
  testDeserializationUsesGuidPolicyByContract()
  Spec target section: Serialization Runtime / GUID Preservation

  SER-KEY-001 — ObjectKey Must Be Preserved For Persistent Identity
  Contract statement: Serialized OAObjects and references must preserve object keys sufficient to reload or resolve
  persistent identity.
  Rationale: Remote references, unresolved references, and datasource reloads depend on keys.
  Source locations: OAObjectSerializer, OAObjectKey, reference serialization hooks.
  Known related CODEX findings: object-key/reference issues reviewed.
  Suggested unit tests: testSerializedObjectPreservesObjectKey(),
  testSerializedReferencePreservesObjectKeyOnlyWhenReferenceMode()
  Spec target section: Serialization Runtime / ObjectKey Preservation

  4. Reference vs Full Object Contracts

  SER-REFERENCE-001 — Reference-Only Serialization Must Not Serialize Full Object State
  Contract statement: When an object is serialized as a reference, payload must include identity information but not
  full mutable object state unless explicitly configured.
  Rationale: Prevents unintended graph expansion and stale state overwrites.
  Source locations: OAObjectSerializer, reference selection logic.
  Known related CODEX findings: reference/full-object distinction reviewed.
  Suggested unit tests: testReferenceOnlySerializationIncludesKeyNotProperties(),
  testReferenceOnlySerializationDoesNotOverwriteCachedState()
  Spec target section: Serialization Runtime / Reference Serialization

  SER-FULL-001 — Full Object Serialization Must Include Required Loaded State
  Contract statement: When an object is serialized fully, required scalar properties and configured references must
  be written consistently.
  Rationale: Full serialization is used for object-cache storage, sync transfer, and replication snapshots.
  Source locations: OAObjectSerializer, object property serialization hooks.
  Known related CODEX findings: none observed.
  Suggested unit tests: testFullSerializationIncludesScalarProperties(),
  testFullSerializationIncludesConfiguredLoadedReferences()
  Spec target section: Serialization Runtime / Full Object Serialization

  SER-SHALLOW-001 — Shallow Serialization Must Preserve Object Boundary
  Contract statement: Shallow serialization must serialize the object’s own state without accidentally traversing
  unconfigured deep references.
  Rationale: Prevents payload explosion and circular graph traversal when not intended.
  Source locations: OAObjectSerializer shallow/deep options.
  Known related CODEX findings: shallow/deep behavior reviewed.
  Suggested unit tests: testShallowSerializationDoesNotSerializeChildHubContents(),
  testShallowSerializationKeepsReferenceAsReference()
  Spec target section: Serialization Runtime / Shallow Serialization

  5. Hub Serialization Contracts

  SER-HUB-001 — Hub Serialization Preserves Membership Identity And Order
  Contract statement: Serialized Hubs must preserve member identity and order according to Hub contract.
  Rationale: Detail/view state, UI, replication, and object-cache storage require deterministic Hub reconstruction.
  Source locations: Hub serialization hooks, OAObjectSerializer, Hub-related serializers.
  Known related CODEX findings: Hub serialization risks reviewed.
  Suggested unit tests: testSerializedHubPreservesMemberOrder(), testSerializedHubMembersResolveByIdentity()
  Spec target section: Serialization Runtime / Hub Serialization

  SER-HUB-002 — Hub Serialization Must Preserve Loaded/Unloaded Semantics
  Contract statement: A serialized Hub must not falsely become loaded, empty, or fully populated unless that was the
  serialized semantic state.
  Rationale: False loaded-empty state prevents lazy-load retry and hides data.
  Source locations: Hub serialization hooks, OAObjectSerializer.
  Known related CODEX findings: lazy-load/reference state issues reviewed.
  Suggested unit tests: testUnloadedHubDeserializesAsUnloaded(),
  testLoadedEmptyHubDeserializesAsLoadedEmptyByContract()
  Spec target section: Serialization Runtime / Hub Load State

  6. Unresolved Reference Contracts

  SER-UNRESOLVED-001 — Unresolved References Remain Unresolved, Not Null
  Contract statement: A serialized unresolved object reference must deserialize as an unresolved reference/key
  state, not as a resolved null.
  Rationale: Later datasource or sync resolution must remain possible.
  Source locations: reference serialization hooks, OAObjectSerializer, object property services.
  Known related CODEX findings: unresolved reference bugs reviewed in graph/serialize scans.
  Suggested unit tests: testUnresolvedReferenceDeserializesAsUnresolvedKey(),
  testUnresolvedReferenceCanBeResolvedAfterDeserialize()
  Spec target section: Serialization Runtime / Unresolved References

  SER-UNRESOLVED-002 — Missing Target Must Be Distinguishable From Not-Yet-Loaded
  Contract statement: Serialization/deserialization must preserve the distinction between authoritative missing/null
  and not-yet-loaded reference.
  Rationale: Retry/lazy-load correctness depends on this distinction.
  Source locations: OAObjectSerializer, reference state serialization.
  Known related CODEX findings: lazy-load null/unresolved risks reviewed.
  Suggested unit tests: testNullReferenceDeserializesAsNull(), testNotLoadedReferenceDoesNotDeserializeAsNull()
  Spec target section: Serialization Runtime / Reference Load State

  7. Serialization Context Contracts

  SER-CONTEXT-001 — Serialization Context Is Operation-Scoped
  Contract statement: Serializer/deserializer context must be scoped to the current serialization operation and must
  not leak across independent operations.
  Rationale: Context leakage can cause wrong reference/full-object decisions or identity reuse.
  Source locations: serializer context classes, OAObjectSerializer.
  Known related CODEX findings: context leakage issues reviewed.
  Suggested unit tests: testSerializationContextDoesNotLeakAcrossOperations(),
  testDeserializerContextIsClearedAfterRead()
  Spec target section: Serialization Runtime / Context Scope

  SER-CONTEXT-002 — Context Must Track Already-Visited Objects
  Contract statement: Serialization context must track visited objects to preserve identity and prevent circular
  traversal loops.
  Rationale: OA object graphs are cyclic by design.
  Source locations: OAObjectSerializer, context tracking helpers.
  Known related CODEX findings: circular/duplicate handling reviewed.
  Suggested unit tests: testSerializationTracksVisitedObjects(), testCircularReferenceSerializationTerminates()
  Spec target section: Serialization Runtime / Context Identity Tracking

  8. Read / Write Stream Contracts

  SER-STREAM-001 — Write Success Means Complete Serialized Payload Was Written
  Contract statement: A successful serialization write must mean the complete payload for the configured object/
  graph was emitted.
  Rationale: Partial serialized data is not safely recoverable as a valid object graph.
  Source locations: OAObjectSerializer, object output stream integration.
  Known related CODEX findings: false-success/data-loss risks reviewed.
  Suggested unit tests: testWriteFailurePropagates(), testPartialWriteDoesNotReportSuccess()
  Spec target section: Serialization Runtime / Write Contract

  SER-STREAM-002 — Read Success Means Complete Object Semantics Were Restored
  Contract statement: Successful deserialization must restore the configured object/reference/Hub semantics
  completely or fail visibly.
  Rationale: Silent partial read creates corrupt graph state.
  Source locations: OAObjectSerializer, object input stream integration.
  Known related CODEX findings: read/failure behavior reviewed.
  Suggested unit tests: testReadFailurePropagates(), testPartialReadDoesNotReturnPartiallyValidObject()
  Spec target section: Serialization Runtime / Read Contract

  9. Blob / Large Value Contracts

  SER-BLOB-001 — Blob Inclusion Flag Controls Blob Serialization
  Contract statement: Blob/large property values must be serialized only when the include-blobs option requires it,
  and excluded otherwise.
  Rationale: Blob inclusion affects payload size, storage behavior, and lazy blob loading.
  Source locations: OAObjectSerializer.setIncludeBlobs(...), property serialization hooks.
  Known related CODEX findings: none observed.
  Suggested unit tests: testIncludeBlobsTrueSerializesBlobProperty(), testIncludeBlobsFalseExcludesBlobProperty()
  Spec target section: Serialization Runtime / Blob Serialization

  SER-BLOB-002 — Excluded Blob Must Remain Loadable Or Known-Absent
  Contract statement: If a blob is excluded from serialization, deserialized state must preserve whether it can be
  loaded later or is truly absent.
  Rationale: Excluding blobs must not accidentally convert existing blob data into null.
  Source locations: blob property serialization hooks, datasource blob access paths.
  Known related CODEX findings: blob/load state risks reviewed indirectly.
  Suggested unit tests: testExcludedBlobDoesNotBecomeNullWhenLoadable(), testNullBlobRemainsNullAfterSerialization()
  Spec target section: Serialization Runtime / Blob Load State

  10. Circular Graph / Duplicate Object Contracts

  SER-CIRCULAR-001 — Circular Object Graphs Must Serialize Without Infinite Recursion
  Contract statement: Serializing cyclic OAObject/Hub/reference graphs must terminate using reference tracking.
  Rationale: OA relationships are commonly bidirectional/cyclic.
  Source locations: OAObjectSerializer, context visited-object tracking.
  Known related CODEX findings: circular graph handling reviewed.
  Suggested unit tests: testParentChildReverseLinkSerializationTerminates(),
  testSelfReferenceSerializationTerminates()
  Spec target section: Serialization Runtime / Circular Graph Handling

  SER-DUPLICATE-001 — Duplicate Object Encounters Must Preserve Single Logical Identity
  Contract statement: If the same OAObject is encountered multiple times while serializing one graph, it must
  deserialize as one logical identity.
  Rationale: Shared references must remain shared.
  Source locations: OAObjectSerializer, context identity tracking, object cache integration.
  Known related CODEX findings: duplicate/reference risks reviewed.
  Suggested unit tests: testSharedChildReferenceDeserializesAsSameObject(),
  testDuplicateEncounterUsesReferenceAfterFirstFullObject()
  Spec target section: Serialization Runtime / Duplicate Object Handling

  11. Failure / Retry / Data Loss Contracts

  SER-FAILURE-001 — Serialization Failure Must Be Visible
  Contract statement: Serialization/deserialization failures must not be silently converted into successful but
  incomplete payloads or objects.
  Rationale: Silent data loss corrupts graph state and persistence/replication payloads.
  Source locations: OAObjectSerializer, stream integration.
  Known related CODEX findings: false-success/silent data-loss risks reviewed.
  Suggested unit tests: testSerializationExceptionPropagates(), testDeserializationExceptionPropagates()
  Spec target section: Serialization Runtime / Failure Semantics

  SER-FAILURE-002 — Retry After Serialization Failure Must Use Clean Context
  Contract statement: After serialization/deserialization failure, a retry must not reuse contaminated operation
  context.
  Rationale: Partially visited object maps or reference state can corrupt retry output.
  Source locations: serializer/deserializer context handling.
  Known related CODEX findings: context cleanup risks reviewed.
  Suggested unit tests: testRetryAfterSerializationFailureUsesFreshContext(),
  testRetryAfterDeserializeFailureUsesFreshContext()
  Spec target section: Serialization Runtime / Retry Semantics

  SER-COMPAT-001 — Compatibility Behavior Must Be Explicit
  Contract statement: When serialized data is read across OA versions or model changes, compatibility behavior must
  be defined: migrate, ignore, preserve, or fail visibly.
  Rationale: OA object-cache storage, replication, and generated apps may persist serialized data across versions.
  Source locations: OAObjectSerializer, read/write version handling if present, object property read hooks.
  Known related CODEX findings: compatibility concerns reviewed.
  Suggested unit tests: testUnknownPropertyCompatibilityByContract(), testMissingPropertyCompatibilityByContract()
  Spec target section: Serialization Runtime / Compatibility

  12. Test Coverage Matrix

  Runtime/options:

  - testSerializerPreservesGraphObjectSemantics
  - testSerializedObjectCanReenterGraphCorrectly
  - testShallowSerializationExcludesDeepReferences
  - testDeepSerializationIncludesConfiguredReferences

  Identity/GUID/key:

  - testDeserializeExistingObjectReusesCachedIdentity
  - testDeserializeDoesNotCreateDuplicateForSameKey
  - testRepeatedObjectReferenceDeserializesToSameInstance
  - testSerializationPreservesGuidWhenConfigured
  - testSerializedObjectPreservesObjectKey

  Reference/full/shallow:

  - testReferenceOnlySerializationIncludesKeyNotProperties
  - testReferenceOnlySerializationDoesNotOverwriteCachedState
  - testFullSerializationIncludesScalarProperties
  - testFullSerializationIncludesConfiguredLoadedReferences
  - testShallowSerializationDoesNotSerializeChildHubContents

  Hub/unresolved:

  - testSerializedHubPreservesMemberOrder
  - testSerializedHubMembersResolveByIdentity
  - testUnloadedHubDeserializesAsUnloaded
  - testUnresolvedReferenceDeserializesAsUnresolvedKey
  - testUnresolvedReferenceCanBeResolvedAfterDeserialize
  - testNotLoadedReferenceDoesNotDeserializeAsNull

  Context/circular/duplicate:

  - testSerializationContextDoesNotLeakAcrossOperations
  - testDeserializerContextIsClearedAfterRead
  - testSerializationTracksVisitedObjects
  - testCircularReferenceSerializationTerminates
  - testParentChildReverseLinkSerializationTerminates
  - testSharedChildReferenceDeserializesAsSameObject

  Stream/blob/failure:

  - testWriteFailurePropagates
  - testPartialWriteDoesNotReportSuccess
  - testReadFailurePropagates
  - testPartialReadDoesNotReturnPartiallyValidObject
  - testIncludeBlobsTrueSerializesBlobProperty
  - testIncludeBlobsFalseExcludesBlobProperty
  - testSerializationExceptionPropagates
  - testRetryAfterSerializationFailureUsesFreshContext
  - testUnknownPropertyCompatibilityByContract


*/


