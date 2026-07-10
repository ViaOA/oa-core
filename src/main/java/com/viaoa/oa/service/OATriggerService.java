package com.viaoa.oa.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.oa.api.internal.TriggersOps;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OAThreadLocal;
import com.viaoa.session.OASessionUser;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

/*qqqqqqqqqq
CODEX

#10 — boundary risk
  File/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:61, addTrigger/removeTrigger; src/
  main/java/com/viaoa/oa/service/object/OAObjectAnnotationService.java:1065
  Concern: trigger registration looks up OARuntime.oa(rootClass) from inside OA services instead of using the
  owning OA service instance.
  Why it matters: calling oaA.addTrigger(triggerForOAB) silently registers in OA runtime B. That may be convenient,
  but it bypasses OA facade ownership and makes service layering less explicit.
  Minimal fix: either enforce current OA runtime ownership at OA.addTrigger, or document/reroute deliberately
  through OARuntime.oa(rootClass).
  Invariant: OA_TRIGGER_REGISTRATION_TARGET_IS_EXPLICIT
  Test coverage: register triggers through matching and non-matching OA runtime instances; verify target OA runtime.

#11 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:138, runTrigger/getExecutorService
  Concern: async trigger execution uses one static global executor with an effectively unbounded queue and no OA runtime
  lifecycle hook.
  Why it matters: trigger work can outlive OA runtime lifecycle, mix OA runtime workloads, and accumulate without OA-runtime-level
  backpressure or shutdown control.
  Minimal fix: make this a runtime-managed executor with explicit lifecycle, or document it as JVM-wide OA
  trigger infrastructure and add queue/cleanup invariants.
  Invariant: OA_TRIGGER_EXECUTOR_HAS_RUNTIME_LIFECYCLE
  Test coverage: async trigger preserves context/loading state, does not leak after OA runtime reset/shutdown, and
  handles overload deterministically.

 #9 — boundary risk
  File/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:91, addTrigger/removeTrigger
  Exact concern: OATriggerService has no owning OA runtime reference and resolves the target OA runtime through
  OARuntime.oa(trigger.getRootClass()).
  Why it matters: unlike object/hub parent services, trigger service is not OA-owned in its wiring. Calling
  through one OA runtime can register against another OA runtime based on trigger root class.
  Minimal fix: inject the owning OA runtime or explicitly document target-by-root-class routing.
  Suggested invariant: OA_TRIGGER_TARGET_OA_IS_EXPLICIT
  Suggested test coverage: register a trigger through a non-owning OA runtime and verify documented behavior.

 #10 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:132, TriggerRunnable
  Exact concern: async trigger execution preserves only context and loading state. It does not preserve
  sendSyncMessages or other runtime/thread-local flags.
  Why it matters: a caller can suppress sync messages during a OA operation, but async trigger work may run later
  with executor-thread defaults and emit sync unexpectedly.
  Minimal fix: capture and restore the thread-local flags that affect OA/runtime behavior, especially
  sendSyncMessages.
  Suggested invariant: OA_ASYNC_TRIGGER_PRESERVES_RUNTIME_THREAD_FLAGS
  Suggested test coverage: disable send-sync, enqueue async trigger, verify trigger work does not send sync.

 #11 — invariant risk
  File/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:180, getExecutorService()
  Exact concern: trigger executor is static/global, unbounded, and has no OA runtime lifecycle hook.
  Why it matters: trigger work can outlive OA runtime lifecycle and mix workloads from multiple OA runtimes.
  Minimal fix: make it runtime-managed with explicit lifecycle, or document/test it as JVM-wide trigger
  infrastructure.
  Suggested invariant: OA_TRIGGER_EXECUTOR_LIFECYCLE_IS_EXPLICIT
  Suggested test coverage: OA runtime reset/shutdown does not leave stale trigger work with OA runtime state.


2. file/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:204 runTrigger

  concrete bug: Background trigger failures are silently captured by ExecutorService.submit.

  runtime scenario: A trigger uses bUseBackgroundThread or is shifted to background by bUseBackgroundThreadIfNeeded.
  runTrigger wraps it and calls submit(rx). If the trigger throws, the exception is stored in the returned Future, but
  the Future is discarded.

  why this violates OA trigger semantics: Trigger execution can fail without caller visibility, logging, retry
  signaling, or any observable failure path. This is silent false-success for async trigger execution.

  minimal fix direction: Use execute with a top-level catch/log/failure hook, or retain/inspect the Future through an
  executor afterExecute hook so trigger failures are observable.

  suggested CODEX comment location: At line 207 where submit(rx) is called.


1. file/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:220 runTrigger /
     getExecutorService

  concrete bug: Background trigger execution does not preserve trigger event ordering.

  runtime scenario: Two eligible trigger events are submitted in order for the same object/path, but the executor is a
  fixed pool of 5 threads. The later event can run or complete before the earlier event.

  why this violates OA trigger semantics: Trigger ordering can matter for derived state, Hub membership, cache-
  filter updates, sync side effects, and business-rule callbacks. A committed event stream should not be reordered
  silently when the trigger is marked background.

  minimal fix direction: Use ordered execution per trigger/root/event stream, or explicitly contract background
  triggers as unordered and restrict use where ordering matters. If OA expects ordering, route background trigger work
  through a single ordered queue or keyed serial executor.

  suggested CODEX comment location: Around src/main/java/com/viaoa/oa/service/OATriggerService.java:235, where the
  multi-thread executor is created.

2. file/class/method: src/main/java/com/viaoa/oa/service/OATriggerService.java:139 removeTrigger

  concrete bug: removeTrigger returns true for any non-null trigger, even when no registration was removed.

  runtime scenario: Caller removes a trigger that was never registered, was already removed, or failed partial
  registration. removeTrigger calls oi.removeTrigger(trigger) and unconditionally returns true.

  why this violates OA trigger semantics: The method’s boolean return is the only observable removal result at this
  API level. Returning success when no trigger was removed is false-success behavior and can hide stale registration
  assumptions in cache filters, Hub listeners, or future runtime cleanup code.

  minimal fix direction: Have OAObjectInfo.removeTrigger return a removed count/boolean, including dependent triggers,
  and propagate that result from OATriggerService.removeTrigger.

  suggested CODEX comment location: Around src/main/java/com/viaoa/oa/service/OATriggerService.java:144, before/
  after the oi.removeTrigger(trigger) call.
  
  

*/

/**
 * Service for registering, removing, and executing {@link OATrigger} instances.
 * <p>
 * Triggers are stored in the {@link OAObjectInfo} for their root class and are
 * invoked when matching property paths change. Asynchronous trigger work is run
 * through a shared executor while preserving selected OA thread-local state.
 * </p>
 *
 * @see OATrigger
 * @see OAObjectInfo
 * @see OATriggerListener
 */
public class OATriggerService implements TriggersOps {
	private final OA oa;

	/**
	 * Creates a trigger service for an OA runtime.
	 *
	 * @param oa owning OA runtime
	 */
	public OATriggerService(OA oa) {
		this.oa = oa;
	}
	
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
		
		OAObjectInfo oi = oa.info((Class<? extends OAObject>) trigger.getRootClass());
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

		OAObjectInfo oi = oa.info((Class<? extends OAObject>) trigger.getRootClass());
		oi.removeTrigger(trigger);

		return true;
	}

	protected static class TriggerRunnable implements Runnable {
		Runnable runnable;
		boolean bIsLoading;
		boolean bSendMessages;
		OAThreadLocal tlOrig;

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
			this.bSendMessages = srvcOAThreadLocal.getSendSyncMessages();
			this.tlOrig = srvcOAThreadLocal.getOAThreadLocal();
		}

		/**
		 * Restores the captured thread-local context and loading state, executes
		 * the wrapped runnable, and then resets the loading state if necessary.
		 */
		@Override
		public void run() {
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
			srvcOAThreadLocal.initialize(tlOrig);
			
			boolean bWasLoading = true;
			boolean bHold2 = srvcOAThreadLocal.getSendSyncMessages();
			try {
				if (bIsLoading) {
					bWasLoading = srvcOAThreadLocal.setLoading(true);
				}
				srvcOAThreadLocal.setSendSyncMessages(bSendMessages);
				runnable.run();
			} finally {
				srvcOAThreadLocal.initialize(null);
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
	@Override
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
