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

import com.viaoa.datetime.OADate;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

/**
 * Controller used for selecting and assigning schedule date–time values to the
 * active object of a {@link com.viaoa.hub.Hub}. The controller interprets the
 * relationship between the object being scheduled and the schedule object
 * referenced by a property path, determines the scheduling mode, and assists
 * in locating, creating, or updating the associated schedule entry. <p>
 *
 * The controller supports models where schedule objects are shared, owned,
 * or created through link objects. It can operate on either date–time
 * properties or separate date and time properties, performs availability
 * checks using {@link OAObjectSchedulerDelegate}, and updates the selected
 * schedule object accordingly.
 */
public class OASchedulerController<F extends OAObject, T extends OAObject> {

	/**
	 * Property path for the schedule object's begin datetime value.
	 */
    private String ppDateTimeFrom;

    /**
     * Property path for the schedule object's end datetime value.
     */
    private String ppDateTimeTo;

    /**
     * Property path for the schedule object's begin date value when date+time
     * properties are used instead of a single datetime.
     */
    private String ppDateFrom;
    
    /**
     * Property path for the schedule object's begin time value.
     */
    private String ppTimeFrom;
    
    /**
     * Property path for the schedule object's end date value when date+time
     * properties are used.
     */
    private String ppDateTo;
    
    /**
     * Property path for the schedule object's end time value.
     */
    private String ppTimeTo;

    /**
     * Hub that contains the objects being scheduled and whose active object is
     * used as the scheduling target.
     */
    private Hub<F> hubFrom;
    
    /**
     * Property path to the schedule reference (OAOne) on the objects in hubFrom.
     */
    private String ppSchedule;
    
    /**
     * Detail hub associated with the schedule property, used to locate or create
     * schedule objects.
     */
    private Hub<? extends OAObject> hubDetail;
    
    /**
     * Optional link hub used when schedules are attached through a link object.
     */
    private Hub<? extends OAObject> hubLink;

    /** type of schedule
        1: m obj -> 1 sch  => a single timeslot can have many objects sharing it, and can not change the dt
        2: 1 obj -> 1 sch  => a single timeslot owns a schedule, and can change the dt
        3: obj -> m objx -> m sch  => using a link object, that is created when the timeslot is choosen, and the timeslot dt cant be changed
        4: obj -> m objx -> 1 sch  => using a link object, that is created when the timeslot is choosen, and the timeslot dt can be changed
    */
    private int type;
    
    
    /**
     * Creates a scheduler controller using separate date and time properties for
     * the schedule object. Link hubs may optionally be supplied when relationships
     * involve link objects.
     *
     * @param hubFrom hub containing objects being scheduled
     * @param hubLink optional link hub for accessing linked schedule objects
     * @param ppSchedule property path to the schedule reference
     * @param ppDateFrom schedule begin date property
     * @param ppTimeFrom schedule begin time property
     * @param ppDateTo schedule end date property
     * @param ppTimeTo schedule end time property
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
    
    /**
     * Creates a scheduler controller using separate date and time properties
     * without supplying a link hub.
     *
     * @param hubFrom hub containing objects being scheduled
     * @param ppSchedule property path to the schedule reference
     * @param ppDateFrom schedule begin date property
     * @param ppTimeFrom schedule begin time property
     * @param ppDateTo schedule end date property
     * @param ppTimeTo schedule end time property
     */
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
     * Creates a scheduler controller using datetime properties for begin and end.
     *
     * @param hubFrom hub containing objects being scheduled
     * @param hubLink optional link hub
     * @param ppSchedule property path to the schedule reference
     * @param ppDateTimeFrom begin datetime property
     * @param ppDateTimeTo end datetime property
     */
    public OASchedulerController(Hub<F> hubFrom, Hub hubLink, String ppSchedule, String ppDateTimeFrom, String ppDateTimeTo) {
        this.hubFrom = hubFrom;
        this.hubLink = hubLink;
        this.ppSchedule = ppSchedule;
        this.ppDateTimeFrom = ppDateTimeFrom;
        this.ppDateTimeTo = ppDateTimeTo;
        setup();
    }
    
    /**
     * Creates a scheduler controller using datetime properties without supplying
     * a link hub.
     *
     * @param hubFrom hub containing objects being scheduled
     * @param ppSchedule property path to the schedule reference
     * @param ppDateTimeFrom begin datetime property
     * @param ppDateTimeTo end datetime property
     */
    public OASchedulerController(Hub<F> hubFrom, String ppSchedule, String ppDateTimeFrom, String ppDateTimeTo) {
        this.hubFrom = hubFrom;
        // this.hubLink =
        this.ppSchedule = ppSchedule;
        this.ppDateTimeFrom = ppDateTimeFrom;
        this.ppDateTimeTo = ppDateTimeTo;
        setup();
    }

    /**
     * Returns the resolved scheduling mode.
     *
     * @return the scheduling type value
     */
    public int getType() {
        return this.type;
    }

    /**
     * Returns the detail hub associated with the schedule reference property.
     *
     * @return the detail hub, or null if none exists
     */
    public Hub getDetailHub() {
        return hubDetail;
    }

    /**
     * Returns the effective begin-date property. Prefers the date property when
     * provided, otherwise returns the datetime property.
     *
     * @return the begin-date or begin-datetime property path
     */
    public String getFromDateProperty() {
        if (ppDateFrom != null) return ppDateFrom;
        return ppDateTimeFrom;
    }
    
    /**
     * Analyzes the hubFrom → schedule relationship using property paths and link
     * metadata. Determines the scheduling type and initializes hubDetail.
     */
    protected void setup() {
        if (hubFrom == null) return;
        
        hubDetail = hubFrom.getDetailHub(ppSchedule);
        OAPath pp = new OAPath(hubFrom.getObjectClass(), ppSchedule);
        
        OAPath ppRev = pp.getReversePropertyPath();
        OALinkInfo[] lis = ppRev == null ? null : ppRev.getLinkInfos();
        if (lis == null || lis.length == 0) {
            // no-op
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
     * Invokes the OAObjectSchedulerDelegate to obtain an OAScheduler for the
     * active object at the specified date.
     *
     * @param date the date for which scheduling should be evaluated
     * @return the scheduler created for the active object
     */
    public OAScheduler getSchedulerCallback(OADate date) {
        F obj = hubFrom.getAO();
        
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
        
        OAScheduler sch = og.objectsInternal().callObjectSchedulerGetScheduler(obj, ppSchedule, date);
        return sch;
    }
    
    
    /**
     * Assigns the schedule object's begin and end date–time values for the active
     * object. Handles four scheduling types, checks for existing schedule usage,
     * locates or creates the appropriate schedule object, and updates or assigns it
     * based on relationship rules.
     *
     * @param dtFrom the selected begin date–time
     * @param dtTo the selected end date–time
     */
    public void set(final OADateTime dtFrom, final OADateTime dtTo) {
        if (dtFrom == null) return;
        if (dtTo == null) return;
        if (hubDetail == null) return;
        
        final F obj = hubFrom.getAO();
        if (obj == null) return;

        // see if date/time is already used for this object
        OAFinder f = new OAFinder(hubFrom, ppSchedule) {
        	/**
        	 * Determines whether the given schedule object matches the specified begin and
        	 * end date–time values. Compares either datetime properties or separate date
        	 * and time properties depending on controller configuration.
        	 *
        	 * @param obj the schedule object to test
        	 * @return true if the schedule object's date–time values match dtFrom/dtTo
        	 */
            @Override
            protected boolean isUsed(OAObject obj) {
                if (ppDateTimeFrom != null) {
                    Object objx = obj.getProperty(ppDateTimeFrom);
                    if (objx == null || !objx.equals(dtFrom)) return false;
                    objx = obj.getProperty(ppDateTimeTo);
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
                OAPath pp = new OAPath(hubFrom.getObjectClass(), ppSchedule);
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
        		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubDetail.getObjectClass());
                objSchedule = (OAObject) og.objectsInternal().callObjectCacheFind(hubDetail.getObjectClass(), finder);
                
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
    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubDetail);
            objSchedule = (OAObject) og.objectsInternal().callObjectReflectCreateNewObject(hubDetail.getObjectClass());
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
            OAPath pp = new OAPath(hubFrom.getObjectClass(), ppSchedule);
            Hub hubx = (Hub) obj.getProperty(pp.getProperties()[0]);
    		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(hubx);
            objx = (OAObject) og.objectsInternal().callObjectReflectCreateNewObject(hubx.getObjectClass());
            objx.setProperty(pp.getProperties()[1], objSchedule);
            hubx.add(objx);
            if (hubLink != null) hubLink.setAO(objx);
        }
    }
}
