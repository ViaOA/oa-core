package com.viaoa.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.hub.Hub;
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
	
	private final Map<String, OAGraph> hmPackageNameGraph = new ConcurrentHashMap<>();
	private final Map<String, OAGraph> hmPackageNameGraphHelper = new ConcurrentHashMap<>();
	private final Map<String, RuntimeException> hmPackageNameException = new ConcurrentHashMap<>();
	private final Map<Class<?>, Class<?>> hmClassHelper = new ConcurrentHashMap<>();
	
	private OAGraphImpl graphDefault;

	private final OADataSourceService srvcDataSource = new OADataSourceService();
	private final OAThreadService srvcThread = new OAThreadService();
	
	private OARuntime() {
	}
	
	static {
		runtime.graphDefault = (OAGraphImpl) runtime.createGraphInternal("");
	}
	
	public static OARuntime get() {
		return runtime;
	}

	public static OAGraph createGraph(final Package pkg) {
		return runtime.createGraphInternal(pkg);
	}

	private OAGraph createGraphInternal(final Package pkg) {
		String pn;
		if (pkg != null) pn = pkg.getName();
		else pn = null;
		return createGraphInternal(pn);
	}	
	
	public static OAGraph createGraph(final String pkgName) {
		return runtime.createGraphInternal(pkgName);
	}
	
	private OAGraph createGraphInternal(final String pkgName) {
		if (pkgName == null) return null;

		OAGraph og = hmPackageNameGraph.get(pkgName);
		if (og != null) return og;

		synchronized (hmPackageNameGraph) {
			og = hmPackageNameGraph.get(pkgName);
			if (og != null) return og;
		
			RuntimeException exRt = hmPackageNameException.get(pkgName);
			if (exRt != null) throw exRt;
			
			og = new OAGraphImpl(pkgName);
			try {
				((OAGraphImpl) og).initialize();
				hmPackageNameGraphHelper.clear();
				hmPackageNameGraph.put(pkgName, og);
			} catch (ClassNotFoundException | IOException e) {
				RuntimeException ex = new RuntimeException("Could not initialize OAGraph, package name is " + pkgName, e);
				hmPackageNameGraph.remove(pkgName);
				hmPackageNameException.put(pkgName, ex);
				throw ex;
			}
		}
		return og;
	}

	public static OAGraph graph(final OAObject obj) {
		return runtime.graphInternal(obj);
	}	
	
	private OAGraph graphInternal(final OAObject obj) {
		Class<?> c = obj == null ? null : obj.getClass();
		return graphInternal(c);
	}

	public static OAGraph graph(final Hub hub) {
		return runtime.graphInternal(hub);
	}
	
	private OAGraph graphInternal(final Hub hub) {
		Class<?> c = hub == null ? null : hub.getObjectClass();
		return graphInternal(c);
	}

	public static OAGraph graph(final Hub hub, final OAObject obj) {
		return runtime.graphInternal(hub, obj);
	}
	
	private OAGraph graphInternal(final Hub hub, final OAObject obj) {
		Class<?> c = hub == null ? null : hub.getObjectClass();
		if (c == null && obj != null) {
			c = obj.getClass();
		}
		return graphInternal(c);
	}
	
	public static OAGraph graph(final Class<?> clazz) {
		return runtime.graphInternal(clazz);
	}
	
	private OAGraph graphInternal(final Class<?> clazz) {
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
	    return graphInternal(pn);
	}
	
	public static OAGraph graph(final Package pkg) {
		return runtime.graphInternal(pkg);
	}
	
	private OAGraph graphInternal(final Package pkg) {
		String pn = pkg == null ? null : pkg.getName();
		return graphInternal(pn);
	}	

	public static OAGraph graph(String pkgName) {
		return runtime.graphInternal(pkgName);
	}
	
	private OAGraph graphInternal(String pkgName) {
		if (pkgName == null) pkgName = "";

		OAGraph og = hmPackageNameGraph.get(pkgName);
		if (og != null) return og;
		
		og = hmPackageNameGraphHelper.get(pkgName);
		if (og != null) return og;
		
		RuntimeException exRt = hmPackageNameException.get(pkgName);
		if (exRt != null) throw exRt;
		
		String fnd = null;
		for (String s : hmPackageNameGraph.keySet()) {
			if (pkgName.equals(s) || pkgName.startsWith(s + ".")) {
				if (fnd == null || s.length() > fnd.length()) fnd = s;
			}
		}
		if (fnd != null) {
			og = hmPackageNameGraph.get(fnd);
			hmPackageNameGraphHelper.put(pkgName, og);
			return og;
		}
		hmPackageNameGraphHelper.put(pkgName, graphDefault);
		return graphDefault;
	}	


	/**
	 * same as default graph
	 */
	public static OAGraph graph() {
		return runtime.graphInternal("");
	}

	public static OAGraph defaultGraph() {
		return runtime.graphInternal("");
	}

	public static OAThreadService thread() {
		return runtime.srvcThread;
	}
	
	public static OADataSourceService datasource() {
		return runtime.srvcDataSource;
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
