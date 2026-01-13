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
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.*;
import com.viaoa.remote.*;
import com.viaoa.runtime.OARuntime;

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

	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}

    
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
    	OAGraph g = getGraph(null, obj);
    	if (g == null) return false;
    	return g.objects().getOAObjectCSService().isServer(obj);
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
    	OAGraph g = getGraph(null, obj);
    	if (g == null) return false;
    	return g.objects().getOAObjectCSService().isWorkstation(obj);
    }

    
    /**
     * Notifies the synchronization client that an object has been created
     * on a workstation. Invoked by {@code OAObjectDelegate.initialize()}.
     *
     * @param obj the newly created object; ignored if {@code null}
     */
    public static void objectCreated(OAObject obj) {
    	OAGraph g = getGraph(null, obj);
    	if (g == null) return;
    	g.objects().getOAObjectCSService().objectCreated(obj);
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
    	OAGraph g = getGraph(null, obj);
    	if (g == null) return;
    	g.objects().getOAObjectCSService().objectFinalized(obj.getGuid());
    }

    /**
     * Requests that the synchronization client update any objects whose
     * state is not managed through hubs, based on the supplied object.
     *
     * @param obj the object used to trigger updates; ignored if {@code null}
     *            or if its GUID is invalid
     */
    public static void updateObjectsWithoutHubs(OAObject obj) {
    	OAGraph g = getGraph(null, obj);
    	if (g == null) return;
    	g.objects().getOAObjectCSService().updateObjectsWithoutHubs(obj);
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
     public static OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
    	//qqqqqqqqqq method was protected
     	OAGraph g = getGraph(null, oaObj);
     	if (g == null) return null;
     	return g.objects().getOAObjectCSService().createCopy(oaObj, excludeProperties);
     }
	
     /**
      * Requests a new GUID from the server for the class of the specified
      * object. If {@code obj} is {@code null}, {@code Object.class} is used.
      *
      * @param obj the object whose class determines the GUID source
      * @return the GUID supplied by the server
      */
     /*qqqqqqq
     protected static long getGuidFromServer(OAObject obj) {
      	OAGraph g = getGraph(null, obj);
      	if (g == null) return 0L;
      	return g.objects().getOAObjectCSService().getGuidFromServer(obj);
     }
     */
     
     /**
      * Requests a new GUID from the server for the given class.
      *
      * @param clazz the class whose GUID is requested; defaults to
      *              {@code Object.class} if {@code null}
      * @return the GUID supplied by the server
      */
     /*qqqqqqqqqq
     protected static long getGuidFromServer(Class clazz) {
 		OAGraph g = OARuntime.get().graph(clazz);
 		if (g == null) return 0l;
      	return g.objects().getOAObjectCSService().getGuidFromServer(clazz);
    }
    */

     /**
      * Saves the specified object on the server using the provided cascade
      * rule, if a remote server is available.
      *
      * @param oaObj the object to save
      * @param iCascadeRule cascade rule applied to the save operation
      * @return {@code true} if the object was saved on the server;
      *         otherwise {@code false}
      */
    public static boolean save(OAObject oaObj, int iCascadeRule) {
    	//qqqqqqqqqq method was protected
      	OAGraph g = getGraph(null, oaObj);
      	if (g == null) return false;
      	return g.objects().getOAObjectCSService().save(oaObj, iCascadeRule);
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
    public static boolean delete(final OAObject obj) {
    	//qqqqqqqq method was protected
      	OAGraph g = getGraph(null, obj);
      	if (g == null) return false;
      	return g.objects().getOAObjectCSService().delete(obj);
    }

    /**
     * Sends a delete notification to all connected clients when running
     * in server mode, unless synchronization message suppression flags
     * are active or the object is marked local-only.
     *
     * @param obj the object being deleted; ignored if {@code null}
     */
    public static void sendDeleteToClients(OAObject obj) {
    	//qqqqqqq method was protected
      	OAGraph g = getGraph(null, obj);
      	if (g == null) return;
      	g.objects().getOAObjectCSService().sendDeleteToClients(obj);
    }
    
    /**
     * Retrieves an object from the server using the specified class and key.
     *
     * @param clazz the object's class
     * @param key the object's key
     * @return the retrieved object, or {@code null} if no remote server exists
     */
	public static OAObject getServerObject(Class clazz, OAObjectKey key) {
		//qqqqqq method was protected
 		OAGraph g = OARuntime.get().graph(clazz);
 		if (g == null) return null;
      	return g.objects().getOAObjectCSService().getServerObject(clazz, key);
	}    
	
	/**
	 * Retrieves a blob value for a reference link property from the server
	 * using the synchronization client.
	 *
	 * @param oaObj the owner object
	 * @param linkPropertyName the link property name
	 * @return the blob bytes if available; otherwise {@code null}
	 */
    public static byte[] getServerReferenceBlob(OAObject oaObj, String linkPropertyName) {
		//qqqqqq method was protected
      	OAGraph g = getGraph(null, oaObj);
      	if (g == null) return null;
      	return g.objects().getOAObjectCSService().getServerReferenceBlob(oaObj, linkPropertyName);
    }    
	
    /**
     * Retrieves a reference value for the specified link property from the
     * server using the synchronization client.
     *
     * @param oaObj the owner object
     * @param linkPropertyName the link property name
     * @return the reference value, or {@code null} if unavailable
     */
    public static Object getServerReference(OAObject oaObj, String linkPropertyName) {
		//qqqqqq method was protected
      	OAGraph g = getGraph(null, oaObj);
      	if (g == null) return null;
      	return g.objects().getOAObjectCSService().getServerReference(oaObj, linkPropertyName);
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
      	OAGraph g = getGraph(null, oaObj);
      	if (g == null) return null;
      	return g.objects().getOAObjectCSService().getServerReferenceHub(oaObj, linkPropertyName);
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
	public static boolean loadReferenceHubDataOnServer(Hub thisHub, OASelect select) {
		//qqqqqq method was protected
      	OAGraph g = getGraph(thisHub, null);
      	if (g == null) return false;
      	return g.objects().getOAObjectCSService().loadReferenceHubDataOnServer(thisHub, select);
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
    public static void fireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
    	//qqqqqqqqqq method was protected
      	OAGraph g = getGraph(null, obj);
      	if (g == null) return;
      	g.objects().getOAObjectCSService().fireBeforePropertyChange(obj, propertyName, oldValue, newValue);
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

