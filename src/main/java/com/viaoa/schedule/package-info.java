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
 * Scheduling utilities built on top of the OA object model. This package
 * provides components for defining date–time availability ranges, merging
 * schedules, determining availability, and assigning schedule entries to
 * OAObjects. <p>
 *
 * Key components include:
 * <ul>
 *   <li>{@link com.viaoa.schedule.OADateTimeRange} – representation of an
 *       individual date–time range.</li>
 *   <li>{@link com.viaoa.schedule.OASchedule} – interval-set structure for
 *       merging and iterating date–time ranges.</li>
 *   <li>{@link com.viaoa.schedule.OASchedulerPlan} – full plan containing
 *       open, preferred, blocked, and scheduled ranges.</li>
 *   <li>{@link com.viaoa.schedule.OAScheduler} – aggregator for multiple
 *       plans belonging to a resource or object.</li>
 *   <li>{@link com.viaoa.schedule.OASchedulerController} – controller for
 *       selecting and applying schedule date–time values to OAObjects.</li>
 * </ul>
 *
 * These classes support rich scheduling models where availability depends on
 * both object relationships and date–time logic.
 */
package com.viaoa.schedule;

/* CODEX Invariants

SCHED-SCOPE-001 — Scheduling Availability Authority
Contract statement:
com.viaoa.schedule defines OA scheduling and availability semantics for date/time ranges, availability plans,
resource scheduling, schedule assignment, and date-change notification; it is not the authority for general-purpose
task execution.
Rationale:
The package primarily models schedule state and timed availability over OAObjects and Hubs, while executor-style
process behavior belongs to process/concurrent packages.
Source scope:
OADateTimeRange, OASchedule, OASchedulerPlan, OAScheduler, OASchedulerController, OADateChangeController.
Related CODEX findings:
package-info notes distinguish schedule modeling from general scheduled job execution.
Suggested unit tests:
schedulePackageModelsAvailabilityNotGenericExecution(),
schedulerControllerMutatesScheduleObjectsThroughScheduleContract().
Spec target section:
Schedule Runtime / Package Responsibility Semantics.

SCHED-RANGE-001 — Date/Time Range Identity and Ordering
Contract statement:
Schedule ranges must have deterministic begin/end identity, ordering, equality, hash, string, reference, and child-
provenance semantics for the same date/time bounds and reference state.
Rationale:
Schedule merging, iteration, availability checks, UI rendering, and object assignment depend on stable range
identity and chronological ordering.
Source scope:
OADateTimeRange.equals(...), hashCode(), compareTo(...), toString(), getBegin(), getEnd(), getReference(),
addChild(...), getChildren().
Related CODEX findings:
range provenance behavior is present but not fully specified.
Suggested unit tests:
dateTimeRangeOrdersChronologically(), dateTimeRangeEqualityMatchesRangeIdentityContract(),
dateTimeRangeChildrenPreserveSourceProvenance().
Spec target section:
Schedule Runtime / Range Semantics.

SCHED-INTERVAL-001 — Consistent Interval Boundary Semantics
Contract statement:
Add, clear, contains/range-added checks, plan availability, and scheduler availability must use one consistent
interval boundary contract for exact begin and end timestamps.
Rationale:
Availability at exact boundary times must not differ depending on which API is used.
Source scope:
OASchedule.add(...), OASchedule.clear(...), OASchedule.isRangeAdded(...), OASchedulerPlan.isAvailable(...),
OAScheduler.isAvailable(...).
Related CODEX findings:
package-info notes possible mixed inclusive and half-open boundary behavior.
Suggested unit tests:
scheduleClearBoundaryMatchesRangeAddedBoundary(), schedulerPlanAvailabilityUsesSameBoundaryContract(),
schedulerAvailabilityAtExactEndIsDefined().
Spec target section:
Schedule Runtime / Interval Boundary Semantics.

SCHED-MERGE-001 — Range Merge and Clear Correctness
Contract statement:
Adding and clearing ranges must produce a deterministic, non-corrupt schedule range set that preserves chronological
order, intended coverage, and exposed provenance.
Rationale:
Availability plans are built by combining open, preferred, blocked, and scheduled ranges; incorrect merge/split
behavior creates false availability or false blocking.
Source scope:
OASchedule.add(...), OASchedule.clear(...), OASchedule.size(), OASchedule.getSize(), OADateTimeRange.addChild(...),
OADateTimeRange.getChildren().
Related CODEX findings:
package-info notes provenance and boundary ambiguity during add/clear churn.
Suggested unit tests:
scheduleAddMergesOverlappingRangesDeterministically(), scheduleClearSplitsRangeCorrectly(),
scheduleClearSplitPreservesChildProvenance().
Spec target section:
Schedule Runtime / Range Merge Semantics.

SCHED-ITERATE-001 — Deterministic Range Traversal
Contract statement:
Schedule traversal must return ranges chronologically without null terminal elements, skipped ranges, duplicate
ranges, or interference between independent iteration contexts.
Rationale:
Availability rendering, slot search, and plan calculation depend on repeatable traversal of range sets.
Source scope:
OASchedule.iterator(), OASchedule.next(), OASchedule.nextEmpty(), OASchedule.reset(), OASchedule.rewind(),
OASchedule.isEndOfList().
Related CODEX findings:
OAScheduler.java CODEX block references OASchedule.iterator hasNext terminal behavior and shared cursor concerns.
Suggested unit tests:
scheduleIteratorStopsAfterLastRange(), scheduleNestedIterationIsIndependentOrExplicitlyRejected(),
scheduleCursorResetRestartsTraversal().
Spec target section:
Schedule Runtime / Traversal Semantics.

SCHED-MUTATE-001 — Schedule Mutation Resets Traversal State
Contract statement:
Mutating a schedule through add, clear, or full clear must leave subsequent traversal and availability checks in a
valid deterministic state.
Rationale:
Stale cursor or traversal state after mutation can skip new ranges or falsely report end-of-list.
Source scope:
OASchedule.add(...), OASchedule.clear(...), OASchedule.clear(), OASchedule.next(), OASchedule.nextEmpty(),
OASchedule.iterator().
Related CODEX findings:
package-info notes OASchedule.add/clear may not consistently reset object-global traversal state.
Suggested unit tests:
scheduleMutationResetsCursorState(), scheduleClearAllResetsTraversalState(),
scheduleAddAfterEndOfListIsVisibleToTraversal().
Spec target section:
Schedule Runtime / Mutation and Traversal Semantics.

SCHED-PLAN-001 — Availability Plan Category Semantics
Contract statement:
An OASchedulerPlan must evaluate open, soft-open, preferred, soft-preferred, blocked, soft-blocked, and scheduled
ranges according to their distinct availability roles.
Rationale:
Plan categories encode business scheduling meaning; conflating them can incorrectly allow, prefer, block, or reserve
a time slot.
Source scope:
OASchedulerPlan.getOpenSchedule(), getOpenSoftSchedule(), getPreferredSchedule(), getPreferredSoftSchedule(),
getBlockedSchedule(), getBlockedSoftSchedule(), getScheduledSchedule(), isAvailable(...).
Related CODEX findings:
none observed.
Suggested unit tests:
schedulerPlanOpenRangeAllowsAvailability(), schedulerPlanBlockedRangeRejectsAvailability(),
schedulerPlanScheduledRangeAffectsAvailabilityByContract().
Spec target section:
Schedule Runtime / Plan Category Semantics.

SCHED-PLAN-002 — Plan Window Semantics
Contract statement:
A scheduler plan must apply its declared begin/end window consistently before evaluating child schedule ranges.
Rationale:
The plan window is the outer temporal boundary for all contained availability rules.
Source scope:
OASchedulerPlan constructors, getBegin(), getEnd(), isAvailable(...).
Related CODEX findings:
OASchedulerPlan.java CODEX note on constructor window semantics for OASchedulerPlan(OADateTime).
Suggested unit tests:
schedulerPlanDateConstructorUsesDocumentedWindow(), schedulerPlanDateTimeConstructorUsesDocumentedWindow(),
schedulerPlanAvailabilityRejectsOutsidePlanWindow().
Spec target section:
Schedule Runtime / Plan Window Semantics.

SCHED-SCHEDULER-001 — Scheduler Aggregation Semantics
Contract statement:
An OAScheduler must aggregate its schedule plans deterministically for the same search object, scheduler window, and
plan list.
Rationale:
Resource availability must not depend on accidental plan order, stale calculation state, or repeated calculation
side effects unless explicitly contracted.
Source scope:
OAScheduler constructor, getSearchObject(), getBegin(), getEnd(), add(...), calculate(), getSchedulePlans(),
isAvailable(...).
Related CODEX findings:
none observed.
Suggested unit tests:
schedulerAggregationIsDeterministicForSamePlans(), schedulerCalculateDoesNotDuplicatePlanEffects(),
schedulerAvailabilityCombinesPlansByContract().
Spec target section:
Schedule Runtime / Scheduler Aggregation Semantics.

SCHED-SCHEDULER-002 — Scheduler Window Authority
Contract statement:
Scheduler-level begin/end bounds must define the outer availability window for scheduler availability results.
Rationale:
Callers rely on a scheduler’s top-level window to constrain resource availability before child plans are considered.
Source scope:
OAScheduler.getBegin(), OAScheduler.getEnd(), OAScheduler.isAvailable(...).
Related CODEX findings:
package-info notes scheduler availability may delegate only to plans.
Suggested unit tests:
schedulerAvailabilityRejectsBeforeTopLevelBegin(), schedulerAvailabilityRejectsAfterTopLevelEnd(),
schedulerAvailabilityWithinWindowUsesPlans().
Spec target section:
Schedule Runtime / Scheduler Window Semantics.

SCHED-CONTROLLER-001 — Controller Setup Validity
Contract statement:
OASchedulerController must resolve a valid schedule relationship type, detail hub, property path, and assignment
strategy before mutating OAObjects or Hubs.
Rationale:
Schedule assignment writes runtime object graph state; unresolved relationship metadata must not produce false-
success object creation or missing links.
Source scope:
OASchedulerController constructors, setup(), getType(), getDetailHub(), getFromDateProperty(), set(...).
Related CODEX findings:
package-info notes type == 0 unresolved relationship risk and false-success set paths.
Suggested unit tests:
schedulerControllerFailsVisibleWhenRelationshipTypeUnresolved(), schedulerControllerRequiresResolvableDetailHub(),
schedulerControllerSetupClassifiesRelationshipTypeDeterministically().
Spec target section:
Schedule Runtime / Controller Setup Semantics.

SCHED-CONTROLLER-002 — Schedule Assignment Graph Consistency
Contract statement:
Schedule assignment must preserve OAObject, Hub, link, reverse-link, cardinality, and owning graph semantics for the
relationship shape being assigned.
Rationale:
Scheduling controller mutations become observable object graph state and may be persisted, serialized, synced, or
replicated.
Source scope:
OASchedulerController.set(...), setup(), getDetailHub(), integration with Hub, OAObject, OARuntime.graph(...), path/
link metadata.
Related CODEX findings:
package-info notes type 1-4 paths depend on inferred reverse path shape and graph ownership.
Suggested unit tests:
schedulerControllerTypeOneAssignmentPreservesLinkSemantics(),
schedulerControllerTypeManyAssignmentPreservesHubMembership(), schedulerControllerCreatesLinkObjectOnOwningGraph().
Spec target section:
Schedule Runtime / Object Graph Assignment Semantics.

SCHED-CONTROLLER-003 — Duplicate Slot Detection Semantics
Contract statement:
Duplicate schedule-slot detection must compare the same semantic date/time fields and relationship fields that
assignment writes.
Rationale:
Date/time split models must not create duplicate schedule objects for the same slot, and duplicate checks must not
miss existing schedule records.
Source scope:
OASchedulerController.set(...), ppDateFrom, ppTimeFrom, ppDateTo, ppTimeTo, ppSchedule, datasource/cache lookup
paths.
Related CODEX findings:
OASchedulerController.java CODEX note on separate date/time duplicate detection comparing date properties to
OADateTime values.
Suggested unit tests:
schedulerControllerSeparateDateTimeDuplicateDetectionUsesDateAndTime(),
schedulerControllerReusesExistingTimeslotWhenContractRequires(),
schedulerControllerDoesNotCreateDuplicateSlotForSameRange().
Spec target section:
Schedule Runtime / Duplicate Assignment Semantics.

SCHED-CONTROLLER-004 — Assignment Failure Visibility
Contract statement:
Schedule assignment must not return as successful after failing to create, find, assign, link, or persist the
intended schedule relationship unless the no-op behavior is explicitly contracted.
Rationale:
Silent assignment failure can leave persistent OAObjects and observable schedule state inconsistent.
Source scope:
OASchedulerController.set(...), datasource/select lookup, Hub/link mutation, graph object creation.
Related CODEX findings:
package-info notes false-success paths, invalid/precondition returns, and partial-progress ambiguity.
Suggested unit tests:
schedulerControllerRejectsReverseRange(), schedulerControllerFailsVisibleWhenNoAssignmentPerformed(),
schedulerControllerPartialFailureIsObservable().
Spec target section:
Schedule Runtime / Assignment Failure Semantics.

SCHED-CALLBACK-001 — Date-Change Callback Registration Semantics
Contract statement:
Date-change callback registration must have explicit lifetime semantics: either callbacks remain active until
unregistered or weak-reference lifetime is the documented contract.
Rationale:
A date-change callback that disappears silently can skip required time-driven runtime behavior.
Source scope:
OADateChangeController.Callback, OADateChangeController.onChange(...), callback weak-reference list.
Related CODEX findings:
OADateChangeController.java CODEX notes lifecycle and weak callback registration risks.
Suggested unit tests:
dateChangeWeakCallbackRequiresStrongReferenceOrRegistrationContract(), dateChangeLiveCallbackReceivesDateChange(),
dateChangeDeadWeakCallbackIsNotInvoked().
Spec target section:
Schedule Runtime / Date-Change Registration Semantics.

SCHED-CALLBACK-002 — Date-Change Notification Lifecycle
Contract statement:
The date-change notifier must have deterministic singleton/lifecycle behavior for the callback list and must not
duplicate notifications through competing notifier threads.
Rationale:
Duplicate notifier threads can call callbacks multiple times, overlap execution, and retain unnecessary runtime
resources.
Source scope:
OADateChangeController.onChange(...), OADateChangeController.process(), static notifier thread/list state.
Related CODEX findings:
OADateChangeController.java CODEX note that new notifier thread is assigned to a local variable rather than the
static field.
Suggested unit tests:
dateChangeControllerStartsOnlyOneNotifierThread(), dateChangeControllerDoesNotDuplicateNotifications(),
dateChangeNotifierLifecycleStateIsObservable().
Spec target section:
Schedule Runtime / Date-Change Lifecycle Semantics.

SCHED-TIME-001 — Local Calendar Date Transition Semantics
Contract statement:
Date-change notification must be based on local calendar-date transitions, not merely elapsed 24-hour intervals.
Rationale:
Scheduling and availability behavior is date-oriented and must align with OA date semantics across midnight,
daylight-saving, and local calendar boundaries.
Source scope:
OADateChangeController.process(), OADate, OADateTime integration.
Related CODEX findings:
package-info notes behavior under system clock jumps and DST is not specified.
Suggested unit tests:
dateChangeFiresOncePerLocalDateTransition(), dateChangeHandlesForwardClockJumpByContract(),
dateChangeHandlesBackwardClockJumpWithoutBusySpin().
Spec target section:
Schedule Runtime / Calendar Timing Semantics.

SCHED-TIME-002 — Sleep and Clock Drift Boundary
Contract statement:
Computed sleep intervals must handle zero, negative, interrupted, and clock-adjusted values without busy loops,
missed date transitions, or silent notifier death.
Rationale:
Long-running OA runtimes can cross daylight-saving changes, system clock adjustments, and shutdown interrupts.
Source scope:
OADateChangeController.process(), Thread.sleep timing loop.
Related CODEX findings:
package-info notes sleep exceptions are swallowed and drift/backoff policy is unspecified.
Suggested unit tests:
dateChangeControllerHandlesZeroSleepWithoutBusySpin(),
dateChangeControllerHandlesClockJumpForwardWithoutDuplicateNotification(),
dateChangeControllerHandlesSleepExceptionObservably().
Spec target section:
Schedule Runtime / Clock Drift and Sleep Semantics.

SCHED-ORDER-001 — Callback Snapshot and Ordering
Contract statement:
Each date-change notification cycle must invoke callbacks from a stable registration snapshot in deterministic order
for that cycle.
Rationale:
Concurrent registration, stale-reference cleanup, and reentrant callback registration must not corrupt active
delivery.
Source scope:
OADateChangeController.onChange(...), OADateChangeController.process(), callback list snapshot.
Related CODEX findings:
package-info notes snapshot behavior and reentrant registration expectations.
Suggested unit tests:
dateChangeCallbacksExecuteInRegistrationOrder(),
dateChangeCallbackSnapshotIgnoresConcurrentRegistrationUntilNextCycle(),
dateChangeCallbackRegistrationDuringCallbackAffectsFutureCycleOnly().
Spec target section:
Schedule Runtime / Callback Ordering Semantics.

SCHED-FAIL-001 — Callback Failure Isolation and Visibility
Contract statement:
A date-change callback failure must be observable and must not prevent remaining eligible callbacks or future date-
change cycles unless the package explicitly defines fail-fast behavior.
Rationale:
Date-change callbacks are observer infrastructure; one observer must not silently kill the scheduling notifier or
skip unrelated observers.
Source scope:
OADateChangeController.Callback.onDateChange(), OADateChangeController.process().
Related CODEX findings:
package-info notes callback exceptions can stop remaining callbacks and terminate notifier processing.
Suggested unit tests:
dateChangeCallbackExceptionDoesNotStopRemainingCallbacks(),
dateChangeCallbackExceptionDoesNotKillFutureNotifications(), dateChangeCallbackExceptionIsObservable().
Spec target section:
Schedule Runtime / Callback Failure Semantics.

SCHED-RESOURCE-001 — Select and Runtime Resource Cleanup
Contract statement:
Resources opened by schedule assignment, including datasource/select resources, must be closed or released on
success and failure unless ownership is explicitly transferred.
Rationale:
Repeated schedule assignment in production must not leak datasource, select, or object graph resources.
Source scope:
OASchedulerController.set(...), OASelect usage, datasource lookup paths.
Related CODEX findings:
package-info notes OASelect opened by schedule assignment should be closed on success and failure.
Suggested unit tests:
schedulerControllerClosesSelectAfterSuccessfulLookup(), schedulerControllerClosesSelectAfterFailedLookup(),
schedulerControllerResourceCleanupRunsOnException().
Spec target section:
Schedule Runtime / Resource Cleanup Semantics.

SCHED-PARTIAL-001 — Partial Assignment Visibility
Contract statement:
Schedule operations that create or mutate multiple objects or links must not appear complete unless the intended
schedule object, link object, Hub membership, and date/time values are all committed or incompleteness is
observable.
Rationale:
Schedule assignment can be observed by persistence, serialization, sync, replication, triggers, and UI bindings.
Source scope:
OASchedulerController.set(...), Hub/link mutation, OARuntime.graph object creation, datasource/select lookup.
Related CODEX findings:
package-info notes no visible transaction/rollback boundary and partial-progress ambiguity.
Suggested unit tests:
scheduleControllerPartialFailureIsCallerVisible(),
scheduleControllerTransactionRollbackLeavesNoFalseLinkedSchedule(),
scheduleControllerDoesNotEmitCompleteStateForIncompleteAssignment().
Spec target section:
Schedule Runtime / Partial Progress Semantics.

SCHED-CONCURRENT-001 — Schedule Model Thread Ownership
Contract statement:
Schedule model objects are single-thread-owned unless externally synchronized; concurrent mutation and traversal
must not be assumed safe without a synchronization contract.
Rationale:
Schedules and plans expose mutable collections and cursor-like traversal state that can be corrupted by concurrent
mutation.
Source scope:
OASchedule, OASchedulerPlan, OAScheduler, OADateTimeRange.getChildren(), OAScheduler.getSchedulePlans().
Related CODEX findings:
package-info notes internal ArrayList/TreeSet/cursor fields are not thread-safe and mutable collections are live.
Suggested unit tests:
scheduleConcurrentMutationRequiresExternalSynchronization(), scheduleLiveCollectionsAreDocumentedMutableState(),
schedulerPlanConcurrentMutationDoesNotHaveImplicitSafety().
Spec target section:
Schedule Runtime / Thread Ownership Semantics.

SCHED-TL-001 — Runtime Context Restoration
Contract statement:
Background schedule callbacks or controller code that establishes OA ThreadLocal, transaction, graph, security, or
runtime context must restore prior context with try/finally.
Rationale:
Schedule callbacks can interact with OAObjects, Hubs, graph services, datasource work, sync, and replication; leaked
runtime context can corrupt later operations.
Source scope:
OADateChangeController.process(), OADateChangeController.Callback.onDateChange(), OASchedulerController.set(...),
cross-package runtime context boundaries.
Related CODEX findings:
package-info notes notifier thread does not establish or clear OA runtime context.
Suggested unit tests:
dateChangeCallbackRestoresOAThreadLocalContext(), scheduleControllerRestoresRuntimeContextAfterFailure(),
dateChangeNotifierDoesNotLeakContextBetweenCallbacks().
Spec target section:
Schedule Runtime / ThreadLocal Context Semantics.

SCHED-INTEGRATION-001 — Metadata and Relationship Integration
Contract statement:
Schedule assignment and availability evaluation must remain consistent with OA metadata, path, object, Hub,
datasource, transaction, sync, replication, and graph ownership contracts.
Rationale:
Scheduling is observable Object Graph behavior; schedule objects, relationships, and availability results must not
conflict with runtime metadata authority.
Source scope:
OASchedulerController setup/set; OAScheduler.getSchedulerCallback(...); OASchedulerPlan; OASchedule; integration
with OAObject, Hub, OARuntime.graph, OASelect, path/link metadata.
Related CODEX findings:
package-info notes graph routing, type derivation, callback reflection exceptions, and sync/replication side-effect
boundaries.
Suggested unit tests:
schedulerControllerPreservesHubLinkMetadataSemantics(), schedulerControllerCreatesObjectsInOwningGraph(),
schedulerCallbackFailureIsRuntimeVisible().
Spec target section:
Schedule Runtime / Cross-Package Integration Semantics.

SCHED-BOUNDARY-001 — Schedule Trigger Versus Semantic Runtime Success
Contract statement:
A successful schedule trigger, availability calculation, or assignment method return only establishes schedule-
package success; it must not imply successful datasource commit, transaction commit, sync propagation, replication
replay, or broader Object Graph semantic success.
Rationale:
Scheduling is a runtime coordination boundary, but persistence and distributed operation success are owned by their
respective packages.
Source scope:
OADateChangeController; OASchedule; OASchedulerPlan; OAScheduler; OASchedulerController; boundaries with datasource,
transaction, sync, replication, object, hub, and graph packages.
Related CODEX findings:
package-info notes schedule assignment can emit partial object/link changes observed by sync/replication.
Suggested unit tests:
scheduleAssignmentSuccessDoesNotImplyTransactionCommit(),
dateChangeCallbackSuccessDoesNotImplyGraphMutationSuccess(), availabilityCalculationDoesNotMutateObjectGraph().
Spec target section:
Schedule Runtime / Runtime Boundary Semantics.

*/

