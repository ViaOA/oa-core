package com.viaoa.scheduler;

import java.util.ArrayList;

import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;

/**
 * Used to combine a complete datetime ranges for scheduling.
 *  
 * @author vvia
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
}
