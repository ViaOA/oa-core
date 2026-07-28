package com.viaoa.oa.service.hub;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.hub.sort.HubSortListener;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.*;

/**
 * Provides low-level access to Hub storage and membership data.
 */

public abstract class HubDataService {
	private final Logger LOG = Logger.getLogger(HubDataService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubDataService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	
	/**
	 * Assigns the object class for this hub. The class cannot be changed if the hub
	 * already contains objects, has detail hubs, has a master object, or is shared.
	 * If validation passes, the hub's object class is updated.
	 *
	 * @param thisHub  the hub whose object class is being changed
	 * @param objClass the new object class
	 * @throws RuntimeException if the object class cannot be changed due to
	 *                          existing state constraints
	 */
	public <T extends OAObject> void setObjectClass(Hub<T> thisHub, Class<T> objClass) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		final HubDataMaster hdm = faHub.getHubDataMaster(thisHub);
		final HubDataUnique<T> hdu = faHub.getHubDataUnique(thisHub);
		
		Class<? extends OAObject> cx = faHub.getHubData(thisHub).getObjClass();
		
		if (cx != null && !cx.equals(objClass) && !cx.equals(OAObject.class)) {
			if (callHubDataGetCurrentSize(thisHub) > 0 || (hdu.getVecHubDetail() != null && hdu.getVecHubDetail().size() > 0)) {
				throw new RuntimeException("cant change object class if objects are in hub");
			}
			if (hdm.getMasterHub() != null || hdm.getMasterObject() != null) {
				throw new RuntimeException("cant change object class if masterObject exists");
			}
			if (hdu.getSharedHub() != null || callHubShareGetSharedWeakHubSize(thisHub) > 0) {
				throw new RuntimeException("cant change object class since this is a shared hub.");
			}
		}
		// 20141111 removed since the select could be valid
		// srvcHub.getHubSelectService().cancelSelect(thisHub, true);
		faHub.getHubData(thisHub).setObjClass(objClass);

		/* 20141111 not needed here
		if (objClass != null) {
		    // find out if class is OAObject
			thisHub.data.setOAObjectFlag(OAObject.class.isAssignableFrom(objClass));
			// thisHub.data.setObjectInfo(srvcObject.getOAObjectInfoService().getOAObjectInfo(objClass));
		}
		else {
		    thisHub.data.setObjectInfo(null);
		    thisHub.data.setOAObjectFlag(false);
		}
		*/
	}
	
	
    /**
     * Clears all internal Hub data structures and resets tracking state.
     * Removes all elements from the Hub’s vector and clears add/remove
     * tracking lists. Drops the {@code hubDatax} extension if it is no
     * longer needed, resets the changed flag, and increments the change
     * counter.
     *
     * @param thisHub the hub whose internal state is being reset
     */
	public <T extends OAObject> void clearAllAndReset(Hub<T> thisHub) {
		final HubData<T> hd = faHub.getHubData(thisHub);
    	synchronized (hd) {
    		Vector<T> v = hd.getVecAdd();
            if (v != null) v.removeAllElements();
            
    		v = hd.getVecRemove();
    		if (v != null) v.removeAllElements();
    		
    		hd.getVector().removeAllElements();
    	
            // 20160407
            if (hd.getHubDatax() != null) {
                if (!hd.getHubDatax().isNeeded()) hd.setHubDataxNull();
            }
        	hd.setChanged(false);
        	hd.incrementChangeCount();
    	}
	}
	
	/**
	 * Ensures that the Hub’s underlying vector has capacity for at least
	 * the specified number of elements.
	 *
	 * @param thisHub the hub whose vector capacity is being checked
	 * @param size    the minimum capacity required
	 */
	public void ensureCapacity(Hub<?> thisHub, int size) {
		//qqqqqq method was protected
		faHub.getHubData(thisHub).getVector().ensureCapacity(size);
	}
	
	/**
	 * Trims the Hub’s underlying vector to its current size. No-op when
	 * serialization is in progress and the vector is null.
	 *
	 * @param thisHub the hub whose vector should be trimmed
	 */
	public <T extends OAObject> void resizeToFit(Hub<T> thisHub) {
		Vector<T> v = faHub.getHubData(thisHub).getVector();
		if (v == null) return; // could be called during serialization
		v.trimToSize();
	}

	/**
	 * Clears the Hub’s add/remove tracking lists and resets the changed
	 * flag when those lists become empty. Drops {@code hubDatax} if it is
	 * no longer required. Sends a remote clear-changes event when any
	 * tracked changes existed.
	 *
	 * @param thisHub the hub whose change tracking is being cleared
	 */
	public <T extends OAObject> void clearHubChanges(Hub<T> thisHub) {
	    if (thisHub == null) return;
        boolean bSendEvent = false;
        synchronized (faHub.getHubData(thisHub)) {
            Vector<T> v = faHub.getHubData(thisHub).getVecAdd(); 
            if (v != null) {
                bSendEvent = v.size() > 0;
                v.removeAllElements();
            }
            v = faHub.getHubData(thisHub).getVecRemove(); 
            if (v != null) {
                bSendEvent = bSendEvent || v.size() > 0;
                v.removeAllElements();
            }
            
            
            if (faHub.getHubData(thisHub).getHubDatax() != null) {
                if (!faHub.getHubData(thisHub).getHubDatax().isNeeded()) {
                	faHub.getHubData(thisHub).setHubDataxNull();
                }
                if (faHub.getHubData(thisHub).getChanged()) {
                    boolean b = (faHub.getHubData(thisHub).getHubDatax() == null);
                    if (!b) {
                    	v = faHub.getHubData(thisHub).getVecAdd();
                        b = (v == null || v.size() == 0);
                    	v = faHub.getHubData(thisHub).getVecRemove();
                        b &= (v == null || v.size() == 0);
                    }
                    if (b) {
                    	faHub.getHubData(thisHub).setChanged(false);
                    	faHub.getHubData(thisHub).incrementChangeCount();
                    }
                }
            }
        }
        if (bSendEvent) {
        	callHubCSClearHubChanges(thisHub);
        }
	}
	
	/**
	 * Copies all elements from the Hub’s vector into the supplied array.
	 * The copy is performed under synchronization for thread safety.
	 *
	 * @param thisHub the hub whose elements are being copied
	 * @param anArray the destination array
	 */
    public <T extends OAObject> void copyInto(Hub<T> thisHub, T anArray[]) {
    	//qqqqqqqq method was protected
        synchronized (faHub.getHubData(thisHub)) {
        	faHub.getHubData(thisHub).getVector().copyInto(anArray);
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
    @SuppressWarnings({"unchecked"})
	public <T extends OAObject> T[] toArray(Hub<T> thisHub) {
	    thisHub.getSize(); // call before sync, in case it needs to load
        T[] objs;
        synchronized (faHub.getHubData(thisHub)) {
        	objs = (T[]) Array.newInstance(thisHub.getObjectClass(), thisHub.getSize());
        	faHub.getHubData(thisHub).getVector().copyInto(objs);
        }
	    return objs;
	}

	/**
	 * Returns the current number of elements in the Hub.
	 *
	 * @param thisHub the hub whose size is being retrieved
	 * @return the number of elements in the Hub
	 */
    public int getCurrentSize(Hub<?> thisHub) {
        return faHub.getHubData(thisHub).getVector().size();
    }
	

    /**
     * Copies the underlying vector from one Hub to another as part of
     * cloning operations.
     *
     * @param thisHub the source hub
     * @param newHub  the destination hub
     */
    @SuppressWarnings({"unchecked"})
    public <T extends OAObject> void _clone(Hub<T> thisHub, Hub<T> newHub) {
    	Vector<T> v = (Vector<T>) faHub.getHubData(thisHub).getVector().clone();
        faHub.getHubData(newHub).setVector(v);
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
    public <T extends OAObject> int _remove(Hub<T> thisHub, T obj, boolean bDeleting, boolean bIsRemovingAll) {
        int pos = 0;
        try {
            callThreadLocalLock(thisHub);
            pos = _remove2(thisHub, obj, bDeleting, bIsRemovingAll);
        }
        finally {
            callThreadLocalUnlock(thisHub);
        }
        if (!bIsRemovingAll) {
        	callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
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
    private <T extends OAObject> int _remove2(Hub<T> thisHub, T obj, boolean bDeleting, boolean bIsRemovingAll) {
        
    	int pos;
        if (bIsRemovingAll) {
            pos = -1;
        }
        else {
	        pos = thisHub.getPos(obj);
	        if (pos >= 0) {
	        	faHub.getHubData(thisHub).getVector().removeElementAt(pos);
	        }
        }

	    if (pos >= 0) {
	        boolean b = (obj instanceof OAObject);	        
            if (b) {
                b = (faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges());
                if (!b && !callSyncIsSingleUserOrServer()) {
                    if ( ((OAObject) obj).isChanged()) {
                        if (faHub.getHubDataMaster(thisHub).getMasterObject() != null) {
                            // could be ServerRoot
                            OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
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
	    		Vector<T> v = faHub.getHubData(thisHub).getVecAdd();
	            if (v != null && v.removeElement(obj)) {
	                // no-op
	            }
	            else {
	                if (!bDeleting) {
                    	Vector<T> vec = createVecRemove(thisHub);
                    	if (!vec.contains(obj)) vec.addElement(obj);
	                }
	            }
	    		v = faHub.getHubData(thisHub).getVecAdd();
	    		Vector<T> v2 = faHub.getHubData(thisHub).getVecRemove();
		        thisHub.setChanged( (v != null && v.size() > 0) || (v2 != null && v2.size() > 0) );
		    }
		    else {
		    	callHubStatusSetChanged(thisHub, true);
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
    public <T extends OAObject> boolean _add(Hub<T> thisHub, T obj, boolean bHasLock, boolean bCheckContains) {
        boolean b = false;
        try {
            if (!bHasLock) callThreadLocalLock(thisHub);
            b = _add2(thisHub, obj, bCheckContains);
        }
        finally {
            if (!bHasLock ) callThreadLocalUnlock(thisHub);
        }
        callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
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
    private <T extends OAObject> boolean _add2(Hub<T> thisHub, T obj, final boolean bCheckContains) {
        if (bCheckContains && thisHub.contains(obj)) return false;
        faHub.getHubData(thisHub).getVector().addElement(obj);
        
        int xx = faHub.getHubData(thisHub).getVector().size();
        if (xx > 499 && faHub.getHubDataMaster(thisHub).getMasterObject() != null && (xx%100)==0) {
            if (xx < 1000 || (xx%1000)==0) LOG.fine("large Hub with masterObject, Hub="+thisHub);
            if ((xx%10000)==0) {
                LOG.fine("large Hub with masterObject, Hub="+thisHub);
            }
        }

        if (!callThreadLocalIsLoading()) {
        	
            if ((faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges()) && (obj instanceof OAObject)) {
                Vector<T> v  = faHub.getHubData(thisHub).getVecRemove();
            	if (v != null && v.contains(obj)) {
            		v.removeElement(obj);
                }
                else {
                    createVecAdd(thisHub).addElement(obj);
                }
                Vector<T> v2  = faHub.getHubData(thisHub).getVecAdd();
                thisHub.setChanged( (v2 != null && v2.size() > 0) || (v != null && v.size() > 0) );
            }
            else {
                thisHub.setChanged(true);
            }
        }
        faHub.getHubData(thisHub).incrementChangeCount();
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
    public <T extends OAObject> boolean _insert(Hub<T> thisHub, T obj, int pos, boolean bIsLocked) {
    	//qqqqqqqqq method was protected
        boolean b = false;
        try {
            if (!bIsLocked) callThreadLocalLock(thisHub);
            //was b = _insert2(thisHub, key, obj, pos, bLock);
            b = _insert2(thisHub, obj, pos);
        }
        finally {
            if (!bIsLocked) callThreadLocalUnlock(thisHub);
        }
        
        callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
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
	private <T extends OAObject> boolean _insert2(Hub<T> thisHub, T obj, int pos) {
        boolean b = callThreadLocalIsLoading();

        faHub.getHubData(thisHub).getVector().insertElementAt(obj, pos);
    	if (!b) {
            if ((faHub.getHubDataMaster(thisHub).getTrackChanges() || faHub.getHubData(thisHub).getTrackChanges()) && (obj instanceof OAObject)) {
                Vector<T> v  = faHub.getHubData(thisHub).getVecRemove();
            	if (v != null && v.contains(obj)) {
            		v.removeElement(obj);
                }
                else {
                    createVecAdd(thisHub).addElement(obj);
                }
                Vector<T> v2  = faHub.getHubData(thisHub).getVecAdd();
                thisHub.setChanged( (v2 != null && v2.size() > 0) || (v != null && v.size() > 0) );
            }
    	    else thisHub.setChanged(true);
    	}
        faHub.getHubData(thisHub).incrementChangeCount();
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
	public <T extends OAObject> void _move(Hub<T> thisHub, T obj, int posFrom, int posTo) {
        try {
            callThreadLocalLock(thisHub);
            faHub.getHubData(thisHub).incrementChangeCount();
            
            Vector<T> v = faHub.getHubData(thisHub).getVector();
            v.removeElementAt(posFrom);
            v.insertElementAt(obj, posTo);
        }
        finally {
            callThreadLocalUnlock(thisHub);
        }
        callRemoteThreadStartNextThread(); // if this is OAClientThread, so that OAClientMessageHandler can continue with next message
	}
	
	/**
	 * Adds all objects currently in the Hub to the add-tracking vector,
	 * creating the vector when necessary. No-op when the hub is null.
	 *
	 * @param thisHub the hub whose contents are being tracked as added
	 */
	public <T extends OAObject> void addAllToAddVector(Hub<T> thisHub) {
	    if (thisHub == null) return;
        createVecAdd(thisHub);
	    for (T objx :  thisHub) {
	    	faHub.getHubData(thisHub).getVecAdd().add(objx);	        
	    }
	}
	
	/**
	 * Creates the Hub’s add-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose add-tracking vector is being created
	 * @return the add-tracking vector
	 */
	public <T extends OAObject> Vector<T> createVecAdd(Hub<T> thisHub) {
        if (faHub.getHubData(thisHub).getVecAdd() == null) {
	        synchronized (faHub.getHubData(thisHub)) {
	            if (faHub.getHubData(thisHub).getVecAdd() == null) {
	            	faHub.getHubData(thisHub).setVecAdd(new Vector<T>(10, 10));
	            }
	        }
        }
        return faHub.getHubData(thisHub).getVecAdd();
	}
	
	/**
	 * Creates the Hub’s remove-tracking vector if it does not already exist.
	 * Ensures thread-safe initialization using synchronization.
	 *
	 * @param thisHub the hub whose remove-tracking vector is being created
	 * @return the remove-tracking vector
	 */
	public <T extends OAObject> Vector<T> createVecRemove(Hub<T> thisHub) {
        if (faHub.getHubData(thisHub).getVecRemove() == null) {
	        synchronized (faHub.getHubData(thisHub)) {
	            if (faHub.getHubData(thisHub).getVecRemove() == null) {
	            	faHub.getHubData(thisHub).setVecRemove(new Vector<T>(10, 10));
	            }
	        }
		}
        return faHub.getHubData(thisHub).getVecRemove();
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
    @SuppressWarnings("unchecked")
	public <T extends OAObject> T[] getAddedObjects(Hub<T> thisHub) {
        final Object hd = faHub.getHubData(thisHub);        
        synchronized (hd) {
            final Vector<T> v = faHub.getHubData(thisHub).getVecAdd();
            if (v == null || v.isEmpty()) return null;
        
            final Class<T> cx = (Class<T>) thisHub.getObjectClass(); // or other hub API you have

            int x = v.size();
            final T[] objs = (T[]) Array.newInstance(cx, x);
        	
        	
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
    @SuppressWarnings("unchecked")
	public <T extends OAObject> T[] getRemovedObjects(Hub<T> thisHub) {
        final Object hd = faHub.getHubData(thisHub);        
        synchronized (hd) {
            final Vector<T> v = faHub.getHubData(thisHub).getVecRemove();
            if (v == null || v.isEmpty()) return null;
        
            final Class<T> cx = (Class<T>) thisHub.getObjectClass(); // or other hub API you have

            int x = v.size();
            final T[] objs = (T[]) Array.newInstance(cx, x);
        	
			if (x > 0) v.copyInto(objs);
			return objs;
        }
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
	public <T extends OAObject> T getObject(final Hub<T> thisHub, Object key) {
		if (thisHub == null || key == null) return null;
	    if (!(key instanceof OAObjectKey)) {
	    	if (key instanceof OAObject) key = callObjectKeyGetKey((OAObject) key);
	    	else key = callObjectKeyCreateObjectKey(thisHub.getObjectClass(), key);
	    }
		for (int i=0; ; i++) {
			T obj = getObjectAt(thisHub, i);
			if (obj == null) break;
			if (obj == key) return obj;
			if (obj instanceof OAObject) {
				OAObjectKey k = callObjectKeyGetKey((OAObject) obj);
				// note: dont send class to isForSameOAObject: dont want it to do a cache lookup
				if (callObjectKeyIsForSameOAObject(null, k, (OAObjectKey) key)) return obj;
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
	public <T extends OAObject> T getObjectAt(Hub<T> thisHub, int pos) {
	    T ho;
	    if (pos < 0) return null;
	    
	    final Vector<T> v = (Vector<T>) faHub.getHubData(thisHub).getVector();
	    
	    int size = v.size();
	    if (pos < size) {
	        T obj = null;
	        try {
	        	obj = v.elementAt(pos);
	        }
	        catch (Exception e) {
	        	obj = null;  // hub could have changed, and pos is not valid anymore
	        }
	        if (obj != null) return obj;
	    }
	
	    if (!callHubSelectIsMoreData(thisHub)) {
	        return null;
	    }
	
	    // fetch more records from data source
	    for ( ; pos >= v.size() && callHubSelectIsMoreData(thisHub) ; ) {
	    	callHubSelectFetchMore(thisHub);
	    }
	    ho = getObjectAt(thisHub, pos);
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
	public <T extends OAObject> int getPos(final Hub<T> thisHub, Object object, final boolean adjustMaster, final boolean bUpdateLink) {
	    int pos;
	    if (object == null || thisHub == null) return -1;

	    if (!(object instanceof OAObject)) {
	        if (OAObject.class.isAssignableFrom(object.getClass())) {  // could be hub of strings
	            object = callHubFindGetRealObject(thisHub, object);
	        }
	    }
	    pos = -1;
	    if (object != null) {
	        for ( ;; ) {
	            pos = faHub.getHubData(thisHub).getVector().indexOf(object);
	            if (pos >= 0) return pos;
	            if (!callHubSelectIsMoreData(thisHub)) break;
	            callHubSelectFetchMore(thisHub);
	        }
	    }

	    
        if (pos < 0 && adjustMaster && (faHub.getHubDataUnique(thisHub).getSharedHub() != null || faHub.getHubDataMaster(thisHub).getMasterHub() != null)) {
            OALinkInfo liRecursiveOne = callObjectInfoGetRecursiveLinkInfo(thisHub.getOAObjectInfo(), OALinkInfo.ONE);

            // need to verify that this hub is recursive with masterObject
            if (liRecursiveOne != null) {  
                OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
                if (li != null) {
                    li = callObjectInfoGetReverseLinkInfo(li);
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
                Object parent = callObjectReflectGetProperty((OAObject)object, liRecursiveOne.getName());
                if (parent == null) {  // might be in root hub
                    Hub h = thisHub.getRootHub();  // could be owner of hub
                    if (h != null && h != thisHub && faHub.getHubDataUnique(thisHub).getSharedHub() != h) {
                    	callHubShareSetSharedHub(thisHub, h, false);
                        pos = getPos(h, object, adjustMaster, bUpdateLink);
                    }
                    if (pos < 0) {
                        bUseMaster = true;  // adjust master/owner for this recursive hub
                    }
                }
                else {
                	OALinkInfo liMany = callObjectInfoGetReverseLinkInfo(liRecursiveOne);
                	if (liMany != null) {
                        hashRecursiveHubDetail.computeIfAbsent(thisHub, k -> {
                            HubDataMaster dm = faHub.getHubDataMaster(thisHub);
                            if (dm.getDetailToMasterLinkInfo() != null) return dm.getDetailToMasterLinkInfo();
                        	return null;
                        });
                    	Object val = callObjectReflectGetProperty((OAObject)parent, liMany.getName());
                    	// reassign the sharedHub to correct recursive hub in the hierarchy
                    	callHubShareSetSharedHub(thisHub, (Hub) val, false, object);
                        pos = getPos((Hub)val, object, adjustMaster, bUpdateLink);
                	}
                }
            }

            if (bUseMaster) {
                if (faHub.getHubDataMaster(thisHub).getMasterHub() != null && faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo() != null) {  
                    // only do this if a masterHub, since a hub that has a masterObject (w/o hub) should not do this adjustment
                    Object parent = callObjectReflectGetProperty((OAObject)object, faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo().getName());
                    if (parent != null) {
                        OALinkInfo li = callObjectInfoGetReverseLinkInfo(faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo());
                        if (li != null) {
                            hashRecursiveHubDetail.computeIfAbsent(thisHub, k -> {
                                HubDataMaster dm = faHub.getHubDataMaster(thisHub);
                                if (dm.getDetailToMasterLinkInfo() != null) return  dm.getDetailToMasterLinkInfo();
                                return null;
                            });
                            Object val = callObjectReflectGetProperty((OAObject)parent, li.getName());
                            callHubShareSetSharedHub(thisHub, (Hub<T>) val, false, object);
                            pos = getPos((Hub<?>)val, object, adjustMaster, bUpdateLink);
                        }
                    }
                }
                else {
                    // see if it was a master/detail that was reassigned (shared) to a child hub that is recursive
                    OALinkInfo li = hashRecursiveHubDetail.get(thisHub);
                    if (li != null) {
                        Object parent = callObjectReflectGetProperty((OAObject)object, li.getName());
                        if (parent != null) {
                            Object val = callObjectReflectGetProperty((OAObject)parent, li.getReverseName());
                            callHubShareSetSharedHub(thisHub, (Hub<T>) val, false, object);
                            pos = getPos((Hub<?>)val, object, adjustMaster, bUpdateLink);
                        }
                    }
                }
            }
        }
        

        if (pos < 0 && adjustMaster) {
            if (callHubDetailSetMasterHubActiveObject(thisHub, (T) object, bUpdateLink)) {
                pos = getPos(thisHub, object, false, false);
            }
        }
	    return pos;
	}
	
    /**
     * Used by srvcHub.getHubDataService().getPos(..) when finding the object for recursive links
     */
    private final Map<Hub<?>, OALinkInfo> hashRecursiveHubDetail = new ConcurrentHashMap<>(11, 0.75F);
    
    /**
     * Removes the given object from the Hub’s change-tracking “added” list.
     *
     * <p>Also updates the Hub’s change flag if the removal results in no remaining
     * tracked additions or removals.</p>
     *
     * @param thisHub the Hub whose added list is modified
     * @param obj the object to remove from the added list
     */
	public <T extends OAObject> void removeFromAddedList(Hub<T> thisHub, T obj) {
	    synchronized (faHub.getHubData(thisHub)) {
            if (faHub.getHubData(thisHub).getHubDatax() == null) return;
	    	Vector<T> v = faHub.getHubData(thisHub).getVecAdd();
	    	if (v != null) v.remove(obj);

            if (faHub.getHubData(thisHub).getHubDatax() != null) {
                if (!faHub.getHubData(thisHub).getHubDatax().isNeeded()) {
                	faHub.getHubData(thisHub).setHubDataxNull();
                }
                if (faHub.getHubData(thisHub).getChanged()) {
                    boolean b = (faHub.getHubData(thisHub).getHubDatax() == null);
                    if (!b) {
                    	v = faHub.getHubData(thisHub).getVecAdd();
                        b = (v == null || v.size() == 0);
                    	v = faHub.getHubData(thisHub).getVecRemove();
                        b &= (v == null || v.size() == 0);
                    }
                    if (b) {
                    	faHub.getHubData(thisHub).setChanged(false);
                    	faHub.getHubData(thisHub).incrementChangeCount();
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
	public <T extends OAObject> void removeFromRemovedList(Hub<T> thisHub, T obj) {
        if (faHub.getHubData(thisHub).getHubDatax() == null) return;
	    synchronized (faHub.getHubData(thisHub)) {
	    	Vector<T> v = faHub.getHubData(thisHub).getVecRemove();
	    	if (v != null) v.remove(obj);
	    	
            if (faHub.getHubData(thisHub).getHubDatax() != null) {
                if (!faHub.getHubData(thisHub).getHubDatax().isNeeded()) {
                	faHub.getHubData(thisHub).setHubDataxNull();
                }
                if (faHub.getHubData(thisHub).getChanged()) {
                    boolean b = (faHub.getHubData(thisHub).getHubDatax() == null);
                    if (!b) {
                    	v = faHub.getHubData(thisHub).getVecAdd();
                        b = (v == null || v.size() == 0);
                    	v = faHub.getHubData(thisHub).getVecRemove();
                        b &= (v == null || v.size() == 0);
                    }
                    if (b) {
                    	faHub.getHubData(thisHub).setChanged(false);
                    	faHub.getHubData(thisHub).incrementChangeCount();
                    }
                }
            }
	    }
	}


	/**
	 * Returns the changed for the supplied Hub context.
	 *
	 * @param thisHub method input
	 * @return result value
	 */


	public boolean getChanged(Hub<?> thisHub) {
    	return faHub.getHubData(thisHub).getChanged();
	}
	
	
	/**
	 * Returns the Hub’s change counter value.
	 *
	 * @param thisHub the Hub whose change count is requested
	 * @return the current change count
	 */
	public int getChangeCount(Hub<?> thisHub) {
    	return faHub.getHubData(thisHub).getChangeCount();
	}
	
	/**
	 * Increments the Hub’s change counter.
	 *
	 * @param thisHub the Hub whose counter is incremented
	 */
	public void incChangeCount(Hub<?> thisHub) {
    	faHub.getHubData(thisHub).incrementChangeCount();
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
	public boolean contains(Hub<?> hub, Object obj) {
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
	public <T extends OAObject> boolean contains(Hub<T> hub, Object obj, final boolean bJustAdded) {
        if (hub == null || obj == null) return false;
        
        final int size = faHub.getHubData(hub).getVector().size();
        if (size == 0) return false;

        if (bJustAdded) {
            for (int i=1; i<3 && i<=size; i++) {
                if (faHub.getHubData(hub).getVector().elementAt(size-i) == obj) {
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
            // oaObjectKey, or Id value
            obj = callObjectCacheGetUsingKey(hub.getObjectClass(), obj);
            if (obj == null) return false;
        }        
        
        boolean b = callObjectHubIsAlreadyInHub((T) obj, hub);
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
    public <T extends OAObject> boolean containsDirect(Hub<T> hub, Object obj) {
        if (hub == null || obj == null) return false;
        int x = faHub.getHubData(hub).getVector().size();
        if (x == 0) return false;
        if (x > 125) {
            if (faHub.getHubData(hub).getSortListener() != null) {
                x = findUsingQuickSort(hub, obj);
                if (x == 1) return true;
                if (x == 2) return false;
                if (x == -1) return false;
                if (x == -3) return false;
            }
        }
        return faHub.getHubData(hub).getVector().contains(obj);
    }
    
    /**
     * Performs a binary search on a sorted Hub to determine whether it contains the object.
     *
     * @param thisHub the Hub to search
     * @param obj the object to check
     * @return status code indicating match, mismatch, or invalid conditions
     */
    private int findUsingQuickSort(final Hub<?> thisHub, final Object obj) {
        if (thisHub == null || obj == null) return -1;
        
        HubSortListener hsl = faHub.getHubData(thisHub).getSortListener(); 
        if (hsl == null) return -2;
        Class<? extends OAObject> cx = thisHub.getObjectClass();
        if (cx == null || !cx.equals(obj.getClass())) return -3;
        
        int head = -1;
        int tail = faHub.getHubData(thisHub).getVector().size();
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
            int c = hsl.getComparator().compare(obj, cobj);

            if (c == 0) {
                int iHold = i;
                // see if it's already in the list
                for ( ; i>head; i--) {
                    cobj = thisHub.elementAt(i);
                    if (cobj == null) continue;
                    if (obj == cobj || obj.equals(cobj)) return 1;
                    if (hsl.getComparator().compare(obj, cobj) != 0) break;
                }
                for (i=iHold+1; i < tail;i++) {
                    cobj = thisHub.elementAt(i);
                    if (cobj == null) continue;
                    if (obj == cobj || obj.equals(cobj)) return 1;
                    if (hsl.getComparator().compare(obj, cobj) != 0) break;
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
    public boolean isHubBeingUsedAsRecursive(Hub<?> thisHub) {
        if (thisHub == null) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
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
    public void setTrackChanges(Hub<?> thisHub, boolean b) {
    	faHub.getHubData(thisHub).setTrackChanges(b);        
    }

    /**
     * Returns the Hub’s track-changes flag.
     *
     * @param thisHub the Hub to query
     * @return {@code true} if track changes is enabled, otherwise {@code false}
     */
    public boolean getTrackChanges(Hub<?> thisHub) {
    	return faHub.getHubData(thisHub).getTrackChanges();        
    }
    
    /**
     * Returns whether the Hub is in a load-all-data state.
     *
     * @param thisHub the Hub to check
     * @return {@code true} if loading all data, otherwise {@code false}
     */
    public boolean isLoadingAllData(Hub<?> thisHub) {
        if (thisHub == null) return false;
        boolean b;
        synchronized (faHub.getHubData(thisHub)) {
            b = (thisHub != null && faHub.getHubData(thisHub).isLoadingAllData());
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
    public boolean setLoadingAllData(Hub<?> thisHub, boolean b) {
        boolean bx = false;
        if (thisHub != null) {
            synchronized (faHub.getHubData(thisHub)) {
                bx = faHub.getHubData(thisHub).setLoadingAllData(b);
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
    public boolean setLoadingAllData(Hub<?> thisHub, boolean b, Thread thread) {
        boolean bx = false;
        if (thisHub != null) {
            synchronized (faHub.getHubData(thisHub)) {
                bx = faHub.getHubData(thisHub).setLoadingAllData(b, thread);
            }
        }
        return bx;
    }

	/**
	 * Dependency hook used by this service for ObjectKeyGetKey behavior.
	 *
	 * @param oaObj method input
	 * @return result value
	 */

	public abstract OAObjectKey callObjectKeyGetKey(OAObject oaObj);
	/**
	 * Dependency hook used by this service for ObjectKeyIsForSameOAObject behavior.
	 *
	 * @param clazz method input
	 * @param ok1 method input
	 * @param ok2 method input
	 * @return result value
	 */
	public abstract boolean callObjectKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	/**
	 * Dependency hook used by this service for ObjectReflectGetObject behavior.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callObjectReflectGetObject(Class<T> clazz, Object key);
	/**
	 * Dependency hook used by this service for ObjectHubAddHub behavior.
	 *
	 * @param oaObj method input
	 * @param hub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callObjectHubAddHub(T oaObj, Hub<T> hub);
	/**
	 * Dependency hook used by this service for ObjectInfoGetRecursiveLinkInfo behavior.
	 *
	 * @param thisOI method input
	 * @param type method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type);
	/**
	 * Dependency hook used by this service for ObjectInfoGetReverseLinkInfo behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for ObjectReflectGetProperty behavior.
	 *
	 * @param oaObj method input
	 * @param propPath method input
	 * @return result value
	 */
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String propPath);
	/**
	 * Dependency hook used by this service for ObjectCacheGet behavior.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callObjectCacheGetUsingKey(Class<T> clazz, Object key);
	/**
	 * Dependency hook used by this service for ObjectHubIsAlreadyInHub behavior.
	 *
	 * @param oaObj method input
	 * @param hubFind method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callObjectHubIsAlreadyInHub(T oaObj, Hub<T> hubFind);
	/**
	 * Dependency hook used by this service for ObjectKeyCreateObjectKey behavior.
	 *
	 * @param c method input
	 * @param ids method input
	 * @return result value
	 */
	public abstract OAObjectKey callObjectKeyCreateObjectKey(final Class<? extends OAObject> c, final Object ...ids);
	/**
	 * Dependency hook used by this service for HubCSClearHubChanges behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract boolean callHubCSClearHubChanges(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDetailSetPropertyToMasterHub behavior.
	 *
	 * @param thisHub method input
	 * @param detailObject method input
	 * @param objMaster method input
	 */
	public abstract <T extends OAObject> void callHubDetailSetPropertyToMasterHub(Hub<T> thisHub, T detailObject, OAObject objMaster);
	/**
	 * Dependency hook used by this service for HubSelectIsMoreData behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract boolean callHubSelectIsMoreData(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubSelectFetchMore behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract int callHubSelectFetchMore(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubFindGetRealObject behavior.
	 *
	 * @param hub method input
	 * @param object method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callHubFindGetRealObject(Hub<T> hub, Object object);
	/**
	 * Dependency hook used by this service for HubShareSetSharedHub behavior.
	 *
	 * @param thisHub method input
	 * @param sharedMasterHub method input
	 * @param shareActiveObject method input
	 */
	public abstract <T extends OAObject> void callHubShareSetSharedHub(Hub<T> thisHub, Hub<T> sharedMasterHub, boolean shareActiveObject);
	/**
	 * Dependency hook used by this service for HubShareSetSharedHub behavior.
	 *
	 * @param thisHub method input
	 * @param sharedMasterHub method input
	 * @param shareActiveObject method input
	 * @param newLinkValue method input
	 */
	public abstract <T extends OAObject> void callHubShareSetSharedHub(Hub<T> thisHub, Hub<T> sharedMasterHub, boolean shareActiveObject, Object newLinkValue);
	/**
	 * Dependency hook used by this service for HubDetailSetMasterHubActiveObject behavior.
	 *
	 * @param thisHub method input
	 * @param detailObject method input
	 * @param bUpdateLink method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubDetailSetMasterHubActiveObject(Hub<T> thisHub, T detailObject, boolean bUpdateLink);
	/**
	 * Dependency hook used by this service for ThreadLocalLock behavior.
	 *
	 * @param object method input
	 */
	public abstract void callThreadLocalLock(Object object);
	/**
	 * Dependency hook used by this service for ThreadLocalUnlock behavior.
	 *
	 * @param object method input
	 */
	public abstract void callThreadLocalUnlock(Object object);
	/**
	 * Dependency hook used by this service for RemoteThreadStartNextThread behavior.
	 */
	public abstract void callRemoteThreadStartNextThread();
	/**
	 * Dependency hook used by this service for ThreadLocalIsLoading behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service for SyncIsClient behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callSyncIsSingleUserOrServer();
	/**
	 * Dependency hook used by this service for HubDataGetCurrentSize behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract int callHubDataGetCurrentSize(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubShareGetSharedWeakHubSize behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract int callHubShareGetSharedWeakHubSize(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubStatusSetChanged behavior.
	 *
	 * @param thisHub method input
	 * @param b method input
	 */
	public abstract void callHubStatusSetChanged(Hub<?> thisHub, boolean b);
}
