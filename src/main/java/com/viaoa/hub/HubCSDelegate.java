/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.util.Comparator;
import java.util.logging.Logger;

import com.viaoa.sync.*;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.object.*;
import com.viaoa.remote.OARemoteThreadDelegate;

/**
 * Handles all Client/Server synchronization logic for {@link Hub} operations.
 * <p>
 * The HubCSDelegate is invoked by Hub internals whenever objects are added,
 * removed, inserted, moved, sorted, or otherwise modified so that the same
 * change can be propagated to all connected systems.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Translate Hub events into remote synchronization commands.</li>
 *   <li>Route commands through {@link RemoteSyncInterface} (server → clients)
 *       or {@link RemoteClientInterface} (client → server).</li>
 *   <li>Respect suppression flags and skip calculated or local-only objects.</li>
 *   <li>Ensure that distributed Hubs remain state-identical without causing
 *       feedback loops or redundant updates.</li>
 * </ul>
 *
 * <h3>Supported Remote Actions</h3>
 * <ul>
 *   <li>{@link #addToHub(Hub, OAObject)} — replicate an add event.</li>
 *   <li>{@link #insertInHub(Hub, OAObject, int)} — replicate insert at index.</li>
 *   <li>{@link #removeFromHub(Hub, OAObject, int)} and {@link #removeAllFromHub(Hub)}.</li>
 *   <li>{@link #moveObjectInHub(Hub, int, int)} — reorder synchronization.</li>
 *   <li>{@link #sort(Hub, String, boolean, Comparator)} — remote sort propagation.</li>
 *   <li>{@link #deleteAll(Hub)} — instructs remote delete on server.</li>
 *   <li>{@link #clearHubChanges(Hub)} — resets change tracking on clients.</li>
 *   <li>{@link #sendRefresh(Hub)} — triggers remote refresh request.</li>
 * </ul>
 *
 * <h3>Behavioral Safeguards</h3>
 * <ul>
 *   <li>All methods no-op in single-user mode or when
 *       {@code OAThreadLocalDelegate.isSuppressCSMessages()} is true.</li>
 *   <li>Skips propagation for calculated or local-only relationships.</li>
 *   <li>Uses {@link OARemoteThreadDelegate#shouldSendMessages()} to avoid
 *       feedback loops during replication.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Hub<Order> hubOrders = new Hub<>(Order.class);
 * hubOrders.add(order);  // transparently propagated to all clients
 * }</pre>
 *
 * <p>This delegate underpins OA’s distributed object graph consistency.</p>
 */
public class HubCSDelegate {
    private static Logger LOG = Logger.getLogger(HubCSDelegate.class.getName());

    public static void removeAllFromHub(Hub thisHub) {
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (thisHub.datam.getMasterObject() == null) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) {
            return;
        }

        // 20140708 
        OALinkInfo li = thisHub.datam.liDetailToMaster;
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
                thisHub.datam.getMasterObject().getClass(), 
                thisHub.datam.getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub) 
            );
        }
    }
    
    /**
	 * Have object removed from same Hub on other workstations.
	 */
	public static void removeFromHub(Hub thisHub, OAObject obj, int pos) {
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (thisHub.datam.getMasterObject() == null) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) {
            return;
        }
	    
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return;
                }
            }
        }
    	
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)thisHub.datam.getMasterObject()).getLocalOnly()) return;
    	
        // must have a master object to be able to know which hub to add object to
        // send REMOVE message
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.removeFromHub(
                    thisHub.datam.getMasterObject().getClass(), 
                    thisHub.datam.getMasterObject().getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), 
                    obj.getClass(), obj.getObjectKey());
        }
	}

	/**
	 * Have object added to same Hub on other workstations.
	 */
	public static void addToHub(final Hub thisHub, final OAObject thisObj) {
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisObj);
	    if (oi.getLocalOnly()) return;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
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
        
        final OAObject master = (OAObject) thisHub.datam.getMasterObject();
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
                            thisHub.datam.getMasterObject().getClass(), 
                            thisHub.datam.getMasterObject().getObjectKey(), 
                            HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), oos);
                            
                    return;
                }
            }
            
            rs.addToHub(
                thisHub.datam.getMasterObject().getClass(), 
                thisHub.datam.getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), thisObj);
        }
	}	

	/**
	 * Have object inserted in same Hub on other workstations.
	 */
	public static boolean insertInHub(Hub thisHub, OAObject obj, int pos) {
        if (OASyncDelegate.isSingleUser(thisHub)) return false;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return  false;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return false;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) {
                if (!OASyncDelegate.isServer(thisHub) || !liRev.getServerSideCalc()) {
                    return false;
                }
            }
        }

        if (thisHub.datam.getMasterObject() == null) return false;
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)thisHub.datam.getMasterObject()).getLocalOnly()) return false;

        // must have a master object to be able to know which hub to add object to
        // send ADD message

        // 20110323 note: must send object, other clients might not have it.        
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.insertInHub(
                    thisHub.datam.getMasterObject().getClass(), 
                    thisHub.datam.getMasterObject().getObjectKey(), 
                    HubDetailDelegate.getPropertyFromMasterToDetail(thisHub), 
                    obj, pos);
        }
        return true;
	}	
	
	/**
	 * Have object added to same Hub on other workstations.
	 */
	public static void moveObjectInHub(Hub thisHub, int posFrom, int posTo) {
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
	    OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
	    if (oi.getLocalOnly()) return; 
    	
        // 20130319 dont send out calc changes
        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return;
        }

        OAObject objMaster = thisHub.datam.getMasterObject();
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

	public static boolean isServer(Hub h) {
        return OASyncDelegate.isServer(h);
	}		
	public static boolean isRemoteThread() {
		return (OARemoteThreadDelegate.isRemoteThread());
	}		
	
	/**
	 * Sort objects in hub
	 */
	public static void sort(Hub thisHub, String propertyPaths, boolean bAscending, Comparator comp) {
        if (OASyncDelegate.isSingleUser(thisHub)) return;
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObject objMaster = thisHub.datam.getMasterObject();
        if (objMaster == null) return;
        if (OAObjectInfoDelegate.getOAObjectInfo(objMaster).getLocalOnly()) return;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
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
     * 20150206 returns true if this should be deleted on this computer, false if it is done on the server. 
    */
    protected static boolean deleteAll(Hub thisHub) {
        if (OASyncDelegate.isServer(thisHub)) return true;  // invoke on the server
        LOG.fine("hub="+thisHub);
        
        if (!OARemoteThreadDelegate.shouldSendMessages()) return true;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return true;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return true; 
        
        OALinkInfo li = thisHub.datam.liDetailToMaster;
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
    
    // 20150420
    /**
     * Hub hubData.vecAdd/Remove cleared on clients
     */
    public static boolean clearHubChanges(Hub thisHub) {
        if (thisHub == null) return false;
        if (OASync.isSingleUser(thisHub)) return false;
        if (!OASync.shouldSendMessages()) return  false;
        if (OASync.getSuppressCSMessages()) return false;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(thisHub.getObjectClass());
        if (oi.getLocalOnly()) return false;

        OALinkInfo li = thisHub.datam.liDetailToMaster;
        if (li != null) {
            OALinkInfo liRev = OAObjectInfoDelegate.getReverseLinkInfo(li);
            if (liRev != null && liRev.getCalculated()) return false;
        }

        if (thisHub.datam.getMasterObject() == null) return false;
        if (OAObjectInfoDelegate.getOAObjectInfo((OAObject)thisHub.datam.getMasterObject()).getLocalOnly()) return false;

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(thisHub);
        if (rs != null) {
            rs.clearHubChanges(
                thisHub.datam.getMasterObject().getClass(), 
                thisHub.datam.getMasterObject().getObjectKey(), 
                HubDetailDelegate.getPropertyFromMasterToDetail(thisHub) 
            );
        }
        return true;
    }   

    public static void sendRefresh(Hub thisHub) {
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

