package com.viaoa.graph.service.object;

import java.util.UUID;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
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
		return isServer();
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
		return !isServer();
	}

	/**
	 * Notifies the synchronization client that an object has been created on a
	 * workstation. Invoked by {@code OAObjectDelegate.initialize()}.
	 *
	 * @param obj the newly created object; ignored if {@code null}
	 */
	public void objectCreated(OAObject obj) {
		if (obj == null) return;
		if (isClient()) {
			callObjectCreated(obj);
		}
	}


	public void objectFinalized(UUID guid) {
		if (guid == null) return;
		if (isClient()) {
			callObjectFinalized(guid);
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
		if (getOAObjectInfo(obj.getClass()).getLocalOnly()) return;
		callUpdateObjectsWithoutHubs(obj);
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
		OAObject obj = createCopy(oaObj.getClass(), oaObj.getObjectKey(), excludeProperties);
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
		return syncServerSave(oaObj.getClass(), oaObj.getObjectKey(), iCascadeRule);
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

        if (isSingleUser()) {
            return true; // run delete
        }

        OAObjectInfo oi = getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return true; 
        
        if (isClient()) { 
	        if (!shouldSendMessages()) return true;
	        if (isSuppressCSMessages()) return true;
        }
        
        serverDelete(obj.getClass(), obj.getObjectKey());  // will call OAObjectDeleteDelegate
        
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

        if (!isServer()) return;
        // needs to send these to client if on RemoteThread        
        
        if (isSuppressCSMessages()) return;
        
        OAObjectInfo oi = getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return; 
        
        clientDelete(obj.getClass(), obj.getObjectKey());
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
        
		if (isClient()) {
            obj = getDetail(oaObj, propertyName);
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
		if (isClient()) {
            value = getDetail(oaObj, linkPropertyName);
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
    	Hub hub = null;

		if (isClient()) {
            Object obj = getDetail(oaObj, linkPropertyName);
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
	public boolean loadReferenceHubDataOnServer(Hub thisHub, OASelect select) {
        if (thisHub == null) return false;
        boolean bResult;
        if (isServer()) {
            //LOG.finest("hub="+hub);

            // 20140328 performance improvement 
            if (thisHub.getSelect() == null && select == null) return true;
            
            
            bResult = true;
            // load all data without sending messages
            // even though Hub.writeObject does this, this data could be used on server application
        	try {
        		setSuppressCSMessages(true);
        		loadAllData(thisHub, select);
        	}
        	finally {
        		setSuppressCSMessages(false);        	
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
		
		if (!isServer() && !isClient()) return;
        
        if (!shouldSendMessages()) return;
        if (isLoading()) return;
        if (isSuppressCSMessages()) return;

        OAObjectInfo oi = getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return;

        // LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        
        // 20130319 dont send out calc prop changes
        OALinkInfo li = getLinkInfo(oi, propertyName);
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
        propertyChange(obj.getClass(), key, propertyName, newValue, bIsBlob);
	}



	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "getOAObjectInfo(class)"
	)
	public abstract OAObjectInfo getOAObjectInfo(Class clazz);	
    
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.isSingleUser()"
	)
	public abstract boolean isSingleUser();
    
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.isServer()"
	)
	public abstract boolean isServer();

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.isClient()"
	)
	public abstract boolean isClient();
	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getSyncClient().getDetail(oaObj, linkPropertyName)"
	)
	public abstract Object getDetail(final OAObject masterObject, final String propertyName);	

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getRemoteSync().propertyChange(c, ok, propertyName, val, b)"
	)
	public abstract boolean propertyChange(Class objectClass, OAObjectKey origKey, String propertyName, Object newValue, boolean bIsBlob);

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "OAObjectService.this.srvcSync.getSyncClient().objectCreated"
	)
	public abstract void callObjectCreated(OAObject obj);	

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "OAObjectService.this.srvcSync.getSyncClient().objectFinalized"
	)
	public abstract void callObjectFinalized(UUID guid);	


	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcHub.getHubSelectService().loadAllData(thisHub, select)"
	)
	public abstract void loadAllData(Hub thisHub, OASelect select);
	
	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getSyncClient().updateObjectsWithoutHubs(obj)"
	)
	public abstract void callUpdateObjectsWithoutHubs(OAObject obj);


	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getRemoteClient().createCopy(c, ok, excludeProps)"
	)
	public abstract OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties);


	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "getSyncService().getRemoteServer().save(oaObj.getClass(), oaObj.getObjectKey(), iCascadeRule)"
	)
	public abstract boolean syncServerSave(Class objectClass, OAObjectKey objectKey, int iCascadeRule);

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcObjectInfo.getOALinkInfo(oi, name)"
	)
	public abstract OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName);

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = ""
	)
	public abstract boolean shouldSendMessages();
	
	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcOAThreadLocal.isSuppressCSMessages()"
	)
	public abstract boolean isSuppressCSMessages();		
	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcOAThreadLocal.isLoading()"
	)
	public abstract boolean isLoading();		


	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcOAThreadLocal.setSuppressCSMessages(b)"
	)
	public abstract void setSuppressCSMessages(boolean b);		

	
	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getRemoteServer().getObject(clazz, key)"
	)
	public abstract OAObject getServerObject(Class clazz, OAObjectKey key);

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getRemoteSync().serverDelete(clazz, key)"
	)
	public abstract boolean serverDelete(Class clazz, OAObjectKey key);

	@OAParentProvided (
		parentName = "OAObjectService", 
		purpose="", 
		example = "srvcSync.getRemoteSync().clientDelete(clazz, key)"
	)
	public abstract boolean clientDelete(Class clazz, OAObjectKey key);

}

