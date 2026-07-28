package com.viaoa.oa.api.internal.objects;

import java.util.List;
import java.util.UUID;

import com.viaoa.cache.OAObjectCacheListener;
import com.viaoa.callback.OACallback;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

/**
 * Internal access to the OA object cache, cache listeners, select-all hubs, and cache searches.
 */
public interface OAObjectCacheOps {

	/**
	 * Fires the OA after-load event for an object.
	 *
	 * @param oaObj the loaded object
	 */
	public void fireAfterLoadEvent(OAObject oaObj);
	/**
	 * Returns object classes currently known to the object cache.
	 *
	 * @return the cached OAObject classes
	 */
	public Class<? extends OAObject>[] getClasses();
	/**
	 * Invokes a callback for cached objects of the supplied class.
	 *
	 * @param clazz the object class
	 * @param callback the callback to invoke
	 */
	public <T extends OAObject> void callback(Class<T> clazz, OACallback<T> callback);
	/**
	 * Returns the number of cached objects for a class.
	 *
	 * @param clazz the object class
	 * @return the cached object count
	 */
	public int getTotal(Class<? extends OAObject> clazz);
	/**
	 * Registers an object-cache listener for a class.
	 *
	 * @param clazz the object class
	 * @param cachelistener the listener to add
	 */
	public <T extends OAObject> void addListener(Class<T> clazz, OAObjectCacheListener<T> cachelistener);
	/**
	 * Visits cached objects of a class using the supplied callback.
	 *
	 * @param clazz the object class
	 * @param callback the callback to invoke
	 */
	public <T extends OAObject> void visit(Class<T> clazz, OACallback<T> callback);
	/**
	 * Removes an object-cache listener for a class.
	 *
	 * @param clazz the object class
	 * @param cacheListener the listener to remove
	 */
	public <T extends OAObject> void removeListener(Class<T> clazz, OAObjectCacheListener<T> cacheListener);
	/**
	 * Returns the select-all Hub associated with a cached class, when one is registered.
	 *
	 * @param clazz the object class
	 * @return the select-all Hub, or {@code null}
	 */
	public <T extends OAObject> Hub<T> getSelectAllHub(Class<T> clazz);
	/**
	 * Registers the select-all Hub for its object class.
	 *
	 * @param hub the select-all Hub
	 */
	public <T extends OAObject> void setSelectAllHub(Hub<T> hub);
	/**
	 * Finds a cached object by class and OAObjectKey.
	 *
	 * @param clazz the object class
	 * @param objectKey the object key
	 * @return the cached object, or {@code null}
	 */
	public <T extends OAObject> T getUsingKey(Class<T> clazz, Object key);

	public <T extends OAObject> T getUsingGuid(Class<T> clazz, UUID guid);
	/**
	 * Removes an object from the cache.
	 *
	 * @param oaObj the object to remove
	 */
	public void removeObject(OAObject oaObj);
	/**
	 * Refreshes cache state for a class.
	 *
	 * @param clazz the object class
	 */
	public void refresh(Class<? extends OAObject> clazz);
	/**
	 * Removes all cached objects for a class.
	 *
	 * @param clazz the object class
	 */
	public void removeAllObjects(Class<? extends OAObject> clazz);

	public void removeAllObjects();
	
	/**
	 * Finds cached objects using a finder, filter, or property-path search.
	 *
	 * @return the first matching object, matching array, or {@code null} depending on overload
	 */
	public <T extends OAObject> T find(Class<T> clazz, OAFinder<T, T> finder);
	/**
	 * Finds cached objects using a finder, filter, or property-path search.
	 *
	 * @return the first matching object, matching array, or {@code null} depending on overload
	 */
	public <T extends OAObject> T find(T fromObject, Class<T> clazz, int fetchAmount, List<T> alResults);
	/**
	 * Finds cached objects using a finder, filter, or property-path search.
	 *
	 * @return the first matching object, matching array, or {@code null} depending on overload
	 */
	public <T extends OAObject> T find(T fromObject, Class<T> clazz, OAFilter<T> filter, boolean bSkipNew, boolean bThrowException, int fetchAmount, List<T> alResults);
	
	/**
	 * Adds an object to the cache.
	 *
	 * @param oaObj the object to add
	 * @param bErrorIfExists {@code true} to reject an existing cached object
	 * @param bAddToSelectAll {@code true} to also add to the select-all Hub
	 * @return the cached object instance
	 */
	public <T extends OAObject> T add(T oaObj, boolean bErrorIfExists, boolean bAddToSelectAll);
	/**
	 * Unregisters a select-all Hub.
	 *
	 * @param hub the Hub to unregister
	 */
	public <T extends OAObject> void removeSelectAllHub(Hub<T> hub);
	/**
	 * Appends cache diagnostic information to a list.
	 *
	 * @param al the list that receives diagnostic lines
	 */
	public void getInfo(List<String> al);
	/**
	 * Returns a random cached object for a class.
	 *
	 * @param clazz the object class
	 * @param max maximum number of cached entries to consider
	 * @return a cached object, or {@code null}
	 */
	public OAObject getRandom(Class<? extends OAObject> clazz, int max);
}
