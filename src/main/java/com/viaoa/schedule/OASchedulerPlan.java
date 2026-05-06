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

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;

/**
 * Represents a complete scheduling plan for a resource. The plan defines
 * multiple categories of date–time ranges such as open, preferred, blocked,
 * and already scheduled ranges. Each category is stored as an
 * {@link OASchedule}, allowing the plan to express both hard and soft
 * availability rules. <p>
 *
 * The {@link #isAvailable(com.viaoa.datetime.OADateTime)} method applies these
 * rules to determine whether the resource is available at a particular
 * date–time.
 */
public class OASchedulerPlan<R> {
    
	/**
	 * The overall begin and end timestamps that bound the scheduling plan.
	 * Availability checks require the requested time to fall within this window.
	 */
	private OADateTime dtBegin, dtEnd;

	/**
	 * Optional list of child scheduler plans. Currently unused but reserved for
	 * composite or hierarchical scheduling structures.
	 */
    private ArrayList<OASchedulerPlan> alSchedulePlan;

    /**
     * Schedule representing hard open/available ranges for the resource.
     */
    private OASchedule<R> schOpen;

    /**
     * Schedule representing soft or flexible open ranges that are available
     * only when needed.
     */
    private OASchedule<R> schOpenSoft;
    
    /**
     * Schedule representing preferred available date–time ranges.
     */
    private OASchedule<R> schPreferred;

    /**
     * Schedule representing preferred but flexible ranges that may adjust
     * when necessary.
     */
    private OASchedule<R> schPreferredSoft;
    
    /**
     * Schedule representing hard blocked (unavailable) date–time ranges.
     */
    private OASchedule<R> schBlocked;

    /**
     * Schedule representing soft blocked ranges that may be available only in
     * emergency or override scenarios.
     */
    private OASchedule<R> schBlockedSoft;
    
    /**
     * Schedule representing ranges already scheduled for the resource.
     */
    private OASchedule<R> schScheduled;
    
    
    /**
     * Creates a scheduling plan covering a default one-day range starting today.
     */
    public OASchedulerPlan() {
        this(new OADate(), new OADate().addDays(1));
    }

    /**
     * Creates a scheduling plan beginning at the supplied date and extending
     * one day forward.
     *
     * @param date the starting date for the plan; if null, today's date is used
     */
    public OASchedulerPlan(OADate date) {
        this(date != null ? date : new OADate(), date != null ? date.addDays(1) : new OADate().addDays(1));
    }
    
    /**
     * Creates a scheduling plan starting at the supplied date–time and extending
     * one day forward.
     *
     * @param dt the starting date–time; if null, the current time is used
     */
    public OASchedulerPlan(OADateTime dt) {
        this(dt != null ? dt : new OADateTime(), dt != null ? new OADate(dt).addDays(1) : new OADate().addDays(1));
    }
    
    /**
     * Creates a scheduling plan using explicit begin and end date–time values.
     * The supplied values are copied into new OADateTime instances.
     *
     * @param dtBegin the start of the plan
     * @param dtEnd the end of the plan
     */
    public OASchedulerPlan(OADateTime dtBegin, OADateTime dtEnd) {
        this.dtBegin = new OADateTime(dtBegin);
        this.dtEnd = new OADateTime(dtEnd);
    }

    /**
     * Returns the beginning of the plan's active date–time window.
     *
     * @return the begin timestamp
     */
    public OADateTime getBegin() {
        return dtBegin;
    }
    
    /**
     * Returns the end of the plan's active date–time window.
     *
     * @return the end timestamp
     */
    public OADateTime getEnd() {
        return dtEnd;
    }
    
    /**
     * Returns the hard-open schedule, initializing it on first access.
     *
     * @return the open schedule
     */
    public OASchedule<R> getOpenSchedule() {
        if (schOpen == null) {
            schOpen = new OASchedule();
        }
        return schOpen;
    }
    
    /**
     * Returns the soft-open schedule, initializing it on first access.
     *
     * @return the soft open schedule
     */
    public OASchedule<R> getOpenSoftSchedule() {
        if (schOpenSoft == null) {
            schOpenSoft = new OASchedule();
        }
        return schOpenSoft;
    }
    
    /**
     * Returns the preferred schedule, initializing it on first access.
     *
     * @return the preferred schedule
     */
    public OASchedule<R> getPreferredSchedule() {
        if (schPreferred == null) {
            schPreferred = new OASchedule();
        }
        return schPreferred;
    }
    
    /**
     * Returns the flexible preferred schedule, initializing it on first access.
     *
     * @return the soft preferred schedule
     */
    public OASchedule<R> getPreferredSoftSchedule() {
        if (schPreferredSoft == null) {
            schPreferredSoft = new OASchedule();
        }
        return schPreferredSoft;
    }
    
    /**
     * Returns the hard-blocked schedule, initializing it on first access.
     *
     * @return the blocked schedule
     */
    public OASchedule<R> getBlockedSchedule() {
        if (schBlocked == null) {
            schBlocked = new OASchedule();
        }
        return schBlocked;
    }
    
    /**
     * Returns the soft-blocked schedule, initializing it on first access. These
     * ranges indicate times that are normally unavailable but may be overridden
     * under special circumstances.
     *
     * @return the soft blocked schedule
     */
    public OASchedule<R> getBlockedSoftSchedule() {
        if (schBlockedSoft == null) {
            schBlockedSoft = new OASchedule();
        }
        return schBlockedSoft;
    }
    
    /**
     * Returns the ranges in which the resource is already scheduled. The schedule
     * is initialized on first access.
     *
     * @return the scheduled ranges
     */
    public OASchedule<R> getScheduledSchedule() {
        if (schScheduled == null) {
            schScheduled = new OASchedule();
        }
        return schScheduled;
    }
    
    /**
     * Determines whether the resource is available at the specified date–time.
     * Enforces the following rules:
     * <ul>
     *   <li>dt must fall within the plan's begin/end window.</li>
     *   <li>dt must be inside an open or soft-open range.</li>
     *   <li>dt must not be inside any blocked or soft-blocked range.</li>
     *   <li>dt must not be inside a previously scheduled range.</li>
     * </ul>
     *
     * @param dt the date–time to test; null returns false
     * @return true if available, false otherwise
     */
    public boolean isAvailable(OADateTime dt) {
        if (dt == null) return false;
        
        if (dt.before(dtBegin)) return false;
        
        if (dt.after(dtEnd)) return false;
        
        OASchedule<R> sch;
        
        boolean b = false;
        sch = getOpenSchedule();
        if (!sch.isRangeAdded(dt)) {
            sch = getOpenSoftSchedule();
            if (!sch.isRangeAdded(dt)) {
                return false;
            }
        }
        
        sch = getBlockedSchedule();
        if (sch.isRangeAdded(dt)) {
            return false;
        }
        
        sch = getBlockedSoftSchedule();
        if (sch.isRangeAdded(dt)) {
            return false;
        }
        
        sch = getScheduledSchedule();
        if (sch.isRangeAdded(dt)) {
            return false;
        }
        
        return true;
    }
}
