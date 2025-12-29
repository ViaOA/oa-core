/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.object;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.remote.OARemoteThreadDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.util.OANotExist;

/**
 * Internal delegate responsible for storing and managing property values on
 * OAObject instances. This includes primitive and simple properties, object
 * references, and Hub-based collection links.
 *
 * <p>Values are stored compactly as name/value pairs inside OAObject, allowing
 * efficient memory usage and fast access without requiring a Map structure.
 * Support for lazy loading is built in: when a reference property contains an
 * OAObjectKey instead of a loaded OAObject, resolution occurs on demand and
 * the real object is substituted transparently.</p>
 *
 * <p>Relationship integrity is automatically enforced through metadata from
 * OALinkInfo. Setting a reference updates the reverse side of the relationship,
 * ensuring the OA Object Graph always remains consistent without developer
 * intervention. Foreign key assignment and identity reconciliation are also
 * handled internally.</p>
 *
 * <p>Property changes propagate to the OAObjectEditDelegate for dirty tracking,
 * trigger evaluation, and persistence synchronization. This ensures that all
 * updates are recorded, distributed events reflect the correct state, and
 * business rules execute in the proper sequence.</p>
 *
 * <p>This delegate is a core part of the OA runtime, providing automatic Graph
 * behavior driven entirely by metadata, supporting offline, lazy, and highly
 * dynamic application architectures.</p>
 *
 * @see OAObjectEditDelegate
 * @see OAObjectInfo
 * @see OALinkInfo
 * @see OAObject
 */
public class OAObjectPropertyDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectPropertyDelegate.class.getName());

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectPropertyService().??(oaObj);
    */
	
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
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
	public static boolean isPropertyLoaded(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().isPropertyLoaded(oaObj, name);
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
	public static boolean isReferenceNull(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().isReferenceNull(oaObj, name);
	}

	/**
	 * Returns all property names currently stored on the given object. Only
	 * property slots with a non-null name are included in the result.
	 *
	 * @param oaObj the object whose property names are requested
	 * @return an array of property names, or null if the object has no
	 *         properties defined
	 */
	public static String[] getPropertyNames(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectPropertyService().getPropertyNames(oaObj);
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
	static void unsafeAddProperty(OAObject oaObj, String name, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().unsafeAddProperty(oaObj, name, value);
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
	public static void unsafeSetProperty(OAObject oaObj, String name, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().unsafeSetProperty(oaObj, name, value);
	}

	/**
	 * Stores the property value only if no existing entry for the property
	 * name is present. No events are fired and no validation is performed.
	 *
	 * @param oaObj the target object
	 * @param name  the property name
	 * @param value the value to assign if the property is not already defined
	 */
	static void unsafeSetPropertyIfEmpty(OAObject oaObj, String name, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().unsafeSetPropertyIfEmpty(oaObj, name, value);
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
	public static void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().removeProperty(oaObj, name, bFirePropertyChange);
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
	public static boolean removePropertyIfNull(OAObject oaObj, String name, boolean bFirePropertyChange) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().removePropertyIfNull(oaObj, name, bFirePropertyChange);
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
	public static void setProperty(OAObject oaObj, String name, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().setProperty(oaObj, name, value);
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
	public static void setPropertyHubIfNotSet(OAObject oaObj, String name, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().setPropertyHubIfNotSet(oaObj, name, value);
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
	public static Object setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue);
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
	public static Object setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist,
			boolean bReturnNotExist) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectPropertyService().setPropertyCAS(oaObj, name, newValue, matchValue, bMustNotExist, bReturnNotExist);
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
	public static Object getProperty(OAObject oaObj, String name) {
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
	public static Object getProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectPropertyService().getProperty(oaObj, name, bReturnNotExist, bConvertWeakRef);
	}


	/**
	 * Attempts to acquire an exclusive lock for the specified property.  
	 * This call will wait if necessary until the lock becomes available.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to lock
	 * @return true if the lock is successfully acquired; false otherwise
	 */
	public static boolean setPropertyLock(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().setPropertyLock(oaObj, name);
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
	public static boolean attemptPropertyLock(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().attemptPropertyLock(oaObj, name);
	}


	/**
	 * Releases the lock associated with the specified property, if one exists.
	 * Any threads waiting on the lock are notified so they may attempt to
	 * acquire it.
	 *
	 * @param oaObj the target object
	 * @param name  the property name whose lock should be released
	 */
	public static void releasePropertyLock(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().releasePropertyLock(oaObj, name);
	}

	/**
	 * Checks whether a lock exists for the specified property.
	 *
	 * @param oaObj the target object
	 * @param name  the property name to check
	 * @return true if the property is currently locked; false otherwise
	 */
	public static boolean isPropertyLocked(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().isPropertyLocked(oaObj, name);
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
	public static boolean setPropertyWeakRef(OAObject oaObj, String name, boolean bToWeakRef, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectPropertyService().setPropertyWeakRef(oaObj, name, bToWeakRef, value);
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
	public static void setReferenceable(OAObject obj, boolean bReferenceable) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().setReferenceable(obj, bReferenceable);
	}


	/**
	 * Clears all stored properties on the given object by removing its internal
	 * property array.
	 *
	 * @param oaObj the object whose properties should be cleared
	 */
	public static void clearProperties(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectPropertyService().clearProperties(oaObj);
	}

}
