package com.viaoa.graph.object;

import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.HubService;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.sync.remote.RemoteSyncInterface;

public class OAObjectCSService {
	private static final Logger LOG = Logger.getLogger(OAObjectCSService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final HubService srvcHub;
	private final OASyncService srvcSync;

	public OAObjectCSService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, HubService srvcHub, OASyncService srvcSync) {
		if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
		this.srvcObject = srvcObject;
		if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
		if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
		this.srvcHub = srvcHub;
		if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
		this.srvcSync = srvcSync;
	}

	public OAObjectService getObjectService() {
		return srvcObject;
	}

	/**
	 * Determines whether the current thread is an {@code OARemoteThread}, which
	 * indicates that the caller is executing within a remote-message processing
	 * context.
	 *
	 * <p>
	 * This performs a delegation to {@code OARemoteThreadDelegate.isRemoteThread()}
	 * which uses {@code Thread instanceof OARemoteThread} to identify remote
	 * execution threads.
	 * </p>
	 *
	 * @return {@code true} if the current thread is a remote execution thread,
	 *         otherwise {@code false}
	 */
	public static boolean isRemoteThread() {
		return OARemoteThreadDelegate.isRemoteThread();
	}

	/**
	 * Determines whether this runtime is operating in server or standalone mode for
	 * the class of the specified object.
	 *
	 * <p>
	 * If {@code obj} is {@code null}, {@code Object.class} is used.
	 * </p>
	 *
	 * @param obj the object whose class is evaluated for server mode
	 * @return {@code true} if running as a server or standalone runtime;
	 *         {@code false} if running as a client
	 */
	public boolean isServer(OAObject obj) {
		Class c;
		if (obj == null)
			c = Object.class;
		else
			c = obj.getClass();
		return OASyncDelegate.isServer(c);
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
		Class c;
		if (obj == null)
			c = Object.class;
		else
			c = obj.getClass();
		return !OASyncDelegate.isServer(c);
	}

	/**
	 * Notifies the synchronization client that an object has been created on a
	 * workstation. Invoked by {@code OAObjectDelegate.initialize()}.
	 *
	 * @param obj the newly created object; ignored if {@code null}
	 */
	public void objectCreated(OAObject obj) {
		if (obj == null)
			return;

		OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
		if (sc != null)
			sc.objectCreated(obj);
	}

	/**
	 * Notifies the synchronization client that an object has been finalized or
	 * removed on a client JVM.
	 *
	 * <p>
	 * Called by {@code OAObject.finalize()}.
	 * </p>
	 *
	 * @param obj the object being finalized; ignored if {@code null}
	 */
	public void objectFinalized(OAObject obj) {
		if (obj == null)
			return;
		OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
		if (sc != null)
			sc.objectFinalized(obj.getGuid());
	}

	/**
	 * Requests that the synchronization client update any objects whose state is
	 * not managed through hubs, based on the supplied object.
	 *
	 * @param obj the object used to trigger updates; ignored if {@code null} or if
	 *            its GUID is invalid
	 */
	public void updateObjectsWithoutHubs(OAObject obj) {
		if (obj == null)
			return;
		long guid = obj.getGuid();
		if (guid < 0)
			return;

		OASyncClient sc = OASyncDelegate.getSyncClient(obj.getClass());
		if (sc != null)
			sc.updateObjectsWithoutHubs(obj);
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
		if (oaObj == null)
			return null;
		RemoteClientInterface ri = OASyncDelegate.getRemoteClient(oaObj.getClass());
		if (ri != null) {
			OAObject obj = ri.createCopy(oaObj.getClass(), oaObj.getObjectKey(), excludeProperties);
			;
			return obj;
		}
		return null;
	}

	/**
	 * Requests a new GUID from the server for the class of the specified object. If
	 * {@code obj} is {@code null}, {@code Object.class} is used.
	 *
	 * @param obj the object whose class determines the GUID source
	 * @return the GUID supplied by the server
	 */
	public long getGuidFromServer(OAObject obj) {
		Class c;
		if (obj == null)
			c = Object.class;
		else
			c = obj.getClass();
		return getGuidFromServer(c);
	}

	/**
	 * Requests a new GUID from the server for the given class.
	 *
	 * @param clazz the class whose GUID is requested; defaults to
	 *              {@code Object.class} if {@code null}
	 * @return the GUID supplied by the server
	 */
	public long getGuidFromServer(Class clazz) {
		if (clazz == null)
			clazz = Object.class;
		long guid = OASyncDelegate.getGuidFromServer(clazz);
		return guid;
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
		if (oaObj == null)
			return false;
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
    public boolean delete(final OAObject obj) {
    	//qqqqqqqq method was protected
        if (obj == null) return false;
        LOG.finer("obj="+obj);

        if (OASyncDelegate.isSingleUser()) {
            return true; // run delete
        }

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return true;

        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(obj.getClass());
        if (oi.getLocalOnly()) return true; 
        
        if (OASyncDelegate.isServer(obj.getClass())) { 
            // this will invoke on the server using OARemoteThread
            rs.serverDelete(obj.getClass(), obj.getObjectKey());
            return false;  
        }

        // this is running as OAClient
        if (!OARemoteThreadDelegate.shouldSendMessages()) return true;
        if (OARuntime.get().threadLocalService().isSuppressCSMessages()) return true;
        
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
    public void sendDeleteToClients(OAObject obj) {
        if (obj == null) return;

        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        if (rs == null) return;
        
        if (!OASyncDelegate.isServer(obj.getClass())) return;
        // needs to send these to client if on RemoteThread        
        
        if (OARuntime.get().threadLocalService().isSuppressCSMessages()) return;
        
        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(obj.getClass());
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
	public OAObject getServerObject(Class clazz, OAObjectKey key) {
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
    public byte[] getServerReferenceBlob(OAObject oaObj, String linkPropertyName) {
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
    public Object getServerReference(OAObject oaObj, String linkPropertyName) {
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
	public Hub getServerReferenceHub(OAObject oaObj, String linkPropertyName) {
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
	public boolean loadReferenceHubDataOnServer(Hub thisHub, OASelect select) {
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
        		OARuntime.get().threadLocalService().setSuppressCSMessages(true);
        		srvcHub.getHubSelectService().loadAllData(thisHub, select);
        	}
        	finally {
        		OARuntime.get().threadLocalService().setSuppressCSMessages(false);        	
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
        RemoteSyncInterface rs = OASyncDelegate.getRemoteSync(obj.getClass());
        
        if (rs == null) return;
        
        if (!OARemoteThreadDelegate.shouldSendMessages()) return;
        
        if (OARuntime.get().threadLocalService().isLoading()) return;
        if (OARuntime.get().threadLocalService().isSuppressCSMessages()) return;

        OAObjectInfo oi = srvcObject.getOAObjectInfoService().getOAObjectInfo(obj);
        if (oi.getLocalOnly()) return;

        // LOG.finer("properyName="+propertyName+", obj="+obj+", newValue="+newValue);
        
        // 20130319 dont send out calc prop changes
        OALinkInfo li = srvcObject.getOAObjectInfoService().getLinkInfo(oi, propertyName);
        if (li != null && li.getCalculated()) return;
        // LOG.finer("object="+obj+", key="+origKey+", prop="+propertyName+", newValue="+newValue+", oldValue="+oldValue);

        
        // 20130318 if blob, then set a flag so that the server does not broadcast to all clients
        //     the clients (OAClient.procesPropChange) will recv the msg and know how to handle it.
        //       so that the next time the prop getXxx is called, it will then get it from the server
        boolean bIsBlob = false;
        if (newValue != null && newValue instanceof byte[]) {
            byte[] bs = (byte[]) newValue;
            if (bs.length > 400) {
                OAPropertyInfo pi = srvcObject.getOAObjectInfoService().getPropertyInfo(oi, propertyName);
                if (pi.isBlob()) {
                    bIsBlob = true;
                }
            }
        }
        OAObjectKey key = obj.getObjectKey();
        rs.propertyChange(obj.getClass(), key, propertyName, newValue, bIsBlob);
	}

	
}
