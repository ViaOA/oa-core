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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.viaoa.annotation.OAClass;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAReflect;
import com.viaoa.util.OAString;

/**
 * Internal delegate responsible for building and caching OAObjectInfo metadata
 * for each OAObject type. Metadata is discovered using reflection and augmented
 * by annotations and OABuilder model generation.
 *
 * <p>This delegate performs the one-time scan of a class to identify its
 * persistent and calculated properties, link relationships, primary key
 * properties, and lifecycle callback methods.</p>
 *
 * <p>The resulting OAObjectInfo instance is cached and reused for all objects
 * of the same type, enabling fast metadata lookups during runtime operations
 * such as lazy loading, change tracking, relationship updates, and UI binding.</p>
 *
 * <p>This metadata discovery is the foundation of OA's model-driven architecture.
 * It allows domain behavior to be configured declaratively in the model and
 * leveraged consistently throughout the Object Graph without requiring manual
 * registration or configuration.</p>
 *
 * @see OAObjectInfo
 * @see OALinkInfo
 * @see OAObject
 */
public class OAObjectInfoDelegate {

	/*
	OAGraph g = getGraph(null, oaObj);
	if (g == null) return;
	g.objects().getOAObjectInfoService().??(oaObj);
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
     * Returns the OAObjectInfo associated with the class of the supplied
     * OAObject. Delegates to {@link #getOAObjectInfo(Class)} using the
     * object's runtime class, or null if the object is null.
     *
     * @param obj the OAObject whose metadata is requested.
     * @return the OAObjectInfo for the object's class.
     */
	private static OAObjectInfo getOAObjectInfo(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOAObjectInfo(obj);
	}

	/**
	 * Convenience wrapper around {@link #getOAObjectInfo(OAObject)}.
	 *
	 * @param obj the OAObject whose metadata is requested.
	 * @return the OAObjectInfo for the object's class.
	 */
	private static OAObjectInfo getObjectInfo(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOAObjectInfo(obj);
	}

	/**
	 * Returns the OAObjectInfo associated with the supplied class.
	 * If the class is null, not an OAObject subclass, or OAObject itself,
	 * returns a placeholder OAObjectInfo based on String.class. Otherwise,
	 * checks the cache and delegates to the recursive builder when needed.
	 *
	 * @param clazz the class to retrieve metadata for.
	 * @return the corresponding OAObjectInfo instance.
	 */
	private static OAObjectInfo getOAObjectInfo(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOAObjectInfo(clazz);
	}



	/**
	 * Convenience wrapper around {@link #getOAObjectInfo(Class)}.
	 *
	 * @param clazz the class whose metadata is requested.
	 * @return the OAObjectInfo for the class.
	 */
	private static OAObjectInfo getObjectInfo(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOAObjectInfo(clazz);
	}


	/**
	 * Adds the supplied link definition to the OAObjectInfo. If a link with
	 * the same name already exists, it is removed before adding the new one.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param li     the link info to add.
	 */
	private static void addLinkInfo(OAObjectInfo thisOI, OALinkInfo li) {
		Class c = thisOI.getForClass();
		if (c == null) return;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return;
		g.objects().getOAObjectInfoService().addLinkInfo(thisOI, li);
	}

	/**
	 * Adds the supplied calculated-property metadata to the OAObjectInfo
	 * if it is not null.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param ci     the calculated-property info to add.
	 */
	private static void addCalcInfo(OAObjectInfo thisOI, OACalcInfo ci) {
		Class c = thisOI.getForClass();
		if (c == null) return;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return;
		g.objects().getOAObjectInfoService().addCalcInfo(thisOI, ci);
	}

	/**
	 * Looks up the calculated-property metadata by name within the
	 * OAObjectInfo. The comparison is case-insensitive.
	 *
	 * @param thisOI the OAObjectInfo to search.
	 * @param name   the calculated property name.
	 * @return the matching OACalcInfo, or null if not found.
	 */
	private static OACalcInfo getOACalcInfo(OAObjectInfo thisOI, String name) {
		Class c = thisOI.getForClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOACalcInfo(thisOI, name);
	}

	/**
	 * Returns the recursive link info for the specified type (ONE or MANY).
	 * Ensures recursive-link initialization occurs only once and then caches
	 * the result in the OAObjectInfo.
	 *
	 * @param thisOI the OAObjectInfo whose recursive link is requested.
	 * @param type   link type constant from OALinkInfo.
	 * @return the recursive link info, or null if none exists.
	 */
	private static OALinkInfo getRecursiveLinkInfo(OAObjectInfo thisOI, int type) {
		Class c = thisOI.getForClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getRecursiveLinkInfo(thisOI, type);
	}


	/**
	 * Returns the link that identifies this object’s owner, if any.
	 * A link qualifies when its reverse link exists, is used, is marked
	 * as owner, and is not a recursive self-link. Caches the result in
	 * the OAObjectInfo.
	 *
	 * @param thisOI the OAObjectInfo to examine.
	 * @return the owner link info, or null if none.
	 */
	private static OALinkInfo getLinkToOwner(OAObjectInfo thisOI) {
		Class c = thisOI.getForClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkToOwner(thisOI);
	}

	/**
	 * Sets the root Hub for all objects of this OAObjectInfo when
	 * the type is recursive and does not have an owner. Stores or
	 * removes the Hub from the root-hub cache.
	 *
	 * @param thisOI the OAObjectInfo to update.
	 * @param h      the root Hub to assign, or null to remove.
	 */
	private static void setRootHub(OAObjectInfo thisOI, Hub h) {
		Class c = thisOI.getForClass();
		if (c == null) return;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return;
		g.objects().getOAObjectInfoService().setRootHub(thisOI, h);
	}

	/**
	 * Returns the root Hub previously assigned to this OAObjectInfo,
	 * or null if none has been set.
	 *
	 * @param thisOI the OAObjectInfo whose root Hub is requested.
	 * @return the root Hub or null.
	 */
	private static Hub getRootHub(OAObjectInfo thisOI) {
		Class c = thisOI.getForClass();
		if (c == null) return null;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getRootHub(thisOI);
	}

	/**
	 * Attempts to cache the supplied Hub instance for the given link info.
	 * Validates cache rules, acquires the per-link write lock, and delegates
	 * to the internal cache method. Returns true if the Hub was accepted
	 * into the cache.
	 *
	 * @param li  the link info whose cache is used.
	 * @param hub the Hub instance to cache.
	 * @return true if the Hub was cached; false otherwise.
	 */
	private static boolean cacheHub(OALinkInfo li, final Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().cacheHub(li, hub);
	}


	// for testing
	/**
	 * Returns true if the supplied Hub is currently present in the cache
	 * associated with the given link info. Acquires the per-link read lock
	 * and checks the cached set for membership.
	 *
	 * @param li  the link info whose cache is examined.
	 * @param hub the Hub instance to check.
	 * @return true if cached; false otherwise.
	 */
	private static boolean isCached(OALinkInfo li, Hub hub) {
		OAGraph g = getGraph(hub, null);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isCached(li, hub);
	}

	/**
	 * Returns the reverse link information for the supplied link info,
	 * or null if the link has no reverse relationship.
	 *
	 * @param thisLi the link info.
	 * @return the reverse link info, or null.
	 */
	private static OALinkInfo getReverseLinkInfo(OALinkInfo thisLi) {
		Class c = thisLi.getToClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getReverseLinkInfo(thisLi);
	}

	/**
	 * Returns true if the supplied link and its reverse link both have
	 * type MANY, indicating a many-to-many relationship.
	 *
	 * @param thisLi the link info to evaluate.
	 * @return true if many-to-many.
	 */
	private static boolean isMany2Many(OALinkInfo thisLi) {
		Class c = thisLi.getToClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isMany2Many(thisLi);
	}

	/**
	 * Returns true if the supplied link and its reverse link both have
	 * type ONE, indicating a one-to-one relationship.
	 *
	 * @param thisLi the link info to evaluate.
	 * @return true if one-to-one.
	 */
	private static boolean isOne2One(OALinkInfo thisLi) {
		Class c = thisLi.getToClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isOne2One(thisLi);
	}

	/**
	 * Retrieves a method by name from the supplied class. Ensures that
	 * OAObjectInfo is initialized so that the method cache is populated,
	 * then performs a cached lookup.
	 *
	 * @param clazz      the class to search.
	 * @param methodName the method name.
	 * @return the matching Method, or null if not found.
	 */
	private static Method getMethod(Class clazz, String methodName) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getMethod(clazz, methodName);
	}

	/**
	 * Returns the getter Method associated with the supplied link info.
	 * Looks up the reverse link, obtains the target class, and retrieves
	 * the corresponding getter method for the link name.
	 *
	 * @param li the link info.
	 * @return the getter Method, or null.
	 */
	private static Method getMethod(OALinkInfo li) {
		Class c = li.getToClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getMethod(li);
	}

	/**
	 * Convenience wrapper around {@link #getMethod(OAObjectInfo, String, int)}
	 * using an argument count of -1 to indicate that any parameter count
	 * is acceptable.
	 *
	 * @param oi         the OAObjectInfo whose class is examined.
	 * @param methodName the method name to resolve.
	 * @return the matching Method, or null.
	 */
	private static Method getMethod(OAObjectInfo oi, String methodName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getMethod(oi, methodName);
	}

	/**
	 * Retrieves a method from the OAObjectInfo’s class by name and
	 * argument count. Uses cached lookup when possible, otherwise performs
	 * reflective resolution and updates the method cache.
	 *
	 * @param oi            the OAObjectInfo providing the class context.
	 * @param methodName    the method name (case-insensitive).
	 * @param argumentCount expected number of parameters, or -1 for any.
	 * @return the matching Method, or null.
	 */
	private static Method getMethod(OAObjectInfo oi, String methodName, int argumentCount) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getMethod(oi, methodName, argumentCount);
	}

	/**
	 * Retrieves a method from the OAObjectInfo’s class by name and a
	 * single parameter type. Checks cached entries first, then resolves
	 * reflectively and updates the cache.
	 *
	 * @param oi         the OAObjectInfo providing the class context.
	 * @param methodName the method name (case-insensitive).
	 * @param classParam the expected parameter type.
	 * @return the matching Method, or null.
	 */
	private static Method getMethod(OAObjectInfo oi, String methodName, final Class classParam) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getMethod(oi, methodName, classParam);
	}

	/**
	 * Stores the supplied method in the per-class method cache, ensuring
	 * accessibility is enabled for reflective invocation.
	 *
	 * @param clazz  the class whose cache is updated.
	 * @param method the method to store.
	 */
	private static void storeMethod(Class clazz, Method method) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return;
		g.objects().getOAObjectInfoService().storeMethod(clazz, method);
	}

	/**
	 * Returns all cached methods associated with the OAObjectInfo’s class.
	 * Extracts the values from the per-class method map and returns them
	 * as an array.
	 *
	 * @param oi the OAObjectInfo whose methods are requested.
	 * @return array of all cached methods.
	 */
	private static Method[] getAllMethods(OAObjectInfo oi) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getAllMethods(oi);
	}

	/**
	 * Returns the return type of the getter method for the named property
	 * within the supplied OAObjectInfo. Returns null if the getter is not
	 * found.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return the property’s class type, or null.
	 */
	private static Class getPropertyClass(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPropertyClass(oi, propertyName);
	}

	/**
	 * Returns the return type of the getter method for the named property
	 * on the supplied class. Returns null if the getter is not found.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the property name.
	 * @return the property’s class type, or null.
	 */
	private static Class getPropertyClass(Class clazz, String propertyName) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPropertyClass(clazz, propertyName);
	}

	/**
	 * Returns the target-class type of a hub property by locating the
	 * corresponding link info. Returns null if the link is not defined.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the hub-property name.
	 * @return the target class for the hub, or null.
	 */
	private static Class getHubPropertyClass(Class clazz, String propertyName) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getHubPropertyClass(clazz, propertyName);
	}

	/**
	 * Returns the link info defined for the supplied property name on the
	 * given class by retrieving the class’s OAObjectInfo and delegating to
	 * the link-info lookup.
	 *
	 * @param clazz        the class to inspect.
	 * @param propertyName the link-property name.
	 * @return the matching OALinkInfo, or null.
	 */
	private static OALinkInfo getLinkInfo(Class clazz, String propertyName) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkInfo(clazz, propertyName);
	}

	/**
	 * Returns the link info defined for the supplied property name within
	 * the given OAObjectInfo, using the OAObjectInfo’s internal lookup.
	 *
	 * @param oi           the OAObjectInfo to inspect.
	 * @param propertyName the link-property name.
	 * @return the matching OALinkInfo, or null.
	 */
	private static OALinkInfo getLinkInfo(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkInfo(oi, propertyName);
	}

	/**
	 * Returns all link infos that are marked as owned within the supplied
	 * OAObjectInfo.
	 *
	 * @param oi the OAObjectInfo to inspect.
	 * @return array of owned-link infos.
	 */
	private static OALinkInfo[] getOwnedLinkInfos(OAObjectInfo oi) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOwnedLinkInfos(oi);
	}

	// linkinfo that this object owns
	/**
	 * Returns all link infos that are marked as owned for the class of the
	 * supplied OAObject. Delegates to {@link #getOwndedLinkInfos(OAObjectInfo)}.
	 *
	 * @param obj the OAObject whose owned links are requested.
	 * @return array of owned-link infos.
	 */
	private static OALinkInfo[] getOwnedLinkInfos(OAObject obj) {
		OAGraph g = getGraph(null, obj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getOwnedLinkInfos(obj);
	}

	/**
	 * Finds the link info whose reference on the supplied object matches
	 * the provided Hub instance. Scans all used link infos and compares
	 * raw references retrieved from the object.
	 *
	 * @param oi         the OAObjectInfo describing the object.
	 * @param fromObject the object whose links are examined.
	 * @param hub        the Hub instance to match.
	 * @return the associated link info, or null.
	 */
	private static OALinkInfo getLinkInfo(OAObjectInfo oi, OAObject fromObject, Hub hub) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkInfo(oi, fromObject, hub);
	}

	/**
	 * Returns the link info that points from the source class to the
	 * target class by retrieving the source class’s OAObjectInfo and
	 * delegating to the class-level lookup.
	 *
	 * @param fromClass the source class.
	 * @param toClass   the target class.
	 * @return the matching link info, or null.
	 */
	private static OALinkInfo getLinkInfo(Class fromClass, Class toClass) {
		Class c = fromClass;
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkInfo(fromClass, toClass);
	}

	/**
	 * Returns the link info within the supplied OAObjectInfo whose target
	 * class matches the provided class. Only used link infos are examined.
	 *
	 * @param oi      the OAObjectInfo to inspect.
	 * @param toClass the target class.
	 * @return the matching link info, or null.
	 */
	private static OALinkInfo getLinkInfo(OAObjectInfo oi, Class toClass) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getLinkInfo(oi, toClass);
	}

	/**
	 * Returns the OAPropertyInfo for the named property from the supplied
	 * OAObjectInfo, using its internal lookup method.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return the property info, or null.
	 */
	private static OAPropertyInfo getPropertyInfo(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPropertyInfo(oi, propertyName);
	}

	/**
	 * Returns true if the supplied property name is listed among the
	 * OAObjectInfo's ID properties. Comparison is case-insensitive.
	 *
	 * @param oi           the OAObjectInfo to inspect.
	 * @param propertyName the property name.
	 * @return true if the property is an ID property.
	 */
	private static boolean isIdProperty(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isIdProperty(oi, propertyName);
	}

	/**
	 * Returns true if the supplied property info represents a primitive
	 * Java type. Validates that its class type is non-null and primitive.
	 *
	 * @param pi the property info.
	 * @return true if the property is primitive.
	 */
	private static boolean isPrimitive(OAPropertyInfo pi) {
		return (pi != null && pi.getClassType() != null && pi.getClassType().isPrimitive());
	}

	/**
	 * Returns true if the named property is a primitive type. Looks up the
	 * OAPropertyInfo and checks the underlying Java class for primitiveness.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return true if the property is primitive.
	 */
	private static boolean isPrimitiveProperty(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isPrimitiveProperty(oi, propertyName);
	}

	/**
	 * Returns true if the named property is a Hub property. Resolves the
	 * getter method and verifies that its return type is Hub.
	 *
	 * @param oi           the OAObjectInfo containing metadata.
	 * @param propertyName the property name.
	 * @return true if the property is a Hub property.
	 */
	private static boolean isHubProperty(OAObjectInfo oi, String propertyName) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isHubProperty(oi, propertyName);
	}

	/**
	 * Returns an array of ID property values for the supplied OAObject.
	 * Retrieves the ID-property list from the OAObjectInfo and extracts
	 * each value using raw property reflection.
	 *
	 * @param oaObj the OAObject whose ID values are requested.
	 * @return array of ID values; empty array if none; null if object is null.
	 */
	private static Object[] getPropertyIdValues(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPropertyIdValues(oaObj);
	}

	/**
	 * Returns the null-bitmask array from the supplied OAObject, or null
	 * if the object is null. The bitmask indicates which primitive
	 * properties are currently null.
	 *
	 * @param oaObj the OAObject to inspect.
	 * @return the object's null-bitmask array, or null.
	 */
	private static byte[] getNullBitMask(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getNullBitMask(oaObj);
	}

	/**
	 * Returns a list of primitive property names for the supplied OAObject
	 * class that support null tracking. Delegates to the OAObjectInfo to
	 * retrieve the primitive-property list.
	 *
	 * @param clazz the OAObject class to inspect.
	 * @return list of primitive property names, or null if class is null.
	 */
	private static List<String> getPrimitiveNullPropertyNames(Class<? extends OAObject> clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPrimitiveNullPropertyNames(clazz);
	}

	/**
	 * Returns a list of primitive property names whose null bit is set on
	 * the supplied OAObject. Determines bit positions using the OAObjectInfo’s
	 * primitive property list and inspects the object's null-bitmask.
	 *
	 * @param oaObj the OAObject to inspect.
	 * @return list of primitive property names marked as null, or null.
	 */
	private static List<String> getPrimitiveNullProperties(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getPrimitiveNullProperties(oaObj);
	}

	/**
	 * Convenience wrapper around {@link #isPrimitiveNull(OAObject, String)}
	 * that returns whether the specified primitive property is null.
	 *
	 * @param oaObj        the OAObject to inspect.
	 * @param propertyName the property name.
	 * @return true if the primitive property is null.
	 */
	private static boolean getPrimitiveNull(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().getPrimitiveNull(oaObj, propertyName);
	}

	/**
	 * Returns true if the specified primitive property on the supplied
	 * object is marked as null in the object's null-bitmask. Validates that
	 * the property supports null-tracking and checks its assigned bit.
	 *
	 * @param oaObj        the OAObject to inspect.
	 * @param propertyName the property name (case-insensitive).
	 * @return true if the primitive property is null; false otherwise.
	 */
	private static boolean isPrimitiveNull(OAObject oaObj, String propertyName) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isPrimitiveNull(oaObj, propertyName);
	}

	/**
	 * Sets or clears the null-bit for the specified primitive property on
	 * the supplied object. Computes the bit position based on the
	 * OAObjectInfo’s primitive-property list and updates the object's
	 * null-bitmask accordingly.
	 *
	 * @param oaObj        the OAObject whose bitmask is modified.
	 * @param propertyName the property name (case-insensitive).
	 * @param bSetToNull   true to mark the property as null; false to clear.
	 */
	private static void setPrimitiveNull(OAObject oaObj, String propertyName, boolean bSetToNull) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return;
		g.objects().getOAObjectInfoService().setPrimitiveNull(oaObj, propertyName, bSetToNull);
	}

	/*
	 * NOTE: 20100930 I started this to use for reversing from TreeNode to get path to top/root
	 * this wont work, unless the parent nodes are also used
	 * Take a property path that is "to" a class, and reverse it.
	 * Example: from a X class, the propPath "dept.manager.address.zipCode"
	 * where address.class would be the clazz; would return "manager.dept", used to get from an address to the dept.
	 */

	/**
	 * Reverses a property path by attempting to follow reverse link
	 * definitions from the supplied class. Tokenizes the path, builds a
	 * reversed version, then resolves each component through link
	 * relationships. Returns null if the reverse path cannot be
	 * determined.
	 *
	 * @param clazz        the starting class.
	 * @param propertyPath the forward property path.
	 * @return the reversed property path, or null.
	 */
	private static String reversePath(Class clazz, String propertyPath) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().reversePath(clazz, propertyPath);
	}

	/**
	 * Returns true if the supplied object is weak-referenceable based on
	 * its OAObjectInfo. Delegates to the OAObjectInfo-level evaluation.
	 *
	 * @param oaObj the OAObject to check.
	 * @return true if weak-referenceable.
	 */
	private static boolean isWeakReferenceable(OAObject oaObj) {
		OAGraph g = getGraph(null, oaObj);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isWeakReferenceable(oaObj);
	}

	/**
	 * Returns true if any parent link configuration indicates that objects
	 * of this type may be weak-referenceable. Delegates to the internal
	 * recursive evaluation.
	 *
	 * @param oi the OAObjectInfo to check.
	 * @return true if weak-referenceable.
	 */
	private static boolean isWeakReferenceable(OAObjectInfo oi) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isWeakReferenceable(oi);
	}


	/**
	 * Returns true if the supplied OAObjectInfo is configured to use a
	 * singleton Pojo, either directly or via owner-link traversal.
	 *
	 * @param oi the OAObjectInfo to inspect.
	 * @return true if the type uses a singleton Pojo.
	 */
	private static boolean isPojoSingleton(final OAObjectInfo oi) {
		Class c = oi.getForClass();
		OAGraph g = OARuntime.get().graph(c);
		if (g == null) return false;
		return g.objects().getOAObjectInfoService().isPojoSingleton(oi);
	}


	/**
	 * Returns the method-cache map for the supplied class, creating it if
	 * necessary. The cache stores methods keyed by their uppercase names.
	 *
	 * @param clazz the class whose method cache is requested.
	 * @return the method cache map.
	 */
	private static Map<String, Method> getClassMethodMap(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getClassMethodMap(clazz);
	}

	/**
	 * Returns the per-class set used to record method names that were
	 * previously searched for but not found. Creates the set if it does
	 * not already exist.
	 *
	 * @param clazz the class whose not-found map is requested.
	 * @return the not-found method-name set.
	 */
    private static Set<String> getClassMethodNotFoundMap(Class clazz) {
		OAGraph g = OARuntime.get().graph(clazz);
		if (g == null) return null;
		return g.objects().getOAObjectInfoService().getClassMethodNotFoundMap(clazz);
    }

    /**
     * Returns the global map that associates each Class with its
     * OAObjectInfo instance. This is the shared cache used for all
     * metadata lookups.
     *
     * @return the Class-to-OAObjectInfo map.
     */
    private static Map<Class, OAObjectInfo> getObjectInfoMap_XXX() {
    	// qqqqqqqqqqqqq cant get to OAObjectInfoService qqqqqqqqqqqqqq
    	//return hmObjectInfo;
    	return null;
    }
}
