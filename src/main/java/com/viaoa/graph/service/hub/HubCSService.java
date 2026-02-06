package com.viaoa.graph.service.hub;

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OAObjectSerializerCallback;

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
    public void removeAllFromHub(Hub thisHub) {
        if (callSyncIsSingleUser()) return;
        
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;
        if (callThreadLocalIsSuppressCSMessages()) return;
        if (!callRemoteThreadShouldSendMessages()) {
            return;
        }

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
	public void removeFromHub(Hub thisHub, OAObject obj, int pos) {
//qqqqq?? pos is not used		
        if (callSyncIsSingleUser()) return;
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;

        if (callThreadLocalIsSuppressCSMessages()) return;
        if (!callRemoteThreadShouldSendMessages()) {
            return;
        }
	    
	    OAObjectInfo oi = callObjectInfoGetOAObjectInfo(obj);
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
    	
        if (callObjectInfoGetOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return;
    	
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
	public void addToHub(final Hub thisHub, final OAObject thisObj) {
		if (callSyncIsSingleUser()) return;
        if (!callRemoteThreadShouldSendMessages()) return;
        if (callThreadLocalIsSuppressCSMessages()) return;
        
	    OAObjectInfo oi = callObjectInfoGetOAObjectInfo(thisObj);
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
	    if (callObjectInfoGetOAObjectInfo(master).getLocalOnly()) return;

	    /* 20160826 removed, since this is only needed when loading oaobj.hub, which already suppresses messages when loading
	    if (OASync.isServer() && thisHub.isFetching()) {
	        return; // 20140309
	    }
	    */
	    
	    

        final boolean bIsLoading = callThreadLocalIsLoading(); 
        if (bIsLoading) {
            if (!callSyncSyncClientIsObjectOnServer(master)) {
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
                        protected void beforeSerialize(OAObject obj) {
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
					callSyncRemoteSyncAddNewToHub(
                        faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                        faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                        callHubDetailGetPropertyFromMasterToDetail(thisHub), oos);
                    return;
                }
            }
            
            callSyncRemoteSyncAddToHub(
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
	public boolean insertInHub(Hub thisHub, OAObject obj, int pos) {
        if (callSyncIsSingleUser()) return false;
        if (!callRemoteThreadShouldSendMessages()) return  false;
        if (callThreadLocalIsSuppressCSMessages()) return false;
        
        OAObjectInfo oi = callObjectInfoGetOAObjectInfo(obj);
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
        if (callObjectInfoGetOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        // must have a master object to be able to know which hub to add object to
        // send ADD message

        // 20110323 note: must send object, other clients might not have it.        
    	callSyncRemoteSyncInsertInHub(
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
	public void moveObjectInHub(Hub thisHub, int posFrom, int posTo) {
        if (callSyncIsSingleUser()) return;
        if (!callRemoteThreadShouldSendMessages()) return;
        if (callThreadLocalIsSuppressCSMessages()) return;
        
	    OAObjectInfo oi = callObjectInfoGetOAObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return; 
    	
        // 20130319 dont send out calc changes
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
	    if (callObjectInfoGetOAObjectInfo(objMaster).getLocalOnly()) return;
	    
	    
        // must have a master object to be able to know which hub to use
        // send MOVE message
	    
    	callSyncRemoteSyncMoveObjectInHub(objMaster.getClass(), 
            objMaster.getObjectKey(), 
            callHubDetailGetPropertyFromMasterToDetail(thisHub), posFrom, posTo);
	}

	/**
	 * Determines whether the specified hub is operating on the server.
	 *
	 * @param h the hub to check
	 * @return {@code true} if this is the server; otherwise {@code false}
	 */
	public boolean isServer(Hub thisHub) {
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
	public void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
        if (callSyncIsSingleUser()) return;
        if (!callRemoteThreadShouldSendMessages()) return;
        if (callThreadLocalIsSuppressCSMessages()) return;

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
        if (callObjectInfoGetOAObjectInfo(objMaster).getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

    	callSyncRemoteSyncSort(objMaster.getClass(), objMaster.getObjectKey(), 
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
	 * @return {@code true} if deletion is local; otherwise {@code false}
	 */
    public boolean deleteAll(Hub thisHub) {
        if (callSyncIsServer()) return true;  // invoke on the server
        LOG.fine("hub="+thisHub);

        if (!callRemoteThreadShouldSendMessages()) return true;
        if (callThreadLocalIsSuppressCSMessages()) return true;
        
        OAObjectInfo oi = callObjectInfoGetOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return true; 
        
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return true;
        }

        OAObject master = thisHub.getMasterObject();
        if (master == null) return true;

        String prop = callHubDetailGetPropertyFromMasterToDetail(thisHub);
        if (prop == null) return true;

        callSyncRemoteClientDeleteAll(master.getClass(), master.getObjectKey(), prop);
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
    public boolean clearHubChanges(Hub thisHub) {
        if (thisHub == null) return false;

        if (callSyncIsSingleUser()) return false;
        if (!callSyncShouldSendMessages()) return  false;
        if (callSyncGetSuppressCSMessages()) return false;
        
        OAObjectInfo oi = callObjectInfoGetOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return false;
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (callObjectInfoGetOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        callSyncRemoteSyncClearHubChanges(
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
    public void sendRefresh(Hub thisHub) {
        if (thisHub == null) return;
        if (callSyncIsSingleUser()) return;

        OAObject obj = thisHub.getMasterObject();
        if (obj == null) return;
        OALinkInfo li = callHubDetailGetLinkInfoFromMasterObjectToDetail(thisHub);
        if (li == null) return;
        callSyncRemoteSyncRefresh(obj.getClass(), obj.getObjectKey(), li.getName());
    }

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
    
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(OAObject obj);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo")
	public abstract OAObjectInfo callObjectInfoGetOAObjectInfo(Class c);

	@OAParentProvided (example = "srvcObject.getOAObjectHubService().isInHub")
	public abstract boolean callObjectHubIsInHub(OAObject oaObj);
	
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().isInHubWithMaster")
	public abstract boolean callHubIsInHubWithMaster(OAObject oaObj);

	@OAParentProvided (example = "srvcObject.getOAObjectHubService().isInHubWithMaster")
	public abstract boolean callHubIsInHubWithMaster(OAObject oaObj, Hub hubIgnore);


	
	@OAParentProvided (example = "srvcHub.getHubDetailService().getPropertyFromMasterToDetail")
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub thisHub);

	@OAParentProvided (example = "srvcHub.getHubDetailService().getLinkInfoFromMasterObjectToDetail")
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub thisDetailHub);


	
	@OAParentProvided (example = "srvcSync.isServer")
	public abstract boolean callSyncIsServer();

	@OAParentProvided (example = "srvcSync.isClient")
	public abstract boolean callSyncIsClient();
	
	@OAParentProvided (example = "srvcSync.isSingleUser")
	public abstract boolean callSyncIsSingleUser();
	

	@OAParentProvided (example = "srvcSync.getRemoteSync().removeAllFromHub")
	public abstract boolean callSyncRemoteSyncRemoveAllFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName);
	/*
    RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
    if (rs != null) rs.removeAllFromHub(..)
    */        

	@OAParentProvided (example = "srvcSync.getRemoteSync().removeFromHub")
	public abstract boolean callSyncRemoteSyncRemoveFromHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName, Class objectClassX, OAObjectKey objectKeyX);	
	/*
    RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
    if (rs != null) rs.removeFromHub(..)
    */        

	@OAParentProvided (example = "srvcSync.getSyncClient().isObjectOnServer")
	public abstract boolean callSyncSyncClientIsObjectOnServer(OAObject obj);
	/*
        final OASyncClient sc = og.getSyncService().getSyncClient();
        if (sc != null) {
            if (!sc.isObjectOnServer(master)) return;
        }
	*/
	

	@OAParentProvided (example = "srvcSync.getRemoteSync().insertInHub")
	public abstract boolean callSyncRemoteSyncInsertInHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos);
	/*
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.insertInHub(
                    faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                    faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                    callHubDetailGetPropertyFromMasterToDetail(thisHub), 
                    obj, pos);
        }
	
	*/
	
	@OAParentProvided (example = "srvcSync.getRemoteSync().moveObjectInHub")
	public abstract boolean callSyncRemoteSyncMoveObjectInHub(Class objectClass, OAObjectKey objectKey, String hubPropertyName,  int posFrom, int posTo);
	/*
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.moveObjectInHub(objMaster.getClass(), 
                    objMaster.getObjectKey(), 
                    callHubDetailGetPropertyFromMasterToDetail(thisHub), posFrom, posTo);
        }
	*/

	@OAParentProvided (example = "srvcSync.getRemoteSync().sort")
	public abstract boolean callSyncRemoteSyncSort(Class objectClass, OAObjectKey objectKey, String hubPropertyName, String propertyPaths, boolean bAscending, Comparator comp);
	/*
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.sort(objMaster.getClass(), objMaster.getObjectKey(), 
            		callHubDetailGetPropertyFromMasterToDetail(thisHub), 
                    propertyPaths, bAscending, comp);
        }
	
	*/

	@OAParentProvided (example = "srvcSync.getRemoteClient().deleteAll")
	public abstract boolean callSyncRemoteClientDeleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName);
	/*
        RemoteClientInterface rs = og.getSyncService().getRemoteClient();
        if (rs == null) return true;
        
        rs.deleteAll(master.getClass(), master.getObjectKey(), prop);
	*/

	@OAParentProvided (example = "srvcSync.shouldSendMessages")
	public abstract boolean callSyncShouldSendMessages();
	//og.getSyncService().shouldSendMessages()
	
	@OAParentProvided (example = "srvcSync.getSuppressCSMessages")
	public abstract boolean callSyncGetSuppressCSMessages();
    // if (og.getSyncService().getSuppressCSMessages()) return false;


	@OAParentProvided (example = "srvcSync.getRemoteSync().clearHubChanges")
	public abstract void callSyncRemoteSyncClearHubChanges(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	/*
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.clearHubChanges(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                callHubDetailGetPropertyFromMasterToDetail(thisHub) 
            );
        }
	
	*/

	@OAParentProvided (example = "srvcSync.getRemoteSync().refresh")
	public abstract void callSyncRemoteSyncRefresh(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	/*
        RemoteSyncInterface rsi = og.getSyncService().getRemoteSync();
        if (rsi == null) return;
        OAObject obj = thisHub.getMasterObject();
        if (obj == null) return;
        OALinkInfo li = callHubDetailGetLinkInfoFromMasterObjectToDetail(thisHub);
        if (li == null) return;
        rsi.refresh(obj.getClass(), obj.getObjectKey(), li.getName());
	
	*/
	
	@OAParentProvided (example = "srvcSync.getRemoteSync().addNewToHub")
	public abstract boolean callSyncRemoteSyncAddNewToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, OAObjectSerializer obj);
	/*	
	RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
	                    rs.addNewToHub(
	                            faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
	                            faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
	                            callHubDetailGetPropertyFromMasterToDetail(thisHub), oos);
	*/

	@OAParentProvided (example = "srvcSync.getRemoteSync().addToHub")
	public abstract boolean callSyncRemoteSyncAddToHub(Class masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj);
	

	
	
	
	@OAParentProvided (example = "srvcThreadLocal.isSuppressCSMessages")
	public abstract boolean callThreadLocalIsSuppressCSMessages();		

	@OAParentProvided (example = "srvcThreadLocal.isLoading")
	public abstract boolean callThreadLocalIsLoading();		
	
	@OAParentProvided (example = "srvcRemoteThread.shouldSendMessages")
	public abstract boolean callRemoteThreadShouldSendMessages();

	@OAParentProvided (example = "srvcRemoteThread.isRemoteThread")
	public abstract boolean callRemoteThreadIsRemoteThread();
	
    
}
