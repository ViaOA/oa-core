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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.OASync;

/**
 * Core delegate for internal Hub data operations.
 * <p>
 * Directly manipulates the underlying {@code HubData.vector} and change-tracking
 * structures. Provides thread-safe add/remove/insert operations and maintains
 * Hub-level change counters.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Manage object storage within the Hub’s {@code Vector}.</li>
 *   <li>Track added and removed objects for persistence and synchronization.</li>
 *   <li>Coordinate with {@link HubCSDelegate} and {@link HubAddRemoveDelegate}
 *       for client/server propagation.</li>
 *   <li>Maintain Hub change counters and dirty-state flags.</li>
 * </ul>
 *
 * <p>Called by most other Hub delegates; low-level changes should only be made
 * through these internal APIs to guarantee event and cache consistency.
 */
public class HubDataDelegate {
	
    private static Logger LOG = Logger.getLogger(HubDataDelegate.class.getName());
    
    /**
     * Clears all internal Hub data structures and resets tracking state.
     * Removes all elements from the Hub’s vector and clears add/remove
     * tracking lists. Drops the {@code hubDatax} extension if it is no
     * longer needed, resets the changed flag, and increments the change
     * counter.
     *
     * @param thisHub the hub whose internal state is being reset
     */
	protected static void clearAllAndReset(Hub thisHub) {
    	synchronized (thisHub.data) {
            if (thisHub.data.getVecAdd() != null) thisHub.data.getVecAdd().removeAllElements();
    		if (thisHub.data.getVecRemove() != null) thisHub.data.getVecRemove().removeAllElements();
    		thisHub.data.vector.removeAllElements();
    	
            // 20160407
            if (thisHub.data.hubDatax != null) {
                if (!thisHub.data.hubDatax.isNeeded()) thisHub.data.hubDatax = null;
            }
    	}
        thisHub.data.changed = false;
		thisHub.data.changeCount++;
	}
	
	/**
	 * Ensures that the Hub’s underlying vector has capacity for at least
	 * the specified number of elements.
	 *
	 * @param thisHub the hub whose vector capacity is being checked
	 * @param size    the minimum capacity required
	 */
	protected static void ensureCapacity(Hub thisHub, int size) {
		thisHub.data.vector.ensureCapacity(size);
	}
	
	/**
	 * Trims the Hub’s underlying vector to its current size. No-op when
	 * serialization is in progress and the vector is null.
	 *
	 * @param thisHub the hub whose vector should be trimmed
	 */
	public static void resizeToFit(Hub thisHub) {
		if (thisHub.data.vector == null) return; // could be called during serialization
		thisHub.data.vector.trimToSize();
	}

	/**
	 * Updates the Hub’s changed flag and increments its change counter
	 * when the value transitions. Clearing the changed flag also clears
	 * tracked add/remove lists. When marking as changed, the master
	 * object may also be marked changed based on link metadata.
	 *
	 * @param thisHub   the hub whose changed state is being updated
	 * @param bChanged  the new changed value
	 */
	protected static void setChanged(Hub thisHub, boolean bChanged) {
	    if (thisHub == null) return;
        boolean old = thisHub.data.changed;
        if (bChanged == old) return;
        thisHub.data.changed = bChanged;
        if (bChanged != old) thisHub.data.changeCount++;
        if (!bChanged) {
            clearHubChanges(thisHub);
        }
        else {  // 20180529 if changed, then masterObject needs to be flagged as changed
            OAObject obj = thisHub.getMasterObject();
            if (obj != null) {
                OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterHubToDetail(thisHub);
                if (li != null && (li.getType() == li.MANY)) {
                    boolean bx = (li.getOwner() || li.getCascadeSave());
                    if (!bx) { 
                        OALinkInfo rli = li.getReverseLinkInfo();
                        bx = (rli != null && rli.getType() == li.MANY);
                    }
                    if (bx) obj.setChanged(true);
                }
            }
        }
    }
	
	/**
	 * Clears the Hub’s add/remove tracking lists and resets the changed
	 * flag when those lists become empty. Drops {@code hubDatax} if it is
	 * no longer required. Sends a remote clear-changes event when any
	 * tracked changes existed.
	 *
	 * @param thisHub the hub whose change tracking is being cleared
	 */
	public static void clearHubChanges(Hub thisHub) {
	    if (thisHub == null) return;
        boolean bSendEvent = false;
        synchronized (thisHub.data) {
            Vector v = thisHub.data.getVecAdd(); 
            if (v != null) {
                bSendEvent = v.size() > 0;
                v.removeAllElements();
            }
            v = thisHub.data.getVecRemove(); 
            if (v != null) {
                bSendEvent = bSendEvent || v.size() > 0;
                v.removeAllElements();
            }
            
            if (thisHub.data.hubDatax != null) {
                if (!thisHub.data.hubDatax.isNeeded()) {
                    thisHub.data.hubDatax = null;
                }
                if (thisHub.data.changed) {
                    boolean b = (thisHub.data.hubDatax == null);
                    if (!b) {
                        b = (thisHub.data.hubDatax.vecAdd == null || thisHub.data.hubDatax.vecAdd.size() == 0);
                        b &= (thisHub.data.hubDatax.vecRemove == null || thisHub.data.hubDatax.vecRemove.size() == 0);
                    }
                    if (b) {
                        thisHub.data.changed = false;
                        thisHub.data.changeCount++;
                    }
                }
            }
        }
        if (bSendEvent) {
            HubCSDelegate.clearHubChanges(thisHub);
        }
	}
	
	/**
	 * Copies all elements from the Hub’s vector into the supplied array.
	 * The copy is performed under synchronization for thread safety.
	 *
	 * @param thisHub the hub whose elements are being copied
	 * @param anArray the destination array
	 */
    protected static void copyInto(Hub thisHub, Object anArray[]) {
        synchronized (thisHub.data) {
            thisHub.data.vector.copyInto(anArray);
        }
    }

    /**
     * Returns an array containing all elements in the Hub. Retries the
     * copy operation if the Hub’s contents change during the attempt.
     * Ensures that the Hub’s size is known before copying.
     *
     * @param thisHub the hub whose elements are being converted to an array
     * @return an array containing the Hub’s elements
     */
	public static Object[] toArray(Hub thisHub) {
	    thisHub.getSize(); // call before sync, in case it needs to load
        Object[] objs;
        for (int i=0;;i++) {
            synchronized (thisHub.data) {
                objs = new Object[thisHub.getSize()];
                try {
                    thisHub.data.vector.copyInto(objs);
                    break;
                }
                catch (Exception e) {
                    // if exception, then try again
                }
            }
        }
	    return objs;
	}

	/**
	 * Returns the current number of elements in the Hub.
	 *
	 * @param thisHub the hub whose size is being retrieved
	 * @return the number of elements in the Hub
	 */
    public static int getCurrentSize(Hub thisHub) {
        return thisHub.data.vector.size();
    }
	

    /**
     * Copies the underlying vector from one Hub to another as part of
     * cloning operations.
     *
     * @param thisHub the source hub
     * @param newHub  the destination hub
     */
    public static void _clone(Hub thisHub, Hub newHub) {
    	newHub.data.vector = (Vector) thisHub.data.vector.clone();
    }
    
    /**
     * Removes an object from the Hub while managing locking and remote
     * thread coordination. Delegates to the internal removal method and
     * returns the removed position unless removing all elements.
     *
     * @param thisHub        the hub from which the object is removed
     * @param obj            the object being removed
     * @param bDeleting      whether the object is being permanently deleted
     * @param bIsRemovingAll whether the entire Hub is being cleared
     * @return the position from which the object was removed, or -1
     */
    protected static int _remove(Hub thisHub, Object obj, boolean bDeleting, boolean bIsRemovingAll) {
        int pos = 0;
        try {
            OAThreadLocalDelegate.lock(thisHub);
            pos = _remove2(thisHub, obj, bDeleting, bIsRemovingAll);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
        }
        if (!bIsRemovingAll) {
            OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
        }
        return pos;
    }
    
    /**
     * Performs the core remove operation without acquiring locks.
     * Removes the object from the Hub's vector when applicable and
     * updates add/remove tracking lists based on deletion rules,
     * tracking mode, and server state. Updates the Hub’s changed flag
     * and change counter as needed.
     *
     * @param thisHub        the hub from which the object is being removed
     * @param obj            the object being removed
     * @param bDeleting      whether the object is being permanently deleted
     * @param bIsRemovingAll whether the entire Hub is being cleared
     * @return the position from which the object was removed, or -1
     */
    private static int _remove2(Hub thisHub, Object obj, boolean bDeleting, boolean bIsRemovingAll) {
        int pos;
        if (bIsRemovingAll) {
            pos = -1;
        }
        else {
	        pos = thisHub.getPos(obj);
	        if (pos >= 0) {
	            thisHub.data.vector.removeElementAt(pos);
	        }
        }

	    if (pos >= 0) {
	        boolean b = (obj instanceof OAObject);	        
            if (b) {
                b = ((thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()));
                if (!b && OASync.isServer()) {
                    if ( ((OAObject) obj).isChanged()) {
                        if (thisHub.datam.getMasterObject() != null) {
                            // could be ServerRoot
                            OALinkInfo li = thisHub.datam.liDetailToMaster;
                            if (li != null && !li.getCalculated()) {
                                li = li.getReverseLinkInfo();
                                if (li != null && !li.getCalculated()) {
                                    b = true;
                                }
                            }
                        }
                    }
                }
            }
            
	        
	    	if (b) {
	            if (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().removeElement(obj)) {
	                // no-op
	            }
	            else {
	                if (!bDeleting) {
                    	Vector vec = createVecRemove(thisHub);
                    	if (!vec.contains(obj)) vec.addElement(obj);
	                }
	            }
		        thisHub.setChanged( (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0) || (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0) );
		    }
		    else {
		    	setChanged(thisHub, true);
		    }
	    }	    
	    return pos;
	}

	
    /**
     * Adds an object to the Hub's vector, optionally acquiring a lock
     * unless already held. Delegates to the internal add method and
     * triggers remote thread processing. Returns whether the object
     * was added.
     *
     * @param thisHub        the hub receiving the object
     * @param obj            the object to add
     * @param bHasLock       whether the calling thread already holds the lock
     * @param bCheckContains whether to skip the add if the object is already present
     * @return {@code true} if the object was added; otherwise {@code false}
     */
    protected static boolean _add(Hub thisHub, Object obj, boolean bHasLock, boolean bCheckContains) {
        boolean b = false;
        try {
            if (!bHasLock) OAThreadLocalDelegate.lock(thisHub);
            b = _add2(thisHub, obj, bCheckContains);
        }
        finally {
            if (!bHasLock ) OAThreadLocalDelegate.unlock(thisHub);
        }
        OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
        return b;
    }
    
    /**
     * Performs the core add operation without acquiring locks.
     * Adds the object to the Hub's vector, updates tracking lists
     * when required, sets the Hub’s changed flag, and increments
     * the change counter. Skips addition when instructed and
     * the object is already present.
     *
     * @param thisHub        the hub receiving the object
     * @param obj            the object to add
     * @param bCheckContains whether to skip if the object already exists
     * @return {@code true} if the object was added; otherwise {@code false}
     */
    private static boolean _add2(Hub thisHub, Object obj, final boolean bCheckContains) {
        if (bCheckContains && thisHub.contains(obj)) return false;
    	thisHub.data.vector.addElement(obj);
        
        int xx = thisHub.data.vector.size();
        if (xx > 499 && thisHub.datam.getMasterObject() != null && (xx%100)==0) {
            if (xx < 1000 || (xx%1000)==0) LOG.fine("large Hub with masterObject, Hub="+thisHub);
            if ((xx%10000)==0) {
                LOG.fine("large Hub with masterObject, Hub="+thisHub);
            }
        }

        if (!OAThreadLocalDelegate.isLoading()) {
            if ((thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()) && (obj instanceof OAObject)) {
                if (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().contains(obj)) {
            		thisHub.data.getVecRemove().removeElement(obj);
                }
                else {
                    createVecAdd(thisHub).addElement(obj);
                }
                thisHub.setChanged( (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0) || (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0) );
            }
            else {
                thisHub.setChanged(true);
            }
        }
        thisHub.data.changeCount++;
	    return true;
	}


    /**
     * Inserts an object at the specified position in the Hub's vector,
     * optionally acquiring a lock when not already held. Delegates to
     * the internal insert method and triggers remote thread processing.
     *
     * @param thisHub   the hub receiving the insertion
     * @param obj       the object to insert
     * @param pos       the index at which to insert the object
     * @param bIsLocked whether the calling thread already holds the lock
     * @return {@code true} if the object was inserted
     */
    protected static boolean _insert(Hub thisHub, Object obj, int pos, boolean bIsLocked) {
        boolean b = false;
        try {
            if (!bIsLocked) OAThreadLocalDelegate.lock(thisHub);
            //was b = _insert2(thisHub, key, obj, pos, bLock);
            b = _insert2(thisHub, obj, pos);
        }
        finally {
            if (!bIsLocked) OAThreadLocalDelegate.unlock(thisHub);
        }
        
        OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
        return b;
    }
   
    /**
     * Performs the core insert operation without acquiring locks.
     * Inserts the object into the Hub’s vector at the specified
     * position, updates tracking lists when enabled, sets the
     * changed flag, and increments the change counter.
     *
     * @param thisHub the hub receiving the object
     * @param obj     the object to insert
     * @param pos     the position at which to insert the object
     * @return {@code true} if the insert completed
     */
	private static boolean _insert2(Hub thisHub, Object obj, int pos) {
        boolean b = OAThreadLocalDelegate.isLoading();

    	thisHub.data.vector.insertElementAt(obj, pos);

    	if (!b) {
        	if ((thisHub.datam.getTrackChanges() || thisHub.data.getTrackChanges()) && (obj instanceof OAObject)) {
                if (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().contains(obj)) {
            		thisHub.data.getVecRemove().removeElement(obj);
                }
                else {
                    createVecAdd(thisHub).addElement(obj);
                }
                thisHub.setChanged( (thisHub.data.getVecAdd() != null && thisHub.data.getVecAdd().size() > 0) || (thisHub.data.getVecRemove() != null && thisHub.data.getVecRemove().size() > 0) );
    	    }
    	    else thisHub.setChanged(true);
    	}
		
	    thisHub.data.changeCount++;
	    return true;
	}

	/**
	 * Moves an object within the Hub from one index to another.
	 * Performs the vector operations under lock, increments the
	 * change counter, and triggers remote thread processing.
	 *
	 * @param thisHub the hub containing the object
	 * @param obj     the object to move
	 * @param posFrom the original index
	 * @param posTo   the destination index
	 */
	protected static void _move(Hub thisHub, Object obj, int posFrom, int posTo) {
        try {
            OAThreadLocalDelegate.lock(thisHub);
            thisHub.data.changeCount++;
            thisHub.data.vector.removeElementAt(posFrom);
            thisHub.data.vector.insertElementAt(obj, posTo);
        }
        finally {
            OAThreadLocalDelegate.unlock(thisHub);
        }
        OARemoteThreadDelegate.startNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
	}
	
	/**
	 * Adds all objects currently in the Hub to the add-tracking vector,
	 * creating the vector when necessary. No-op when the hub is null.
	 *
	 * @param thisHub the hub whose contents are being tracked as added
	 */
	public static void addAllToAddVector(Hub thisHub) {
	    if (thisHub == null) return;
        createVecAdd(thisHub);
	    for (Object objx :  thisHub) {
	        thisHub.data.getVecAdd().add(objx);	        
	    }
	}
	
	/**
	 * Creates the Hub’s add-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose add-tracking vector is being created
	 * @return the add-tracking vector
	 */
	protected static Vector createVecAdd(Hub thisHub) {
        if (thisHub.data.getVecAdd() == null) {
	        synchronized (thisHub.data) {
	            if (thisHub.data.getVecAdd() == null) thisHub.data.setVecAdd(new Vector(10, 10));
	        }
        }
        return thisHub.data.getVecAdd();
	}
	
	/**
	 * Creates the Hub’s remove-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose remove-tracking vector is being created
	 * @return the remove-tracking vector
	 */
	protected static Vector createVecRemove(Hub thisHub) {
		if (thisHub.data.getVecRemove() == null) {
	        synchronized (thisHub.data) {
	            if (thisHub.data.getVecRemove() == null) thisHub.data.setVecRemove(new Vector(10,10));
	        }
		}
        return thisHub.data.getVecRemove();
	}
	
	/**
	 * Returns an array of objects that have been added to the Hub but not yet cleared
	 * from the Hub’s change-tracking “added” list.
	 *
	 * <p>The method checks the Hub’s internal {@code vecAdd} list and, if it contains
	 * entries, copies them into a new {@code OAObject[]} array under synchronization.</p>
	 *
	 * @param thisHub the Hub whose added-object list is being queried
	 * @return an array of added {@link OAObject} instances, or {@code null} if none
	 *         have been recorded
	 */
	public static OAObject[] getAddedObjects(Hub thisHub) {
        Vector v = thisHub.data.getVecAdd();
        if (v == null || v.size() == 0) return null;
        synchronized (thisHub.data) {
     		OAObject[] objs;
			int x = (v == null) ? 0 : v.size();
			objs = new OAObject[x];
			if (x > 0) v.copyInto(objs);
			return objs;
        }
	}

	/**
	 * Returns an array of objects that have been removed from the Hub but not yet
	 * cleared from the Hub’s change-tracking “removed” list.
	 *
	 * <p>The method checks the Hub’s internal {@code vecRemove} list and, if it
	 * contains entries, copies them into a new {@code OAObject[]} array under
	 * synchronization.</p>
	 *
	 * @param thisHub the Hub whose removed-object list is being queried
	 * @return an array of removed {@link OAObject} instances, or {@code null} if
	 *         none have been recorded
	 */
	public static OAObject[] getRemovedObjects(Hub thisHub) {
        Vector v = thisHub.data.getVecRemove();
        if (v == null || v.size() == 0) return null;
        synchronized (thisHub.data) {
			OAObject[] objs;
			int x = (v == null) ? 0 : v.size();
			objs = new OAObject[x];
			if (x > 0) v.copyInto(objs);
			return objs;
        }
	}

	/**
	 * Returns whether the Hub is marked as changed.
	 *
	 * @param thisHub the Hub to check
	 * @return {@code true} if the Hub is marked as changed, otherwise {@code false}
	 */
	public static boolean getChanged(Hub thisHub) {
	    return (thisHub.data.changed);
	}
	
	/**
	 * Returns the object in the Hub matching the supplied key.
	 *
	 * <p>The key is converted to an {@link OAObjectKey} if needed, and the Hub is
	 * searched sequentially. If the Hub supports loading additional data, more
	 * records are fetched until a match is found or no more records are available.</p>
	 *
	 * @param thisHub the Hub to search
	 * @param key the key or object used to identify the desired element
	 * @return the matching object, or {@code null} if not found
	 */
	public static Object getObject(final Hub thisHub, Object key) {
		if (thisHub == null || key == null) return null;
	    if (!(key instanceof OAObjectKey)) {
	    	if (key instanceof OAObject) key = OAObjectKeyDelegate.getKey((OAObject) key);
	    	else key = OAObjectKeyDelegate.createObjectKey(thisHub.getObjectClass(), key);
	    }
		for (int i=0; ; i++) {
			Object obj = getObjectAt(thisHub, i);
			if (obj == null) break;
			if (obj == key) return obj;
			if (obj instanceof OAObject) {
				OAObjectKey k = OAObjectKeyDelegate.getKey((OAObject) obj);
				// note: dont send class to isForSameOAObject: dont want it to do a cache lookup
				if (OAObjectKeyDelegate.isForSameOAObject(null, k, (OAObjectKey) key)) return obj;
			}
		}
		return null;
	}
	
	/**
	 * Retrieves the object at the specified position within the Hub.
	 *
	 * <p>If the object is an {@link OAObjectKey}, it is resolved to its underlying
	 * {@link OAObject}. If the Hub supports incremental loading, additional data
	 * may be fetched to satisfy the request.</p>
	 *
	 * @param thisHub the Hub containing the data
	 * @param pos the index to retrieve
	 * @return the object at the position, or {@code null} if not available
	 */
	protected static Object getObjectAt(Hub thisHub, int pos) {
	    Object ho;
	    if (pos < 0) return null;
	    
	    int size = thisHub.data.vector.size();
	    if (pos < size) {
	        Object obj = null;
	        try {
	        	obj = thisHub.data.vector.elementAt(pos);
	        }
	        catch (Exception e) {
	        	obj = null;  // hub could have changed, and pos is not valid anymore
	        }
	        if (obj instanceof OAObjectKey && thisHub.isOAObject()) {
            	obj = OAObjectReflectDelegate.getObject(thisHub.getObjectClass(), obj);
                if (obj != null) {
	                OAObjectHubDelegate.addHub((OAObject)obj, thisHub);
	                thisHub.data.vector.setElementAt(obj, pos);
	                if (thisHub.datam.getMasterObject() != null) {
		                // need to set property to MasterHub
	                	HubDetailDelegate.setPropertyToMasterHub(thisHub, obj, thisHub.datam.getMasterObject());
	                }
                }
	        }
	        if (obj != null) return obj;
	    }
	
	    if (!HubSelectDelegate.isMoreData(thisHub)) {
	        return null;
	    }
	
	    // fetch more records from data source
	    for ( ; pos >= thisHub.data.vector.size() && HubSelectDelegate.isMoreData(thisHub) ; ) {
	    	HubSelectDelegate.fetchMore(thisHub);
	    }
	    ho = HubDataDelegate.getObjectAt(thisHub, pos);
	    return ho;
	}
	
	/**
	 * Finds the position of the given object within the Hub.
	 *
	 * <p>If not present, optional master adjustment and recursive-hub logic may be
	 * applied to locate and reposition the Hub within a recursive hierarchy.</p>
	 *
	 * @param thisHub the Hub to search
	 * @param object the object to locate
	 * @param adjustMaster whether to adjust the master Hub relationship if needed
	 * @param bUpdateLink whether to update link relationships during the search
	 * @return the position of the object, or {@code -1} if not found
	 */
	public static int getPos(final Hub thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink) {
	    int pos;
	    if (object == null || thisHub == null) return -1;

	    if (!(object instanceof OAObject)) {
	        if (OAObject.class.isAssignableFrom(object.getClass())) {  // could be hub of strings
	            object = HubDelegate.getRealObject(thisHub, object);
	        }
	    }
	    pos = -1;
	    if (object != null) {
	        for ( ;; ) {
	            pos = thisHub.data.vector.indexOf(object);
	            if (pos >= 0) return pos;
	            if (!HubSelectDelegate.isMoreData(thisHub)) break;
                HubSelectDelegate.fetchMore(thisHub);
	        }
	    }

        if (pos < 0 && adjustMaster && (thisHub.datau.getSharedHub() != null || thisHub.datam.getMasterHub() != null)) {
            OALinkInfo liRecursiveOne = OAObjectInfoDelegate.getRecursiveLinkInfo(thisHub.data.getObjectInfo(), OALinkInfo.ONE);

            // need to verify that this hub is recursive with masterObject
            if (liRecursiveOne != null) {  
                OALinkInfo li = thisHub.datam.liDetailToMaster;
                if (li != null) {
                    li = OAObjectInfoDelegate.getReverseLinkInfo(li);
                    if (li == null || !li.getRecursive()) {
                        liRecursiveOne = null;
                    }
                }
                else {
                    // 20171123
                    Hub rh = thisHub.getRootHub();
                    boolean b = (rh != null && (thisHub == rh || thisHub.getSharedHub() == rh));
                    if (!b) {
                        // dont treat as recursive. This is when there are a collection of objects not used for recursion
                        //   ex: a Hub with only one location in it (no masterHub)
                        liRecursiveOne = null; 
                    }
                }
            }

            boolean bUseMaster = false;
            if (liRecursiveOne != null) {  // if recursive
                Object parent = OAObjectReflectDelegate.getProperty((OAObject)object, liRecursiveOne.getName());
                if (parent == null) {  // might be in root hub
                    Hub h = thisHub.getRootHub();  // could be owner of hub
                    if (h != null && h != thisHub && thisHub.datau.getSharedHub() != h) {
                        HubShareDelegate.setSharedHub(thisHub, h, false);
                        pos = getPos(h, object, adjustMaster, bUpdateLink);
                    }
                    if (pos < 0) {
                        bUseMaster = true;  // adjust master/owner for this recursive hub
                    }
                }
                else {
                	OALinkInfo liMany = OAObjectInfoDelegate.getReverseLinkInfo(liRecursiveOne);
                	if (liMany != null) {
                        hashRecursiveHubDetail.computeIfAbsent(thisHub, k -> {
                            HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
                            if (dm.liDetailToMaster != null) return dm.liDetailToMaster;
                        	return null;
                        });
                    	Object val = OAObjectReflectDelegate.getProperty((OAObject)parent, liMany.getName());
                    	// reassign the sharedHub to correct recursive hub in the hierarchy
                    	HubShareDelegate.setSharedHub(thisHub, (Hub) val, false, object);
                        pos = getPos((Hub)val, object, adjustMaster, bUpdateLink);
                	}
                }
            }

            if (bUseMaster) {
                if (thisHub.datam.getMasterHub() != null && thisHub.datam.liDetailToMaster != null) {  
                    // only do this if a masterHub, since a hub that has a masterObject (w/o hub) should not do this adjustment
                    Object parent = OAObjectReflectDelegate.getProperty((OAObject)object, thisHub.datam.liDetailToMaster.getName());
                    if (parent != null) {
                        OALinkInfo li = OAObjectInfoDelegate.getReverseLinkInfo(thisHub.datam.liDetailToMaster);
                        if (li != null) {
                            hashRecursiveHubDetail.computeIfAbsent(thisHub, k -> {
                                HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
                                if (dm.liDetailToMaster != null) return  dm.liDetailToMaster;
                                return null;
                            });
                            Object val = OAObjectReflectDelegate.getProperty((OAObject)parent, li.getName());
                            HubShareDelegate.setSharedHub(thisHub, (Hub) val, false, object);
                            pos = getPos((Hub)val, object, adjustMaster, bUpdateLink);
                        }
                    }
                }
                else {
                    // see if it was a master/detail that was reassigned (shared) to a child hub that is recursive
                    OALinkInfo li = hashRecursiveHubDetail.get(thisHub);
                    if (li != null) {
                        Object parent = OAObjectReflectDelegate.getProperty((OAObject)object, li.getName());
                        if (parent != null) {
                            Object val = OAObjectReflectDelegate.getProperty((OAObject)parent, li.getReverseName());
                            HubShareDelegate.setSharedHub(thisHub, (Hub) val, false, object);
                            pos = getPos((Hub)val, object, adjustMaster, bUpdateLink);
                        }
                    }
                }
            }
        }
        

        if (pos < 0 && adjustMaster) {
            if (HubDetailDelegate.setMasterHubActiveObject(thisHub, object, bUpdateLink)) {
                pos = getPos(thisHub, object, false, false);
            }
        }
	    return pos;
	}
	
    /**
     * Used by HubDataDelegate.getPos(..) when finding the object for recursive links
     */
    static private final Map<Hub, OALinkInfo> hashRecursiveHubDetail = new ConcurrentHashMap<Hub, OALinkInfo>(11, 0.75F);
    
    /**
     * Removes the given object from the Hub’s change-tracking “added” list.
     *
     * <p>Also updates the Hub’s change flag if the removal results in no remaining
     * tracked additions or removals.</p>
     *
     * @param thisHub the Hub whose added list is modified
     * @param obj the object to remove from the added list
     */
	protected static void removeFromAddedList(Hub thisHub, Object obj) {
	    synchronized (thisHub.data) {
            if (thisHub.data.hubDatax == null) return;
	    	Vector v = thisHub.data.getVecAdd();
	    	if (v != null) v.remove(obj);
            if (thisHub.data.hubDatax != null) {
                if (!thisHub.data.hubDatax.isNeeded()) {
                    thisHub.data.hubDatax = null;
                }
                if (thisHub.data.changed) {
                    boolean b = (thisHub.data.hubDatax == null);
                    if (!b) {
                        b = (thisHub.data.hubDatax.vecAdd == null || thisHub.data.hubDatax.vecAdd.size() == 0);
                        b &= (thisHub.data.hubDatax.vecRemove == null || thisHub.data.hubDatax.vecRemove.size() == 0);
                    }
                    if (b) {
                        thisHub.data.changed = false;
                        thisHub.data.changeCount++;
                    }
                }
            }
	    }
	}
	
	/**
	 * Removes the given object from the Hub’s change-tracking “removed” list.
	 *
	 * <p>Also updates the Hub’s change flag if the removal results in no remaining
	 * tracked additions or removals.</p>
	 *
	 * @param thisHub the Hub whose removed list is modified
	 * @param obj the object to remove from the removed list
	 */
	public static void removeFromRemovedList(Hub thisHub, Object obj) {
        if (thisHub.data.hubDatax == null) return;
	    synchronized (thisHub.data) {
	    	Vector v = thisHub.data.getVecRemove();
	    	if (v != null) v.remove(obj);
            if (thisHub.data.hubDatax != null) {
                if (!thisHub.data.hubDatax.isNeeded()) {
                    thisHub.data.hubDatax = null;
                }
                if (thisHub.data.changed) {
                    boolean b = (thisHub.data.hubDatax == null);
                    if (!b) {
                        b = (thisHub.data.hubDatax.vecAdd == null || thisHub.data.hubDatax.vecAdd.size() == 0);
                        b &= (thisHub.data.hubDatax.vecRemove == null || thisHub.data.hubDatax.vecRemove.size() == 0);
                    }
                    if (b) {
                        thisHub.data.changed = false;
                        thisHub.data.changeCount++;
                    }
                }
            }
	    }
	}

	/**
	 * Returns the Hub’s change counter value.
	 *
	 * @param thisHub the Hub whose change count is requested
	 * @return the current change count
	 */
	public static int getChangeCount(Hub thisHub) {
	    return thisHub.data.changeCount;
	}
	
	/**
	 * Increments the Hub’s change counter.
	 *
	 * @param thisHub the Hub whose counter is incremented
	 */
	protected static void incChangeCount(Hub thisHub) {
		thisHub.data.changeCount++;
	}

	/**
	 * Determines whether the Hub contains the specified object.
	 *
	 * <p>Equivalent to calling {@link #contains(Hub, Object, boolean)} with
	 * {@code false} for the {@code bJustAdded} flag.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to look for
	 * @return {@code true} if the Hub contains the object, otherwise {@code false}
	 */
	public static boolean contains(Hub hub, Object obj) {
		return contains(hub, obj, false);
	}
	
	/**
	 * Determines whether the Hub contains the specified object, with an option to
	 * restrict the check to recently added objects.
	 *
	 * <p>This method uses direct lookup, key resolution, or Hub-level OAObject
	 * presence checks depending on the Hub configuration and object type.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to check for
	 * @param bJustAdded if {@code true}, only checks the most recently added items
	 * @return {@code true} if the Hub contains the object, otherwise {@code false}
	 */
	public static boolean contains(Hub hub, Object obj, final boolean bJustAdded) {
        if (hub == null || obj == null) return false;
        
        final int size = hub.data.vector.size();
        if (size == 0) return false;

        if (bJustAdded) {
            for (int i=1; i<3; i++) {
                if (hub.data.vector.elementAt(size-i) == obj) {
                    return true;
                }
            }
        }
        
        if (size < 35) {
            if (size == 0) return false;
            boolean b = containsDirect(hub, obj);
            if (b) return true;
            if (obj.getClass().equals(hub.getObjectClass())) return false;
        }
        
        if (!(obj instanceof OAObject)) {
            if (!hub.data.isOAObjectFlag()) {
                return containsDirect(hub, obj);
            }
            // oaObjectKey, or Id value
            obj = OAObjectCacheDelegate.get(hub.getObjectClass(), obj);
            if (obj == null) return false;
        }        
        
        if (!hub.data.isOAObjectFlag()) {
            return containsDirect(hub, obj);
        }
        
        boolean b = OAObjectHubDelegate.isAlreadyInHub((OAObject) obj, hub);
        return b;
    }
	
	/**
	 * Performs a direct lookup to determine whether the Hub contains the object.
	 *
	 * <p>For large Hubs with a sort listener, a quicksort-based search may be used.</p>
	 *
	 * @param hub the Hub to search
	 * @param obj the object to check for
	 * @return {@code true} if the object is present, otherwise {@code false}
	 */
    public static boolean containsDirect(Hub hub, Object obj) {
        if (hub == null || obj == null) return false;
        int x = hub.data.vector.size();
        if (x == 0) return false;
        if (x > 125) {
            if (hub.data.getSortListener() != null) {
                x = findUsingQuickSort(hub, obj);
                if (x == 1) return true;
                if (x == 2) return false;
                if (x == -1) return false;
                if (x == -3) return false;
            }
        }
        return hub.data.vector.contains(obj);
    }
    
    /**
     * Performs a binary search on a sorted Hub to determine whether it contains the object.
     *
     * @param thisHub the Hub to search
     * @param obj the object to check
     * @return status code indicating match, mismatch, or invalid conditions
     */
    private static int findUsingQuickSort(final Hub thisHub, final Object obj) {
        if (thisHub == null || obj == null) return -1;
        
        HubSortListener hsl = thisHub.data.getSortListener(); 
        if (hsl == null) return -2;
        Class cx = thisHub.getObjectClass();
        if (cx == null || !cx.equals(obj.getClass())) return -3;
        
        int head = -1;
        int tail = thisHub.data.vector.size();
        for ( ;; ) {
            if (head+1 >= tail) {
                break;
            }
            
            int i = ((tail - head) / 2);
            i += head;

            if (i == head) i++;
            else if (i == tail) i--;
            
            Object cobj = thisHub.elementAt(i);
            if (obj == cobj || obj.equals(cobj)) return 1;
            int c = hsl.comparator.compare(obj, cobj);

            if (c == 0) {
                int iHold = i;
                // see if it's already in the list
                for ( ; i>head; i--) {
                    cobj = thisHub.elementAt(i);
                    if (cobj == null) continue;
                    if (obj == cobj || obj.equals(cobj)) return 1;
                    if (hsl.comparator.compare(obj, cobj) != 0) break;
                }
                for (i=iHold+1; i < tail;i++) {
                    cobj = thisHub.elementAt(i);
                    if (cobj == null) continue;
                    if (obj == cobj || obj.equals(cobj)) return 1;
                    if (hsl.comparator.compare(obj, cobj) != 0) break;
                }
                break;
            }
            else if (c < 0) {
                tail = i;
            }
            else {
                head = i;
            }
        }
        return 2;
    }

    /**
     * Determines whether the Hub participates in a recursive link structure.
     *
     * @param thisHub the Hub to check
     * @return {@code true} if this Hub is used recursively, otherwise {@code false}
     */
    public static boolean isHubBeingUsedAsRecursive(Hub thisHub) {
        if (thisHub == null) return false;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li == null) {
            return (thisHub == thisHub.getRootHub());
        }
        return li.getReverseLinkInfo().getRecursive();
    }
    
    /**
     * Sets the Hub’s track-changes flag.
     *
     * @param thisHub the Hub to update
     * @param b the new track-changes state
     */
    public static void setTrackChanges(Hub thisHub, boolean b) {
        thisHub.data.setTrackChanges(b);        
    }

    /**
     * Returns the Hub’s track-changes flag.
     *
     * @param thisHub the Hub to query
     * @return {@code true} if track changes is enabled, otherwise {@code false}
     */
    public static boolean getTrackChanges(Hub thisHub) {
        return thisHub != null && thisHub.data.getTrackChanges();        
    }
    
    /**
     * Returns whether the Hub is in a load-all-data state.
     *
     * @param thisHub the Hub to check
     * @return {@code true} if loading all data, otherwise {@code false}
     */
    public static boolean isLoadingAllData(Hub thisHub) {
        if (thisHub == null) return false;
        boolean b;
        synchronized (thisHub.data) {
            b = (thisHub != null && thisHub.data.isLoadingAllData());
        }
        return b;
    }
    
    /**
     * Sets the Hub’s load-all-data flag.
     *
     * @param thisHub the Hub to update
     * @param b the new load-all-data state
     * @return the previous state
     */
    public static boolean setLoadingAllData(Hub thisHub, boolean b) {
        boolean bx = false;
        if (thisHub != null) {
            synchronized (thisHub.data) {
                bx = thisHub.data.setLoadingAllData(b);
            }
        }
        return bx;
    }
    
    /**
     * Sets the Hub’s load-all-data flag and associates it with a thread.
     *
     * @param thisHub the Hub to update
     * @param b the new state
     * @param thread the thread to associate with the load-all flag
     * @return the previous state
     */
    public static boolean setLoadingAllData(Hub thisHub, boolean b, Thread thread) {
        boolean bx = false;
        if (thisHub != null) {
            synchronized (thisHub.data) {
                bx = thisHub.data.setLoadingAllData(b, thread);
            }
        }
        return bx;
    }
}
