package com.viaoa.scheduler;

import java.util.ArrayList;

import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

/**
 * Top level component/controller for gathering one or more schedule plans. 
 * 
 * @author vvia
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
