/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.hub;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.*;
import com.viaoa.ds.*;
import com.viaoa.object.*;

/**
 * Observable Collection Class that has similar methods as both ArrayList and
 * HashMap. When used with OAObject, the Hub sends all events for the objects
 * that it contains.
 * <p>
 * 
 * Observable means that will notify listeners whenever an event happens. Hub
 * has methods to register listeners and for sending Events.
 * <p>
 * 
 * <i>Searching</i><br>
 * Hub has methods to find any object based on a property path and search value.
 * <p>
 * 
 * <i>Sorting</i><br>
 * Hub has methods for sorting/ordering of objects within the collection. The
 * collection is kept sorted as objects are added, inserted, or changed. A
 * property path can be used or a customized comparator can be used.
 * <p>
 * 
 * <i>Manages OAObjects</i><br>
 * Hub is used by OAObject for sending events and for managing event listeners.
 * <p>
 * 
 * <i>Works directly with {@link OADataSource}</i><br>
 * The Hub Class has methods to directly select objects from a
 * DataSource/database using an {@link OASelect}. Hubs are also set up to only
 * pre-fetch a certain number of objects at a time, so that response is faster.
 * Methods to get a total count of objects and to load all objects are also
 * included.
 * <p>
 * 
 * <i>Recursive Hubs</i><br>
 * A Hub that is recursive is where each object has children objects of the same
 * class. Each object has a method to get its "parent". A "Root Hub" is the top
 * Hub where all of the objects in it do not have a parent (value is null). Hub
 * and OAObject will automatically put objects in the correct Hub based on the
 * value of the parent. If another Object owns the Hub, then all children under
 * it will have a reference to the owner object.
 * <p>
 * 
 * <i>Hub Filtering</i><br>
 * A Hub can be created that filters objects from another Hub. see
 * {@link HubFilter} for more information.
 * <p>
 * 
 * <i>XML Support</i><br>
 * Hub has methods to work directly with OAXMLReader/Writer to read/write XML.
 * <p>
 * 
 * <i>Serialization</i><br>
 * Works with OAObject to handle serialization of objects to/from a stream.
 * <p>
 * 
 * Inside the Hub are the following objects:
 * <ul>
 * <li>Data - a Vector and Hashtable that are used to store the objects. This
 * can be shared/used by other Hubs.
 * <li>Unique - information that is unique to a single Hub. ie: the registered
 * event listeners.
 * <li>Active - keeps track of the object within the Hub that has the current
 * focus. This can be shared/used by other Hubs.
 * <li>Master - Hub/Object that this Hub belongs with. Example: a Hub of
 * Employee objects that belongs to a Department object.
 * </ul>
 * <p>
 * 
 * <b>Navigational features</b><br>
 * The Hub Collection has methods that allow it to be <i>navigated</i>. This is
 * primarily used when using Hubs with GUI components, where the Hub acts as the
 * Model in MVC (Model/View/Controller) that is commonly used for building GUI
 * applications.
 * <p>
 * 
 * Hubs have an <i>Active Object</i>, which is a reference to the object in the
 * Hub that currently has the <i>focus</i>. Navigational methods in the Hub can
 * be used to change the active object. <br>
 * &nbsp;&nbsp;&nbsp;<img src="doc-files/Hub2.gif" alt="hub">
 * <p>
 * 
 * 
 * <b>Configuring Hubs to work together (the <i>Wiring</i>)</b><br>
 * Hubs can be configured to form relationships and automatically work together.
 * <br>
 * This includes:
 * <ul>
 * <li>Creating Master/Detail relationships. see {@link DetailHub}
 * <li>Hubs that Share the same data. see {@link SharedHub}
 * <li>Linking/Connecting Hubs together. see {@link HubLink}
 * </ul>
 * &nbsp;&nbsp;&nbsp;<img src="doc-files/Hub1.gif" alt="">
 */
public class Hub<TYPE> implements Serializable, List<TYPE>, Cloneable, Comparable<TYPE>, Iterable<TYPE> {
    static final long serialVersionUID = 1L; // used for object serialization

    /** Internal object used to store objects in Vector and Hashtable. */
    protected volatile HubData data;

    /** Internal object used unique information about this Hub. */
    protected HubDataUnique datau;

    /** Internal object used store active object, bof flag, eof flag. */
    protected volatile HubDataActive dataa;

    /** Internal object used store master object and Hub. */
    protected HubDataMaster datam;

    /**
     * No argument constructor for creating a new Hub. Note: you must call
     * Hub.setObjectClass(objClass) before adding objects.
     */
    public Hub() {
        this((Class) null, 5);
    }

    /**
     * Create a new hub with a single object, and make it AO
     */
    public Hub(OAObject obj) {
        Class objClass = ((obj == null) ? (Class) null : obj.getClass());
        data = new HubData(objClass, 5);
        datau = new HubDataUnique();
        dataa = new HubDataActive();
        datam = new HubDataMaster();
        if (obj != null) this.setPos(0);
    }
    
    /**
     * Create a hub that will contain objects for a particular Class. Objects
     * that are added must be from this Class or subclass of this Class.
     * <p>
     * Example: Hub h = new Hub(Employee.class)
     * 
     * @param objClass
     *            Class for the object being stored
     */
    public Hub(Class<TYPE> objClass) {
        this(objClass, 5);
    }

    /**
     * Create a hub that will contain a Class of objects. Objects that are added
     * must be from this Class or subclass of this Class.
     * 
     * @param objClass
     *            Class for the object being stored
     * @param vecSize
     *            initial size of vector
     */
    public Hub(Class<TYPE> objClass, int vecSize) {
        data = new HubData(objClass, vecSize);
        datau = new HubDataUnique();
        dataa = new HubDataActive();
        datam = new HubDataMaster();
    }
    public Hub(Class<TYPE> objClass, int vecSize, int incrementSize) {
        data = new HubData(objClass, vecSize, incrementSize);
        datau = new HubDataUnique();
        dataa = new HubDataActive();
        datam = new HubDataMaster();
    }

    /**
     * Create a shared hub, this is same as calling hub.getSharedHub().
     * 
     * @see #createSharedHub
     */
    public Hub(Hub masterHub) {
        this(masterHub == null ? (Class) null : masterHub.getObjectClass(), 5);
        if (masterHub != null) {
            HubShareDelegate.setSharedHub(this, masterHub, false);
        }
    }

    /**
     * Create a hub that will contain a Class of objects, and the Master object
     * for this hub is known. This is only used by OAObject.getHub() to create a
     * Hub that is owned by a object.
     * <p>
     * example: Hub(Employee.class, dept) to set up a hub of employees for a
     * specific department<br>
     * if a dataSource exists for this hub's objects, then select() will be
     * called whenever the objects are referenced or by calling executeSelect()
     * directly.<br>
     * 
     * @param linkInfo
     *            from hub class to master object
     */
    public Hub(Class clazz, OAObject masterObject, OALinkInfo linkInfo, boolean bCreateSelect) {
        this(clazz, 5);
        if (linkInfo == null) {
            HubDetailDelegate.setMasterObject(this, masterObject);
        }
        else {
            HubDetailDelegate.setMasterObject(this, masterObject, linkInfo);

            if (bCreateSelect) {
                // create select, but dont call select.select(), since it could be
                // coming from server. See: OAObjectReflectDelegate.getReferenceHub(..)
                OASelect sel = HubSelectDelegate.getSelect(this, true);
                if (masterObject != null) {
                    sel.setWhereObject(masterObject);
                    sel.setPropertyFromWhereObject(linkInfo.getReverseName());
                }
            }
        }
    }
    public Hub(Class clazz, OAObject masterObject) {
        this(clazz, masterObject, null, true);
    }
    /**
     * This will set the new capacity for the Hub.
     */
    public void ensureCapacity(int size) {
        HubDataDelegate.ensureCapacity(this, size);
    }

    /**
     * This will set the new capacity for the Hub to the current size.
     */
    public void resizeToFit() {
        HubDataDelegate.resizeToFit(this);
    }

    /**
     * Used by serialization to store Hub.
     */
    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        HubSerializeDelegate._writeObject(this, stream);
    }

    /**
     * Used by serialization when reading objects from stream.
     */
    protected Object readResolve() throws ObjectStreamException {
        return HubSerializeDelegate._readResolve(this);
    }

    /**
     * Misc name/values pairs stored in this Hub.
     * <p>
     * Used for storing additional info for hub. The property will be saved with
     * hub on serialization.
     * 
     * @param name
     *            of property (case insensitive)
     * @param obj value to store
     * @see #getProperty
     * @see #removeProperty
     */
    public void setProperty(String name, Object obj) {
        HubDelegate.setProperty(this, name, obj);
    }

    /**
     * Retrieve object from name/value pair.
     * 
     * @param name
     *            of property to get. (case insensitive)
     * see #putProperty(String, Object)
     * @see #removeProperty
     */
    public Object getProperty(String name) {
        return HubDelegate.getProperty(this, name);
    }

    /**
     * Remove name/value pair.
     * 
     * @param name
     *            of property to get. (case insensitive)
     * see #putProperty(String, Object)
     * see #getProperty
     */
    public void removeProperty(String name) {
        HubDelegate.removeProperty(this, name);
    }

    /**
     * Returns default toString(), plus name of Object Class of the objects that
     * are in the collection.
     */
    public String toString() {
        return _toString(0, null);
    }

    
    private String _toString(int cnt, ArrayList<Hub> alHub) {
        if (datau == null) return "Hub";
        String s = OAString.getClassName(this.getClass()) + "." + OAString.getClassName(data.objClass); 
        // was:  super._toString() datau.objClass;
        
        if (alHub != null) {
            if (alHub.contains(this)) {
                OAObjectInfo oi = getOAObjectInfo();
                if (oi.getRecursiveLinkInfo(OALinkInfo.MANY) != null) return " ... (recursive)"; 
                return " - ERROR: hub has a  endless loop of references, current Hub=" + s;
            }
            if (alHub.size() > 20) {
                return " ... note: hub has more then 20+ ";
            }
        }

        if (datau.getSharedHub() != null) {
            if (cnt > 5) {
                if (alHub == null) alHub = new ArrayList<Hub>(5);
                alHub.add(this);
            }
            s += "->Shared:" + datau.getSharedHub()._toString(cnt+1, alHub);
        }
        else {
            /* 20151111 dont call select methods, since this could cause a deadlock
             *  ** alling toString should not have any side effects
            OASelect sel = data.getSelect();
            if (sel != null) {
                boolean b = sel.isCounted();

                if (!sel.hasBeenStarted()) {
                    if (b) s += " counted: " + sel.getCount() + ", ";
                    s += " not selected";
                }
                else {
                    if (sel.hasMore()) {
                        if (b) s += ",counted:" + sel.getCount() + ", ";
                        s += ",currentSize:" + getCurrentSize();
                        s += ",moreData=true";
                    }
                    else s += ",size:" + getSize();
                }
            }
            else {
            */
                s += ",csize:" + getCurrentSize();
            //}

            HubDataMaster dm = HubDetailDelegate.getDataMaster(this);
            if (dm.getMasterHub() != null) {
                if (cnt > 5) {
                    if (alHub == null) alHub = new ArrayList<Hub>();
                    alHub.add(this);
                }
                s += ">MasterHub:" + dm.getMasterHub()._toString(cnt+1, alHub);
            }
            else if (dm.getMasterObject() != null) {
                s += ">MasterObject:" + dm.getMasterObject();
            }
        }
        return s;
    }

    /**
     * Flag to have Object's data refreshed from datasource whenever it is the
     * active object (not implemented).
     * <p>
     * Note: *** not yet implemented ***
     * 
     * @see #getRefresh
     */
    public void setRefresh(boolean b) {
        data.setRefresh(b);
    }

    /**
     * Flag that determines if objects are refreshed from datasource when they
     * become the active object (not yet implemented).
     * <p>
     * Note: *** not yet implemented ***
     * 
     * @see Hub#getRefresh
     */
    public boolean getRefresh() {
        return data.isRefresh();
    }

    /**
     * Returns true if the hub has changed (objects were
     * added/inserted/removed/replaced, or any object has been changed).
     * 
     * @param cascadeRule
     *            is the rule from OAObject
     */
    public boolean getChanged(int cascadeRule) {
        if (data.changed) return true;
        OACascade cascade = new OACascade();
        return HubDelegate.getChanged(this, cascadeRule, cascade);
    }

    /**
     * Flag to know if hub has been changed. This only affects changes that
     * occurred by adds/inserts/removes/replaces and not changes to the Objects
     */
    public void setChanged(boolean b) {
        HubDataDelegate.setChanged(this, b);
    }

    /**
     * Copy objects into an array.
     * 
     * @see Vector#copyInto
     */
    public void copyInto(TYPE[] anArray) {
        HubSelectDelegate.loadAllData(this);
        HubDataDelegate.copyInto(this, anArray);
    }

    /**
     * Copy and return an array objects.
     * 
     * @see Vector#copyInto
     */
    public TYPE[] toArray() {
        HubSelectDelegate.loadAllData(this);
        return (TYPE[]) HubDataDelegate.toArray(this);
    }
    
    @Override
    public <TYPE> TYPE[] toArray(TYPE[] anArray) {
        HubSelectDelegate.loadAllData(this);
        int x1 = anArray.length;
        int x2 = getSize();
        if (x1 != x2) {
            anArray = (TYPE[]) Array.newInstance(getObjectClass(), x2);
        }
        for (int i = 0; i<x2 ; i++) {
            Object obj = this.elementAt(i);
            if (obj == null) break;
            anArray[i] = (TYPE) obj;
        }
        return anArray;
    }

    /**
     * Copy all objects in this hub to Hub h.
     */
    public void copyInto(Hub h) {
        if (h == null) return;
        for (int i = 0;; i++) {
            Object obj = this.elementAt(i);
            if (obj == null) break;
            if (!h.contains(obj)) h.add(obj);
        }
    }

    /**
     * Returns true if this Hub's objects are a subclass of OAObject
     */
    public boolean isOAObject() {
        return data.isOAObjectFlag();
    }

    /**
     * Returns the Class of the objects that are being stored in this Hub.
     */
    public Class<TYPE> getObjectClass() {
        return data.objClass;
    }

    /**
     * Finalize method.
     * 
     * @exception Throwable
     */
    protected void finalize() throws Throwable {
        super.finalize();
        
        Hub hx;
        if (this.datau != null) hx = this.datau.getSharedHub();
        else hx = null;
        
        if (hx != null) {
            HubShareDelegate.removeSharedHub(hx, this);
        }
        else {  
            HubSelectDelegate.cancelSelect(this, true);
            Vector vec = data.vector;
            if (vec != null) {
                try {
                    int x = vec.size();
                    for (int i=0; i<x; i++) {
                        Object obj = vec.get(i);
                        if (obj instanceof OAObject) {
                            OAObjectHubDelegate.removeHub((OAObject) obj, this, true);
                        }
                    }
                }
                catch (Exception e) {
                    //e.printStackTrace();
                    //System.out.println("Hub.finalize exception="+e);
                }
            }
        }
    }

    /**
     * @return true if more data is to be loaded from OASelect.
     */
    public boolean isMoreData() {
        return HubSelectDelegate.isMoreData(this);
    }

    public void loadAllData() {
        HubSelectDelegate.loadAllData(this);
    }

    /**
     * Number of Objects currently loaded in the collection.
     * <p>
     * Note: this is used to get the number of objects that are currently in the
     * Hub. If a select needs to be executed for this hub, it will not be done
     * when calling this method.
     */
    public int getCurrentSize() {
        return HubDataDelegate.getCurrentSize(this);
    }

    /**
     * Number of objects currently in collection. If a select needs to be
     * executed, it will be done and the total number from the select will be
     * returned.
     * <p>
     * Note: for database objects where all of the records have not been loaded,
     * OASelect.count() will be returned. Otherwise, the returned number will be
     * the number of objects currently in hub.
     * 
     * @see #getCurrentSize() for the amount that is physcially loaded
     */
    public int getSize() {
        return HubDelegate.getSize(this);
    }
    public int size() {
        return HubDelegate.getSize(this);
    }

    /**
     * Save all objects in this hub. If objects are OAObjects, then each object
     * save is called.
     * <p>
     * Note: this will abort if any of the objects throws an exception
     * @see OAObject#save
     */
    public void saveAll() {
        boolean b1 = OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        boolean b2 = OAThreadLocalDelegate.setAlwaysAllowEnabled(true);
        boolean b3 = OAThreadLocalDelegate.setAdmin(true);
        try {
            HubSaveDelegate.saveAll(this, OAObject.CASCADE_LINK_RULES);
        }
        finally {
            OAThreadLocalDelegate.setAlwaysAllowEditProcessed(b1);
            OAThreadLocalDelegate.setAlwaysAllowEnabled(b2);
            OAThreadLocalDelegate.setAdmin(b3);
        }
    }

    public void saveAll(int iCascadeRule) {
        HubSaveDelegate.saveAll(this, iCascadeRule);
    }

    /**
     * Delete all objects in this hub. If objects are OAObjects, then each
     * object delete method is called. Another option is to remove or
     * removeAll/clear the Hub and then have the objects deleted when it
     * master/owner object is saved.
     * <p>
     * Note: this does not abort if any of the objects cant be deleted.
     * 
     * @see OAObject#delete
     * @see Hub#clear() which will delete the objects during a save of parent
     *      object (based on cascade rules)
     * @see Hub#removeAll() which will delete the objects during a save of
     *      parent object (based on cascade rules)
     */
    public void deleteAll() {
        HubDeleteDelegate.deleteAll(this);
    }

    /**
     * Used to know if all of the objects in this Hub being deleted.
     * 
     * @return
     */
    public boolean isDeletingAll() {
        return HubDeleteDelegate.isDeletingAll(this);
    }

    /**
     * Creates a new Hub with the same objects that are in this Hub.
     * <p>
     * Note: dataunique (listeners, etc.) and dataactive (active object) are not
     * cloned. Objects are not cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        HubSelectDelegate.loadAllData(this);
        Hub h = new Hub(this.getObjectClass());
        HubDataDelegate._clone(this, h);
        return h;
    }

    /**
     * Used to mimic Hashtable.get(key). Find object with a matching key.
     * 
     * @param key
     *            hashkey of object to find. OAObject "hashCode()" and "equals"
     *            is used to find match
     * @return Object or null if not found. Note: you need to cast this value
     * @param key
     *            object to compare to, object or objects[] to compare this
     *            object's objectId(s) with or OAObjectKey to compare with this
     *            object's objectId
     * @see OAObjectKey#OAObjectKey
     */
    public TYPE getObject(Object key) {
        return (TYPE) HubDataDelegate.getObject(this, key);
    }

    /**
     * Get an object at a particular position, relative to 0
     * 
     * @param pos
     *            position of object to retreive, relative to 0
     * @return null if pos &gt; size()
     * @see #elementAt
     */
    public TYPE getObjectAt(int pos) {
        return (TYPE) HubDataDelegate.getObjectAt(this, pos);
    }

    public TYPE getAt(int pos) {
        return (TYPE) HubDataDelegate.getObjectAt(this, pos);
    }
    public TYPE getLast() {
        int pos = getSize() - 1;
        if (pos < 0) return null;
        return (TYPE) HubDataDelegate.getObjectAt(this, pos);
    }

    /**
     * Returns true if object exists in Hub.
     */
    public boolean contains(Object obj) {
        return HubDataDelegate.contains(this, obj);
    }

    /**
     * Finds the position of an object in this hub.
     * 
     * @see getPos(Object) which will adjust linkage hubs if necessary to find
     *      the object
     */
    public int indexOf(Object obj) {
        return HubDataDelegate.getPos(this, obj, false, false);
    }

    /**
     * Returns object at a particular position within the Hub. If position does
     * not exist, null is returned.
     * 
     * @see Hub#getObject
     */
    public TYPE elementAt(int pos) { // mimic Vector
        return (TYPE) HubDataDelegate.getObjectAt(this, pos);
    }

    /**
     * Navigational method used to retrieve the current active object, or null
     * if not set.
     * 
     * @see Hub#setActiveObject
     */
    public TYPE getActiveObject() {
        return (TYPE) dataa.activeObject;
    }

    public TYPE getAO() {
        return (TYPE) getActiveObject();
    }

    /**
     * Navigational method that will set the position of the active object. GUI
     * components use this to recongnize which object that they are working
     * with.
     * 
     * @param pos
     *            position to set. If &gt; size() or &lt; 0 then it will be set to
     *            null, and getPos() will return -1
     * @see Hub#getActiveObject
     */
    public TYPE setActiveObject(int pos) {
        return (TYPE) HubAODelegate.setActiveObject(this, pos);
    }

    public Object setAO(int pos) {
        return HubAODelegate.setActiveObject(this, pos);
    }

    /**
     * Navigational method to set the active object.
     * 
     * @param object
     *            Object to make active. If it does not exist in Hub, then
     *            active object will be set to null
     * @see Hub#getActiveObject
     */
    public void setActiveObject(Object object) {
        HubAODelegate.setActiveObject(this, object);
    }

    /**
     * Navigational method that sets the current active object.
     * 
     * @param object
     *            is object to make the active object.
     */
    public void setAO(Object object) {
        HubAODelegate.setActiveObject(this, object);
    }

    /**
     * Navigational method that resets the current active object.
     * 
     */
    public void resetAO() {
        HubAODelegate.setActiveObjectForce(this, getAO());
    }

    /**
     * If this is a recursive hub with an owner, then the root hub will be
     * returned, else null.
     */
    public Hub getRootHub() {
        return HubRootDelegate.getRootHub(this);
    }

    public void setRootHub() {
        HubRootDelegate.setRootHub(this, true);
    }

    /**
     * Hub used to add active object to whenever this active object is changed in
     * this Hub. This can be used for building a pick list type program, where a
     * user can select objects that are then added to a list.
     */
    public void setAddHub(Hub addHub) {
        datau.setAddHub(addHub);
        setAO(null);
    }

    /**
     * Returns the Hub that this Hub's active objects are added to.
     * 
     * @see #setAddHub
     */
    public Hub getAddHub() {
        return datau.getAddHub();
    }

    /**
     * If this is a shared Hub, returns the Hub that this is shared from, else
     * will return this Hub.
     */
    public Hub getRealHub() {
        Hub h = this;
        for (;;) {
            if (h.datau.getSharedHub() == null) {
                break;
            }
            h = h.datau.getSharedHub();
        }
        return h;
    }

    /**
     * Returns true if this hub of objects is owned by a master object.
     */
    public boolean isOwned() {
        return HubDetailDelegate.isOwned(this);
    }

    /**
     * This is a property that is unique within this Hub, not other Objects in
     * this Hub can have the same value.
     */
    public void setUniqueProperty(String propertyName) {
        HubDelegate.setUniqueProperty(this, propertyName);
    }

    /**
     * Navigational method that sets the defalut position of active object when
     * a new list is set. Can be set to 0 so that first object is always made
     * the active object whenever a new list is created. Default is -1 (set to
     * null).
     * <p>
     * This can be set for Detail Hubs, so that the first object is active
     * whenever a new list is create - which is when the Master Hub changes its
     * active object.
     */
    public void setDefaultPos(int pos) {
        datau.setDefaultPos(pos);
    }

    /**
     * Navigational method used to get the default position to set when a new
     * list is generated.
     * 
     * @see #setDefaultPos
     */
    public int getDefaultPos() {
        return datau.getDefaultPos();
    }

    /**
     * Navigational method used to set the active object to the object at
     * specific position. If position is &lt; 0 or there is not an object at
     * position, then the active object will be set to null.
     * 
     * @see Hub#setActiveObject
     */
    public TYPE setPos(int pos) {
        return setActiveObject(pos);
    }

    /**
     * Navigational method used to set the active object to the object at
     * specific position. If the active object is null, then -1 will be
     * returned.
     * 
     * @see Hub#setActiveObject
     */
    public int getPos() {
        int result = getPos(dataa.activeObject);
        return result;
    }

    /**
     * Returns the position of an object within the Hub.
     * 
     * @param object
     *            Object to located. This can also be the value of the objectId
     *            for the object to find. If the object is not found and there
     *            is a masterObject, then the active object in the master Hub
     *            could be changed so that the object can then be found in this
     *            Hub.
     * @return position of object in the Hub, else -1.
     */
    public int getPos(Object object) {
        // 20150203 changed to not update master/detail if object is not in this hub
        return HubDataDelegate.getPos(this, object, false, false);
        //was: return HubDataDelegate.getPos(this, object, true, true);
    }
    public int getPos(Object object, boolean bAdjustMaster) {
        return HubDataDelegate.getPos(this, object, bAdjustMaster, false);
    }

    /**
     * Add an Object to end of collection. All listeners will be notified of add
     * event.
     * <p>
     * If Hub is sorted, then object will be inserted at correct/sorted
     * position.
     * 
     * @param obj
     *            Object to add, must be from the same class that was used when
     *            creating the Hub
     * return true if object was added else false (Hub.canAdd(obj) returned false
     */
    @Override
    public boolean add(TYPE obj) {
        return HubAddRemoveDelegate.add(this, obj);
    }

    public void add(Hub<TYPE> hub) {
        if (hub == null) return;
        boolean b = (getSize() == 0);
        if (b) OAThreadLocalDelegate.setLoading(true);
        try {
            for (TYPE obj : hub) {
                HubAddRemoveDelegate.add(this, obj);
            }
        }
        finally {
            if (b) {
                OAThreadLocalDelegate.setLoading(false);
                HubEventDelegate.fireOnNewListEvent(hub, true);
            }
        }
    }
    
    /**
     * Flag to know if add/remove are enabled
     */
    public boolean getEnabled() {
        if (data.isDisabled()) return false;
        return OAObjectEditQueryDelegate.getAllowEnabled(this);
    }
    public void setEnabled(boolean b) {
        this.data.setDisabled(!b);
    }
    
    /**
     * Helper method that mimics Vector.addElement(), it calls Hub.add(obj,
     * false)
     * 
     * @see #add(Object)
     */
    public void addElement(TYPE obj) {
        HubAddRemoveDelegate.add(this, obj);
    }

    /**
     * Swap the positon of two different objects within the hub. This will call
     * the move method.
     * 
     * @param pos1
     *            position of object to move from, if there is not an object at
     *            this position, then no move is performed.
     * @param pos2
     *            position of object to move to, if there is not an object at
     *            this position, then no move is performed.
     * @see #move
     */
    public void swap(int pos1, int pos2) {
        HubAddRemoveDelegate.swap(this, pos1, pos2);
    }

    /**
     * Swap the positon of two different objects within the hub. This will call
     * the move method. Sends a hubMove event to all HubListeners.
     * 
     * @param posFrom
     *            position of object to move, if there is not an object at this
     *            position, then no move is performed.
     * @param posTo
     *            the new ending position for object to be in, <b>after</b> the
     *            object is removed from posFrom
     */
    public void move(int posFrom, int posTo) {
        HubAddRemoveDelegate.move(this, posFrom, posTo);
    }

    /**
     * Replace an existing object with a new one within the Hub. This will send
     * a hubReplace() event. Sends a hubReplace Event to all HubListeners.
     * 
     * @param oldObj
     *            object to remove, if not found then no replace is performed.
     * @param newObj
     *            object to replace oldObj with.
     * 
     *            2007/10/28 taken out, not really needed. This will need to
     *            also update masterObject property public void replace(Object
     *            oldObj, Object newObj) { HubAddRemoveDelegate.replace(this,
     *            oldObj, newObj); }
     */

    /**
     * Insert an Object at a position. Hub Listeners will be notified with an
     * insert event.
     * <p>
     * If Hub is sorted, then object will be inserted at correct/sorted
     * position.
     * 
     * @param obj
     *            Object to insert, must be from the same class that was used
     *            when creating the Hub
     * @param pos
     *            position to insert the object into the Hub. If greater then
     *            size of Hub, then it will be added to the end.
     * @return true if object was added else false (event hubBeforeAdd() threw
     *         an exception)
     * @see #getObjectClass
     * @see #add
     * @see #sort
     */
    public boolean insert(TYPE obj, int pos) {
        return HubAddRemoveDelegate.insert(this, obj, pos);
    }

    /**
     * This will remove an object from this collection and send a remove event
     * to all HubListeners.
     */
    public boolean remove(Object obj) {
        return HubAddRemoveDelegate.remove(this, obj);
    }

    /**
     * This will remove the object at a position from this collection and send a
     * remove event to all HubListeners.
     * 
     * @param pos
     *            position of object to remove. If an object does not exist at
     *            this position, then no action is taken.
     */
    @Override
    public TYPE remove(int pos) {
        return (TYPE) HubAddRemoveDelegate.remove(this, pos);
    }

    public TYPE removeAt(int pos) {
        return (TYPE) HubAddRemoveDelegate.remove(this, pos);
    }

    /**
     * Flag used to have the active object set to null when active object is
     * removed from Hub (default=false).
     * 
     * @param b
     *            if true, then active object will be set to null. If false,
     *            then the active object will be set to next object. If next
     *            object does not exist, then previous object is set.
     */
    public void setNullOnRemove(boolean b) {
        datau.setNullOnRemove(b);
    }

    /**
     * Flag used to have the active object set to null when active object is
     * removed from Hub.
     * 
     * @see #setNullOnRemove
     */
    public boolean getNullOnRemove() {
        return datau.isNullOnRemove();
    }

    /**
     * Removes all objects from this Hub. Sends a hubRemoveAll event before
     * clearing. Calls remove() for each object, but does not send a hubRemove
     * HubEvent. Sends a hubNewList event when finished. Same as removeAll,
     * removeAllElements
     * 
     * @see #remove
     */
    public void clear() {
        HubAddRemoveDelegate.clear(this);
    }

    /**
     * Removes all objects from this Hub. Sends a hubRemoveAll event before
     * clearing. Calls remove() for each object, but does not send a hubRemove
     * HubEvent. Sends a hubNewList event when finished. Same as removeAll,
     * removeAllElements
     * 
     * @see #remove
     */
    public void removeAll() {
        HubAddRemoveDelegate.clear(this);
    }

    /**
     * Navigational method used to create a shared version of another Hub, so
     * that this Hub will use the same objects as the shared hub. All events
     * that affect the data will be sent to all shared Hubs.
     * 
     * @see SharedHub
     */
    public Hub<TYPE> createSharedHub() {
        return HubShareDelegate.createSharedHub(this, false);
    }

    /**
     * Create a shared version of another Hub, so that this Hub will use the
     * same objects as the shared hub. All events that affect the data will be
     * sent to all shared Hubs.
     * 
     * @param bShareAO
     *            true=use same activeObject as shared hub, false:use seperate
     *            activeObject
     * @see SharedHub
     */
    public Hub<TYPE> createSharedHub(boolean bShareAO) {
        return HubShareDelegate.createSharedHub(this, bShareAO);
    }

    /**
     * Create a shared version of another Hub, so that this Hub will use the
     * same objects as the shared hub. All events that affect the data will be
     * sent to all shared Hubs.
     * 
     * @param masterHub
     *            is the source Hub that this Hub will share data from.
     * @param bShareAO
     *            true=use same activeObject as shared hub, false:use seperate
     *            activeObject
     */
    public void setSharedHub(Hub<TYPE> masterHub, boolean bShareAO) {
        HubShareDelegate.setSharedHub(this, masterHub, bShareAO);
    }

    /**
     * Create a shared version of another Hub, so that this Hub will use the
     * same objects as the shared hub. All events that affect the data will be
     * sent to all shared Hubs.
     * 
     * @param masterHub
     *            is the source Hub that this Hub will share data from.
     */
    public void setSharedHub(Hub<TYPE> masterHub) {
        HubShareDelegate.setSharedHub(this, masterHub, false);
    }

    /**
     * Returns the Hub that this Hub is sharing objects with. Same as getShared.
     */
    public Hub<TYPE> getSharedHub() {
        return datau.getSharedHub();
    }

    /**
     * Used to create Master/Detail relationships.
     * <p>
     * Full Description<br>
     * This will create and return a Hub that will automatically be populated
     * with the objects from a reference property from this Hubs active object.
     * Whenever this Hub's active object is changed, then the Detail Hub will
     * have its list changed to match the objects in the reference property.
     * <p>
     * Example:<br>
     * A Department Class that has a method name getEmployees() that will return
     * a Hub of all Employees that belong to the Department.
     * 
     * <pre>
     * Hub hubDept = new Hub(Department.class); // create new Hub for Department objects
     * hubDept.select(); // select all departments from datasource
     * Hub hubEmp = hubDept.getDetail(Employee.class); // this will find the method in
     * // Department Class that returns
     * // Employee Class objects
     * // Or
     * Hub hubEmp = hubDept.getDetail(&quot;Employees&quot;); // this will use the getEmployees()
     * // method in Department.
     * // Or
     * Hub hubEmp = hubDept.getDetail(&quot;Employees&quot;, &quot;lastName, firstName&quot;); // sets sort order
     * 
     * // Or any other of the getDetail() methods
     * 
     </pre>
     * 
     * This getDetail() example creates a Hub that will be populated with the
     * Employee objects from the current active object in the hubDept Hub. When
     * the active object is changed in hubDept, the hubEmp Hub will be
     * automatically updated by calling the new active object Department object
     * to get the Employee objects. A new list event will be sent to listeners
     * for the hubEmp whenever this happens.
     * <p>
     * Note: If the property being used for the Detail Hub is also a Hub, then
     * the Detail Hub will call setSharedHub() to use the same objects.
     * <p>
     * same as getDetailHub()
     * <p>
     * <b>Note:</b> see DetailHub Class {@link DetailHub}
     * 
     * @param path
     *            is the property path to follow to get to the object property
     *            needed for the detail Hub.
     * @param bShareActive
     *            is used to determine if the active object for hub that is
     *            being used (shared) is going to be the same for new Detail
     *            Hub. (default=false). In the example above, each Department
     *            object has a Hub of Employee Objects. If bShareActive=true,
     *            then the Detail Hub will use the same active object as the one
     *            in the Hub that it is sharing.
     * @param selectOrder is the sort order to select objects in.
     * @see #setSharedHub
     * @see DetailHub
     */
    public Hub getDetailHub(String path, boolean bShareActive, String selectOrder) {
        return HubDetailDelegate.getDetailHub(this, path, bShareActive, selectOrder);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(String path, boolean bShareActive) {
        return HubDetailDelegate.getDetailHub(this, path, bShareActive);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(String path, String selectOrder) {
        return HubDetailDelegate.getDetailHub(this, path, selectOrder);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(String path) {
        Hub h = HubDetailDelegate.getDetailHub(this, path);
        return h;
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(String path, Class objectClass, boolean bShareActive) {
        return HubDetailDelegate.getDetailHub(this, path, objectClass, bShareActive);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(String path, Class objectClass) {
        return HubDetailDelegate.getDetailHub(this, path, objectClass, false);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(Class clazz, boolean bShareActive, String selectOrder) {
        return HubDetailDelegate.getDetailHub(this, clazz, bShareActive, selectOrder);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(Class clazz, boolean bShareActive) {
        return HubDetailDelegate.getDetailHub(this, clazz, bShareActive, null);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(Class clazz, String selectOrder) {
        return HubDetailDelegate.getDetailHub(this, clazz, false, selectOrder);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(Class clazz) {
        return HubDetailDelegate.getDetailHub(this, clazz, false, null);
    }

    /**
     * Used to create Master/Detail relationships.
     */
    public Hub getDetailHub(Class[] classes) {
        return HubDetailDelegate.getDetailHub(this, classes);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     * 
     */
    public void setMasterHub(Hub masterHub) {
        HubDetailDelegate.setMasterHub(this, masterHub, null, false, null);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     * 
     */
    public void setMasterHub(Hub masterHub, boolean bShared) {
        HubDetailDelegate.setMasterHub(this, masterHub, null, bShared, null);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     * 
     * @param path
     *            is the property path from masterHub to get to this hub
     */
    public void setMasterHub(Hub masterHub, String path) {
        HubDetailDelegate.setMasterHub(this, masterHub, path, false, null);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     * 
     * @param path
     *            is the property path from masterHub to get to this hub
     */
    public void setMasterHub(Hub masterHub, String path, boolean bShared) {
        HubDetailDelegate.setMasterHub(this, masterHub, path, false, null);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     */
    public void setMasterHub(Hub masterHub, Class clazz, String path, boolean bShared, String selectOrder) {
        HubDetailDelegate.setMasterHub(this, masterHub, path, false, selectOrder);
    }

    /**
     * Used to create Master/Detail relationships. Set the controlling/master
     * hub for this hub
     */
    public Hub getMasterHub() {
        return HubDetailDelegate.getMasterHub(this);
    }

    public OAObject getMasterObject() {
        return HubDetailDelegate.getMasterObject(this);
    }

    public Class getMasterClass() {
        return HubDetailDelegate.getMasterClass(this);
    }

    /**
     * Returns true if this Hub has any detail hubs created.
     */
    public boolean hasDetailHubs() {
        int x = (datau.getVecHubDetail() == null) ? 0 : datau.getVecHubDetail().size();
        return x > 0;
    }

    /**
     * Used to remove Master/Detail relationships.
     */
    public boolean removeDetailHub(Hub hub) {
        return HubDetailDelegate.removeDetailHub(this, hub);
    }

    /**
     * Add a Listener to this hub specifying a specific property name. If
     * property is a calcualated property, then the Hub will automatically set
     * up internal listeners to know when the calculated property changes.
     * 
     * @param property
     *            name to listen for
     */
    public void addHubListener(HubListener<TYPE> hl, String property) {
        HubEventDelegate.addHubListener(this, hl, property);
    }
    public void addHubListener(HubListener<TYPE> hl, String property, boolean bActiveObjectOnly) {
        HubEventDelegate.addHubListener(this, hl, property, bActiveObjectOnly);
    }
    public void addHubListener(HubListener<TYPE> hl, boolean bActiveObjectOnly) {
        HubEventDelegate.addHubListener(this, hl, bActiveObjectOnly);
    }

    public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths) {
        HubEventDelegate.addHubListener(this, hl, property, dependentPropertyPaths);
    }
    public void addHubListener(HubListener<TYPE> hl, String property, String dependentPropertyPath) {
        String[] ss;
        if (dependentPropertyPath != null && dependentPropertyPath.length() > 0) {
            ss = new String[] {dependentPropertyPath};
        }
        else ss = null;
        HubEventDelegate.addHubListener(this, hl, property, ss);
    }
    public void addHubListener(HubListener<TYPE> hl, String property, String dependentPropertyPath, boolean bActiveObjectOnly) {
        String[] ss;
        if (dependentPropertyPath != null && dependentPropertyPath.length() > 0) {
            ss = new String[] {dependentPropertyPath};
        }
        else ss = null;
        HubEventDelegate.addHubListener(this, hl, property, ss, bActiveObjectOnly);
    }
    public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly) {
        HubEventDelegate.addHubListener(this, hl, property, dependentPropertyPaths, bActiveObjectOnly);
    }
    public void addHubListener(HubListener<TYPE> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread) {
        HubEventDelegate.addHubListener(this, hl, property, dependentPropertyPaths, bActiveObjectOnly, bUseBackgroundThread);
    }

    // 20160827
    /**
     * add a trigger to this hub that will send a property change event.
     * @param hl listener
     * @param property name of property for afterPropertyChange event
     * @param propertyPath to listen to
     */
    public void addTriggerListener(HubListener<TYPE> hl, final String property, String propertyPath) {
        OATriggerListener tl = new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) throws Exception {
                HubEventDelegate.fireCalcPropertyChange(Hub.this, obj, property);
            }
        };
        OATrigger trigger = OATriggerDelegate.createTrigger(property, getObjectClass(), tl, new String[] {propertyPath}, true, false, false, true);
    }
    public void addTriggerListener(HubListener<TYPE> hl, final String property, String propertyPath, boolean useBackgroundThread) {
        OATriggerListener tl = new OATriggerListener() {
            @Override
            public void onTrigger(OAObject obj, HubEvent hubEvent, String propertyPath) throws Exception {
                HubEventDelegate.fireCalcPropertyChange(Hub.this, obj, property);
            }
        };
        OATrigger trigger = OATriggerDelegate.createTrigger(property, getObjectClass(), tl, new String[] {propertyPath}, true, false, useBackgroundThread, true);
    }
    
    
    /**
     * Add a Listener to this hub specifying a specific property name and list
     * of property paths. The Hub will automatically set up internal listeners
     * to know when the any of the properties change and then send a property
     * change event based on the property name used.
     * 
     * @param listener
     *            HubListener object
     * @param property
     *            name that will be used when sending property change event.
     *            Note: this does not need to be a valid property for the class.
     * @see HubEvent
     */
    /*
     * qqq??? not sure if this is being used public void
     * addHubListener(HubListener hl, String property, String[] properties) {
     * HubEventDelegate.addHubListener(this, hl, property, properties); }
     */
    /**
     * Add a Listener to this Hub.
     */
    public void addHubListener(HubListener<TYPE> hl) {
        HubEventDelegate.addHubListener(this, hl);
    }
    public void addListener(HubListener<TYPE> hl) {
        HubEventDelegate.addHubListener(this, hl);
    }

    /**
     * Remove a Listener to this Hub.
     */
    public void removeHubListener(HubListener<TYPE> hl) {
        HubEventDelegate.removeHubListener(this, hl);
    }
    public void removeListener(HubListener<TYPE> hl) {
        HubEventDelegate.removeHubListener(this, hl);
    }

    /**
     * Remove a Listener to this Hub.
     * 
     * @param listener
     *            HubListener object
     * @see #addHubListener
     */
    /*
     * 20101218 replaced by HubListenerTree
     * 
     * public void removeHubListener(HubListener hl, String property) {
     * HubEventDelegate.removeHubListener(this, hl, property); }
     */
    /**
     * Used to update a property in each object to equal/store the position
     * within this Hub.
     * 
     * @param property
     *            is neme of property to update.
     */
    public void setAutoSequence(String property) {
        this.setAutoSequence(property, 0);
    }

    /**
     * Used to update a property in each object to equal/store the position
     * within this Hub.
     * 
     * @param property
     *            is neme of property to update.
     * @param startNumber
     *            is number to begin numbering at. Default is 0, which will
     *            match the Hub position.
     */
    public void setAutoSequence(String property, int startNumber) {
        HubDelegate.setAutoSequence(this, property, startNumber, true);
    }

    public void resequence() {
        HubDelegate.resequence(this);
    }
    
    /**
     * Used to update a property in each object to equal/store the position
     * within this Hub.
     * 
     * @param property
     *            is name of property to update.
     * @param startNumber
     *            is number to begin numbering at. Default is 0, which will
     *            match the Hub position.
     * @param bKeepSeq
     *            , if false then seq numbers are not updated when an object is
     *            removed
     */
    public void setAutoSequence(String property, int startNumber, boolean bKeepSeq) {
        HubDelegate.setAutoSequence(this, property, startNumber, bKeepSeq);
    }

    /**
     * Makes sure that for each object in a hubMaster, there exists an object in
     * this hub where property is equal to the hubMaster object.
     * 
     * @param hubMaster
     *            hub with list of object
     * @param property
     *            Property in this hubs objects that match object type in
     *            hubMaster
     */
    public void setAutoMatch(String property, Hub hubMaster) {
        HubDelegate.setAutoMatch(this, property, hubMaster, false);
    }
    public void setAutoMatch(String property, Hub hubMaster, boolean bServerSideOnly) {
        HubDelegate.setAutoMatch(this, property, hubMaster, bServerSideOnly);
    }

    /**
     * Makes sure that for each object in a hubMaster, there exists an object in
     * this hub.
     * 
     * @param hubMaster
     *            hub with list of object
     */
    public void setAutoMatch(Hub hubMaster) {
        HubDelegate.setAutoMatch(this, null, hubMaster, false);
    }
    public void setAutoMatch(Hub hubMaster, boolean bServerSideonly) {
        HubDelegate.setAutoMatch(this, null, hubMaster, bServerSideonly);
    }

    /**
     * Reorder objects in Hub using a custom Comparator.
     * 
     * @param comp
     *            is comparator that has callback method used to sort objects in
     *            this Hub.
     */
    public void sort(Comparator comp) {
        HubSortDelegate.sort(this, null, true, comp);
    }

    /**
     * Reorder objects in this Hub, sorted by the value(s) from propertyPath(s).
     */
    public void sort(String... propertyPaths) {
        String s = "";
        for (int i = 0; propertyPaths != null && i < propertyPaths.length; i++) {
            if (propertyPaths[i] == null) continue;
            if (s.length() > 0) s += ", ";
            s += propertyPaths[i];
        }
        HubSortDelegate.sort(this, s, true, null);
    }

    /**
     * Sorts objects by propertyPath and calls hubAfterSort() method from
     * listeners. All objects that are added or inserted will automatically be
     * placed in sort order.
     * <p>
     * Note: a HubSorter is used to continually keep the objects sorted. To
     * remove HubSorter, call cancelSort
     * 
     * 
     * example:
     * hubEmp.sort("lastName, firstName")
     * hubEmp.sort("lastName, age desc, weight desc")
     * 
     * @param propertyPaths, list of propertypaths, separated by comma, can also include " asc" or " desc" to override sort ordering.
     * @param bAscending true=sort ascending, false=descending
     */
    public void sort(String propertyPaths, boolean bAscending) {
        HubSortDelegate.sort(this, propertyPaths, bAscending, null);
    }

    /**
     * Reorder objects in this Hub, sorted by the value(s) from propertyPath(s).
     * 
     * @see sort(String,boolean)
     * see HubSorter
     * see #cancelSort
     */
    public void sort(String propertyPaths, boolean bAscending, Comparator comp) {
        HubSortDelegate.sort(this, propertyPaths, bAscending, comp);
    }

    /**
     * Used to keep objects sorted based on last call to select method. By
     * default, the sort order used in a select is not maintained within the
     * Hub. This method will keep the objects sorted using the same property
     * paths used by select.
     */
/**  20150810 removed, sort will keepSorted by default  
    public void keepSorted() {
        HubSortDelegate.keepSorted(this);
    }
*/
    /**
     * Returns true if this Hub has a sorter that is keeping it sorted.
     */
    public boolean isSorted() {
        return HubSortDelegate.isSorted(this);
    }

    /**
     * Removes/disconnects HubSorter (if any) that is keeping objects in a
     * sorted order.
     * 
     * @see sort(String,boolean)
     */
    public void cancelSort() {
        HubSortDelegate.cancelSort(this);
    }

    /**
     * Re-sort using parameters from last sort or select.
     */
    public void sort() {
        HubSortDelegate.sort(this);
    }

    /**
     * Re-sort using last parameters from last sort or select.
     */
    public void resort() {
        HubSortDelegate.sort(this);
    }

    /**
     * Finds first object that has a property that matches a value. 
     * The comparison is done using OACompare.like(..)
     * 
     * @param propertyPath
     *            property to use to find value
     * @param findValue value to find
     */
    public TYPE find(String propertyPath, Object findValue) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, false, null);
    }
    public TYPE find(String propertyPath, Object findValue, boolean bSetAO) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, bSetAO, null);
    }
    public TYPE find(TYPE fromObject, String propertyPath, Object findValue) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, false, (OAObject) fromObject);
    }
    public TYPE find(TYPE fromObject, String propertyPath, Object findValue, boolean bSetAO) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, bSetAO, (OAObject) fromObject);
    }

    public TYPE findNext(TYPE fromObject, String propertyPath, Object findValue) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, false, (OAObject) fromObject);
    }
    public TYPE findNext(TYPE fromObject, String propertyPath, Object findValue, boolean bSetAO) {
        return (TYPE) HubFindDelegate.findFirst(this, propertyPath, findValue, bSetAO, (OAObject) fromObject);
    }
    
    

    /**
     * Sets ActiveObject to next object in Hub that has property equal to
     * findObject. Starts at the next object. Set to null if eof.
     * <p>
     * Warning: this is not thread safe. This will use the search property and
     * object from last call to find, findFirst, findLast method.
     * 
     * @see findPrevious(HubFinder)
     */
/*    
    public TYPE findNext(boolean bSetAO) {
        return (TYPE) HubFindDelegate.findNext(this, bSetAO);
    }
*/
    /**
     * WHERE clause to use for select.
     * 
     * @see #setSelectOrder
     * @see OASelect
     */
    public void setSelectWhere(String s) {
        HubSelectDelegate.setSelectWhere(this, s);
    }

    public String getSelectWhere() {
        return HubSelectDelegate.getSelectWhere(this);
    }

    /**
     * Sort Order clause to use for select.
     * 
     * @see #getSelectOrder
     * @see OASelect
     */
    public void setSelectOrder(String s) {
        HubSelectDelegate.setSelectOrder(this, s);
    }

    /**
     * Sort Order clause to use for select.
     * 
     * @see #setSelectOrder
     * @see OASelect
     */
    public String getSelectOrder(Hub thisHub) {
        return HubSelectDelegate.getSelectOrder(this);
    }

    /**
     * Select all objects from OADataSource that have a reference to parameter
     * "object".
     * <p>
     * example: emp.select(dept); select all employees in Department "dept"
     * 
     * @see OASelect
     */
    public void select(OAObject whereObject, String orderByClause) {
        HubSelectDelegate.select(this, whereObject, null, null, orderByClause, false);
    }

    /**
     * Used for retrieving all objects from OADataSource. If this hub has a
     * masterObject, then this will call select(getMasterObject()).
     * <p>
     * Note: orderBy clause is blank, there will not be a sort order.
     * 


     * @see OASelect
     */
    public void select() {
        HubSelectDelegate.select(this, false);
    }

    /**
     * Used for generating query to retrieve objects from OADataSource. If this
     * hub has a masterObject, then this will call select(getMasterObject(),
     * clause) where clause is the orderBy.
     * <p>
     * If this hub does not have a masterObject, then it will select all objects
     * and the supplied clause will be used for the where clause.
     * 



     */
    public void select(String whereClause) {
        HubSelectDelegate.select(this, null, whereClause, null, null, false);
    }

    public void select(String whereClause, Object[] params) {
        HubSelectDelegate.select(this, null, whereClause, params, null, false);
    }

    /**
     * Used for generating query to retrieve objects from OADataSource. Example:<br>
     * <code>
        empHub.select("dept.manager.name LIKE 'V%' AND order.items.product.vendor = 123")
        </code>
     * 
     * @param whereClause
     *            string used for generating SQL WHERE
     * @param orderBy
     *            string used for generating SQL ORDER BY
     * @see OASelect
     */
    public void select(String whereClause, String orderBy) {
        HubSelectDelegate.select(this, null, whereClause, null, orderBy, false);
    }
    public void select(String whereClause, String orderBy, OAFilter filter) {
        HubSelectDelegate.select(this, null, whereClause, null, orderBy, false, filter);
    }

    /**
     * Used for generating query to retrieve objects from OADataSource. Example:<br>
     * <code>
	    empHub.select("dept.manager.name LIKE 'V%' AND order.items.product.vendor = 123")
	    </code>
     * 
     * @param whereClause
     *            string used for generating SQL WHERE
     * @param orderBy
     *            string used for generating SQL ORDER BY
     * @see OASelect
     */
    public void select(String whereClause, Object[] whereParams, String orderBy, OAFilter filter) {
        HubSelectDelegate.select(this, null, whereClause, whereParams, orderBy, false, filter);
    }
    public void select(String whereClause, Object[] whereParams, String orderBy) {
        HubSelectDelegate.select(this, null, whereClause, whereParams, orderBy, false);
    }

    public void select(String whereClause, Object whereParam, String orderBy) {
        Object[] params = null;
        if (whereParam != null) {
            params = new Object[] { whereParam };
        }
        HubSelectDelegate.select(this, null, whereClause, params, orderBy, false);
    }
    public void select(String whereClause, Object whereParam, String orderBy, OAFilter filter) {
        Object[] params = null;
        if (whereParam != null) {
            params = new Object[] { whereParam };
        }
        HubSelectDelegate.select(this, null, whereClause, params, orderBy, false, filter);
    }

    /**
     * Used to populate Hub with objects returned from a OADataSource select. By
     * defalut, all objects will first be removed from the Hub,
     * OASelect.select() will be called, and the first 45 objects will be added
     * to Hub and active object will be set to null. As the Hub is accessed for
     * more objects, more will be returned until the query is exhausted of
     * objects.
     * 
     * @see OASelect
     * @see #loadAllData
     * @see #isMoreData
     */
    public void select(OASelect select) { // This is the main select method for
        // Hub that all of the other select
        // methods call.
        HubSelectDelegate.select(this, select);
    }

    /**
     * Send the query to OADataSource.selectPassthru.
     * 
     * @param whereClause
     *            is where clause in native query language used by datasource,
     *            must start with "FROM tableName".
     * @param orderClause
     *            is sort order in native query language used by datasource.
     * @see OASelect
     */
    public void selectPassthru(String whereClause, String orderClause) {
        HubSelectDelegate.selectPassthru(this, whereClause, orderClause);
    }

    /**
     * Returns the OASelect object currently used by select().
     */
    public OASelect getSelect() {
        return HubSelectDelegate.getSelect(this);
    }
    public OASelect getSelect(boolean bCreateIfNull) {
        OASelect sel = HubSelectDelegate.getSelect(this, true);
        return sel;
    }

    /**
     * Cancel the reading of anymore records from OADataSource.
     */
    public void cancelSelect() {
        HubSelectDelegate.cancelSelect(this, true);
    }

    /**
     * This will re-run the last select.
     * 
     * @see OASelect
     */
    /* 20160421, removed so that an exhausted select can be removed and Hub.data.datax can be freed
    public void refreshSelect() {
        HubSelectDelegate.refreshSelect(this);
    }
    */

    /** 20181211 removed, use getLinkHub(boolean) instead
     * Returns the Hub that this Hub is linked to.
     * 
     * @see HubLink
     * @see #getLinkHub(boolean, boolean)
     * @deprecated use {@link #getLinkHub(boolean)} instead

    public Hub getLinkHub() {
        return datau.getLinkToHub();
    }
    */
    
    /**
     * Find the linkHub for this hub or any of this hub's shared hubs that have a linkHub.
     * @param bSearchOtherHubs also check any shared or copied/filtered hubs that use the same AO
     * @see HubLinkDelegate#getHubWithLink(Hub, boolean) for other options
     */
    public Hub getLinkHub(boolean bSearchOtherHubs) {
        if (!bSearchOtherHubs) {
            return this.datau.getLinkToHub();
        }
        Hub hx = HubLinkDelegate.getHubWithLink(this, true);
        if (hx == null) return null;
        return hx.datau.getLinkToHub();
    }
    

    /**
     * Set the property in a Hub to the position of the active object in this
     * Hub.
 


     */
    public void setLinkHubOnPos(Hub linkHub, String property) {
        setLinkHub(null, linkHub, property, true);
    }

    /**
     * Sets up this Hub so that it will automatically work with a property of
     * the same Class in another (link) Hub. This <i>linking</i> will
     * automatically set the active object in this Hub to the value of the
     * property in the active object in the Hub that is being linked to.
     * Whenever the active object is changed in the link Hub, the active object
     * in this Hub will be changed to the same value as the link property in the
     * link Hub.
     * <p>
     * When the active object is changed in this Hub, the property value for the
     * link Hub will be changed to the value of the active object.
     * <p>
     * Types of linking:<br>
     * 1: link the active object in this Hub to a property in another Hub, where
     * the property type is the same Class as the objects in this Hub.<br>
     * 2: link the position of the active object in this Hub to a property
     * (numeric) in another Hub.<br>
     * 3: link a property in this Hub to a property in another Hub.<br>
     * 4: a link that will automatically create a new object in the link Hub and
     * set the link property whenever the active object in this Hub is changed.
     * 
     * <p>
     * Examples:<br>
     * 
     * <pre>
     * // Link department Hub to the department property in a Employee Hub
     * Hub hubDept = new Hub(Department.class);   // create new Hub for Department objects
     * hubDept.select();      // select all departments from datasource
     * Hub hubEmp = new Hub(Employee.class);
     * hubEmp.select();   // select all employees from datasource
     * hubDept.setLink(hubEmp, &quot;Department&quot;);
     * -or-
     * hubDept.setLink(hubEmp);
     * -or-
     * hubDept.setLink(hubEmp, Employee.class);
     * 
     * // Link the position of a value to a property in another Hub
     * Hub hub = new Hub(String.class);
     * hub.add(&quot;Yes&quot;);
     * hub.add(&quot;No&quot;);
     * hub.add(&quot;Maybe&quot;);
     * hub.setLinkOnPos(hubEmployee, &quot;retiredStatus&quot;);  // values will be set to 0,1, or 2
     * 
     * // Link a the property value of active object to a property in the link Hub
     * Hub hub = new Hub(State.class);  // Class that stores information about all 50 states
     * hub.select();   // select all from OADataSource
     * hub.setLink(&quot;stateName&quot;, hubEmp, &quot;state&quot;);  // set the state property to name of state
     * 
     * // automatically create an object and set link property when active object is changed
     * Hub hubItem = new Hub(Item.class);
     * Hub hubOrder = new Hub(Order.class);
     * Hub hubOrderItem = hubOrder.getDetail(OrderItem.class);  // create detail Hub for
     *                                                          // order items
     * hubItem.setLink(hubOrderItem, true);  // whenever hubItem's active object is
     *                                       // changed, a new OrderItem object will
     *                                       // be created with a reference to the
     * // selected Item object.
     * 
     * </pre>
     * &lt;br&gt;
     * 
     *         &#064;param linkHub hub that this hub will change
     *         &#064;param property name of property in linkHub that will get changed.  If this is not
     *         supplied,then it will be found using OAObjectInfo, OALinkInfo or Reflection
     * @see HubLink
     */
    public void setLinkHub(Hub linkHub, String property) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, property, false, false, false);
    }

    /**
     * Have this Hub linked to another Hub. If property is not known, then
     * OAObjectInfo, OALinkInfo will be used to find the correct property to
     * link to.
 



     */
    public void setLinkHub(Hub linkHub) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, null, false, false, false);
    }

    /**
     * Link/Connect a property in this hub to a property in another hub.
     * 
     * @param fromProperty
     *            is property active object of this Hub
     * @param toProperty
     *            is property in link Hub to set.
     * @see Hub#setLinkHub(Hub, boolean, boolean) Full Description of Linking Hubs
     * @see HubLink
     */
    public void setLinkHub(String fromProperty, Hub linkHub, String toProperty) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, fromProperty, linkHub, toProperty, false, false, false);
    }

    /**
     * Remove the link that this Hub has with another Hub.
     */
    public void removeLinkHub() {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, null, null, false, false, false);
    }

    /**
     * Used to automatically create a new Object in link Hub whenever the active
     * object in this Hub is changed.
     * 
     * @param bAutoCreate
     *            if true then a new object will be created and added to
     *            linkHub.
     */
    public void setLinkHub(Hub linkHub, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, null, false, bAutoCreate, bAutoCreateAllowDups);
    }

    public void setLinkHub(Hub linkHub, boolean bAutoCreate) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, null, false, bAutoCreate, false);
    }

    /**
     * Used to automatically create a new Object in link Hub whenever the active
     * object in this Hub is changed.
     * 
     * @param bAutoCreate
     *            if true then a new object will be created and added to
     *            linkHub.
     * @param property
     *            is name of property in link Hub that will be set.
     */
    public void setLinkHub(Hub linkHub, String property, boolean bAutoCreate, boolean bAutoCreateAllowDups) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, property, false, bAutoCreate, bAutoCreateAllowDups);
    }

    public void setLinkHub(Hub linkHub, String property, boolean bAutoCreate) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, null, linkHub, property, false, bAutoCreate, false);
    }

    /**


     */
    protected void setLinkHub(String propertyFrom, Hub linkHub, String propertyTo, boolean linkPosFlag) {
        // setLinkHub(Hub thisHub, String propertyFrom, Hub linkHub, String
        // propertyTo, boolean linkPosFlag, boolean bAutoCreate) {
        HubLinkDelegate.setLinkHub(this, propertyFrom, linkHub, propertyTo, linkPosFlag, false, false);
    }

    // 2008/01/02 all of these were created to support the old oa.html package
    public boolean isValid() {
        return HubDelegate.isValid(this);
    }

    /**
     * @return path that this hub is linked to.
     */
    public String getLinkPath(boolean bSearchOtherHubs) {
        return HubLinkDelegate.getLinkHubPath(this, bSearchOtherHubs);
    }

    public static OAObjectInfo getOAObjectInfo(Class c) {
        return OAObjectInfoDelegate.getOAObjectInfo(c);
    }

    public OAObjectInfo getOAObjectInfo() {
        return OAObjectInfoDelegate.getOAObjectInfo(getObjectClass());
    }

    public void setLink(Hub hub) {
        this.setLinkHub(hub);
    }

    public Hub<TYPE> createShared() {
        return this.createSharedHub();
    }

    public void updateLinkProperty(Object obj, Object value) {
        Hub h = HubLinkDelegate.getHubWithLink(this, true);
        if (h == null) return;
        HubLinkDelegate.updateLinkedToHub(h, h.getLinkHub(false), value);
    }

    public int compareTo(Object obj) {
        if (obj == null) return 1;
        if (obj == this) return 0;
        if (this.hashCode() > obj.hashCode()) return 1;
        return -1;
    }

    public boolean isServer() {
        return OASyncDelegate.isServer(getObjectClass());
    }
    
    public boolean canAdd() {
        return OAObjectEditQueryDelegate.getAllowAdd(this, false);
    }
    public boolean canAdd(OAObject obj) {
        if (!OAObjectEditQueryDelegate.getAllowAdd(this, false)) return false;
        return OAObjectEditQueryDelegate.getVerifyAdd(this, obj);
    }
    
    public String getCanAddMessage(OAObject obj) {
        OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowAddEditQuery(this, false);
        if (eq != null && !eq.getAllowed()) {
            String s = eq.getResponse();
            s = OAString.concat(s, eq.getThrowable(), ", ");
            return "EditQuery.allowAdd returned: "+s;
        }

        eq = OAObjectEditQueryDelegate.getVerifyAddEditQuery(this, obj);
        if (eq != null && !eq.getAllowed()) {
            String s = eq.getResponse();
            s = OAString.concat(s, eq.getThrowable(), ", ");
            return "EditQuery.verifyAdd returned: "+s;
        }
        
        return null;
    }
    public boolean canRemove() {
        return OAObjectEditQueryDelegate.getAllowRemove(this, false);
    }
    public boolean canRemove(OAObject obj) {
        if (!OAObjectEditQueryDelegate.getAllowRemove(this, false)) return false;
        return OAObjectEditQueryDelegate.getVerifyRemove(this, obj);
    }
    public boolean canRemoveAll() {
        if (!OAObjectEditQueryDelegate.getAllowRemoveAll(this)) return false;
        return OAObjectEditQueryDelegate.getVerifyRemoveAll(this);
    }
    
    public void setLoading(boolean b) {
        OAThreadLocalDelegate.setLoading(b);
    }
    public boolean isLoading() {
        return OAThreadLocalDelegate.isLoading();
    }

    /**
     * notifies clients that hub has changed, and should be retrieved from server.
     */
    public void sendRefresh() {
        HubCSDelegate.sendRefresh(this);
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) return true;
        for (Object obj : c) {
            if (!contains(obj)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends TYPE> c) {
        if (c == null) return true;
        for (Object obj : c) {
            add((TYPE) obj);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends TYPE> c) {
        if (c == null) return true;
        for (Object obj : c) {
            insert((TYPE) obj, index++);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) return true;
        for (Object obj : c) {
            remove(obj);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) return true;
        for (int i=0; ;) {
            Object obj = get(i);
            if (obj == null) break;
            if (c.contains(obj)) {
                i++;
            }
            else {
                if (removeAt(i) == null) i++; 
            }
        }
        return true;
    }

    @Override
    public TYPE get(int index) {
        return getAt(index);
    }

    @Override
    public TYPE set(int index, TYPE element) {
        if (element == null) return null;
        TYPE objx = remove(index);
        if (objx != null) insert(element, index);
        return objx;
    }

    @Override
    public void add(int index, TYPE element) {
        if (element == null) return;
        insert(element, index);
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    
    @Override
    public Iterator<TYPE> iterator() {
        ListIterator<TYPE> listIterator = listIterator();
        return listIterator;
    }
    
    @Override
    public ListIterator<TYPE> listIterator() {
        // create a snapshot, so that concurrent issues dont happen
        final List list = Arrays.asList(toArray());
        
        ListIterator<TYPE> iter = new ListIterator<TYPE>() {
            int pos = -1;

            @Override
            public boolean hasNext() {
                int x = list.size();
                return (x > 0 && pos < (x-1));
            }

            @Override
            public void remove() {
                if (pos >= 0 && pos < list.size()) {
                    Object objx = list.remove(pos);
                    if (objx != null) Hub.this.remove(objx);
                }
            }

            @Override
            public TYPE next() {
                int x = list.size();
                if (pos < x) {
                    ++pos;
                    if (pos < x) return (TYPE) list.get(pos);
                }
                return null;
            }

            @Override
            public boolean hasPrevious() {
                return (pos > 0);
            }

            @Override
            public TYPE previous() {
                if (pos >= 0) {
                    --pos;
                    if (pos >= 0) return (TYPE) list.get(pos);
                }
                return null;
            }

            @Override
            public int nextIndex() {
                int x = list.size();
                if (pos == x) return pos;
                return pos+1;
            }

            @Override
            public int previousIndex() {
                if (pos < 0) return pos;
                return pos-1;
            }

            @Override
            public void set(TYPE e) {
                if (pos >= 0 && pos < list.size()) {
                    Hub.this.remove(e);
                    Hub.this.insert(e, pos);
                    list.set(pos, e);
                }
                
            }

            @Override
            public void add(TYPE e) {
                if (list.contains(e)) return;
                list.add(e);
                Hub.this.add(e);
            }
        };
        return iter;
    }
    
    @Override
    public ListIterator<TYPE> listIterator(int index) {
        ListIterator li = listIterator();
        for (int i=0; i<index; i++) {
            li.next();
        }
        return null;
    }

    @Override
    public List<TYPE> subList(int fromIndex, int toIndex) {
        ArrayList al = new ArrayList();
        for (int i=fromIndex; i<toIndex; i++) {
            Object objx = getAt(i);
            if (objx == null) break;
            al.add(objx);
        }
        return al;
    }

    // public transient boolean DEBUG; // for debugging
}
