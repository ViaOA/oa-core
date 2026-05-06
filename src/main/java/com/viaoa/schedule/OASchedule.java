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

import java.util.Iterator;
import java.util.TreeSet;

import com.viaoa.datetime.OADateTime;

/**
 * Maintains a set of merged and ordered date–time ranges. Ranges that overlap
 * are combined into parent ranges, and clearing or adding ranges adjusts the
 * underlying structure by splitting or merging entries as required. <p>
 *
 * The schedule behaves like a simplified interval tree: a {@link TreeSet} is
 * used for chronological ordering, and {@link OADateTimeRange} nodes contain a
 * list of absorbed child ranges when overlaps occur. Clients can iterate over
 * ranges in chronological order, locate “empty” spaces between them, and test
 * whether a specific {@link com.viaoa.datetime.OADateTime} falls within any range.
 */
public class OASchedule<R> implements Iterable<OADateTimeRange<R>> {

	/**
	 * Holds the chronologically ordered set of date–time ranges that make up
	 * the schedule. Entries are merged, split, and replaced as ranges are added
	 * or cleared.
	 */
	private TreeSet<OADateTimeRange<R>> tree = new TreeSet<>();
    
	/**
	 * Tracks the last range returned by iteration methods, enabling sequential
	 * navigation through the schedule.
	 */
	private OADateTimeRange<R> dtrLast;
    
	/**
	 * Flag indicating whether iteration has reached the end of the range list.
	 */
	private boolean bEol;

	/**
	 * Creates an empty schedule with no ranges defined.
	 */
    public OASchedule() {
    }

    /**
     * Removes or adjusts existing scheduled ranges within the specified begin
     * and end timestamps. Overlapping ranges are split or replaced as needed to
     * eliminate any portion that falls inside the supplied range.
     *
     * @param dtBegin the start of the range to clear; if null, the earliest
     *                existing begin value is used
     * @param dtEnd the end of the range to clear; if null, the latest existing
     *              end value is used
     */
    public void clear(OADateTime dtBegin, OADateTime dtEnd) {
        if (dtBegin == null) {
            if (tree.size() > 0) dtBegin = tree.first().getBegin();
            if (dtBegin == null) return;
        }
        if (dtEnd == null) {
            if (tree.size() > 0) dtEnd = tree.last().getEnd();
            if (dtEnd == null) return;
        }
        if (dtEnd.before(dtBegin)) return;
        dtBegin = new OADateTime(dtBegin);
        dtEnd = new OADateTime(dtEnd);
        
        OADateTimeRange<R> dtrOpen = new OADateTimeRange<R>(dtBegin, dtEnd, null);
        OADateTimeRange<R> dtr = tree.floor(dtrOpen);  // less or equal to
        if (dtr == null) dtr = tree.higher(dtrOpen);
        for ( ; dtr != null; dtr = tree.higher(dtr)) {

            if (dtr.getBegin().compareTo(dtrOpen.getEnd()) >= 0) {
                // past
                break;
            }
            if (dtr.getEnd().compareTo(dtrOpen.getBegin()) <= 0) continue;
            
            if (dtr.getBegin().compareTo(dtrOpen.getBegin()) >= 0) {
                if (dtr.getEnd().compareTo(dtrOpen.getEnd()) <= 0) {
                    // inside of dtrOpen
                    tree.remove(dtr); 
                    continue;
                }
                // extends past dtrOpen
                OADateTimeRange<R> dtrx = new OADateTimeRange(dtrOpen.getEnd(), dtr.getEnd(), null);
                dtrx.addChild(dtr);
                tree.remove(dtr);
                tree.add(dtrx);
                break;
            }
            
            // dtr.begin is before dtrOpen
            if (dtr.getEnd().compareTo(dtrOpen.getEnd()) <= 0) {
                // dtr is before dtrOpen and ends before dtrOpen
                OADateTimeRange<R> dtrx = new OADateTimeRange(dtr.getBegin(), dtrOpen.getBegin(), null);
                dtrx.addChild(dtr);
                tree.remove(dtr);
                tree.add(dtrx);
            }
            else {
                // dtr is before dtrOpen and is larger then dtrOpen
                OADateTimeRange<R> dtrx = new OADateTimeRange(dtr.getBegin(), dtrOpen.getBegin(), null);
                dtrx.addChild(dtr);
                tree.remove(dtr);
                tree.add(dtrx);
                dtrx = new OADateTimeRange(dtrOpen.getEnd(), dtr.getEnd(), null);
                tree.add(dtrx);
                break;
            }
        }        
    }
    
    /**
     * Convenience method for adding a range without an associated reference.
     *
     * @param dtBegin the begin timestamp
     * @param dtEnd the end timestamp
     */
    public void add(OADateTime dtBegin, OADateTime dtEnd) {
        add(dtBegin, dtEnd, null);
    }

    /**
     * Adds a new date–time range to the schedule. The method merges the new
     * range with existing ones when overlaps occur, possibly absorbing or being
     * absorbed by neighboring ranges, and may create new merged ranges to
     * maintain a consistent, non-overlapping structure.
     *
     * @param dtBegin the starting timestamp; may default to the earliest
     *                existing begin when null
     * @param dtEnd the ending timestamp; may default to the latest existing
     *              end when null
     * @param reference optional reference object to associate with the added range
     */
    public void add(OADateTime dtBegin, OADateTime dtEnd, R reference) {
        if (dtBegin == null) {
            if (tree.size() > 0) dtBegin = tree.first().getBegin();
            if (dtBegin == null) return;
        }
        if (dtEnd == null) {
            if (tree.size() > 0) dtEnd = tree.last().getEnd();
            if (dtEnd == null) return;
        }
        if (dtEnd.before(dtBegin)) return;
        dtBegin = new OADateTime(dtBegin);
        dtEnd = new OADateTime(dtEnd);

        OADateTimeRange<R> dtrNew = new OADateTimeRange<R>(dtBegin, dtEnd, reference);
        for ( ;; ) {
            OADateTimeRange dtr1 = tree.floor(dtrNew);  // less or equal to
            if (dtr1 == dtrNew) dtr1 = null;
            OADateTimeRange dtr2 = tree.higher(dtrNew); // greater

            if (dtr1 != null && dtr1.getEnd().before(dtrNew.getBegin())) dtr1 = null;
            if (dtr2 != null && dtr2.getBegin().after(dtrNew.getEnd())) dtr2 = null;
            
            // No dtr1 or dtr2
            if (dtr1 == null && dtr2 == null) {
                tree.add(dtrNew);
                break;
            }

            if (dtr1 != null) {
                if (dtrNew.getBegin().equals(dtr1.getBegin())) {
                    if (dtrNew.getEnd().after(dtr1.getEnd())) {
                        // dtrNew consume dtr1
                        dtrNew.addChild(dtr1);
                        tree.remove(dtr1);
                        continue;
                    }
                    else {
                        // dtr1 consumes dtrNew
                        dtr1.addChild(dtrNew);
                        break;
                    }
                }
                
                if (dtrNew.getEnd().compareTo(dtr1.getEnd()) <= 0) {
                    // dtr1 consumes dtrNew
                    dtr1.addChild(dtrNew);
                    break;
                }
                
                // dtr1 and dtrNew overlap
                OADateTimeRange dtrx = new OADateTimeRange(dtr1.getBegin(), dtrNew.getEnd(), null); // holder with a span
                dtrx.addChild(dtr1);
                dtrx.addChild(dtrNew);
                tree.remove(dtr1);
                dtrNew = dtrx;
                continue;
            }

            if (dtrNew.getEnd().compareTo(dtr2.getEnd()) >= 0) {
                // dtrNew consumes dtr2
                dtrNew.addChild(dtr2);
                tree.remove(dtr2);
                continue;
            }
            
            // need to merge
            OADateTimeRange dtrx = new OADateTimeRange(dtrNew.getBegin(), dtr2.getEnd(), null); // holder with a span
            dtrx.addChild(dtrNew);
            dtrx.addChild(dtr2);
            tree.remove(dtr2);
            dtrNew = dtrx;
        }
    }
    
    
    
    
    /**
     * Indicates whether iteration has reached the end of the schedule.
     *
     * @return true if no more ranges remain during sequential iteration
     */
    public boolean isEndOfList() {
        return bEol;
    }

    /**
     * Resets the iteration state so that the next call to next() begins from
     * the first range.
     */
    public void reset() {
        bEol = false;
    }

    /**
     * Same as reset(); restores iteration to the beginning.
     */
    public void rewind() {
        bEol = false;
    }

    /**
     * Returns the number of scheduled ranges currently stored.
     *
     * @return the number of ranges
     */
    public int size() {
        return tree.size();
    }

    /**
     * Delegates to {@link #size()}.
     */
    public int getSize() {
        return tree.size();
    }
    
    /**
     * Returns the next scheduled range in chronological order, updating
     * internal iteration state. Returns null when the list is empty or fully
     * consumed.
     *
     * @return the next range, or null when iteration is complete
     */
    public OADateTimeRange<R> next() {
        if (bEol) return null;
        if (tree.size() == 0) dtrLast = null;
        else if (dtrLast == null) {
            dtrLast = tree.first();
        }
        else {
            dtrLast = tree.higher(dtrLast);
        }
        bEol = (dtrLast == null);
        return dtrLast;
    }

    /**
     * Returns a synthetic date–time range representing the empty space between
     * the last returned range and the next one. Uses the end of the last range
     * and the begin of the upcoming range to define the gap.
     *
     * @return a range representing the empty interval, or null at end of list
     */
    public OADateTimeRange<R> nextEmpty() {
        if (bEol) return null;

        OADateTimeRange dtrHold = dtrLast;
        
        next();
        
        OADateTimeRange dtr = new OADateTimeRange<R>(dtrHold == null ? null : dtrHold.getEnd(), dtrLast == null ? null : dtrLast.getBegin(), null);
        return dtr;
    }
    
    
    /**
     * Removes all scheduled ranges, leaving the schedule empty.
     */
    public void clear() {
        tree.clear();
    }
    
    /**
     * Returns an iterator that traverses scheduled ranges in chronological
     * order. Iteration state is reset prior to creation of the iterator.
     *
     * @return an iterator over the scheduled ranges
     */
    public Iterator<OADateTimeRange<R>> iterator() {
        reset();
        Iterator<OADateTimeRange<R>> iter = new Iterator<OADateTimeRange<R>>() {
            int pos;

            @Override
            public boolean hasNext() {
                if (bEol) return false;
                if (tree.size() == 0) return false;
                return true;
            }

            @Override
            public void remove() {
            }

            @Override
            public OADateTimeRange<R> next() {
                return OASchedule.this.next();
            }
        };
        return iter;
    }
    
    /**
     * Determines whether the given timestamp falls within any scheduled range.
     * Iterates through the schedule and checks begin/end boundaries.
     *
     * @param dt the timestamp to test; null returns false
     * @return true if the timestamp lies within an existing range
     */
    public boolean isRangeAdded(OADateTime dt) {
        if (dt == null) return false;
        for (OADateTimeRange dtr : this) {
            OADateTime dt1 = dtr.getBegin();
            OADateTime dt2 = dtr.getEnd();
            if (dt.compareTo(dt1) >= 0 && dt.compareTo(dt2) <= 0) {
                return true;
            }
        }
        reset();
        return false;
    }
    
    
}
