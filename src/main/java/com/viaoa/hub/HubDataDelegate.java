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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.OASync;

/**
 * Main delegate that works with the HubData class.
 * All methods that have an "_" prefix should not be called directly, as there is
 * a calling method that should be used, that performs additional functionality.
 * If a method does not have the "_" prefix and is accessible, then it is ok
 * to call it, but will most likely have a matching method name in the Hub class.
 * @author vincevia
 */
public class HubDataDelegate {
	
    private static Logger LOG = Logger.getLogger(HubDataDelegate.class.getName());
    
	// used by HubSelectDelegate.select()
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
	
	protected static void ensureCapacity(Hub thisHub, int size) {
		thisHub.data.vector.ensureCapacity(size);
	}
	public static void resizeToFit(Hub thisHub) {
		if (thisHub.data.vector == null) return; // could be called during serialization
//LOG.config("resizing, from:"+thisHub.data.vector.capacity()+", to:"+x+", hub:"+thisHub);//qqqqqqqq                        
		thisHub.data.vector.trimToSize();
	}

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
	
    // 20150420
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
	
	
    protected static void copyInto(Hub thisHub, Object anArray[]) {
        synchronized (thisHub.data) {
            thisHub.data.vector.copyInto(anArray);
        }
    }

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

    public static int getCurrentSize(Hub thisHub) {
        return thisHub.data.vector.size();
    }
	
    /** called by Hub.clone(); */
    public static void _clone(Hub thisHub, Hub newHub) {
    	newHub.data.vector = (Vector) thisHub.data.vector.clone();
    }
    
	// called by HubAddRemoveDelegate
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
    
    private static int _remove2(Hub thisHub, Object obj, boolean bDeleting, boolean bIsRemovingAll) {
        int pos;
        if (bIsRemovingAll) {
            pos = -1;
            /*
            if (thisHub.data.vector.remove(obj)) pos = 0;
            else pos = -1;
            */
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
                            b = true;
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

	
	// called by HubAddRemoveDelegate.internalAdd
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
    //was: private static boolean _insert2(Hub thisHub, OAObjectKey key, Object obj, int pos, boolean bLock) {
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

	// called by HubAddRemoveDelegate.move
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
	
	public static void addAllToAddVector(Hub thisHub) {
	    if (thisHub == null) return;
        createVecAdd(thisHub);
	    for (Object objx :  thisHub) {
	        thisHub.data.getVecAdd().add(objx);	        
	    }
	}
	
	protected static Vector createVecAdd(Hub thisHub) {
        if (thisHub.data.getVecAdd() == null) {
	        synchronized (thisHub.data) {
	            if (thisHub.data.getVecAdd() == null) thisHub.data.setVecAdd(new Vector(10, 10));
	        }
        }
        return thisHub.data.getVecAdd();
	}
	protected static Vector createVecRemove(Hub thisHub) {
		if (thisHub.data.getVecRemove() == null) {
	        synchronized (thisHub.data) {
	            if (thisHub.data.getVecRemove() == null) thisHub.data.setVecRemove(new Vector(10,10));
	        }
		}
        return thisHub.data.getVecRemove();
	}
	
	// used to "know" which objects have been added to the Hub.
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
	// used to "know" which objects have been removed to the Hub.
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

	public static boolean getChanged(Hub thisHub) {
	    return (thisHub.data.changed);
	}
	
	public static Object getObject(Hub thisHub, Object key) {
		if (key == null) return null;
	    if (!(key instanceof OAObjectKey)) {
	    	if (key instanceof OAObject) key = OAObjectKeyDelegate.getKey((OAObject) key);
	    	else key = OAObjectKeyDelegate.convertToObjectKey(thisHub.getObjectClass(), key);
	    }
		for (int i=0; ; i++) {
			Object obj = getObjectAt(thisHub, i);
			if (obj == null) break;
			if (obj == key) return obj;
			if (obj instanceof OAObject) {
				Object k = OAObjectKeyDelegate.getKey((OAObject) obj);
				if (k.equals(key)) return obj;
			}
		}
		return null;
	}
	
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
	
	/*
	    Find the position for an object within the Hub.  If the object is not in the Hub and there
	    is a Master Hub from getDetailHub(), then the Master Hub will updated to the master object.
	    This will also check and adjust for recursive hubs.
	    <p>
	    Note: if masterHub (or one of its shared) has a linkHub, it will still be updated.
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
                        if (hashRecursiveHubDetail.get(thisHub) == null) {
                            HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
                            if (dm.liDetailToMaster != null) hashRecursiveHubDetail.put(thisHub, dm.liDetailToMaster);
                        }
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
                            if (hashRecursiveHubDetail.get(thisHub) == null) {
                                HubDataMaster dm = HubDetailDelegate.getDataMaster(thisHub);
                                if (dm.liDetailToMaster != null) hashRecursiveHubDetail.put(thisHub, dm.liDetailToMaster);
                            }
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
    static private final ConcurrentHashMap<Hub, OALinkInfo> hashRecursiveHubDetail = new ConcurrentHashMap<Hub, OALinkInfo>(11, 0.75F);
    
	
	
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
		Counter that is incremented on: add(), insert(), remove(), setting shared hub,
	    remove(), move(), sort(), select()
	    This is used by html/jsp components so that they "know" when/if Hub has changed,
	    which will cause them to be refreshed.
	*/
	public static int getChangeCount(Hub thisHub) {
	    return thisHub.data.changeCount;
	}
	
	protected static void incChangeCount(Hub thisHub) {
		thisHub.data.changeCount++;
	}

    /**
	    Counter that is incremented when a new list of objects is loaded: select, setSharedHub, and when
	    detail hubs list is changed to match the master hub's activeObject
	    This is used by html/jsp components so that they "know" when/if Hub has changed,
	    which will cause them to be refreshed.
	*/
/*	
	public static int getNewListCount(Hub thisHub) {
	    return thisHub.data.getNewListCount();
	}
*/
	public static boolean contains(Hub hub, Object obj) {
		return contains(hub, obj, false);
	}
	public static boolean contains(Hub hub, Object obj, final boolean bJustAdded) {
        if (hub == null || obj == null) return false;
        if (!(obj instanceof OAObject)) {
            if (!hub.data.isOAObjectFlag()) {
                return containsDirect(hub, obj);
            }
            obj = OAObjectCacheDelegate.get(hub.getObjectClass(), obj);
            if (obj == null) return false;
        }        
        
        int size = hub.data.vector.size(); 
        if (size < 25) {
            return containsDirect(hub, obj);
        }
        
        if (bJustAdded) {
        	for (int i=1; i<3; i++) {
        		if (hub.data.vector.elementAt(size-i) == obj) {
        			return true;
        		}
        	}
        }
        
        if (!hub.data.isOAObjectFlag()) {
            return containsDirect(hub, obj);
        }
        
        boolean b = OAObjectHubDelegate.isAlreadyInHub((OAObject) obj, hub);
        return b;
    }
    public static boolean containsDirect(Hub hub, Object obj) {
        if (hub == null || obj == null) return false;
        if (hub.data.vector.size() > 125 && hub.data.getSortListener() != null) {
            int x = findUsingQuickSort(hub, obj);
            if (x == 1) return true;
            if (x == 2) return false;
            if (x == -1) return false;
            if (x == -3) return false;
        }
        return hub.data.vector.contains(obj);
    }
    
    // 20170608
    /**
     * performs a quicksort search if the hub is loaded.
     * @return
     *    -1 obj=null, 
     *    -2 if not sorted (and did not check)
     *    -3 object is not same class as hub
     *    1 found
     *    2 not found
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

    public static boolean isHubBeingUsedAsRecursive(Hub thisHub) {
        if (thisHub == null) return false;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li == null) {
            return (thisHub == thisHub.getRootHub());
        }
        return li.getReverseLinkInfo().getRecursive();
    }
    
    public static void setTrackChanges(Hub thisHub, boolean b) {
        thisHub.data.setTrackChanges(b);        
    }
    public static boolean getTrackChanges(Hub thisHub) {
        return thisHub != null && thisHub.data.getTrackChanges();        
    }
    
    public static boolean isLoadingAllData(Hub thisHub) {
        if (thisHub == null) return false;
        boolean b;
        synchronized (thisHub.data) {
            b = (thisHub != null && thisHub.data.isLoadingAllData());
        }
        return b;
    }
    public static boolean setLoadingAllData(Hub thisHub, boolean b) {
        boolean bx = false;
        if (thisHub != null) {
            synchronized (thisHub.data) {
                bx = thisHub.data.setLoadingAllData(b);
            }
        }
        return bx;
    }
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
