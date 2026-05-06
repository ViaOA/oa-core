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

import com.viaoa.compare.OACompare;
import com.viaoa.datetime.OADateTime;

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
    
	/**
	 * The beginning timestamp of this date–time range.
	 */
	private OADateTime dtBegin;
    
	/**
	 * The ending timestamp of this date–time range. A null value indicates an open-ended range.
	 */
	private OADateTime dtEnd;
    
	/**
	 * List of child ranges that were overlapped or absorbed by this range.
	 */
    private ArrayList<OADateTimeRange<R>> alChildren; 

    /**
     * Optional reference object associated with this range as supplied by callers.
     */
    private R reference;
    
    /**
     * Creates a new date–time range with the specified begin and end values and an
     * optional reference object.
     *
     * @param dtBegin the starting timestamp for the range
     * @param dtEnd the ending timestamp for the range, or null for open-ended
     * @param ref an optional reference object to associate with this range
     */
    public OADateTimeRange(OADateTime dtBegin, OADateTime dtEnd, R ref) {
        this.dtBegin = dtBegin;
        this.dtEnd = dtEnd;
        this.reference = ref;
    }
    
    /**
     * Compares this range with another object for equality based on matching begin
     * and end timestamps. Returns true only when both values are equal.
     *
     * @param obj the object to compare against
     * @return true if both ranges have matching begin and end values; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof OADateTimeRange)) return false;
        
        if (!OACompare.isEqual(((OADateTimeRange)obj).dtBegin, this.dtBegin)) return false;
        if (!OACompare.isEqual(((OADateTimeRange)obj).dtEnd, this.dtEnd)) return false;
        return true;
    }
    
    
    /**
     * Computes a hash value for this range using the begin timestamp. Returns zero
     * when the begin value is null.
     *
     * @return the hash code based on the begin timestamp
     */
    @Override
    public int hashCode() {
        if (dtBegin == null) return 0;
        return dtBegin.hashCode();
    }

    /**
     * Compares this range to another by ordering based on the begin timestamp.
     *
     * @param obj the object to compare against
     * @return a negative, zero, or positive value depending on ordering
     */
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

    /**
     * Returns a string representation of this range in the form
     * "begin to end". If the end is null, "forever" is used.
     *
     * @return a human-readable representation of this date–time range
     */
    @Override
    public String toString() {
        String s = (dtEnd == null ? "forever" : dtEnd.toString()); 
        s = dtBegin + " to " + s; 
        return s;
    }
    
    /**
     * Adds the supplied range to this range's list of child ranges. A null value
     * is ignored. Initializes the child list on first use.
     *
     * @param dtr the child range to add
     */
    public void addChild(OADateTimeRange dtr) {
        if (dtr == null) return;
        if (alChildren == null) alChildren = new ArrayList<>();
        alChildren.add(dtr);
    }

    /**
     * Returns the list of child ranges associated with this range. The list is
     * lazily created on first access.
     *
     * @return the list of child ranges, never null
     */
    public ArrayList<OADateTimeRange<R>> getChildren() {
        if (alChildren == null) alChildren = new ArrayList<>();
        return alChildren;
    }
    
    /**
     * Returns the begin timestamp for this range.
     *
     * @return the begin timestamp, or null if not set
     */
    public OADateTime getBegin() {
        return dtBegin;
    }

    /**
     * Returns the end timestamp for this range.
     *
     * @return the end timestamp, or null if the range is open-ended
     */
    public OADateTime getEnd() {
        return dtEnd;
    }

    /**
     * Returns the reference object associated with this range.
     *
     * @return the reference object, or null if none was supplied
     */
    public R getReference() {
        return reference;
    }
    
}