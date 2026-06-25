package com.viaoa.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.oa.OAImpl;
import com.viaoa.object.OAObject;

/* qqqqqqqqqqqqq
CODEX

 #11 — cleanup-only / invariant risk
  File/class/method: src/main/java/com/viaoa/runtime/OARuntime.java:235
  Exact concern: public reset method is effectively a guarded no-op with old commented cache reset logic.
  Why it matters: OA 4.0 unit tests need deterministic runtime isolation. A reset method that does not
  reset runtime state can hide test contamination.
  Minimal fix: either implement a real runtime test reset or remove/rename this until ready.
  Suggested invariant ID/name: RUNTIME_TEST_RESET_RESTORES_CORE_SINGLETON_STATE
  Suggested test coverage: graph helper cache, datasource registry, thread-local state, and known runtime
  maps are reset or explicitly preserved.

#2 — bug
  File/class/method: src/main/java/com/viaoa/runtime/OARuntime.java:78, src/main/java/com/viaoa/runtime/
  OARuntime.java:183
  Concern: graphInternal(pkg) can cache default graph in hmPackageNameGraphHelper; if createGraph(pkg) later fails,
  the helper cache is not cleared. Future graph(pkg) returns cached default before checking hmPackageNameException.
  Why it matters: a failed graph package can silently continue using default graph after a previous lookup. That
  hides initialization failure and routes objects to the wrong graph.
  Severity: bug
  Minimal fix: clear hmPackageNameGraphHelper before graph creation attempt or in both success and failure paths.
  Check exception before helper lookup for exact package names.
  Suggested invariant: FAILED_GRAPH_CREATION_NEVER_FALLS_BACK_TO_DEFAULT
  Suggested test coverage: call graph("bad.pkg"), then force createGraph("bad.pkg") failure, then verify
  graph("bad.pkg") throws the cached failure instead of returning default.

 #3 — lifecycle risk
  File/class/method: src/main/java/com/viaoa/runtime/OARuntime.java:88
  Concern: failed graph initialization is cached permanently in hmPackageNameException with no retry/clear
  lifecycle.
  Why it matters: package scanning/classloading can fail because of classloader timing, generated classes, or test
  setup. Once poisoned, the package cannot recover without JVM restart.
  Severity: invariant risk
  Minimal fix: provide an explicit test/runtime reset path for graph exceptions, or make retry behavior deliberate
  and documented.
  Suggested invariant: GRAPH_INIT_FAILURE_CACHE_HAS_EXPLICIT_LIFECYCLE
  Suggested test coverage: failed graph init is either permanently sticky by contract or can be cleared by runtime
  reset.

 #4 — invariant risk
  File/class/method: src/main/java/com/viaoa/runtime/OARuntime.java:140
  Concern: subclasses of an OAObject subclass are canonicalized to the nearest superclass under OAObject, and cached
  in hmClassHelper.
  Why it matters: this is useful for proxies/enhanced classes, but it means a real model subclass in a different
  package can never resolve to its own package graph.
  Severity: invariant risk
  Minimal fix: make the rule explicit: either “subclasses always use root OAObject model class graph” or add a
  marker/hook to distinguish proxy/helper subclasses from real model subclasses.
  Suggested invariant: GRAPH_CLASS_CANONICALIZATION_IS_EXPLICIT
  Suggested test coverage: subclass in same package, subclass in different package, and proxy/helper subclass all
  resolve according to documented contract.


*/


public final class OARuntime {
	private static Logger LOG = Logger.getLogger(OARuntime.class.getName());

	private static OARuntime runtime = new OARuntime();
	
	private final Map<String, OA> hmPackageNameOA = new ConcurrentHashMap<>();
	private final Map<String, OA> hmPackageNameOAHelper = new ConcurrentHashMap<>();
	private final Map<String, RuntimeException> hmPackageNameException = new ConcurrentHashMap<>();
	private final Map<Class<?>, Class<?>> hmClassHelper = new ConcurrentHashMap<>();
	
	private OA oaCatchAll;
	private OA oaDefault;

	private final OADataSourceService srvcDataSource = new OADataSourceService();
	private final OAThreadService srvcThread = new OAThreadService();
	private final OAContextService srvcContext = new OAContextService();
	
	private OARuntime() {
	}
	
	static {
		runtime.oaCatchAll = (OAImpl) runtime.createOAInternal("");
	}
	
	public static OARuntime get() {
		return runtime;
	}

	public static OA createOA(final Package pkg) {
		return runtime.createOAInternal(pkg);
	}

	public static OA createDefaultOA(final Package pkg) {
		OA oa = runtime.createOAInternal(pkg);
		runtime.defaultOA(oa);
		return oa;
	}
	
	private OA createOAInternal(final Package pkg) {
		String pn;
		if (pkg != null) pn = pkg.getName();
		else pn = null;
		return createOAInternal(pn);
	}	
	
	public static OA createOA(final String pkgName) {
		return runtime.createOAInternal(pkgName);
	}
	
	private OA createOAInternal(final String pkgName) {
		if (pkgName == null) return null;

		OA oa = hmPackageNameOA.get(pkgName);
		if (oa != null) return oa;

		synchronized (hmPackageNameOA) {
			oa = hmPackageNameOA.get(pkgName);
			if (oa != null) return oa;
		
			RuntimeException exRt = hmPackageNameException.get(pkgName);
			if (exRt != null) throw exRt;
			
			oa = new OAImpl(pkgName) {
				@Override
				public void close() {
					super.close();
					hmPackageNameOAHelper.clear();
					hmPackageNameOA.remove(pkgName);
				}
			};
			try {
				((OAImpl) oa).initialize();
				hmPackageNameOAHelper.clear();
				hmPackageNameOA.put(pkgName, oa);
			} catch (ClassNotFoundException | IOException e) {
				RuntimeException ex = new RuntimeException("Could not initialize OA, package name is " + pkgName, e);
				hmPackageNameOA.remove(pkgName);
				hmPackageNameOAHelper.clear();
				hmPackageNameException.put(pkgName, ex);
				throw ex;
			}
		}
		return oa;
	}

	public static OA oa(final OAObject obj) {
		return runtime.oaInternal(obj);
	}	
	
	private OA oaInternal(final OAObject obj) {
		Class<?> c = obj == null ? null : obj.getClass();
		return oaInternal(c);
	}

	public static OA oa(final Hub hub) {
		return runtime.oaInternal(hub);
	}
	
	private OA oaInternal(final Hub hub) {
		Class<?> c = hub == null ? null : hub.getObjectClass();
		return oaInternal(c);
	}

	public static OA oa(final Hub hub, final OAObject obj) {
		return runtime.oaInternal(hub, obj);
	}
	
	private OA oaInternal(final Hub hub, final OAObject obj) {
		Class<?> c = hub == null ? null : hub.getObjectClass();
		if (c == null && obj != null) {
			c = obj.getClass();
		}
		return oaInternal(c);
	}
	
	public static OA oa(final Class<?> clazz) {
		return runtime.oaInternal(clazz);
	}
	
	private OA oaInternal(final Class<?> clazz) {
	    Class<?> classFound = clazz;

	    Class<?> classSuper = (classFound == null) ? null : classFound.getSuperclass();
	    if (classSuper != null && classSuper != OAObject.class) {
	        Class<?> cx = hmClassHelper.get(clazz);
	        if (cx != null) {
	            classFound = cx;
	        }
	        else {
	            for (;;) {
	                classSuper = classFound.getSuperclass();
	                if (classSuper == null) { 
	                	classFound = clazz; 
		                hmClassHelper.put(clazz, clazz);
	                	break; 
	                }
	                if (classSuper == OAObject.class) break;
	                classFound = classSuper;
	            }
	            if (classFound != clazz) {
	                hmClassHelper.put(clazz, classFound);
	            }
	        }
	    }

	    String pn = (classFound == null) ? "" : classFound.getPackage().getName();
	    return oaInternal(pn);
	}
	
	public static OA oa(final Package pkg) {
		return runtime.oaInternal(pkg);
	}
	
	private OA oaInternal(final Package pkg) {
		String pn = pkg == null ? null : pkg.getName();
		return oaInternal(pn);
	}	

	public static OA oa(String pkgName) {
		return runtime.oaInternal(pkgName);
	}
	
	private OA oaInternal(String pkgName) {
		if (pkgName == null) pkgName = "";

		OA oa = hmPackageNameOA.get(pkgName);
		if (oa != null) return oa;
		
		oa = hmPackageNameOAHelper.get(pkgName);
		if (oa != null) return oa;
		
		RuntimeException exRt = hmPackageNameException.get(pkgName);
		if (exRt != null) throw exRt;
		
		String fnd = null;
		for (String s : hmPackageNameOA.keySet()) {
			if (pkgName.equals(s) || pkgName.startsWith(s + ".")) {
				if (fnd == null || s.length() > fnd.length()) fnd = s;
			}
		}
		if (fnd != null) {
			oa = hmPackageNameOA.get(fnd);
			hmPackageNameOAHelper.put(pkgName, oa);
			return oa;
		}
		hmPackageNameOAHelper.put(pkgName, oaCatchAll);
		return oaCatchAll;
	}	


	/**
	 */
	public static OA oa() {
		if (runtime.oaDefault != null) return runtime.oaDefault;
		return runtime.oaInternal("");
	}

	public static OA defaultOA() {
		return runtime.oaDefault;
	}

	public static void defaultOA(OA og) {
		runtime.oaDefault = og;
	}
	
	public static OA catchAllOA() {
		return runtime.oaInternal("");
	}

	
	
	public static OAThreadService thread() {
		return runtime.srvcThread;
	}
	
	public static OADataSourceService datasource() {
		return runtime.srvcDataSource;
	}
		
	public static OAContextService context() {
		return runtime.srvcContext;
	}
	
	
	//qqqqqqq temporary	
	/**
	 * Enables or disables unit test mode. When enabled, certain operations
	 * such as {@link #resetCache()} are permitted; otherwise they will throw
	 * an exception. This flag is intended for internal testing only.
	 *
	 * @param b {@code true} to enable unit test mode, {@code false} to disable it
	 */
	public void setUnitTestMode(boolean b) {
		UnitTestMode = b;
	}
	//qqqqqqq temporary	
	private boolean UnitTestMode;
	
	//qqqqqqq temporary	
	/**
	 * Clears all cache data, listeners, select-all Hubs, and named Hubs.
	 * This operation is permitted only when unit test mode is enabled;
	 * otherwise, a {@link RuntimeException} is thrown.
	 *
	 * @throws RuntimeException if unit test mode is not enabled
	 */
	public void unitTestReset() {
//		LOG.warning("call to reset cache, UnitTestMode=" + UnitTestMode);
		if (!UnitTestMode) {
			throw new RuntimeException("Can only call reset cache if UnitTestMode is true");
		}
		/*qqqqqqqqqqq for each OG.cache
		objectCache.clearCache();
		hmCacheListener.clear();
		aiListenerCount.set(0);
		synchronized (hmCacheSelectAllHub) {
			hmCacheSelectAllHub.clear();
		}
		synchronized (hmCacheNamedHub) {
			hmCacheNamedHub.clear();
		}
		*/		
	}
	
}
