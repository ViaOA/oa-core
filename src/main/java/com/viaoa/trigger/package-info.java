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


1. Package Summary

  com.viaoa.trigger defines OA’s model-level trigger contract. A trigger watches one or more property paths from a
  root OAObject class and invokes an OATriggerListener when object or Hub events occur along those paths.

  It is not a simple Hub listener layer. It depends on metadata/path resolution in OAObjectInfo, graph routing through
  OATriggerService, object/Hub event publication, reverse-path lookup, datasource fallback, object cache scanning,
  calculated-property dependency expansion, and runtime thread-local state.

  Primary classes:

  - src/main/java/com/viaoa/trigger/OATrigger.java:66: trigger definition.
  - src/main/java/com/viaoa/trigger/OATriggerListener.java:30: callback contract.
  - src/main/java/com/viaoa/trigger/OATriggerMethodListener.java:118: reflection-backed listener for @OATriggerMethod.

  Key enforcement points outside package:

  - src/main/java/com/viaoa/graph/service/OATriggerService.java:154: graph/runtime trigger registration and async
    execution.
  - src/main/java/com/viaoa/metadata/OAObjectInfo.java:1518: trigger registration, path indexing, dispatch, recursion
    guard.
  - src/main/java/com/viaoa/graph/service/object/OAObjectEventService.java:670: object-property trigger source.
  - src/main/java/com/viaoa/graph/service/hub/HubEventService.java:150: Hub membership trigger source.

  2. Core Concepts

  - Trigger: An OATrigger root class, path list, listener, and execution flags.
  - Trigger listener/callback: OATriggerListener.onTrigger(root, hubEvent, propertyPathFromRoot).
  - Class-level trigger: Trigger scoped to rootClass.
  - Property/path-level trigger: Metadata registrations created for every listened segment of each trigger path.
  - Dependent property/path tracking: propertyPaths plus calculated-property dependent triggers created in
    OAObjectInfo._addTrigger.
  - Registration: OATriggerService.addTrigger routes to root class OAObjectInfo.createTrigger.
  - Deregistration: OATriggerService.removeTrigger routes to OAObjectInfo.removeTrigger.
  - Firing/execution: Object/Hub events call OAObjectInfo.onChange, which dispatches inline or through
    OATriggerService.runTrigger.
  - Recursion/reentrancy: Guarded by OAThreadLocalService.getRecursiveTriggerCount.
  - Ordering: Inline triggers follow current listener list order; background triggers currently use a shared executor.
  - Enable/disable/pause: No explicit trigger pause mechanism exists in this package.
  - Hub/OAObject interaction: Hub events and object property changes are converted into HubEvent and passed through
    OAObjectInfo.onChange.

  3. Invariants

  A. Registration and Lifecycle

  1. TRIGGER-REG-001: A trigger registration must become visible only after all property paths and dependent
     calculated-property paths are valid and committed.
     Why: Partial registration causes triggers to fire after caller-visible registration failure.
     Locations: OAObjectInfo.createTrigger, _addTrigger.
     Confidence: Medium.
     Gap: Current code mutates as it validates; CODEX noted.
  2. TRIGGER-REG-002: A registered trigger’s property path set must be immutable for the lifetime of the registration.
     Why: Removal traverses current paths; mutation can leave stale registrations.
     Locations: OATrigger constructors/getters, OAObjectInfo.removeTrigger.
     Confidence: Low.
     Gap: Arrays are externally mutable; CODEX noted.
  3. TRIGGER-REG-003: Deregistration must remove all direct and dependent trigger registrations and must report
     whether removal actually occurred.
     Why: Close/remove must prevent future eligible execution.
     Locations: OATriggerService.removeTrigger, OAObjectInfo.removeTrigger, _removeTrigger.
     Confidence: Medium.
     Gap: Service returns true for any non-null trigger; CODEX noted.
  4. TRIGGER-REG-004: Registration dedupe must not collapse distinct trigger contracts.
     Why: Same listener/path with different flags can have different runtime semantics.
     Locations: OAObjectInfo._addTrigger.
     Confidence: Low.
     Gap: Dedupe ignores trigger instance and flags; CODEX noted.

  B. Class / Property / Path Matching

  5. TRIGGER-PATH-001: Each trigger property path must be resolved from rootClass using OA metadata/path semantics.
     Why: Wrong path resolution means missed or wrong trigger execution.
     Locations: OAObjectInfo.createTrigger, OAPath.
     Confidence: Medium.
     Gap: Empty paths are skipped; intended semantics should be documented.
  6. TRIGGER-PATH-002: Trigger dispatch must match the changed property case-insensitively but semantically exactly.
     Why: Broad or narrow matching creates false positives/negatives.
     Locations: hmTriggerInfo keyed by uppercase property; OAObjectInfo.onChange.
     Confidence: High.
     Gap: Locale behavior of toUpperCase() is not explicit.
  7. TRIGGER-PATH-003: Reverse-path resolution must find each affected root object exactly once per event.
     Why: Duplicate root execution corrupts derived state or business callbacks.
     Locations: OAObjectInfo._runOnChange2, OAFinder, GUID de-dupe set.
     Confidence: Medium.
     Gap: Fallback paths and no-root paths have known edge risks.

  C. Trigger Firing

  8. TRIGGER-FIRE-001: A committed eligible OAObject or Hub event must not silently miss a required trigger.
     Why: Triggers drive calculated values, cache filters, Hub updates, and business rules.
     Locations: OAObjectEventService.firePropertyChange, HubEventService, OAObjectInfo.onChange.
     Confidence: Medium.
     Gap: Null-root chained trigger path was CODEX-noted.
  9. TRIGGER-FIRE-002: If root object is known, listener must receive that root; if unknown, listener must receive
     null plus enough event/path context to resolve affected roots.
     Why: Listener fallback depends on this distinction.
     Locations: OAObjectInfo._runOnChange2, OATriggerMethodListener.onTrigger.
     Confidence: Medium.
     Gap: fromObject == null handling needs stronger invariant.
  10. TRIGGER-FIRE-003: Trigger execution must respect serverSideOnly, onlyUseLoadedData, and background flags1.
     Package Summary

  com.viaoa.trigger defines OA’s model-level trigger contract. A trigger binds a root OAObject class plus one or more
  property paths to an OATriggerListener. Runtime registration and dispatch are implemented mostly through
  OAObjectInfo and OATriggerService, while OATriggerMethodListener adapts annotated methods into trigger callbacks.

  In OA 4.0 terms, triggers are lower-level runtime infrastructure for cross-object dependency reactions: calculated
  property propagation, cache/hub filter refresh, derived-state updates, and class/path-level business callbacks. They
  are deeper and more expensive than direct Hub listeners because they can reverse-walk object graph paths or scan/
  select root objects when the affected root cannot be resolved directly.

  2. Core Concepts

  - trigger: OATrigger, a root class, dependent property path list, listener, and execution flags.
  - trigger listener/callback: OATriggerListener.onTrigger, invoked with root object, HubEvent, and path from root to
    event source.
  - class-level trigger: trigger rooted at an OAObject class, registered through OATriggerService.addTrigger.
  - property/path-level trigger: trigger expanded by OAObjectInfo.createTrigger into per-property TriggerInfo entries
    along the path.
  - dependent property/path tracking: trigger property paths and calculated-property dependent properties used to
    decide what events should fire.
  - registration: OATriggerService.addTrigger delegates to OAObjectInfo.createTrigger.
  - deregistration: OATriggerService.removeTrigger delegates to OAObjectInfo.removeTrigger.
  - firing/execution: object and Hub event services call OAObjectInfo.onChange, which resolves roots and invokes
    listeners.
  - recursion/reentrancy: OAThreadLocalService.getRecursiveTriggerCount limits recursive trigger chains.
  - ordering: inline triggers follow current CopyOnWriteArrayList iteration order; background triggers use a shared
    executor.
  - enable/disable or pause behavior: no explicit trigger-level pause/enable API is present.
  - Hub/OAObject event interaction: property changes and detail Hub add/remove/insert events are converted to HubEvent
    and routed through OAObjectInfo.onChange.

  3. Invariants

  A. Registration And Lifecycle

  TRIGGER-REG-001: A trigger registration must not become visible until all declared property paths and dependent
  calculated paths are valid and registered.
  Why: partial registration creates callbacks for a trigger the caller believes failed.
  Locations: OATriggerService.addTrigger, OAObjectInfo.createTrigger, OAObjectInfo._addTrigger.
  Confidence: Medium.
  Gap: current code mutates as it walks paths; CODEX notes partial-commit risk.

  TRIGGER-REG-002: Registered trigger definitions must be immutable for routing purposes.
  Why: removal and dispatch depend on the same root class/property paths used during registration.
  Locations: OATrigger constructors/getters, OAObjectInfo.removeTrigger.
  Confidence: Low.
  Gap: arrays are currently exposed/mutable; CODEX notes stale unregister risk.

  TRIGGER-REG-003: Registering the same logical trigger twice must either be idempotent by documented key or create
  independent registrations.
  Why: silent collapse causes missed trigger execution; duplicate registration causes duplicate execution.
  Locations: OAObjectInfo._addTrigger.
  Confidence: Low.
  Gap: dedupe key ignores trigger instance and execution flags.

  TRIGGER-LIFE-001: Removing a trigger must prevent all future executions caused by that trigger, including path-
  segment and calculated dependent triggers.
  Why: stale registrations leak memory and fire closed/invalid listeners.
  Locations: OATriggerService.removeTrigger, OAObjectInfo.removeTrigger, OAObjectInfo._removeTrigger.
  Confidence: Medium.
  Gap: return value does not prove removal; mutable paths and shared calc triggers weaken this.

  B. Class / Property / Path Matching

  TRIGGER-PATH-001: Property-path matching must be metadata-consistent and case-insensitive only where OA metadata
  says property names are case-insensitive.
  Why: wrong path expansion causes missed or wrong-object callbacks.
  Locations: OAObjectInfo.createTrigger, OAPath, OAObjectInfo._addTrigger.
  Confidence: Medium.
  Gap: behavior depends on OAPath; invalid paths fail during registration.

  TRIGGER-PATH-002: Each link segment in a trigger path must register against the correct intermediate OAObjectInfo.
  Why: Hub/detail and nested object changes must fire root triggers.
  Locations: OAObjectInfo.createTrigger path loop.
  Confidence: High.
  Gap: private/no-reverse links require fallback behavior.

  TRIGGER-PATH-003: Reverse-path state must distinguish “cannot reverse” from “reverse can work but data is not
  loaded.”
  Why: fallback selection/scanning differs from direct reverse traversal.
  Locations: TriggerInfo.bNoReverseFinder, bReverseHasMany, OAObjectInfo._runOnChange2.
  Confidence: Medium.
  Gap: catch blocks can conflate listener failure with reverse traversal failure.

  C. Trigger Firing

  TRIGGER-FIRE-001: Every committed eligible OAObject property or detail-Hub event must fire all matching triggers
  exactly according to registration semantics.
  Why: triggers maintain derived state and model-level dependency reactions.
  Locations: OAObjectEventService.firePropertyChange, HubEventService, OAObjectInfo.onChange.
  Confidence: Medium.
  Gap: noted no-root chained trigger path can miss downstream reverse triggers.

  TRIGGER-FIRE-002: If the changed root object can be resolved, listener invocation must receive the concrete root
  object.
  Why: direct callbacks avoid expensive scans and preserve exact affected-object semantics.
  Locations: OAObjectInfo._runOnChange2, OATriggerListener.onTrigger.
  Confidence: High.
  Gap: reverse traversal must not silently skip duplicate roots except by identity.

  TRIGGER-FIRE-003: If the changed root cannot be resolved, listener invocation must receive objRoot == null and
  enough event/path context to find affected roots.
  Why: listeners like OATriggerMethodListener rely on fallback scanning/selecting.
  Locations: OAObjectInfo._runOnChange2, OATriggerMethodListener.onTrigger.
  Confidence: Medium.
  Gap: fallback behavior differs by cache/select path.

  D. Ordering And Determinism

  TRIGGER-ORDER-001: Inline trigger execution must be deterministic for a stable registration set.
  Why: derived state and event side effects depend on repeatable order.
  Locations: OAObjectInfo.onChange, CopyOnWriteArrayList<TriggerInfo>.
  Confidence: Medium.
  Gap: concurrent registration can change future order.

  TRIGGER-ORDER-002: Background trigger ordering must be explicitly contracted.
  Why: current shared executor can reorder event processing.
  Locations: OATriggerService.runTrigger, OATriggerService.Holder.createExecutor.
  Confidence: Low.
  Gap: CODEX notes multi-thread executor ordering risk.

  E. Thread-Safety / Concurrency

  TRIGGER-CONC-001: Trigger registration and dedupe must be atomic per object-info/listen-property.
  Why: concurrent add can create duplicate registrations.
  Locations: OAObjectInfo._addTrigger.
  Confidence: Low.
  Gap: current dedupe/add is not synchronized as one operation.

  TRIGGER-CONC-002: Trigger removal must be safe while firing.
  Why: listeners can close filters or remove triggers during callbacks.
  Locations: CopyOnWriteArrayList, OAObjectInfo.onChange, _removeTrigger.
  Confidence: Medium.
  Gap: copy-on-write helps iteration, but stale already-scheduled background work can still run.

  TRIGGER-TL-001: Any ThreadLocal state changed by trigger dispatch must be restored in finally.
  Why: loading/context/sendSync state leaks can corrupt sync and event behavior.
  Locations: OAObjectInfo._runOnChange1, OATriggerService.TriggerRunnable.run.
  Confidence: Medium.
  Gap: context restored to null, not previous context; verify intended baseline.

  F. Reentrancy / Recursion / Event-Storm

  TRIGGER-RECURSE-001: Recursive trigger chains must be bounded and fail visibly when the bound is exceeded.
  Why: self-triggering calculations can create infinite loops.
  Locations: OAObjectInfo.onChange, OAThreadLocalService.getRecursiveTriggerCount.
  Confidence: Medium.
  Gap: async trigger execution currently escapes the scheduling-thread counter.

  TRIGGER-RECURSE-002: Recursion suppression must suppress only invalid recursion, not legitimate downstream
  dependency propagation.
  Why: calculated-property chains and Hub refresh triggers must still fire.
  Locations: OAObjectInfo.onChange, calculated-property trigger creation in _addTrigger.
  Confidence: Medium.
  Gap: no trigger identity/path-specific recursion model is visible.

  G. Object Identity / Cache / Graph

  TRIGGER-GRAPH-001: Trigger registration and execution must route through the graph owning the trigger root class.
  Why: cross-graph registration or dispatch can corrupt runtime ownership.
  Locations: OATriggerService.addTrigger/removeTrigger, OARuntime.graph(trigger.getRootClass()), OAObjectInfo._onChan
  ge.
  Confidence: Medium.
  Gap: earlier CODEX comments note graph ownership is implicit by root class.

  TRIGGER-ID-001: Duplicate root callback suppression must be based on stable OA object identity.
  Why: reverse traversal can find the same root through multiple paths.
  Locations: OAObjectInfo._runOnChange2, GUID hash set in OAFinder.onFound.
  Confidence: High.
  Gap: depends on valid object key/GUID service.

  TRIGGER-CACHE-001: Loaded-data-only triggers must never force datasource loading.
  Why: callers use onlyUseLoadedData to bound cost and avoid side effects.
  Locations: OATrigger.getOnlyUseLoadedData, OATriggerMethodListener.onTrigger, OAFinder.setUseOnlyLoadedData.
  Confidence: Medium.
  Gap: fallback behavior should be tested with unloaded Hubs/references.

  H. Error Handling

  TRIGGER-ERR-001: Trigger listener failure must be observable and must not silently appear successful.
  Why: failed derived updates/business callbacks leave stale state.
  Locations: OATriggerListener.onTrigger, OAObjectInfo._runOnChange2, OATriggerMethodListener.onTrigger,
  OATriggerService.runTrigger.
  Confidence: Low.
  Gap: CODEX notes cache fallback swallows exceptions and submit hides async failures.

  TRIGGER-ERR-002: Traversal failure must not be confused with listener failure.
  Why: fallback retry can duplicate side effects or mask original cause.
  Locations: OAObjectInfo._runOnChange2 catch/fallback paths.
  Confidence: Low.
  Gap: CODEX notes broad catch around finder.find.

  TRIGGER-ERR-003: Datasource/select resources opened during trigger resolution must be closed on success and failure.
  Why: trigger failures should not leak runtime resources.
  Locations: OATriggerMethodListener.onTrigger.
  Confidence: Low.
  Gap: CODEX notes OASelect close is missing.

  I. Performance / Bounded-Cost

  TRIGGER-PERF-001: Trigger cost must be bounded by explicit mode: direct reverse traversal, loaded-data scan,
  datasource select, or background execution.
  Why: triggers are deeper than Hub listeners and can be expensive.
  Locations: OATrigger.onlyUseLoadedData, OATrigger.useBackgroundThread, OATriggerMethodListener.
  Confidence: Medium.
  Gap: background executor queue is effectively unbounded.

  TRIGGER-PERF-002: Fallback scans/selects must preserve correctness while avoiding unnecessary full scans when
  queryable context exists.
  Why: expensive fallback paths can affect production object graphs.
  Locations: OATriggerMethodListener.onTrigger cache visit/select branches.
  Confidence: Medium.
  Gap: CODEX notes wrong query parameter for nested Hub membership.

  J. Integration

  TRIGGER-INT-001: OAObject property changes must invoke triggers only after the object/property transition is
  visible.
  Why: callbacks must observe committed object state.
  Locations: OAObjectEventService.firePropertyChange, trigger call after update/link handling.
  Confidence: Medium.
  Gap: exact event ordering should be locked by tests.

  TRIGGER-INT-002: Hub add/remove/insert/removeAll events for detail Hubs must be converted into trigger events on the
  master property.
  Why: property-path triggers depend on Hub membership changes.
  Locations: HubEventService trigger calls.
  Confidence: Medium.
  Gap: add/remove/insert coverage needed.

  TRIGGER-INT-003: Calculated-property triggers must keep dependent-property chains alive as long as any trigger
  depends on the calculated property.
  Why: removing one trigger must not disable another.
  Locations: OAObjectInfo._addTrigger, dependent trigger creation/removal.
  Confidence: Low.
  Gap: CODEX notes ownership/reference-count issue.

  TRIGGER-INT-004: Annotated trigger methods must apply to the concrete OAObjectInfo being initialized, including
  inherited methods.
  Why: subclass object changes should fire inherited model callbacks.
  Locations: OAObjectAnnotationService.update2, _update2, OATriggerMethodListener.
  Confidence: Low.
  Gap: CODEX notes superclass registration issue.

  4. Failure Modes

  - Missed trigger: no-root reverse path, wrong datasource query parameter, inherited annotation registered to
    superclass.
  - Duplicate trigger: concurrent registration or listener/path dedupe ambiguity.
  - Wrong object/class match: graph/root class mismatch or wrong fallback select value.
  - Stale dependent path: externally mutable propertyPaths.
  - Infinite recursion: async trigger work escaping recursive counter.
  - Event storm: self-triggering background callbacks with unbounded queue.
  - Out-of-order execution: multi-thread background executor.
  - Memory leak: stale HubFilter-created triggers or failed unregister.
  - Trigger firing during load/replay: ThreadLocal loading/sendSync semantics must be preserved.
  - Trigger incorrectly suppressed: server/client graph lookup from null or wrong graph.
  - Cross-graph contamination: trigger service routes by root class rather than owning graph instance.
  - Concurrency race: non-atomic _addTrigger dedupe/add.

  5. Test Recommendations

  Registration/lifecycle:

  - testTriggerRegistrationIsAllOrNothingForMultiplePaths
  - testRemoveTriggerRemovesIntermediateAndDependentRegistrations
  - testMutablePropertyPathArrayCannotLeaveStaleRegistration
  - testRemoveUnregisteredTriggerReportsFalseOrNoRemovedCount

  Path matching:

  - testNestedPropertyPathFiresForLeafPropertyChange
  - testDetailHubAddFiresRootPathTrigger
  - testNoReversePathFallsBackToNullRootCallback

  Firing/order:

  - testInlineTriggersFireInRegistrationOrder
  - testBackgroundTriggerOrderingContract
  - testMultipleRootsFoundOnceByGuid

  Concurrency:

  - testConcurrentSameTriggerRegistrationDoesNotDuplicate
  - testRemoveDuringTriggerDispatchDoesNotCorruptIteration
  - testScheduledTriggerAfterRemoveHasDocumentedBehavior

  Recursion:

  - testInlineRecursiveTriggerStopsAtLimit
  - testAsyncRecursiveTriggerDoesNotBypassLimit
  - testLegitimateCalculatedChainNotSuppressed

  Error handling:

  - testTriggerListenerExceptionPropagatesInline
  - testTriggerMethodCacheFallbackExceptionVisible
  - testAsyncTriggerExceptionObservable
  - testSelectClosedWhenTriggerMethodThrows

  Integration:

  - testInheritedTriggerMethodRegisteredForSubclass
  - testCalculatedPropertyDependencyTriggerReferenceCount
  - testServerSideOnlyTriggerSkippedOnClientRunsOnServerAndSingleUserAsContracted
  - testTriggerThreadLocalSendSyncMessagesRestored

  6. Hardening Recommendations

  - Defensively copy OATrigger.propertyPaths and dependent trigger arrays.
  - Add a trigger registration plan/commit step or rollback on registration failure.
  - Make _addTrigger atomic per listen property.
  - Replace boolean removeTrigger false-success with removed count.
  - Separate traversal exceptions from listener exceptions.
  - Add explicit async failure logging/hook instead of discarded Future.
  - Add optional trigger diagnostics: trigger id, root class, listen property, path, execution mode, owner graph.
  - Add bounded queue or runtime-managed executor lifecycle for background triggers.
  - Document whether background triggers are ordered or unordered.
  - Add shared/ref-counted calculated-property dependency triggers.
  - Add comments near no-root fallback semantics: objRoot == null means listener must resolve affected roots.

  7. Open Questions

  - Are background triggers required to preserve event order, or are they explicitly unordered?
  - Should serverSideOnly run in SingleUser mode? Current isClient check implies yes, but the contract should say it.
  - Should removeTrigger guarantee that already-scheduled background trigger work will not run?
  - Is OATrigger intended to be immutable after registration?
  - Should duplicate listener/path registrations be independent, idempotent, or rejected?
  - Should trigger registration be graph-owned by the caller graph or always routed by root class?
  - Is objRoot == null a first-class listener contract for all listeners, or only for internal method/cache listeners?
  - Should trigger listener exceptions abort the originating object/Hub event, or only be reported asynchronously for
    background triggers?
  - What is the intended behavior during load, sync replay, and replication apply: suppress triggers, run loaded-only
    triggers, or run all non-side-effect triggers?

*/



