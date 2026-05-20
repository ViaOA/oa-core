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
 * <p>
 */
package com.viaoa.callback;

/* CODEX Invariants


com.viaoa.callback Invariants

  ID: CALLBACK-INVOKE-001
  Contract statement: Required callbacks must be invoked exactly according to the owning operation’s contract: once
  per eligible object/event/stage, and never for ineligible state unless explicitly documented.
  Rationale: OA callbacks participate in traversal, validation, serialization, copy behavior, and runtime policy
  checks. Missed or duplicate callback execution can skip validation, duplicate side effects, or corrupt traversal
  results.
  Source locations: OACallback.updateObject; OAObjectCallback;
  OAObjectSerializerCallback.beforeSerialize/afterSerialize; OACopyCallback; consumers in cache/object/serialize/trig
  ger services.
  Related CODEX findings: none.
  Suggested unit tests: testCacheVisitInvokesCallbackOncePerObject, testSerializerInvokesBeforeAndAfterOncePerObject,
  testCopyCallbackInvokedForEligibleOwnedHubOnly.
  Spec target section: Callback Runtime / Invocation Semantics.

  ID: CALLBACK-RETURN-001
  Contract statement: Callback return values must be honored consistently. For OACallback.updateObject, true means
  continue traversal and false means stop traversal.
  Rationale: Stop/continue semantics are control-flow, not advisory. Ignoring them can over-traverse caches, apply
  work after cancellation, or miss intended early termination.
  Source locations: OACallback.updateObject; cache traversal services using OACallback; trigger/cache/filter/find
  traversal consumers.
  Related CODEX findings: none.
  Suggested unit tests: testCallbackFalseStopsCacheTraversal, testCallbackTrueContinuesTraversal,
  testCallbackStopDoesNotReportFullTraversal.
  Spec target section: Callback Runtime / Return Semantics.

  ID: CALLBACK-ORDER-001
  Contract statement: Callback ordering must be deterministic wherever OA traversal, lifecycle processing,
  serialization, validation, or listener behavior depends on order. If no order is guaranteed, callers must treat
  ordering as undefined.
  Rationale: Some callbacks are observers, while others participate in lifecycle decisions. Deterministic ordering
  prevents inconsistent validation, serialization, and event behavior.
  Source locations: OACallback; OAObjectSerializerCallback.beforeSerialize/afterSerialize; OAObjectCallback.Type; con
  sumers in OAObjectCallbackService, OAObjectSerializer, cache services.
  Related CODEX findings: none.
  Suggested unit tests: testSerializerCallbackBeforeAfterOrderIsNestedCorrectly, testCallbackChainOrderIsDocumented,
  testTraversalOrderContractIsExplicit.
  Spec target section: Callback Runtime / Ordering Semantics.

  ID: CALLBACK-FAIL-001
  Contract statement: Callback failures must be caller-visible or explicitly recorded. A failed callback must not
  silently appear successful unless the owning operation explicitly defines observer-only best-effort behavior.
  Rationale: Callback failures can mean validation, traversal, serialization selection, or policy enforcement did not
  complete. Silent success creates hidden runtime divergence.
  Source locations: OACallback.updateObject; OAObjectSerializerCallback.beforeSerialize/afterSerialize; OAObjectCallb
  ack.throwable; OAObjectCallback.getDisplayResponse.
  Related CODEX findings: none.
  Suggested unit tests: testCallbackExceptionPropagatesFromTraversal,
  testSerializerCallbackExceptionDoesNotAppearSuccessful, testObjectCallbackThrowableIsVisibleToCaller.
  Spec target section: Callback Runtime / Failure Visibility.

  ID: CALLBACK-CHAIN-001
  Contract statement: Partial callback-chain execution must not be reported as full success unless explicitly
  contracted. BEFORE/participant callbacks may fail-fast; observer/finalization callbacks should continue or aggregate
  failures according to the owning subsystem policy.
  Rationale: OA uses callbacks in mixed roles: participants, observers, and cleanup hooks. The result must distinguish
  complete execution from stopped or failed execution.
  Source locations: OAObjectCallback.Type; OAObjectSerializerCallback.beforeSerialize/afterSerialize; callback consum
  ers in object, serialization, trigger, queue, transaction-like services.
  Related CODEX findings: none.
  Suggested unit tests: testParticipantCallbackCanFailFast,
  testObserverCallbackFailureDoesNotSkipRemainingObserversWhenContracted,
  testPartialCallbackChainIsReportedAsIncomplete.
  Spec target section: Callback Runtime / Chain Execution Semantics.

  ID: CALLBACK-STATE-001
  Contract statement: Callback carrier state must be scoped to a single logical invocation unless reuse is explicitly
  documented and reset. Stale allowed, value, response, throwable, label, serializer, or acknowledgement state must
  not leak into later operations.
  Rationale: OAObjectCallback and serializer callbacks carry mutable decision state. Reusing stale state can
  incorrectly allow/block operations or serialize the wrong properties.
  Source locations: OAObjectCallback fields and constructors; OAObjectSerializerCallback.setOAObjectSerializer;
  OACallbackLabel; OACopyCallback.
  Related CODEX findings: none.
  Suggested unit tests: testObjectCallbackDefaultsDoNotCarryPreviousDecision,
  testCopiedObjectCallbackCopiesOnlyIntendedContext, testSerializerCallbackReuseRequiresExplicitReset.
  Spec target section: Callback Runtime / State Isolation.

  ID: CALLBACK-REENTRANT-001
  Contract statement: Recursive or reentrant callback execution must not corrupt traversal state, stop/continue
  decisions, serializer include/exclude stacks, or lifecycle state.
  Rationale: Callbacks can trigger object access, serialization, Hub traversal, or additional callbacks. Reentrancy
  must preserve nested state boundaries.
  Source locations: OACallback.updateObject; OAObjectSerializerCallback; OAObjectSerializer callback stack
  integration; OAObjectCallbackService callback creation.
  Related CODEX findings: none.
  Suggested unit tests: testNestedSerializerCallbackRestoresIncludeExcludeState,
  testReentrantTraversalCallbackDoesNotCorruptOuterStopState, testCallbackTriggeredCallbackUsesSeparateCarrierState.
  Spec target section: Callback Runtime / Reentrancy Semantics.

  ID: CALLBACK-TL-001
  Contract statement: Callback code that changes ThreadLocal, OAThreadLocal, transaction, sync, replay, loading, or
  runtime context state must restore it with try/finally before returning to the owner.
  Rationale: Callbacks often run inside sensitive runtime flows. Context leakage across callback boundaries can
  corrupt sync, load, transaction, trigger, or event behavior.
  Source locations: callback package contracts; callback consumers in runtime/thread services, object services, queue/
  trigger/sync/serialization paths.
  Related CODEX findings: none.
  Suggested unit tests: testCallbackThreadLocalStateRestoredAfterSuccess,
  testCallbackThreadLocalStateRestoredAfterException, testCallbackDoesNotLeakSendSyncMessagesState.
  Spec target section: Callback Runtime / ThreadLocal Context Semantics.

  ID: CALLBACK-CONCURRENT-001
  Contract statement: Concurrent callback execution must not corrupt shared callback state or callback registration
  state. Mutable callback instances are per-invocation unless documented as thread-safe.
  Rationale: OA callbacks can be used by background queues, cache traversal, serialization, sync, and UI/controller
  code. Shared mutable callback state can cause nondeterministic results under load.
  Source locations: OAObjectCallback; OAObjectSerializerCallback; OACallbackLabel; callback registration/execution
  consumers.
  Related CODEX findings: none.
  Suggested unit tests: testMutableObjectCallbackIsNotSharedAcrossConcurrentInvocations,
  testConcurrentSerializerCallbacksUseIndependentState, testCallbackRegistrationSnapshotSafeDuringConcurrentExecution.
  Spec target section: Callback Runtime / Concurrency Semantics.

  ID: CALLBACK-SERIAL-001
  Contract statement: Serialization callbacks must preserve serializer stack, include/exclude property decisions,
  reference decisions, and identity requirements for each serialized object.
  Rationale: Serialization callbacks can suppress properties and references. Incorrect callback state can drop
  identity, send stale references, or corrupt remote/sync payload semantics.
  Source locations: OAObjectSerializerCallback.beforeSerialize, afterSerialize, shouldSerializeReference,
  getReferenceValueToSend, helper methods includeProperties, excludeProperties, includeAllProperties,
  excludeAllProperties.
  Related CODEX findings: none.
  Suggested unit tests: testSerializerCallbackIncludeExcludeScopedPerObject,
  testSerializerCallbackDoesNotSuppressIdentityValues,
  testSerializerCallbackReferenceValuePreservesObjectIdentityContract.
  Spec target section: Callback Runtime / Serialization Callback Semantics.

  ID: CALLBACK-OBJECT-001
  Contract statement: OAObjectCallback decisions must preserve the semantic meaning of the callback type: allow/verify
  callbacks control permission, confirm callbacks supply user confirmation data, and get/render callbacks supply UI or
  copy values without changing unrelated decision fields.
  Rationale: OAObjectCallback is the shared carrier between model rules, Hub context, and controllers. Mixing semantic
  fields can allow invalid operations or block valid ones.
  Source locations: OAObjectCallback.Type; OAObjectCallback.allowed, value, oldValue, response, throwable,
  confirmTitle, confirmMessage, toolTip, format, label.
  Related CODEX findings: none.
  Suggested unit tests: testVerifyPropertyChangeUsesOldAndNewValueContext,
  testAllowCallbackDefaultIsAllowedUntilBlocked, testConfirmCallbackDoesNotChangeAllowedUnlessExplicitlySet.
  Spec target section: Callback Runtime / Object Callback Semantics.

  ID: CALLBACK-COPY-001
  Contract statement: Copy callbacks must preserve deep-copy ownership semantics unless they explicitly substitute,
  suppress, or override copied values according to the callback contract.
  Rationale: Copy callbacks can alter object graph duplication. They must not accidentally share owned state, skip
  required owned Hubs, or change identity semantics silently.
  Source locations: OACopyCallback.shouldCopyOwnedHub, createCopy, getPropertyValue; copy consumers in object
  reflection/copy services.
  Related CODEX findings: none.
  Suggested unit tests: testCopyCallbackDefaultPreservesNormalDeepCopy, testCopyCallbackCanSuppressOwnedHubExplicitly,
  testCopyCallbackSubstitutedObjectMaintainsLinkSemantics.
  Spec target section: Callback Runtime / Copy Callback Semantics.

  ID: CALLBACK-INTEGRATION-001
  Contract statement: Callback behavior must remain compatible with object, Hub, cache, find/filter/select, load,
  datasource, trigger, queue, serialization, sync/replication, and runtime contracts.
  Rationale: Callback code is a low-level extension point. A broken callback contract can propagate into traversal,
  lifecycle, validation, serialization, or sync correctness.
  Source locations: package com.viaoa.callback; consumers across com.viaoa.cache, object, hub, find, filter, select,
  load, datasource, trigger, queue, serialize, sync, runtime.
  Related CODEX findings: none.
  Suggested unit tests: testCallbackStopSemanticsPropagateThroughCacheService,
  testObjectCallbackFailureBlocksInvalidSave, testCallbackExceptionDoesNotLeaveRuntimeContextDirty.
  Spec target section: Callback Runtime / Cross-Package Integration.

  Suggested Package-Level Spec Summary

  - com.viaoa.callback defines shared callback contracts and mutable carrier objects used by OA runtime traversal,
    validation, serialization, copying, policy checks, and UI/controller integration.
  - The package is responsible for clear callback invocation, return-value, state-carrier, and failure semantics.
  - OACallback.updateObject provides the core traversal callback contract: return true to continue, false to stop.
  - OAObjectCallback carries object/Hub/property context, permission decisions, responses, throwable failure state,
    confirmation data, formatting data, and UI label data.
  - OAObjectSerializerCallback controls serialization inclusion/exclusion and reference behavior for each object in a
    serialization session.
  - OACopyCallback customizes deep-copy behavior while preserving OA ownership/link semantics unless explicitly
    overridden.
  - Callback failures must not silently appear successful when the callback participates in control flow, validation,
    serialization, or lifecycle decisions.
  - Callback state must be scoped per logical invocation or explicitly reset before reuse.
  - Callback code that changes ThreadLocal/OAThreadLocal/runtime context must restore it with try/finally.
  - Mutable callback instances should be treated as per-invocation objects unless explicitly documented as thread-
    safe.
  - Callback behavior must preserve cross-package contracts for object lifecycle, Hub membership, cache traversal,
    datasource/load/save/delete, triggers, queues, serialization, sync, replication, and runtime context.

  Likely unit-test categories:

  - traversal stop/continue behavior
  - callback exception propagation
  - callback chain partial-failure behavior
  - OAObjectCallback default and copied-state behavior
  - serializer callback include/exclude stack scoping
  - copy callback default and override behavior
  - reentrant callback behavior
  - ThreadLocal cleanup around callback execution
  - concurrent callback reuse/registration behavior
  - cross-package callback integration tests




qqqqqqqqqqqqqqqqqq other

Findings

  No concrete callback-package runtime correctness bugs found in this pass.

  I reviewed:

  - OACallback
  - OACopyCallback
  - OAObjectCallback
  - OAObjectSerializerCallback
  - OACallbackLabel
  - existing package-info.java / CODEX section

  The package is mostly contract/carrier classes rather than active dispatch logic. The main execution-sensitive
  behavior is the OACallback.updateObject boolean contract and OAObjectSerializerCallback’s serializer-bound helper
  methods. I did not find a concrete normal-OA path inside this package where callback return values are ignored,
  callback failure is swallowed, callback chains falsely report success, or ThreadLocal/context state is changed
  without cleanup.

  Notes, not reported as bugs:

  - OAObjectSerializerCallback helper methods no-op when no serializer is assigned, but normal OA usage assigns the
    serializer through OAObjectSerializer.setCallback(...) before invoking serialization callbacks.
  - OAObjectCallback is mutable and not thread-safe, but it appears intended as a per-request carrier object, not
    shared state.
  - package-info.java currently contains a placeholder/stale “Undo Invariants” text, but that is documentation
    coverage, not a runtime correctness bug.


*/


