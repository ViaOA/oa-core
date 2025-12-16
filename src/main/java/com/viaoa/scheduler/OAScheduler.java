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
package com.viaoa.scheduler;

import java.util.ArrayList;

import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

/**
 * Aggregates one or more {@link OASchedulerPlan} instances for a particular
 * {@link com.viaoa.object.OAObject} and provides top-level scheduling logic.
 * Callers set an overall begin/end window and add plans that represent
 * different availability rules. <p>
 *
 * The {@link #isAvailable(com.viaoa.util.OADateTime)} method returns true only
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
