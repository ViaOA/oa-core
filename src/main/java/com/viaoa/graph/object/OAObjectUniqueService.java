package com.viaoa.graph.object;

import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.OASyncService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.util.OAString;

public class OAObjectUniqueService {
	private static final Logger LOG = Logger.getLogger(OAObjectUniqueService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;
	private final OASyncService srvcSync;
	
    public OAObjectUniqueService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess, OASyncService srvcSync) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = oaObjectFriendAccess;
    	if (srvcSync == null) throw new IllegalArgumentException("OASyncService can not be null");
    	this.srvcSync = srvcSync;
    }

    
    
    private final Object Lock = new Object();

    /**
     * Finds or creates an {@link OAObject} instance with the specified unique
     * property value. The method performs the lookup using several layers of
     * resolution and optionally creates a new instance when no match exists.
     * <p>
     * Behavior visible in this implementation:
     * <ul>
     *   <li>Immediately returns {@code null} if {@code clazz}, {@code uniqueKey},
     *       or {@code propertyName} are invalid.</li>
     *   <li>Searches the {@link OAObjectCacheDelegate} for an existing object
     *       matching the class, property name, and unique value.</li>
     *   <li>If running as a client, attempts to delegate the request to the
     *       remote server using {@link OASyncClient} and
     *       {@link RemoteServerInterface#getUnique(Class, String, Object, boolean)}.</li>
     *   <li>Performs a data source query using {@link OASelect} if not already
     *       found.</li>
     *   <li>If still not found and {@code bAutoCreate} is {@code true}, enters a
     *       synchronized block to safely create and initialize a new instance.</li>
     *   <li>Uses {@link OAThreadLocalDelegate#setLoading(boolean)} to suppress
     *       change events during initialization of the new instance.</li>
     * </ul>
     *
     * @param clazz the class of object to search or create
     * @param propertyName the name of the unique property
     * @param uniqueKey the unique value to match
     * @param bAutoCreate whether to create a new instance if none exists
     * @return the matching or newly created {@link OAObject}, or {@code null} if
     *         not found and auto-creation is disabled
     */
    public OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate) {
        
        if (clazz == null) return null;
        if (uniqueKey == null) return null;
        if (OAString.isEmpty(propertyName)) return null;
        
        OAObject oaObj = (OAObject) srvcObject.getOAObjectCacheService().find(clazz, propertyName, uniqueKey);
        if (oaObj != null) return oaObj;
        
        // not found
        if (srvcSync.isClient()) {
            OASyncClient sc = OASync.getSyncClient();
            RemoteServerInterface rs;
            try {
                rs = sc.getRemoteServer();

                if (rs != null) {
                    oaObj = rs.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
                    return oaObj;
                }
            }
            catch (Exception e) {
                throw new RuntimeException("getUnique() getRemoteServer() exception", e);
            }
        }
        
        OASelect select = new OASelect(clazz);
        select.setWhere(propertyName+" = ?", new Object[] {uniqueKey});
        oaObj = select.next();
        if (oaObj != null) {
            return oaObj;
        }
        if (!bAutoCreate) return null;

        // need to create new, this needs to be synchronized
        synchronized (Lock) {
            oaObj = getUnique(clazz, propertyName, uniqueKey, false);
            if (oaObj != null) return oaObj;
            oaObj = (OAObject) srvcObject.getOAObjectReflectService().createNewObject(clazz);
            try {
            	OARuntime.get().threadService().setLoading(true);
                oaObj.setProperty(propertyName, uniqueKey);
            }
            finally {
            	OARuntime.get().threadService().setLoading(false);
            }
        }
        
        return oaObj;
    }

}
