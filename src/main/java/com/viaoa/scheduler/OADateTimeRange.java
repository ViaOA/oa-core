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

import com.viaoa.util.OACompare;
import com.viaoa.util.OADateTime;

/**
 * Represents an immutable date–time range used by {@link OASchedule} to
 * combine, merge, and order ranges. Each range can optionally maintain a list
 * of child ranges representing segments that were absorbed or overlapped
 * during schedule consolidation. <p>
 *
 * Instances are ordered by their begin date and two ranges are considered
 * equal when both their begin and end values match. The range may also carry a
 * reference object supplied by callers of {@link OASchedule#add}.
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