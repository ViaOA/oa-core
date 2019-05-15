package com.viaoa.scheduler;

import java.util.ArrayList;

import com.viaoa.util.OACompare;
import com.viaoa.util.OADateTime;

/**
 * Used to manage a datetime range for OASchedule.
 * This is used by OASchedule to collect a range of datetimes and organize them into a dt listing.
 * 
 * Overlapping ranges will result in a parent range being created, that adds the overlapped ranges as children to itself.
 * 
 * @author vvia
 *
 * @param <R> type of object attached to this range.
 */
class OADateTimeRange<R> implements Comparable {
    private OADateTime dtBegin;
    private OADateTime dtEnd;
    
    /**
     * Children that this range has "overlapped".
     */
    private ArrayList<OADateTimeRange<R>> alChildren; 
    private R reference;
    
    public OADateTimeRange(OADateTime dtBegin, OADateTime dtEnd, R ref) {
        this.dtBegin = dtBegin;
        this.dtEnd = dtEnd;
        this.reference = ref;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof OADateTimeRange)) return false;
        
        if (!OACompare.isEqual(((OADateTimeRange)obj).dtBegin, this.dtBegin)) return false;
        if (!OACompare.isEqual(((OADateTimeRange)obj).dtEnd, this.dtEnd)) return false;
        return true;
    }
    
    
    @Override
    public int hashCode() {
        if (dtBegin == null) return 0;
        return dtBegin.hashCode();
    }
    @Override
    public int compareTo(Object obj) {
        if (obj == this) return 0;
        if (obj == null) return 1;
        if (!(obj instanceof OADateTimeRange)) return 1;
        return this.dtBegin.compareTo( ((OADateTimeRange)obj).dtBegin);
        /*
        int x = OACompare.compare(this.dtBegin, ((DateTimeRange)obj).dtBegin);
        if (x != 0) return x;
        
        x = OACompare.compare(this.dtEnd, ((DateTimeRange)obj).dtEnd);
        return x;
        */
    }
    @Override
    public String toString() {
        String s = (dtEnd == null ? "forever" : dtEnd.toString()); 
        s = dtBegin + " to " + s; 
        return s;
    }
    
    public void addChild(OADateTimeRange dtr) {
        if (dtr == null) return;
        if (alChildren == null) alChildren = new ArrayList<>();
        alChildren.add(dtr);
    }

    public ArrayList<OADateTimeRange<R>> getChildren() {
        if (alChildren == null) alChildren = new ArrayList<>();
        return alChildren;
    }
    
    public OADateTime getBegin() {
        return dtBegin;
    }
    public OADateTime getEnd() {
        return dtEnd;
    }
    public R getReference() {
        return reference;
    }
    
}