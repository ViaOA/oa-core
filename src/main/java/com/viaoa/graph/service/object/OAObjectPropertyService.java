package com.viaoa.graph.service.object;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OANotExist;

public abstract class OAObjectPropertyService {
	private static final Logger LOG = Logger.getLogger(OAObjectPropertyService.class.getName());

	private final OAObject.FriendAccess faObject;
	
    public OAObjectPropertyService(OAObject.FriendAccess faObject) {
    	if (faObject == null) throw new IllegalArgumentException("OAObjectFriendAccess can not be null");
    	this.faObject = faObject;
    }
	
	/**
	 * Returns whether the specified property has already been loaded for the
	 * given object. A property is considered loaded when its stored value is
	 * present and does not require resolution from a data source or remote
	 * server.  
	 *
	 * <p>The method checks for:</p>
	 * <ul>
	 *   <li>A direct stored value</li>
	 *   <li>A WeakReference whose referent is still available</li>
	 *   <li>An OAObjectKey that can be resolved in the cache to a real object</li>
	 * </ul>
	 *
	 * @param oaObj the object whose property is being checked
	 * @param name  the property name, case-insensitive
	 * @return true if the property value is fully loaded and available;
	 *         false if it is missing, unresolved, or not yet loaded
	 */
	public boolean isPropertyLoaded(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		Object[] props = faObject.getProperties(oaObj);
		if (props == null) {
			return false;
		}

		for (int i = 0; i < props.length; i += 2) {
			if (props[i] == null || !name.equalsIgnoreCase((String) props[i])) {
				continue;
			}

			Object objx = props[i + 1];
			if (objx instanceof WeakReference) {
				objx = ((WeakReference) objx).get();
				if (objx == null) {
					return false;
				}
			} else if (objx instanceof OAObjectKey) {
				OALinkInfo li = callInfoGetLinkInfo(oaObj.getClass(), name);
				if (li == null) {
					return false;
				}
				Object objz = callCacheGet(li.getToClass(), (OAObjectKey) objx);
				return (objz != null);
			}
			return true; // real value is null (/does not exist)
		}
		return false;
	}

	/**
	 * Determines whether the specified property reference is effectively null.
	 * A reference is considered null when no entry for the given property name
	 * exists in the object's internal property array.
	 *
	 * @param oaObj the object whose property reference is being checked
	 * @param name  the property name, case-insensitive
	 * @return true if the property name is not present in the stored properties;
	 *         false if the property exists (regardless of its value)
	 */
	public boolean isReferenceNull(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		Object[] props = faObject.getProperties(oaObj);
		if (props == null) {
			return false;
		}

		for (int i = 0; i < props.length; i += 2) {
			if (props[i] == null || !name.equalsIgnoreCase((String) props[i])) {
				continue;
			}
			return false;
		}
		return true;
	}

	/**
	 * Returns all property names currently stored on the given object. Only
	 * property slots with a non-null name are included in the result.
	 *
	 * @param oaObj the object whose property names are requested
	 * @return an array of property names, or null if the object has no
	 *         properties defined
	 */
	public String[] getPropertyNames(OAObject oaObj) {
		Object[] props = faObject.getProperties(oaObj);
		if (props == null) {
			return null;
		}
		String[] ss;

		int cnt = 0;
		for (int i = 0; i < props.length; i += 2) {
			if (props[i] != null) {
				cnt++;
			}
		}
		ss = new String[cnt];
		int j = 0;
		for (int i = 0; i < props.length; i += 2) {
			if (props[i] != null) {
				ss[j++] = (String) props[i];
			}
		}
		return ss;
	}

	/**
	 * Internal helper that stores a property without performing any existence
	 * checks or firing events. This directly inserts or overwrites the value in
	 * the object's property array.
	 *
	 * @param oaObj the target object
	 * @param name  the property name
	 * @param value the value to store
	 */
	public void unsafeAddProperty(OAObject oaObj, String name, Object value) {
		//qqqqqqqq method was protected
		unsafeSetProperty(oaObj, name, value, false, false);
	}
	
	/**
	 * Convenience wrapper around the internal unsafeSetProperty method.  
	 * Stores the given property value without firing events and replaces the
	 * value if the property already exists.
	 *
	 * @param oaObj the target object
	 * @param name  the property name
	 * @param value the value to assign
	 */
	public void unsafeSetProperty(OAObject oaObj, String name, Object value) {
		//qqqqqqqq method was protected
		unsafeSetProperty(oaObj, name, value, true, false);
	}

	/**
	 * Stores the property value only if no existing entry for the property
	 * name is present. No events are fired and no validation is performed.
	 *
	 * @param oaObj the target object
	 * @param name  the property name
	 * @param value the value to assign if the property is not already defined
	 */
	public void unsafeSetPropertyIfEmpty(OAObject oaObj, String name, Object value) {
		//qqqqqqqq method was protected
		unsafeSetProperty(oaObj, name, value, true, true);
	}

	/**
	 * Core implementation for setting a property without firing change events
	 * or performing validation. Depending on the supplied flags, this method
	 * can either overwrite existing entries or only insert a new value when
	 * no matching property name is found.
	 *
	 * <p>When a Hub value is assigned, its master object is automatically
	 * initialized if necessary.</p>
	 *
	 * @param oaObj           the target object
	 * @param name            the property name
	 * @param value           the value to store
	 * @param bCheckFirst     if true, existing entries are checked for reuse
	 * @param bOnlyIfNotFound if true, the value is stored only when the
	 *                        property does not already exist
	 */
	private void unsafeSetProperty(OAObject oaObj, String name, Object value, boolean bCheckFirst, boolean bOnlyIfNotFound) {
		int pos;
		Object[] properties = faObject.getProperties(oaObj);
		if (properties == null) {
			properties = new Object[2];
			faObject.setProperties(oaObj, properties);
			pos = 0;
		} else {
			pos = -1;
			if (bCheckFirst || bOnlyIfNotFound) {
				for (int i = 0; i < properties.length; i += 2) {
					if (pos == -1 && properties[i] == null) {
						pos = i;
					} else if (name.equalsIgnoreCase((String) properties[i])) {
						if (bOnlyIfNotFound) {
							return;
						}
						pos = i;
						break;
					}
				}
			}
			if (pos < 0) {
				pos = properties.length;
				properties = Arrays.copyOf(properties, pos + 2);
				faObject.setProperties(oaObj, properties);
			}
		}
		properties[pos] = name;
		properties[pos + 1] = value;

		// in case Hub.datam.masterObject is not set
		Object objx = value;
		if (objx instanceof WeakReference) {
			objx = ((WeakReference) objx).get();
		}
		if (objx instanceof Hub) {
			Hub hub = (Hub) objx;
			if (hub.getMasterObject() == null) {
				callHubSetMasterObject((Hub) objx, oaObj, name);
			}
		}
	}

	/**
	 * Removes the specified property from the object. The internal property
	 * array is compacted if empty slots are detected. Optionally fires a
	 * property change event after removal.
	 *
	 * @param oaObj               the target object
	 * @param name                the property name to remove
	 * @param bFirePropertyChange true to fire a property change event after
	 *                            removal, false to suppress event generation
	 */
	public void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
		Object[] properties = faObject.getProperties(oaObj);
		if (properties == null || name == null) {
			return;
		}
		Object value = null;
		boolean bResize = false;
		synchronized (oaObj) {
			for (int i = 0; i < properties.length; i += 2) {
				if (properties[i] == null) {
					bResize = true;
				} else if (name.equalsIgnoreCase((String) properties[i])) {
					value = properties[i + 1];
					properties[i] = null;
					properties[i + 1] = null;
					if (bResize) {
						resizeProperties(oaObj);
					}
					break;
				}
			}
		}
		if (bFirePropertyChange) {
			faObject.firePropertyChange(oaObj, name, value, null);
		}
	}

	/**
	 * Removes the specified property only if its current value is null.  
	 * The internal property array is compacted if empty slots are detected.
	 * Optionally fires a property change event when removal occurs.
	 *
	 * @param oaObj               the target object
	 * @param name                the property name to check and remove
	 * @param bFirePropertyChange true to fire a property change event when the
	 *                            property is removed
	 * @return true if the property existed and was removed because its value
	 *         was null; false if the property did not exist or its value was
	 *         non-null
	 */
	public boolean removePropertyIfNull(OAObject oaObj, String name, boolean bFirePropertyChange) {
		Object[] properties = faObject.getProperties(oaObj);
		if (oaObj == null || properties == null || name == null) {
			return false;
		}
		Object value = null;
		boolean bResize = false;
		synchronized (oaObj) {
			for (int i = 0; i < properties.length; i += 2) {
				if (properties[i] == null) {
					bResize = true;
				} else if (name.equalsIgnoreCase((String) properties[i])) {
					value = properties[i + 1];
					if (value != null) {
						return false;
					}

					properties[i] = null;
					properties[i + 1] = null;
					if (bResize) {
						resizeProperties(oaObj);
					}
					break;
				}
			}
		}
		if (bFirePropertyChange) {
			faObject.firePropertyChange(oaObj, name, value, null);
		}
		return true;
	}
	
	/**
	 * Compacts the internal property array by removing null entries and
	 * resizing the array to contain only active name/value pairs.
	 *
	 * @param oaObj the object whose property array should be resized
	 */
	private void resizeProperties(OAObject oaObj) {
		Object[] properties = faObject.getProperties(oaObj);
		int newSize = 0;
		for (int i = 0; i < properties.length; i += 2) {
			if (properties[i] != null) {
				newSize += 2;
			}
		}
		Object[] objs = new Object[newSize];
		for (int i = 0, j = 0; i < properties.length; i += 2) {
			if (properties[i] != null) {
				objs[j++] = properties[i];
				objs[j++] = properties[i + 1];
			}
		}
		faObject.setProperties(oaObj, objs);
	}

	/**
	 * Sets or updates the specified property on the object. The internal
	 * property array is expanded as needed and the value is stored.  
	 * If the value is a Hub, its master object is initialized when required.
	 *
	 * @param oaObj the target object
	 * @param name  the property name, case-insensitive
	 * @param value the value to assign
	 */
	public void setProperty(OAObject oaObj, String name, Object value) {
		if (oaObj == null || name == null) {
			return;
		}

		synchronized (oaObj) {
			Object[] properties = faObject.getProperties(oaObj);
			int pos;
			if (properties == null) {
				properties = new Object[2];
				faObject.setProperties(oaObj, properties);
				pos = 0;
			} else {
				pos = -1;
				for (int i = 0; i < properties.length; i += 2) {
					if (pos == -1 && properties[i] == null) {
						pos = i;
					} else if (name.equalsIgnoreCase((String) properties[i])) {
						pos = i;
						break;
					}
				}
				if (pos < 0) {
					pos = properties.length;
					properties = Arrays.copyOf(properties, pos + 2);
					faObject.setProperties(oaObj, properties);
				}
			}
			properties[pos] = name;
			properties[pos + 1] = value;
		}

		// in case Hub.datam.masterObject is not set
		Object objx = value;
		if (objx instanceof WeakReference) {
			objx = ((WeakReference) objx).get();
		}
		if (objx instanceof Hub) {
			Hub hub = (Hub) objx;
			if (hub.getMasterObject() == null) {
				callHubSetMasterObject((Hub) objx, oaObj, name);
			}
		}
	}

	/**
	 * Sets the value for a Hub-based property only if no existing non-null
	 * value is already stored. WeakReference values are treated as empty when
	 * their referent has been garbage collected.  
	 * The property array is expanded as needed.
	 *
	 * <p>If the assigned value is a Hub, its master object is initialized
	 * when required.</p>
	 *
	 * @param oaObj the target object
	 * @param name  the property name, case-insensitive
	 * @param value the value to assign if the property is not already set
	 */
	public void setPropertyHubIfNotSet(OAObject oaObj, String name, Object value) {
		if (oaObj == null || name == null) {
			return;
		}

		Object[] properties = faObject.getProperties(oaObj);
		if (properties != null) {
			for (int i = 0; i < properties.length; i += 2) {
				if (name.equalsIgnoreCase((String) properties[i])) {
					if (properties[i + 1] != null) {
						if (!(properties[i + 1] instanceof WeakReference)) {
							return;
						}
						if (((WeakReference) properties[i + 1]).get() != null) {
							return;
						}
					}
				}
			}
		}

		synchronized (oaObj) {
			properties = faObject.getProperties(oaObj);
			int pos;
			if (properties == null) {
				properties = new Object[2];
				faObject.setProperties(oaObj, properties);
				pos = 0;
			} else {
				pos = -1;
				for (int i = 0; i < properties.length; i += 2) {
					if (pos == -1 && properties[i] == null) {
						pos = i;
					} else if (name.equalsIgnoreCase((String) properties[i])) {
						pos = i;
						break;
					}
				}
				if (pos < 0) {
					pos = properties.length;
					properties = Arrays.copyOf(properties, pos + 2);
					faObject.setProperties(oaObj, properties);
				}
			}
			if (properties[pos + 1] == null || ((properties[pos + 1] instanceof WeakReference)
					&& (((WeakReference) properties[pos + 1]).get() == null))) {
				properties[pos + 1] = value;
				properties[pos] = name;
			}
		}

		// in case Hub.datam.masterObject is not set
		Object objx = value;
		if (objx instanceof WeakReference) {
			objx = ((WeakReference) objx).get();
		}
		if (objx instanceof Hub) {
			Hub hub = (Hub) objx;
			if (hub.getMasterObject() == null) {
				callHubSetMasterObject((Hub) objx, oaObj, name);
			}
		}
	}

	/**
	 * Convenience wrapper around the full compare-and-swap implementation.  
	 * Attempts to update the property only when its current value matches
	 * the supplied match value.
	 *
	 * @param oaObj     the target object
	 * @param name      the property name, case-insensitive
	 * @param newValue  the value to assign if the current value matches
	 * @param matchValue the expected current value
	 * @return the resulting stored value
	 */
	public Object setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
		return setPropertyCAS(oaObj, name, newValue, matchValue, false, false);
	}

	/**
	 * Performs an atomic compare-and-swap update on the specified property.
	 * The update occurs only when the property's current value satisfies the
	 * provided match conditions, including optional requirements regarding
	 * existence or non-existence.
	 *
	 * <p>WeakReference values are resolved for comparison when needed.
	 * If a Hub value already exists, it is not overwritten with null.</p>
	 *
	 * @param oaObj            the target object
	 * @param name             the property name, case-insensitive
	 * @param newValue         the value to assign when the match succeeds
	 * @param matchValue       the expected current value
	 * @param bMustNotExist    if true, the update occurs only when the
	 *                         property does not already exist
	 * @param bReturnNotExist  if true, returns {@code OANotExist.instance}
	 *                         when the match fails and the property does not exist
	 * @return the value stored after the operation, or the existing value
	 *         when the match fails
	 */
	public Object setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist,
			boolean bReturnNotExist) {
		if (oaObj == null || name == null) {
			return null;
		}
		synchronized (oaObj) {
			Object[] properties = faObject.getProperties(oaObj);
			int pos;
			if (properties == null) {
				if (!bMustNotExist) {
					if (matchValue != null) {
						if (bReturnNotExist) {
							return OANotExist.instance;
						}
						return null;
					}
				}
				properties = new Object[2];
				faObject.setProperties(oaObj, properties);
				pos = 0;
			} else {
				pos = -1;
				for (int i = 0; i < properties.length; i += 2) {
					if (pos == -1 && properties[i] == null) {
						pos = i;
						continue;
					}
					if (!name.equalsIgnoreCase((String) properties[i])) {
						continue;
					}

					if (bMustNotExist) {
						return properties[i + 1];
					}

					if (matchValue != properties[i + 1]) {
						if (properties[i + 1] instanceof WeakReference) {
							Object objx = ((WeakReference) properties[i + 1]).get();
							if (matchValue == objx) {
								pos = i;
							}
							break;
						}

						if (matchValue == null) {
							return properties[i + 1];
						}
						if (!matchValue.equals(properties[i + 1])) {
							if (!(matchValue instanceof OAObjectKey) || !(newValue instanceof OAObject)) {
								return properties[i + 1];
							}
							OAObjectKey k = callKeyGetKey((OAObject) newValue);
							if (!callKeyIsForSameOAObject(null, (OAObjectKey)matchValue, k)) {
								return properties[i + 1];
							}
						}
					}
					pos = i;
					break;
				}
				if (pos < 0) {
					if (!bMustNotExist) {
						if (matchValue != null) {
							if (bReturnNotExist) {
								return OANotExist.instance;
							}
							return null;
						}
					}
					pos = properties.length;
					properties = Arrays.copyOf(properties, pos + 2);
					faObject.setProperties(oaObj, properties);
				} else if (properties[pos] == null) {
					if (!bMustNotExist) {
						if (matchValue != null) {
							if (bReturnNotExist) {
								return OANotExist.instance;
							}
							return null;
						}
					}
				}
			}
			properties[pos] = name;

			if (newValue != null || !(properties[pos + 1] instanceof Hub)) { // 20120827 dont set an existing Hub to null (sent that way if size is 0)
				properties[pos + 1] = newValue;
			}

			// in case Hub.datam.masterObject is not set
			Object objx = newValue;
			if (objx instanceof WeakReference) {
				objx = ((WeakReference) objx).get();
			}
			if (objx instanceof Hub) {
				Hub hub = (Hub) objx;
				if (hub.getMasterObject() == null) {
					callHubSetMasterObject((Hub) objx, oaObj, name);
				}
			}
		}
		return newValue;
	}
	
	/**
	 * Convenience wrapper that retrieves the value of the specified property
	 * without converting WeakReference values and without returning
	 * {@code OANotExist} for missing entries.
	 *
	 * @param oaObj the target object
	 * @param name  the property name, case-insensitive
	 * @return the stored value, or null if the property is not found
	 */
	public Object getProperty(OAObject oaObj, String name) {
		return getProperty(oaObj, name, false, false);
	}
	
	
	/**
	 * Retrieves the value of the specified property with optional handling
	 * for missing entries and WeakReference values.
	 *
	 * <p>If {@code bConvertWeakRef} is true and the stored value is a
	 * WeakReference, its referent is returned when available. If the referent
	 * has been garbage collected, the method returns either null or
	 * {@code OANotExist.instance}, depending on {@code bReturnNotExist}.</p>
	 *
	 * @param oaObj           the target object
	 * @param name            the property name, case-insensitive
	 * @param bReturnNotExist true to return {@code OANotExist.instance} when
	 *                        the property does not exist or is unresolved
	 * @param bConvertWeakRef true to resolve and return values stored as
	 *                        WeakReferences
	 * @return the stored value, a resolved referent, {@code OANotExist.instance},
	 *         or null depending on the parameters and property state
	 */
	public Object getProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
		if (oaObj == null || name == null) {
			return null;
		}

		Object[] objs = faObject.getProperties(oaObj);
		if (objs == null) {
			if (bReturnNotExist) {
				return OANotExist.instance;
			}
			return null;
		}
		for (int i = 0; i < objs.length; i += 2) {
			if (objs[i] == null || !name.equalsIgnoreCase((String) objs[i])) {
				continue;
			}
			Object objx = objs[i + 1];
			if (bConvertWeakRef && objx instanceof WeakReference) {
				objx = ((WeakReference) objx).get();
				if (objx == null) {
					if (bReturnNotExist) {
						return OANotExist.instance;
					}
					return null;
				}
			}
			return objx;
		}
		if (bReturnNotExist) {
			return OANotExist.instance;
		}
		return null;
	}

	
	// property locking
	private final ConcurrentHashMap<String, PropertyLock> hmLock = new ConcurrentHashMap<String, PropertyLock>();
	private final ConcurrentHashMap<Thread, Thread> hmLockedThread = new ConcurrentHashMap<Thread, Thread>();

	private static class PropertyLock {
		final Thread thread;
		boolean done;
		boolean hasWait;

		public PropertyLock(Thread thread) {
			this.thread = thread;
		}

	}
	
	/**
	 * Attempts to acquire an exclusive lock for the specified property.  
	 * This call will wait if necessary until the lock becomes available.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to lock
	 * @return true if the lock is successfully acquired; false otherwise
	 */
	public boolean setPropertyLock(OAObject oaObj, String name) {
		return _setPropertyLock(oaObj, name, true, false);
	}

	/**
	 * Attempts to acquire an exclusive lock for the specified property
	 * without waiting.  
	 * If the lock is already held by another thread, this method returns
	 * immediately with {@code false}.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to lock
	 * @return true if the lock is acquired; false if it is already held
	 */
	public boolean attemptPropertyLock(OAObject oaObj, String name) {
		return _setPropertyLock(oaObj, name, false, true);
	}

	/**
	 * Core implementation for acquiring a property-level lock.  
	 * Creates or reuses a lock entry and manages waiting behavior, deadlock
	 * detection, and re-entry checks depending on the supplied flags.
	 *
	 * @param oaObj              the target object
	 * @param name               the property name to lock
	 * @param bWaitIfNeeded      true to wait until the lock becomes available;
	 *                           false to return immediately if locked
	 * @param bCheckIfThisThread true to return true only when the current
	 *                           thread already owns the lock
	 * @return true if the lock is acquired according to the requested rules;
	 *         false otherwise
	 */
	private boolean _setPropertyLock(final OAObject oaObj, final String name, final boolean bWaitIfNeeded,
			final boolean bCheckIfThisThread) {
		if (oaObj == null || name == null) {
			return false;
		}
		String key = callGuidGetGuid(oaObj) + "." + name.toUpperCase();
		PropertyLock lock;
		final Thread threadThis = Thread.currentThread();
		
		
		lock = hmLock.computeIfAbsent(key, k -> new PropertyLock(threadThis));
		if (lock.thread == threadThis) {
			return true;
		}

		hmLockedThread.put(threadThis, lock.thread);
		try {
			callRemoteThreadStartNextThread();
			synchronized (lock) {
				if (lock.thread == Thread.currentThread()) {
					return bCheckIfThisThread;
				}
				if (!bWaitIfNeeded) {
					return false;
				}
				long ms = 0;
				for (int i = 0;; i++) {
					if (i > 3) {

						// see if the thread that thisThread is waiting on is waiting on another thread
						Thread tx = hmLockedThread.get(lock.thread);
						if (tx != null) {
							if (OAObject.getDebugMode()) {
								String s = oaObj.getObjectKey().toString();
								s = "thread with lock is waiting on a lock, obj=" + oaObj + ", key=" + s + ", prop=" + name
										+ ", this.Thread=" + Thread.currentThread().getName() + ", waiting on Thread="
										+ lock.thread.getName() + " (see next stacktrace), will continue";
								LOG.log(Level.WARNING, s, new Exception("fyi: avoiding deadlock, will continue"));
								StackTraceElement[] stes = lock.thread.getStackTrace();
								Exception ex = new Exception();
								ex.setStackTrace(stes);
								LOG.log(Level.WARNING, "... waiting on this Thread=" + lock.thread.getName(), ex);
							}
							break;
						}

						if (ms == 0) {
							ms = System.currentTimeMillis();
						} else if (System.currentTimeMillis() - ms > 60000) {
							if (OAObject.getDebugMode()) {
								String s = oaObj.getObjectKey().toString();
								s = "wait time exceeded for lock, obj=" + oaObj + ", key=" + s + ", prop=" + name + ", this.Thread="
										+ Thread.currentThread().getName() + ", waiting on Thread=" + lock.thread.getName()
										+ " (see next stacktrace), will continue";
								LOG.log(Level.WARNING, s, new Exception("fyi: wait time exceeded, will continue"));
								StackTraceElement[] stes = lock.thread.getStackTrace();
								Exception ex = new Exception();
								ex.setStackTrace(stes);
								LOG.log(Level.WARNING, "... waiting on this Thread=" + lock.thread.getName(), ex);
							}
							return false; // bail out, ouch
						}
					}
					if (lock.done) {
						break;
					}
					lock.hasWait = true;
					try {
						lock.wait(100);
					} catch (Exception e) {
					}
				}
			}
		} finally {
			hmLockedThread.remove(threadThis);
		}
		return _setPropertyLock(oaObj, name, bWaitIfNeeded, bCheckIfThisThread); // create a new one
	}
	
	/**
	 * Releases the lock associated with the specified property, if one exists.
	 * Any threads waiting on the lock are notified so they may attempt to
	 * acquire it.
	 *
	 * @param oaObj the target object
	 * @param name  the property name whose lock should be released
	 */
	public void releasePropertyLock(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return;
		}
		String key = callGuidGetGuid(oaObj) + "." + name.toUpperCase();
		PropertyLock lock;
		synchronized (oaObj) {
			lock = hmLock.remove(key);
		}
		if (lock != null) {
			synchronized (lock) {
				lock.done = true;
				if (lock.hasWait) {
					lock.notifyAll();
				}
			}
		}
	}
	
	/**
	 * Checks whether a lock exists for the specified property.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to check
	 * @return true if the property is currently locked; false otherwise
	 */
	public boolean isPropertyLocked(OAObject oaObj, String name) {
		if (oaObj == null || name == null) {
			return false;
		}
		String key = callKeyGetKey(oaObj).getGuid() + "." + name.toUpperCase();
		return (hmLock.get(key) != null);
	}
	
	/**
	 * Converts the stored value for the specified property to or from a
	 * {@link WeakReference}.  
	 *
	 * <p>If converting to a WeakReference, the current value is wrapped unless
	 * it is already weak.  
	 * If converting from a WeakReference, the referent is restored when
	 * available; otherwise the property is removed if appropriate.</p>
	 *
	 * @param oaObj      the target object
	 * @param name       the property name, case-insensitive
	 * @param bToWeakRef true to convert the value to a WeakReference;
	 *                   false to restore a strong reference
	 * @param value      fallback value used when restoring from a collected
	 *                   WeakReference
	 * @return true if the stored value was changed; false otherwise
	 */
	public boolean setPropertyWeakRef(OAObject oaObj, String name, boolean bToWeakRef, Object value) {
		if (name == null || oaObj == null) {
			return false;
		}

		boolean b = false;
		synchronized (oaObj) {
			Object[] properties = faObject.getProperties(oaObj);
			if (properties == null) return false;

			for (int i = 0; i < properties.length; i += 2) {
				if (!name.equalsIgnoreCase((String) properties[i])) {
					continue;
				}
				Object val = properties[i + 1];
				if (val == null) {
					break;
				}
				if (bToWeakRef) {
					if (!(val instanceof WeakReference)) {
						properties[i + 1] = new WeakReference(val);
						b = true;
					}
				} else {
					if (val instanceof WeakReference) {
						b = true;
						val = ((WeakReference) val).get();
						if (val == null) {
							val = value;
						}
						if (val == null) {
							removePropertyIfNull(oaObj, name, false);
						} else {
							properties[i + 1] = val;
						}
					}
				}
				break;
			}
		}
		return b;
	}

	/**
	 * Ensures that the specified object and its parent objects maintain either
	 * strong or weak references depending on the supplied flag.  
	 *
	 * <p>This is used on the server to prevent Hub values from being garbage
	 * collected when their parent objects have a cache size that allows
	 * eviction. The operation is applied recursively through one-to-many
	 * relationships.</p>
	 *
	 * @param obj            the object to process
	 * @param bReferenceable true to enforce strong references; false to allow
	 *                       weak references
	 */
	public void setReferenceable(OAObject obj, boolean bReferenceable) {
		setReferenceable(obj, bReferenceable, null);
	}

	/**
	 * Internal recursive implementation used to apply strong or weak reference
	 * rules to an object and its parent objects.  
	 *
	 * <p>The method walks one-to-many reverse links, ensuring that referenced
	 * Hubs are converted to strong or weak references as needed, and prevents
	 * repeated processing through the supplied cascade tracker.</p>
	 *
	 * @param obj            the object to process
	 * @param bReferenceable true to enforce strong references; false to allow
	 *                       weak references
	 * @param cascade        tracker used to avoid repeated processing of the
	 *                       same objects during recursion
	 */
	private void setReferenceable(final OAObject obj, boolean bReferenceable, OACascade cascade) {
		if (obj == null) {
			return;
		}
		if (!callSyncIsServer()) {
			return;
		}
		if (cascade != null && cascade.wasCascaded(obj, true)) {
			return;
		}

		OAObjectInfo oi = callInfoGetObjectInfo(obj.getClass());
		if (!callInfoIsWeakReferenceable(oi)) {
			return;
		}

		boolean bSupportStorage = oi.getSupportsStorage();

		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != OALinkInfo.ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (!li.getUsed()) {
				continue;
			}
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev == null) {
				continue;
			}
			if (liRev.getType() != OALinkInfo.MANY) {
				continue;
			}
			if (liRev.getTransient()) {
				continue;
			}
			if (!isPropertyLoaded(obj, li.getName())) {
				continue;
			}

			Object parent = li.getValue(obj); // parent
			if (!(parent instanceof OAObject)) {
				continue;
			}

			if (!isPropertyLoaded((OAObject) parent, liRev.getName())) {
				continue;
			}

			if (liRev.getCacheSize() > 0) {
				Object objx = getProperty((OAObject) parent, liRev.getName(), true, false);
				if (objx instanceof OANotExist) {
					continue;
				}

				if (objx instanceof WeakReference) {
					objx = ((WeakReference) objx).get();
				}
				if (!(objx instanceof Hub)) {
					continue;
				}
				boolean b = setPropertyWeakRef((OAObject) parent, liRev.getName(), !bReferenceable, (Hub) objx);
				if (!b) {
					break; // already changed, dont need to continue
				}
			}
			if (bReferenceable) {
				if (cascade == null) {
					cascade = new OACascade();
				}
				cascade.wasCascaded(obj, true);
				setReferenceable((OAObject) parent, bReferenceable, cascade);
			}
		}
	}

	/**
	 * Clears all stored properties on the given object by removing its internal
	 * property array.
	 *
	 * @param oaObj the object whose properties should be cleared
	 */
	public void clearProperties(OAObject oaObj) {
		if (oaObj != null) {
			synchronized (oaObj) {
				faObject.setProperties(oaObj, null);
			}
		}
	}

	@OAParentProvided (example = "srvcObject.getOAObjectCacheService().get")
	public abstract <T extends OAObject> T callCacheGet(Class<T> clazz, OAObjectKey ok);

	@OAParentProvided (example = "srvcObject.getOAObjectGuidService().getGuid")
	public abstract UUID callGuidGetGuid(OAObject oaObj);
		
	@OAParentProvided (example = "srvcObject.getOAObjectHubService().setMasterObject")
	public abstract void callHubSetMasterObject(Hub hub, OAObject oaObj, String nameFromMasterToDetail);
		
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getOAObjectInfo(clazz)")
	public abstract OAObjectInfo callInfoGetObjectInfo(Class clazz); 

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().isWeakReferenceable")
	public abstract boolean callInfoIsWeakReferenceable(OAObjectInfo oi);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkInfo")
	public abstract OALinkInfo callInfoGetLinkInfo(Class clazz, String propertyName);	
	
	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().isForSameOAObject")
	public abstract boolean callKeyIsForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);

	@OAParentProvided (example = "srvcObject.getOAObjectKeyService().getKey")
	public abstract OAObjectKey callKeyGetKey(OAObject oaObj);
	
	@OAParentProvided (example = "srvcSync.isServer()")
	public abstract boolean callSyncIsServer();

	@OAParentProvided (example = "srvcOARemoteThread.startNextThread")
	public abstract void callRemoteThreadStartNextThread();
}
