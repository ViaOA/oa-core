/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Represents a complete scheduling plan for a resource. The plan defines
 * multiple categories of date–time ranges such as open, preferred, blocked,
 * and already scheduled ranges. Each category is stored as an
 * {@link OASchedule}, allowing the plan to express both hard and soft
 * availability rules. <p>
 *
 * The {@link #isAvailable(com.viaoa.util.OADateTime)} method applies these
 * rules to determine whether the resource is available at a particular
 * date–time.
 */
public class OASchedulerPlan<R> {
    private OADateTime dtBegin, dtEnd;
    private ArrayList<OASchedulerPlan> alSchedulePlan;

    /**
     * DateTime ranges that this resource is avail/open.
     */
    private OASchedule<R> schOpen;

    /**
     * DateTime ranges that this resource is willing to extend open/avail only if necessary.
     */
    private OASchedule<R> schOpenSoft;
    
    /**
     * Preferred DateTime ranges for this resouce. 
     */
    private OASchedule<R> schPreferred;

    /**
     * Preferred but flexible DateTime ranges for this resouce. 
     */
    private OASchedule<R> schPreferredSoft;
    
    /**
     * DateTime ranges that this resource has blocked (private)
     */
    private OASchedule<R> schBlocked;

    /**
     * DateTime ranges that this resource has blocked but can be available only if needed (ex: emergency)
     */
    private OASchedule<R> schBlockedSoft;
    
    /**
     * DateTime ranges that this resource is already scheduled.
     */
    private OASchedule<R> schScheduled;
    
    
    public OASchedulerPlan() {
        this(new OADate(), new OADate().addDays(1));
    }
    public OASchedulerPlan(OADate date) {
        this(date != null ? date : new OADate(), date != null ? date.addDays(1) : new OADate().addDays(1));
    }
    public OASchedulerPlan(OADateTime dt) {
        this(dt != null ? dt : new OADateTime(), dt != null ? new OADate(dt).addDays(1) : new OADate().addDays(1));
    }
    
    public OASchedulerPlan(OADateTime dtBegin, OADateTime dtEnd) {
        this.dtBegin = new OADateTime(dtBegin);
        this.dtEnd = new OADateTime(dtEnd);
    }

    public OADateTime getBegin() {
        return dtBegin;
    }
    public OADateTime getEnd() {
        return dtEnd;
    }
    
    public OASchedule<R> getOpenSchedule() {
        if (schOpen == null) {
            schOpen = new OASchedule();
        }
        return schOpen;
    }
    public OASchedule<R> getOpenSoftSchedule() {
        if (schOpenSoft == null) {
            schOpenSoft = new OASchedule();
        }
        return schOpenSoft;
    }
    public OASchedule<R> getPreferredSchedule() {
        if (schPreferred == null) {
            schPreferred = new OASchedule();
        }
        return schPreferred;
    }
    public OASchedule<R> getPreferredSoftSchedule() {
        if (schPreferredSoft == null) {
            schPreferredSoft = new OASchedule();
        }
        return schPreferredSoft;
    }
    public OASchedule<R> getBlockedSchedule() {
        if (schBlocked == null) {
            schBlocked = new OASchedule();
        }
        return schBlocked;
    }
    public OASchedule<R> getBlockedSoftSchedule() {
        if (schBlockedSoft == null) {
            schBlockedSoft = new OASchedule();
        }
        return schBlockedSoft;
    }
    
    public OASchedule<R> getScheduledSchedule() {
        if (schScheduled == null) {
            schScheduled = new OASchedule();
        }
        return schScheduled;
    }
    
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
