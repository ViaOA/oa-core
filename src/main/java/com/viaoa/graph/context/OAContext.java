package com.viaoa.graph.context;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.converter.OAConv;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/*qqqqqqqqqq
CODEX
#1
  file/class/method: src/main/java/com/viaoa/graph/context/OAContext.java:401 OAContext.removeContext(Object),
  removeContext()
  exact concern: removeContext(null) normalizes null for the Hub map, then calls hmContextUserAccess.remove(context)
  with context == null, which throws NullPointerException on ConcurrentHashMap. removeContext() also only removes
  the Hub mapping and leaves NullContext user-access behind.
  why it matters: context cleanup is not deterministic; access rules can leak for the default/null context, and
  valid cleanup with null can fail at runtime.
  severity: bug
  minimal fix: normalize context once before removing from both maps; make removeContext() delegate to
  removeContext(null).
  suggested invariant ID/name: CTX-REMOVE-NULL-CLEARS-ALL
  suggested test coverage: set context Hub and OAUserAccess for null context, call both removeContext() and
  removeContext(null), assert no exception and both maps resolve null afterward.

 #2
  file/class/method: src/main/java/com/viaoa/graph/context/OAContext.java:306, src/main/java/com/viaoa/graph/
  context/OAContext.java:363, src/main/java/com/viaoa/graph/context/OAContext.java:447
  exact concern: context Hub and OAUserAccess are stored only through WeakReference. setContextObject creates a new
  local Hub and stores no strong reference in OAContext.
  why it matters: context identity and access rules can disappear after GC while the context key still exists. That
  makes permission checks nondeterministic across requests/threads and is risky for server/runtime access semantics.
  severity: invariant risk
  minimal fix: decide the ownership contract explicitly. If context registration means “active until removed,” store
  strong references. If weak ownership is intentional, require caller-owned strong references and document/test that
  contract.
  suggested invariant ID/name: CTX-ACCESS-LIFETIME-DETERMINISTIC
  suggested test coverage: register context object/access, force GC pressure, verify whether access must remain or
  may expire according to the chosen contract.




*/

public class OAContext {

	/**
	 * Map of context-key → Hub, stored using weak references so entries expire
	 * automatically when no longer referenced. Each Hub's active object represents
	 * the OAObject associated with that context.
	 */
	private final ConcurrentHashMap<Object, WeakReference<Hub<? extends OAObject>>> hmContextHub = new ConcurrentHashMap<>();
	
	/**
	 * Map of context-key → OAUserAccess rules, stored using weak references.
	 * Enables context-specific permission evaluation.
	 */
	private final ConcurrentHashMap<Object, WeakReference<OAUserAccess>> hmContextUserAccess = new ConcurrentHashMap<>();

	/**
	 * Special placeholder used when no thread-local context is provided. This
	 * distinguishes “no context provided” from an actual null key.
	 */
	private final Object NullContext = new Object();

	// by default, these property names are in AppUser

	/**
	 * Property path used to determine whether the context user has admin rights.
	 * Defaults to "Admin".
	 */
	private String adminPropertyPath = "Admin";
	
	/**
	 * Property path used to determine whether the context user has super-admin
	 * rights. Defaults to "SuperAdmin".
	 */
	private String superAdminPropertyPath = "SuperAdmin";
	
	/**
	 * Property path used to determine whether the context user may edit processed
	 * objects. Defaults to "EditProcessed".
	 */
	private String allowEditProcessedPropertyPath = "EditProcessed";

	
	/**
	 * Sets the property path used to determine whether a user may edit processed
	 * objects.
	 *
	 * @param pp property path to use
	 */
	public void setAllowEditProcessedPropertyPath(String pp) {
		allowEditProcessedPropertyPath = pp;
	}

	/**
	 * Returns the property path used for evaluating edit-processed permissions.
	 *
	 * @return property path
	 */
	public String getAllowEditProcessedPropertyPath() {
		return allowEditProcessedPropertyPath;
	}

	/**
	 * Returns whether the current thread's context permits editing processed
	 * objects. Delegates to {@link #getAllowEditProcessed(Object)}.
	 *
	 * @return true if permitted; false otherwise
	 */
	public boolean getAllowEditProcessed() {
		return getAllowEditProcessed(OARuntime.thread().getContext());
	}

	/**
	 * Evaluates whether the specified context allows editing processed objects.
	 * Applies special server-thread logic when context is null.
	 *
	 * @param context context key; null is converted to NullContext
	 * @return true if permitted; false otherwise
	 */
	public boolean getAllowEditProcessed(Object context) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);
		
		// default for main server thread (context=null) is always true
		if (context == NullContext && !og.syncInternal().isClient()) {
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

		Object val = oaObj.getProperty(allowEditProcessedPropertyPath);
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
	public void setAdminPropertyPath(String pp) {
		adminPropertyPath = pp;
	}

	/**
	 * Returns the property path used for admin-right evaluation.
	 *
	 * @return property path
	 */
	public String getAdminPropertyPath() {
		return adminPropertyPath;
	}

	/**
	 * Returns whether the current thread's context has admin rights.
	 *
	 * @return true if admin; false otherwise
	 */
	public boolean isAdmin() {
		return isAdmin(OARuntime.thread().getContext());
	}

	/**
	 * Evaluates whether the specified context has admin rights. Applies special
	 * server-thread rules when context is null.
	 *
	 * @param context context key
	 * @return true if admin; false otherwise
	 */
	public boolean isAdmin(Object context) {
		if (context == null) {
			context = NullContext;
		}

		if (OARuntime.thread().isAdmin()) {
			return true;
		}

		final OAObject oaObj = getContextObject(context);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);

		// default for main server thread (context=null) is always true
		if (context == NullContext && !og.syncInternal().isClient()) {
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

		Object val = oaObj.getProperty(adminPropertyPath);
		boolean b = OAConv.toBoolean(val);
		b = b || isSuperAdmin(context);
		return b;
	}

	/**
	 * Sets the property path used to determine super-admin rights.
	 *
	 * @param pp property path
	 */
	public void setSuperAdminPropertyPath(String pp) {
		superAdminPropertyPath = pp;
	}

	/**
	 * Returns the property path used for evaluating super-admin rights.
	 *
	 * @return property path
	 */
	public String getSuperAdminPropertyPath() {
		return superAdminPropertyPath;
	}

	/**
	 * Returns whether the current thread’s context has super-admin rights.
	 *
	 * @return true if super-admin; false otherwise
	 */
	public boolean isSuperAdmin() {
		Object context = OARuntime.thread().getContext();
		return isSuperAdmin(context);
	}

	/**
	 * Evaluates whether the specified context has super-admin rights.
	 *
	 * @param context context key; null converted to NullContext
	 * @return true if super-admin; false otherwise
	 */
	public boolean isSuperAdmin(Object context) {
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

		Object val = oaObj.getProperty(superAdminPropertyPath);
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
	public boolean isEnabled(final String pp, final boolean bEqualTo) {
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
	public boolean isEnabled(Object context, final String pp, final boolean bEqualTo) {
		if (context == null) {
			context = NullContext;
		}

		final OAObject oaObj = getContextObject(context);
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(oaObj);

		// default for main server thread (context=null) is always true
		if (context == NullContext && !og.syncInternal().isClient()) {
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
	 * @param context context key; null is converted to NullContext
	 * @param obj OAObject representing the user; null removes the mapping
	 */
	public void setContextObject(Object context, OAObject obj) {
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
	public void setContext(Object context, OAObject obj) {
		setContextObject(context, obj);
	}

	/**
	 * Returns the OAObject associated with the current thread's context, or
	 * null if none.
	 *
	 * @return associated OAObject or null
	 */
	public OAObject getContextObject() {
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
	public OAObject getContextObject(Object context) {
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
	public void setContextHub(Object context, Hub<? extends OAObject> hub) {
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
	public void removeContextHub() {
		removeContextHub(NullContext);
	}

	/**
	 * Removes the Hub associated with the specified context.
	 *
	 * @param context context key
	 */
	public void removeContextHub(Object context) {
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
	public void removeContext(Object context) {
		removeContextHub(context);
		if (context != null) hmContextUserAccess.remove(context);
	}

	/**
	 * Removes all context information for the NullContext.
	 */
	public void removeContext() {
		removeContextHub(null);
	}

	/**
	 * Returns the Hub associated with the current thread's context.
	 *
	 * @return associated Hub, or null
	 */
	public Hub<? extends OAObject> getContextHub() {
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
	public Hub<? extends OAObject> getContextHub(Object context) {
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
	public void setContextUserAccess(Object context, OAUserAccess ua) {
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
	public OAUserAccess getContextUserAccess() {
		Object context = OARuntime.thread().getContext();
		return getContextUserAccess(context);
	}

	/**
	 * Returns the OAUserAccess associated with the specified context.
	 *
	 * @param context context key
	 * @return OAUserAccess instance or null
	 */
	public OAUserAccess getContextUserAccess(Object context) {
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
