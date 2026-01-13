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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataDelegate;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubLinkDelegate;
import com.viaoa.hub.HubMerger;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareDelegate;
import com.viaoa.hub.HubSortDelegate;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.*;
import com.viaoa.util.*;

/**
 * Reflection-based helper for OAObject that implements dynamic property access,
 * reference resolution, and object creation driven by OA metadata.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Create new domain instances via default constructors (with optional
 *       remote/server creation when enabled).</li>
 *   <li>Get and set properties by name or property path, including support for
 *       primitive-null semantics and value conversion using {@code OAConverter}.</li>
 *   <li>Resolve reference properties: promote stored {@code OAObjectKey} values
 *       to real {@code OAObject} instances using the cache and/or datasource,
 *       without forcing unnecessary hydration.</li>
 *   <li>Enforce relationship integrity using {@code OALinkInfo}: reverse-link
 *       updates, ONE↔MANY handling, and Hub (collection) delegation.</li>
 *   <li>Support lazy-loading, sibling prefetch, and {@code autoCreateNew} for
 *       one-to-one links when configured by metadata.</li>
 *   <li>Fire before/after property change events in correct order so that dirty
 *       tracking, triggers, and distributed sync receive accurate deltas.</li>
 * </ul>
 * <p>
 * This delegate is part of OA's model-driven runtime: it interprets metadata
 * from {@link OAObjectInfo}/{@link OALinkInfo} to provide dynamic, consistent
 * behavior without hand-written reflection code in application classes.
 *
 * @see OAObject
 * @see OAObjectPropertyDelegate
 * @see OAObjectCacheDelegate
 * @see OAObjectInfo
 * @see OALinkInfo
 */
public class OAObjectReflectDelegate {

	private static Logger LOG = Logger.getLogger(OAObjectReflectDelegate.class.getName());

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectReflectService().??(oaObj);

	OAGraph g = OARuntime.get().graph(c);
	if (g == null) return;
	g.objects().getOAObjectReflectService().??(oaObj);
    */
	static OAGraph getGraph(Hub hub, OAObject obj) {
		Class c = null;
		if (hub != null) c = hub.getObjectClass();
		if (c == null && obj != null) c = obj.getClass();
		// if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		return g;
	}
	
	/**
	 * Creates a new instance of the specified class by delegating to
	 * the internal {@code _createNewObject} method. This method will
	 * attempt construction using the default no-arg constructor and
	 * return the resulting object instance.
	 *
	 * @param clazz the class to instantiate
	 * @return a new instance of the class, or a primitive wrapper/empty
	 *         primitive placeholder when applicable
	 */
	public static Object createNewObject(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().createNewObject(clazz);
	}


	/**
	 * Retrieves a property value from the active object of the given
	 * {@link Hub} using the supplied property path. Delegates to the
	 * more general {@code getProperty(hub, null, propPath)}.
	 *
	 * @param hub      the Hub whose active object is used
	 * @param propPath the property name or path to evaluate
	 * @return the resolved property value or {@code null}
	 */
	public static Object getProperty(Hub hub, String propPath) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getProperty(hub, propPath);
	}

	/**
	 * Resolves the value of the specified property path starting from the
	 * given {@link OAObject}. This delegates to the combined hub/object
	 * path evaluator {@code getProperty(null, oaObj, propPath)}.
	 *
	 * @param oaObj    the starting object
	 * @param propPath the property name or dotted path
	 * @return the value resolved from the path, or {@code null}
	 */
	public static Object getProperty(OAObject oaObj, String propPath) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getProperty(oaObj, propPath);
	}

	/**
	 * Resolves the value of a property or nested property path using
	 * reflection, Hub navigation, and OAObject metadata. Supports path
	 * tokens, optional class-cast segments, and transitions between
	 * Hubs and OAObjects while walking the path.
	 *
	 * @param hubLast  a Hub that may supply context when evaluating
	 *                 calculated Hub-based getters
	 * @param oaObj    the current OAObject in the traversal
	 * @param propPath the property or dotted path to evaluate
	 * @return the final resolved value or {@code null} if unavailable
	 */
	public static Object getProperty(Hub hubLast, OAObject oaObj, String propPath) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getProperty(hubLast, oaObj, propPath);
	}


	/**
	 * Sets a property value on an {@link OAObject}, handling property-path
	 * navigation, primitive-null semantics, link updates, Hub assignment,
	 * type conversion, event firing, and reference resolution. When the
	 * value targets a MANY relationship, Hub-based logic is applied.
	 *
	 * @param oaObj    the target object
	 * @param propName the property name or path
	 * @param value    the new value (may be OAObject, OAObjectKey, or raw)
	 * @param fmt      optional formatter used for type conversion
	 */
	public static void setProperty(final OAObject oaObj, String propName, Object value, final String fmt) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().setProperty(oaObj, propName, value, fmt);
	}

	/**
	 * Stores a raw link value directly into an object's property store,
	 * converting non-OAObject values to {@link OAObjectKey} when the link
	 * is a ONE relationship. No events or reverse-link handling are
	 * performed.
	 *
	 * @param oaObj        the object whose link is updated
	 * @param propertyName the name of the link property
	 * @param value        the raw value or key to store
	 */
	public static void storeLinkValue(OAObject oaObj, String propertyName, Object value) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().storeLinkValue(oaObj, propertyName, value);
	}

	/**
	 * Determines whether a primitive property has its null flag set.
	 * This checks the object's internal null-tracking byte array and
	 * delegates to metadata to verify whether the given property is
	 * currently marked as representing a null primitive.
	 *
	 * @param oaObj        the object containing the property
	 * @param propertyName the property name
	 * @return {@code true} if the property represents a null primitive
	 */
	public static boolean getPrimitiveNull(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().getPrimitiveNull(oaObj, propertyName);
	}

	/**
	 * Sets or clears the null flag for a primitive property. This method
	 * delegates to the appropriate internal setter to mark the primitive
	 * as null or not without firing any property-change events.
	 *
	 * @param oaObj        the object whose property flag is updated
	 * @param propertyName the primitive property name
	 * @param bNull        {@code true} to set null, {@code false} to clear
	 */
	public static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bNull) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().setPrimitiveNull(oaObj, propertyName, bNull);
	}


	/**
	 * Retrieves an {@link OAObject} instance given a key or raw identifier.
	 * The lookup searches the cache first, then the server (when running
	 * as a client), and finally the datasource when needed.
	 *
	 * @param clazz the object's class type
	 * @param key   a key value or {@link OAObjectKey}
	 * @return the resolved object or {@code null} if not found
	 */
	public static OAObject getObject(Class clazz, Object key) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getObject(clazz, key);
	}

	/**
	 * Variant of {@link #getObject(Class, Object)} that uses a supplied
	 * {@link OAObjectInfo}. Ensures the key is an {@link OAObjectKey}
	 * before performing cache, server, or datasource retrieval.
	 *
	 * @param clazz the object's class type
	 * @param key   a raw identifier or {@link OAObjectKey}
	 * @param oi    metadata associated with the class
	 * @return the located {@link OAObject} or {@code null}
	 */
	public static OAObject getObject(Class clazz, Object key, OAObjectInfo oi) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getObject(clazz, key, oi);
	}

	/**
	 * Retrieves a MANY relationship as a Hub of referenced objects,
	 * optionally applying sort order, sequencing, autoMatch assignment,
	 * and server/client-specific behaviors. Loads data as needed and
	 * caches or wraps the Hub based on metadata rules.
	 *
	 * @param oaObj            the master object
	 * @param linkPropertyName link property name (case insensitive)
	 * @param sortOrder        sort expression or {@code null}
	 * @param bSequence        true to enable sequencing support
	 * @param hubMatch         optional Hub for autoMatch
	 * @return the reference Hub, possibly empty but never {@code null}
	 */
	public static Hub getReferenceHub(final OAObject oaObj, final String linkPropertyName, String sortOrder, boolean bSequence,
			Hub hubMatch) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getReferenceHub(oaObj, linkPropertyName, sortOrder, bSequence, hubMatch);
	}


	/**
	 * Returns the raw stored reference value for the specified link
	 * property without triggering loading. The result can be
	 * {@code null}, an {@link OAObjectKey}, an {@link OAObject},
	 * or a Hub containing either keys or objects.
	 *
	 * @param oaObj the object whose link is accessed
	 * @param name  the link property name
	 * @return the raw stored value
	 */
	public static Object getRawReference(OAObject oaObj, String name) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getRawReference(oaObj, name);
	}

	/**
	 * Determines whether the given object is referenced by any of its
	 * relationships. Scans all used link properties and checks for
	 * non-null references, Hubs, resolved objects, or reverse links.
	 *
	 * @param oaObj the object to inspect
	 * @return {@code true} if it is referenced, otherwise {@code false}
	 */
	public static boolean hasReference(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().hasReference(oaObj);
	}

	/**
	 * Returns the names of link properties whose referenced values have
	 * not yet been loaded. Includes or excludes calculated links based
	 * on the flag.
	 *
	 * @param obj          the target object
	 * @param bIncludeCalc true to include calculated links
	 * @return array of unloaded link property names, or {@code null}
	 */
	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getUnloadedReferences(obj, bIncludeCalc);
	}

	/**
	 * Variant of {@link #getUnloadedReferences(OAObject, boolean)} that
	 * excludes a specific property from consideration.
	 *
	 * @param obj                the object inspected
	 * @param bIncludeCalc       include calculated links if true
	 * @param exceptPropertyName property name to exclude
	 * @return array of unloaded link names, or {@code null}
	 */
	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName);
	}

	/**
	 * Returns unloaded reference-property names, optionally filtering out
	 * calculated links, a named exception, and links marked as large.
	 *
	 * @param obj                the object inspected
	 * @param bIncludeCalc       include calculated links if true
	 * @param exceptPropertyName property name to exclude
	 * @param bIncludeLarge      include large links if true
	 * @return array of unloaded reference names, or {@code null}
	 */
	public static String[] getUnloadedReferences(OAObject obj, boolean bIncludeCalc, String exceptPropertyName, boolean bIncludeLarge) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getUnloadedReferences(obj, bIncludeCalc, exceptPropertyName, bIncludeLarge);
	}

	/**
	 * Loads all reference properties for the given object, excluding
	 * calculated links. Delegates to {@code loadAllReferences(obj,false)}.
	 *
	 * @param obj the object whose references will be loaded
	 */
	public static void loadAllReferences(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadAllReferences(obj);
	}

	/**
	 * Loads all reference properties for each object contained in the
	 * Hub, excluding calculated links. Delegates to
	 * {@code loadAllReferences(hub,false)}.
	 *
	 * @param hub the Hub whose objects will have references loaded
	 */
	public static void loadAllReferences(Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadAllReferences(hub);
	}

	/**
	 * Loads all reference properties for each object in the Hub, optionally
	 * including calculated links. Creates a sibling helper while loading.
	 *
	 * @param hub          Hub containing objects to load
	 * @param bIncludeCalc true to include calculated links
	 */
	public static void loadAllReferences(Hub hub, boolean bIncludeCalc) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadAllReferences(hub, bIncludeCalc);
	}

	/**
	 * Loads all reference properties for the given object, optionally
	 * including calculated links. Equivalent to a single-level load.
	 *
	 * @param obj          the object to load
	 * @param bIncludeCalc include calculated links if true
	 */
	public static void loadAllReferences(OAObject obj, boolean bIncludeCalc) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadAllReferences(obj, bIncludeCalc);
	}

	/**
	 * Loads reference properties for the given object up to a maximum
	 * count. Respects calculated-link inclusion rules and uses metadata
	 * to determine whether a link is already loaded.
	 *
	 * @param obj          the object whose references are loaded
	 * @param bIncludeCalc include calculated links if true
	 * @param max          maximum number of references to load
	 */
	public static void loadReferences(OAObject obj, boolean bIncludeCalc, int max) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadReferences(obj, bIncludeCalc, max);
	}

	/**
	 * Determines whether all reference properties for the given object
	 * are fully loaded. Checks raw stored values, keys, Hub configurations,
	 * and server-side autoMatch requirements.
	 *
	 * @param obj          the object to check
	 * @param bIncludeCalc include calculated links if true
	 * @return {@code true} if all references are loaded
	 */
	public static boolean areAllReferencesLoaded(OAObject obj, boolean bIncludeCalc) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().areAllReferencesLoaded(obj, bIncludeCalc);
	}

	/**
	 * Loads reference properties of selected link types (ONE and/or MANY)
	 * for the given object. Increments and returns a count of loaded links.
	 *
	 * @param obj          the object whose references are loaded
	 * @param bOne         include ONE links if true
	 * @param bMany        include MANY links if true
	 * @param bIncludeCalc include calculated links if true
	 * @return number of loaded references
	 */
	public static int loadAllReferences(OAObject obj, boolean bOne, boolean bMany, boolean bIncludeCalc) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, bOne, bMany, bIncludeCalc);
	}

	/**
	 * Recursively loads reference properties up to a maximum depth.
	 *
	 * @param obj              the starting object
	 * @param maxLevelsToLoad  maximum recursive depth
	 * @return count of loaded references
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad);
	}

	/**
	 * Loads all reference properties for each object contained in the
	 * supplied Hub up to the specified maximum recursion depth. Uses
	 * the internal recursive reference loader with default settings
	 * for owned-reference levels, calculated-link inclusion, callback,
	 * cascade, and maximum reference count.
	 *
	 * @param hub              the Hub whose objects will have references loaded
	 * @param maxLevelsToLoad  the maximum depth of recursive loading
	 * @return the total number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad);
	}

	/**
	 * Loads reference properties for the given object up to the specified
	 * maximum recursion depth, including additional levels of owned links.
	 * Uses the internal recursive loader with defaults for calculated-link
	 * inclusion, callback, cascade, and maximum reference count.
	 *
	 * @param obj                        the starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad);
	}

	/**
	 * Loads reference properties for the given object up to a specified
	 * recursion depth and includes additional owned-reference levels.
	 * Limits the total number of references loaded to the supplied maximum.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for each object in the Hub up to the
	 * given recursion depth, including extra owned-reference levels.
	 * Uses default settings for calculated-link inclusion, callback,
	 * cascade, and maximum reference count.
	 *
	 * @param hub                        Hub containing objects
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @return number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad);
	}

	/**
	 * Loads reference properties for all objects in the Hub, respecting
	 * recursion depth and additional owned-reference levels while limiting
	 * the total number of references loaded.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, int maxRefsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the given object, optionally including
	 * calculated links, and using the supplied recursion and owned-link depth.
	 *
	 * @param obj                        the object to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
	}

	/**
	 * Loads reference properties for the given object with control over
	 * recursion depth, owned-link depth, calculated-link inclusion, and
	 * maximum references to load.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, 
				maxRefsToLoad);
	}

	/**
	 * Loads references for the given object with full control settings,
	 * including recursion depth, owned-link depth, calculated-link
	 * inclusion, maximum reference count, and a time limit for the load.
	 *
	 * @param obj                        the object to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @param maxEndTime                 time limit in milliseconds
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad, long maxEndTime) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, 
				maxRefsToLoad, maxEndTime);
	}

	/**
	 * Loads references for each object in the Hub with the specified
	 * recursion depth, owned-link depth, and optional calculated-link
	 * inclusion.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @return number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
	}

	/**
	 * Loads references for all objects in the Hub using recursion and
	 * owned-link-depth rules while limiting the maximum number of
	 * references loaded.
	 *
	 * @param hub                        Hub containing objects
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			int maxRefsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, maxRefsToLoad);
	}

	/**
	 * Loads references for the given object using recursion depth, owned
	 * levels, and optional calculated-link inclusion, calling the supplied
	 * callback before loading each object's references.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading references
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback);
	}

	/**
	 * Loads reference properties for the supplied object using the specified
	 * recursion depth, owned-link depth, calculated-link inclusion, and
	 * callback. Limits the total number of references loaded to the
	 * maximum supplied.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param maxRefsToLoad              maximum number of references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for all objects in the supplied Hub using
	 * the specified recursion depth, owned-link depth, calculated-link
	 * inclusion, and callback.
	 *
	 * @param hub                        Hub containing objects to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @return number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback);
	}

	/**
	 * Loads references for all objects in the Hub using recursion depth,
	 * owned-link depth, calculated-link inclusion, and callback rules,
	 * while enforcing a maximum number of references to load.
	 *
	 * @param hub                        Hub to process
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACallback callback, int maxRefsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the Hub beginning at a specified
	 * starting depth, applying recursion depth, owned-link depth,
	 * calculated-link inclusion, callback processing, and cascade rules.
	 * Creates and manages a sibling helper for the duration of the load.
	 *
	 * @param hub                        starting Hub
	 * @param levelsLoaded               initial number of levels already loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param cascade                    cascade manager used during loading
	 * @return number of references loaded
	 */
	public static int loadAllReferences(final Hub hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, cascade);
	}

	/**
	 * Loads reference properties for the Hub starting at a defined depth,
	 * applying recursion limits, owned-link depth, calculated-link rules,
	 * callback behavior, and cascade management, while enforcing a maximum
	 * number of references to load.
	 *
	 * @param hub                        the Hub being processed
	 * @param levelsLoaded               initial depth already loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(final Hub hub, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, int maxRefsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, cascade, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for all objects in the Hub according to
	 * recursion depth, owned-link depth, and calculated-link rules, using
	 * the supplied cascade for traversal.
	 *
	 * @param hub                        Hub being loaded
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param cascade                    cascade handler
	 * @return number of loaded references
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, cascade);
	}

	/**
	 * Loads references for all objects in the Hub using recursion depth,
	 * owned-link depth, calculated-link inclusion, and cascade management,
	 * while enforcing a maximum number of references to load.
	 *
	 * @param hub                        target Hub
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param cascade                    cascade handler
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of loaded references
	 */
	public static int loadAllReferences(Hub hub, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(hub, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, cascade, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the given object using recursion
	 * depth, owned-link depth, calculated-link inclusion, and callback
	 * behavior. Uses defaults for cascade and maximum reference count.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading each object
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, cascade);
	}

	/**
	 * Loads reference properties for the given object using recursion,
	 * owned-link depth, calculated-link inclusion, a callback, and a
	 * maximum reference count.
	 *
	 * @param obj                        starting object
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading
	 * @param maxRefsToLoad              maximum references to load
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc,
			OACascade cascade, int maxRefsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, cascade, maxRefsToLoad);
	}

	/**
	 * Loads reference properties for the Hub with the specified recursion
	 * depth, owned-link depth, calculated-link inclusion, and callback.
	 *
	 * @param hub                        Hub to load
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-link depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, cascade);
	}

	// ** MAIN reference loader here **
	/**
	 * Loads reference properties for the given object using recursion depth,
	 * owned-link depth, calculated-link inclusion, callback behavior, and cascade
	 * management. Limits the total number of references loaded to the specified
	 * maximum.
	 *
	 * @param obj                        the starting object
	 * @param levelsLoaded               number of previously loaded levels
	 * @param maxLevelsToLoad            maximum recursion depth
	 * @param additionalOwnedLevelsToLoad additional owned-reference depth
	 * @param bIncludeCalc               include calculated links if true
	 * @param callback                   invoked before loading references
	 * @param cascade                    cascade handler for traversal
	 * @param maxRefsToLoad              total max references allowed
	 * @return number of references loaded
	 */
	public static int loadAllReferences(OAObject obj, int levelsLoaded, int maxLevelsToLoad, int additionalOwnedLevelsToLoad,
			boolean bIncludeCalc, OACallback callback, OACascade cascade, final int maxRefsToLoad) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return 0;
		return g.objects().getOAObjectReflectService().loadAllReferences(obj, levelsLoaded, maxLevelsToLoad, additionalOwnedLevelsToLoad, 
				bIncludeCalc, callback, cascade, maxRefsToLoad);
	}

	
	/**
	 * Retrieves the blob value for a reference property. Attempts to return a
	 * previously loaded byte array when available. If the property has not been
	 * loaded, this method acquires a property lock and retrieves the blob either
	 * from the server (in client mode) or from the datasource (in server mode),
	 * then stores the result using CAS assignment.
	 *
	 * @param oaObj        the object whose reference blob is requested
	 * @param propertyName the name of the reference property
	 * @return the blob as a byte array, or null if unavailable
	 */
	public static byte[] getReferenceBlob(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getReferenceBlob(oaObj, propertyName);
	}

	/**
	 * Retrieves the referenced object for the specified link property. If the
	 * reference is already loaded and not an OAObjectKey, the existing value is
	 * returned. Otherwise this method acquires a property lock and delegates to
	 * the internal reference resolver. If a loaded result replaces a stored key,
	 * the property value is updated using CAS assignment.
	 *
	 * @param oaObj            the source object
	 * @param linkPropertyName the link property name
	 * @return the referenced OAObject or null
	 */
	public static Object getReferenceObject(final OAObject oaObj, final String linkPropertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getReferenceObject(oaObj, linkPropertyName);
	}


	/**
	 * Retrieves the OAObjectKey for a reference property without loading the
	 * referenced object. Uses the internally stored value, which may be an
	 * OAObjectKey, an OAObject (from which a key is derived), or null when no
	 * key is available. This method never triggers object loading.
	 *
	 * @param oaObj    the source object
	 * @param property the reference property name
	 * @return the stored OAObjectKey, a derived key from an OAObject, or null
	 */
	public static OAObjectKey getPropertyObjectKey(OAObject oaObj, String property) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getPropertyObjectKey(oaObj, property);
	}

	/**
	 * Determines whether the reference value for the given property has been
	 * loaded. This includes detecting stored nulls, OANotExist markers, loaded
	 * OAObjects, non-key Hubs, and OAObjectKeys that can be resolved from the
	 * cache. When a cached match is found for a key, the property value is
	 * updated using CAS assignment.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the reference is loaded or resolved, false otherwise
	 */
	public static boolean hasReferenceObjectBeenLoaded(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().hasReferenceObjectBeenLoaded(oaObj, propertyName);
	}

	/**
	 * Determines whether the reference property is null or explicitly marked as
	 * not existing. A stored null or OANotExist marker indicates that the reference
	 * is empty without requiring object loading.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the property is null or OANotExist, false otherwise
	 */
	public static boolean isReferenceObjectNullOrEmpty(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceObjectNullOrEmpty(oaObj, propertyName);
	}

	/**
	 * Determines whether the reference property is loaded and represents
	 * a non-empty value. Loaded OAObjects, non-key Hubs, and OAObjectKeys
	 * resolved from cache qualify as loaded and not empty. Null and
	 * OANotExist indicate empty or not loaded.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if loaded and non-empty
	 */
	public static boolean isReferenceObjectLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceObjectLoadedAndNotEmpty(oaObj, propertyName);
	}

	/**
	 * Determines whether a reference property is either null or not yet loaded.
	 * Null, OANotExist, or unresolved OAObjectKeys are treated as null or not
	 * loaded. Loaded OAObjects or Hubs return false.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property
	 * @return true if null or not loaded
	 */
	public static boolean isReferenceNullOrNotLoaded(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceNullOrNotLoaded(oaObj, propertyName);
	}

	/**
	 * Determines whether the reference property is null, not loaded, or
	 * represented by an empty Hub. A stored null or OANotExist marker,
	 * an unresolved OAObjectKey, or a Hub with zero elements will return
	 * true. Loaded OAObjects and non-empty Hubs return false.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the reference property name
	 * @return true if the reference is null, not loaded, or an empty Hub
	 */
	public static boolean isReferenceNullOrNotLoadedOrEmptyHub(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceNullOrNotLoadedOrEmptyHub(oaObj, propertyName);
	}

	/**
	 * Determines whether the MANY-relationship Hub for the specified
	 * property has been loaded. Evaluates the stored raw value and
	 * returns true only when the value is a Hub whose data has been
	 * fully loaded according to its internal load state.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub exists and is fully loaded
	 */
	public static boolean isReferenceHubLoaded(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceHubLoaded(oaObj, propertyName);
	}

	/**
	 * Determines whether the MANY-relationship Hub for the given property
	 * is both loaded and contains zero elements. A Hub qualifies only if
	 * it is fully loaded and its size is zero. Null, OANotExist, unresolved
	 * keys, and non-Hub values do not qualify.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub is loaded and empty
	 */
	public static boolean isReferenceHubLoadedAndEmpty(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceHubLoadedAndEmpty(oaObj, propertyName);
	}

	/**
	 * Determines whether the MANY-relationship Hub for the given property
	 * is both fully loaded and contains one or more elements. A Hub must
	 * be loaded and have a size greater than zero to qualify. Null,
	 * OANotExist, unresolved keys, and unloaded Hubs do not qualify.
	 *
	 * @param oaObj        the object inspected
	 * @param propertyName the MANY link property name
	 * @return true if the Hub is loaded and contains data
	 */
	public static boolean isReferenceHubLoadedAndNotEmpty(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectReflectService().isReferenceHubLoadedAndNotEmpty(oaObj, propertyName);
	}

	/**
	 * Loads the properties specified by the given property paths. Each path
	 * can reference a simple property or a dotted nested path. For each path,
	 * the method retrieves the corresponding value to ensure that it is
	 * loaded. No property-change events are fired by this method.
	 *
	 * @param oaObj         the object whose properties are to be loaded
	 * @param propertyPaths one or more property names or dotted paths
	 */
	public static void loadProperties(OAObject oaObj, String... propertyPaths) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadProperties(oaObj, propertyPaths);
	}

	/**
	 * Loads the properties specified by the given property paths for every
	 * object in the supplied Hub. Each path can refer to a simple property
	 * or a dotted nested path. For each object and each path, the property
	 * value is accessed to ensure it is loaded. No property-change events
	 * are fired by this method.
	 *
	 * @param hub           the Hub whose objects will have properties loaded
	 * @param propertyPaths one or more property names or dotted paths
	 */
	public static void loadProperties(Hub hub, String... propertyPaths) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return;
		g.objects().getOAObjectReflectService().loadProperties(hub, propertyPaths);
	}



	/**
	 * Creates a shallow copy of the supplied OAObject, excluding any
	 * properties listed in the excludeProperties array. A new instance
	 * of the same class is created, and each property not excluded is
	 * assigned using the source object's current values. Link properties
	 * are copied by reference without loading additional data.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @return the newly created copied object
	 */
	public static OAObject createCopy(OAObject oaObj, String[] excludeProperties) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().createCopy(oaObj, excludeProperties);
	}

	/**
	 * Creates a shallow copy of the supplied OAObject, excluding any
	 * properties listed in the excludeProperties array and allowing a
	 * callback to customize property-copy behavior. A new instance of
	 * the same class is created, and each non-excluded property is
	 * assigned from the source object's current value unless the
	 * callback overrides the assignment.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback to customize copying
	 * @return the newly created copied object
	 */
	public static OAObject createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().createCopy(oaObj, excludeProperties, copyCallback);
	}

	/**
	 * Internal implementation used to create a shallow copy of the supplied
	 * OAObject. A new instance is created, and each non-excluded property is
	 * copied from the source object unless overridden by the callback. The
	 * hmNew map is used to track objects that have already been copied to
	 * prevent duplication when copying graphs of related objects.
	 *
	 * @param oaObj            the source object to copy
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map used to track created copies
	 * @return the newly created copied object
	 */
	public static OAObject _createCopy(OAObject oaObj, String[] excludeProperties, OACopyCallback copyCallback,
			Map<UUID, Object> hmNew) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService()._createCopy(oaObj, excludeProperties, copyCallback, hmNew);
	}

	/**
	 * Copies the properties of the source OAObject into the supplied
	 * destination object. Properties listed in excludeProperties are
	 * skipped. For each non-excluded property, the current value from
	 * the source object is assigned to the destination unless the
	 * callback overrides or blocks the assignment. Link properties
	 * are copied by reference without triggering additional loading.
	 *
	 * @param oaObj            the source object whose values are copied
	 * @param newObject        the destination object
	 * @param excludeProperties property names to exclude from copying
	 * @param copyCallback     optional callback to customize copy behavior
	 */
	public static void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().copyInto(oaObj, newObject, excludeProperties, copyCallback);
	}

	/**
	 * Internal implementation used to copy property values from the source
	 * OAObject into the destination object. Properties listed in
	 * excludeProperties are skipped. For each non-excluded property, the
	 * current value from the source object is assigned to the destination
	 * unless the callback overrides the assignment. The hmNew map tracks
	 * objects already processed to prevent duplicate copying when copying
	 * object graphs.
	 *
	 * @param oaObj            the source object
	 * @param newObject        the destination object
	 * @param excludeProperties properties to exclude from copying
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map tracking objects already copied
	 */
	public static void copyInto(OAObject oaObj, OAObject newObject, String[] excludeProperties, OACopyCallback copyCallback,
			HashMap<UUID, Object> hmNew) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService().copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
	}

	/**
	 * Internal recursive implementation for copying property values from the
	 * source OAObject into the destination object. Excluded properties are
	 * skipped. For each non-excluded property, the current value from the
	 * source object is assigned to the destination unless the callback
	 * overrides or blocks the assignment. The hmNew map tracks objects that
	 * have already been processed to prevent duplicating work when copying
	 * object graphs.
	 *
	 * @param oaObj            the source object
	 * @param newObject        the destination object
	 * @param excludeProperties property names to exclude
	 * @param copyCallback     optional callback invoked during copying
	 * @param hmNew            map tracking already-copied objects
	 */
	public static void _copyInto(final OAObject oaObj, final OAObject newObject, final String[] excludeProperties,
			final OACopyCallback copyCallback, final Map<UUID, Object> hmNew) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectReflectService()._copyInto(oaObj, newObject, excludeProperties, copyCallback, hmNew);
	}

	public static Class getHubObjectClass(Method method) {
		Class cx = null;
		Type rt = method.getGenericReturnType();
		if (rt instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) rt;
			try {
				Type[] types = pt.getActualTypeArguments();
				if (types != null && types.length > 0 && types[0] instanceof Class) {
					cx = (Class) types[0];
				}
			} catch (Throwable t) {
			}
		}
		return cx;
	}

	/**
	 * Searches upward through the parent hierarchy of the two supplied
	 * OAObjects to find a common Hub. Each object’s parent chain is
	 * traversed up to the specified maximum number of levels, and the
	 * first Hub encountered in both hierarchies is returned.
	 *
	 * @param obj1             the first object
	 * @param obj2             the second object
	 * @param maxLevelsToCheck maximum number of parent levels to traverse
	 * @return the first common Hub found, or null if none exists
	 */
	public static Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int maxLevelsToCheck) {
		OAGraph g = getGraph(null, obj1);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().findCommonHierarchyHub(obj1, obj2, maxLevelsToCheck);
	}

	/**
	 * Recursive helper used to search for a common Hub in the parent
	 * hierarchies of the two supplied OAObjects. The search proceeds
	 * upward through each object's hierarchy while tracking the
	 * current recursion depth, stopping when the maximum number of
	 * levels has been reached or a common Hub is found.
	 *
	 * @param obj1             the first object
	 * @param obj2             the second object
	 * @param currentLevel     the current recursion level
	 * @param maxLevelsToCheck maximum allowed recursion depth
	 * @return the common Hub if found, otherwise null
	 */
	protected static Hub findCommonHierarchyHub(OAObject obj1, OAObject obj2, int currentLevel, int maxLevelsToCheck) {
		OAGraph g = getGraph(null, obj1);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().findCommonHierarchyHub(obj1, obj2, currentLevel, maxLevelsToCheck);
	}

	/**
	 * Determines how many parent-hierarchy levels separate the supplied
	 * OAObject from the specified Hub. The method walks upward through
	 * the object's parent chain up to the maximum number of levels and
	 * returns the number of levels required to reach the target Hub.
	 * Returns -1 if the Hub is not found within the allowed depth.
	 *
	 * @param findHub          the Hub being searched for
	 * @param fromObj          the starting object
	 * @param maxLevelsToCheck the maximum number of parent levels to traverse
	 * @return the number of levels to reach the Hub, or -1 if not found
	 */
	public static int getHierarchyLevelsToHub(Hub findHub, OAObject fromObj, int maxLevelsToCheck) {
		OAGraph g = getGraph(null, fromObj);
		if (g == null) return -1;
		return g.objects().getOAObjectReflectService().getHierarchyLevelsToHub(findHub, fromObj, maxLevelsToCheck);
	}

	/**
	 * Recursive helper that determines how many hierarchy levels separate
	 * the supplied OAObject from the target Hub. The search walks upward
	 * through the object's parent chain, incrementing the current recursion
	 * level until the Hub is found or the maximum depth is reached.
	 *
	 * @param findHub          the Hub being searched for
	 * @param fromObj          the starting object
	 * @param currentLevel     the current recursion depth
	 * @param maxLevelsToCheck the maximum number of levels allowed
	 * @return the number of levels to reach the Hub, or -1 if not found
	 */
	protected static int getHierarchyLevelsToHub(Hub findHub, OAObject fromObj, int currentLevel, int maxLevelsToCheck) {
		OAGraph g = getGraph(null, fromObj);
		if (g == null) return -1;
		return g.objects().getOAObjectReflectService().getHierarchyLevelsToHub(findHub, fromObj, currentLevel, maxLevelsToCheck);
	}


	/**
	 * Determines the property path from the supplied parent OAObject to the
	 * master object of the given child Hub. Traverses the links from the
	 * parent object to identify which property leads to the Hub. Returns
	 * null if no direct relationship path exists.
	 *
	 * @param objParent the parent OAObject
	 * @param hubChild  the child Hub
	 * @return the property path from the parent to the Hub, or null if none exists
	 */
	public static String getPropertyPathFromMaster(final OAObject objParent, final Hub hubChild) {
		OAGraph g = getGraph(null, objParent);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getPropertyPathFromMaster(objParent, hubChild);
	}

	/**
	 * Determines the object that should be displayed in the child Hub when
	 * navigating from the supplied parent Hub. Uses the given source object
	 * and the relationship between the two Hubs to locate the appropriate
	 * referenced object. Returns null when no matching object can be found.
	 *
	 * @param hubFrom    the parent Hub
	 * @param fromObject the object from which navigation begins
	 * @param hubChild   the child Hub whose display object is needed
	 * @return the object to display in the child Hub, or null if none applies
	 */
	public static Object getObjectToDisplay(final Hub hubFrom, Object fromObject, final Hub hubChild) {
		OAGraph g = getGraph(hubFrom, null);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getObjectToDisplay(hubFrom, fromObject, hubChild);
	}

	/**
	 * Determines the full property path that links the parent Hub to the
	 * child Hub. Examines the relationship between the master objects of
	 * the two Hubs and returns the property name or dotted path that
	 * connects them. Returns null if no direct relationship path exists.
	 *
	 * @param hubParent the parent Hub
	 * @param hubChild  the child Hub
	 * @return the property path between the two Hubs, or null if none exists
	 */
	public static String getPropertyPathBetweenHubs(final Hub hubParent, final Hub hubChild) {
		OAGraph g = getGraph(hubParent, null);
		if (g == null) return null;
		return g.objects().getOAObjectReflectService().getPropertyPathBetweenHubs(hubParent, hubChild);
	}

}

