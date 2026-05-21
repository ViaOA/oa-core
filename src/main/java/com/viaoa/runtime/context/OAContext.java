package com.viaoa.runtime.context;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.object.OAObject;

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


CTX-USER-001 — Null Context User Represents System Authority Unless User Context Is Required
Contract statement:
When OAThreadLocal has no OAContextUser, runtime access checks execute as system/runtime authority unless the current 
runtime context or application configuration requires a user context. User-facing execution scopes must explicitly 
install an OAContextUser to avoid system-authority fallback.


MEDIUM — weak OAContextUser ownership can make registered users disappear
  src/main/java/com/viaoa/runtime/context/OAContext.java:60 stores context users as WeakReference. If the context
  registration is intended to own the session/request user binding, access behavior can become GC-dependent. This is
  acceptable only if the contract is explicitly “caller/thread/session owns the strong reference; OAContext only
  indexes it weakly.”


*/


/**
 * 
 * @param <KEYTYPE> type of object used as key.  example: OASession
 * @param <OBJTYPE> type of OAObject to use.  example: Employee
 */
public class OAContext<KEYTYPE, OBJTYPE extends OAObject> {
	final private Object key;
	final private OAContextAccess contextAccess;

    private final Map<KEYTYPE, WeakReference<OAContextUser<OBJTYPE>>> hmContextUser = new ConcurrentHashMap<>();
	
	// by default, these property names are in AppUser

	/**
	 * Property path used to determine whether the context user has admin rights.
	 * Defaults to "Admin".
	 */
	private String adminPath = "Admin";

	/**
	 * Property path used to determine whether the context user has super-admin
	 * rights. Defaults to "SuperAdmin".
	 */
	private String superAdminPath = "SuperAdmin";
	
	/**
	 * Property path used to determine whether the context user may edit processed
	 * objects. Defaults to "EditProcessed".
	 */
	private String allowEditProcessedPath = "EditProcessed";

	
	public OAContext(KEYTYPE key, OAContextAccess contextAccess) {
		this.key = key;
		this.contextAccess = contextAccess;
	}

	public Object getKey() {
		return this.key;
	}

	public OAContextAccess getContextAccess() {
		return this.contextAccess;
	}

    public OAContextUser<OBJTYPE> getContextUser(KEYTYPE key) {
    	if (key == null) return null;
    	WeakReference<OAContextUser<OBJTYPE>> wf = hmContextUser.get(key);
    	if (wf == null) return null;
    	OAContextUser<OBJTYPE> cu = wf.get();
    	if (cu == null) hmContextUser.remove(key);
    	return cu;
    }
    
    public void addContextUser(KEYTYPE key, OAContextUser<OBJTYPE> cu) {
    	if (key == null) return;
    	if (cu == null) hmContextUser.remove(key);
    	else hmContextUser.put(key, new WeakReference<>(cu));
    }
	
	
	/**
	 * Sets the property path used to determine whether a user may edit processed
	 * objects.
	 *
	 * @param pp property path to use
	 */
	public void setAllowEditProcessedPath(String pp) {
		allowEditProcessedPath = pp;
	}

	/**
	 * Returns the property path used for evaluating edit-processed permissions.
	 *
	 * @return property path
	 */
	public String getAllowEditProcessedPath() {
		return allowEditProcessedPath;
	}

	
	/**
	 * Sets the property path used to determine whether the context user has
	 * admin rights.
	 *
	 * @param pp property path to use
	 */
	public void setAdminPath(String pp) {
		adminPath = pp;
	}

	/**
	 * Returns the property path used for admin-right evaluation.
	 *
	 * @return property path
	 */
	public String getAdminPath() {
		return adminPath;
	}

	
	 /* Sets the path used to determine super-admin rights.
	 *
	 * @param pp property path
	 */
	public void setSuperAdminPath(String pp) {
		superAdminPath = pp;
	}

	/**
	 * Returns the property path used for evaluating super-admin rights.
	 *
	 * @return property path
	 */
	public String getSuperAdminPath() {
		return superAdminPath;
	}
}
