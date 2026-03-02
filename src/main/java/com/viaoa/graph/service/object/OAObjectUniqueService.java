package com.viaoa.graph.service.object;

import java.util.logging.Logger;

import com.viaoa.datasource.OASelect;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAString;

public abstract class OAObjectUniqueService {
	private static final Logger LOG = Logger.getLogger(OAObjectUniqueService.class.getName());

    public OAObjectUniqueService() {
    }
    
    // qqqq could be too heavy, rework new solution
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
        
        OAObject oaObj = (OAObject) callCacheFind(clazz, propertyName, uniqueKey);
        if (oaObj != null) return oaObj;
        
        // not found
        if (callCSIsClient()) {
            try {
            	oaObj = callSyncClientGetUnique(clazz, propertyName, uniqueKey, bAutoCreate);
                return oaObj;
            }
            catch (Exception e) {
                throw new RuntimeException("getUnique() getRemoteServer() exception", e);
            }
        }
        
        OASelect<?> select = new OASelect<>(clazz);
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
            oaObj = (OAObject) callReflectCreateNewObject(clazz);
            try {
            	callThreadLocalSetLoading(true);
                oaObj.setProperty(propertyName, uniqueKey);
            }
            finally {
            	callThreadLocalSetLoading(false);
            }
        }
        
        return oaObj;
    }

	public abstract Object callCacheFind(Class<? extends OAObject> clazz, String propertyPath, Object findObject);
	public abstract Object callReflectCreateNewObject(Class<?> clazz); 
	public abstract boolean callCSIsClient();
	public abstract OAObject callSyncClientGetUnique(Class<? extends OAObject> clazz, final String propertyName, Object uniqueKey, boolean bAutoCreate);
	public abstract void callThreadLocalSetLoading(boolean b);
}
