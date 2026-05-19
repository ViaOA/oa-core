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
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

/*qqqqqqqqqq
CODEX

#10 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:61, addTrigger/removeTrigger; src/
  main/java/com/viaoa/graph/service/object/OAObjectAnnotationService.java:1065
  Concern: trigger registration looks up OARuntime.graph(rootClass) from inside graph services instead of using the
  owning graph service instance.
  Why it matters: calling graphA.addTrigger(triggerForGraphB) silently registers in graph B. That may be convenient,
  but it bypasses graph facade ownership and makes service layering less explicit.
  Minimal fix: either enforce current graph ownership at OAGraphImpl.addTrigger, or document/reroute deliberately
  through OARuntime.graph(rootClass).
  Invariant: GRAPH_TRIGGER_REGISTRATION_TARGET_IS_EXPLICIT
  Test coverage: register triggers through matching and non-matching graph instances; verify target graph.

#11 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:138, runTrigger/getExecutorService
  Concern: async trigger execution uses one static global executor with an effectively unbounded queue and no graph
  lifecycle hook.
  Why it matters: trigger work can outlive graph lifecycle, mix graph workloads, and accumulate without graph-level
  backpressure or shutdown control.
  Minimal fix: make this a runtime-managed executor with explicit lifecycle, or document it as JVM-wide graph
  trigger infrastructure and add queue/cleanup invariants.
  Invariant: GRAPH_TRIGGER_EXECUTOR_HAS_RUNTIME_LIFECYCLE
  Test coverage: async trigger preserves context/loading state, does not leak after graph reset/shutdown, and
  handles overload deterministically.

 #9 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:91, addTrigger/removeTrigger
  Exact concern: OATriggerService has no owning graph reference and resolves the target graph through
  OARuntime.graph(trigger.getRootClass()).
  Why it matters: unlike object/hub parent services, trigger service is not graph-owned in its wiring. Calling
  through one graph can register against another graph based on trigger root class.
  Minimal fix: inject the owning graph or explicitly document target-by-root-class routing.
  Suggested invariant: GRAPH_TRIGGER_TARGET_GRAPH_IS_EXPLICIT
  Suggested test coverage: register a trigger through a non-owning graph and verify documented behavior.

 #10 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:132, TriggerRunnable
  Exact concern: async trigger execution preserves only context and loading state. It does not preserve
  sendSyncMessages or other runtime/thread-local flags.
  Why it matters: a caller can suppress sync messages during a graph operation, but async trigger work may run later
  with executor-thread defaults and emit sync unexpectedly.
  Minimal fix: capture and restore the thread-local flags that affect graph/runtime behavior, especially
  sendSyncMessages.
  Suggested invariant: GRAPH_ASYNC_TRIGGER_PRESERVES_RUNTIME_THREAD_FLAGS
  Suggested test coverage: disable send-sync, enqueue async trigger, verify trigger work does not send sync.

 #11 — invariant risk
  File/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:180, getExecutorService()
  Exact concern: trigger executor is static/global, unbounded, and has no graph lifecycle hook.
  Why it matters: trigger work can outlive graph lifecycle and mix workloads from multiple graphs.
  Minimal fix: make it runtime-managed with explicit lifecycle, or document/test it as JVM-wide trigger
  infrastructure.
  Suggested invariant: GRAPH_TRIGGER_EXECUTOR_LIFECYCLE_IS_EXPLICIT
  Suggested test coverage: graph reset/shutdown does not leave stale trigger work with graph state.


2. file/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:204 runTrigger

  concrete bug: Background trigger failures are silently captured by ExecutorService.submit.

  runtime scenario: A trigger uses bUseBackgroundThread or is shifted to background by bUseBackgroundThreadIfNeeded.
  runTrigger wraps it and calls submit(rx). If the trigger throws, the exception is stored in the returned Future, but
  the Future is discarded.

  why this violates OA/OG trigger semantics: Trigger execution can fail without caller visibility, logging, retry
  signaling, or any observable failure path. This is silent false-success for async trigger execution.

  minimal fix direction: Use execute with a top-level catch/log/failure hook, or retain/inspect the Future through an
  executor afterExecute hook so trigger failures are observable.

  suggested CODEX comment location: At line 207 where submit(rx) is called.


1. file/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:220 runTrigger /
     getExecutorService

  concrete bug: Background trigger execution does not preserve trigger event ordering.

  runtime scenario: Two eligible trigger events are submitted in order for the same object/path, but the executor is a
  fixed pool of 5 threads. The later event can run or complete before the earlier event.

  why this violates OA/OG trigger semantics: Trigger ordering can matter for derived state, Hub membership, cache-
  filter updates, sync side effects, and business-rule callbacks. A committed event stream should not be reordered
  silently when the trigger is marked background.

  minimal fix direction: Use ordered execution per trigger/root/event stream, or explicitly contract background
  triggers as unordered and restrict use where ordering matters. If OA expects ordering, route background trigger work
  through a single ordered queue or keyed serial executor.

  suggested CODEX comment location: Around src/main/java/com/viaoa/graph/service/OATriggerService.java:235, where the
  multi-thread executor is created.

2. file/class/method: src/main/java/com/viaoa/graph/service/OATriggerService.java:139 removeTrigger

  concrete bug: removeTrigger returns true for any non-null trigger, even when no registration was removed.

  runtime scenario: Caller removes a trigger that was never registered, was already removed, or failed partial
  registration. removeTrigger calls oi.removeTrigger(trigger) and unconditionally returns true.

  why this violates OA/OG trigger semantics: The method’s boolean return is the only observable removal result at this
  API level. Returning success when no trigger was removed is false-success behavior and can hide stale registration
  assumptions in cache filters, Hub listeners, or future runtime cleanup code.

  minimal fix direction: Have OAObjectInfo.removeTrigger return a removed count/boolean, including dependent triggers,
  and propagate that result from OATriggerService.removeTrigger.

  suggested CODEX comment location: Around src/main/java/com/viaoa/graph/service/OATriggerService.java:144, before/
  after the oi.removeTrigger(trigger) call.
  
  

*/

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
		boolean bSendMessages;
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
			this.bSendMessages = srvcOAThreadLocal.getSendSyncMessages();
		}

		/**
		 * Restores the captured thread-local context and loading state, executes
		 * the wrapped runnable, and then resets the loading state if necessary.
		 */
		@Override
		public void run() {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
			boolean bWasLoading = true;
			boolean bHold2 = srvcOAThreadLocal.getSendSyncMessages();
			try {
				srvcOAThreadLocal.setContext(context);
				if (bIsLoading) {
					bWasLoading = srvcOAThreadLocal.setLoading(true);
				}
				srvcOAThreadLocal.setSendSyncMessages(bSendMessages);
				runnable.run();
			} finally {
				srvcOAThreadLocal.setContext(null);
				if (bIsLoading) {
					srvcOAThreadLocal.setLoading(bWasLoading);
				}
				srvcOAThreadLocal.setSendSyncMessages(bHold2);
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
