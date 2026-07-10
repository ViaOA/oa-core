package com.viaoa.oa.service.hub;

import java.util.logging.Logger;

import com.viaoa.filter.*;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.*;
import com.viaoa.object.*;

/**
 * Finds objects within Hubs and related Hub structures.
 */

public abstract class HubFindService {
	private final Logger LOG = Logger.getLogger(HubFindService.class.getName());

	public HubFindService() {
	}

	
	/**
	 * Resolves the canonical instance of the given object for this hub. If the
	 * object's class does not match the hub's object class, the cache is queried
	 * first; if no cached instance exists, the hub is asked to resolve the object,
	 * potentially triggering data loading.
	 *
	 * @param hub    the hub providing the object class and lookup context
	 * @param object the object or key to resolve
	 * @return the resolved object instance, or the original value if no resolution
	 *         occurs
	 */
	@SuppressWarnings("unchecked")
	public <T extends OAObject> T getRealObject(Hub<T> hub, Object object) {
		if (object != null && !object.getClass().equals(hub.getObjectClass())) {
			T objx = callObjectCacheGet(hub.getObjectClass(), object);
			if (objx != null) {
				return objx;
			}
			object = callHubDataGetObject(hub, object); // might not have loaded all data yet (fetchMore will be called)
		}
		return (T) object;
	}

	
	/**
	 * Finds the first object in the specified {@code Hub} whose property located by
	 * {@code path} matches the supplied {@code findValue} using a
	 * {@link com.viaoa.filter.OALikeFilter}.
	 *
	 * <p>If {@code bSetAO} is {@code true}, the found object is also set as the
	 * Hub’s active object.</p>
	 *
	 * @param thisHub the {@code Hub} to search; may be {@code null}
	 * @param path the property path to evaluate for matching
	 * @param findValue the value to compare against using a like-filter match
	 * @param bSetAO if {@code true}, sets the active object to the found object
	 * @param lastFoundObject the last object found, used by {@link com.viaoa.find.OAFinder#findNext}
	 * @return the first matching object, or {@code null} if none found
	 */
	@SuppressWarnings("unchecked")
    public <T extends OAObject> T findFirst(Hub<T> thisHub, String path, final Object findValue, final boolean bSetAO, T lastFoundObject) {
        if (thisHub == null) return null;
        
        OAFinder<T,?> finder = new OAFinder<>();
        finder.addFilter(new OALikeFilter(path, findValue));
        T foundObj = (T) finder.findNext(thisHub, lastFoundObject);
        
        if (bSetAO) thisHub.setAO(foundObj);
        return foundObj;
	}

	/**
	 * Dependency hook used by this service for ObjectCacheGet behavior.
	 *
	 * @param clazz method input
	 * @param key method input
	 * @return result value
	 */

	public abstract <T extends OAObject> T callObjectCacheGet(Class<T> clazz, Object key);
	/**
	 * Dependency hook used by this service for HubDataGetObject behavior.
	 *
	 * @param thisHub method input
	 * @param key method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T callHubDataGetObject(final Hub<T> thisHub, Object key);

}


