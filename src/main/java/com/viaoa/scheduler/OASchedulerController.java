package com.viaoa.scheduler;

import java.util.ArrayList;

import com.viaoa.ds.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectSchedulerDelegate;
import com.viaoa.scheduler.OAScheduler;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/*
 * Used to set date[time] property values for an Hub.activeObject that has calendar properties.
 * Includes an OAScheduler callback that can be used to assist in selecting a date[time].  
 * 
 * @param <F> from object
 * @param <T> scheduler object
 * 
 * @author vvia
 */
public class OASchedulerController<F extends OAObject, T extends OAObject> {

    // can use either datetime or date+time properties
    private String ppDateTimeFrom;
    private String ppDateTimeTo;

    private String ppDateFrom;
    private String ppTimeFrom;
    private String ppDateTo;
    private String ppTimeTo;

    private Hub<F> hubFrom;
    private String ppSchedule;
    private Hub<? extends OAObject> hubDetail;
    private Hub<? extends OAObject> hubLink;

    /** type of schedule
        1: m obj -> 1 sch  => a single timeslot can have many objects sharing it, and can not change the dt
        2: 1 obj -> 1 sch  => a single timeslot owns a schedule, and can change the dt
        3: obj -> m objx -> m sch  => using a link object, that is created when the timeslot is choosen, and the timeslot dt cant be changed
        4: obj -> m objx -> 1 sch  => using a link object, that is created when the timeslot is choosen, and the timeslot dt can be changed
    */
    private int type;
    
    
    /**
     * Create a new scheduler panel, that allows user to find an available datetime slot to schedule the active object in hubFrom.
     * @param hubFrom object that is being scheduled.
     * @param hubLink (optional) link hub that has the reference schedule object 
     * @param ppSchedule property path to the schedule reference (OAOne) link object. 
     * @param ppDisplay property from schedule to use for display/renderer. 
     * @param ppDateFrom property from schedule object
     * @param ppTimeFrom property from schedule object
     * @param ppDateTo property from schedule object
     * @param ppTimeTo property from schedule object
     */
    public OASchedulerController(Hub<F> hubFrom, Hub hubLink, String ppSchedule, String ppDateFrom, String ppTimeFrom, String ppDateTo, String ppTimeTo) {
        this.hubFrom = hubFrom;
        this.hubLink = hubLink;
        this.ppSchedule = ppSchedule;
        this.ppDateFrom = ppDateFrom;
        this.ppTimeFrom = ppTimeFrom;
        this.ppDateTo = ppDateTo;
        this.ppTimeTo = ppTimeTo;
        setup();
    }
    public OASchedulerController(Hub<F> hubFrom, String ppSchedule, String ppDateFrom, String ppTimeFrom, String ppDateTo, String ppTimeTo) {
        this.hubFrom = hubFrom;
        // this.hubLink = 
        this.ppSchedule = ppSchedule;
        this.ppDateFrom = ppDateFrom;
        this.ppTimeFrom = ppTimeFrom;
        this.ppDateTo = ppDateTo;
        this.ppTimeTo = ppTimeTo;
        setup();
    }

    
    /**
     * Constructor that uses datetime properties (instead of date & time)
     * @param hubFrom
     * @param ppSchedule
     * @param ppDisplay
     * @param ppDateTimeFrom
     * @param ppDateTimeTo
     */
    public OASchedulerController(Hub<F> hubFrom, Hub hubLink, String ppSchedule, String ppDateTimeFrom, String ppDateTimeTo) {
        this.hubFrom = hubFrom;
        this.hubLink = hubLink;
        this.ppSchedule = ppSchedule;
        this.ppDateTimeFrom = ppDateTimeFrom;
        this.ppDateTimeTo = ppDateTimeTo;
        setup();
    }
    public OASchedulerController(Hub<F> hubFrom, String ppSchedule, String ppDateTimeFrom, String ppDateTimeTo) {
        this.hubFrom = hubFrom;
        // this.hubLink =
        this.ppSchedule = ppSchedule;
        this.ppDateTimeFrom = ppDateTimeFrom;
        this.ppDateTimeTo = ppDateTimeTo;
        setup();
    }

    public int getType() {
        return this.type;
    }

    public Hub getDetailHub() {
        return hubDetail;
    }
    public String getFromDateProperty() {
        if (ppDateFrom != null) return ppDateFrom;
        return ppDateTimeFrom;
    }
    protected void setup() {
        if (hubFrom == null) return;
        
        hubDetail = hubFrom.getDetailHub(ppSchedule);
        OAPropertyPath pp = new OAPropertyPath(hubFrom.getObjectClass(), ppSchedule);
        
        OAPropertyPath ppRev = pp.getReversePropertyPath();
        OALinkInfo[] lis = ppRev.getLinkInfos();
        if (lis == null || lis.length == 0) {
            
        }
        else if (lis.length == 1 && lis[0].getType() == OALinkInfo.TYPE_MANY) {
            type = 1;
        }
        else if (lis.length == 1 && lis[0].getType() == OALinkInfo.TYPE_ONE) {
            type = 2;
        }
        else if (lis.length == 2 && lis[1].getType() == OALinkInfo.TYPE_MANY) {
            type = 3;
        }
        else if (lis.length == 2 && lis[1].getType() == OALinkInfo.TYPE_ONE) {
            type = 4;
        }
        else {
            throw new RuntimeException("invalid type of relationship between the hub object the schedule object");
        }
    }

    /**
     * Called to call the Hub activeObject scheduler callback.  Uses OAObjectSchedulerDelegate.getScheduler(..) to create and populate an OAScheduler.
     * @param date
     * @return
     */
    public OAScheduler getSchedulerCallback(OADate date) {
        F obj = hubFrom.getAO();
        OAScheduler sch = OAObjectSchedulerDelegate.getScheduler(obj, ppSchedule, date);
        return sch;
    }
    
    
    /**
     * Called to update the active object's schedule object's date & time values.
     * @param objSchedule schedule object that was selected (can be null)
     * @param dtFrom begin date/time slot selected
     * @param dtTo end date/time slot selected
     */
    public void set(final OADateTime dtFrom, final OADateTime dtTo) {
        if (dtFrom == null) return;
        if (dtTo == null) return;
        if (hubDetail == null) return;
        
        final F obj = hubFrom.getAO();
        if (obj == null) return;

        // see if date/time is already used for this object
        OAFinder f = new OAFinder(hubFrom, ppSchedule) {
            @Override
            protected boolean isUsed(OAObject obj) {
                if (ppDateTimeFrom != null) {
                    Object objx = obj.getProperty(ppDateTimeFrom);
                    if (objx == null || !objx.equals(dtFrom)) return false;
                    Object bjx = obj.getProperty(ppDateTimeTo);
                    if (objx == null || !objx.equals(dtTo)) return false;
                }
                else {
                    Object objx = obj.getProperty(ppDateFrom);
                    if (objx == null || !objx.equals(dtFrom)) return false;
                    objx = obj.getProperty(ppDateTo);
                    if (objx == null || !objx.equals(dtTo)) return false;
                }
                return true;
            }
        };
        OAObject objx = f.findFirst(obj);
        if (objx != null) {
            if (hubLink != null) {
                // set AO
                OAPropertyPath pp = new OAPropertyPath(hubFrom.getObjectClass(), ppSchedule);
                hubLink.find(pp.getLastPropertyName(), objx, true);
            }
            return;  // already used
        }

        OAObject objSchedule = (OAObject) obj.getProperty(ppSchedule);
        
        if (objSchedule == null) {
            if (type == 2 || type == 4) {
                // see if there is an existing scheduler in the object cache
                OAFinder finder = new OAFinder();
                if (ppDateTimeFrom != null) {
                    finder.addEqualFilter(ppDateTimeFrom, dtFrom);
                    if (ppDateTimeTo != null) finder.addEqualFilter(ppDateTimeTo, dtTo);
                }
                else {
                    if (ppDateFrom != null) finder.addEqualFilter(ppDateFrom, new OADate(dtFrom));
                    if (ppTimeFrom != null) finder.addEqualFilter(ppTimeFrom, new OATime(dtFrom));
                    if (ppDateTo != null) finder.addEqualFilter(ppDateTo, new OADate(dtTo));
                    if (ppTimeTo != null) finder.addEqualFilter(ppTimeTo, new OATime(dtTo));
                }
                objSchedule = (OAObject) OAObjectCacheDelegate.find(hubDetail.getObjectClass(), finder);
                
                if (objSchedule == null) {
                    // need to check datasource
                    String sql = "";
                    ArrayList al = new ArrayList(); 
                    if (ppDateTimeFrom != null) {
                        sql = ppDateTimeFrom + " = ?";
                        al.add(dtFrom);
                        if (ppDateTimeTo != null) {
                            sql += " AND " + ppDateTimeTo + " = ?";
                            al.add(dtTo);
                        }
                    }
                    else {
                        if (ppDateFrom != null) {
                            sql += OAString.append(sql, ppDateFrom + " = ?", " AND ");
                            al.add(new OADate(dtFrom));
                        }
                        if (ppTimeFrom != null) {
                            sql += OAString.append(sql, ppTimeFrom + " = ?", " AND ");
                            al.add(new OATime(dtFrom));
                        }
                        if (ppDateTo != null) {
                            sql += OAString.append(sql, ppDateTo + " = ?", " AND ");
                            al.add(new OADate(dtTo));
                        }
                        if (ppTimeTo != null) {
                            sql += OAString.append(sql, ppTimeTo + " = ?", " AND ");
                            al.add(new OATime(dtTo));
                        }
                    }
                    OASelect sel = new OASelect(hubDetail.getObjectClass());
                    Object[] params = new Object[al.size()];
                    al.toArray(params);
                    sel.select(sql, params);
                    objSchedule = sel.next();
                }
            }
        }
        else {
            if (type == 1 || type == 3) {
                // need to create a new schedule object since the schedule object is shared
                objSchedule = null;
            }
        }
        
        boolean bNew = false;
        if (objSchedule == null) {
            objSchedule = (OAObject) OAObjectReflectDelegate.createNewObject(hubDetail.getObjectClass());
            bNew = true;
        }
        
        if (bNew || type == 2 || type == 4) {
            if (ppDateTimeFrom != null) {
                objSchedule.setProperty(ppDateTimeFrom, dtFrom);
                if (ppDateTimeTo != null) objSchedule.setProperty(ppDateTimeTo, dtTo);
            }
            else {
                if (ppDateFrom != null) objSchedule.setProperty(ppDateFrom, new OADate(dtFrom));
                if (ppTimeFrom != null) objSchedule.setProperty(ppTimeFrom, new OATime(dtFrom));
    
                if (ppDateTo != null) objSchedule.setProperty(ppDateTo, new OADate(dtTo));
                if (ppTimeTo != null) objSchedule.setProperty(ppTimeTo, new OATime(dtTo));
            }
        }

        // assign the schedule object
        if (type == 1) {
            obj.setProperty(ppSchedule, objSchedule);
        }
        else if (type == 2) {
            if (bNew) obj.setProperty(ppSchedule, objSchedule);
        }
        else if (type == 3 || type == 4) {
            OAPropertyPath pp = new OAPropertyPath(hubFrom.getObjectClass(), ppSchedule);
            Hub hubx = (Hub) obj.getProperty(pp.getProperties()[0]);
            objx = (OAObject) OAObjectReflectDelegate.createNewObject(hubx.getObjectClass());
            objx.setProperty(pp.getProperties()[1], objSchedule);
            hubx.add(objx);
            if (hubLink != null) hubLink.setAO(objx);
        }
    }
}
