package com.viaoa.graph.object;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.objectcache.OADataSourceObjectCache;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAFilterDelegate;
import com.viaoa.filter.OAFilterDelegate.FinderInfo;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubTemp;
import com.viaoa.object.OACallback;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCache;
import com.viaoa.object.OAObjectCacheListener;
import com.viaoa.object.OAObjectHubDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectKeyDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Internal service responsible for managing the OAGraph OAObject cache,
 * ensuring global identity consistency and fast lookup by either GUID or
 * business keys. All stored references are weak so that objects may be
 * reclaimed by the garbage collector when no longer referenced elsewhere.
 *
 * <p>Identity resolution is GUID-first: if both GUID and business keys are
 * supplied, any conflict is resolved in favor of GUID. Business key lookups
 * are used when the GUID is unknown (e.g., object created or referenced using
 * only persistent identity), allowing lazy loading and identity reconciliation.</p>
 *
 * <p>The delegate also cooperates with client-session tracking for distributed
 * UI operation. When a client receives objects over the network, the server
 * maintains a strong reference set of objects currently visible to that client.
 * When the client releases objects, the cache is updated so that objects are
 * eligible for GC if no other strong references exist.</p>
 *
 * <p>This design allows the OA framework to maintain a consistent and correct
 * object graph while efficiently supporting distributed, event-driven, and
 * offline-first application behavior.</p>
 *
 * @see OAObjectCache
 * @see OAObject
 * @see OAObjectKey
 * @see OAObjectKeyDelegate
 */
public class OAObjectCacheService {
	private static final Logger LOG = Logger.getLogger(OAObjectCacheService.class.getName());

	private final OAObjectService srvcObject;
	private final OAObject.FriendAccess faObject;

	public OAObjectCacheService(OAObjectService srvcObject, OAObject.FriendAccess oaObjectFriendAccess) {
    	if (srvcObject == null) throw new IllegalArgumentException("ObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (oaObjectFriendAccess == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
		this.faObject = oaObjectFriendAccess;
	}

    public OAObjectService getObjectService() {
    	return srvcObject;
    }

	private final Map<Class<? extends OAObject>, List<OAObjectCacheListener<? extends OAObject>>> hmCacheListener = new ConcurrentHashMap<>();
    private final Map<Class<? extends OAObject>, WeakReference<Hub<? extends OAObject>>[]> hmCacheSelectAllHub = new HashMap<>(37,.75F);
    private final Map<String, WeakReference<Hub<? extends OAObject>>> hmCacheNamedHub = new HashMap<>(29,.75F);
	
	private final AtomicInteger aiListenerCount = new AtomicInteger();
	private boolean bDisableCache = false;
    
	private final LinkedBlockingQueue<SendAddEventInfo> queCacheSendAddEvent = new LinkedBlockingQueue<>();

	private final OAObjectCache objectCache = new OAObjectCache();
    
	protected int defaultAddMode = 1;
	
	/**
	 * throw an exception if a duplicate object is added. This is Default. see HubController#setAddMode
	 */
	static public final int NO_DUPS = 1; // dont use 0

	/**
	 * dont store object if a duplicate is already stored. If the object is being deserialized (see OAObject.readResolve) then the object
	 * that is already loaded will be used. see HubController#setAddMode
	 *
	 * @see OAObject#readResolve
	 */
	static public final int IGNORE_DUPS = 2;

	/**
	 * store object even if another exists see HubController#setAddMode
	 */
	// static public final int OVERWRITE_DUPS = 3; // not used qqqqqqqqqqqqqq

	/**
	 * dont store objects. see HubController#setAddMode
	 */
	static public final int IGNORE_ALL = 4;
	static protected final int MODE_MAX = 4;
	
	private volatile Thread threadCacheSendAddEvent;
	
	
	/**
	 * Internal helper class used to store information about an after-add
	 * event that needs to be dispatched asynchronously. Contains the array of
	 * listeners to notify and the object that was added.
	 */
	private class SendAddEventInfo {
		OAObjectCacheListener[] hls;
		Object obj;

		public SendAddEventInfo(OAObjectCacheListener[] hls, Object obj) {
			this.hls = hls;
			this.obj = obj;
		}
	}

	
	/**
	 * Returns all Hubs registered as “select all” Hubs for the specified class.
	 * These Hubs are maintained as weak references and automatically cleaned up
	 * when they are no longer strongly referenced elsewhere.
	 *
	 * @param clazz the class whose select-all Hubs are requested
	 * @return an array of matching Hubs, or {@code null} if none exist
	 */
	public Hub[] getSelectAllHubs(Class clazz) {
		if (clazz == null) {
			return null;
		}
		synchronized (hmCacheSelectAllHub) {
			WeakReference<Hub<? extends OAObject>>[] refs = hmCacheSelectAllHub.get(clazz);
			if (refs == null) return null;
			Hub[] hubs = new Hub[refs.length];
			for (int i = 0; i < refs.length; i++) {
				hubs[i] = (Hub) refs[i].get();
				if (hubs[i] == null) {
					if (refs.length == 1) {
						hmCacheSelectAllHub.remove(clazz);
						return null;
					} else {
						hmCacheSelectAllHub.put(clazz, removeSelectAllHubs(refs, refs[i]));
						return getSelectAllHubs(clazz);
					}
				}
			}
			return hubs;
		}
	}
	
	/**
	 * Returns the first Hub registered as a “select all” Hub for the
	 * specified class. If no such Hubs exist, this method returns {@code null}.
	 *
	 * @param clazz the class whose first select-all Hub is requested
	 * @return the first matching Hub, or {@code null} if none exist
	 */
	public Hub getSelectAllHub(Class clazz) {
		Hub[] hs = getSelectAllHubs(clazz);
		if (hs != null && hs.length > 0) {
			return hs[0];
		}
		return null;
	}
	
	/**
	 * Removes the specified weak reference from the array of select-all Hub
	 * references. If the reference is not found, the original array is returned.
	 *
	 * @param refs      the array of existing weak Hub references
	 * @param refRemove the specific weak reference to remove
	 * @return a new array with the reference removed, or the original array if not found
	 */
	private WeakReference[] removeSelectAllHubs(WeakReference[] refs, WeakReference refRemove) {
		WeakReference[] refs2 = new WeakReference[refs.length - 1];
		boolean bFound = false;
		int j = 0;
		for (int i = 0; i < refs.length; i++) {
			if (refs[i] == refRemove) {
				bFound = true;
			} else {
				refs2[j++] = refs[i];
			}
		}
		if (!bFound) {
			return refs;
		}
		return refs2;
	}
	
	/**
	 * Registers the specified Hub as a “select all” Hub for its object class.
	 * A weak reference is stored so the Hub can be automatically cleared when
	 * no longer strongly referenced. If the Hub is already registered, the call
	 * is ignored.
	 *
	 * @param hub the Hub to register as a select-all Hub
	 */
	public void setSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		LOG.fine("Hub.objectClass = " + clazz);

		synchronized (hmCacheSelectAllHub) {
			WeakReference[] refs = (WeakReference[]) hmCacheSelectAllHub.get(clazz);
			if (refs == null) {
				refs = new WeakReference[1];
			} else {
				// first see if Hub is already in the list
				for (int i = 0; i < refs.length; i++) {
					if (hub == refs[i].get()) {
						return;
					}
				}
				WeakReference[] refs2 = new WeakReference[refs.length + 1];
				System.arraycopy(refs, 0, refs2, 0, refs.length);
				refs = refs2;
			}
			refs[refs.length - 1] = new WeakReference(hub);
			hmCacheSelectAllHub.put(clazz, refs);
			LOG.finer("total for class=" + clazz + " is now " + refs.length);
		}
	}
	
	/**
	 * Unregisters the specified Hub from the list of “select all” Hubs for
	 * its object class. If the Hub is the only entry, the class is removed
	 * entirely from the registry.
	 *
	 * @param hub the Hub to remove from the select-all list
	 */
	public void removeSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		if (clazz == null) {
			return;
		}

		synchronized (hmCacheSelectAllHub) {
			WeakReference[] refs = (WeakReference[]) hmCacheSelectAllHub.get(clazz);
			if (refs == null) return;
			
			for (int i = 0; i < refs.length; i++) {
				Hub h = (Hub) refs[i].get();
				if (h == hub) {
					if (refs.length == 1) {
						hmCacheSelectAllHub.remove(clazz);
						LOG.fine("total for class=" + clazz + " is now 0");
					} else {
						WeakReference[] refNew = removeSelectAllHubs(refs, refs[i]);
						hmCacheSelectAllHub.put(clazz, refNew);
						LOG.finer("total for class=" + clazz + " is now " + refNew.length);
					}
				}
			}
		}
	}
	
	/**
	 * Removes all registered “select all” Hubs across all classes. The
	 * underlying map is cleared, removing all weak references to Hubs.
	 */
	public void removeAllSelectAllHubs() {
		synchronized (hmCacheSelectAllHub) {
			hmCacheSelectAllHub.clear();
		}
	}

	/**
	 * Stores the specified Hub under a global name using a weak reference.
	 * The name is treated case-insensitively. If either argument is null,
	 * the call is ignored.
	 *
	 * @param name the reference name (case-insensitive)
	 * @param hub  the Hub to associate with the name
	 */
	public void setNamedHub(String name, Hub<? extends OAObject> hub) {
		LOG.fine("Hub=" + hub + ", name=" + name);
		if (name == null || hub == null) {
			return;
		}
		synchronized (hmCacheNamedHub) {
			hmCacheNamedHub.put(name.toUpperCase(), new WeakReference(hub));
			LOG.fine("total named Hubs is now =" + hmCacheNamedHub.size());
		}
	}
	
	/**
	 * Retrieves a Hub previously stored under the given name. The lookup
	 * is case-insensitive. If the weak reference has been cleared, the
	 * entry is removed and {@code null} is returned.
	 *
	 * @param name the name of the Hub to retrieve (case-insensitive)
	 * @return the Hub associated with the name, or {@code null} if not found
	 */
	public Hub getNamedHub(String name) {
		//LOG.finer("Name="+name);
		if (name == null) {
			return null;
		}
		
		Hub hub = null;
		synchronized (hmCacheNamedHub) {
			WeakReference ref = (WeakReference) hmCacheNamedHub.get(name.toUpperCase());
			if (ref != null) {
				hub = (Hub) ref.get();
				if (hub == null) {
					hmCacheNamedHub.remove(name.toUpperCase());
				}
			}
		}
		return hub;
	}
	
	/**
	 * Registers a cache listener for the specified class. The listener will
	 * receive events such as afterAdd, afterRemove, and afterPropertyChange
	 * for objects of that class. Duplicate registrations are ignored.
	 *
	 * @param clazz the class whose events the listener should receive
	 * @param l     the listener to register
	 */
	public <T extends OAObject> void addListener(final Class<T> clazz, final OAObjectCacheListener<T> l) {
		LOG.fine("class=" + clazz);
		List alListener = hmCacheListener.computeIfAbsent(clazz, k -> new ArrayList<>());
		
		synchronized (alListener) {
			if (!alListener.contains(l)) {
				aiListenerCount.incrementAndGet();
				alListener.add(l);
				LOG.fine("total listeners=" + aiListenerCount.get());
			}
		}
	}
	


	/**
	 * Clears all cache data, listeners, select-all Hubs, and named Hubs.
	 * This operation is permitted only when unit test mode is enabled;
	 * otherwise, a {@link RuntimeException} is thrown.
	 *
	 * @throws RuntimeException if unit test mode is not enabled
	 */
	protected void resetCache() {
		objectCache.clearCache();
		hmCacheListener.clear();
		aiListenerCount.set(0);
		synchronized (hmCacheSelectAllHub) {
			hmCacheSelectAllHub.clear();
		}
		synchronized (hmCacheNamedHub) {
			hmCacheNamedHub.clear();
		}
	}

	/**
	 * Unregisters the specified listener for the given class. If the listener
	 * is found and removed, the global listener count is decremented.
	 *
	 * @param clazz the class whose listener list should be modified
	 * @param l     the listener to remove
	 */
	public void removeListener(Class clazz, OAObjectCacheListener l) {
		LOG.fine("class=" + clazz);
		if (clazz == null || l == null) return;
	
		List alListener = hmCacheListener.get(clazz);
		if (alListener != null) {
			synchronized (alListener) {
				if (alListener.remove(l)) {
					aiListenerCount.decrementAndGet();
					LOG.fine("total listeners=" + aiListenerCount.get());
				}
			}
		}
	}
	
	/**
	 * Returns all registered cache listeners for the specified class.
	 * If no listeners are registered globally or for the class, this
	 * method returns {@code null}. The returned array is a snapshot of
	 * the current listeners.
	 *
	 * @param c the class whose listeners should be retrieved
	 * @return an array of listeners for the class, or {@code null} if none exist
	 */
	@SuppressWarnings("unchecked")
	public <T extends OAObject> OAObjectCacheListener<T>[] getListeners(final Class<T> c) {
		if (c == null || aiListenerCount.get() == 0) {
			return null;
		}
	
	    List<? extends OAObjectCacheListener<?>> alListener = hmCacheListener.get(c);
		if (alListener == null) {
			return null;
		}

		OAObjectCacheListener<T>[] listeners = null;
		synchronized (alListener) {
			int x = alListener.size();
			listeners = (OAObjectCacheListener<T>[]) new OAObjectCacheListener<?>[x];
			
			for (int i = 0; i < x; i++) {
				listeners[i] =  (OAObjectCacheListener<T>) alListener.get(i);
			}
		}
		return listeners;
	}
	
	/**
	 * Notifies all registered cache listeners for the object's class that a
	 * property value has changed. The event is sent only if listener count is
	 * nonzero and {@code bSendEvent} is {@code true}. The original and new
	 * values are forwarded for listener handling.
	 *
	 * @param obj          the object whose property changed
	 * @param origKey      the original object key (may include old primary key values)
	 * @param propertyName the name of the changed property
	 * @param oldValue     the prior value of the property
	 * @param newValue     the new value of the property
	 * @param bLocalOnly   unused indicator for local-only routing
	 * @param bSendEvent   whether to dispatch the event to listeners
	 */
	public void fireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue,
			boolean bLocalOnly, boolean bSendEvent) {
		// Note: oldValue could be OAObjectKey, but will be resolved when HubEvent.getOldValue() is called
		if (aiListenerCount.get() == 0) {
			return;
		}
		if (obj == null || propertyName == null) {
			return;
		}
		if (bSendEvent) {
			// LOG.finest("object="+obj+", propertyName="+propertyName+", key="+origKey);
			OAObjectCacheListener[] hl = getListeners(obj.getClass());
			if (hl != null && hl.length > 0) {
				for (int i = 0; i < hl.length; i++) {
					hl[i].afterPropertyChange(obj, propertyName, oldValue, newValue);
				}
			}
		}
	}

	/**
	 * Sends an after-load event to all registered listeners for the object's class.
	 * The event is triggered only if listeners exist. Each listener's
	 * {@code afterLoad} method is invoked.
	 *
	 * @param obj the object that has just been loaded
	 */
	public <T extends OAObject> void fireAfterLoadEvent(T obj) {
		if (obj == null) return;
		if (aiListenerCount.get() == 0) return;

		final OAObjectCacheListener<T>[] hl = getListeners((Class<T>) obj.getClass());
		if (hl == null) return;
		final int x = hl.length;
		if (x > 0) {
			for (int i = 0; i < x; i++) {
				hl[i].afterLoad(obj);
			}
		}
	}

	/**
	 * Sends an after-add event to all registered listeners for the object's class.
	 * The event is dispatched only if listeners exist and both the Hub and object
	 * are non-null. Each listener's {@code afterAdd(Hub, T)} method is invoked.
	 *
	 * @param hub the Hub to which the object was added
	 * @param obj the object that was added
	 */
	public <T extends OAObject> void fireAfterAddEvent(Hub<T> hub, T obj) {
		if (hub == null || obj == null) return;
		if (aiListenerCount.get() == 0) return;

		final OAObjectCacheListener<T>[] hl = getListeners((Class<T>) obj.getClass());
		if (hl == null) return;
		final int x = hl.length;
		if (x > 0) {
			// LOG.finest("Hub="+thisHub+", object="+obj);
			for (int i = 0; i < x; i++) {
				hl[i].afterAdd(hub, obj);
			}
		}
	}
	
	/**
	 * Sends an after-remove event to all registered listeners for the object's class.
	 * The event is dispatched only if listeners exist and both the Hub and object
	 * are non-null. Each listener's {@code afterRemove(Hub, T)} method is invoked.
	 *
	 * @param hub the Hub from which the object was removed
	 * @param obj the object that was removed
	 */
	public <T extends OAObject> void fireAfterRemoveEvent(Hub<T> hub, T obj) {
		if (hub == null || obj == null) return;
		if (aiListenerCount.get() == 0) return;

		final OAObjectCacheListener<T>[] hl = getListeners((Class<T>) obj.getClass());
		if (hl == null) return;

		final int x = hl.length;
		if (x > 0) {
			for (int i = 0; i < x; i++) {
				hl[i].afterRemove(hub, obj);
			}
		}
	}

	/**
	 * Removes all objects from the object cache across all OAObject classes.
	 * Each class registered in the cache is cleared in turn. This does not
	 * affect listeners or select-all/named Hub registrations.
	 */
	public void removeAllObjects() {
		LOG.warning("removing all Objects was called (fyi only)");
		for (Class c : objectCache.getClasses()) {
			removeAllObjects(c);
		}
	}
	
	/**
	 * Removes all cached objects for the specified class. This clears only
	 * the cache entries for the class and does not affect listeners or other
	 * cache metadata.
	 *
	 * @param c the class whose cached objects should be removed
	 */
	public void removeAllObjects(Class c) {
		LOG.warning(String.format("removing all Objects for class=%s was called (fyi only)", c.getSimpleName()));
		objectCache.clearCache(c);
	}

	/**
	 * Visits all cached objects across all classes by invoking the specified
	 * {@link OACallback}. This is a convenience method that delegates to
	 * {@link #visit(OACallback)}.
	 *
	 * @param callback the callback to be invoked for each cached object
	 */
	public void callback(OACallback callback) {
		visit(callback);
	}
	
	/**
	 * Visits every cached object across all OAObject classes by delegating
	 * to the underlying {@link OAObjectCache}. Each object is passed to the
	 * supplied {@link OACallback}.
	 *
	 * @param callback the callback invoked for each cached object
	 */
	public void visit(OACallback callback) {
		LOG.fine("visit");
		objectCache.visit(callback);
	}
	
	/**
	 * Invokes the specified {@link OACallback} for every cached object of
	 * the given class. This is a convenience wrapper that delegates to
	 * {@link #visit(Class, OACallback)}.
	 *
	 * @param clazz    the OAObject class whose cached instances should be processed
	 * @param callback the callback to invoke for each object
	 */
	public void callback(Class<? extends OAObject> clazz, OACallback callback) {
		objectCache.visit(clazz, callback);
	}

	/**
	 * Visits all cached objects of the specified class by delegating to the
	 * underlying {@link OAObjectCache}. Objects are passed to the supplied
	 * {@link OACallback}.
	 *
	 * @param clazz    the OAObject class to visit
	 * @param callback the callback invoked for each object
	 */
	public void visit(Class clazz, OACallback callback) {
		objectCache.visit(clazz, callback);
	}
	
	/**
	 * Convenience wrapper that invokes the specified {@link OACallback} for
	 * every cached object of the given class. This delegates directly to
	 * {@link #callback(Class, OACallback)}.
	 *
	 * @param callback the callback to invoke for each cached object
	 * @param clazz    the OAObject class whose objects should be visited
	 */
	public void callback(OACallback callback, Class clazz) {
		objectCache.visit(clazz, callback);
	}
	
	/**
	 * Convenience wrapper that visits all cached objects of the specified
	 * class by delegating to {@link #visit(Class, OACallback)}. Each object
	 * is passed to the supplied {@link OACallback}.
	 *
	 * @param callback the callback invoked for each cached object
	 * @param clazz    the OAObject class whose cached instances should be visited
	 */
	public void visit(OACallback callback, Class clazz) {
		objectCache.visit(clazz, callback);
	}

	/**
	 * Populates the supplied list with cache summary information. The method
	 * adds a series of entries describing cache statistics, including:
	 * <ul>
	 *   <li>whether unit test mode is enabled</li>
	 *   <li>the total number of registered listeners</li>
	 *   <li>the count of select-all Hubs</li>
	 *   <li>the count of named Hubs</li>
	 * </ul>
	 *
	 * @param al the list to which cache information entries are added
	 */
	public void getInfo(List al) {
		List alx = getInfo();
		al.add(alx);
	}

	/**
	 * Returns all classes currently registered in the object cache. This is
	 * a convenience wrapper around the underlying {@link OAObjectCache}
	 * implementation.
	 *
	 * @return an array of OAObject classes known to the cache
	 */
	public Class[] getClasses() {
		return objectCache.getClasses();
	}
	
	/**
	 * Returns the number of cached objects for the specified class. This is a
	 * convenience wrapper that delegates to the underlying {@link OAObjectCache}.
	 *
	 * @param clazz the class whose cached object count is requested
	 * @return the number of cached objects for the class
	 */
	public int getTotal(Class clazz) {
		return objectCache.getTotal(clazz);
	}
	
	/**
	 * Returns a newly created list containing cache summary information.
	 * This method constructs the list, populates it using
	 * {@link #getInfo(List)}, and returns the populated result.
	 *
	 * @return a list containing cache summary information
	 */
	public List getInfo() {
		// LOG.finer("called");
		List<String> al = new ArrayList();
		al.add("ObjectCache Info --- ");

		Class[] cs = getClasses();
		if (cs == null) {
			return al;
		}
		int x = cs.length;

		int max = 0;
		for (int i = 0; i < x; i++) {
			max = Math.max(max, ((Class) cs[i]).getName().length());
		}
		String fmt = max + "L";

		/* this requires that the SizeOf -D property is set when starting
		long ll = SizeOf.sizeOf(OAObjectHashDelegate.hashCacheClass, true);
		vec.addElement(OAString.fmt("  SizeOf cache", fmt)+" "+OAString.format(ll,"#,##0"));
		*/

		for (int i = 0; i < x; i++) {
			al.add(OAString.format(cs[i].getName(), fmt) + " " + String.format("%,2d", getTotal(cs[i])));
		}
		al.add(OAString.fmt("TempHubs", fmt) + " " + HubTemp.getCount());
		Collections.sort(al);
		return al;
	}

	/**
	 * Sets the default add mode used by HubController operations for all threads
	 * that do not already have an assigned add mode. The mode controls how
	 * duplicate objects are handled when added to the cache.
	 *
	 * <p>If the supplied mode is outside the valid range {@code 0–4}, an
	 * {@link IllegalArgumentException} is thrown. Valid modes include
	 * {@link #NO_DUPS}, {@link #IGNORE_DUPS}, and {@link #IGNORE_ALL}.</p>
	 *
	 * @param mode the default add mode to assign
	 * @throws IllegalArgumentException if the mode is not between 0 and 4
	 */
	public void setDefaultAddMode(int mode) {
		LOG.config("default add mode=" + mode);
		if (mode > 4 || mode < 0) {
			throw new IllegalArgumentException("HubController.setDefaultAddMode() must be 0,1,2,3 or 4");
		}
		defaultAddMode = mode;
	}

	/**
	 * Returns the current default add mode used for threads that do not have a
	 * thread-local add mode assigned. This setting determines how duplicate
	 * objects are handled when added to the cache.
	 *
	 * @return the default add mode value
	 */
	public int getDefaultAddMode() {
		return defaultAddMode;
	}

	/**
	 * Clears all cached objects for the specified class by delegating to the
	 * underlying {@link OAObjectCache}. Only the cache entries for the given
	 * class are removed; listeners and other cache metadata are unaffected.
	 *
	 * @param clazz the class whose cached objects should be cleared
	 */
	public void clearCache(Class clazz) {
		objectCache.clearCache(clazz);
	}
	
	/**
	 * Adds the specified object to the cache using default behavior. This is a
	 * convenience wrapper that delegates to
	 * {@link #add(OAObject, boolean, boolean)} with duplicate-error checking
	 * disabled and automatic addition to select-all Hubs enabled.
	 *
	 * @param obj the object to add to the cache
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public OAObject add(OAObject obj) {
		return add(obj, false, true);
	}

	/**
	 * Adds the specified object to the cache with explicit control over duplicate
	 * handling and select-all Hub population. This method delegates to
	 * {@link #add(OAObject, boolean, boolean, boolean)} with event dispatching
	 * performed in the current thread.
	 *
	 * @param obj               the object to add to the cache
	 * @param bErrorIfExists    whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll   whether the object should be added to all select-all Hubs
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
		return add(obj, bErrorIfExists, bAddToSelectAll, false);
	}

	/**
	 * Adds the specified object to the cache with full control over duplicate
	 * handling, select-all Hub population, and whether after-add events are
	 * dispatched asynchronously in another thread. If caching is disabled,
	 * the supplied object is returned unchanged.
	 *
	 * @param obj                             the object to add to the cache
	 * @param bErrorIfExists                  whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll                 whether the object should be added to all select-all Hubs
	 * @param bSendAddEventInAnotherThread    whether after-add events should be queued for asynchronous dispatch
	 * @return the existing cached object if one matches, otherwise the supplied object
	 */
	public OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread) {
		if (bDisableCache) {
			return obj;
		}
		OAObject objx = _add(obj, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
		return objx;
	}

	/**
	 * Internal helper that creates the object's key and delegates to
	 * {@link #_add2(OAObject, OAObjectKey, boolean, boolean, boolean)} to
	 * perform the actual cache insertion logic. This method does not perform
	 * any additional validation beyond key creation.
	 *
	 * @param obj                             the object to add
	 * @param bErrorIfExists                  whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll                 whether to add the object to all select-all Hubs
	 * @param bSendAddEventInAnotherThread    whether after-add events should be dispatched asynchronously
	 * @return the existing cached object if found, otherwise the supplied object
	 */
	private OAObject _add(final OAObject obj, final boolean bErrorIfExists, boolean bAddToSelectAll,
			final boolean bSendAddEventInAnotherThread) {
		final OAObjectKey key = OAObjectKeyDelegate.createObjectKey(obj);
		OAObject objResult;

		objResult = _add2(obj, key, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
		return objResult;
	}
	
	/**
	 * Core internal method that inserts the object into the cache using the
	 * provided {@link OAObjectKey}. Handles duplicate checking based on the
	 * current add mode, updates the cache entry if necessary, optionally adds
	 * the object to select-all Hubs, and dispatches after-add events either
	 * synchronously or asynchronously.
	 *
	 * @param obj                             the object to add
	 * @param key                             the key representing the object's identity
	 * @param bErrorIfExists                  whether to throw an exception if a duplicate exists
	 * @param bAddToSelectAll                 whether to add the object to all select-all Hubs
	 * @param bSendAddEventInAnotherThread    whether after-add events should be dispatched asynchronously
	 * @return the existing cached object if found, otherwise the supplied object
	 * @throws RuntimeException if the object or key is null or if the key has an invalid GUID
	 */
	private OAObject _add2(final OAObject obj, final OAObjectKey key, final boolean bErrorIfExists, boolean bAddToSelectAll,
			final boolean bSendAddEventInAnotherThread) {

		if (obj == null) return null;
		if (key == null) {
			throw new RuntimeException("Adding to object cache without a key"); 
		}
		
		final Class clazz = obj.getClass();
		final long guid = key.getGuid();
		if (guid == 0L) {
			throw new RuntimeException("Adding to object cache without a valid key (guid!=0), key="+key); 
		}
		
		final OAObject objFound = objectCache.getObject(clazz, guid);

		boolean bSendAddEvent = false;
		final int mode = OARuntime.get().threadService().getObjectCacheAddMode();
		if (objFound == null) {
			if (mode != IGNORE_ALL) {
				objectCache.updateObject(obj, key, clazz);
				bSendAddEvent = true;
			}			
		}
		else {
			if (obj != objFound && mode == NO_DUPS) {
				if (bErrorIfExists) {
					throw new RuntimeException("OAObjectCacheDelegate.add() object already exists " + obj);
				}
			}
			else {
				objectCache.updateObject(obj, key, clazz);
			}
			bAddToSelectAll = false;
		}

		if (bAddToSelectAll) {
			Hub[] hs = getSelectAllHubs(obj.getClass());
			for (int i = 0; hs != null && i < hs.length; i++) {
				hs[i].add(obj);
			}
		}
		if (bSendAddEvent) {
			fireAfterAddEvent(obj, bSendAddEventInAnotherThread);
		}

		if (objFound != null) return objFound;
		return obj;
	}	

	/**
	 * Dispatches the after-add event to all registered cache listeners for the
	 * object's class. If {@code bSendAddEventInAnotherThread} is {@code true},
	 * the events are queued and processed asynchronously; otherwise, they are
	 * invoked immediately in the current thread. No action is taken if the
	 * listener count is zero or the object is null.
	 *
	 * @param obj                             the object that was added to the cache
	 * @param bSendAddEventInAnotherThread    whether to dispatch events asynchronously
	 */
	public <T extends OAObject> void fireAfterAddEvent(T obj, boolean bSendAddEventInAnotherThread) {
		if (obj == null) return;
		if (aiListenerCount.get() == 0) return;

		final OAObjectCacheListener<T>[] hls = getListeners((Class<T>) obj.getClass());
		if (hls == null) return;
		final int x = hls.length;
		if (x == 0) return;

		if (bSendAddEventInAnotherThread) {
			if (threadCacheSendAddEvent == null) {
				startCacheSendAddEventThread();
			}
			queCacheSendAddEvent.add(new SendAddEventInfo(hls, obj));
		} else {
			for (int i = 0; i < x; i++) {
				hls[i].afterAdd(obj);
			}
		}
	}

	/**
	 * Starts the background thread responsible for processing queued after-add
	 * events. If the thread is already running, this method returns immediately.
	 * The thread continuously takes events from the queue and invokes the
	 * {@code afterAdd} method on each listener. The thread is a daemon and
	 * runs indefinitely.
	 */
	protected synchronized void startCacheSendAddEventThread() {
		if (threadCacheSendAddEvent != null) {
			return;
		}
		threadCacheSendAddEvent = new Thread(new Runnable() {
			@Override
			public void run() {
				int cnt = 0;
				for (;;) {
					try {
						SendAddEventInfo se = queCacheSendAddEvent.take();
						for (OAObjectCacheListener hl : se.hls) {
							hl.afterAdd((OAObject) se.obj);
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}
		}, "OAObjectCacheService.SendAddEvent");
		threadCacheSendAddEvent.setDaemon(true);
		threadCacheSendAddEvent.start();
	}
	
	
	/**
	 * Adds the specified object to all registered select-all Hubs for its class.
	 * If a Hub already contains the object, it is skipped. This ensures that
	 * objects are automatically included in global views without creating duplicates.
	 *
	 * @param obj the object to add to all select-all Hubs
	 */
	public void addToSelectAllHubs(OAObject obj) {
		Hub[] hs = getSelectAllHubs(obj.getClass());
		for (int i = 0; hs != null && i < hs.length; i++) {
			LOG.finer("adding to selectAll Hub=" + hs[i]);
			if (!hs[i].contains(obj)) {
				hs[i].add(obj);
			}
		}
	}

	/**
	 * Notifies the cache that a key property value of the specified object has
	 * changed. The object cache is updated to reflect the new key value, ensuring
	 * that future lookups using the updated key will succeed. No action is taken
	 * if caching is disabled.
	 *
	 * @param obj the object whose key property has changed
	 */
	public void propertyKeyValueChanged(OAObject obj) {
		if (bDisableCache) return;
		objectCache.updateObject(obj);
	}

	/**
	 * Removes the specified object from the cache. This operation delegates
	 * directly to the underlying {@link OAObjectCache} and does not affect
	 * listeners or select-all Hub registrations.
	 *
	 * @param obj the object to remove from the cache
	 */
	public void removeObject(final OAObject obj) {
		objectCache.removeObject(obj);
	}
	
	/**
	 * Retrieves an object from the cache based on its object ID property value.
	 * This is a convenience wrapper around {@link #get(Class, Object)}.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param key   the object ID, an array of IDs, or an {@link OAObjectKey} representing the object
	 * @return the cached object matching the key, or {@code null} if not found
	 * @see OAObjectKey#OAObjectKey
	 * @see OAObject#equals
	 */
	public <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		return get(clazz, key);
	}

	/**
	 * Retrieves an object from the cache using an integer ID. This method
	 * delegates to {@link #get(Class, Object)} by wrapping the integer in
	 * an {@link Integer} object.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param id    the integer ID of the object
	 * @return the cached object matching the ID, or {@code null} if not found
	 */
	public <T extends OAObject> T get(Class<T> clazz, int id) {
		return get(clazz, Integer.valueOf(id));
	}

	/**
	 * Retrieves an object from the cache based on the provided key. If the key
	 * is not an {@link OAObjectKey}, it is converted appropriately. Delegates
	 * to {@link #get(Class, OAObjectKey)} for the final retrieval.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param key   the object, key value, array of key values, or {@link OAObjectKey}
	 * @return the cached object matching the key, or {@code null} if not found
	 */
	public <T extends OAObject> T get(Class<T> clazz, Object key) {
		if (!(key instanceof OAObjectKey)) {
			if (key instanceof OAObject) {
				key = OAObjectKeyDelegate.getKey((OAObject) key);
			} else {
				key = OAObjectKeyDelegate.createObjectKey(clazz, key);
			}
		}
		OAObject obj = null;
		final OAObjectKey ok = (OAObjectKey) key;
		return get(clazz, ok);
	}

	/**
	 * Retrieves an object from the cache using its {@link OAObjectKey}. If either
	 * the class or key is null, {@code null} is returned. Delegates directly
	 * to the underlying {@link OAObjectCache} to fetch the object.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param ok    the {@link OAObjectKey} representing the object's identity
	 * @return the cached object matching the key, or {@code null} if not found
	 */
	public <T extends OAObject> T get(Class<T> clazz, OAObjectKey ok) {
		if (clazz == null || ok == null) return null;
		OAObject obj = objectCache.getObject(clazz, ok); 
		return (T) obj;
	}
	
	//qqqqqq remove this method ??
	/**
	 * Retrieves an object from the cache by its GUID. This method returns the
	 * object if it exists in the cache; it does not create a new instance.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param guid  the globally unique identifier of the object
	 * @return the cached object matching the GUID, or {@code null} if not found
	 */
	public <T extends OAObject> T getNewObjectUsingGuid(Class<T> clazz, long guid) {
		Object obj = objectCache.getObject((Class<OAObject>) clazz, guid); 
		return (T) obj;
	}

	/**
	 * Retrieves an object from the cache based on its GUID. Delegates directly
	 * to the underlying {@link OAObjectCache} for the lookup.
	 *
	 * @param clazz the class of the object to retrieve
	 * @param guid  the globally unique identifier of the object
	 * @return the cached object matching the GUID, or {@code null} if not found
	 */
	public <T extends OAObject> T getUsingGuid(Class<T> clazz, long guid) {
		Object obj = objectCache.getObject(clazz, guid); 
		return (T) obj;
	}

	/**
	 * Retrieves the cached instance of the specified object based on its
	 * current key values. If caching is disabled or the object is null,
	 * {@code null} is returned.
	 *
	 * @param obj the object whose cached instance is requested
	 * @return the cached object matching the key, or {@code null} if not found or caching is disabled
	 */
	public Object get(OAObject obj) {
		if (bDisableCache) {
			return null;
		}
		if (obj == null) {
			return null;
		}
		return get(obj.getClass(), OAObjectKeyDelegate.getKey((OAObject) obj));
	}

	/**
	 * Finds the next object in the cache following the specified object.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * method with default parameters.
	 *
	 * @param fromObject the object from which to start the search; if null, search starts at the beginning
	 * @return the next object in the cache, or {@code null} if none found
	 */
	public Object findNext(Object fromObject) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), null, null, false, true);
	}
	/**
	 * Finds the next object in the cache after {@code fromObject} that matches
	 * the specified property path and value. Delegates to the internal
	 * {@link #_find(Object, Class, String, Object, boolean, boolean)} method
	 * with default skip-new and exception behavior.
	 *
	 * @param fromObject   the object from which to start the search; if null, search starts at the beginning
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public Object findNext(Object fromObject, String propertyPath, Object findObject) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), propertyPath, findObject, false, true);
	}
	
	/**
	 * Finds the next object in the cache after {@code fromObject} that matches
	 * the specified property path and value. Allows control over whether new
	 * objects should be skipped and whether exceptions should be thrown if
	 * no match is found. Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}.
	 *
	 * @param fromObject     the object from which to start the search; if null, search starts at the beginning
	 * @param propertyPath   the property path to match
	 * @param findObject     the value to compare against
	 * @param bSkipNew       whether newly added (unsaved) objects should be skipped
	 * @param bThrowException whether to throw an exception if no matching object is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public Object findNext(Object fromObject, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), propertyPath, findObject, bSkipNew, bThrowException);
	}

	/**
	 * Finds the next object in the cache of the specified class after
	 * {@code fromObject} that matches the given property path and value.
	 * If {@code fromClass} is null, the class of {@code fromObject} is used.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}.
	 *
	 * @param fromObject   the object from which to start the search; if null, search starts at the beginning
	 * @param fromClass    the class of objects to search; if null, the class of {@code fromObject} is used
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public Object findNext(Object fromObject, Class fromClass, String propertyPath, Object findObject) {
		if (fromObject == null && fromClass == null) {
			return null;
		}
		if (fromClass == null) {
			fromClass = fromObject.getClass();
		}
		return _find(fromObject, fromClass, propertyPath, findObject, false, true);
	}
	
	/**
	 * Searches the cache for any object of the specified class. Delegates to
	 * the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * method with default parameters.
	 *
	 * @param clazz the class of objects to search
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public Object find(Class clazz) {
		return _find(null, clazz, null, null, false, true);
	}

	/**
	 * Searches the cache for an object of the specified class that satisfies
	 * the given {@link OAFinder}. Delegates to the internal
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean)} method with
	 * default skip-new and exception behavior.
	 *
	 * @param clazz  the class of objects to search
	 * @param finder the finder specifying the search criteria
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public Object find(Class clazz, OAFinder finder) {
		return _find(null, clazz, finder, false, true);
	}

	/**
	 * Searches the cache for an object of the specified class where the
	 * property at {@code propertyPath} equals {@code findObject}. Delegates
	 * to the internal {@link #_find(Object, Class, String, Object, boolean, boolean)}
	 * with default skip-new and exception behavior.
	 *
	 * @param clazz        the class of objects to search
	 * @param propertyPath the property path to match
	 * @param findObject   the value to compare against
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public Object find(Class clazz, String propertyPath, Object findObject) {
		return _find(null, clazz, propertyPath, findObject, false, true);
	}
	
	/**
	 * Searches the cache for an object of the specified class where the
	 * property at {@code propertyPath} equals {@code findObject}, with
	 * control over skipping new objects and throwing exceptions if no match
	 * is found. Delegates to the internal
	 * {@link #_find(Object, Class, String, Object, boolean, boolean)} method.
	 *
	 * @param clazz         the class of objects to search
	 * @param propertyPath  the property path to match
	 * @param findObject    the value to compare against
	 * @param bSkipNew      whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public Object find(Class clazz, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		return _find(null, clazz, propertyPath, findObject, bSkipNew, bThrowException);
	}

	/**
	 * Searches the cache for an object of the specified class that satisfies
	 * the given {@link OAFinder}, with control over skipping new objects and
	 * throwing exceptions if no match is found. Delegates to the internal
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean)} method.
	 *
	 * @param clazz          the class of objects to search
	 * @param finder         the finder specifying the search criteria
	 * @param bSkipNew       whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the first matching object in the cache, or {@code null} if none found
	 */
	public Object find(Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		return _find(null, clazz, finder, false, true); 
		//qqqqqqqqqqqq not using bThrowException ??
	}

	/**
	 * Internal helper that searches the cache for an object of the specified class
	 * matching the given property path and value. Delegates to the more detailed
	 * {@link #_find(Object, Class, String, Object, boolean, boolean, int, List)}
	 * method with default fetch amount and result list.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param propertyPath    the property path to match; may be null
	 * @param findObject      the value to compare against
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public Object _find(Object fromObject, Class clazz, String propertyPath, Object findObject, boolean bSkipNew,
			boolean bThrowException) {
		return _find(fromObject, clazz, propertyPath, findObject, bSkipNew, bThrowException, 1, null);
	}
	
	/**
	 * Internal helper that searches the cache for an object of the specified class
	 * using the provided {@link OAFinder}. Delegates to the more detailed
	 * {@link #_find(Object, Class, OAFinder, boolean, boolean, int, List)} method
	 * with default fetch amount and result list.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param finder          the finder specifying search criteria
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @return the next matching object in the cache, or {@code null} if none found
	 */
	public Object _find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, 1, null);
	}

	public Object find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}
	
	/**
	 * Searches the cache for objects of the specified class that satisfy the given
	 * {@link OAFilter}. Converts the filter into an {@link OAFinder} internally
	 * and delegates to the core {@link #_find(Object, Class, OAFinder, boolean, boolean, int, List)} method.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param filter          the filter specifying search criteria; may be null
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @param fetchAmount     maximum number of objects to return
	 * @param alResults       list to accumulate found objects; may be null
	 * @return the last matching object found, or {@code null} if none
	 */
	public Object find(Object fromObject, Class clazz, OAFilter filter, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		OAFinder finder = new OAFinder();
		if (filter != null) {
			finder.addFilter(filter);
		}
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	/**
	 * Searches the cache for objects of the specified class starting from
	 * {@code fromObject}, returning up to {@code fetchAmount} results.
	 * Delegates to the internal {@link #_find(Object, Class, String, Object, boolean, boolean, int, List)}
	 * method with default search parameters.
	 *
	 * @param fromObject  the object from which to start the search; may be null
	 * @param clazz       the class of objects to search
	 * @param fetchAmount maximum number of objects to return
	 * @param alResults   list to accumulate found objects; may be null
	 * @return the last object found, or {@code null} if none
	 */
	public Object find(Object fromObject, Class clazz, int fetchAmount, List<OAObject> alResults) {
		return _find(fromObject, clazz, null, false, false, fetchAmount, alResults);
	}
	
	/**
	 * Internal method that searches the cache for objects of the specified class
	 * matching the given property path and value. Supports skipping new objects,
	 * throwing exceptions if no match is found, limiting the number of results,
	 * and accumulating results into a provided list. Converts property paths
	 * into an appropriate {@link OAFinder} internally when necessary.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search; must not be null
	 * @param propertyPath    the property path to match; may be null
	 * @param findValue       the value to compare against
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no matching object is found
	 * @param fetchAmount     maximum number of objects to retrieve
	 * @param alResults       list to accumulate found objects; may be null
	 * @return the last object found in the cache, or {@code null} if none
	 * @throws IllegalArgumentException if clazz is null or findValue is invalid
	 */
	protected Object _find(Object fromObject, Class clazz, String propertyPath, Object findValue, boolean bSkipNew,
			boolean bThrowException, int fetchAmount, List<OAObject> alResults) {
		if (bDisableCache) {
			return null;
		}
		// LOG.fine("class="+clazz+", propertyPath="+propertyPath+" findObject="+findObject+", bSkipNew="+bSkipNew);
		if (propertyPath == null || propertyPath.length() == 0) {
			propertyPath = null;
			// throw new IllegalArgumentException("HubController.find() property cant be null");
		}
		if (clazz == null) {
			throw new IllegalArgumentException("HubController.find() class cant be null");
		}

		if (findValue instanceof Hub) {
			throw new IllegalArgumentException(
					"findValue can not be a Hub, class=" + clazz.getSimpleName() + ", propertyPath=" + propertyPath);
		}

		// 20140201 replace methods with finder
		OAFinder finder;
		OAFilter filter = null;
		if (!OAString.isEmpty(propertyPath)) {
			OAPropertyPath pp = new OAPropertyPath(clazz, propertyPath);
			FinderInfo fi;
			try {
				fi = OAFilterDelegate.createFinder(clazz, pp);
			} catch (Exception e) {
				throw new RuntimeException("find error with propertyPath", e);
			}

			if (fi != null) {
				finder = fi.finder;
				filter = new OAEqualFilter(fi.pp, findValue);
				((OAEqualFilter) filter).setIgnoreCase(true);
			} else {
				finder = new OAFinder();
				filter = new OAEqualFilter(pp, findValue);
				((OAEqualFilter) filter).setIgnoreCase(true);
			}
		} else {
			finder = new OAFinder();
			if (findValue != null) {
				filter = new OAEqualFilter((String) null, findValue);
				((OAEqualFilter) filter).setIgnoreCase(true);
			}
		}
		if (filter != null) {
			finder.addFilter(filter);
		}
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	/**
	 * Internal method that searches the cache for objects of the specified class
	 * using the provided {@link OAFinder}. Supports skipping new objects,
	 * exception handling, limiting the number of results, and accumulating
	 * matches into a provided list. Delegates directly to the underlying
	 * {@link OAObjectCache} for execution.
	 *
	 * @param fromObject      the object from which to start the search; may be null
	 * @param clazz           the class of objects to search
	 * @param finder          the finder specifying search criteria
	 * @param bSkipNew        whether to skip newly added (unsaved) objects
	 * @param bThrowException whether to throw an exception if no match is found
	 * @param fetchAmount     maximum number of objects to retrieve
	 * @param alResults       list to accumulate found objects; may be null
	 * @return the last object found, or {@code null} if none
	 */
	protected Object _find(final Object fromObject, final Class<? extends OAObject> clazz, final OAFinder finder, final boolean bSkipNew,
			final boolean bThrowException, int fetchAmount, final List<OAObject> alResults) {
		if (bDisableCache) {
			return null;
		}
		return objectCache.find(fromObject, clazz, finder, bSkipNew, fetchAmount, alResults);
	}	

	/**
	 * Refreshes all objects of the specified class from the underlying
	 * {@link OADataSource}. If called on a client, the refresh is performed
	 * asynchronously on the server. Updates all relevant Hubs for the class.
	 * Objects already present in select-all or detail Hubs are checked to
	 * determine if they require refresh.
	 *
	 * @param clazz the class of objects to refresh; if null, no action is taken
	 */
	public void refresh(Class clazz) {
		if (clazz == null) {
			return;
		}
		LOG.fine("refreshing " + clazz.getSimpleName());

		if (!OASyncDelegate.isServer(clazz)) {
			OASyncDelegate.getRemoteServer(clazz).refreshCache(clazz);
			LOG.fine("refreshing " + clazz.getSimpleName() + " will be ran on the server");
			return;
		}
		final Set<Hub> hsHub = new HashSet<Hub>();

		OADataSource ds = OADataSource.getDataSource(clazz);
		if (ds == null) {
			return;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		OADataSourceObjectCache dsCache = new OADataSourceObjectCache(false);
		Iterator it = dsCache.select(clazz);

		int cntTotal = 0;
		int cntAlone = 0;
		for (; it.hasNext(); cntTotal++) {
			OAObject obj = (OAObject) it.next();
			Hub[] hubs = OAObjectHubDelegate.getHubReferences(obj);

			boolean bNeedsRefreshed = true;
			if (hubs != null) {
				for (Hub h : hubs) {
					if (h == null) {
						continue;
					}
					if (h.getSelect() == null) {
						if (h.getMasterObject() == null) {
							continue;
						}
						OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(h);
						if (li != null) {
							li = li.getReverseLinkInfo();
							if (li == null || li.getCalculated()) {
								continue;
							}
							if (li.getPrivateMethod()) {
								continue;
							}
						}
					}
					bNeedsRefreshed = false;
					if (!hsHub.contains(h)) {
						hsHub.add(h);
					}
				}
			}

			if (bNeedsRefreshed) {
				OAObjectKey key = OAObjectKeyDelegate.getKey(obj);
				ds.getObject(oi, clazz, key, true);
				cntAlone++;
				continue;
			}
		}

		int cntHubs = 0;
		int cntInHubs = 0;
		for (Hub h : hsHub) {
			HubSelectDelegate.refreshSelect(h);
			cntHubs++;
			cntInHubs += h.getSize();
		}
		dsCache.close();
		LOG.fine(String.format(	"refreshed %s, total=%d, alongCnt=%d, hubCnt=%d, inHubsCnt=%d",
								clazz.getSimpleName(), cntTotal, cntAlone, cntHubs, cntInHubs));
	}

	
	/**
	 * Returns the underlying {@link OAObjectCache} instance used by this delegate.
	 * This provides direct access to low-level cache operations for advanced use cases.
	 *
	 * @return the global {@link OAObjectCache} instance
	 */
	public OAObjectCache getOAObjectCache() {
		return objectCache;
	}
	
	
	/**
	 * Enables or disables the object cache globally. When caching is disabled,
	 * methods that would normally retrieve or store objects in the cache will
	 * bypass it, effectively returning objects directly without caching.
	 *
	 * @param b {@code true} to disable caching, {@code false} to enable it
	 */
	public void setDisableCache(boolean b) {
		bDisableCache = b;
		//qqqqqq not fully used
	}
	
	
	

}
