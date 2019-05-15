package com.viaoa.scheduler;

import java.util.Iterator;
import java.util.TreeSet;
import com.viaoa.util.OADateTime;

/**
 * Used to combine DateTime ranges that could overlap, and then iterate through them in datetime order.
 * 
 * @author vvia
 */
public class OASchedule<R> implements Iterable<OADateTimeRange<R>> {
    private TreeSet<OADateTimeRange<R>> tree = new TreeSet<>();
    private OADateTimeRange<R> dtrLast;
    private boolean bEol;

    public OASchedule() {
    }

    /**
     * Clear a time range that might already be scheduled.
     * This allows for overriding the current available time ranges.
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
     * Add a date range.
     */
    public void add(OADateTime dtBegin, OADateTime dtEnd) {
        add(dtBegin, dtEnd, null);
    }
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
     * true if next has exhausted the list.
     * @return
     */
    public boolean isEndOfList() {
        return bEol;
    }

    /**
     * rewind so that next will start from the beginning.
     */
    public void reset() {
        bEol = false;
    }
    public void rewind() {
        bEol = false;
    }

    public int size() {
        return tree.size();
    }
    public int getSize() {
        return tree.size();
    }
    
    /**
     * Get the next dtr, starting at the beginning and each call to next will be the next one in order.
     * @return dateRange, else null if list is empty or exhausted.
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

    public OADateTimeRange<R> nextEmpty() {
        if (bEol) return null;

        OADateTimeRange dtrHold = dtrLast;
        
        next();
        
        OADateTimeRange dtr = new OADateTimeRange<R>(dtrHold == null ? null : dtrHold.getEnd(), dtrLast == null ? null : dtrLast.getBegin(), null);
        return dtr;
    }
    
    
    public void clear() {
        tree.clear();
    }
    
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
                return next();
            }
        };
        return iter;
    }
    
}
