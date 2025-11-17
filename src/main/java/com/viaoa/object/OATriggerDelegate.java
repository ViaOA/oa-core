/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

	public static void createTrigger(OATrigger trigger) {
		createTrigger(trigger, false);
	}

	/**
	 * @param bSkipFirstNonManyProperty if true, then if the first prop of the propertyPath is not Type=many, then it will not be used. This
	 *                                  is used when there is a HubListener already listening to the objects.
	 */
	public static void createTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
		if (trigger == null) {
			return;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(trigger.rootClass);
		oi.createTrigger(trigger, bSkipFirstNonManyProperty);
	}

	public static boolean removeTrigger(OATrigger trigger) {
		if (trigger == null) {
			return false;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(trigger.rootClass);
		oi.removeTrigger(trigger);

		return true;
	}

	protected static class TriggerRunnable implements Runnable {
		Runnable runnable;
		boolean bIsLoading;
		public Object context;

		public TriggerRunnable(Runnable runnable) {
			this.runnable = runnable;
			this.bIsLoading = OAThreadLocalDelegate.isLoading();
			this.context = OAThreadLocalDelegate.getContext();
		}

		@Override
		public void run() {
			try {
				OAThreadLocalDelegate.setContext(context);
				if (bIsLoading) {
					OAThreadLocalDelegate.setLoading(true);
				}
				runnable.run();
			} finally {
				if (bIsLoading) {
					OAThreadLocalDelegate.setLoading(false);
				}
			}
		}
	}

	public static void runTrigger(Runnable r) {
		Runnable rx = new TriggerRunnable(r);
		getExecutorService().submit(rx);
	}

	private static volatile ThreadPoolExecutor executorService;
	
	protected static ExecutorService getExecutorService() {
	    return Holder.INSTANCE;
	}

	private static class Holder {
	    static final ExecutorService INSTANCE = createExecutor();

	    private static ExecutorService createExecutor() {
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
