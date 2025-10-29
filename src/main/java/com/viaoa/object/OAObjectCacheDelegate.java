/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

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
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubTemp;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;

/**
 * Cache for OAObjects.  
 * 
 */
public class OAObjectCacheDelegate {
	private static final Logger LOG = Logger.getLogger(OAObjectCacheDelegate.class.getName());

	private static final Map<Class<? extends OAObject>, List<OAObjectCacheListener<? extends OAObject>>> hmCacheListener = new ConcurrentHashMap<>();
    private static final Map<Class<? extends OAObject>, WeakReference<Hub<? extends OAObject>>[]> hmCacheSelectAllHub = new HashMap<>(37,.75F);
    private static final Map<String, WeakReference<Hub<? extends OAObject>>> hmCacheNamedHub = new HashMap<>(29,.75F);
	
	private static final AtomicInteger aiListenerCount = new AtomicInteger();
	private static boolean bDisableCache = false;
    
	private static final LinkedBlockingQueue<SendAddEventInfo> queCacheSendAddEvent = new LinkedBlockingQueue<>();
	private static volatile Thread threadCacheSendAddEvent;

	private static final OAObjectCache objectCache = new OAObjectCache();
    
	protected static int DefaultAddMode = 1;

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

	/**
	 * Automatically set by Hub.select() when a select is done without a where clause. A WeakReference is used for storage. When a new
	 * OAObject is created, it will be added to a SelectAllHub.
	 */
	public static Hub[] getSelectAllHubs(Class clazz) {
		
		if (clazz == null) {
			return null;
		}
		synchronized (OAObjectCacheDelegate.hmCacheSelectAllHub) {
			WeakReference<Hub<? extends OAObject>>[] refs = OAObjectCacheDelegate.hmCacheSelectAllHub.get(clazz);
			if (refs == null) return null;
			Hub[] hubs = new Hub[refs.length];
			for (int i = 0; i < refs.length; i++) {
				hubs[i] = (Hub) refs[i].get();
				if (hubs[i] == null) {
					if (refs.length == 1) {
						OAObjectCacheDelegate.hmCacheSelectAllHub.remove(clazz);
						return null;
					} else {
						OAObjectCacheDelegate.hmCacheSelectAllHub.put(clazz, removeSelectAllHubs(refs, refs[i]));
						return getSelectAllHubs(clazz);
					}
				}
			}
			return hubs;
		}
	}

	/** returns first hub from getSelectAllHubs() */
	public static Hub getSelectAllHub(Class clazz) {
		Hub[] hs = getSelectAllHubs(clazz);
		if (hs != null && hs.length > 0) {
			return hs[0];
		}
		return null;
	}

	private static WeakReference[] removeSelectAllHubs(WeakReference[] refs, WeakReference refRemove) {
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
	 * Used by Hub.select() to register a Hub that has all data selected.
	 *
	 * @since 2007/08/16
	 */
	public static void setSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		LOG.fine("Hub.objectClass = " + clazz);

		synchronized (OAObjectCacheDelegate.hmCacheSelectAllHub) {
			WeakReference[] refs = (WeakReference[]) OAObjectCacheDelegate.hmCacheSelectAllHub.get(clazz);
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
			OAObjectCacheDelegate.hmCacheSelectAllHub.put(clazz, refs);
			LOG.finer("total for class=" + clazz + " is now " + refs.length);
		}
	}

	/**
	 * Used by Hub to unregister a Hub that had all data selected.
	 *
	 * @since 2007/08/16
	 */
	public static void removeSelectAllHub(Hub hub) {
		if (hub == null) {
			return;
		}
		Class clazz = hub.getObjectClass();
		if (clazz == null) {
			return;
		}

		synchronized (OAObjectCacheDelegate.hmCacheSelectAllHub) {
			WeakReference[] refs = (WeakReference[]) OAObjectCacheDelegate.hmCacheSelectAllHub.get(clazz);
			if (refs == null) return;
			
			for (int i = 0; i < refs.length; i++) {
				Hub h = (Hub) refs[i].get();
				if (h == hub) {
					if (refs.length == 1) {
						OAObjectCacheDelegate.hmCacheSelectAllHub.remove(clazz);
						LOG.fine("total for class=" + clazz + " is now 0");
					} else {
						WeakReference[] refNew = removeSelectAllHubs(refs, refs[i]);
						OAObjectCacheDelegate.hmCacheSelectAllHub.put(clazz, refNew);
						LOG.finer("total for class=" + clazz + " is now " + refNew.length);
					}
				}
			}
		}
	}

	public static void removeAllSelectAllHubs() {
		synchronized (OAObjectCacheDelegate.hmCacheSelectAllHub) {
			OAObjectCacheDelegate.hmCacheSelectAllHub.clear();
		}
	}

	/**
	 * Used to store a global hub by name, using a WeakReference.
	 *
	 * @param name reference name to use, not case-sensitive
	 */
	static public void setNamedHub(String name, Hub<? extends OAObject> hub) {
		LOG.fine("Hub=" + hub + ", name=" + name);
		if (name == null || hub == null) {
			return;
		}
		synchronized (OAObjectCacheDelegate.hmCacheNamedHub) {
			OAObjectCacheDelegate.hmCacheNamedHub.put(name.toUpperCase(), new WeakReference(hub));
			LOG.fine("total named Hubs is now =" + OAObjectCacheDelegate.hmCacheNamedHub.size());
		}
	}

	/**
	 * Gets a hub that is stored by name.
	 *
	 * @param name reference name to use, not case-sensitive
	 * @return if found then Hub, else null.
	 */
	public static Hub getNamedHub(String name) {
		//LOG.finer("Name="+name);
		if (name == null) {
			return null;
		}
		
		Hub hub = null;
		synchronized (OAObjectCacheDelegate.hmCacheNamedHub) {
			WeakReference ref = (WeakReference) OAObjectCacheDelegate.hmCacheNamedHub.get(name.toUpperCase());
			if (ref != null) {
				hub = (Hub) ref.get();
				if (hub == null) {
					OAObjectCacheDelegate.hmCacheNamedHub.remove(name.toUpperCase());
				}
			}
		}
		return hub;
	}


	/**
	 * Listeners support for HubEvents.
	 * <p>
	 * The following events are sent:<br>
	 * Events from Hubs: afterAdd, afterRemove<br>
	 * Events from OAObjects: afterPropertyChange
	 */
	public static <T extends OAObject> void addListener(final Class<T> clazz, final OAObjectCacheListener<T> l) {
		LOG.fine("class=" + clazz);
		List alListener = OAObjectCacheDelegate.hmCacheListener.computeIfAbsent(clazz, k -> new ArrayList<>());
		
		synchronized (alListener) {
			if (!alListener.contains(l)) {
				aiListenerCount.incrementAndGet();
				alListener.add(l);
				LOG.fine("total listeners=" + aiListenerCount.get());
			}
		}
	}

	private static boolean UnitTestMode;

	/**
	 * Flag to allow system to be running in test mode This is used by {@link #resetCache()}
	 */
	public static void setUnitTestMode(boolean b) {
		UnitTestMode = b;
	}

	/**
	 * Clear out object cache, remove all listeners, remove all selectAllHubs, remove all named hubs.
	 * <p>
	 * NOTE: this can only be used if UnitTestMode=true
	 *
	 * @see #setUnitTestMode must be true, else an Exception is thrown
	 * @see #removeAllObjects()
	 * @see #clearCache(Class)
	 */
	public static void resetCache() {
		LOG.warning("call to reset cache, UnitTestMode=" + UnitTestMode);
		if (!UnitTestMode) {
			throw new RuntimeException("Can only call reset cache if UnitTestMode is true");
		}

		objectCache.clearCache();
		OAObjectCacheDelegate.hmCacheListener.clear();
		aiListenerCount.set(0);
		synchronized (OAObjectCacheDelegate.hmCacheSelectAllHub) {
			OAObjectCacheDelegate.hmCacheSelectAllHub.clear();
		}
		synchronized (OAObjectCacheDelegate.hmCacheNamedHub) {
			OAObjectCacheDelegate.hmCacheNamedHub.clear();
		}
	}

	/* see addListener(Class, HubListener) */
	public static void removeListener(Class clazz, OAObjectCacheListener l) {
		LOG.fine("class=" + clazz);
		
		List alListener = OAObjectCacheDelegate.hmCacheListener.get(clazz);
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
	 * Returns array of HubListeners for a given class. see addListener(Class, HubListener)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends OAObject> OAObjectCacheListener<T>[] getListeners(final Class<T> c) {
		if (aiListenerCount.get() == 0) {
			return null;
		}
	
	    List<? extends OAObjectCacheListener<?>> alListener = OAObjectCacheDelegate.hmCacheListener.get(c);
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
	
	
	/** called by OAObject to send a HubEvent. */
	protected static void fireAfterPropertyChange(OAObject obj, OAObjectKey origKey, String propertyName, Object oldValue, Object newValue,
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

	protected static <T extends OAObject> void fireAfterLoadEvent(T obj) {
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

	public static <T extends OAObject> void fireAfterAddEvent(Hub<T> hub, T obj) {
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

	
	
	public static <T extends OAObject> void fireAfterRemoveEvent(Hub<T> hub, T obj) {
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

	
	
	public static void removeAllObjects() {
		LOG.warning("removing all Objects was called (fyi only)");
		for (Class c : getOAObjectCache().getClasses()) {
			removeAllObjects(c);
		}
	}

	public static void removeAllObjects(Class c) {
		LOG.warning(String.format("removing all Objects for class=%s was called (fyi only)", c.getSimpleName()));
		getOAObjectCache().clearCache(c);
	}

	/**
	 * Used to <i>visit</i> every object in the Cache.
	 */
	public static void callback(OACallback callback) {
		visit(callback);
	}

	public static void visit(OACallback callback) {
		LOG.fine("visit");
		getOAObjectCache().visit(callback);
	}

	public static void callback(Class<? extends OAObject> clazz, OACallback callback) {
		getOAObjectCache().visit(clazz, callback);
	}

	public static void visit(Class clazz, OACallback callback) {
		getOAObjectCache().visit(clazz, callback);
	}

	/**
	 * Used to <i>visit</i> every object in the Cache for a Class.
	 */
	public static void callback(OACallback callback, Class clazz) {
		getOAObjectCache().visit(clazz, callback);
	}

	public static void visit(OACallback callback, Class clazz) {
		getOAObjectCache().visit(clazz, callback);
	}

	/**
	 * Populates a List of Strings that describe the Classes and amount of objects that are loaded.
	 */
	public static void getInfo(List al) {
		List alx = getInfo();
		al.add(alx);
	}

	public static Class[] getClasses() {
		return getOAObjectCache().getClasses();
	}

	public static int getTotal(Class clazz) {
		return getOAObjectCache().getTotal(clazz);
	}


	/**
	 * Returns a List of Strings that describe the Classes and amount of objects that are loaded.
	 */
	public static List getInfo() {
		// LOG.finer("called");
		List<String> al = new ArrayList();
		al.add("HubController Info --- ");

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
	 * The DefaultAddMode determines how HubController.addObject() will handle an object if it already exists. This method sets the Default
	 * mode for all unassigned threads.
	 *
	 * @param mode AddModes are NO_DUPS (default), IGNORE_DUPS, OVERWRITE_DUPS. see HubController#setAddMode
	 */
	static public void setDefaultAddMode(int mode) {
		LOG.config("default add mode=" + mode);
		if (mode > 4 || mode < 0) {
			throw new IllegalArgumentException("HubController.setDefaultAddMode() must be 0,1,2,3 or 4");
		}
		DefaultAddMode = mode;
	}

	/**
	 * @see #setDefaultAddMode(int)
	 */
	static public int getDefaultAddMode() {
		return DefaultAddMode;
	}


	public static void clearCache(Class clazz) {
		getOAObjectCache().clearCache(clazz);
	}


	/**
	 * Used to cache new objects using a weakReference.
	 * @return if another object matches, else this obj. 
	 */
	public static OAObject add(OAObject obj) {
		return add(obj, false, true);
	}
	public static OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll) {
		return add(obj, bErrorIfExists, bAddToSelectAll, false);
	}

	public static OAObject add(OAObject obj, boolean bErrorIfExists, boolean bAddToSelectAll, boolean bSendAddEventInAnotherThread) {
		if (bDisableCache) {
			return obj;
		}
		OAObject objx = _add(obj, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
		return objx;
	}



	private static OAObject _add(final OAObject obj, final boolean bErrorIfExists, boolean bAddToSelectAll,
			final boolean bSendAddEventInAnotherThread) {
		final OAObjectKey key = OAObjectKeyDelegate.createObjectKey(obj);
		OAObject objResult;

		objResult = _add2(obj, key, bErrorIfExists, bAddToSelectAll, bSendAddEventInAnotherThread);
		return objResult;
	}

	
	private static OAObject _add2(final OAObject obj, final OAObjectKey key, final boolean bErrorIfExists, boolean bAddToSelectAll,
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
		
		final OAObject objFound = getOAObjectCache().getObject(clazz, guid);

		boolean bSendAddEvent = false;
		int mode = OAThreadLocalDelegate.getObjectCacheAddMode();
		if (objFound == null) {
			if (mode != IGNORE_ALL) {
				getOAObjectCache().updateObject(obj, key, clazz);
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
				getOAObjectCache().updateObject(obj, key, clazz);
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
	
	
	

	protected static <T extends OAObject> void fireAfterAddEvent(T obj, boolean bSendAddEventInAnotherThread) {
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

	
	private static class SendAddEventInfo {
		OAObjectCacheListener[] hls;
		Object obj;

		public SendAddEventInfo(OAObjectCacheListener[] hls, Object obj) {
			this.hls = hls;
			this.obj = obj;
		}
	}


	protected static synchronized void startCacheSendAddEventThread() {
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
		}, "OAObjectCacheDelegate.SendAddEvent");
		threadCacheSendAddEvent.setDaemon(true);
		threadCacheSendAddEvent.start();
	}

	public static void addToSelectAllHubs(OAObject obj) {
		Hub[] hs = getSelectAllHubs(obj.getClass());
		for (int i = 0; hs != null && i < hs.length; i++) {
			LOG.finer("adding to selectAll Hub=" + hs[i]);
			if (!hs[i].contains(obj)) {
				hs[i].add(obj);
			}
		}
	}

	
	protected static void propertyKeyValueChanged(OAObject obj) {
		if (bDisableCache) return;
		getOAObjectCache().updateObject(obj);
	}

	
	static public void removeObject(final OAObject obj) {
		getOAObjectCache().removeObject(obj);
	}
	


	/**
	 * Used to retrieve any object based on its Object Id property value.
	 *
	 * @param key object to compare to, object or objects[] to compare this object's objectId(s) with or OAObjectKey to compare with this
	 *            object's objectId
	 * @see OAObjectKey#OAObjectKey
	 * @see OAObject#equals
	 */
	public static <T extends OAObject> T getObject(Class<T> clazz, Object key) {
		return get(clazz, key);
	}

	/**
	 * Used to retrieve any object based on its Object Id property value.
	 *
	 * @see getObject(Class, Object)
	 */
	public static <T extends OAObject> T get(Class<T> clazz, int id) {
		return get(clazz, Integer.valueOf(id));
	}

	
	public static <T extends OAObject> T get(Class<T> clazz, Object key) {
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
	
	public static <T extends OAObject> T get(Class<T> clazz, OAObjectKey ok) {
		if (clazz == null || ok == null) return null;
		OAObject obj = getOAObjectCache().getObject(clazz, ok); 
		return (T) obj;
	}

//qqqqqq remove this method ??	
	public static <T extends OAObject> T getNewObjectUsingGuid(Class<T> clazz, long guid) {
		Object obj = getOAObjectCache().getObject((Class<OAObject>) clazz, guid); 
		return (T) obj;
	}

	public static <T extends OAObject> T getUsingGuid(Class<T> clazz, long guid) {
		Object obj = getOAObjectCache().getObject(clazz, guid); 
		return (T) obj;
	}
	
	/**
	 * Used to retrieve any object.
	 *
	 * @param currentIndexKey object to find.
	 */
	public static Object get(OAObject obj) {
		if (bDisableCache) {
			return null;
		}
		if (obj == null) {
			return null;
		}
		return get(obj.getClass(), OAObjectKeyDelegate.getKey((OAObject) obj));
	}

	public static Object findNext(Object fromObject) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), null, null, false, true);
	}

	public static Object findNext(Object fromObject, String propertyPath, Object findObject) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), propertyPath, findObject, false, true);
	}

	public static Object findNext(Object fromObject, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		if (fromObject == null) {
			return null;
		}
		return _find(fromObject, fromObject.getClass(), propertyPath, findObject, bSkipNew, bThrowException);
	}

	public static Object findNext(Object fromObject, Class fromClass, String propertyPath, Object findObject) {
		if (fromObject == null && fromClass == null) {
			return null;
		}
		if (fromClass == null) {
			fromClass = fromObject.getClass();
		}
		return _find(fromObject, fromClass, propertyPath, findObject, false, true);
	}

	/**
	 * Searches all objects in Class clazz for an object with property equalTo findObject.
	 */
	public static Object find(Class clazz) {
		return _find(null, clazz, null, null, false, true);
	}

	public static Object find(Class clazz, OAFinder finder) {
		return _find(null, clazz, finder, false, true);
	}

	public static Object find(Class clazz, String propertyPath, Object findObject) {
		return _find(null, clazz, propertyPath, findObject, false, true);
	}

	public static Object find(Class clazz, String propertyPath, Object findObject, boolean bSkipNew, boolean bThrowException) {
		return _find(null, clazz, propertyPath, findObject, bSkipNew, bThrowException);
	}

	public static Object find(Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		return _find(null, clazz, finder, false, true);
	}

	protected static Object _find(Object fromObject, Class clazz, String propertyPath, Object findObject, boolean bSkipNew,
			boolean bThrowException) {
		return _find(fromObject, clazz, propertyPath, findObject, bSkipNew, bThrowException, 1, null);
	}

	protected static Object _find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException) {
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, 1, null);
	}

	public static Object find(Object fromObject, Class clazz, OAFinder finder, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	public static Object find(Object fromObject, Class clazz, OAFilter filter, boolean bSkipNew, boolean bThrowException, int fetchAmount,
			List<OAObject> alResults) {
		OAFinder finder = new OAFinder();
		if (filter != null) {
			finder.addFilter(filter);
		}
		return _find(fromObject, clazz, finder, bSkipNew, bThrowException, fetchAmount, alResults);
	}

	public static Object find(Object fromObject, Class clazz, int fetchAmount, List<OAObject> alResults) {
		return _find(fromObject, clazz, null, false, false, fetchAmount, alResults);
	}

	// 20140125 get objects from cache
	/**
	 * Returns objects from the objectCache.
	 *
	 * @param clazz       type of objects
	 * @param fromObject  null to start from the beginning, else use the last object previously returned.
	 * @param fetchAmount max number to add to the alResults
	 * @param alResults   list of objects, after the fromObject
	 * @return last object in alResults, that can be used as the fromObject on the next call to fetch
	 */
	protected static Object _find(Object fromObject, Class clazz, String propertyPath, Object findValue, boolean bSkipNew,
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

	protected static Object _find(final Object fromObject, final Class<? extends OAObject> clazz, final OAFinder finder, final boolean bSkipNew,
			final boolean bThrowException, int fetchAmount, final List<OAObject> alResults) {
		if (bDisableCache) {
			return null;
		}
		return getOAObjectCache().find(fromObject, clazz, finder, bSkipNew, fetchAmount, alResults);
	}	
	

	/**
	 * Refresh all objects from the datasource. This will be ran on the server, if called by client then it will async to run on server.
	 *
	 * @param clazz Class of objects to update, will also requery all hubs for this class.
	 */
	public static void refresh(Class clazz) {
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

	/*
	public static void updateClientInfo(OAClientInfo ci) {
		// LOG.fine("called");
		for (Class c : getOAObjectCache().getClasses()) {
	    	ci.getCacheHashMap().put(c, getOAObjectCache().getTotal(c));
		}
	}
	*/
	
	public static void setDisableCache(boolean b) {
		bDisableCache = b;
	}

	public static OAObjectCache getOAObjectCache() {
		return objectCache;
	}
	
}

