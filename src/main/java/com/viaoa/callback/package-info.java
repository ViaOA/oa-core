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

// JUnit tests created 

/* CODEX Invariants

CALLBACK-RUNTIME-001 — Runtime Callback Contract
Contract statement:
com.viaoa.callback defines shared callback contracts and mutable callback carrier objects used by OA runtime
traversal, object visitation, filtering, lifecycle participation, serialization, copying, UI/controller policy, and
graph coordination.
Rationale:
Callbacks are runtime control points. Their invocation, return-value, state, and failure semantics must be stable
wherever OA packages use them.
Source scope:
OACallback, OAObjectCallback, OAObjectSerializerCallback, OACopyCallback, OACallbackLabel, callback consumers across
object, hub, cache, find, filter, select, serialize, trigger, sync, replication, and runtime services.
Related CODEX findings:
Existing package-info notes no concrete callback-package bugs found; callback package is mainly contract/carrier
classes rather than active dispatch logic.
Suggested unit tests:
testCallbackContractsAreUsableAcrossRuntimeConsumers(), testCallbackCarrierDefaultsAreStable(),
testCallbackFailureDoesNotAppearSuccessful()
Spec target section:
Callback Runtime / Core Responsibility

CALLBACK-INVOKE-001 — Invocation Eligibility
Contract statement:
Required callbacks must be invoked exactly according to the owning operation’s contract: once per eligible object,
event, property, stage, or traversal element, and never for ineligible state unless explicitly documented.
Rationale:
Missed callbacks skip policy, validation, serialization, copy, or traversal logic; duplicate callbacks can duplicate
side effects or corrupt traversal results.
Source scope:
OACallback.updateObject(...), OAObjectCallback, OAObjectSerializerCallback.beforeSerialize(...),
OAObjectSerializerCallback.afterSerialize(...), OACopyCallback, runtime callback consumers.
Related CODEX findings:
None observed in source; existing package-info records this as a core contract.
Suggested unit tests:
testTraversalInvokesCallbackOncePerEligibleObject(), testSerializerInvokesBeforeAndAfterOncePerObject(),
testCopyCallbackInvokedOnlyForEligibleOwnedHub()
Spec target section:
Callback Runtime / Invocation Semantics

CALLBACK-RETURN-001 — Stop and Continue Semantics
Contract statement:
OACallback.updateObject return values are runtime control flow: true means continue the owning traversal or
visitation, and false means stop according to the owning operation’s stop boundary.
Rationale:
Ignoring callback return values can over-traverse caches, continue after cancellation, report false completion, or
perform unintended runtime work.
Source scope:
OACallback.updateObject(TYPE obj), cache/object/find/filter/select/traversal consumers using OACallback.
Related CODEX findings:
None observed in source; existing package-info identifies return semantics as the main execution-sensitive behavior
in the package.
Suggested unit tests:
testCallbackTrueContinuesTraversal(), testCallbackFalseStopsTraversal(),
testCallbackStopDoesNotReportFullTraversal()
Spec target section:
Callback Runtime / Return Semantics

CALLBACK-ORDER-001 — Deterministic Ordering Where Contracted
Contract statement:
Callback ordering must be deterministic wherever the owning OA operation defines an order for traversal, lifecycle,
serialization, validation, event, or listener behavior; if no order is guaranteed, the owner must not rely on
callback order.
Rationale:
Some callbacks are observers, while others participate in decisions. Order-dependent runtime behavior must be
explicit to avoid inconsistent graph state.
Source scope:
OACallback, OAObjectCallback.Type, OAObjectSerializerCallback.beforeSerialize(...),
OAObjectSerializerCallback.afterSerialize(...), runtime callback chains.
Related CODEX findings:
None observed in source.
Suggested unit tests:
testSerializerCallbackBeforeAfterOrderIsDeterministic(), testCallbackChainOrderMatchesOwnerContract(),
testUnorderedTraversalDoesNotExposeOrderAsContract()
Spec target section:
Callback Runtime / Ordering Semantics

CALLBACK-FAIL-001 — Failure Visibility
Contract statement:
Callback failures must be caller-visible, carrier-visible, or explicitly recorded by the owning operation; a failed
participant callback must not silently appear successful unless the owner explicitly defines best-effort observer
semantics.
Rationale:
Callback failure can mean validation, traversal, serialization, policy, lifecycle participation, or cleanup did not
complete.
Source scope:
OACallback.updateObject(...), OAObjectSerializerCallback.beforeSerialize(...),
OAObjectSerializerCallback.afterSerialize(...), OAObjectCallback.setThrowable(...), OAObjectCallback.getThrowable(),
OAObjectCallback.getDisplayResponse().
Related CODEX findings:
None observed in source.
Suggested unit tests:
testTraversalCallbackExceptionIsVisible(), testSerializerCallbackExceptionDoesNotAppearSuccessful(),
testObjectCallbackThrowableIsVisibleInDisplayResponse()
Spec target section:
Callback Runtime / Failure Visibility

CALLBACK-PARTIAL-001 — Partial Callback Execution
Contract statement:
Partial callback-chain execution must not be reported as full success unless explicitly contracted; participant
callbacks may fail fast, observer callbacks must follow the owner’s isolation policy, and cleanup/finalization
callbacks must expose skipped or failed cleanup.
Rationale:
OA callbacks can be participants, observers, or finalizers. The result must distinguish complete execution from
stopped, skipped, or failed execution.
Source scope:
OAObjectCallback.Type, OACallback.updateObject(...), OAObjectSerializerCallback.beforeSerialize(...),
OAObjectSerializerCallback.afterSerialize(...), callback consumers in object, serialization, trigger, queue, and
runtime services.
Related CODEX findings:
None observed in source.
Suggested unit tests:
testParticipantCallbackCanFailFast(), testObserverCallbackFailurePolicyIsOwnerVisible(),
testPartialCallbackChainIsReportedAsIncomplete()
Spec target section:
Callback Runtime / Partial Progress Semantics

CALLBACK-STATE-001 — Invocation-Scoped Carrier State
Contract statement:
Mutable callback carrier state must be scoped to one logical invocation unless reuse and reset behavior are
explicitly documented; stale allowed, value, oldValue, response, throwable, label, serializer, format,
acknowledgement, or context state must not leak into later operations.
Rationale:
OAObjectCallback and OAObjectSerializerCallback carry runtime decisions. Reusing stale state can incorrectly allow
or block operations, serialize wrong properties, or report old failures.
Source scope:
OAObjectCallback constructors and fields, OAObjectCallback.setAllowed(...), setValue(...), setOldValue(...),
setResponse(...), setThrowable(...), ack(), setAcknownledged(...), setLabel(...),
OAObjectSerializerCallback.setOAObjectSerializer(...), OACallbackLabel.
Related CODEX findings:
Existing notes identify OAObjectCallback as mutable and intended as a per-request carrier object.
Suggested unit tests:
testObjectCallbackDefaultsDoNotCarryPreviousDecision(),
testObjectCallbackCopyConstructorCopiesOnlyIntendedContext(), testSerializerCallbackReuseRequiresExplicitReset()
Spec target section:
Callback Runtime / State Isolation

CALLBACK-OBJECT-001 — Object Callback Decision Semantics
Contract statement:
OAObjectCallback fields must preserve the semantic meaning of the callback Type: allow/verify callbacks control
permission, confirm callbacks carry confirmation state, get/render callbacks carry values or labels, and failure
callbacks expose throwable state without mutating unrelated decision fields.
Rationale:
OAObjectCallback is a shared carrier between object rules, Hub context, UI/controller behavior, and runtime policy.
Mixing field semantics can allow invalid operations or block valid ones.
Source scope:
OAObjectCallback.Type, OAObjectCallback constructors, getType(), getCheckType(), getHub(), getObject(),
getPropertyName(), getOldValue(), getValue(), getAllowed(), isAllowed(), getResponse(), getThrowable(),
getDisplayResponse(), getConfirmTitle(), getConfirmMessage(), getToolTip(), getLabel(), getFormat(), getName().
Related CODEX findings:
None observed in source.
Suggested unit tests:
testVerifyPropertyChangeUsesOldAndNewValueContext(), testAllowCallbackDefaultIsAllowedUntilBlocked(),
testConfirmCallbackDoesNotChangeAllowedUnlessExplicitlySet(), testDisplayResponseIncludesThrowableWhenPresent()
Spec target section:
Callback Runtime / Object Callback Semantics

CALLBACK-LABEL-001 — Label Carrier Semantics
Contract statement:
OACallbackLabel must act as a simple invocation-scoped display carrier for text, tooltip, style, color, background,
font, alignment, size, visibility, and enabled state without implying independent runtime enforcement.
Rationale:
Label state can affect UI/controller output, but enforcement belongs to the owning callback consumer and must not be
inferred from the carrier alone.
Source scope:
OACallbackLabel getters and setters, OAObjectCallback.getLabel(), OAObjectCallback.setLabel(...).
Related CODEX findings:
None observed in source.
Suggested unit tests:
testCallbackLabelStoresDisplayState(), testObjectCallbackLabelIsInvocationScoped(),
testLabelVisibilityDoesNotMutateAllowedDecision()
Spec target section:
Callback Runtime / Display Carrier Semantics

CALLBACK-SERIAL-001 — Serialization Callback Scope
Contract statement:
OAObjectSerializerCallback decisions must be scoped to the active serializer/object stack and must preserve include/
exclude property decisions, reference decisions, stack access, and identity requirements for each serialized object.
Rationale:
Serialization callbacks can suppress properties or replace references. Incorrect scope can drop identity, serialize
stale values, or corrupt sync/remote payload semantics.
Source scope:
OAObjectSerializerCallback.setOAObjectSerializer(...), includeProperties(...), excludeProperties(...),
includeAllProperties(), excludeAllProperties(), getStackSize(), getPreviousObject(), getStackObject(...),
getLevelsDeep(), shouldSerializeReference(...), beforeSerialize(...), afterSerialize(...),
getReferenceValueToSend(...).
Related CODEX findings:
Existing notes state serializer helper methods no-op when no serializer is assigned; normal OA usage assigns
serializer before invocation.
Suggested unit tests:
testSerializerCallbackIncludeExcludeScopedPerObject(),
testSerializerCallbackReferenceDecisionUsesDefaultWhenNotOverridden(),
testSerializerCallbackStackAccessMatchesActiveSerializationStack()
Spec target section:
Callback Runtime / Serialization Callback Semantics

CALLBACK-SERIAL-002 — Serializer Assignment Boundary
Contract statement:
OAObjectSerializerCallback helper methods that depend on an assigned OAObjectSerializer must be used only inside a
serializer-owned invocation, or their no-op/default behavior must remain explicit and non-misleading.
Rationale:
Calling serializer helper methods without an active serializer must not falsely imply properties were included,
excluded, or reference behavior was changed.
Source scope:
OAObjectSerializerCallback.setOAObjectSerializer(...), includeProperties(...), excludeProperties(...),
includeAllProperties(), excludeAllProperties(), getStackSize(), getPreviousObject(), getStackObject(...),
getLevelsDeep().
Related CODEX findings:
Existing notes: helper methods no-op when no serializer is assigned, while normal OA usage assigns the serializer
through OAObjectSerializer before callbacks.
Suggested unit tests:
testSerializerHelpersWithoutAssignedSerializerHaveExplicitNoOpBehavior(),
testAssignedSerializerReceivesIncludeExcludeDecisions(),
testSerializerStackHelpersReturnDefinedValuesWithoutSerializer()
Spec target section:
Callback Runtime / Serialization Assignment Boundary

CALLBACK-COPY-001 — Copy Callback Ownership Semantics
Contract statement:
OACopyCallback must preserve OA copy ownership semantics by default, and may substitute, suppress, or override
owned-Hub copying, object copy creation, or property values only through explicit callback return behavior.
Rationale:
Copy callbacks can alter duplicated object graphs. They must not accidentally share owned state, skip required owned
Hubs, or change identity semantics silently.
Source scope:
OACopyCallback.shouldCopyOwnedHub(...), OACopyCallback.createCopy(...), OACopyCallback.getPropertyValue(...), object
copy consumers.
Related CODEX findings:
None observed in source.
Suggested unit tests:
testCopyCallbackDefaultPreservesNormalDeepCopy(), testCopyCallbackCanSuppressOwnedHubExplicitly(),
testCopyCallbackSubstitutedObjectMaintainsLinkSemantics(), testCopyCallbackPropertyOverrideIsExplicit()
Spec target section:
Callback Runtime / Copy Callback Semantics

CALLBACK-REENTRANT-001 — Reentrant Callback Isolation
Contract statement:
Recursive or reentrant callback execution must not corrupt outer traversal decisions, callback carrier state,
serializer include/exclude stacks, copy decisions, or lifecycle state.
Rationale:
Callbacks can access objects, trigger serialization, traverse Hubs, invoke filters, or cause additional callbacks.
Nested execution must preserve each invocation boundary.
Source scope:
OACallback.updateObject(...), OAObjectCallback, OAObjectSerializerCallback stack helper methods, OACopyCallback,
runtime callback consumers.
Related CODEX findings:
None observed in source.
Suggested unit tests:
testReentrantTraversalCallbackDoesNotCorruptOuterStopState(),
testNestedSerializerCallbackRestoresIncludeExcludeState(), testCallbackTriggeredCallbackUsesSeparateCarrierState()
Spec target section:
Callback Runtime / Reentrancy Semantics

CALLBACK-TL-001 — Runtime Context Restoration
Contract statement:
Callback execution that changes ThreadLocal, OAThreadLocal, transaction, sync, replication, loading, serialization,
or runtime context state must restore the previous value with try/finally before returning to the owner.
Rationale:
Callbacks often run inside sensitive runtime flows. Context leakage can corrupt sync, load, transaction, trigger,
event, serialization, or replay behavior.
Source scope:
Callback package contracts; callback consumers in object, hub, cache, trigger, queue, serialization, sync,
replication, transaction, and runtime services.
Related CODEX findings:
None observed in callback source; existing package-info records this as a cross-package callback contract.
Suggested unit tests:
testCallbackThreadLocalStateRestoredAfterSuccess(), testCallbackThreadLocalStateRestoredAfterException(),
testCallbackDoesNotLeakSendSyncMessagesState()
Spec target section:
Callback Runtime / ThreadLocal Context

CALLBACK-CONCURRENT-001 — Mutable Callback Thread-Safety Boundary
Contract statement:
Mutable callback instances are per-invocation unless explicitly documented as thread-safe; concurrent callback
execution must not share mutable OAObjectCallback, OAObjectSerializerCallback, OACallbackLabel, or copy callback
state in a way that changes results nondeterministically.
Rationale:
Callbacks can be used by background queues, cache traversal, serialization, sync, replication, and UI/controller
code under concurrent load.
Source scope:
OAObjectCallback, OAObjectSerializerCallback, OACallbackLabel, OACopyCallback, OACallback, callback registration/
execution consumers.
Related CODEX findings:
Existing notes identify OAObjectCallback as mutable and not thread-safe by design as a per-request carrier.
Suggested unit tests:
testMutableObjectCallbackIsNotSharedAcrossConcurrentInvocations(),
testConcurrentSerializerCallbacksUseIndependentState(),
testCallbackRegistrationSnapshotSafeDuringConcurrentExecution()
Spec target section:
Callback Runtime / Concurrency Semantics

CALLBACK-NULL-001 — Null and Missing Callback Behavior
Contract statement:
Null or absent callbacks must have owner-defined behavior: no-op, default allow/continue, default deny/stop, or
visible failure; callback absence must not silently change semantic operation success outside the owning contract.
Rationale:
OA runtime flows often make callbacks optional. Optionality must not create hidden validation, traversal,
serialization, or policy gaps.
Source scope:
OACallback consumers, OAObjectCallback default state, OAObjectSerializerCallback default implementations,
OACopyCallback default implementations.
Related CODEX findings:
None observed in source.
Suggested unit tests:
testMissingTraversalCallbackUsesOwnerDefault(), testDefaultObjectCallbackAllowsUntilExplicitlyBlocked(),
testDefaultCopyCallbackDoesNotOverrideCopyBehavior()
Spec target section:
Callback Runtime / Optional Callback Semantics

CALLBACK-INTEGRATION-001 — Cross-Package Callback Compatibility
Contract statement:
Callback behavior must remain compatible with object, Hub, cache, find/filter/select, load, datasource, trigger,
queue, serialization, sync, replication, transaction, and graph/runtime contracts; callback success is not
automatically semantic operation success unless the owner defines it.
Rationale:
Callbacks are low-level extension points. Broken callback contracts can propagate into traversal, lifecycle,
validation, serialization, sync, replication, and runtime context correctness.
Source scope:
com.viaoa.callback.*, consumers across com.viaoa.object, com.viaoa.hub, com.viaoa.cache, com.viaoa.find,
com.viaoa.filter, com.viaoa.select, com.viaoa.serialize, com.viaoa.trigger, com.viaoa.sync, com.viaoa.replication,
com.viaoa.runtime.
Related CODEX findings:
Existing package-info notes no direct bugs but emphasizes cross-package callback role.
Suggested unit tests:
testCallbackStopSemanticsPropagateThroughCacheService(), testObjectCallbackFailureBlocksInvalidSave(),
testCallbackExceptionDoesNotLeaveRuntimeContextDirty(), testCallbackResultDistinguishedFromOperationCommit()
Spec target section:
Callback Runtime / Cross-Package Integration

*/