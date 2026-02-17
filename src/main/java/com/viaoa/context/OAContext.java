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
package com.viaoa.context;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThread;
import com.viaoa.runtime.OAThreadImpl;
import com.viaoa.runtime.thread.OAThreadLocalService;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Provides the thread-scoped application context used by OA for permission
 * evaluation, user identity, and context-specific Hub and OAUserAccess
 * associations. <p>
 *
 * OAContext ties together the OA thread-local context mechanism
 * ({@link com.viaoa.object.OAThreadLocalDelegate}), the logged-in user
 * (represented as an {@link com.viaoa.object.OAObject}), and any
 * {@link com.viaoa.context.OAUserAccess} rules that determine visibility and
 * enabled/disabled permissions throughout an OAObject graph. <p>
 *
 * The context API allows callers to:
 * <ul>
 *   <li>Associate an OAObject with a context as the “current user”.</li>
 *   <li>Associate a Hub containing the user as the active object.</li>
 *   <li>Associate OAUserAccess instances with a context.</li>
 *   <li>Query for admin, super-admin, and “allow edit processed” permissions
 *       using pluggable property paths.</li>
 *   <li>Retrieve context-specific Hub, OAObject, or OAUserAccess values.</li>
 * </ul>
 *
 * All context-based associations are stored in thread-safe maps using
 * {@link WeakReference} so entries automatically expire when the context is no
 * longer referenced. The special server thread (no context set) receives
 * elevated default permissions when {@link com.viaoa.sync.OASync#callSyncIsServer()}
 * is true.
 *
 * OAContext acts as the bridge between the OA object graph, thread-local
 * execution state, and permission-enforcement mechanisms such as
 * {@link com.viaoa.object.OAObjectCallback}.
 */
public class OAContext {
	
	/**
	 * Map of context-key → Hub, stored using weak references so entries expire
	 * automatically when no longer referenced. Each Hub's active object represents
	 * the OAObject associated with that context.
	 */
	private static final ConcurrentHashMap<Object, WeakReference<Hub<? extends OAObject>>> hmContextHub = new ConcurrentHashMap<>();
	
	/**
	 * Map of context-key → OAUserAccess rules, stored using weak references.
	 * Enables context-specific permission evaluation.
	 */
	private static final ConcurrentHashMap<Object, WeakReference<OAUserAccess>> hmContextUserAccess = new ConcurrentHashMap<>();

	/**
	 * Special placeholder used when no thread-local context is provided. This
	 * distinguishes “no context provided” from an actual null key.
	 */
	private static final Object NullContext = new Object();

	// by default, these property names are in AppUser

	/**
	 * Property path used to determine whether the context user has admin rights.
	 * Defaults to "Admin".
	 */
	private static String adminPropertyPath = "Admin";
	
	/**
	 * Property path used to determine whether the context user has super-admin
	 * rights. Defaults to "SuperAdmin".
	 */
	private static String superAdminPropertyPath = "SuperAdmin";
	
	/**
	 * Property path used to determine whether the context user may edit processed
	 * objects. Defaults to "EditProcessed".
	 */
	private static String allowEditProcessedPropertyPath = "EditProcessed";

	
	/**
	 * Private constructor to prevent instantiation. OAContext exposes only static
	 * methods.
	 */
	private OAContext() {
	    // static methods only
	}
	
	/**
	 * Sets the property path used to determine whether a user may edit processed
	 * objects.
	 *
	 * @param pp property path to use
	 */
	public static void setAllowEditProcessedPropertyPath(String pp) {
		OAContext.allowEditProcessedPropertyPath = pp;
	}

	/**
	 * Returns the property path used for evaluating edit-processed permissions.
	 *
	 * @return property path
	 */
	public static String getAllowEditProcessedPropertyPath() {
		return OAContext.allowEditProcessedPropertyPath;
	}

	/**
	 * Returns whether the current thread's context permits editing processed
	 * objects. Delegates to {@link #getAllowEditProcessed(Object)}.
	 *
	 * @return true if permitted; false otherwise
	 */
	public static boolean getAllowEditProcessed() {
		return getAllowEditProcessed(OARuntime.thread().getContext());
	}

	/**
	 * Evaluates whether the specified context allows editing processed objects.
	 * Applies special server-thread logic when context is null.
	 *
	 * @param context context key; null is converted to NullContext
	 * @return true if permitted; false otherwise
	 */
	public static boolean getAllowEditProcessed(Object context) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
		
		// default for main server thread (context=null) is always true
		if (context == NullContext && og.syncInternal().isServer()) {
			if (oaObj == null) {
				return true;
			}
			if (OAString.isEmpty(allowEditProcessedPropertyPath)) {
				return true;
			}
		}

		if (oaObj == null) {
			return false;
		}
		if (OAString.isEmpty(allowEditProcessedPropertyPath)) {
			return false;
		}

		Object val = oaObj.getProperty(OAContext.allowEditProcessedPropertyPath);
		boolean b = OAConv.toBoolean(val);
		b = b || isSuperAdmin(context);
		return b;
	}

	/**
	 * Sets the property path used to determine whether the context user has
	 * admin rights.
	 *
	 * @param pp property path to use
	 */
	public static void setAdminPropertyPath(String pp) {
		OAContext.adminPropertyPath = pp;
	}

	/**
	 * Returns the property path used for admin-right evaluation.
	 *
	 * @return property path
	 */
	public static String getAdminPropertyPath() {
		return OAContext.adminPropertyPath;
	}

	/**
	 * Returns whether the current thread's context has admin rights.
	 *
	 * @return true if admin; false otherwise
	 */
	public static boolean isAdmin() {
		return isAdmin(OARuntime.thread().getContext());
	}

	/**
	 * Evaluates whether the specified context has admin rights. Applies special
	 * server-thread rules when context is null.
	 *
	 * @param context context key
	 * @return true if admin; false otherwise
	 */
	public static boolean isAdmin(Object context) {
		if (context == null) {
			context = NullContext;
		}

		if (OARuntime.thread().isAdmin()) {
			return true;
		}

		final OAObject oaObj = getContextObject(context);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);

		// default for main server thread (context=null) is always true
		if (context == NullContext && og.syncInternal().isServer()) {
			if (oaObj == null) {
				return true;
			}
			if (OAString.isEmpty(adminPropertyPath)) {
				return true;
			}
		}

		if (oaObj == null) {
			return false;
		}
		if (OAString.isEmpty(adminPropertyPath)) {
			return false;
		}

		Object val = oaObj.getProperty(OAContext.adminPropertyPath);
		boolean b = OAConv.toBoolean(val);
		b = b || isSuperAdmin(context);
		return b;
	}

	/**
	 * Sets the property path used to determine super-admin rights.
	 *
	 * @param pp property path
	 */
	public static void setSuperAdminPropertyPath(String pp) {
		OAContext.superAdminPropertyPath = pp;
	}

	/**
	 * Returns the property path used for evaluating super-admin rights.
	 *
	 * @return property path
	 */
	public static String getSuperAdminPropertyPath() {
		return OAContext.superAdminPropertyPath;
	}

	/**
	 * Returns whether the current thread’s context has super-admin rights.
	 *
	 * @return true if super-admin; false otherwise
	 */
	public static boolean isSuperAdmin() {
		Object context = OARuntime.thread().getContext();
		return isSuperAdmin(context);
	}

	/**
	 * Evaluates whether the specified context has super-admin rights.
	 *
	 * @param context context key; null converted to NullContext
	 * @return true if super-admin; false otherwise
	 */
	public static boolean isSuperAdmin(Object context) {
		if (OAString.isEmpty(superAdminPropertyPath)) {
			return false;
		}
		if (context == null) {
			context = NullContext;
		}
		OAObject oaObj = getContextObject(context);
		if (oaObj == null) {
			return false;
		}

		Object val = oaObj.getProperty(OAContext.superAdminPropertyPath);
		boolean b = OAConv.toBoolean(val);
		return b;
	}

	/**
	 * Determines whether the property at the given path for the current context
	 * equals the specified boolean value. Delegates to
	 * {@link #isEnabled(Object, String, boolean)}.
	 *
	 * @param pp property path
	 * @param bEqualTo required boolean value
	 * @return true if property equals bEqualTo; false otherwise
	 */
	public static boolean isEnabled(final String pp, final boolean bEqualTo) {
		Object context = OARuntime.thread().getContext();
		return isEnabled(context, pp, bEqualTo);
	}

	/**
	 * Determines whether the property at the given path for the specified context
	 * matches the required boolean value. Applies special server-thread defaults.
	 *
	 * @param context context key
	 * @param pp property path
	 * @param bEqualTo required boolean value
	 * @return true if enabled; false otherwise
	 */
	public static boolean isEnabled(Object context, final String pp, final boolean bEqualTo) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);

		// default for main server thread (context=null) is always true
		if (context == NullContext && og.syncInternal().isServer()) {
			if (oaObj == null) {
				return true;
			}
			if (OAString.isEmpty(pp)) {
				return true;
			}
		}

		if (oaObj == null) {
			return false;
		}
		if (OAString.isEmpty(pp)) {
			return false;
		}

		Object val = oaObj.getProperty(pp);
		boolean b = OAConv.toBoolean(val);
		b = (b == bEqualTo);
		b = b || isSuperAdmin(context);
		return b;
	}

	/**
	 * Associates an OAObject with the specified context. The object is wrapped
	 * inside a Hub whose active object represents the context user.
	 *
	 * @param context context key; null converted to NullContext
	 * @param obj OAObject representing the user; null removes the mapping
	 */
	public static void setContextObject(Object context, OAObject obj) {
		if (context == null) {
			context = NullContext;
		}
		if (obj == null) {
			hmContextHub.remove(context);
		} else {
			Hub h = new Hub();
			h.add(obj);
			h.setAO(obj);
			setContextHub(context, h);
		}
	}

	/**
	 * Alias for {@link #setContextObject(Object, OAObject)}.
	 *
	 * @param context context key
	 * @param obj OAObject to associate
	 */
	public static void setContext(Object context, OAObject obj) {
		setContextObject(context, obj);
	}

	/**
	 * Returns the OAObject associated with the current thread's context, or
	 * null if none.
	 *
	 * @return associated OAObject or null
	 */
	public static OAObject getContextObject() {
		Object context = OARuntime.thread().getContext();
		return getContextObject(context);
	}

	/**
	 * Returns the OAObject associated with the specified context by retrieving the
	 * context Hub's active object.
	 *
	 * @param context context key
	 * @return OAObject, or null
	 */
	public static OAObject getContextObject(Object context) {
		Hub<? extends OAObject> hub = getContextHub(context);
		if (hub == null) {
			return null;
		}
		return hub.getAO();
	}

	/**
	 * Associates a Hub with the specified context. The Hub’s active object
	 * represents the context user.
	 *
	 * @param context context key
	 * @param hub Hub to associate; null removes the mapping
	 */
	public static void setContextHub(Object context, Hub<? extends OAObject> hub) {
		if (context == null) {
			context = NullContext;
		}
		if (hub == null) {
			hmContextHub.remove(context);
		} else {
			if (hub.getAO() == null) {
				hub.setPos(0);
			}
			hmContextHub.put(context, new WeakReference(hub));
		}
	}

	/**
	 * Removes the Hub associated with the NullContext.
	 */
	public static void removeContextHub() {
		removeContextHub(NullContext);
	}

	/**
	 * Removes the Hub associated with the specified context.
	 *
	 * @param context context key
	 */
	public static void removeContextHub(Object context) {
		if (context == null) {
			context = NullContext;
		}
		hmContextHub.remove(context);
	}

	/**
	 * Removes all context information for the specified context key.
	 *
	 * @param context context key
	 */
	public static void removeContext(Object context) {
		removeContextHub(context);
	}

	/**
	 * Removes all context information for the NullContext.
	 */
	public static void removeContext() {
		removeContextHub(null);
	}

	/**
	 * Returns the Hub associated with the current thread's context.
	 *
	 * @return associated Hub, or null
	 */
	public static Hub<? extends OAObject> getContextHub() {
		Object context = OARuntime.thread().getContext();
		return getContextHub(context);
	}

	/**
	 * Returns the Hub associated with the specified context by dereferencing its
	 * weak reference.
	 *
	 * @param context context key
	 * @return Hub instance or null
	 */
	public static Hub<? extends OAObject> getContextHub(Object context) {
		if (context == null) {
			context = NullContext;
		}
		WeakReference<Hub<? extends OAObject>> ref = hmContextHub.get(context);
		if (ref == null) {
			return null;
		}
		return ref.get();
	}

	/**
	 * Associates an OAUserAccess rule object with the specified context.
	 *
	 * @param context context key
	 * @param ua OAUserAccess instance; null removes mapping
	 */
	public static void setContextUserAccess(Object context, OAUserAccess ua) {
		if (context == null) {
			context = NullContext;
		}
		if (ua == null) {
			hmContextUserAccess.remove(context);
		} else {
			hmContextUserAccess.put(context, new WeakReference(ua));
		}
	}

	/**
	 * Returns the OAUserAccess associated with the current thread's context.
	 *
	 * @return OAUserAccess or null
	 */
	public static OAUserAccess getContextUserAccess() {
		Object context = OARuntime.thread().getContext();
		return getContextUserAccess(context);
	}

	/**
	 * Returns the OAUserAccess associated with the specified context.
	 *
	 * @param context context key
	 * @return OAUserAccess instance or null
	 */
	public static OAUserAccess getContextUserAccess(Object context) {
		if (context == null) {
			context = NullContext;
		}
		WeakReference<OAUserAccess> ref = hmContextUserAccess.get(context);
		if (ref == null) {
			return null;
		}
		return ref.get();
	}

}
