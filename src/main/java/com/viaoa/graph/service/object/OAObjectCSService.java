package com.viaoa.graph.service.object;

import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;

/**
   Relies on OASyncService to coordinate internal OA CS (client/server) functionality.
	qqqqqqqqqqqq needs updated
*/
public abstract class OAObjectCSService {
	private static final Logger LOG = Logger.getLogger(OAObjectCSService.class.getName());

	public OAObjectCSService() {
	}

	/**
	 * Determines whether this runtime is operating in server or standalone mode.
	 * 
	 * @return {@code true} if running as a server or standalone runtime;
	 *         {@code false} if running as a client
	 */
	public boolean isServer(OAObject obj) {
		//qqqqqqq obj not used, remove qqqqq todo:
		return callSyncIsServer();
	}

	
	/**
	 * Determines whether this runtime is operating in workstation (client) mode for
	 * the class of the specified object.
	 *
	 * <p>
	 * If {@code obj} is {@code null}, {@code Object.class} is used.
	 * </p>
	 *
	 * @param obj the object whose class is evaluated for workstation mode
	 * @return {@code true} if not running as a server; otherwise {@code false}
	 */
	public boolean isWorkstation(OAObject obj) {
		return !callSyncIsServer();
	}

	public boolean isWorkstation() {
		return !callSyncIsServer();
	}
	
	/**
	 * Notifies the synchronization client that an object has been created on a
	 * workstation. Invoked by {@code OAObjectDelegate.initialize()}.
	 *
	 * @param obj the newly created object; ignored if {@code null}
	 */
	public void objectCreated(OAObject obj) {
		if (obj == null) return;
		if (callSyncIsClient()) {
			callSyncClientObjectCreated(obj);
		}
	}


	public void objectFinalized(UUID guid) {
		if (guid == null) return;
		if (callSyncIsClient()) {
			callSyncClientObjectFinalized(guid);
		}
	}

	
	/**
	 * Requests that the synchronization client update any objects whose state is
	 * not managed through hubs, based on the supplied object.
	 *
	 * @param obj the object used to trigger updates; ignored if {@code null} or if
	 *            its GUID is invalid
	 */
	public void updateObjectsWithoutHubs(OAObject obj) {
		if (obj == null) return;
		if (callInfoGetObjectInfo(obj.getClass()).getLocalOnly()) return;
		callSyncClientUpdateObjectsWithoutHubs(obj);
	}

	/**
	 * Creates a remote copy of the specified object using the remote client,
	 * optionally excluding properties from the copy operation.
	 *
	 * @param oaObj             the source object to copy
	 * @param excludeProperties optional property names to exclude
	 * @return the copied object created on the server, or {@code null} if no remote
	 *         client is available
	 */
	public OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
		if (oaObj == null) return null;
		OAObject obj = callSyncClientCreateCopy(oaObj.getClass(), oaObj.getObjectKey(), excludeProperties);
		return obj;
	}

	/**
	 * Saves the specified object on the server using the provided cascade rule, if
	 * a remote server is available.
	 *
	 * @param oaObj        the object to save
	 * @param iCascadeRule cascade rule applied to the save operation
	 * @return {@code true} if the object was saved on the server; otherwise
	 *         {@code false}
	 */
	public boolean save(OAObject oaObj, int iCascadeRule) {
		if (oaObj == null) return false;
		return callSyncServerSave(oaObj.getClass(), oaObj.getObjectKey(), iCascadeRule);
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
    public boolean delete(final OAObject obj) {
        if (obj == null) return false;
        LOG.finer("obj="+obj);

        if (callSyncIsSingleUser()) {
            return true; // run delete
        }

        OAObjectInfo oi = callInfoGetObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return true; 
        
        if (callSyncIsClient()) { 
	        if (!callRemoteThreadShouldSendMessages()) return true;
	        if (callThreadLocalIsSuppressCSMessages()) return true;
        }
        
        callSyncSyncServerDelete(obj.getClass(), obj.getObjectKey());  // will call OAObjectDeleteDelegate
        
        return false;
    }
	
	
     /**
     * Sends a delete notification to all connected clients when running
     * in server mode, unless synchronization message suppression flags
     * are active or the object is marked local-only.
     *
     * @param obj the object being deleted; ignored if {@code null}
     */
    public void sendDeleteToClients(OAObject obj) {
        if (obj == null) return;

        if (!callSyncIsServer()) return;
        // needs to send these to client if on RemoteThread        
        
        if (callThreadLocalIsSuppressCSMessages()) return;
        
        OAObjectInfo oi = callInfoGetObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return; 
        
        callSyncSyncClientDelete(obj.getClass(), obj.getObjectKey());
    }
	

	/**
	 * Retrieves a blob value for a reference link property from the server
	 * using the synchronization client.
	 *
	 * @param oaObj the owner object
	 * @param propertyName the link property name
	 * @return the blob bytes if available; otherwise {@code null}
	 */
    public byte[] getServerReferenceBlob(OAObject oaObj, String propertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+propertyName);
        if (oaObj == null || propertyName == null) return null;
        Object obj = null;
        
		if (callSyncIsClient()) {
            obj = callSyncClientGetDetail(oaObj, propertyName);
		}
		else {
			LOG.warning("This should only be called from clients, not server. Object="+oaObj+", linkPropertyName="+propertyName);
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
    public Object getServerReference(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
        Object value = null;
		if (callSyncIsClient()) {
            value = callSyncClientGetDetail(oaObj, linkPropertyName);
		}
        else {
            LOG.warning("This should only be called from clients, not server. Object="+oaObj+", linkPropertyName="+linkPropertyName);
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
	public Hub getServerReferenceHub(OAObject oaObj, String linkPropertyName) {
        LOG.finer("object="+oaObj+", linkProperyName="+linkPropertyName);
        if (oaObj == null || linkPropertyName == null) return null;
    	Hub<?> hub = null;

		if (callSyncIsClient()) {
            Object obj = callSyncClientGetDetail(oaObj, linkPropertyName);
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
	public <T extends OAObject> boolean loadReferenceHubDataOnServer(Hub<T> thisHub, OASelect<T> select) {
        if (thisHub == null) return false;
        boolean bResult;
        if (callSyncIsServer()) {
            //LOG.finest("hub="+hub);

            // 20140328 performance improvement 
            if (thisHub.getSelect() == null && select == null) return true;
            
            
            bResult = true;
            // load all data without sending messages
            // even though Hub.writeObject does this, this data could be used on server application
        	try {
        		callThreadLocalSetSuppressCSMessages(true);
        		callHubSelectLoadAllData(thisHub, select);
        	}
        	finally {
        		callThreadLocalSetSuppressCSMessages(false);        	
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
    public void fireBeforePropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
        if (obj == null) return;
		
		if (!callSyncIsServer() && !callSyncIsClient()) return;
        
        if (!callRemoteThreadShouldSendMessages()) return;
        if (callThreadLocalIsLoading()) return;
        if (callThreadLocalIsSuppressCSMessages()) return;

        OAObjectInfo oi = callInfoGetObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return;

        // LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        
        // 20130319 dont send out calc prop changes
        OALinkInfo li = callInfoGetLinkInfo(oi, propertyName);
        if (li != null && li.getCalculated()) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        OAObjectKey key = obj.getObjectKey();
        callRemoteSyncPropertyChange(obj.getClass(), key, propertyName, newValue, bIsBlob);
	}

	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz);	
	public abstract boolean callSyncIsSingleUser();
	public abstract boolean callSyncIsServer();
	public abstract boolean callSyncIsClient();
	public abstract Object callSyncClientGetDetail(final OAObject masterObject, final String propertyName);	
	public abstract boolean callRemoteSyncPropertyChange(Class<? extends OAObject> objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob);
	public abstract void callSyncClientObjectCreated(OAObject obj);	
	public abstract void callSyncClientObjectFinalized(UUID guid);	
	public abstract <T extends OAObject> void callHubSelectLoadAllData(Hub<T> thisHub, OASelect<T> select);
	public abstract void callSyncClientUpdateObjectsWithoutHubs(OAObject obj);
	public abstract <T extends OAObject> T callSyncClientCreateCopy(Class<T> objectClass, OAObjectKey objectKey, String[] excludeProperties);
	public abstract boolean callSyncServerSave(Class<? extends OAObject> objectClass, OAObjectKey objectKey, int iCascadeRule);
	public abstract OALinkInfo callInfoGetLinkInfo(OAObjectInfo oi, String propertyName);
	public abstract boolean callRemoteThreadShouldSendMessages();
	public abstract boolean callThreadLocalIsSuppressCSMessages();		
	public abstract boolean callThreadLocalIsLoading();		
	public abstract void callThreadLocalSetSuppressCSMessages(boolean b);		
	public abstract <T extends OAObject> T callSyncServerGetObject(Class<T> clazz, OAObjectKey key);
	public abstract boolean callSyncSyncServerDelete(Class<? extends OAObject> clazz, OAObjectKey key);
	public abstract boolean callSyncSyncClientDelete(Class<? extends OAObject> clazz, OAObjectKey key);
}

