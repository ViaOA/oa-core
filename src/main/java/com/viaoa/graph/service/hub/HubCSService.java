package com.viaoa.graph.service.hub;

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.serialize.OAObjectSerializer;

public abstract class HubCSService {
	private final Logger LOG = Logger.getLogger(HubCSService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubCSService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

    /**
     * Removes all objects from the same hub on connected systems by sending
     * a remote "remove all" command, if synchronization is enabled and the
     * hub has a master object. No-op when in single-user mode or when
     * client/server message suppression flags are active.
     *
     * @param thisHub the hub whose remote counterparts should remove all items
     */
    public void removeAllFromHub(Hub<?> thisHub) {
        if (callSyncIsSingleUser()) return;
        
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;
        if (!callThreadLocalGetSendSyncMessages()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
        
        callSyncRemoteSyncRemoveAllFromHub(
    		faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
    		faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
    		callHubDetailGetPropertyFromMasterToDetail(thisHub) 
        );
    }
    
    /**
     * Removes the specified object from the same hub on connected systems.
     * Skips calculated or local-only objects, and does not execute when
     * synchronization is suppressed or when the master object is absent.
     *
     * @param thisHub the hub originating the removal
     * @param obj     the object being removed
     * @param pos     the position from which the object was removed
     */
	public <T extends OAObject> void removeFromHub(Hub<T> thisHub, T obj, int pos) {
		if (thisHub == null) return;
		if (obj == null) return;
        if (callSyncIsSingleUser()) return;
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;

        if (!callThreadLocalGetSendSyncMessages()) return;
	    
	    OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
    	
        if (callObjectInfoGetObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return;
    	
        // must have a master object to be able to know which hub to add object to
        // send REMOVE message
        callSyncRemoteSyncRemoveFromHub(
			faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
			faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
			callHubDetailGetPropertyFromMasterToDetail(thisHub), 
			obj.getClass(), obj.getObjectKey()
		);
	}

	/**
	 * Adds an object to the same hub on connected systems. Sends the object
	 * itself if necessary so remote clients can instantiate it. No-op for
	 * local-only or calculated objects, or when synchronization is suppressed.
	 *
	 * @param thisHub the hub originating the add operation
	 * @param thisObj the object being added
	 */
	public <T extends OAObject> void addToHub(final Hub<T> thisHub, final T thisObj) {
		if (thisHub == null || thisObj == null) return;
		if (callSyncIsSingleUser()) return;
        if (!callThreadLocalGetSendSyncMessages()) return;
        
	    OAObjectInfo oi = callObjectInfoGetObjectInfo(thisObj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }

        // must have a master object to be able to know which hub to add object to
        // send ADD message
        
        final OAObject master = (OAObject) faHub.getHubDataMaster(thisHub).getMasterObject();
        if (master == null) return;
	    if (callObjectInfoGetObjectInfo(master).getLocalOnly()) return;

	    /* 20160826 removed, since this is only needed when loading oaobj.hub, which already suppresses messages when loading
	    if (OASync.isServer() && thisHub.isFetching()) {
	        return; // 20140309
	    }
	    */
	    
        final boolean bIsLoading = callThreadLocalIsLoading(); 
        if (bIsLoading) {
            if (!callSyncClientIsObjectOnServer(master)) {
                if (callSyncIsServer()) {
                    return; 
                }
            }
        }
        
        // 20110323 note: must send object, other clients might not have it.
        if (!callSyncIsSingleUser()) {
            if (callSyncIsServer()) {
                // if server, then send extra references if obj is new, so that client will not have to ask for it
                if (thisObj.isNew() && !callHubIsInHubWithMaster(thisObj, thisHub)) {
                    OAObjectSerializer oos = new OAObjectSerializer(thisObj, false, new OAObjectSerializerCallback() {
                        @Override
                        public void beforeSerialize(OAObject obj) {
                        }
                        @Override
                        public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object objRef, boolean bDefault) {
                            if (!bDefault) return false;
                            boolean b = _shouldSerializeReference(oaObj, propertyName, objRef, bDefault);
                            return b;
                        }
                        
                        private boolean _shouldSerializeReference(OAObject oaObj, String propertyName, Object objRef, boolean bDefault) {
                            if (oaObj != thisObj) return false;
                            if (objRef instanceof Hub) return true;
                            if (objRef instanceof OAObject) {
                                if (thisHub.getMasterObject() == objRef) return false;
                                if (((OAObject) objRef).isNew()) {
                                    if (callHubIsInHubWithMaster((OAObject)objRef)) return false;                                    
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
					callSyncSyncAddNewToCache(oos);
                }
            }
            
            callSyncSyncAddToHub(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                callHubDetailGetPropertyFromMasterToDetail(thisHub), thisObj);
        }
	}	

	/**
	 * Inserts an object at the specified position in the same hub on
	 * connected systems. Returns {@code false} when synchronization is
	 * suppressed, when local-only or calculated rules block propagation,
	 * or when no master object exists.
	 *
	 * @param thisHub the hub originating the insert
	 * @param obj     the object being inserted
	 * @param pos     the target index
	 * @return {@code true} if a remote insert command was sent; otherwise {@code false}
	 */
	public <T extends OAObject> boolean insertInHub(Hub<T> thisHub, T obj, int pos) {
		if (thisHub == null || obj == null) return false;
        if (callSyncIsSingleUser()) return false;
        if (!callThreadLocalGetSendSyncMessages()) return  false;
        
        OAObjectInfo oi = callObjectInfoGetObjectInfo(obj);
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (callObjectInfoGetObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        // must have a master object to be able to know which hub to add object to
        // send ADD message

        // 20110323 note: must send object, other clients might not have it.        
    	callSyncSyncInsertInHub(
            faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
            callHubDetailGetPropertyFromMasterToDetail(thisHub), 
            obj, pos);
        return true;
	}	
	
	/**
	 * Moves an object from one index to another in the same hub on
	 * connected systems. Skips propagation when synchronization is
	 * suppressed or when operating on local-only or calculated links.
	 *
	 * @param thisHub the hub originating the move request
	 * @param posFrom the starting index
	 * @param posTo   the destination index
	 */
	public void moveObjectInHub(Hub<?> thisHub, int posFrom, int posTo) {
		if (thisHub == null) return;
        if (callSyncIsSingleUser()) return;
        if (!callThreadLocalGetSendSyncMessages()) return;
        
	    OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return; 
    	
        // 20130319 dont send out calc changes
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
        
        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
	    if (callObjectInfoGetObjectInfo(objMaster).getLocalOnly()) return;
	    
	    
        // must have a master object to be able to know which hub to use
        // send MOVE message
	    
    	callSyncSyncMoveObjectInHub(objMaster.getClass(), 
            objMaster.getObjectKey(), 
            callHubDetailGetPropertyFromMasterToDetail(thisHub), posFrom, posTo);
	}

	/**
	 * Determines whether the specified hub is operating on the server.
	 *
	 * @param h the hub to check
	 * @return {@code true} if this is the server; otherwise {@code false}
	 */
	public boolean isServer(Hub<?> thisHub) {
        return callSyncIsServer();
	}		

	/**
	 * Returns whether the current thread is executing as a remote
	 * synchronization thread.
	 *
	 * @return {@code true} if the thread is a remote thread; otherwise {@code false}
	 */
	public boolean isRemoteThread() {
		return callRemoteThreadIsRemoteThread();
	}		
	
	/**
	 * Sorts objects in the hub on connected systems by sending a remote
	 * sort command. Skips propagation if synchronization is suppressed,
	 * the master object is missing or local-only, or the link is calculated.
	 *
	 * @param thisHub       the hub being sorted
	 * @param propertyPaths the property paths to sort by
	 * @param bAscending    whether sorting is ascending
	 * @param comp          optional comparator used for sorting
	 */
	public void sort(Hub<?> thisHub, String propertyPaths, boolean bAscending, Comparator<?> comp) {
        if (thisHub == null) return;
		if (callSyncIsSingleUser()) return;
        if (!callThreadLocalGetSendSyncMessages()) return;

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
        if (callObjectInfoGetObjectInfo(objMaster).getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }

    	callSyncSyncSort(objMaster.getClass(), objMaster.getObjectKey(), 
    		callHubDetailGetPropertyFromMasterToDetail(thisHub), 
            propertyPaths, bAscending, comp);
	}
	
	/**
	 * Deletes all objects in the hub, either locally or by sending a
	 * remote delete-all request, depending on whether this system is the
	 * server. Returns {@code true} if the deletion should occur locally,
	 * or {@code false} if it was delegated to a remote server.
	 *
	 * @param thisHub the hub whose contents should be deleted
	 * @return {@code true} if deletion is local; otherwise {@code false} means that it's being done on server.
	 */
    public boolean deleteAll(Hub<?> thisHub) {
		if (thisHub == null) return false;
        if (callSyncIsServer()) return true;  // invoke on the server
        if (!callThreadLocalGetSendSyncMessages()) return true;
        LOG.fine("hub="+thisHub);

        OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return true; 
        
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        OAObject master = thisHub.getMasterObject();
        if (master == null) return true;

        String prop = callHubDetailGetPropertyFromMasterToDetail(thisHub);
        if (prop == null) return true;

        callSyncClientDeleteAll(master.getClass(), master.getObjectKey(), prop);
        return false;
    }
    
    /**
     * Clears hub change tracking on connected clients by sending a
     * "clear changes" event, when synchronization is enabled. Returns
     * {@code false} when propagation cannot occur due to suppression,
     * missing master object, or local-only/calculated relationships.
     *
     * @param thisHub the hub whose change state should be cleared remotely
     * @return {@code true} if a clear request was sent; otherwise {@code false}
     */
    public boolean clearHubChanges(Hub<?> thisHub) {
        if (thisHub == null) return false;

        if (callSyncIsSingleUser()) return false;
        if (!callThreadLocalGetSendSyncMessages()) return  false;
        
        OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (callObjectInfoGetObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        callSyncSyncClearHubChanges(
            faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
            callHubDetailGetPropertyFromMasterToDetail(thisHub) 
        );
        return true;
    }   

    /**
     * Sends a remote refresh request for the specified hub’s master object,
     * if synchronization is available and the link information can be obtained.
     *
     * @param thisHub the hub requesting refresh
     */
    public void sendRefresh(Hub<?> thisHub) {
        if (thisHub == null) return;
        if (callSyncIsSingleUser()) return;
        if (!callThreadLocalGetSendSyncMessages()) return;

        OAObject obj = thisHub.getMasterObject();
        if (obj == null) return;
        OALinkInfo li = callHubDetailGetLinkInfoFromMasterObjectToDetail(thisHub);
        if (li == null) return;
        callSyncSyncRefresh(obj.getClass(), obj.getObjectKey(), li.getName());
    }

	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj);
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class<? extends OAObject> c);
	public abstract boolean callObjectHubIsInHub(OAObject oaObj);
	public abstract boolean callHubIsInHubWithMaster(OAObject oaObj);
	public abstract <T extends OAObject> boolean callHubIsInHubWithMaster(T oaObj, Hub<T> hubIgnore);
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> thisDetailHub);
	public abstract boolean callSyncIsServer();
	public abstract boolean callSyncIsClient();
	public abstract boolean callSyncIsSingleUser();
	public abstract boolean callSyncRemoteSyncRemoveAllFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName);
	public abstract boolean callSyncRemoteSyncRemoveFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, Class<? extends OAObject> objectClassX, OAObjectKey objectKeyX);	
	public abstract boolean callSyncClientIsObjectOnServer(OAObject obj);
	public abstract boolean callSyncSyncInsertInHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos);
	public abstract boolean callSyncSyncMoveObjectInHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName,  int posFrom, int posTo);
	public abstract boolean callSyncSyncSort(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator<?> comp);
	public abstract boolean callSyncClientDeleteAll(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName);
	public abstract boolean callThreadLocalGetSendSyncMessages();
	public abstract void callSyncSyncClearHubChanges(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	public abstract void callSyncSyncRefresh(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	public abstract boolean callSyncSyncAddToHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj);
	public abstract boolean callThreadLocalIsLoading();		
	public abstract boolean callRemoteThreadIsRemoteThread();
	public abstract void callSyncSyncAddNewToCache(OAObjectSerializer oos);	
    
}
