package com.viaoa.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphImpl;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.datasource.OADataSourceService;

public final class OARuntime {
	private static Logger LOG = Logger.getLogger(OARuntime.class.getName());

	private static OARuntime runtime = new OARuntime();
	
	private final Map<String, OAGraph> hmPackageGraph = new ConcurrentHashMap<>();
	private final Map<String, OAGraph> hmPackageGraph2 = new ConcurrentHashMap<>();
	private final Map<String, RuntimeException> hmRuntimeException = new ConcurrentHashMap<>();
	private final Map<Class<?>, Class<?>> hmClass = new ConcurrentHashMap<>();
	
	private final OAGraph graphDefault;

	private final OAThread thread;
	private final OADataSource dataSource;
	private final OAContext context;
	
	
	private OARuntime() {
		this.thread = new OAThreadImpl();
		this.dataSource = new OADataSourceImpl();
		

		this.context = new OAContextImpl();
		
		
		graphDefault = new OAGraphImpl(this, null);
		try {
			((OAGraphImpl)graphDefault).init();
		}
		catch (Exception e) {}
	}
	
	public static OARuntime get() {
		return runtime;
	}

	public static OAGraph createGraph(final Package pkg) {
		return runtime.createGraphInternal(pkg);
	}
	private OAGraph createGraphInternal(final Package pkg) {
		if (pkg == null) return null;
		final String pn = pkg.getName();
		return createGraphInternal(pn);
	}	
	
	public static OAGraph createGraph(final String pkgName) {
		return runtime.createGraphInternal(pkgName);
	}
	private OAGraph createGraphInternal(final String pkgName) {
		if (pkgName == null) return null;

		OAGraph og = hmPackageGraph.get(pkgName);
		if (og != null) return og;

		RuntimeException exRt = hmRuntimeException.get(pkgName);
		if (exRt != null) throw exRt;
		
		og = new OAGraphImpl(this, pkgName);
		hmPackageGraph.put(pkgName, og);
		hmPackageGraph2.clear();
			
		try {
			((OAGraphImpl)og).init();
		} catch (ClassNotFoundException | IOException e) {
			RuntimeException ex = new RuntimeException("Could not initialize OAGraph, package name is " + pkgName, e);
			hmPackageGraph.remove(pkgName);
			hmRuntimeException.put(pkgName, ex);
			throw ex;
		}
		return og;
	}

	public static OAGraph graph(final OAObject obj) {
		return runtime.graphInternal(obj);
	}	
	private OAGraph graphInternal(final OAObject obj) {
		Class c = obj == null ? null : obj.getClass();
		return graphInternal(c);
	}

	public static OAGraph graph(final Hub hub) {
		return runtime.graphInternal(hub);
	}
	private OAGraph graphInternal(final Hub hub) {
		Class c = hub == null ? null : hub.getObjectClass();
		return graphInternal(c);
	}

	public static OAGraph graph(final Hub hub, final OAObject obj) {
		return runtime.graphInternal(hub, obj);
	}
	private OAGraph graphInternal(final Hub hub, final OAObject obj) {
		Class c = hub == null ? null : hub.getObjectClass();
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
	        Class<?> cx = hmClass.get(clazz);
	        if (cx != null) {
	            classFound = cx;
	        }
	        else {
	            for (;;) {
	                classSuper = classFound.getSuperclass();
	                if (classSuper == null) { 
	                	classFound = clazz; 
		                hmClass.put(clazz, clazz);
	                	break; 
	                }
	                if (classSuper == OAObject.class) break;
	                classFound = classSuper;
	            }
	            if (classFound != clazz) {
	                hmClass.put(clazz, classFound);
	            }
	        }
	    }

	    String pn = (classFound == null) ? null : classFound.getPackage().getName();
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

		OAGraph og = hmPackageGraph.get(pkgName);
		if (og != null) return og;
		
		og = hmPackageGraph2.get(pkgName);
		if (og != null) return og;
		
		RuntimeException exRt = hmRuntimeException.get(pkgName);
		if (exRt != null) throw exRt;
		
		String fnd = null;
		for (String s : hmPackageGraph.keySet()) {
			if (pkgName.equals(s) || pkgName.startsWith(s + ".")) {
				if (fnd == null || s.length() > fnd.length()) fnd = s;
			}
		}
		if (fnd != null) {
			og = hmPackageGraph.get(fnd);
			hmPackageGraph2.put(pkgName, og);
			return og;
		}
		else {
			hmPackageGraph2.put(pkgName, graphDefault);
		}

		return graphDefault;
	}	

	public static void assignGraph(String pkgName, OAGraph graph) {
		runtime.assignGraphInternal(pkgName, graph);
	}
	
	private void assignGraphInternal(String pkgName, OAGraph graph) {
		if (pkgName == null) pkgName = "";
		
		if (graph == null) hmPackageGraph.remove(pkgName);
		else hmPackageGraph.put(pkgName, graph);
		hmPackageGraph2.clear();
	}

	/**
	 * qqqqqqqqqqqqq same as default graph
	 */
	public static OAGraph graph() {
		return runtime.graphInternal("");
	}

	/**
	 * qqqqqqqqqqqqq 
	 */
	public static OAGraph defaultGraph() {
		return runtime.graphInternal("");
	}
	
	/**
	 * qqqqqqqqqqqqq 
	 */
	public static void setDefaultGraph(OAGraph graph) {
		runtime.assignGraphInternal("", graph);
	}
	
	
	
	public static OAThread thread() {
		return runtime.thread;
	}
	
	public static OAContext context() {
		return runtime.context;
	}

	public static OADataSource datasources() {
		return runtime.dataSource;
	}
	
	
	
	
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
	//qqqqqqq
	private boolean UnitTestMode;
	
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





