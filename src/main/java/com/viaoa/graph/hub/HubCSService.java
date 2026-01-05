package com.viaoa.graph.hub;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCSDelegate;
import com.viaoa.hub.HubData;
import com.viaoa.hub.HubDataActive;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.hub.HubDataUnique;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubLinkDelegate;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OAGroupBy;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCallback;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAObjectSerializer;
import com.viaoa.object.OAObjectSerializerCallback;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.remote.OARemoteThread;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.*;

public class HubCSService {
	private final Logger LOG = Logger.getLogger(HubCSService.class.getName());

	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	public HubCSService(HubService srvcHub, 
			Hub.FriendAccess faHub
			) {
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
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        
        
        
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) {
            return;
        }

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
        

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.removeAllFromHub(
            		faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            		faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub) 
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
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) {
            return;
        }
	    
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
    	
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return;
    	
        // must have a master object to be able to know which hub to add object to
        // send REMOVE message
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.removeFromHub(
            		faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
            		faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), 
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
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisObj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }

        // must have a master object to be able to know which hub to add object to
        // send ADD message
        
        final OAObject master = (OAObject) faHub.getHubDataMaster(thisHub).getMasterObject();
        if (master == null) return;
	    if (OAObjectInfoDelegate.getOAObjectInfo(master).getLocalOnly()) return;

	    /* 20160826 removed, since this is only needed when loading oaobj.hub, which already suppresses messages when loading
	    if (OASync.isServer() && thisHub.isFetching()) {
	        return; // 20140309
	    }
	    */
	    
        final OASyncClient sc = OASyncDelegate.getSyncClient();
        if (sc != null) {
            if (!sc.isObjectOnServer(master)) return;
        }

        // 20160630
        final boolean bIsLoading = OAThreadLocalDelegate.isLoading(); 
        if (bIsLoading) {
            if (!OAObjectHubDelegate.isInHub(master)) {
                if (OASyncDelegate.isServer(master)) {
                    return; 
                }
            }
        }
        
        // 20110323 note: must send object, other clients might not have it.        
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            if (OASync.isServer()) {
                // if server, then send extra references if obj is new, so that client will not have to ask for it
                if (thisObj.isNew() && !OAObjectHubDelegate.isInHubWithMaster(thisObj, thisHub)) {
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
                                    if (OAObjectHubDelegate.isInHubWithMaster((OAObject)objRef)) return false;                                    
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                    
                    rs.addNewToHub(
                            faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                            faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                            HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), oos);
                            
                    return;
                }
            }
            
            rs.addToHub(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), thisObj);
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
        if (OASyncDelegate.isSingleUser(thisHub)) return false;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return  false;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return false;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        // must have a master object to be able to know which hub to add object to
        // send ADD message

        // 20110323 note: must send object, other clients might not have it.        
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.insertInHub(
                    faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                    faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), 
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
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return; 
    	
        // 20130319 dont send out calc changes
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
	    if (OAObjectInfoDelegate.getOAObjectInfo(objMaster).getLocalOnly()) return;
	    
	    
        // must have a master object to be able to know which hub to use
        // send MOVE message
	    
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.moveObjectInHub(objMaster.getClass(), 
                    objMaster.getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), posFrom, posTo);
        }
	}

	/**
	 * Determines whether the specified hub is operating on the server.
	 *
	 * @param h the hub to check
	 * @return {@code true} if this is the server; otherwise {@code false}
	 */
	public boolean isServer(Hub h) {
        return OASyncDelegate.isServer(h);
	}		

	/**
	 * Returns whether the current thread is executing as a remote
	 * synchronization thread.
	 *
	 * @return {@code true} if the thread is a remote thread; otherwise {@code false}
	 */
	public boolean isRemoteThread() {
		return (OARemoteThreadDelegate.isRemoteThread());
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
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObject objMaster = faHub.getHubDataMaster(thisHub).getMasterObject();
        if (objMaster == null) return;
        if (OAObjectInfoDelegate.getOAObjectInfo(objMaster).getLocalOnly()) return;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.sort(objMaster.getClass(), objMaster.getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), 
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
        if (OASyncDelegate.isServer(thisHub)) return true;  // invoke on the server
        LOG.fine("hub="+thisHub);
        
        if (!OARemoteThreadDelegate.shouldSendMessages()) return true;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return true;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return true; 
        
        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return true;
        }

        OAObject master = thisHub.getMasterObject();
        if (master == null) return true;

        String prop = HubDetailDelegate.getPropertyFromMasterToDetail(thisHub);
        if (prop == null) return true;

        RemoteClientInterface rs = OASyncDelegate.getRemoteClient(thisHub);
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
        if (OASync.isSingleUser(thisHub)) return false;
        if (!OASync.shouldSendMessages()) return  false;
        if (OASync.getSuppressCSMessages()) return false;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = faHub.getHubDataMaster(thisHub).getDetailToMasterLinkInfo();
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return false;
        }

        if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) return false;
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)faHub.getHubDataMaster(thisHub).getMasterObject()).getLocalOnly()) return false;

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.clearHubChanges(
                faHub.getHubDataMaster(thisHub).getMasterObject().getClass(), 
                faHub.getHubDataMaster(thisHub).getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub) 
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
        RemoteSyncInterface rsi = OASyncDelegate.getRemoteSync();
        if (rsi == null) return;
        OAObject obj = thisHub.getMasterObject();
        if (obj == null) return;
        OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterObjectToDetail(thisHub);
        if (li == null) return;
        rsi.refresh(obj.getClass(), obj.getObjectKey(), li.getName());
    }

	
}
