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

    private OADateTime dtBegin, dtEnd;
    private ArrayList<OASchedulerPlan<T>> alSchedulePlan;
    private T objSearch;
    
    /**
     * Set the begin and end datetime.
     */
    public OAScheduler(T objSearch, OADateTime dtBegin, OADateTime dtEnd) {
        this.objSearch = objSearch;
        this.dtBegin = dtBegin;
        this.dtEnd = dtEnd;
    }
    
    public T getSearchObject() {
        return objSearch;
    }
    
    public OADateTime getBegin() {
        return dtBegin;
    }
    public OADateTime getEnd() {
        return dtEnd;
    }
    
    public void add(OASchedulerPlan schPlan) {
        if (schPlan == null) return;
        if (alSchedulePlan == null) alSchedulePlan = new ArrayList<>();
        alSchedulePlan.add(schPlan);
    }
    
    public void calculate() {
    }
    
    public ArrayList<OASchedulerPlan<T>> getSchedulePlans() {
        if (alSchedulePlan == null) alSchedulePlan = new ArrayList<>();
        return alSchedulePlan;
    }
    
    public boolean isAvailable(OADateTime dt) {
        for (OASchedulerPlan sp : getSchedulePlans()) {
            if (!sp.isAvailable(dt)) return false;
        }
        return true;
    }
}
