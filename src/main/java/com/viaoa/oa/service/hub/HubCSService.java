package com.viaoa.oa.service.hub;

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.callback.OAObjectSerializerCallback;
import com.viaoa.hub.Hub;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.serialize.OAObjectSerializer;

/*qqqqqqqqqqqqqqq
CODEX


 #5
  1. file/class/method: src/main/java/com/viaoa/oa/service/hub/HubCSService.java, addToHub(...),
     insertInHub(...), removeFromHub(...), moveObjectInHub(...), sort(...), clearHubChanges(...)
  2. exact execution path: synced Hub operation calls the corresponding remote sync method. Several boolean-
     returning remote calls are ignored, and insertInHub(...) returns true after callSyncSyncInsertInHub(...)
     regardless of the remote result.
  3. why this is a real bug under the clarified partial-failure semantics: remote Hub mutation failure can be hidden
     while local Hub mutation continues or the caller believes the remote operation was sent. This can silently
     diverge Hub membership/order/change state across sync peers.
  4. semantic/invariant violated: Hub sync mutation return values must reflect whether the remote operation was
     accepted.
  5. minimal fix: honor every boolean remote sync result. Return false or throw when remote mutation is not
     accepted; do not report sent/success if the remote call returned false.
  6. suggested test: remote addToHub or insertInHub returns false; perform local Hub add/insert in client/server
     sync mode; assert operation fails visibly or does not locally commit as synced.


#6
  1. file/class/method: src/main/java/com/viaoa/oa/service/hub/HubCSService.java, deleteAll(...)
  2. exact execution path: client calls Hub.deleteAll(...); HubDeleteService.deleteAll(...) calls
     callHubCSDeleteAll(...); HubCSService.deleteAll(...) sees a calculated reverse link that is not server-side
     calc and returns false without sending remote delete; HubDeleteService.deleteAll(...) treats false as
     “delegated to server” and returns.
  3. why this is a real bug under the clarified partial-failure semantics: delete-all is not done locally or
     remotely, and caller receives no visible failure.
  4. semantic/invariant violated: false from callHubCSDeleteAll must mean the delete was actually delegated, not
     silently rejected.
  5. minimal fix: return true when no remote delegation occurred so local semantics can decide, or throw/not-allowed
     explicitly for unsupported calculated links.
  6. suggested test: client-side Hub with calculated non-server-side reverse link; call deleteAll; assert it either
     deletes locally or throws, but does not silently no-op.


#3
  File/class/method: src/main/java/com/viaoa/oa/service/hub/HubCSService.java:390, deleteAll(...)

  Exact changed-code path: callSyncClientDeleteAll(...) result is assigned to b, but ignored; method always returns
  true.

  Why this is a regression: failed/rejected remote deleteAll is reported as completed, so
  HubDeleteService.deleteAll(...) returns without local or remote deletion.

  Minimal fix: honor the remote result. For client authoritative routing, throw on false rather than falling back to
  local delete.

  Suggested regression test: client Hub deleteAll where remote returns false; assert caller gets visible failure and
  Hub contents remain unchanged.


*/

/**
 * Coordinates Hub client/server synchronization hooks.
 */

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

        if (callThreadLocalIsLoading()) {
            if (callSyncIsClient() && !callSyncClientIsObjectOnServer(master)) {
                return; 
            }
        }
        
        // 20110323 note: must send object, other clients might not have it.
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
	 * Returns whether the supplied Hub context is client.
	 *
	 * @param thisHub method input
	 * @return result value
	 */

	public boolean isClient(Hub<?> thisHub) {
        return callSyncIsClient();
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
	 * @param paths the property paths to sort by
	 * @param bAscending    whether sorting is ascending
	 * @param comp          optional comparator used for sorting
	 */
	public void sort(Hub<?> thisHub, String paths, boolean bAscending, Comparator<?> comp) {
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
    		paths, bAscending, comp);
	}
	
	/**
	 * Deletes all objects in the hub, either locally or by sending a
	 * remote delete-all request, depending on whether this system is the
	 * server. Returns {@code true} if the deletion should occur locally,
	 * or {@code false} if it was delegated to a remote server.
	 *
	 * @param thisHub the hub whose contents should be deleted
	 * @return {@code true} if deletion was completed on remote.
	 */
    public boolean deleteAll(Hub<?> thisHub) {
		if (thisHub == null) return false;
        if (!callSyncIsClient()) return false;  // invoke on the server
        if (!callThreadLocalGetSendSyncMessages()) return false;
        LOG.fine("hub="+thisHub);

        OAObjectInfo oi = callObjectInfoGetObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false; 
        
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!callSyncIsServer() || !liRev.getServerSideCalc()) {
                    return true;
                }
            }
        }

        OAObject master = thisHub.getMasterObject();
        if (master == null) return false;

        String prop = callHubDetailGetPropertyFromMasterToDetail(thisHub);
        if (prop == null) return false;

        boolean b = callSyncClientDeleteAll(master.getClass(), master.getObjectKey(), prop);
        return true;
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

	/**
	 * Dependency hook used by this service for ObjectInfoGetReverseLinkInfo behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */

	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for ObjectInfoGetObjectInfo behavior.
	 *
	 * @param obj method input
	 * @return result value
	 */
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(OAObject obj);
	/**
	 * Dependency hook used by this service for ObjectInfoGetObjectInfo behavior.
	 *
	 * @param c method input
	 * @return result value
	 */
	public abstract OAObjectInfo callObjectInfoGetObjectInfo(Class<? extends OAObject> c);
	/**
	 * Dependency hook used by this service for ObjectHubIsInHub behavior.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract boolean callObjectHubIsInHub(OAObject oaObj);
	/**
	 * Dependency hook used by this service for HubIsInHubWithMaster behavior.
	 *
	 * @param oaObj method input
	 * @return result value
	 */
	public abstract boolean callHubIsInHubWithMaster(OAObject oaObj);
	/**
	 * Dependency hook used by this service for HubIsInHubWithMaster behavior.
	 *
	 * @param oaObj method input
	 * @param hubIgnore method input
	 * @return result value
	 */
	public abstract <T extends OAObject> boolean callHubIsInHubWithMaster(T oaObj, Hub<T> hubIgnore);
	/**
	 * Dependency hook used by this service for HubDetailGetPropertyFromMasterToDetail behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<?> thisHub);
	/**
	 * Dependency hook used by this service for HubDetailGetLinkInfoFromMasterObjectToDetail behavior.
	 *
	 * @param thisDetailHub method input
	 * @return result value
	 */
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterObjectToDetail(Hub<?> thisDetailHub);
	/**
	 * Dependency hook used by this service for SyncIsServer behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callSyncIsServer();
	/**
	 * Dependency hook used by this service for SyncIsClient behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callSyncIsClient();
	/**
	 * Dependency hook used by this service for SyncIsSingleUser behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callSyncIsSingleUser();
	/**
	 * Dependency hook used by this service for SyncRemoteSyncRemoveAllFromHub behavior.
	 *
	 * @param objectClass method input
	 * @param objectKey method input
	 * @param hubPropertyName method input
	 * @return result value
	 */
	public abstract boolean callSyncRemoteSyncRemoveAllFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName);
	/**
	 * Dependency hook used by this service for SyncRemoteSyncRemoveFromHub behavior.
	 *
	 * @param objectClass method input
	 * @param objectKey method input
	 * @param hubPropertyName method input
	 * @param objectClassX method input
	 * @param objectKeyX method input
	 * @return result value
	 */
	public abstract boolean callSyncRemoteSyncRemoveFromHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, Class<? extends OAObject> objectClassX, OAObjectKey objectKeyX);
	/**
	 * Dependency hook used by this service for SyncClientIsObjectOnServer behavior.
	 *
	 * @param obj method input
	 * @return result value
	 */
	public abstract boolean callSyncClientIsObjectOnServer(OAObject obj);
	/**
	 * Dependency hook used by this service for SyncSyncInsertInHub behavior.
	 *
	 * @param masterObjectClass method input
	 * @param masterObjectKey method input
	 * @param hubPropertyName method input
	 * @param obj method input
	 * @param pos method input
	 * @return result value
	 */
	public abstract boolean callSyncSyncInsertInHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj, int pos);
	/**
	 * Dependency hook used by this service for SyncSyncMoveObjectInHub behavior.
	 *
	 * @param objectClass method input
	 * @param objectKey method input
	 * @param hubPropertyName method input
	 * @param posFrom method input
	 * @param posTo method input
	 * @return result value
	 */
	public abstract boolean callSyncSyncMoveObjectInHub(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName,  int posFrom, int posTo);
	/**
	 * Dependency hook used by this service for SyncSyncSort behavior.
	 *
	 * @param objectClass method input
	 * @param objectKey method input
	 * @param hubPropertyName method input
	 * @param paths method input
	 * @param bAscending method input
	 * @param comp method input
	 * @return result value
	 */
	public abstract boolean callSyncSyncSort(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName, String paths, boolean bAscending, Comparator<?> comp);
	/**
	 * Dependency hook used by this service for SyncClientDeleteAll behavior.
	 *
	 * @param objectClass method input
	 * @param objectKey method input
	 * @param hubPropertyName method input
	 * @return result value
	 */
	public abstract boolean callSyncClientDeleteAll(Class<? extends OAObject> objectClass, OAObjectKey objectKey, String hubPropertyName);
	/**
	 * Dependency hook used by this service for ThreadLocalGetSendSyncMessages behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callThreadLocalGetSendSyncMessages();
	/**
	 * Dependency hook used by this service for SyncSyncClearHubChanges behavior.
	 *
	 * @param masterObjectClass method input
	 * @param masterObjectKey method input
	 * @param hubPropertyName method input
	 */
	public abstract void callSyncSyncClearHubChanges(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	/**
	 * Dependency hook used by this service for SyncSyncRefresh behavior.
	 *
	 * @param masterObjectClass method input
	 * @param masterObjectKey method input
	 * @param hubPropertyName method input
	 */
	public abstract void callSyncSyncRefresh(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName);
	/**
	 * Dependency hook used by this service for SyncSyncAddToHub behavior.
	 *
	 * @param masterObjectClass method input
	 * @param masterObjectKey method input
	 * @param hubPropertyName method input
	 * @param obj method input
	 * @return result value
	 */
	public abstract boolean callSyncSyncAddToHub(Class<? extends OAObject> masterObjectClass, OAObjectKey masterObjectKey, String hubPropertyName, Object obj);
	/**
	 * Dependency hook used by this service for ThreadLocalIsLoading behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callThreadLocalIsLoading();
	/**
	 * Dependency hook used by this service for RemoteThreadIsRemoteThread behavior.
	 *
	 * @return result value
	 */
	public abstract boolean callRemoteThreadIsRemoteThread();
	/**
	 * Dependency hook used by this service for SyncSyncAddNewToCache behavior.
	 *
	 * @param oos method input
	 */
	public abstract void callSyncSyncAddNewToCache(OAObjectSerializer oos);

}
