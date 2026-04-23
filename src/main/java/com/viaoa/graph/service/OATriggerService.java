package com.viaoa.graph.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.api.TriggerOps;
import com.viaoa.graph.api.internal.TriggerInternalOps;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OATrigger;
import com.viaoa.object.OATriggerListener;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;

/**
 * Factory and manager for {@link OATrigger} instances.
 * <p>
 * Provides static methods to create, register, and remove triggers, as well as
 * a thread-pooled execution environment for asynchronous trigger invocation.
 * Each trigger is stored in the {@link OAObjectInfo} of its root class and
 * automatically invoked when matching property paths change.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Registers triggers through {@link OAObjectInfoDelegate}.</li>
 *   <li>Maintains a background thread pool for trigger execution.</li>
 *   <li>Preserves {@link OAThreadLocalDelegate} context for async operations.</li>
 * </ul>
 *
 * @see OATrigger
 * @see OAObjectInfoService
 * @see OATriggerListener
 */
public class OATriggerService implements TriggerOps, TriggerInternalOps {

	/**
	 * Registers the given trigger without skipping any initial non-many
	 * property. This delegates to {@link #createTrigger(OATrigger, boolean)}.
	 *
	 * @param trigger the trigger to register
	 */
	@Override
	public void addTrigger(OATrigger trigger) {
		addTrigger(trigger, false);
	}

	/**
	 * Registers the supplied trigger with the {@link OAObjectInfo} associated
	 * with its root class. Optionally skips the first property in the trigger's
	 * path if it is not a many-relationship.
	 *
	 * @param trigger                       the trigger to register
	 * @param bSkipFirstNonManyProperty     true to skip a non-many first property in the path
	 */
	@Override
	public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		if (trigger == null) {
			return;
		}
		
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(trigger.getRootClass());
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(trigger.getRootClass());
		oi.createTrigger(trigger, bSkipFirstNonManyProperty);
	}

	/**
	 * Removes the specified trigger from the {@link OAObjectInfo} of its root
	 * class.
	 *
	 * @param trigger the trigger to remove
	 * @return true if removed, false if the trigger was null
	 */
	@Override
	public boolean removeTrigger(OATrigger trigger) {
		if (trigger == null) {
			return false;
		}

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(trigger.getRootClass());
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(trigger.getRootClass());
		oi.removeTrigger(trigger);

		return true;
	}

	protected static class TriggerRunnable implements Runnable {
		Runnable runnable;
		boolean bIsLoading;
		public Object context;

		/**
		 * Captures the current thread-local loading state and context
		 * so they can be restored when executed asynchronously.
		 *
		 * @param runnable the runnable to wrap
		 */
		public TriggerRunnable(Runnable runnable) {
			this.runnable = runnable;
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
			this.bIsLoading = srvcOAThreadLocal.isLoading();
			this.context = srvcOAThreadLocal.getContext();
		}

		/**
		 * Restores the captured thread-local context and loading state, executes
		 * the wrapped runnable, and then resets the loading state if necessary.
		 */
		@Override
		public void run() {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
			try {
				srvcOAThreadLocal.setContext(context);
				if (bIsLoading) {
					srvcOAThreadLocal.setLoading(true);
				}
				runnable.run();
			} finally {
				srvcOAThreadLocal.setContext(null);
				if (bIsLoading) {
					srvcOAThreadLocal.setLoading(false);
				}
			}
		}
	}

	/**
	 * Executes the supplied runnable using the trigger executor service,
	 * preserving the caller's thread-local context through a
	 * {@link TriggerRunnable} wrapper.
	 *
	 * @param r the runnable to execute
	 */
	public void runTrigger(Runnable r) {
		if (r == null) return;
		Runnable rx = new TriggerRunnable(r);
		getExecutorService().submit(rx);
	}

	/**
	 * Returns the shared executor service used for asynchronous trigger
	 * execution.
	 *
	 * @return the executor service
	 */
	protected ExecutorService getExecutorService() {
	    return Holder.INSTANCE;
	}

	public static class Holder {
	    static final ExecutorService INSTANCE = createExecutor();

	    public static ExecutorService createExecutor() {
	        ThreadFactory tf = new ThreadFactory() {
	            private final AtomicInteger ai = new AtomicInteger();
	            @Override
	            public Thread newThread(Runnable r) {
	                Thread t = new Thread(r);
	                t.setName("OATrigger.thread." + ai.getAndIncrement());
	                t.setDaemon(true);
	                t.setPriority(Thread.NORM_PRIORITY);
	                return t;
	            }
	        };
	        ThreadPoolExecutor exec = new ThreadPoolExecutor(
	                5, 5, 60L, TimeUnit.SECONDS,
	                new LinkedBlockingQueue<>(Integer.MAX_VALUE),
	                tf);
	        exec.allowCoreThreadTimeOut(true);
	        return exec;
	    }
	}

}
