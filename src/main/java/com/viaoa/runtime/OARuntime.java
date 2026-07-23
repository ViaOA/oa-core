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


/**
 * Singleton entry point for resolving and creating OA runtime instances.
 * <p>
 * {@code OARuntime} maps model packages, OAObject classes, OAObject instances, and Hubs to the {@link OA} runtime that
 * owns their metadata and services. It also tracks the default OA runtime and the catch-all runtime used when no more
 * specific model package has been registered.
 * </p>
 * <p>
 * The runtime owns shared infrastructure services for datasource registration and thread/runtime state. Application and
 * library code normally use the static {@code oa(...)} methods to resolve the correct OA instance for a model object,
 * class, package, or Hub.
 * </p>
 *
 * @see OA
 * @see OAObject
 * @see Hub
 */
public final class OARuntime {
	private static Logger LOG = Logger.getLogger(OARuntime.class.getName());

	private static OARuntime runtime = new OARuntime();

	private final Map<String, OA> hmPackageNameOA = new ConcurrentHashMap<>();
	private final Map<String, OA> hmPackageNameOAHelper = new ConcurrentHashMap<>();
	private final Map<String, RuntimeException> hmPackageNameException = new ConcurrentHashMap<>();
	private final Map<Class<?>, Class<?>> hmClassHelper = new ConcurrentHashMap<>();

	private volatile OA oaCatchAll;
	private volatile OA oaDefault;

	private final OADataSourceService srvcDataSource = new OADataSourceService();
	private final OAThreadService srvcThread = new OAThreadService();

	private OARuntime() {
	}

	static {
		runtime.oaCatchAll = (OAImpl) runtime.createOAInternal("");
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @return the resolved OA runtime
	 */
	public static OARuntime get() {
		return runtime;
	}

	/**
	 * Creates an OA runtime instance for the supplied model context.
	 * @param pkg the creation context
	 * @return the created OA runtime
	 */
	public static OA createOA(final Package pkg) {
		return runtime.createOAInternal(pkg);
	}

	/**
	 * Creates an OA runtime instance for the supplied model context.
	 * @param pkg the creation context
	 * @return the created OA runtime
	 */
	public static OA createDefaultOA(final Package pkg) {
		OA oa = runtime.createOAInternal(pkg);
		runtime.defaultOA(oa);
		return oa;
	}

	/**
	 * Creates an OA runtime instance for the supplied model context.
	 * @param clazz the creation context
	 * @return the created OA runtime
	 */
	public static OA createDefaultOA(final Class clazz) {
		return createDefaultOA(clazz.getPackage());
	}

	private OA createOAInternal(final Package pkg) {
		String pn;
		if (pkg != null) pn = pkg.getName();
		else pn = null;
		return createOAInternal(pn);
	}

	/**
	 * Creates an OA runtime instance for the supplied model context.
	 * @param pkgName the creation context
	 * @return the created OA runtime
	 */
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
				/**
				 * Closes runtime-owned state for this instance.
				 */
				public void close() {
					super.close();
					if (this == oaDefault) oaDefault = null;
					if (this == oaCatchAll) {
						runtime.oaCatchAll = (OAImpl) runtime.createOAInternal("");
					}
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

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param obj the lookup context
	 * @return the resolved OA runtime
	 */
	public static OA oa(final OAObject obj) {
		return runtime.oaInternal(obj);
	}

	private OA oaInternal(final OAObject obj) {
		Class<?> c = obj == null ? null : obj.getClass();
		return oaInternal(c);
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param hub the lookup context
	 * @return the resolved OA runtime
	 */
	public static OA oa(final Hub hub) {
		return runtime.oaInternal(hub);
	}

	private OA oaInternal(final Hub hub) {
		Class<?> c = hub == null ? null : hub.getObjectClass();
		return oaInternal(c);
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param hub the lookup context
	 * @param obj the lookup context
	 * @return the resolved OA runtime
	 */
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

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param clazz the lookup context
	 * @return the resolved OA runtime
	 */
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

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param pkg the lookup context
	 * @return the resolved OA runtime
	 */
	public static OA oa(final Package pkg) {
		return runtime.oaInternal(pkg);
	}

	private OA oaInternal(final Package pkg) {
		String pn = pkg == null ? null : pkg.getName();
		return oaInternal(pn);
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @param pkgName the lookup context
	 * @return the resolved OA runtime
	 */
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

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @return the resolved OA runtime
	 */
	public static OA defaultOA() {
		return runtime.oaDefault;
	}

	/**
	 * Sets the default OA runtime instance.
	 *
	 * @param og the default OA runtime
	 */
	public static void defaultOA(OA og) {
		runtime.oaDefault = og;
	}

	/**
	 * Returns an OA runtime instance for the supplied context.
	 * @return the resolved OA runtime
	 */
	public static OA catchAllOA() {
		return runtime.oaInternal("");
	}



	/**
	 * Returns the runtime thread service.
	 *
	 * @return runtime thread service
	 */
	public static OAThreadService thread() {
		return runtime.srvcThread;
	}

	/**
	 * Returns the runtime datasource service.
	 *
	 * @return runtime datasource service
	 */
	public static OADataSourceService datasource() {
		return runtime.srvcDataSource;
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
	}

}
