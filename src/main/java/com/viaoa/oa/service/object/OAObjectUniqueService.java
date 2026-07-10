package com.viaoa.oa.service.object;

import java.util.logging.Logger;

import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

/**
 * Resolves or creates unique OAObjects by configured unique property values.
 */
public abstract class OAObjectUniqueService {
	private static final Logger LOG = Logger.getLogger(OAObjectUniqueService.class.getName());

	/**
	 * Performs OAObjectUniqueService behavior for the OA object service.
	 */
    public OAObjectUniqueService() {
    }
    
    private Object Lock = new Object();

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
        select.close();
        if (oaObj != null) {
            return oaObj;
        }
        if (!bAutoCreate) return null;

        // need to create new, this needs to be synchronized
        synchronized (Lock) {
            oaObj = getUnique(clazz, propertyName, uniqueKey, false);
            if (oaObj != null) return oaObj;
            oaObj = (OAObject) callReflectCreateNewObject(clazz);
        	boolean bWasLoading = callThreadLocalSetLoading(true);
            try {
                oaObj.setProperty(propertyName, uniqueKey);
            }
            finally {
            	callThreadLocalSetLoading(bWasLoading);
            }
        }
        
        return oaObj;
    }

	/**
	 * Dependency hook used by this service to cacheFind.
	 *
	 * @param clazz method input
	 * @param path method input
	 * @param findObject method input
	 * @return result value
	 */
	public abstract Object callCacheFind(Class<? extends OAObject> clazz, String path, Object findObject);
	/**
	 * Dependency hook used by this service to reflectCreateNewObject.
	 *
	 * @param clazz method input
	 * @return result value
	 */
	public abstract Object callReflectCreateNewObject(Class<?> clazz); 
	/**
	 * Dependency hook used by this service to cSIsClient.
	 *
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callCSIsClient();
	/**
	 * Dependency hook used by this service to syncClientGetUnique.
	 *
	 * @param clazz method input
	 * @param propertyName method input
	 * @param uniqueKey method input
	 * @param bAutoCreate method input
	 * @return result value
	 */
	public abstract OAObject callSyncClientGetUnique(Class<? extends OAObject> clazz, final String propertyName, Object uniqueKey, boolean bAutoCreate);
	/**
	 * Dependency hook used by this service to threadLocalSetLoading.
	 *
	 * @param b method input
	 * @return {@code true} when the operation succeeds or condition is met
	 */
	public abstract boolean callThreadLocalSetLoading(boolean b);
}
