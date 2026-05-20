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
package com.viaoa.trigger;

/* CODEX Invariants

TRIGGER-RUNTIME-001 — Metadata-Driven Reactive Contract
Contract statement:
OA triggers must bind a root OAObject class, one or more metadata-valid dependent property paths, execution flags,
and one listener into a deterministic runtime reaction contract.
Rationale:
Triggers are model-level object graph reactions, not casual callbacks; they drive calculated state, derived
projections, Hub updates, cache/filter refresh, and business/runtime behavior.
Source scope:
OATrigger, OATriggerListener, OATriggerMethodListener; OAObjectInfo trigger registration/dispatch; OATriggerService
integration.
Related CODEX findings:
Existing package-info notes identify triggers as deeper than Hub listeners and dependent on OA metadata/path
resolution.
Suggested unit tests:
testTriggerDefinitionBindsRootClassPathsFlagsAndListener, testTriggerUsesMetadataValidPathContract.
Spec target section:
Trigger Runtime / Trigger Contract

TRIGGER-REG-001 — All-Or-Nothing Registration
Contract statement:
A trigger registration must become visible only after all declared property paths, intermediate registrations,
calculated-property dependencies, and execution metadata are valid and committed.
Rationale:
Partial registration can fire callbacks after caller-visible registration failure or leave stale path registrations.
Source scope:
OATrigger; OATrigger.getPropertyPaths(); OAObjectInfo.createTrigger/_addTrigger; OATriggerService.addTrigger.
Related CODEX findings:
Current registration mutates while validating paths; package-info notes partial-commit registration risk.
Suggested unit tests:
testTriggerRegistrationIsAllOrNothingForMultiplePaths, testInvalidSecondPathLeavesNoPartialRegistration.
Spec target section:
Trigger Runtime / Registration Lifecycle

TRIGGER-REG-002 — Immutable Registered Definition
Contract statement:
Once a trigger is registered, the routing definition used for registration and removal must be immutable for root
class, property paths, dependent triggers, listener, and execution flags.
Rationale:
Removal and dispatch must use the same semantic definition that was registered; mutable arrays can leave stale
registrations or remove the wrong paths.
Source scope:
OATrigger constructors; getPropertyPaths(); getDependentTriggers(); geDependentTriggers();
setDependentTriggers(...); OAObjectInfo.removeTrigger.
Related CODEX findings:
OATrigger stores and returns externally mutable propertyPaths and dependent trigger arrays.
Suggested unit tests:
testPropertyPathArrayMutationAfterRegistrationDoesNotAffectRouting,
testDependentTriggerArrayMutationDoesNotLeaveStaleRegistration.
Spec target section:
Trigger Runtime / Trigger Definition Immutability

TRIGGER-REG-003 — Duplicate Registration Semantics
Contract statement:
Registering the same logical trigger twice must either be idempotent by an explicit trigger identity key or create
independent registrations by explicit contract; distinct flags/listeners/paths must not be silently collapsed.
Rationale:
Silent dedupe causes missed trigger execution, while unintended duplicate registration causes duplicate business-
rule execution.
Source scope:
OATrigger; OAObjectInfo._addTrigger; OATriggerService.addTrigger.
Related CODEX findings:
Existing package-info notes dedupe ignores trigger instance and execution flags.
Suggested unit tests:
testDuplicateSameTriggerRegistrationIsIdempotentByContract, testDistinctTriggerFlagsDoNotCollapseRegistration.
Spec target section:
Trigger Runtime / Registration Identity

TRIGGER-REMOVE-001 — Complete Deregistration
Contract statement:
Removing a trigger must prevent future eligible executions for that trigger and must remove all direct,
intermediate, reverse-path, and dependent calculated-property registrations owned by that registration.
Rationale:
Stale trigger registrations leak listeners and can fire invalid or closed runtime behavior.
Source scope:
OATriggerService.removeTrigger; OAObjectInfo.removeTrigger/_removeTrigger; OATrigger.getPropertyPaths(); OATrigger.
getDependentTriggers().
Related CODEX findings:
Existing package-info notes removal return value does not prove removal and mutable paths can weaken unregister.
Suggested unit tests:
testRemoveTriggerRemovesIntermediateAndDependentRegistrations, testRemovedTriggerDoesNotFireOnFutureEvent,
testRemoveUnregisteredTriggerReportsNoRemovalByContract.
Spec target section:
Trigger Runtime / Deregistration

TRIGGER-PATH-001 — Metadata-Valid Path Binding
Contract statement:
Each trigger property path must resolve from the trigger root class using OA path and metadata semantics, including
object properties, links, Hub/detail relationships, and calculated-property dependencies.
Rationale:
Wrong path expansion causes missed triggers, wrong-object triggers, or expensive fallback scans.
Source scope:
OATrigger.propertyPaths; OAObjectInfo.createTrigger/_addTrigger; OAPath; OAObjectInfo/OALinkInfo integration.
Related CODEX findings:
Existing package-info notes empty path and invalid path semantics need explicit contract.
Suggested unit tests:
testNestedPropertyPathRegistersIntermediateSegments, testInvalidTriggerPathFailsBeforeRegistration,
testEmptyTriggerPathBehaviorByContract.
Spec target section:
Trigger Runtime / Path Binding

TRIGGER-PATH-002 — Exact Property Matching
Contract statement:
Trigger dispatch must match changed properties semantically and exactly under OA metadata property-name rules,
including documented case-insensitivity where OA property names are case-insensitive.
Rationale:
Over-broad matching creates false trigger execution; over-narrow matching misses required runtime reactions.
Source scope:
OAObjectInfo trigger maps keyed by property; OAObjectInfo.onChange; OAObjectEventService property-change
integration.
Related CODEX findings:
Existing package-info notes uppercase property keying and locale/case behavior concerns.
Suggested unit tests:
testTriggerPropertyMatchingIsCaseInsensitiveByMetadataContract, testSimilarPropertyNamesDoNotCrossFire.
Spec target section:
Trigger Runtime / Property Matching

TRIGGER-PATH-003 — Reverse Root Resolution
Contract statement:
When a changed object or Hub event occurs below a trigger root, reverse-path resolution must identify each affected
root object exactly once, or explicitly invoke the null-root fallback contract when direct root resolution is
unavailable.
Rationale:
Duplicate root callbacks corrupt derived state; missed root callbacks leave stale state.
Source scope:
OAObjectInfo._runOnChange2; OAFinder reverse traversal; OATriggerListener.onTrigger;
OATriggerMethodListener.onTrigger.
Related CODEX findings:
Existing package-info notes no-root paths, fallback paths, and duplicate-root suppression risks.
Suggested unit tests:
testReversePathFindsAffectedRootOnce, testMultipleReversePathsDeduplicateSameRoot,
testNoReversePathUsesNullRootFallback.
Spec target section:
Trigger Runtime / Root Resolution

TRIGGER-FIRE-001 — Eligible Events Fire Required Triggers
Contract statement:
Every committed eligible OAObject property event or detail-Hub event must fire all matching triggers according to
registration semantics, execution flags, and runtime context.
Rationale:
Triggers maintain calculated values, object graph projections, cache filters, Hub refreshes, and business callbacks.
Source scope:
OAObjectEventService.firePropertyChange; HubEventService trigger integration; OAObjectInfo.onChange;
OATriggerListener.onTrigger.
Related CODEX findings:
Existing package-info notes no-root chained trigger paths can miss downstream reverse triggers.
Suggested unit tests:
testPropertyChangeFiresRegisteredTrigger, testDetailHubAddFiresRootPathTrigger,
testChainedNoRootTriggerDoesNotMissDownstreamDependency.
Spec target section:
Trigger Runtime / Event Firing

TRIGGER-FIRE-002 — No Duplicate Execution Per Semantic Change
Contract statement:
For one committed semantic object/Hub change, each registered trigger must execute no more than the number of times
defined by its root/path/listener contract.
Rationale:
Duplicate trigger execution can duplicate calculated updates, business actions, sync side effects, or cache
refreshes.
Source scope:
OAObjectInfo.onChange/_runOnChange2; OATriggerService.runTrigger; OATriggerMethodListener.onTrigger.
Related CODEX findings:
Existing package-info notes duplicate registration and reverse traversal duplication risks.
Suggested unit tests:
testSinglePropertyChangeInvokesTriggerOnce, testRootFoundThroughMultiplePathsInvokesOnce,
testConcurrentRegistrationDoesNotDuplicateExecution.
Spec target section:
Trigger Runtime / Duplicate Prevention

TRIGGER-LISTENER-001 — Listener Invocation Contract
Contract statement:
OATriggerListener.onTrigger must receive the resolved root object when available, the original HubEvent context, and
the property path from root to event source; when root is unavailable, objRoot == null is a first-class fallback
signal.
Rationale:
Listeners need a stable contract to react directly or resolve affected roots through cache, datasource, or loaded
graph traversal.
Source scope:
OATriggerListener.onTrigger; OAObjectInfo._runOnChange2; OATriggerMethodListener.onTrigger.
Related CODEX findings:
Existing package-info notes objRoot == null fallback semantics and listener fallback dependence.
Suggested unit tests:
testResolvedRootPassedToListener, testNullRootFallbackPassesHubEventAndPath, testListenerReceivesOriginalHubEvent.
Spec target section:
Trigger Runtime / Listener Callback

TRIGGER-METHOD-001 — Annotated Method Trigger Semantics
Contract statement:
Reflection-backed trigger methods must invoke the configured method on every affected root object according to
loaded-data and datasource fallback rules, and reflective invocation failure must be observable.
Rationale:
Annotated trigger methods are executable model semantics; missed or hidden method failures leave derived state
stale.
Source scope:
OATriggerMethodListener.constructor; OATriggerMethodListener.onTrigger; method.invoke; OAFinder; OASelect; object
cache visit.
Related CODEX findings:
OATriggerMethodListener cache fallback swallows callback exceptions; datasource select path can leak resources;
query parameter mismatch noted for nested Hub membership.
Suggested unit tests:
testTriggerMethodInvokedForResolvedRoot, testTriggerMethodCacheVisitFailureIsObservable,
testTriggerMethodSelectClosedOnFailure.
Spec target section:
Trigger Runtime / Method Triggers

TRIGGER-FLAGS-001 — Execution Flags Have Deterministic Meaning
Contract statement:
serverSideOnly, onlyUseLoadedData, useBackgroundThread, and useBackgroundThreadIfNeeded must affect trigger
execution deterministically and must not silently change path resolution, datasource loading, or execution
visibility.
Rationale:
Trigger flags bound cost, placement, and side effects across server/client, loaded-data, and background execution
boundaries.
Source scope:
OATrigger.getServerSideOnly(); getOnlyUseLoadedData(); getUseBackgroundThread(); getUseBackgroundThreadIfNeeded();
OATriggerMethodListener.onTrigger; OATriggerService.runTrigger.
Related CODEX findings:
Existing package-info notes server/client behavior, loaded-data scan behavior, and background executor ordering/cost
risks.
Suggested unit tests:
testServerSideOnlySkippedOnClientByContract, testOnlyUseLoadedDataDoesNotForceDatasourceLoad,
testBackgroundFlagSchedulesByContract.
Spec target section:
Trigger Runtime / Execution Flags

TRIGGER-ORDER-001 — Inline Ordering
Contract statement:
Inline trigger execution for a stable registration set must be deterministic and must follow the documented
registration/list order.
Rationale:
Derived state and event side effects can depend on repeatable trigger ordering.
Source scope:
OAObjectInfo.onChange; trigger listener lists; OATriggerListener.onTrigger.
Related CODEX findings:
Existing package-info notes CopyOnWriteArrayList iteration order and concurrent registration implications.
Suggested unit tests:
testInlineTriggersFireInRegistrationOrder, testConcurrentRegistrationAffectsOnlyFutureEventsByContract.
Spec target section:
Trigger Runtime / Inline Ordering

TRIGGER-ORDER-002 — Background Ordering Contract
Contract statement:
Background trigger execution must have an explicit ordering contract: either preserve event order for the affected
trigger/root/path scope or declare unordered asynchronous execution with observable failure reporting.
Rationale:
A shared executor can reorder reactive updates; consumers need to know whether triggers are sequential or eventually
processed.
Source scope:
OATrigger.getUseBackgroundThread(); getUseBackgroundThreadIfNeeded(); OATriggerService.runTrigger; background
executor integration.
Related CODEX findings:
Existing package-info notes multi-thread background executor ordering risk and async failure visibility risk.
Suggested unit tests:
testBackgroundTriggerOrderingContract, testBackgroundTriggerExceptionObservable,
testBackgroundTriggerDoesNotSilentlyDropWork.
Spec target section:
Trigger Runtime / Background Ordering

TRIGGER-ERROR-001 — Trigger Failure Visibility
Contract statement:
Trigger listener, reflection, traversal, cache-visit, select, and background execution failures must be observable
through exception propagation, diagnostics, or a documented async failure channel; failed triggers must not silently
appear successful.
Rationale:
Failed triggers can leave calculated properties, projections, filters, and business state stale.
Source scope:
OATriggerListener.onTrigger; OATriggerMethodListener.onTrigger; OAObjectInfo._runOnChange2;
OATriggerService.runTrigger.
Related CODEX findings:
OATriggerMethodListener swallows cache-visit exceptions; async submission can hide failures; broad catch/fallback
can mask listener failures.
Suggested unit tests:
testInlineTriggerListenerExceptionPropagatesOrIsObservable, testCacheFallbackTriggerExceptionVisible,
testAsyncTriggerExceptionObservable.
Spec target section:
Trigger Runtime / Failure Visibility

TRIGGER-ERROR-002 — Traversal Failure Is Distinct From Listener Failure
Contract statement:
Path traversal/root-resolution failures must be distinguished from listener execution failures and must not cause
duplicate listener side effects, hidden fallback retries, or loss of the original failure.
Rationale:
Fallback resolution is useful only when root traversal fails; it must not mask callback failure or re-run partially
executed listener logic.
Source scope:
OAObjectInfo._runOnChange2; OAFinder traversal; OATriggerMethodListener.onTrigger.
Related CODEX findings:
Existing package-info notes broad catch around finder.find can conflate traversal failure with listener failure.
Suggested unit tests:
testTraversalFailureUsesFallbackWithoutCallingListenerTwice,
testListenerFailureDoesNotTriggerTraversalFallbackAsSuccess.
Spec target section:
Trigger Runtime / Failure Classification

TRIGGER-RESOURCE-001 — Trigger Resource Cleanup
Contract statement:
Resources opened during trigger resolution or execution, including datasource/select resources and runtime context
helpers, must be closed or restored on success and failure.
Rationale:
Trigger execution can occur frequently; resource leaks in reactive paths become long-running production leaks.
Source scope:
OATriggerMethodListener.onTrigger; OASelect usage; OAObjectInfo trigger dispatch; OATriggerService background
runnables.
Related CODEX findings:
OATriggerMethodListener select path does not close OASelect on success/failure.
Suggested unit tests:
testTriggerMethodSelectClosedAfterSuccess, testTriggerMethodSelectClosedAfterException,
testTriggerRuntimeContextRestoredAfterFailure.
Spec target section:
Trigger Runtime / Resource Cleanup

TRIGGER-TL-001 — Runtime Context Restoration
Contract statement:
Any ThreadLocal or runtime context changed during trigger dispatch, callback execution, loaded-data traversal, sync
suppression, or background execution must be restored with finally-style cleanup.
Rationale:
Leaked runtime context can corrupt later object events, sync/replication behavior, load-state decisions, and trigger
recursion handling.
Source scope:
OAObjectInfo trigger dispatch; OATriggerService background trigger execution; OAThreadLocalService recursive trigger
and context state.
Related CODEX findings:
Existing package-info notes context restoration to null versus previous context should be verified.
Suggested unit tests:
testTriggerThreadLocalContextRestoredAfterInlineExecution, testTriggerThreadLocalContextRestoredAfterAsyncExecution,
testTriggerSendSyncMessagesStateRestored.
Spec target section:
Trigger Runtime / ThreadLocal Cleanup

TRIGGER-RECURSE-001 — Recursive Trigger Boundaries
Contract statement:
Recursive and reentrant trigger chains must be bounded and must fail visibly or suppress only the invalid recursive
edge when the bound is exceeded.
Rationale:
Self-triggering calculated properties and object graph updates can create infinite trigger loops or event storms.
Source scope:
OAObjectInfo.onChange; OAThreadLocalService.getRecursiveTriggerCount; OATriggerService.runTrigger.
Related CODEX findings:
Existing package-info notes async trigger execution can escape scheduling-thread recursion counters.
Suggested unit tests:
testInlineRecursiveTriggerStopsAtLimit, testAsyncRecursiveTriggerDoesNotBypassLimit,
testRecursiveTriggerFailureObservable.
Spec target section:
Trigger Runtime / Recursion Control

TRIGGER-RECURSE-002 — Legitimate Propagation Not Suppressed
Contract statement:
Recursion/reentrancy protection must suppress only invalid recursion and must allow legitimate downstream dependent-
property, calculated-property, Hub, and projection triggers to execute.
Rationale:
OA calculated/dependent chains rely on trigger propagation to keep runtime graph state current.
Source scope:
OAObjectInfo.onChange; OAObjectInfo._addTrigger calculated dependency registration; OATrigger dependentTriggers.
Related CODEX findings:
Existing package-info notes no trigger identity/path-specific recursion model is visible.
Suggested unit tests:
testLegitimateCalculatedDependencyChainFires, testSelfRecursiveTriggerSuppressedWithoutSuppressingSiblingDependency.
Spec target section:
Trigger Runtime / Reentrant Propagation

TRIGGER-CONCURRENT-001 — Registration Concurrency
Contract statement:
Trigger registration, dedupe, and deregistration must be atomic for a root class/listen-property scope, and
concurrent operations must not create duplicate registrations or leave stale path state.
Rationale:
Triggers can be added/removed by filters, runtime services, or graph setup while events are active.
Source scope:
OAObjectInfo._addTrigger; OAObjectInfo._removeTrigger; OATriggerService.addTrigger/removeTrigger.
Related CODEX findings:
Existing package-info notes non-atomic _addTrigger dedupe/add race.
Suggested unit tests:
testConcurrentSameTriggerRegistrationDoesNotDuplicate, testConcurrentRemoveAndAddLeavesOneValidRegistration.
Spec target section:
Trigger Runtime / Registration Concurrency

TRIGGER-CONCURRENT-002 — Removal During Dispatch
Contract statement:
Trigger removal during dispatch must not corrupt iteration, must have a documented effect on already-running or
already-scheduled callbacks, and must prevent future eligible executions.
Rationale:
Listeners can remove triggers during callbacks, and background work can be scheduled before removal.
Source scope:
OAObjectInfo.onChange; CopyOnWrite trigger lists; OATriggerService.runTrigger; OATriggerService.removeTrigger.
Related CODEX findings:
Existing package-info notes copy-on-write helps iteration, but stale already-scheduled background work can still
run.
Suggested unit tests:
testRemoveDuringTriggerDispatchDoesNotCorruptIteration, testScheduledTriggerAfterRemoveHasDocumentedBehavior,
testRemovedTriggerDoesNotFireFutureEvents.
Spec target section:
Trigger Runtime / Dispatch Concurrency

TRIGGER-IDENTITY-001 — Root Dedup Uses OA Identity
Contract statement:
When reverse traversal or fallback discovery finds the same root through multiple routes, duplicate suppression must
use stable OA object identity/key semantics.
Rationale:
Repeated execution for the same root and event corrupts derived state and business side effects.
Source scope:
OAObjectInfo._runOnChange2; OAFinder root discovery; OAObject key/GUID identity services.
Related CODEX findings:
Existing package-info notes duplicate root callback suppression depends on stable GUID/object key service.
Suggested unit tests:
testDuplicateRootFoundThroughManyPathsInvokedOnce, testDistinctRootsWithSimilarValuesBothFire.
Spec target section:
Trigger Runtime / Identity Semantics

TRIGGER-CACHE-001 — Loaded-Data-Only Cost Boundary
Contract statement:
Triggers configured for loaded-data-only execution must use only loaded object/cache/Hub state and must not force
datasource loading or lazy-load expansion.
Rationale:
Loaded-data-only triggers bound cost and avoid side effects during reactive runtime updates.
Source scope:
OATrigger.getOnlyUseLoadedData(); OATriggerMethodListener.onTrigger; OAFinder.setUseOnlyLoadedData; object cache
visit.
Related CODEX findings:
Existing package-info notes loaded-data-only fallback should be tested with unloaded Hubs/references.
Suggested unit tests:
testOnlyUseLoadedDataTriggerDoesNotSelectDatasource, testLoadedDataOnlyTriggerUsesCacheAndLoadedHubsOnly.
Spec target section:
Trigger Runtime / Loaded Data Boundary

TRIGGER-COST-001 — Trigger Cost Mode Is Explicit
Contract statement:
Trigger root resolution must execute through an explicit cost mode: direct reverse traversal, loaded-data scan,
datasource select, or background execution; fallback scans/selects must not be hidden from the trigger contract.
Rationale:
Triggers are deeper and more expensive than Hub listeners; runtime cost must be predictable for production object
graphs.
Source scope:
OATrigger flags; OATriggerMethodListener.onTrigger cache/select branches; OAObjectInfo reverse traversal;
OATriggerService background execution.
Related CODEX findings:
Existing package-info notes expensive fallback paths, unbounded background queue, and wrong query parameter for
nested Hub membership.
Suggested unit tests:
testDirectReverseTraversalAvoidsFullScanWhenRootResolvable, testFallbackSelectUsesCorrectQueryParameter,
testTriggerCostModeObservableByContract.
Spec target section:
Trigger Runtime / Cost Boundaries

TRIGGER-CALC-001 — Calculated Dependency Lifetime
Contract statement:
Calculated-property dependent trigger registrations must remain active while any registered trigger depends on them
and must not be removed by deregistering an unrelated trigger.
Rationale:
Calculated/dependent property chains keep derived state current across the object graph.
Source scope:
OAObjectInfo._addTrigger; OAObjectInfo._removeTrigger; OATrigger.getDependentTriggers(); setDependentTriggers(...).
Related CODEX findings:
Existing package-info notes calculated-property dependency ownership/reference-count issue.
Suggested unit tests:
testCalculatedPropertyDependencyTriggerReferenceCount,
testRemovingOneTriggerDoesNotDisableSharedCalculatedDependency.
Spec target section:
Trigger Runtime / Calculated Dependencies

TRIGGER-ANNOTATION-001 — Inherited Trigger Method Applicability
Contract statement:
Annotated trigger methods must apply to the concrete OAObjectInfo/class being initialized according to OA
inheritance and metadata rules, including inherited methods where the model contract requires them.
Rationale:
Subclass object changes should fire inherited model callbacks when inherited metadata semantics apply.
Source scope:
OATriggerMethodListener; annotation-driven trigger registration through OAObject annotation/metadata services.
Related CODEX findings:
Existing package-info notes superclass registration issue for annotated trigger methods.
Suggested unit tests:
testInheritedTriggerMethodRegisteredForSubclass, testSubclassChangeFiresInheritedTriggerMethod.
Spec target section:
Trigger Runtime / Annotation Integration

TRIGGER-GRAPH-001 — Graph-Scoped Trigger Authority
Contract statement:
Trigger registration and execution must route through the graph/runtime authority that owns the trigger root class
and affected object graph, without leaking trigger execution across graph boundaries.
Rationale:
Cross-graph trigger execution can corrupt independent runtime graphs, caches, Hubs, and datasource contexts.
Source scope:
OATrigger.getRootClass(); OATriggerMethodListener constructor; OARuntime.graph(...);
OATriggerService.addTrigger/removeTrigger; OAObjectInfo dispatch.
Related CODEX findings:
Existing package-info notes graph ownership is implicit by root class.
Suggested unit tests:
testTriggerRegisteredInOwningGraphOnly, testCrossGraphObjectChangeDoesNotFireOtherGraphTrigger.
Spec target section:
Trigger Runtime / Graph Ownership

TRIGGER-EVENT-001 — Event Ordering And Visibility
Contract statement:
Object property and Hub membership triggers must observe the semantic event after the object/Hub transition is
visible according to OA event ordering, and before/after event semantics must remain consistent with object and Hub
contracts.
Rationale:
Trigger callbacks must see the graph state that caused the event and must not observe stale or pre-commit state
unless explicitly contracted.
Source scope:
OAObjectEventService trigger integration; HubEventService trigger integration; OAObjectInfo.onChange;
OATriggerListener.onTrigger.
Related CODEX findings:
Existing package-info notes property and Hub event ordering should be locked by tests.
Suggested unit tests:
testPropertyTriggerSeesUpdatedValue, testHubAddTriggerSeesAddedMember,
testTriggerEventOrderingMatchesObjectAndHubContracts.
Spec target section:
Trigger Runtime / Event Ordering

TRIGGER-BOUNDARY-001 — Cross-Package Runtime Compatibility
Contract statement:
Trigger behavior must remain compatible with OAObject, Hub, event, metadata, path, cache, datasource, transaction,
sync, replication, queue/background execution, runtime, and graph contracts.
Rationale:
Triggers are reactive graph infrastructure; boundary violations can cause stale derived state, duplicate side
effects, sync/replay drift, or cache corruption.
Source scope:
OATrigger; OATriggerListener; OATriggerMethodListener; OAObjectInfo trigger integration; OATriggerService; object/
hub event services.
Related CODEX findings:
Package-info notes integration risks with object events, Hub events, calculated properties, sync/replication replay,
datasource select, and graph ownership.
Suggested unit tests:
testTriggerCompatibleWithTransactionBoundaryByContract, testTriggerDuringSyncReplayFollowsRuntimePolicy,
testDatasourceFallbackTriggerPreservesCacheIdentity.
Spec target section:
Trigger Runtime / Cross-Package Boundaries

*/


