package com.viaoa.runtime;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.object.OAObject;

public final class OARuntime {
	private static Logger LOG = Logger.getLogger(OARuntime.class.getName());

	private static OARuntime runtime = new OARuntime();
	private final OAThreadLocalService threadLocalService;
	private final OARemoteThreadService remoteThreadService;
	private final OADataSourceService dataSourceService;
	
	private final Map<String, OAGraph> hmPackageGraph = new ConcurrentHashMap<>();
	private final Map<String, RuntimeException> hmRuntimeException = new ConcurrentHashMap<>();
	
	private OARuntime() {
		this.threadLocalService = new OAThreadLocalService(this);
		this.remoteThreadService = new OARemoteThreadService(this);
		this.dataSourceService = new OADataSourceService(this);
	}
	
	public static OARuntime get() {
		return runtime;
	}
	
	public OAGraph graph(final Package pkg) {
		if (pkg == null) return null;
		String pn = pkg.getName();
		
		RuntimeException exRt = hmRuntimeException.get(pn);
		if (exRt != null) throw exRt;
		
		OAGraph og = hmPackageGraph.computeIfAbsent(pn, keyPn -> {
			OAGraph g = new OAGraph(this, pkg);
			try {
				g.init();
			} catch (ClassNotFoundException | IOException e) {
				RuntimeException ex = new RuntimeException("Could not initialize OAGraph, package name is " + keyPn, e);
				hmRuntimeException.put(keyPn, ex);
				throw ex;
			}
			return g;
		});
		return og;
	}

	public OAGraph graph(final OAObject obj) {
//qqqqqqq might want to always return a graph, ... have a catch all ??		
		if (obj == null) return null;
		OAGraph og = graph(obj.getClass().getPackage());
		return og;
	}

	public OAGraph graph(final Class<?> c) {
		if (c == null) return null;
		OAGraph og = graph(c.getPackage());
		return og;
	}
	
	public OAThreadLocalService threadLocalService() {
		return threadLocalService;
	}
	public OAThreadLocalService threadLocals() {
		return threadLocalService;
	}

	public OARemoteThreadService remoteThreadService() {
		return remoteThreadService;
	}
	public OARemoteThreadService remoteThreads() {
		return remoteThreadService;
	}

	public OADataSourceService dataSourceService() {
		return dataSourceService;
	}
	public OADataSourceService dataSources() {
		return dataSourceService;
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
	
	
	
	
	
	
	
// NEXT qqqqqqqqqqqqqqqqqq	
	
// OADataSource  (OADatasourceDelegate, OADatasourceDelegate)
// Scheduling	
	
	
	
	
	
	
}





