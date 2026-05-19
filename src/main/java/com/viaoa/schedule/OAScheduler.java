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
package com.viaoa.schedule;

import java.util.ArrayList;

import com.viaoa.datetime.OADateTime;
import com.viaoa.object.OAObject;

/*qqqqqqqqqqqqq
CODEX

3. src/main/java/com/viaoa/schedule/OASchedule.java:320 iterator
     Bug/risk: hasNext() returns true after the last real range has already been returned because it only checks !bEol
     && tree.size() > 0.
     Production impact: enhanced-for iteration receives a null element. isRangeAdded() will throw NPE when a date is
     not found before the iterator passes the last range.
     Severity: High
     Minimal hardening: use the TreeSet iterator directly, or make hasNext() check whether dtrLast == null ?
     tree.first() != null : tree.higher(dtrLast) != null.
  4. src/main/java/com/viaoa/schedule/OASchedule.java:275 next / src/main/java/com/viaoa/schedule/OASchedule.java:320
     iterator
     Bug/risk: iteration state is stored on the schedule object itself (dtrLast, bEol), not per iterator.
     Production impact: nested iteration, concurrent iteration, or calling isRangeAdded() while iterating corrupts
     traversal position and can skip ranges or return wrong gaps.
     Severity: Medium
     Minimal hardening: make iteration state local to iterator instances; keep next()/nextEmpty() documented as
     stateful cursor APIs if still needed.
  5. src/main/java/com/viaoa/schedule/OASchedule.java:70 clear vs src/main/java/com/viaoa/schedule/OASchedule.java:351
     isRangeAdded
     Bug/risk: range boundary semantics conflict. isRangeAdded() treats end as inclusive (dt <= end), while
     clear(begin,end) treats the clear interval like half-open at boundaries (dtr.end <= clear.begin and dtr.begin >=
     clear.end are skipped).
     Production impact: clearing [11,12] from a range [10,11] leaves time 11 still scheduled even though availability
     checks say it is inside the range.
     Severity: Medium
     Minimal hardening: define closed vs half-open interval semantics and apply consistently in add/clear/
     isRangeAdded.
  6. src/main/java/com/viaoa/schedule/OAScheduler.java:128 isAvailable
     Bug/risk: the scheduler-level dtBegin/dtEnd window is never enforced.
     Production impact: a scheduler with no plans returns true for any date/time, including outside its declared
     evaluation window. With plans, availability is delegated entirely to plans and ignores the top-level boundary
     documented by the class.
     Severity: Medium
     Minimal hardening: return false for null/outside dtBegin/dtEnd before evaluating plans.


=========
 Invariant Risk Areas

  - Date-change notifier must have exactly one live worker per JVM/runtime.
  - Observer callbacks must not kill infrastructure delivery.
  - Schedule iteration must be per-iterator, not shared mutable schedule state.
  - Interval boundary semantics must be explicit and consistent.
  - Availability must respect every declared enclosing window.
  - Schedule assignment must fail visibly when relationship metadata is invalid.
  - Datasource/select resources opened by schedule controllers must be closed.

  Top Production Risks

  - Duplicate date-change callback execution from multiple daemon notifier threads.
  - Scheduler availability checks throwing NPE or returning wrong results because of broken iterator behavior.
  - Duplicate schedule entries in separate date/time mode.
  - Silent schedule assignment failure when relationship type is unresolved.
  - Resource leakage from unclosed OASelect under repeated scheduling operations.

  Hardening Recommendations

  - Add a small lifecycle state to OADateChangeController: started/stopping/stopped, static thread assignment, and
    per-callback exception isolation.
  - Replace OASchedule.iterator() with a real independent iterator over tree.
  - Document and enforce interval semantics as either closed [begin,end] or half-open [begin,end).
  - Add guard assertions in OASchedulerController.set() for valid type, non-null expected hubs, and valid path shape.
  - Add focused tests for: date-change duplicate thread prevention, callback exception isolation, isRangeAdded()
    misses, nested iteration, boundary clear behavior, separate date/time duplicate detection, and unresolved
    relationship type failure.



*/

/**
 * Aggregates one or more {@link OASchedulerPlan} instances for a particular
 * {@link com.viaoa.object.OAObject} and provides top-level scheduling logic.
 * Callers set an overall begin/end window and add plans that represent
 * different availability rules. <p>
 *
 * The {@link #isAvailable(com.viaoa.datetime.OADateTime)} method returns true only
 * when all registered plans consider the specified date–time available.
 */
public class OAScheduler<T extends OAObject> {

	/**
	 * The overall begin and end timestamps that define the scheduler’s evaluation
	 * window. Availability checks use this window as the top-level boundary.
	 */
    private OADateTime dtBegin, dtEnd;

    /**
     * List of scheduler plans used to evaluate availability rules. Initialized
     * lazily when plans are added.
     */
    private ArrayList<OASchedulerPlan<T>> alSchedulePlan;
    
    /**
     * The OAObject instance for which scheduling availability is being evaluated.
     */
    private T objSearch;
    
    /**
     * Creates a scheduler instance for a specific object and date–time window.
     *
     * @param objSearch the object whose availability is being evaluated
     * @param dtBegin the begin timestamp
     * @param dtEnd the end timestamp
     */
    public OAScheduler(T objSearch, OADateTime dtBegin, OADateTime dtEnd) {
        this.objSearch = objSearch;
        this.dtBegin = dtBegin;
        this.dtEnd = dtEnd;
    }
    
    /**
     * Returns the object associated with this scheduler.
     *
     * @return the search object
     */
    public T getSearchObject() {
        return objSearch;
    }
    
    /**
     * Returns the begin timestamp of this scheduler's evaluation window.
     *
     * @return the begin timestamp
     */
    public OADateTime getBegin() {
        return dtBegin;
    }
    
    /**
     * Returns the end timestamp of this scheduler's evaluation window.
     *
     * @return the end timestamp
     */
    public OADateTime getEnd() {
        return dtEnd;
    }
    
    /**
     * Adds a scheduler plan to the list of plans used for availability checks.
     * Ignores null values and initializes the list on first use.
     *
     * @param schPlan the plan to add
     */
    public void add(OASchedulerPlan schPlan) {
        if (schPlan == null) return;
        if (alSchedulePlan == null) alSchedulePlan = new ArrayList<>();
        alSchedulePlan.add(schPlan);
    }
    
    /**
     * Performs plan calculation. Currently a placeholder with no implementation.
     */
    public void calculate() {
    }
    
    /**
     * Returns the list of scheduler plans associated with this scheduler.
     * The list is lazily initialized and never null.
     *
     * @return the list of scheduler plans
     */
    public ArrayList<OASchedulerPlan<T>> getSchedulePlans() {
        if (alSchedulePlan == null) alSchedulePlan = new ArrayList<>();
        return alSchedulePlan;
    }
    
    /**
     * Determines whether the specified timestamp is available according to all
     * registered scheduler plans. Returns false if any plan marks the timestamp
     * as unavailable.
     *
     * @param dt the timestamp to check
     * @return true if all plans consider the timestamp available; false otherwise
     */
    public boolean isAvailable(OADateTime dt) {
        for (OASchedulerPlan sp : getSchedulePlans()) {
            if (!sp.isAvailable(dt)) return false;
        }
        return true;
    }
}
