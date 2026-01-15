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
package com.viaoa.object;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.graph.object.OAObjectInfoService;
import com.viaoa.runtime.OARuntime;

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
 * @see OAObjectInfoDelegate
 * @see OATriggerListener
 */
public class OATriggerDelegate {

	/**
	 * Creates and registers a new trigger using the supplied parameters and
	 * returns the resulting {@link OATrigger}. The trigger is constructed with
	 * the given property paths and behavioral flags, then registered through
	 * {@link #createTrigger(OATrigger)}.
	 *
	 * @param name                    the trigger name
	 * @param rootClass               the root class the trigger applies to
	 * @param triggerListener         the listener invoked when the trigger fires
	 * @param dependentPropertyPaths  the property paths this trigger monitors
	 * @param bOnlyUseLoadedData      true to restrict evaluation to loaded data
	 * @param bServerSideOnly         true to run only on the server
	 * @param bBackgroundThread       true to use a background thread for execution
	 * @param bBackgroundThreadIfNeeded true to use a background thread only when required
	 * @return the newly created trigger
	 */
	public static OATrigger createTrigger(
			String name,
			Class rootClass,
			OATriggerListener triggerListener,
			String[] dependentPropertyPaths,
			final boolean bOnlyUseLoadedData,
			final boolean bServerSideOnly,
			final boolean bBackgroundThread,
			final boolean bBackgroundThreadIfNeeded) {
		OATrigger t = new OATrigger(name, rootClass, triggerListener, dependentPropertyPaths, bOnlyUseLoadedData, bServerSideOnly,
				bBackgroundThread, bBackgroundThreadIfNeeded);

		createTrigger(t);
		return t;
	}

	/**
	 * Registers the given trigger without skipping any initial non-many
	 * property. This delegates to {@link #createTrigger(OATrigger, boolean)}.
	 *
	 * @param trigger the trigger to register
	 */
	public static void createTrigger(OATrigger trigger) {
		createTrigger(trigger, false);
	}

	/**
	 * Registers the supplied trigger with the {@link OAObjectInfo} associated
	 * with its root class. Optionally skips the first property in the trigger's
	 * path if it is not a many-relationship.
	 *
	 * @param trigger                       the trigger to register
	 * @param bSkipFirstNonManyProperty     true to skip a non-many first property in the path
	 */
	public static void createTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		if (trigger == null) {
			return;
		}
		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(trigger.rootClass).objects().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(trigger.rootClass);
		oi.createTrigger(trigger, bSkipFirstNonManyProperty);
	}

	/**
	 * Removes the specified trigger from the {@link OAObjectInfo} of its root
	 * class.
	 *
	 * @param trigger the trigger to remove
	 * @return true if removed, false if the trigger was null
	 */
	public static boolean removeTrigger(OATrigger trigger) {
		if (trigger == null) {
			return false;
		}

		final OAObjectInfoService srvcObjectInfo = OARuntime.get().graph(trigger.rootClass).objects().getOAObjectInfoService();
		OAObjectInfo oi = srvcObjectInfo.getOAObjectInfo(trigger.rootClass);
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
			this.bIsLoading = OARuntime.get().threadLocals().isLoading();
			this.context = OARuntime.get().threadLocals().getContext();
		}

		/**
		 * Restores the captured thread-local context and loading state, executes
		 * the wrapped runnable, and then resets the loading state if necessary.
		 */
		@Override
		public void run() {
			try {
				OARuntime.get().threadLocals().setContext(context);
				if (bIsLoading) {
					OARuntime.get().threadLocals().setLoading(true);
				}
				runnable.run();
			} finally {
				if (bIsLoading) {
					OARuntime.get().threadLocals().setLoading(false);
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
	public static void runTrigger(Runnable r) {
		Runnable rx = new TriggerRunnable(r);
		getExecutorService().submit(rx);
	}

	public static volatile ThreadPoolExecutor executorService;
	
	/**
	 * Returns the shared executor service used for asynchronous trigger
	 * execution.
	 *
	 * @return the executor service
	 */
	protected static ExecutorService getExecutorService() {
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
