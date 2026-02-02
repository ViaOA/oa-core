package com.viaoa.graph.service.hub;

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.service.HubService;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OAObjectSerializerCallback;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OARemoteThreadService;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;

public class HubCSService {
	private final Logger LOG = Logger.getLogger(HubCSService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	public HubCSService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isSingleUser()) return;
        
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return;
        if (!srvcOARemoteThread.shouldSendMessages()) {
            return;
        }

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!og.getSyncService().isServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
        

        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.removeAllFromHub(
            		faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            		faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
            		srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub) 
            );
        }
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isSingleUser()) return;
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;

		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        if (srvcOAThreadLocal.isSuppressCSMessages()) return;
        if (!srvcOARemoteThread.shouldSendMessages()) {
            return;
        }
	    
	    OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(obj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!og.getSyncService().isServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
    	
        if (srvcObject.getOAObjectInfoService().getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return;
    	
        // must have a master object to be able to know which hub to add object to
        // send REMOVE message
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.removeFromHub(
            		faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            		faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
            		srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), 
                    obj.getClass(), obj.getObjectKey());
        }
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
		if (og.getSyncService().isSingleUser()) return;
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        if (!srvcOARemoteThread.shouldSendMessages()) return;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(thisObj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!og.getSyncService().isServer() || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }

        // must have a master object to be able to know which hub to add object to
        // send ADD message
        
        final OAObject master = (OAObject) faHub.getHubDataMaster(thisHub).getMasterObject();
        if (master == null) return;
	    if (srvcObject.getOAObjectInfoService().getOAObjectInfo(master).getLocalOnly()) return;

	    /* 20160826 removed, since this is only needed when loading oaobj.hub, which already suppresses messages when loading
	    if (OASync.isServer() && thisHub.isFetching()) {
	        return; // 20140309
	    }
	    */
	    
        final OASyncClient sc = og.getSyncService().getSyncClient();
        if (sc != null) {
            if (!sc.isObjectOnServer(master)) return;
        }

        // 20160630
        final boolean bIsLoading = srvcOAThreadLocal.isLoading(); 
        if (bIsLoading) {
            if (!srvcObject.getOAObjectHubService().isInHub(master)) {
                if (og.getSyncService().isServer()) {
                    return; 
                }
            }
        }
        
        // 20110323 note: must send object, other clients might not have it.        
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            if (og.getSyncService().isServer()) {
                // if server, then send extra references if obj is new, so that client will not have to ask for it
                if (thisObj.isNew() && !srvcObject.getOAObjectHubService().isInHubWithMaster(thisObj, thisHub)) {
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
                                    if (srvcObject.getOAObjectHubService().isInHubWithMaster((OAObject)objRef)) return false;                                    
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                    
                    rs.addNewToHub(
                            faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                            faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                            srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), oos);
                            
                    return;
                }
            }
            
            rs.addToHub(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), thisObj);
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isSingleUser()) return false;
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        if (!srvcOARemoteThread.shouldSendMessages()) return  false;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return false;
        
        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!og.getSyncService().isServer() || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (srvcObject.getOAObjectInfoService().getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        // must have a master object to be able to know which hub to add object to
        // send ADD message

        // 20110323 note: must send object, other clients might not have it.        
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.insertInHub(
                    faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                    faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                    srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), 
                    obj, pos);
        }
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isSingleUser()) return;
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        if (!srvcOARemoteThread.shouldSendMessages()) return;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return; 
    	
        // 20130319 dont send out calc changes
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
	    if (srvcObject.getOAObjectInfoService().getOAObjectInfo(objMaster).getLocalOnly()) return;
	    
	    
        // must have a master object to be able to know which hub to use
        // send MOVE message
	    
        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.moveObjectInHub(objMaster.getClass(), 
                    objMaster.getObjectKey(), 
                    srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), posFrom, posTo);
        }
	}

	/**
	 * Determines whether the specified hub is operating on the server.
	 *
	 * @param h the hub to check
	 * @return {@code true} if this is the server; otherwise {@code false}
	 */
	public boolean isServer(Hub thisHub) {
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        return og.getSyncService().isServer();
	}		

	/**
	 * Returns whether the current thread is executing as a remote
	 * synchronization thread.
	 *
	 * @return {@code true} if the thread is a remote thread; otherwise {@code false}
	 */
	public boolean isRemoteThread() {
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		return (srvcOARemoteThread.isRemoteThread());
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isSingleUser()) return;
		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        if (!srvcOARemoteThread.shouldSendMessages()) return;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return;

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
        if (srvcObject.getOAObjectInfoService().getOAObjectInfo(objMaster).getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.sort(objMaster.getClass(), objMaster.getObjectKey(), 
            		srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub), 
                    propertyPaths, bAscending, comp);
        }
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        if (og.getSyncService().isServer()) return true;  // invoke on the server
        LOG.fine("hub="+thisHub);

		final OARemoteThreadService srvcOARemoteThread = ((OAThreadImpl) OARuntime.thread()).getRemoteThreadService();  
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadImpl) OARuntime.thread()).getThreadLocalService();  
        
        if (!srvcOARemoteThread.shouldSendMessages()) return true;
        if (srvcOAThreadLocal.isSuppressCSMessages()) return true;
        
        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return true; 
        
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return true;
        }

        OAObject master = thisHub.getMasterObject();
        if (master == null) return true;

        String prop = srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub);
        if (prop == null) return true;

        RemoteClientInterface rs = og.getSyncService().getRemoteClient();
        if (rs == null) return true;
        
        rs.deleteAll(master.getClass(), master.getObjectKey(), prop);
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);

        if (og.getSyncService().isSingleUser()) return false;
        if (!og.getSyncService().shouldSendMessages()) return  false;
        if (og.getSyncService().getSuppressCSMessages()) return false;
        
        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = srvcObject.getOAObjectInfoService().getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return false;
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (srvcObject.getOAObjectInfoService().getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        RemoteSyncInterface rs = og.getSyncService().getRemoteSync();
        if (rs != null) {
            rs.clearHubChanges(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                srvcHub.getHubDetailService().getPropertyFromMasterToDetail(thisHub) 
            );
        }
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
		final OAGraphImpl og = (OAGraphImpl) OARuntime.graph(thisHub);
        RemoteSyncInterface rsi = og.getSyncService().getRemoteSync();
        if (rsi == null) return;
        OAObject obj = thisHub.getMasterObject();
        if (obj == null) return;
        OALinkInfo li = srvcHub.getHubDetailService().getLinkInfoFromMasterObjectToDetail(thisHub);
        if (li == null) return;
        rsi.refresh(obj.getClass(), obj.getObjectKey(), li.getName());
    }

	
}
