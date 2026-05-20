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

SERIAL-RUNTIME-001 — Graph-Aware Serialization Boundary
Contract statement:
OA serialization must preserve OA object-graph semantics across serialization boundaries, including OAObject
identity, Hub membership, object references, loaded/unloaded state, metadata-defined links, and configured traversal
boundaries.
Rationale:
Serialization is used as a runtime boundary for cache, remote, sync, replication, storage, and tooling. Treating OA
objects as plain Java fields can corrupt graph topology, identity, and lazy-load semantics.
Source scope:
OAObjectSerializer; OASerializer.writeObject; OASerializer.writeHub; OADeserializer.readObject;
OADeserializer.readHub.
Related CODEX findings:
Existing package-info invariant notes; no source CODEX comments observed.
Suggested unit tests:
testSerializeDeserializePreservesGraphSemantics; testSerializedObjectCanReenterGraphWithoutIdentityDrift.
Spec target section:
Serialization / Runtime Boundary Semantics

SERIAL-LIFECYCLE-001 — Complete Write Lifecycle
Contract statement:
A serialization write must not be considered successful until the configured root object, optional extra object,
overflow/deferred references, framing metadata, and final object-count metadata have been written according to the
selected compressed or uncompressed format.
Rationale:
Partial serialized payloads can later deserialize into corrupt or misleading graph state.
Source scope:
OAObjectSerializer.writeObject; OAObjectSerializer._writeObject; OAObjectSerializer.finishWrite.
Related CODEX findings:
Existing package-info false-success and partial-write notes.
Suggested unit tests:
testWriteSuccessRequiresFinalMetadata; testPartialWriteFailureDoesNotAppearSuccessful.
Spec target section:
Serialization / Write Lifecycle

SERIAL-LIFECYCLE-002 — Complete Read Lifecycle
Contract statement:
A deserialization read must not be considered successful until the root object, optional extra object, overflow/
deferred objects, and final object-count metadata have been read and reconciled, or failure has been reported.
Rationale:
Returning a partially restored object graph creates silent corruption at cache, sync, replication, and remote
boundaries.
Source scope:
OAObjectSerializer.readObject; OAObjectSerializer._readObject; OAObjectSerializer.finishRead;
OAObjectSerializer.getObject; OAObjectSerializer.getExtraObject.
Related CODEX findings:
Existing package-info partial-read and false-success notes.
Suggested unit tests:
testReadSuccessRequiresOverflowAndCount; testCorruptPayloadReadFailsVisibly.
Spec target section:
Serialization / Read Lifecycle

SERIAL-FAIL-001 — Failure Visibility
Contract statement:
Serialization and deserialization failures must be caller-visible or otherwise observable, and must not be converted
into successful null, empty, truncated, or partially valid graph results.
Rationale:
Silent false-success hides data loss and causes downstream runtime systems to trust invalid state.
Source scope:
OAObjectSerializer.writeObject; OAObjectSerializer._writeObject; OAObjectSerializer._readObject;
OAObjectSerializer.getObject; OASerializeReader; OASerializeWriter.
Related CODEX findings:
Existing package-info failure visibility notes.
Suggested unit tests:
testSerializationExceptionPropagates; testDeserializationExceptionPropagates;
testUnreadableClassDoesNotReturnValidObject.
Spec target section:
Serialization / Failure Semantics

SERIAL-IDENTITY-001 — Logical Identity Preservation
Contract statement:
Multiple serialized references to the same logical OAObject within one payload must deserialize to the same logical
identity according to OA cache/key/GUID semantics.
Rationale:
Duplicate live objects for the same logical entity corrupt Hubs, links, cache indexes, sync, and replication state.
Source scope:
OAObjectSerializer; OASerializeContext.hasWritten; OASerializeContext.markWritten; OAObjectKey interactions.
Related CODEX findings:
Existing package-info identity/reference notes.
Suggested unit tests:
testRepeatedObjectReferenceDeserializesToSameLogicalIdentity; testTwoSerializedHubsShareSameMemberIdentity.
Spec target section:
Serialization / Identity Preservation

SERIAL-IDENTITY-002 — Key And GUID Boundary Preservation
Contract statement:
When serialization mode requires identity reconstruction, serialized payloads must preserve the configured OAObject
key and GUID information, or fail visibly when that identity information cannot be represented.
Rationale:
Remote references, cache reconciliation, datasource reloads, sync, and replication depend on stable identity keys.
Source scope:
OAObjectSerializer; OASerializeContext.getWriteKeys; OASerializeContext.setWriteKeys;
OASerializeContext.getWriteGuid; OASerializeContext.setWriteGuid.
Related CODEX findings:
Existing package-info GUID/key notes.
Suggested unit tests:
testWriteKeysControlsKeySerialization; testWriteGuidControlsGuidSerialization;
testMissingRequiredIdentityFailsVisibly.
Spec target section:
Serialization / Key And GUID Semantics

SERIAL-REFERENCE-001 — Reference Versus Full Object Semantics
Contract statement:
Reference-only serialization must emit enough identity state to resolve the reference without emitting mutable full-
object state, while full-object serialization must emit the configured scalar and relationship state for that
object.
Rationale:
Confusing reference payloads with full-object payloads can overwrite newer cache state or expand graphs
unexpectedly.
Source scope:
OAObjectSerializer.shouldSerializeReference; OAObjectSerializer.getReferenceValueToSend;
OAObjectSerializer.includeProperties; OAObjectSerializer.excludeProperties; OAObjectSerializer.includeAllProperties;
OAObjectSerializer.excludeAllProperties.
Related CODEX findings:
Existing package-info reference/full-object notes.
Suggested unit tests:
testReferenceOnlySerializationIncludesIdentityNotMutableProperties;
testFullSerializationIncludesConfiguredProperties.
Spec target section:
Serialization / Reference Boundaries

SERIAL-OPTIONS-001 — Deterministic Serialization Options
Contract statement:
Serializer options for compression, reference inclusion/exclusion, property inclusion/exclusion, blob inclusion,
maximum objects, maximum size, and extra object payloads must have deterministic effects for the same object graph
and runtime state.
Rationale:
OA runtime systems must be able to choose payload shape predictably for cache, remote, sync, replication, and
tooling use.
Source scope:
OAObjectSerializer constructors; setIncludeBlobs; setExcludedReferences; excludedClasses; setMax; getMax;
setMaxSize; getMaxSize; setExtraObject; getExtraObject.
Related CODEX findings:
Existing package-info serializer option notes.
Suggested unit tests:
testIncludeExcludePropertiesAreDeterministic; testExcludedReferenceClassSuppressesReference;
testExtraObjectRoundTrips.
Spec target section:
Serialization / Options

SERIAL-CONTEXT-001 — Operation-Scoped Context
Contract statement:
Serialization context state, including written-object tracking, include flags, reference flags, key/GUID flags, and
depth state, must be scoped to the active serialization operation and must not leak into independent operations.
Rationale:
Context leakage changes payload shape, reference decisions, and identity handling in later serializations.
Source scope:
OASerializeContext; OAObjectSerializer.writeObject; OAObjectSerializer.beforeSerialize;
OAObjectSerializer.afterSerialize.
Related CODEX findings:
Existing package-info context leakage notes.
Suggested unit tests:
testSerializeContextDoesNotLeakAcrossOperations; testDepthStateRestoredAfterOperation.
Spec target section:
Serialization / Context Scope

SERIAL-CONTEXT-002 — Visited Object Tracking
Contract statement:
Within one serialization operation, already-written object tracking must use object identity semantics and must
prevent duplicate full serialization without suppressing legitimate distinct objects.
Rationale:
OA graphs can contain repeated references and cycles; serialization must preserve topology without infinite
expansion.
Source scope:
OASerializeContext.hasWritten; OASerializeContext.markWritten; OAObjectSerializer.shouldSerializeReference;
OAObjectSerializer.finishWrite.
Related CODEX findings:
Existing package-info circular/duplicate notes.
Suggested unit tests:
testVisitedTrackingUsesObjectIdentity; testDistinctObjectsWithEqualValuesAreNotSuppressed.
Spec target section:
Serialization / Visited Tracking

SERIAL-DEPTH-001 — Depth And Overflow Boundaries
Contract statement:
Depth limits must bound recursive graph serialization deterministically, and deferred overflow objects must be
serialized and restored through explicit overflow records rather than silently dropped.
Rationale:
Deep OA graphs must avoid stack overflow while preserving reachable configured references.
Source scope:
OAObjectSerializer.shouldSerializeReference; OAObjectSerializer.finishWrite; OAObjectSerializer.finishRead;
OASerializeContext.getMaxDepth; OASerializeContext.pushDepth; OASerializeContext.popDepth;
OASerializeContext.isMaxDepthReached.
Related CODEX findings:
Existing package-info circular graph and overflow notes.
Suggested unit tests:
testDeepGraphUsesOverflowInsteadOfDroppingReference; testOverflowReferenceRestoredAfterDeserialize.
Spec target section:
Serialization / Depth And Overflow

SERIAL-HUB-001 — Hub Membership And Order Preservation
Contract statement:
Serialized Hubs must preserve member identity and externally meaningful membership order according to Hub semantics.
Rationale:
Hub order and membership affect UI state, detail relationships, sync payloads, replication, and runtime traversal
behavior.
Source scope:
OASerializer.writeHub; OADeserializer.readHub; OAObjectSerializer root object handling for Hub; Hub serialization
hooks used by OAObjectSerializer.
Related CODEX findings:
Existing package-info Hub serialization notes.
Suggested unit tests:
testSerializedHubPreservesMemberOrder; testSerializedHubMembersResolveByIdentity.
Spec target section:
Serialization / Hub Semantics

SERIAL-HUB-002 — Hub Load-State Preservation
Contract statement:
Serialized Hub state must preserve the distinction between loaded-empty, loaded-populated, and not-yet-loaded/
unresolved states where the owning Hub/link contract exposes that distinction.
Rationale:
False loaded-empty state prevents later lazy loading and can hide real datasource contents.
Source scope:
OAObjectSerializer; OASerializer.writeHub; OADeserializer.readHub; Hub serialization integration.
Related CODEX findings:
Existing package-info Hub loaded/unloaded notes.
Suggested unit tests:
testUnloadedHubDoesNotDeserializeAsLoadedEmpty; testLoadedEmptyHubRoundTripsAsLoadedEmpty.
Spec target section:
Serialization / Hub Load State

SERIAL-NULL-001 — Null Versus Unresolved Reference Semantics
Contract statement:
Serialization must preserve the semantic difference between an authoritative null reference and an unresolved,
unloaded, or key-only reference.
Rationale:
Null means no target exists; unresolved means a target may still be resolved through cache, datasource, sync, or
replication.
Source scope:
OAObjectSerializer; OASerializeContext.getIncludeNulls; OASerializeContext.setIncludeNulls; OAObjectKey reference
handling.
Related CODEX findings:
Existing package-info unresolved reference notes.
Suggested unit tests:
testNullReferenceRoundTripsAsNull; testUnresolvedReferenceRoundTripsAsResolvableReference.
Spec target section:
Serialization / Null And Unresolved State

SERIAL-METADATA-001 — Metadata-Driven Structure
Contract statement:
Serialized OA object structure must follow OA metadata semantics for properties, links, calculated values, transient
values, blobs, and references rather than raw field layout.
Rationale:
Generated blueprint metadata defines what the runtime graph means; raw Java field serialization can include wrong
state or omit required semantic state.
Source scope:
OASerializeContext include flags; OAObjectSerializer; OAObjectInfo/OALinkInfo integration.
Related CODEX findings:
Existing package-info metadata and property inclusion notes.
Suggested unit tests:
testSerializationUsesMetadataProperties; testTransientAndCalculatedInclusionFlagsAreHonored.
Spec target section:
Serialization / Metadata Semantics

SERIAL-BLOB-001 — Blob Inclusion Contract
Contract statement:
Blob and large-value properties must be serialized only when the active serialization contract includes them, and
excluding them must not convert loadable existing blob state into authoritative null.
Rationale:
Blob behavior affects payload size, lazy loading, storage correctness, and remote/sync transfer cost.
Source scope:
OAObjectSerializer.getIncludeBlobs; OAObjectSerializer.setIncludeBlobs; metadata property serialization hooks.
Related CODEX findings:
Existing package-info blob notes.
Suggested unit tests:
testIncludeBlobsTrueSerializesBlob; testIncludeBlobsFalseDoesNotAuthoritativelyNullBlob.
Spec target section:
Serialization / Blob Semantics

SERIAL-CALLBACK-001 — Callback Decision Semantics
Contract statement:
Serializer callbacks may influence reference values, before/after object serialization behavior, and reference
inclusion, but their decisions must be applied deterministically and must not leave include/exclude or stack state
corrupted after the object finishes serializing.
Rationale:
Callbacks are the supported customization boundary for payload shape; callback side effects must remain scoped.
Source scope:
OAObjectSerializer.setCallback; getCallback; getReferenceValueToSend; beforeSerialize; afterSerialize;
shouldSerializeReference; OAObjectSerializerCallback.
Related CODEX findings:
Existing package-info callback/context notes.
Suggested unit tests:
testCallbackReferenceDecisionIsHonored; testCallbackIncludeExcludeStateRestoredAfterObject;
testCallbackExceptionDoesNotLeaveSerializerStackCorrupt.
Spec target section:
Serialization / Callback Semantics

SERIAL-TL-001 — ThreadLocal Serializer Registration Cleanup
Contract statement:
Any serializer registered in OA thread-local runtime state during write processing must be removed in finally-style
cleanup regardless of success or failure.
Rationale:
Leaked serializer context can alter nested or later serialization behavior on pooled/runtime threads.
Source scope:
OAObjectSerializer.writeObject; OARuntime.thread; OAThreadLocalService.addObjectSerializer;
OAThreadLocalService.removeObjectSerializer.
Related CODEX findings:
Existing package-info context cleanup notes.
Suggested unit tests:
testThreadLocalSerializerRemovedAfterSuccessfulWrite; testThreadLocalSerializerRemovedAfterWriteFailure.
Spec target section:
Serialization / ThreadLocal Cleanup

SERIAL-COMPRESS-001 — Compression Frame Integrity
Contract statement:
Compressed serialization must write and read an explicit compression flag, use matching deflate/inflate framing,
finish compressed output before reporting success, and preserve the same object semantics as uncompressed
serialization.
Rationale:
Compression is a transport detail; it must not change graph meaning or create truncated payloads.
Source scope:
OAObjectSerializer._writeObject; OAObjectSerializer._readObject; getCompressedWritten.
Related CODEX findings:
Existing package-info stream/framing notes.
Suggested unit tests:
testCompressedAndUncompressedRoundTripSameGraphSemantics; testCompressedPayloadFailsVisiblyWhenTruncated.
Spec target section:
Serialization / Compression Framing

SERIAL-STREAM-001 — Stream Ownership And Flush Boundaries
Contract statement:
Serialization must flush, finish, close, or leave open underlying streams according to the stream ownership
contract, and must not close caller-owned streams unless ownership has been explicitly transferred by the chosen
serialization path.
Rationale:
OA serialization is often embedded in remote, sync, replication, and file streams where closing the wrong layer can
corrupt the surrounding protocol.
Source scope:
OAObjectSerializer._writeObject; RemoteObjectOutputStream integration; RemoteObjectInputStream integration.
Related CODEX findings:
Existing package-info stream cleanup notes.
Suggested unit tests:
testCompressedWriteFinishesFrameBeforeReturn; testCallerOwnedRemoteStreamNotClosedUnexpectedly.
Spec target section:
Serialization / Stream Ownership

SERIAL-LIMIT-001 — Size And Object Count Limits
Contract statement:
Configured maximum object and size limits must be enforced deterministically and must result in an explicitly
bounded payload, deferred reference behavior, or visible failure according to contract; they must not silently
produce a payload that appears complete when required objects were omitted.
Rationale:
Limits protect runtime transport and memory boundaries, but false-complete payloads corrupt graph meaning.
Source scope:
OAObjectSerializer.setMax; getMax; setMaxSize; getMaxSize; hasReachedMax; getTotalObjectsWritten;
shouldSerializeReference.
Related CODEX findings:
Existing package-info max/partial payload notes.
Suggested unit tests:
testMaxObjectsStopsReferenceExpansionByContract; testReachedMaxIsObservable;
testMaxSizeDoesNotReportCompleteGraphWhenOmitted.
Spec target section:
Serialization / Payload Limits

SERIAL-ORDER-001 — Deterministic Payload Ordering
Contract statement:
For the same graph state, metadata, serializer options, and callback decisions, serialization must emit object,
extra-object, overflow, and final-count data in a deterministic order required by the matching reader.
Rationale:
Reader/writer disagreement or nondeterministic order breaks replay, remote transport, diagnostics, and future
regression testing.
Source scope:
OAObjectSerializer._writeObject; OAObjectSerializer._readObject; finishWrite; finishRead; OASerializeReader;
OASerializeWriter.
Related CODEX findings:
Existing package-info ordering and lifecycle notes.
Suggested unit tests:
testPayloadFieldOrderMatchesReaderContract; testSameGraphSerializesDeterministically.
Spec target section:
Serialization / Deterministic Ordering

SERIAL-RETRY-001 — Retry After Failure Uses Clean State
Contract statement:
After serialization or deserialization failure, retry must use clean operation state and must not reuse contaminated
stack, overflow, depth, callback, stream, inflater, deflater, or context state as if the prior attempt succeeded.
Rationale:
Partially written/read state can corrupt retries and produce misleading success.
Source scope:
OAObjectSerializer; OASerializeContext; callback stack handling; overflow list handling; deflater/inflater handling.
Related CODEX findings:
Existing package-info retry/context cleanup notes.
Suggested unit tests:
testRetryAfterWriteFailureUsesFreshSerializerContext; testRetryAfterReadFailureDoesNotReusePartialOverflowState.
Spec target section:
Serialization / Retry Semantics

SERIAL-REUSE-001 — Serializer Instance Reuse Boundaries
Contract statement:
Serializer and context instances with mutable operation state must either be used for one operation at a time or be
reset/owned explicitly before reuse; shared reuse must not corrupt object counts, stacks, overflow state, or option
state.
Rationale:
OA serialization contains mutable counters, stacks, and callback state that are not safe to treat as stateless
utilities.
Source scope:
OAObjectSerializer mutable fields; OASerializeContext mutable flags/depth/writtenObjects.
Related CODEX findings:
Existing package-info context/state notes.
Suggested unit tests:
testSerializerReuseRequiresExplicitCleanState; testConcurrentUseOfSameContextIsNotAssumedSafe.
Spec target section:
Serialization / Instance Reuse

SERIAL-CONCURRENT-001 — Concurrency Boundaries
Contract statement:
Concurrent serialization operations must not share mutable serializer/context state unless explicitly synchronized
or scoped, while shared immutable metadata may be reused safely.
Rationale:
Serialization can run from remote, sync, queue, and runtime worker threads; shared mutable state can corrupt
payloads across operations.
Source scope:
OAObjectSerializer; OASerializeContext; OASerializer; OADeserializer.
Related CODEX findings:
Existing package-info concurrency/context notes.
Suggested unit tests:
testIndependentSerializersCanRunConcurrently; testSharedContextConcurrentUseRequiresOwnerDecision.
Spec target section:
Serialization / Concurrency

SERIAL-COMPAT-001 — Version And Model Evolution Boundary
Contract statement:
When serialized graph data crosses OA version or generated-model evolution boundaries, unknown, missing, renamed, or
incompatible fields/classes must follow an explicit compatibility policy: preserve, ignore, migrate, resolve through
classloading, or fail visibly.
Rationale:
Serialized payloads can outlive a process and can be used by cache, remote, replication, and tooling flows across
model changes.
Source scope:
OAObjectSerializer; OADeserializer; OASerializeReader; classloading/object reconstruction integration.
Related CODEX findings:
Existing package-info compatibility notes.
Suggested unit tests:
testUnknownSerializedPropertyCompatibilityByContract; testMissingSerializedPropertyCompatibilityByContract;
testMissingClassFailsVisiblyOrUsesDummyByContract.
Spec target section:
Serialization / Compatibility

SERIAL-INTEGRATION-001 — Cross-Package Boundary Compatibility
Contract statement:
Serialization behavior must remain compatible with OAObject, Hub, cache, graph/runtime, metadata, datasource,
remote, sync, replication, classloader, I/O, and logging contracts, and must not silently bypass those packages’
identity, lifecycle, load-state, or failure rules.
Rationale:
Serialization is a boundary package; incorrect assumptions at this layer propagate into object graph corruption,
remote mismatch, replay errors, and stale cache state.
Source scope:
OAObjectSerializer; OASerializer; OADeserializer; OASerializeContext; OASerializeReader; OASerializeWriter.
Related CODEX findings:
Existing package-info cross-runtime notes.
Suggested unit tests:
testDeserializeReconcilesWithCacheContract; testSerializedPayloadCompatibleWithRemoteObjectStreams;
testSerializationPreservesDatasourceResolvableReferences.
Spec target section:
Serialization / Cross-Package Contracts

*/


