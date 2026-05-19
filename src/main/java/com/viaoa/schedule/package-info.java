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

1. Package Summary
  com.viaoa.schedule is a scheduling/availability subsystem, not a general executor-style task scheduler. In OA 4.0
  terms it provides:

  - Date/time range modeling and merging: OASchedule, OADateTimeRange
  - Resource availability plans: OASchedulerPlan
  - Aggregated object/resource scheduling facade: OAScheduler
  - OAObject schedule assignment/controller logic: OASchedulerController
  - A lightweight global date-change callback notifier: OADateChangeController

  The package currently has only one real background execution component: OADateChangeController. The rest is
  deterministic schedule modeling and OAObject relationship assignment.

  2. Core Concepts

  - Scheduler: OAScheduler, an object/resource-level availability evaluator that aggregates one or more
    OASchedulerPlan instances.
  - Scheduled task/job: only present in this package as OADateChangeController.Callback; there is no general scheduled
    job API here.
  - Recurring schedule: modeled indirectly by repeated date/time ranges in OASchedule or category schedules in
    OASchedulerPlan.
  - One-time schedule: a single OADateTimeRange stored in OASchedule.
  - Task execution: OADateChangeController.process() invokes callbacks when the calendar date changes.
  - Task cancellation: no explicit unregister/cancel API exists in OADateChangeController; callback lifetime is weak-
    reference based.
  - Rescheduling: no general rescheduling API; range mutation occurs through OASchedule.add/clear.
  - Scheduler lifecycle: implicit for OADateChangeController, constructor/setup based for OASchedulerController, and
    object-local for schedule/range classes.
  - Shutdown/close: no explicit shutdown/close in this package.
  - Execution thread ownership: OADateChangeController owns a daemon notifier thread.
  - Timing source / clock assumptions: OADateChangeController uses OADate, OADateTime, and Thread.sleep(diff) against
    system/local time.
  - Listener/callback behavior: OADateChangeController.Callback.onDateChange() is an observer callback.

  3. Invariants

  A. Scheduler Lifecycle Invariants

  1. SCHED-LIFE-001: Date-change notifier is singleton per runtime

  - Statement: At most one live OADateChangeController notifier thread may exist for the callback list.
  - Why it matters: duplicate notifier threads can duplicate date-change execution and retain unnecessary runtime
    resources.
  - Code: OADateChangeController.onChange, OADateChangeController.process
  - Confidence: Low
  - Gap: lifecycle is implicit; current code has no explicit started/stopped state.

  2. SCHED-LIFE-002: Schedule controllers must be fully classified before use

  - Statement: OASchedulerController.setup() must resolve a valid relationship type before set() can mutate or assign
    schedule objects.
  - Why it matters: unresolved type can produce false-success object creation without assignment.
  - Code: OASchedulerController.setup, OASchedulerController.set
  - Confidence: Low
  - Gap: type == 0 is possible when reverse metadata is absent.

  3. SCHED-LIFE-003: Scheduler availability must honor its declared window

  - Statement: OAScheduler.isAvailable(dt) must reject dt outside scheduler-level begin/end bounds before applying
    child plans.
  - Why it matters: callers rely on the top-level scheduler window as the outer availability contract.
  - Code: OAScheduler.getBegin, getEnd, isAvailable
  - Confidence: Low
  - Gap: current enforcement appears delegated only to plans.

  B. Task Registration / Cancellation Invariants

  4. SCHED-REG-001: Callback registration must have explicit lifetime semantics

  - Statement: A registered date-change callback must either remain active until explicitly unregistered or clearly
    require caller-held strong reference.
  - Why it matters: weak-only registration can silently disappear.
  - Code: OADateChangeController.onChange, alCallback
  - Confidence: Medium
  - Gap: no unregister API; weak-reference semantics are implicit.

  5. SCHED-REG-002: Stale callback references must be cleaned predictably

  - Statement: cleared weak callback references must not accumulate indefinitely in normal runtime use.
  - Why it matters: long-running runtimes should not retain stale registration wrappers.
  - Code: OADateChangeController.process
  - Confidence: Medium
  - Gap: cleanup only happens during date-change processing.

  6. SCHED-REG-003: Schedule range mutation must reset traversal state

  - Statement: OASchedule.add/clear must leave subsequent traversal in a valid state.
  - Why it matters: stale cursor state can skip new ranges or falsely report end-of-list.
  - Code: OASchedule.add, clear, next, reset
  - Confidence: Low
  - Gap: cursor state is object-global and not reset consistently by mutation.

  C. Timing / Clock Invariants

  7. SCHED-TIME-001: Date-change detection is based on local calendar-date semantics

  - Statement: OADateChangeController must fire once per local date transition, not once per elapsed 24-hour duration.
  - Why it matters: schedule/calendar behavior is date-oriented.
  - Code: OADateChangeController.process
  - Confidence: Medium
  - Gap: behavior under system clock jumps/DST is not specified.

  8. SCHED-TIME-002: Sleep interval must not cause busy loop or missed transition

  - Statement: computed sleep duration must handle zero/negative values and clock changes without spinning or missing
    date change.
  - Why it matters: enterprise runtimes can run across DST changes and system time adjustments.
  - Code: OADateChangeController.process
  - Confidence: Medium
  - Gap: exceptions are swallowed and there is no backoff/diagnostic path.

  9. SCHED-TIME-003: Range boundary semantics must be consistent

  - Statement: add, clear, contains, and availability checks must use the same closed or half-open interval contract.
  - Why it matters: inconsistent boundary handling causes wrong availability at exact begin/end times.
  - Code: OASchedule.add, clear, isRangeAdded; OASchedulerPlan.isAvailable
  - Confidence: Low
  - Gap: current methods appear to mix inclusive and half-open behavior.

  D. Execution Ordering / Determinism Invariants

  10. SCHED-ORDER-001: Date-change callbacks execute from a deterministic snapshot

  - Statement: one date-change cycle must invoke callbacks from a stable snapshot of registrations.
  - Why it matters: concurrent register/remove behavior must not corrupt iteration.
  - Code: OADateChangeController.process
  - Confidence: High
  - Gap: ordering is registration-list order but not documented.

  11. SCHED-ORDER-002: Range iteration must be chronological and independent per iterator

  - Statement: every iterator over OASchedule must traverse ranges chronologically without interfering with other
    iterators.
  - Why it matters: availability checks and UI/time-slot rendering depend on deterministic traversal.
  - Code: OASchedule.iterator, next, nextEmpty
  - Confidence: Low
  - Gap: traversal uses shared object-level state.

  12. SCHED-ORDER-003: Merged ranges preserve provenance when provenance is exposed

  - Statement: when a range absorbs/splits another range, child/reference provenance must remain correct for all
    resulting visible ranges.
  - Why it matters: callers can use OADateTimeRange.getChildren() and getReference() to understand source ranges.
  - Code: OASchedule.add, clear; OADateTimeRange.addChild, getChildren
  - Confidence: Medium
  - Gap: provenance behavior is present but not fully specified.

  E. Threading / Concurrency Invariants

  13. SCHED-THREAD-001: Schedule model classes are single-thread-owned unless synchronized externally

  - Statement: OASchedule, OASchedulerPlan, and OAScheduler must not be mutated concurrently without external
    coordination.
  - Why it matters: internal ArrayList, TreeSet, and cursor fields are not thread-safe.
  - Code: OASchedule, OASchedulerPlan, OAScheduler
  - Confidence: Medium
  - Gap: thread-safety contract is undocumented.

  14. SCHED-THREAD-002: Date-change callback list mutation must be synchronized

  - Statement: registration and stale-reference removal must synchronize on the callback list.
  - Why it matters: date-change callbacks can be registered while notifier is running.
  - Code: OADateChangeController.onChange, process
  - Confidence: High
  - Gap: lifecycle state for the notifier thread is not synchronized because it is not stored reliably.

  15. SCHED-THREAD-003: Scheduler callbacks must not inherit stale OAThreadLocal/transaction state

  - Statement: background callback execution must either run with a clean runtime context or document that callbacks
    inherit notifier-thread state.
  - Why it matters: callbacks can interact with OAObject/Hub/graph services.
  - Code: OADateChangeController.process
  - Confidence: Low
  - Gap: no OAThreadLocal or transaction cleanup/reset is visible here.

  F. Exception-Handling Invariants

  16. SCHED-EX-001: Observer callback failure must not kill date-change delivery

  - Statement: one onDateChange() exception must not prevent remaining callbacks or future date-change cycles.
  - Why it matters: date-change notification is infrastructure; observer failure should be isolated.
  - Code: OADateChangeController.process
  - Confidence: Low
  - Gap: callback invocation is not isolated.

  17. SCHED-EX-002: Infrastructure exceptions must be observable

  - Statement: scheduler infrastructure errors should be logged or otherwise observable, not silently swallowed.
  - Why it matters: silent notifier failure or timing problems are hard production failures.
  - Code: OADateChangeController.process
  - Confidence: Low
  - Gap: sleep exceptions are swallowed.

  18. SCHED-EX-003: Controller false-success paths must be avoided

  - Statement: OASchedulerController.set() must not return normally after failing to assign or link the intended
    schedule.
  - Why it matters: scheduling mutations affect persistent OAObjects.
  - Code: OASchedulerController.set
  - Confidence: Medium
  - Gap: many invalid/precondition paths return silently; some are intentional, others ambiguous.

  G. Reentrancy / Overlap Invariants

  19. SCHED-REENTRANT-001: Date-change callbacks may not recursively corrupt callback delivery

  - Statement: callbacks registering new callbacks during execution must not modify the active callback snapshot.
  - Why it matters: reentrant registration should affect future cycles only.
  - Code: OADateChangeController.process
  - Confidence: High
  - Gap: recursive/unregister behavior is not formalized.

  20. SCHED-REENTRANT-002: Range traversal APIs must define cursor reentrancy

  - Statement: stateful cursor methods next/nextEmpty/reset must not be mixed with iterator/enhanced-for traversal
    unless explicitly allowed.
  - Why it matters: mixed traversal corrupts object-level cursor state.
  - Code: OASchedule.next, nextEmpty, iterator, isRangeAdded
  - Confidence: Low
  - Gap: current API exposes both stateful and iterable traversal without separation.

  21. SCHED-OVERLAP-001: Duplicate slot assignment must be prevented by matching all time fields

  - Statement: duplicate detection must match the same semantic fields that assignment writes.
  - Why it matters: date/time split models must not create duplicate slots for the same time range.
  - Code: OASchedulerController.set
  - Confidence: Medium
  - Gap: matching and assignment logic are not centralized.

  H. Shutdown / Cleanup Invariants

  22. SCHED-SHUTDOWN-001: Background notifier must have defined shutdown semantics

  - Statement: runtime shutdown should be able to stop the date-change notifier or prove daemon-only lifetime is
    acceptable.
  - Why it matters: production runtime restart/teardown should not retain stale callbacks or context.
  - Code: OADateChangeController
  - Confidence: Low
  - Gap: no shutdown/close API.

  23. SCHED-CLEANUP-001: Datasource/select resources must be closed

  - Statement: any OASelect opened by schedule assignment must be closed on success and failure.
  - Why it matters: repeated scheduling operations can leak datasource resources.
  - Code: OASchedulerController.set
  - Confidence: Low
  - Gap: explicit close/finally is absent.

  24. SCHED-CLEANUP-002: Stale weak registrations must not keep the subsystem alive forever

  - Statement: callback list cleanup should allow the notifier to stop or remain bounded when no live callbacks
    remain.
  - Why it matters: long-running runtimes should not accumulate dead scheduling state.
  - Code: OADateChangeController.alCallback, process
  - Confidence: Medium
  - Gap: no self-stop or empty-list lifecycle behavior.

  I. Memory / Resource Retention Invariants

  25. SCHED-MEM-001: Range children must not grow unexpectedly beyond represented overlap history

  - Statement: child ranges should only retain provenance needed by callers, not unbounded duplicate history from
    repeated add/clear churn.
  - Why it matters: schedules can be built dynamically from many slots.
  - Code: OADateTimeRange.alChildren, OASchedule.add, clear
  - Confidence: Medium
  - Gap: provenance retention policy is implicit.

  26. SCHED-MEM-002: Public mutable collections must be treated as live mutable state

  - Statement: callers receiving getSchedulePlans() or getChildren() must understand they are mutating internal state.
  - Why it matters: external mutation can break ordering/lifecycle expectations.
  - Code: OAScheduler.getSchedulePlans, OADateTimeRange.getChildren
  - Confidence: Medium
  - Gap: mutability contract is not explicit.

  J. Integration Invariants

  27. SCHED-INTEGRATION-001: Controller graph routing must use the owning object/hub graph

  - Statement: new schedule/link objects must be created through the graph that owns the target hub/object.
  - Why it matters: cross-graph object creation breaks OG ownership semantics.
  - Code: OASchedulerController.set, OARuntime.graph(...)
  - Confidence: Medium
  - Gap: routing through hubDetail, hubDetail.getObjectClass(), and hubx needs consistent ownership assumptions.

  28. SCHED-INTEGRATION-002: Schedule assignment must preserve Hub/link metadata semantics

  - Statement: type 1-4 assignment paths must preserve one/many/link-object cardinality and reverse-link semantics.
  - Why it matters: scheduling controller mutates OAObject links and Hubs.
  - Code: OASchedulerController.setup, set
  - Confidence: Medium
  - Gap: type derivation is inferred from reverse path shape and is not strongly validated.

  29. SCHED-INTEGRATION-003: Scheduler callbacks must be runtime-visible failures

  - Statement: object scheduler callback exceptions should propagate or be recorded visibly.
  - Why it matters: availability generation failure must not silently produce empty/wrong schedules.
  - Code: OASchedulerController.getSchedulerCallback; integration with
    OAObjectSchedulerService.getScheduler/invokeCallback
  - Confidence: High
  - Gap: graph service wraps callback reflection exceptions; schedule controller itself does not add diagnostics.

  30. SCHED-INTEGRATION-004: Sync/replication side effects must match completed object mutations

  - Statement: schedule controller must not emit object/link changes that falsely represent an incomplete assignment
    as complete.
  - Why it matters: schedule assignment can create schedule and link objects that sync/replication will observe.
  - Code: OASchedulerController.set
  - Confidence: Medium
  - Gap: no transaction/rollback boundary is visible in this package; partial-progress semantics likely rely on OA
    caller/transaction behavior.

  4. Listener / Callback Semantics

  OADateChangeController.Callback is best classified as a DURING observer: it reacts to date change notification and
  should not cancel the notifier or prevent other callbacks.

  Alignment:

  - Callback list is snapshotted before execution, which supports deterministic observer delivery.

  Conflicts:

  - cb.onDateChange() is fail-fast by accident: one exception stops remaining callbacks and can terminate the notifier
    thread.
  - Exceptions are not aggregated or logged.
  - There is no AFTER cleanup/finalization callback phase.
  - Weak callback cleanup is opportunistic and not a formal cleanup phase.

  Required policy for OA-wide consistency:

  - BEFORE: not currently present in this package.
  - DURING: date-change callbacks should continue after individual failures.
  - AFTER: no current construct; if added later, it should always run remaining cleanup callbacks and aggregate
    errors.

  5. Failure Modes

  - Missed execution: callback garbage collected because only weakly referenced.
  - Duplicate execution: multiple notifier threads from bad lifecycle state.
  - Overlapping execution: multiple notifier threads can call the same callback concurrently at date change.
  - Task runs after cancellation: no cancellation API exists; weak GC is not explicit cancellation.
  - Task runs after scheduler shutdown: no shutdown API exists.
  - Scheduler thread dies silently: uncaught callback exception terminates process().
  - Long-running task starves others: callbacks run sequentially on the single notifier thread.
  - Exception skips cleanup: callback failures skip remaining callbacks.
  - Stale scheduled task retained: weak refs remain until next date-change cleanup.
  - Recursive scheduling causes ambiguous behavior: callback can register more callbacks during delivery.
  - Shutdown races with execution: shutdown semantics absent.
  - System clock change causes bad scheduling behavior: sleep/diff logic has no drift/backoff policy.
  - ThreadLocal/transaction context leaks: notifier thread does not establish/clear OA runtime context.
  - False schedule assignment success: controller can return after not linking a created/found schedule.
  - Duplicate schedule objects: split date/time matching/query behavior can miss existing schedule records.
  - Wrong availability result: iterator and boundary semantics can produce false positives/false negatives.

  6. Test Recommendations

  - testDateChangeControllerStartsOnlyOneNotifierThread
  - testDateChangeCallbackExceptionDoesNotStopRemainingCallbacks
  - testDateChangeCallbackExceptionDoesNotKillFutureNotifications
  - testDateChangeWeakCallbackRequiresStrongReferenceOrUnregisterContract
  - testDateChangeControllerCleansDeadWeakReferences
  - testDateChangeControllerHandlesClockJumpBackwardWithoutBusySpin
  - testDateChangeControllerHandlesClockJumpForwardWithoutDuplicateNotification
  - testScheduleIteratorDoesNotReturnNullAfterLastRange
  - testScheduleNestedIterationIsIndependent
  - testScheduleMutationResetsCursorState
  - testScheduleClearBoundaryMatchesContainsBoundary
  - testScheduleClearSplitPreservesChildProvenanceOnBothSides
  - testScheduleCompareToAllowsSameBeginDifferentEndOrRejectsExplicitly
  - testSchedulerAvailabilityHonorsTopLevelBeginEnd
  - testSchedulerPlanDateTimeConstructorUsesDocumentedWindow
  - testSchedulerPlanAvailabilityRejectsOutsidePlanWindow
  - testSchedulerControllerRejectsReverseRange
  - testSchedulerControllerFailsVisibleWhenRelationshipTypeUnresolved
  - testSchedulerControllerSeparateDateTimeDuplicateDetectionUsesDateAndTime
  - testSchedulerControllerSeparateDateTimeDatasourceQueryBuildsValidWhere
  - testSchedulerControllerType2AssignsExistingScheduleFoundInCache
  - testSchedulerControllerSharedTimeslotReusesExistingScheduleWhenContractRequires
  - testSchedulerControllerClosesSelectAfterLookup
  - testSchedulerControllerCreatesLinkObjectOnOwningGraph
  - testScheduleControllerPartialFailureIsCallerVisible
  - testScheduleControllerTransactionRollbackLeavesNoFalseLinkedSchedule
  - testDateChangeCallbackRestoresOAThreadLocalContext

  7. Hardening Recommendations

  - Add explicit lifecycle state to OADateChangeController: NEW, RUNNING, STOPPING, STOPPED.
  - Store notifier thread in the static field and expose a controlled stop/reset method for tests/runtime shutdown.
  - Wrap each date-change callback in exception isolation with logging/aggregation.
  - Clarify weak callback registration semantics or add explicit unregister with strong registration handles.
  - Add optional diagnostics: callback count, live callback count, notifier thread status, last fire time, last
    exception.
  - Separate OASchedule stateful cursor APIs from Iterable; use per-iterator traversal state.
  - Define interval semantics once: closed [begin,end] or half-open [begin,end).
  - Enforce scheduler and plan begin/end windows consistently.
  - Add guard checks in OASchedulerController.set() for reversed ranges, unresolved type, null link hub path, and
    invalid path shape.
  - Centralize schedule-slot matching so cache, datasource, and in-object duplicate checks use the exact same fields
    and conversions.
  - Close OASelect in finally.
  - Document thread-safety: likely single-thread-owned unless caller synchronizes externally.
  - For controller mutations that create schedule/link objects, recommend caller transaction usage or add visible
    failure diagnostics for partial-progress paths.

  8. Open Questions

  - Is OADateChangeController intended to live for the entire JVM, or should OARuntime shutdown stop it?
  - Should date-change callbacks be strong registrations with unregister, or weak observer hooks?
  - Are OASchedule intervals intended to be closed or half-open?
  - Is OASchedulerPlan(OADateTime) intended to mean “24 hours from dt” or “until next midnight”?
  - Is OASchedule.next()/nextEmpty() intended as a legacy cursor API independent from Iterable?
  - Should OAScheduler.isAvailable() return true or false when it has no plans?
  - For OASchedulerController, are type 1 and 3 shared timeslots supposed to reuse existing schedule objects globally?
  - Should OASchedulerController.set() silently return for null inputs/no AO/no detail hub, or should production code
    get visible failure?
  - Should schedule assignment always run inside OATransaction when it can create both schedule and link objects?
  - What OAThreadLocal/context baseline should the date-change notifier establish before invoking callbacks?


*/

