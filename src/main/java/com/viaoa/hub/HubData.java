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
package com.viaoa.hub;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.datasource.*;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.util.OANullObject;

/**
 * Core internal data holder shared by each Hub instance.
 * 
 * <p>Encapsulates the Hub’s collection (Vector), identity class, and change
 * tracking counters, and delegates extended state to {@link HubDatax}.
 *
 * <p>Responsibilities include:
 * <ul>
 *   <li>Maintaining ordered membership and modification counters</li>
 *   <li>Lazy creation and reuse of a shared {@code HubDatax}</li>
 *   <li>Managing load and select states during {@link OASelect} operations</li>
 *   <li>Serializing Hub contents for persistence or client transfer</li>
 *   <li>Providing lookup for metadata, sort order, and tracking options</li>
 * </ul>
 *
 * Thread-safe for concurrent read/update of extended state; most mutators
 * short-circuit when no value is needed to avoid unnecessary object creation.
 */
public class HubData implements java.io.Serializable {
	/**
	 * Serialization identifier used to ensure compatibility when HubData
	 * instances are serialized and deserialized.
	 */
    static final long serialVersionUID = 1L;  // used for object serialization

    private static Logger LOG = Logger.getLogger(HubData.class.getName());

    /**
     * The class type of objects stored in the Hub. Used for validation,
     * metadata lookup, and optimized behavior when working with OAObjects.
     */
    protected volatile Class objClass;
    
    /**
     * Underlying ordered collection of Hub elements. Stores all objects
     * currently in the Hub and preserves their insertion order.
     */
    protected transient Vector vector;

    /**
     * Counter incremented whenever the Hub’s structure changes (add, remove,
     * insert, move, sort, select, or shared-hub assignment). Used for efficient
     * change detection without requiring HubListeners.
     */
    protected volatile transient int changeCount;
    
    /**
     * Tracks whether structural changes have occurred that require refresh or
     * downstream synchronization. Works in conjunction with changeCount.
     */
    protected volatile boolean changed;
    
    /**
     * Lazily created extension object holding optional Hub state such as sort
     * information, change-tracking vectors, metadata, filters, and more.
     */
    protected transient volatile HubDatax hubDatax; // extension
    
    /**
     * Constructs a HubData instance using the specified object class and an
     * initial vector sized according to the provided size parameter.
     *
     * @param objClass the class of objects stored in the hub
     * @param size     the initial capacity of the underlying vector
     */
	public HubData(Class objClass, int size) {
	    int x = size * 2;
	    x = Math.max(5, x);
	    x = Math.min(25, x);
	    vector = new Vector(size, x);
	    this.objClass = objClass;
	}

	/**
	 * Constructs a HubData instance with a default initial vector size of 5.
	 *
	 * @param objClass the class of objects stored in the hub
	 */
	public HubData(Class objClass) {
		this(objClass, 5);
	}
    
	/**
	 * Constructs a HubData instance with explicit initial size and increment
	 * size for the underlying vector.
	 *
	 * @param objClass      the class of objects stored in the hub
	 * @param size          the initial capacity of the vector
	 * @param incrementSize the growth increment for the vector
	 */
	public HubData(Class objClass, int size, int incrementSize) {
        int x = Math.max(1, incrementSize);
        x = Math.min(100, x);
        vector = new Vector(size, x);
        this.objClass = objClass;
    }
	
	/**
	 * Lazily creates and returns the extended HubDatax instance.
	 * Thread-safe double-checked initialization.
	 *
	 * @return the HubDatax extension object
	 */
    private HubDatax getHubDatax() {
        if (hubDatax == null) {
            synchronized (this) {
                if (hubDatax == null) {
                    this.hubDatax = new HubDatax();
                }
            }
        }
        return hubDatax;
    }
    
    /**
     * Returns the vector that tracks added objects, or {@code null} if
     * extended data has not been initialized.
     *
     * @return the add-tracking vector, or {@code null}
     */
    public Vector getVecAdd() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.vecAdd;
    }

    /**
     * Sets the vector used to track added objects. Initializes HubDatax
     * if necessary when a non-null value is supplied.
     *
     * @param vecAdd the vector of added objects
     */
    public void setVecAdd(Vector vecAdd) {
        if (hubDatax != null || vecAdd != null) {
            getHubDatax().vecAdd = vecAdd;
        }
    }
    
    /**
     * Returns the vector that tracks removed objects, or {@code null} if
     * extended data has not been initialized.
     *
     * @return the remove-tracking vector, or {@code null}
     */
    public Vector getVecRemove() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.vecRemove;
    }
    
    /**
     * Sets the vector used to track removed objects. Initializes HubDatax
     * if necessary when a non-null value is supplied.
     *
     * @param vecRemove the vector of removed objects
     */
    public void setVecRemove(Vector vecRemove) {
        if (hubDatax != null || vecRemove != null) {
            getHubDatax().vecRemove = vecRemove;
        }
    }
    
    /**
     * Returns the property name used for sorting, or {@code null} if no
     * sort is defined.
     *
     * @return the sort property name, or {@code null}
     */
    public String getSortProperty() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.sortProperty;
    }

    /**
     * Sets the property name used for sorting. Initializes HubDatax if
     * necessary when a non-null value is supplied.
     *
     * @param sortProperty the sort property name
     */
    public void setSortProperty(String sortProperty) {
        if (hubDatax != null || sortProperty != null) {
            getHubDatax().sortProperty = sortProperty;
        }
    }

    /**
     * Returns whether sorting is in ascending order. Defaults to
     * {@code true} if no sort information exists.
     *
     * @return {@code true} if ascending, otherwise {@code false}
     */
    public boolean isSortAsc() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return true;
        return hdx.sortAsc;
    }

    /**
     * Sets whether sorting is ascending. Initializes HubDatax if
     * necessary when a {@code false} value is supplied.
     *
     * @param sortAsc whether sorting should be ascending
     */
    public void setSortAsc(boolean sortAsc) {
        if (hubDatax != null || !sortAsc) {
            getHubDatax().sortAsc = sortAsc;
        }
    }
    
    /**
     * Returns the listener used for sort operations, or {@code null}
     * if none has been assigned.
     *
     * @return the sort listener, or {@code null}
     */
    public HubSortListener getSortListener() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.sortListener;
    }

    /**
     * Sets the listener used for sort operations. Initializes HubDatax
     * if necessary when a non-null value is provided.
     *
     * @param sortListener the sort listener to assign
     */
    public void setSortListener(HubSortListener sortListener) {
        if (hubDatax != null || sortListener != null) {
            getHubDatax().sortListener = sortListener;
        }
    }
    
    /**
     * Returns the current OASelect instance associated with this hub,
     * or {@code null} if none exists.
     *
     * @return the OASelect instance, or {@code null}
     */
    public OASelect getSelect() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.select;
    }

    /**
     * Assigns an OASelect instance to this hub. Initializes HubDatax
     * if needed. When clearing the select (passing {@code null}),
     * extended state may be released if no longer required, and
     * change tracking may be updated.
     *
     * @param select the OASelect instance to assign, or {@code null}
     */
    public void setSelect(OASelect select) {
        if (hubDatax != null || select != null) {
            getHubDatax().select = select;
            if (select == null) {
                if (hubDatax != null && !hubDatax.isNeeded()) {
                    hubDatax = null;
                }
                if (changed) {
                    boolean b = (hubDatax == null);
                    if (!b) {
                        b = (hubDatax.vecAdd == null || hubDatax.vecAdd.size() == 0);
                        b &= (hubDatax.vecRemove == null || hubDatax.vecRemove.size() == 0);
                    }
                    if (b) {
                        changed = false;
                        changeCount++;
                    }
                }
            }
        }
    }

    /**
     * Returns whether the hub is marked as being in a refresh state.
     *
     * @return {@code true} if refresh is active, otherwise {@code false}
     */
    public boolean isRefresh() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return false;
        return hdx.refresh;
    }
    
    /**
     * Sets the refresh state. Initializes HubDatax if needed when
     * enabling refresh.
     *
     * @param refresh whether refresh mode is active
     */
    public void setRefresh(boolean refresh) {
        if (hubDatax != null || refresh) {
            getHubDatax().refresh = refresh;
        }
    }

    /**
     * Global lookup tracking which HubData instances are currently in an
     * all-data-loading state, keyed by the HubData object and associated with
     * the owning thread.
     */
    private static ConcurrentHashMap<HubData, Thread> hmLoadingAllData = new ConcurrentHashMap<HubData, Thread>(23, .85f);

    /**
     * Returns whether the hub is currently in a “loading all data”
     * state on any thread other than the current one.
     *
     * @return {@code true} if loading-all-data is active, otherwise {@code false}
     */
    public boolean isLoadingAllData() {
        Thread t = hmLoadingAllData.get(this);
        if (t == null) return false;
        return (t != Thread.currentThread());
    }
    
    /**
     * Marks this hub as loading all data on the current thread,
     * or clears the state when disabled.
     *
     * @param loadingAllData whether the state is being enabled
     * @return {@code true} if a previous value was replaced, otherwise {@code false}
     */
    public boolean setLoadingAllData(boolean loadingAllData) {
        Thread t = null;
        if (loadingAllData) t = Thread.currentThread();
        return setLoadingAllData(loadingAllData, t); 
    }

    /**
     * Sets or clears the “loading all data” state using the specified thread.
     *
     * @param loadingAllData whether the state is being enabled
     * @param thread         the thread associated with the state
     * @return {@code true} if a previous value was replaced, otherwise {@code false}
     */
    public boolean setLoadingAllData(boolean loadingAllData, Thread thread) {
        if (loadingAllData) {
            if (thread == null) thread = Thread.currentThread();
            return (hmLoadingAllData.put(this, thread) != null);
        }
        return (hmLoadingAllData.remove(this) != null);
    }

    /**
     * Shared lookup table used to determine whether a HubData instance has
     * been marked for select-all behavior.
     */
    private static ConcurrentHashMap<HubData, HubData> hmSelectAllHub = new ConcurrentHashMap<HubData, HubData>(11, .85f);

    /**
     * Returns whether this hub is flagged to select all items,
     * based on membership in the shared lookup table.
     *
     * @return {@code true} if flagged for select-all, otherwise {@code false}
     */
    public boolean isSelectAllHub() {
        return hmSelectAllHub.containsKey(this);
    }

    /**
     * Sets or clears the select-all flag for this hub.
     *
     * @param bSelectAllHub whether to enable select-all mode
     */
    public void setSelectAllHub(boolean bSelectAllHub) {
        if (bSelectAllHub) hmSelectAllHub.put(this, this);
        else hmSelectAllHub.remove(this);
    }

    /**
     * Returns the unique property name used for lookup, or {@code null}
     * if none is defined.
     *
     * @return the unique property name, or {@code null}
     */
    public String getUniqueProperty() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.uniqueProperty;
    }

    /**
     * Sets the name of the unique property used for lookup. Initializes
     * HubDatax if necessary when a non-null value is assigned.
     *
     * @param uniqueProperty the unique property name
     */
    public void setUniqueProperty(String uniqueProperty) {
        if (hubDatax != null || uniqueProperty != null) {
            getHubDatax().uniqueProperty = uniqueProperty;
        }
    }

    /**
     * Sets the getter method used for retrieving the unique property.
     * Initializes HubDatax if necessary when a non-null method is supplied.
     *
     * @param uniquePropertyGetMethod the getter Method for the unique property
     */
    public Method getUniquePropertyGetMethod() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.uniquePropertyGetMethod;
    }

    /**
     * Sets the getter method used for retrieving the unique property.
     * Initializes HubDatax if necessary when a non-null method is supplied.
     *
     * @param uniquePropertyGetMethod the getter Method for the unique property
     */
    public void setUniquePropertyGetMethod(Method uniquePropertyGetMethod) {
        if (hubDatax != null || uniquePropertyGetMethod != null) {
            getHubDatax().uniquePropertyGetMethod = uniquePropertyGetMethod;
        }
    }
    
    /**
     * Returns whether the hub is marked as disabled.
     *
     * @return {@code true} if the hub is disabled, otherwise {@code false}
     */
    public boolean isDisabled() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return false;
        return hdx.disabled;
    }
    
    /**
     * Sets the disabled flag for the hub. Initializes HubDatax if
     * necessary when enabling the disabled state.
     *
     * @param disabled whether the hub should be disabled
     */
    public void setDisabled(boolean disabled) {
        if (hubDatax != null || disabled) {
            getHubDatax().disabled = disabled;
        }
    }

    /**
     * Returns the hashtable of property values used for tracking or
     * lookup, or {@code null} if none exists.
     *
     * @return the hash property table, or {@code null}
     */
    public Hashtable getHashProperty() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.hashProperty;
    }

    /**
     * Assigns the hashtable used for property tracking or lookup.
     * Initializes HubDatax if necessary when a non-null value is supplied.
     *
     * @param hashProperty the hashtable to assign
     */
    public void setHashProperty(Hashtable hashProperty) {
        if (hubDatax != null || hashProperty != null) {
            getHubDatax().hashProperty = hashProperty;
        }
    }

    /**
     * Returns the OAObjectInfo metadata for the objects in this hub.
     * Cached in HubDatax when available.
     *
     * @return the OAObjectInfo instance for the hub’s object class
     */
    public OAObjectInfo getObjectInfo() {
        OAObjectInfo oi;
        HubDatax hdx = hubDatax;
        if (hdx != null) {
            oi = hdx.objectInfo;
            if (oi != null) return oi;
        }
        oi = OAObjectInfoDelegate.getObjectInfo(objClass);
        if (objClass != null && hubDatax != null) hubDatax.objectInfo = oi;
        return oi;
    }

    /**
     * Assigns the OAObjectInfo metadata for this hub. Updates the
     * object class when necessary.
     *
     * @param objectInfo the OAObjectInfo metadata to assign
     */
    public void setObjectInfo(OAObjectInfo objectInfo) {
        if (hubDatax != null) hubDatax.objectInfo = objectInfo;
        if (objectInfo != null && objClass == null) {
            this.objClass = objectInfo.getForClass();
        }
    }

    /**
     * Returns the HubAutoSequence used for automatically assigning
     * sequence values, or {@code null} if none is defined.
     *
     * @return the HubAutoSequence instance, or {@code null}
     */
    public HubAutoSequence getAutoSequence() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.autoSequence;
    }

    /**
     * Sets the HubAutoSequence used for automatically assigning
     * sequence values. Initializes HubDatax if needed.
     *
     * @param autoSequence the auto-sequence object to assign
     */
    public void setAutoSequence(HubAutoSequence autoSequence) {
        if (hubDatax != null || autoSequence != null) {
            getHubDatax().autoSequence = autoSequence;
        }
    }
    
    /**
     * Returns the HubAutoMatch used for automatic matching behavior,
     * or {@code null} if none is defined.
     *
     * @return the HubAutoMatch instance, or {@code null}
     */
    public HubAutoMatch getAutoMatch() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.autoMatch;
    }

    /**
     * Sets the HubAutoMatch used for automatic matching behavior.
     * Initializes HubDatax if needed.
     *
     * @param autoMatch the auto-match object to assign
     */
    public void setAutoMatch(HubAutoMatch autoMatch) {
        if (hubDatax != null || autoMatch != null) {
            getHubDatax().autoMatch = autoMatch;
        }
    }

    /**
     * Returns whether the hub's object type is an OAObject. Cached
     * in HubDatax when available.
     *
     * @return {@code true} if the hub contains OAObject instances
     */
    public boolean isOAObjectFlag() {
        HubDatax hdx = hubDatax;
        if (hdx != null) {
            if (hdx.oaObjectFlag) return true;
            boolean b = objClass != null && OAObject.class.isAssignableFrom(objClass);
            hdx.oaObjectFlag = b;
            return b;
        }
        return objClass != null && OAObject.class.isAssignableFrom(objClass);
    }

    /**
     * Sets the cached flag indicating whether the hub contains OAObject
     * instances. Has no effect unless HubDatax exists.
     *
     * @param oaObjectFlag the value to assign
     */
    public void setOAObjectFlag(boolean oaObjectFlag) {
        if (hubDatax != null) hubDatax.oaObjectFlag = oaObjectFlag; 
    }

    /**
     * Returns whether duplicate add/remove operations are allowed.
     * Defaults to {@code true} when extended state has not been created.
     *
     * @return {@code true} if duplicate add/remove is allowed, otherwise {@code false}
     */
    public boolean isDupAllowAddRemove() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return true; // default
        return hdx.dupAllowAddRemove;
    }

    /**
     * Sets whether duplicate add/remove operations are allowed.
     * Initializes HubDatax if necessary when disabling the default behavior.
     *
     * @param dupAllowAddRemove whether duplicates are permitted
     */
    public void setDupAllowAddRemove(boolean dupAllowAddRemove) {
        if (hubDatax != null || !dupAllowAddRemove) {
            getHubDatax().dupAllowAddRemove = dupAllowAddRemove;
        }
    }

    /**
     * Returns whether Hub add/remove changes are being tracked.
     *
     * @return {@code true} if change tracking is enabled, otherwise {@code false}
     */
    public boolean getTrackChanges() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return false;
        return hdx.bTrackChanges;
    }

    /**
     * Enables or disables Hub add/remove change tracking.
     * Initializes HubDatax if needed when enabling tracking.
     *
     * @param bTrackChanges whether change tracking should be enabled
     */
    public void setTrackChanges(boolean bTrackChanges) {
        if (hubDatax != null || bTrackChanges) {
            getHubDatax().bTrackChanges = bTrackChanges;
        }
    }

    /**
     * Custom serialization routine that writes HubData’s fields, the
     * HubDatax extension (if serializable), and internal vectors.
     *
     * @param s the output stream used for serialization
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
        s.defaultWriteObject();
        
        HubDatax hdx = hubDatax;
        if (hdx != null && !hdx.shouldSerialize()) hdx = null;
        s.writeObject(hdx);
        
        writeVector(s, vector);
        Vector vec;
        if (hubDatax != null) vec = hubDatax.vecAdd;
        else vec = null;
        writeVector(s, vec);
        if (hubDatax != null) vec = hubDatax.vecRemove;
        else vec = null;
        writeVector(s, vec);
    }
    
    /**
     * Custom deserialization routine that restores HubData’s fields,
     * optional HubDatax extension, and internal vectors for add/remove tracking.
     *
     * @param s the input stream used for deserialization
     * @throws java.io.IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be resolved
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        hubDatax = (HubDatax) s.readObject();
        vector = readVector(s);
        
        Vector vec = readVector(s);
        if (vec != null && vec.size() > 0) setVecAdd(vec);
        
        vec = readVector(s);
        if (vec != null && vec.size() > 0) setVecRemove(vec);
    }

    /**
     * Serializes a vector by writing its capacity, size, and each element
     * in order, substituting {@link OANullObject} for missing entries.
     *
     * @param s   the output stream used for serialization
     * @param vec the vector to serialize, or {@code null}
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeVector(java.io.ObjectOutputStream s, Vector vec) throws java.io.IOException{
        if (vec == null) {
            s.writeInt(-1);
            return;
        }
        
        int cap = vec.capacity();
        s.writeInt(cap);
        int max = vec.size();
        s.writeInt(max);
        
        
        int i = 0;
        for (; i<max; i++) {
            Object obj;
            try {
                obj = vec.elementAt(i);
            }
            catch (Exception e) {
                break;
            }
            s.writeObject(obj);
        }
        for (; i<max; i++) {
            // write out bogus objects
            s.writeObject(OANullObject.instance);
        }        
    }
    
    /**
     * Deserializes a vector previously written by {@link #writeVector}.
     * Reconstructs elements in order, skipping {@link OANullObject} markers.
     *
     * @param s the input stream used for deserialization
     * @return the reconstructed vector, or {@code null}
     * @throws java.io.IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be resolved
     */
    private Vector readVector(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        int capacity = s.readInt();
        if (capacity < 0) return null;
        Vector vec = new Vector(capacity);

        int max = s.readInt();

        // Read in all elements in the proper order. 
        for (int i=0; i<max; i++) {
            Object obj = s.readObject();
            if (!(obj instanceof OANullObject)) vec.addElement(obj);
        }
        return vec;
    }

    /**
     * Returns the hub used for select-where filtering, or {@code null}
     * if none has been assigned.
     *
     * @return the hub used for select-where filtering, or {@code null}
     */
    public Hub getSelectWhereHub() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.selectWhereHub;
    }
    
    /**
     * Sets the hub to be used for select-where filtering. Initializes
     * HubDatax if required.
     *
     * @param hub the hub used for select-where filtering
     */
    public void setSelectWhereHub(Hub hub) {
        if (hubDatax != null || hub != null) {
            getHubDatax().selectWhereHub = hub;
        }
    }

    /**
     * Returns the property path used with the select-where hub, or
     * {@code null} if none has been set.
     *
     * @return the select-where property path, or {@code null}
     */
    public String getSelectWhereHubPropertyPath() {
        HubDatax hdx = hubDatax;
        if (hdx == null) return null;
        return hdx.selectWhereHubPropertyPath;
    }
    
    /**
     * Sets the property path used for select-where filtering. Initializes
     * HubDatax as needed.
     *
     * @param pp the property path to assign
     */
    public void setSelectWhereHubPropertyPath(String pp) {
        if (hubDatax != null || pp != null) {
            getHubDatax().selectWhereHubPropertyPath = pp;
        }
    }

	public static final class FriendAccess {
		private FriendAccess() {
		}
		public Class getObjClass(Hub hub) {
			return hub.data.objClass;
		}
		public Vector getVector(Hub hub) {
			return hub.data.vector;
		}
		public void setVector(Hub hub, Vector v) {
			hub.data.vector = v;
		}
		public HubDatax getHubDatax(Hub hub) {
			return hub.data.hubDatax;
		}
		public void setHubDataxNull(Hub hub) {
			hub.data.hubDatax = null;
		}
		public boolean getChanged(Hub hub) {
			return hub.data.changed;
		}
		public void setChanged(Hub hub, boolean b) {
			hub.data.changed = b;
		}
		public int getChangeCount(Hub hub) {
			return hub.data.changeCount;
		}
		public void incrementChangeCount(Hub hub) {
			hub.data.changeCount++;
		}
	}

	private final static FriendAccess friendAccess = new FriendAccess();
	static FriendAccess getFriendAccess() {
		return friendAccess;
	}
	
}
