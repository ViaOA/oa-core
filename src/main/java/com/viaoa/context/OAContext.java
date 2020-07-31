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
package com.viaoa.context;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Allows storing objects that are associated with a context 1: OAObject as the login user. 2: OAUserAccess Used by EditQuery and other code
 * to work with OAObject permissions.
 *
 * @author vvia
 */
public class OAContext {
	private static final ConcurrentHashMap<Object, WeakReference<Hub<? extends OAObject>>> hmContextHub = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Object, WeakReference<OAUserAccess>> hmContextUserAccess = new ConcurrentHashMap<>();

	private static final Object NullContext = new Object();

	// by default, this property is in AppUser
	private static String adminPropertyPath = "Admin";
	private static String superAdminPropertyPath = "SuperAdmin";
	private static String allowEditProcessedPropertyPath = "EditProcessed";

	/**
	 * Property path used to find the user property for allowing users to edit objects/properties/etc that are annotatied as processed.
	 * Defaults to "EditProcessed"
	 */
	public static void setAllowEditProcessedPropertyPath(String pp) {
		OAContext.allowEditProcessedPropertyPath = pp;
	}

	public static String getAllowEditProcessedPropertyPath() {
		return OAContext.allowEditProcessedPropertyPath;
	}

	/**
	 * Does the current thread have rights to edit processed objects.
	 */
	public static boolean getAllowEditProcessed() {
		Object context = OAThreadLocalDelegate.getContext();
		return getAllowEditProcessed(context);
	}

	/**
	 * Does the context have rights to edit processed objects.
	 */
	public static boolean getAllowEditProcessed(Object context) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);

		// default for main server thread (context=null) is always true
		if (context == NullContext && OASync.isServer()) {
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
	 * Property path used to find the user property for allowing users to have admin rights. Defaults to "Admin"
	 */
	public static void setAdminPropertyPath(String pp) {
		OAContext.adminPropertyPath = pp;
	}

	public static String getAdminPropertyPath() {
		return OAContext.adminPropertyPath;
	}

	/**
	 * Does the context have admin rights.
	 */
	public static boolean isAdmin() {
		Object context = OAThreadLocalDelegate.getContext();
		return isAdmin(context);
	}

	public static boolean isAdmin(Object context) {
		if (context == null) {
			context = NullContext;
		}

		if (OAThreadLocalDelegate.isAdmin()) {
			return true;
		}

		final OAObject oaObj = getContextObject(context);

		// default for main server thread (context=null) is always true
		if (context == NullContext && OASync.isServer()) {
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
	 * Property path used to find the user property for allowing users to have super admin rights. If true, then the user will have all
	 * EditQuery.allowed=true Defaults to "SuperAdmin"
	 */
	public static void setSuperAdminPropertyPath(String pp) {
		OAContext.superAdminPropertyPath = pp;
	}

	public static String getSuperAdminPropertyPath() {
		return OAContext.superAdminPropertyPath;
	}

	/**
	 * Does the context have super admin rights.
	 */
	public static boolean isSuperAdmin() {
		Object context = OAThreadLocalDelegate.getContext();
		return isSuperAdmin(context);
	}

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
	 * Check to see if context (user) property path is equal to bEqualTo.
	 */
	public static boolean isEnabled(final String pp, final boolean bEqualTo) {
		Object context = OAThreadLocalDelegate.getContext();
		return isEnabled(context, pp, bEqualTo);
	}

	public static boolean isEnabled(Object context, final String pp, final boolean bEqualTo) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);

		// default for main server thread (context=null) is always true
		if (context == NullContext && OASync.isServer()) {
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
	 * Associated an object value with a context.
	 *
	 * @param context is value used to lookup obj
	 * @param obj     object that is associated with context.
	 * @see OAThreadLocalDelegate#getContext()
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

	public static void setContext(Object context, OAObject obj) {
		setContextObject(context, obj);
	}

	/**
	 * Returns the object associated with the current thread local (or null) context.
	 */
	public static OAObject getContextObject() {
		Object context = OAThreadLocalDelegate.getContext();
		return getContextObject(context);
	}

	public static OAObject getContextObject(Object context) {
		Hub<? extends OAObject> hub = getContextHub(context);
		if (hub == null) {
			return null;
		}
		return hub.getAO();
	}

	/**
	 * Allows the value to be associated with a context, to be the ActiveObject in a hub.
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

	public static void removeContextHub() {
		removeContextHub(NullContext);
	}

	public static void removeContextHub(Object context) {
		if (context == null) {
			context = NullContext;
		}
		hmContextHub.remove(context);
	}

	public static void removeContext(Object context) {
		removeContextHub(context);
	}

	public static void removeContext() {
		removeContextHub(null);
	}

	public static Hub<? extends OAObject> getContextHub() {
		Object context = OAThreadLocalDelegate.getContext();
		return getContextHub(context);
	}

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
	 * Associated an OAUserAccess with a context.
	 *
	 * @see OAThreadLocalDelegate#getContext()
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

	public static OAUserAccess getContextUserAccess() {
		Object context = OAThreadLocalDelegate.getContext();
		return getContextUserAccess(context);
	}

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
