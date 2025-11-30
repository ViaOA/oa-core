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
package com.viaoa.object;

import java.util.logging.*;

import com.viaoa.sync.*;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.*;
import com.viaoa.remote.*;

/**
 * Core client-server coordination delegate for {@link OAObject}.
 *
 * <p>Handles synchronization, remote method routing, and replication
 * between OA clients and servers.  Encapsulates all messaging between
 * {@link OASyncClient}, {@link RemoteServerInterface},
 * {@link RemoteSyncInterface}, and {@link OASyncDelegate}.</p>
 *
 * <p><b>Major Responsibilities</b>:
 * <ul>
 *   <li>Detect runtime mode — server, client, or standalone.</li>
 *   <li>Propagate create, save, and delete events across network nodes.</li>
 *   <li>Coordinate property-change messaging (before/after events).</li>
 *   <li>Retrieve reference hubs and blobs from the server on demand.</li>
 *   <li>Provide utilities for GUID assignment and remote thread checks.</li>
 * </ul>
 *
 * <p>All cross-thread and cross-process synchronization for distributed
 * OA applications passes through this delegate.</p>
 */
public class OAObjectCSDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectCSDelegate.class.getName());

    
	/**
	 * Determines whether the current thread is an {@code OARemoteThread},
	 * which indicates that the caller is executing within a remote-message
	 * processing context.
	 *
	 * <p>This performs a delegation to
	 * {@code OARemoteThreadDelegate.isRemoteThread()} which uses
	 * {@code Thread instanceof OARemoteThread} to identify remote
	 * execution threads.</p>
	 *
	 * @return {@code true} if the current thread is a remote execution
	 *         thread, otherwise {@code false}
	 */
    public static boolean isRemoteThread() {
       return OARemoteThreadDelegate.isRemoteThread(); 
    }

    /**
     * Determines whether this runtime is operating in server or standalone mode
     * for the class of the specified object.
     *
     * <p>If {@code obj} is {@code null}, {@code Object.class} is used.</p>
     *
     * @param obj the object whose class is evaluated for server mode
     * @return {@code true} if running as a server or standalone runtime;
     *         {@code false} if running as a client
     */
    public static boolean isServer(OAObject obj) {
        Class c;
        if (obj == null) c = Object.class;
        else c = obj.getClass();
		return OASyncDelegate.isServer(c);
    }

    /**
     * Determines whether this runtime is operating in workstation (client)
     * mode for the class of the specified object.
     *
     * <p>If {@code obj} is {@code null}, {@code Object.class} is used.</p>
     *
     * @param obj the object whose class is evaluated for workstation mode
     * @return {@code true} if not running as a server; otherwise {@code false}
     */
    public static boolean isWorkstation(OAObject obj) {
        Class c;
        if (obj == null) c = Object.class;
        else c = obj.getClass();
        return !OASyncDelegate.isServer(c);
    }

    
    /**
     * Notifies the synchronization client that an object has been created
     * on a workstation. Invoked by {@code OAObjectDelegate.initialize()}.
     *
     * @param obj the newly created object; ignored if {@code null}
     */
    public static void objectCreated(OAObject obj) {
	    if (obj == null) return;
	    
        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.objectCreated(obj);
    }

    /**
     * Notifies the synchronization client that an object has been finalized
     * or removed on a client JVM.
     *
     * <p>Called by {@code OAObject.finalize()}.</p>
     *
     * @param obj the object being finalized; ignored if {@code null}
     */
    protected static void objectFinalized(OAObject obj) {
        if (obj == null) return;
        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.objectFinalized(obj.getGuid());
    }

    /**
     * Requests that the synchronization client update any objects whose
     * state is not managed through hubs, based on the supplied object.
     *
     * @param obj the object used to trigger updates; ignored if {@code null}
     *            or if its GUID is invalid
     */
    public static void updateObjectsWithoutHubs(OAObject obj) {
        if (obj == null) return;
        long guid = obj.getGuid();
        if (guid < 0) return;

        OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
        if (sc != null) sc.updateObjectsWithoutHubs(obj);
    }

    
    /**
     * Creates a remote copy of the specified object using the remote client,
     * optionally excluding properties from the copy operation.
     *
     * @param oaObj the source object to copy
     * @param excludeProperties optional property names to exclude
     * @return the copied object created on the server, or {@code null} if
     *         no remote client is available
     */
     protected static OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
         if (oaObj == null) return null;
         RemoteClientInterface ri = OASyncDelegate.getRemoteClient(oaObj.getClass());
         if (ri != null) {
             OAObject obj = ri.createCopy(oaObj.getClass(), oaObj.getObjectKey(), excludeProperties);;
             return obj; 
         }
         return null;
     }
	
     /**
      * Requests a new GUID from the server for the class of the specified
      * object. If {@code obj} is {@code null}, {@code Object.class} is used.
      *
      * @param obj the object whose class determines the GUID source
      * @return the GUID supplied by the server
      */
     protected static long getGuidFromServer(OAObject obj) {
         Class c;
         if (obj == null) c = Object.class;
         else c = obj.getClass();
         return getGuidFromServer(c);
     }
     
     /**
      * Requests a new GUID from the server for the given class.
      *
      * @param clazz the class whose GUID is requested; defaults to
      *              {@code Object.class} if {@code null}
      * @return the GUID supplied by the server
      */
     protected static long getGuidFromServer(Class clazz) {
         if (clazz == null) clazz = Object.class;
         long guid = OASyncDelegate.getGuidFromServer(clazz);
         return guid;
    }

     /**
      * Saves the specified object on the server using the provided cascade
      * rule, if a remote server is available.
      *
      * @param oaObj the object to save
      * @param iCascadeRule cascade rule applied to the save operation
      * @return {@code true} if the object was saved on the server;
      *         otherwise {@code false}
      */
    protected static boolean save(OAObject oaObj, int iCascadeRule) {
        if (oaObj == null) return false;
        RemoteServerInterface rs = OASyncDelegate.getRemoteServer(oaObj.getClass());
        if (rs != null) {
            return rs.save(oaObj.getClass(), oaObj.getObjectKey(), iCascadeRule);
        }
        return false;
    }

    /**
     * Handles deletion routing based on runtime mode, synchronization
     * configuration, and thread-local suppression flags. Determines whether
     * deletion should occur locally or be forwarded to the server.
     *
     * @param obj the object to delete
     * @return {@code true} if deletion should occur locally;
     *         {@code false} if performed on the server
     */
    protected static boolean delete(final OAObject obj) {
        if (obj == null) return false;
        LOG.finer("obj="+obj);

        if (OASyncDelegate.isSingleUser()) {
            return true; // run delete
        }

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return true;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return true; 
        
        if (OASyncDelegate.isServer(obj.getClass())) { 
            // this will invoke on the server using OARemoteThread
            rs.serverDelete(obj.getClass(), obj.getObjectKey());
            return false;  
        }

        // this is running as OAClient
        if (!OARemoteThreadDelegate.shouldSendMessages()) return true;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return true;
        
        rs.serverDelete(obj.getClass(), obj.getObjectKey());  // will call OAObjectDeleteDelegate
        
        return false;
    }

    /**
     * Sends a delete notification to all connected clients when running
     * in server mode, unless synchronization message suppression flags
     * are active or the object is marked local-only.
     *
     * @param obj the object being deleted; ignored if {@code null}
     */
    protected static void sendDeleteToClients(OAObject obj) {
        if (obj == null) return;

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return;
        
        if (!OASyncDelegate.isServer(obj.getClass())) return;
        // needs to send these to client if on RemoteThread        
        
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;
        
        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return; 
        
        rs.clientDelete(obj.getClass(), obj.getObjectKey());
    }
    
    /**
     * Retrieves an object from the server using the specified class and key.
     *
     * @param clazz the object's class
     * @param key the object's key
     * @return the retrieved object, or {@code null} if no remote server exists
     */
	protected static OAObject getServerObject(Class clazz, OAObjectKey key) {
	    if (clazz == null || key == null) return null;
        RemoteServerInterface rs = OASyncDelegate.getRemoteServer(clazz);
        OAObject result;
        if (rs != null) {
            result = rs.getObject(clazz, key);
        }       
        else result = null;
        return result;
	}    
	
	/**
	 * Retrieves a blob value for a reference link property from the server
	 * using the synchronization client.
	 *
	 * @param oaObj the owner object
	 * @param linkPropertyName the link property name
	 * @return the blob bytes if available; otherwise {@code null}
	 */
    protected static byte[] getServerReferenceBlob(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
        Object obj = null;
        
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            obj = sc.getDetail(oaObj, linkPropertyName);
        }
        else {
            LOG.warning("This should only be called from workstations, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
        if (obj instanceof byte[]) return (byte[]) obj;
        return null;
    }    
	
    /**
     * Retrieves a reference value for the specified link property from the
     * server using the synchronization client.
     *
     * @param oaObj the owner object
     * @param linkPropertyName the link property name
     * @return the reference value, or {@code null} if unavailable
     */
    protected static Object getServerReference(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
        Object value = null;
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            value = sc.getDetail(oaObj, linkPropertyName);
        }
        else {
            LOG.warning("This should only be called from workstations, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
        return value;
    }

    /**
     * Retrieves the hub associated with a link property from the server
     * using the synchronization client.
     *
     * @param oaObj the source object
     * @param linkPropertyName the link property name
     * @return the hub instance, or {@code null} if not found
     */
	public static Hub getServerReferenceHub(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
    	Hub hub = null;
        OASyncClient sc = OASyncDelegate.getSyncClient(oaObj.getClass());
        if (sc != null) {
            Object obj = sc.getDetail(oaObj, linkPropertyName);
            if (obj instanceof Hub) hub = (Hub) obj;
            if (hub == null) {
                LOG.warning("OAObject.getDetail(\""+linkPropertyName+"\") not found on server for "+oaObj.getClass().getName());
            }
        }
        else {
            LOG.warning("This should only be called from clients, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
        }
		return hub;
	}
	
	/**
	 * Loads all data for the specified hub on the server without sending
	 * synchronization messages. Uses thread-local suppression flags during
	 * loading.
	 *
	 * @param thisHub the hub to load data into
	 * @param select optional select used when loading data
	 * @return {@code true} if executed on the server; otherwise {@code false}
	 */
	protected static boolean loadReferenceHubDataOnServer(Hub thisHub, OASelect select) {
        if (thisHub == null) return false;
        boolean bResult;
        if (OASyncDelegate.isServer(thisHub)) {
            //LOG.finest("hub="+hub);

            // 20140328 performance improvement 
            if (thisHub.getSelect() == null && select == null) return true;
            
            
            bResult = true;
            // load all data without sending messages
            // even though Hub.writeObject does this, this data could be used on server application
        	try {
        		OAThreadLocalDelegate.setSuppressCSMessages(true);
        		HubSelectDelegate.loadAllData(thisHub, select);
        	}
        	finally {
        		OAThreadLocalDelegate.setSuppressCSMessages(false);        	
        	}
        }
        else bResult = false;
        return bResult;
	}
	
	/**
	 * Sends a before-property-change notification to remote clients if
	 * synchronization rules permit. Handles suppression flags, loading
	 * states, calculated properties, and large blob detection.
	 *
	 * @param obj the source object
	 * @param propertyName the property name
	 * @param oldValue the previous value
	 * @param newValue the new value
	 */
    protected static void fireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
        if (obj == null) return;
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return;
        
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        
        if (OAThreadLocalDelegate.isLoading()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return;

        // LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        
        // 20130319 dont send out calc prop changes
        OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
        if (li != null && li.bCalculated) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        
        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = OAObjectInfoDelegate.getPropertyInfo(oi, propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        OAObjectKey key = obj.getObjectKey();
        rs.propertyChange(obj.getClass(), key, propertyName, newValue, bIsBlob);
	}
	
    /**
     * Disabled method retained for compatibility. Immediately returns
     * without sending any synchronization message.
     *
     * @param obj the source object
     * @param origKey the original object key
     * @param propertyName the property name
     * @param oldValue the old value
     * @param newValue the new value
     * @deprecated
     */
    protected static void fireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue) {
        // qqqqqqqqqqqqqqqqqqqqqqq  Important NOTE: dont send, it is now using beforePropertyChange
        if (true || false) return; //qqqqqqqqqqqqqq

        //LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        
        if (OAThreadLocalDelegate.isLoading()) return;
        if (OAThreadLocalDelegate.isSuppressCSMessages()) return;

        OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return;

        // 20130319 dont send out calc prop changes
        OALinkInfo li = OAObjectInfoDelegate.getLinkInfo(oi, propertyName);
        if (li != null && li.bCalculated) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        
        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = OAObjectInfoDelegate.getPropertyInfo(oi, propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs != null) {
            rs.propertyChange(obj.getClass(), origKey, propertyName, newValue, bIsBlob);
        }
    }
}

