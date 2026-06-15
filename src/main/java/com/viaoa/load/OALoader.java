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
package com.viaoa.load;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.cascade.OACascade;
import com.viaoa.concurrent.OAExecutorService;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.*;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.select.OASelect;

/*qqqqqqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — worker Runnable.run() blocks inside _load(...), lines where if
     (bStop) return; occurs before try/finally
  2. concrete bug
     A worker can return before decrementing aiThreadsUsed and before calling onThreadDone(false).
  3. runtime scenario
     Main thread submits a worker after incrementing aiThreadsUsed. Before the worker starts, caller invokes stop().
     The worker enters run(), sees bStop, returns immediately, and skips the finally block that decrements
     aiThreadsUsed.
  4. why this violates OA/OG load semantics
     The loader can permanently believe a worker is active. waitUntilDone() can spin forever, executor cleanup can be
     skipped, and later load attempts can inherit stale loader state.
  5. minimal fix direction
     Move the if (bStop) check inside the try block, or wrap the entire worker body in try/finally so every submitted
     worker always decrements and calls onThreadDone(false).
  6. suggested CODEX comment location
     Above both worker Runnable.run() implementations in OALoader._load(...).

1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — load(Hub<F> hubRoot)
  2. concrete bug
     hubFrom is set to null before asynchronous worker threads are guaranteed to have started or finished.
  3. runtime scenario
     load(hubRoot) submits worker tasks, then the main loop completes and executes this.hubFrom = null before finally.
     A worker that starts afterward constructs new OASiblingHelper<F>(OALoader.this.hubFrom) with hubFrom == null.
  4. why this violates OA/OG load semantics
     Sibling-helper context is part of load traversal correctness. Clearing the root hub while workers still need it
     can cause missing sibling context, NPEs, or incorrect lazy-load behavior during background traversal.
  5. minimal fix direction
     Remove the early this.hubFrom = null from load(Hub). Let onThreadDone(...) clear it only after aiThreadsUsed ==
     0.
  6. suggested CODEX comment location
     At load(Hub<F> hubRoot), around the current this.hubFrom = null.


1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — async worker exception handling in _load(...)
  2. concrete bug
     Worker exceptions are logged and swallowed, while the public load operation can still appear complete.
  3. runtime scenario
     A worker calls linkInfos[pos].getValue(obj) and datasource/lazy-load traversal throws. The catch logs the
     exception, decrements the worker count, and completion continues. The caller sees waitUntilDone() return with no
     failure signal.
  4. why this violates OA/OG load semantics
     Partial progress is acceptable only when caller-visible failure signals incompleteness. This hides an incomplete
     load and can leave warmed-cache assumptions, reference state, or reporting/query preparation wrong.
  5. minimal fix direction
     Track first worker failure in an AtomicReference<Throwable> and expose/throw it from waitUntilDone() or a result/
     status API. At minimum, set a failed flag that callers can inspect.
  6. suggested CODEX comment location
     Inside both worker catch (Exception e) blocks in OALoader._load(...).

1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — load(OASelect<F> sel)
  2. concrete bug
     The consumed OASelect is not closed in finally, especially when stop() or an exception exits before exhaustion.
  3. runtime scenario
     load(sel) begins pulling datasource results. Caller calls stop() or _load(...) throws before sel.hasMore()
     naturally reaches false. The method removes the sibling helper and calls onThreadDone(true), but does not close
     sel.
  4. why this violates OA/OG load semantics
     Datasource iterators/selects must not remain open after load cancellation or failure. This can leak datasource
     resources and leave query/select state active after the load is no longer progressing.
  5. minimal fix direction
     In finally, close or cancel the select if OALoader owns consumption of that select, or explicitly document caller
     ownership and add a CODEX invariant. Given current method consumes the select, closing in finally is the safer
     contract.
  6. suggested CODEX comment location
     In load(OASelect<F> sel), inside the finally block before onThreadDone(true).


1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — setup(Class c)
  2. concrete bug
     Parsed path state is cached forever after the first setup, even if the same OALoader instance is reused with a
     different root class.
  3. runtime scenario
     Caller creates one OALoader instance and calls load(employeeHub), then later calls load(companyHub) with the same
     property path string but a different root class. setup(...) does not rebuild propertyPath, linkInfos,
     recursiveLinkInfos, methods, or liRecursiveRoot because propertyPath != null.
  4. why this violates OA/OG load semantics
     Path traversal metadata is class-specific. Reusing stale metadata can traverse the wrong links, skip expected
     links, or use the wrong recursive-link metadata.
  5. minimal fix direction
     Track the setup root class. If a later setup(c) uses a different class, rebuild the path metadata or fail
     visibly.
  6. suggested CODEX comment location
     At the if (propertyPath == null) block inside setup(Class c).


1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — _load(Object obj, int pos) recursive-root traversal
  2. concrete bug
     Root recursive traversal has no cascade/depth protection.
  3. runtime scenario
     For a recursive model such as Employee.employees / Employee.parentEmployee, setup(...) sets liRecursiveRoot. At
     pos == 0, _load(...) does:

  Object objx = liRecursiveRoot.getValue(obj);
  _load(objx, pos);

  If the hierarchy contains a cycle, or a child path can reach an ancestor again, traversal loops indefinitely because
  the cascade guard only runs for pos > 0.

  4. why this violates OA/OG load semantics
     Loader traversal must not hang on object graph cycles. OA recursive relationships are first-class graph
     structures, and a loader must not assume perfect acyclic data.
  5. minimal fix direction
     Apply cascade tracking to root recursive traversal as well, or add a dedicated recursive-root cascade/depth guard
     before following liRecursiveRoot.
  6. suggested CODEX comment location
     Inside _load(...), above the if (pos == 0 && liRecursiveRoot != null) block.


1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — _load(Object obj, int pos) locked-link handling
  2. concrete bug
     An unloaded but locked link is silently skipped and not counted as not-loaded.
  3. runtime scenario
     With threaded loading enabled, linkInfos[pos].isLoaded(obj) returns false and linkInfos[pos].isLocked(obj)
     returns true because another thread/session is loading or holding the property. Current code does not increment
     aiNotLoadedCnt, does not wait/retry, and does not traverse after the link.
  4. why this violates OA/OG load semantics
     The loader can report completion while part of the requested path was not loaded or traversed. This is silent
     false-success load behavior.
  5. minimal fix direction
     Treat locked/unloaded as incomplete: count it, retry after unlock, wait with a bounded policy, or expose
     incomplete status to caller.
  6. suggested CODEX comment location
     At both bLoaded/bLocked branches for recursiveLinkInfos[pos - 1] and linkInfos[pos].

1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — abMainThreadRunning initialization and null-input load(...) paths
  2. concrete bug
     abMainThreadRunning starts as true, and null-input load calls return without clearing it. waitUntilDone() can
     therefore block forever even when no load ever started.
  3. runtime scenario
     Caller uses the normal lifecycle pattern:

  OALoader loader = new OALoader(5, "employees");
  loader.load((Hub) null);
  loader.waitUntilDone();

  load(null) returns immediately. executorService == null, but abMainThreadRunning is still true, so waitUntilDone()
  loops forever.

  4. why this violates OA/OG load semantics
     A no-op load must not leave the loader in a “running” state. This creates a hidden blocked-thread condition from
     valid defensive/null-tolerant API usage.
  5. minimal fix direction
     Initialize abMainThreadRunning to false, and ensure null-input load methods leave it false. Set it true only
     after validating input and immediately before work actually starts.
  6. suggested CODEX comment location
     At the abMainThreadRunning field declaration and at the early returns in the three public load(...) overloads.


1. file/class/method
     src/main/java/com/viaoa/load/OALoader.java — public load(...) methods before/around setup(...)
  2. concrete bug
     If setup(...) throws, abMainThreadRunning remains true and waitUntilDone() can block forever after a caller-
     visible setup failure.
  3. runtime scenario
     Caller supplies a property path that fails validation:

  loader.load(hub);

  load(...) sets abMainThreadRunning true, then setup(...) throws before the method reaches the try/finally that calls
  onThreadDone(true). Caller catches the exception and calls waitUntilDone() during cleanup; it never returns.

  4. why this violates OA/OG load semantics
     Caller-visible setup failure is valid incomplete-operation signaling, but the loader’s lifecycle state must still
     remain retryable and wait-safe after failure.
  5. minimal fix direction
     Move setup(...) inside a try/finally that clears main-running state on failure, or set abMainThreadRunning true
     only after setup succeeds and the load try/finally is established.
  6. suggested CODEX comment location
     In all three public load(...) methods, around the abMainThreadRunning.set(true) / setup(...) sequence.


*/

/**
 * Multi-threaded loader used to prefetch or traverse a property path
 * from a root {@link OAObject} or {@link com.viaoa.hub.Hub}.
 *
 * <p>OALoader enables high-performance recursive loading of related objects
 * across complex property paths (e.g. {@code company.locations.employees})
 * using a configurable number of worker threads.  It is typically used to
 * warm caches, pre-load data before analysis, or recursively traverse
 * a graph of {@link OAObject}s.</p>
 *
 * <p><b>Core Features</b>:
 * <ul>
 *   <li>Parallel traversal via {@link com.viaoa.concurrent.OAExecutorService}.</li>
 *   <li>Thread-safe atomic counters for visited and not-yet-loaded objects.</li>
 *   <li>Automatic integration with {@link com.viaoa.hub.Hub} and
 *       {@link OASelect} sources.</li>
 *   <li>Support for recursive link handling and sibling-helper context
 *       propagation.</li>
 *   <li>Graceful shutdown through {@link #stop()} and {@link #waitUntilDone()}.</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>
 *   OALoader&lt;Company, Employee&gt; loader =
 *       new OALoader&lt;&gt;(5, CompanyPP.locations.employees);
 *   loader.load(companyHub);
 * </pre>
 *
 * @param <F> type of the starting OAObject
 * @param <T> type of the target OAObject
 */
public class OALoader<F extends OAObject, T extends OAObject> {
    private static Logger LOG = Logger.getLogger(OALoader.class.getName());
    
    /**
     * Raw string form of the property path to be traversed during loading.
     */
    private String strPropertyPath;
    
    /**
     * Parsed representation of the property path used to navigate linked and
     * recursive relationships.
     */
    private OAPath<T> propertyPath;

    /**
     * LinkInfo describing the recursive parent relationship for the root class.
     */
    private OALinkInfo liRecursiveRoot;

    /**
     * LinkInfo sequence defining each step in the configured property path.
     */
    private OALinkInfo[] linkInfos;
    
    /**
     * LinkInfo array describing recursive relationships encountered while
     * traversing the property path.
     */
    private OALinkInfo[] recursiveLinkInfos;
    
    /**
     * Reflection methods used to access property values for each path segment.
     */
    private Method[] methods;

    /**
     * Flag indicating that traversal should stop as soon as possible.
     */
    private volatile boolean bStop;
    
    /**
     * Maximum number of worker threads allocated for parallel loading.
     */
    private final int threadCount;
    
    /**
     * Executor service used to submit multi-threaded load tasks.
     */
    private volatile OAExecutorService executorService;
    
    /**
     * Counter tracking the number of active worker threads currently running.
     */
    private final AtomicInteger aiThreadsUsed = new AtomicInteger(); 

    /**
     * Counter tracking how many objects have been visited during traversal.
     */
    private final AtomicInteger aiVisitCnt = new AtomicInteger();
    
    /**
     * Counter tracking how many objects were encountered whose linked values
     * were not yet loaded.
     */
    private final AtomicInteger aiNotLoadedCnt = new AtomicInteger();
    
    /**
     * Hub containing root objects for traversal when loading from a Hub or
     * OASelect source.
     */
    private Hub<F> hubFrom;
    
    /**
     * Cascade-tracking instances used to prevent redundant visits and infinite
     * recursion during traversal.
     */
    private OACascade[] cascades;

    /**
     * Indicates whether the main traversal thread is still active, used for
     * shutdown coordination with worker threads.
     */
    private final AtomicBoolean abMainThreadRunning = new AtomicBoolean(true);
    
    
    /**
     * Creates a new multi-threaded loader configured to traverse the
     * specified property path using up to the given number of worker threads.
     *
     * @param threadCount the maximum number of worker threads to use;
     *                    values greater than 50 are capped at 50
     * @param propPath    the property path to traverse during loading
     */
    public OALoader(int threadCount, String propPath) {
        this.threadCount = Math.min(threadCount, 50);
        this.strPropertyPath = propPath;
    }

    
    /**
     * Requests that the current load operation stop as soon as possible.
     *
     * <p>This can be used by subclasses overriding {@code onFound()} or
     * other custom logic to interrupt a long-running traversal.</p>
     */
    public void stop() {
        bStop = true;
    }

    /**
     * Returns the number of objects that have been visited during the load operation.
     *
     * @return the count of visited objects
     */
    public int getVisitCount() {
        return aiVisitCnt.get();
    }

    /**
     * Returns the number of objects encountered whose linked values were not loaded.
     *
     * @return the count of not-yet-loaded objects
     */
    public int getNotLoadedCount() {
        return aiNotLoadedCnt.get();
    }

    /**
     * Loads objects by traversing the configured property path starting from
     * the specified root hub.
     *
     * <p>Initializes traversal state, sets up thread execution when applicable,
     * registers a sibling-helper context, and processes each object in the hub
     * until completion or until {@link #stop()} is invoked.</p>
     *
     * @param hubRoot the root hub from which traversal begins
     */
    public void load(Hub<F> hubRoot) {
        if (hubRoot == null) return;
        abMainThreadRunning.set(true);

        bStop = false;
        setup(hubRoot.getObjectClass());
        if (threadCount > 0) executorService = new OAExecutorService(threadCount, "OALoader");

        this.hubFrom = hubRoot;
        final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(hubRoot);
        siblingHelper.add(OALoader.this.strPropertyPath);
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
        srvcOAThreadLocal.addSiblingHelper(siblingHelper); 
        try {
            for (F obj : hubRoot) {
                _load(obj);
                if (bStop) break;
            }
            this.hubFrom = null;
        }
        finally {
            srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
            onThreadDone(true);
        }
    }
    
    /**
     * Loads objects by traversing the configured property path using an
     * {@link OASelect} as the source of objects.
     *
     * <p>Objects are incrementally pulled from the select into an internal hub,
     * allowing traversal to continue while additional objects become available.
     * Traversal continues until the select is exhausted, all queued objects are
     * processed, or {@link #stop()} is invoked.</p>
     *
     * @param sel the {@code OASelect} providing objects to traverse
     */
    public void load(OASelect<F> sel) {
        if (sel == null) return;
        abMainThreadRunning.set(true);

        bStop = false;
        setup(sel.getSelectClass());
        if (threadCount > 0) executorService = new OAExecutorService(threadCount, "OALoader");
        this.hubFrom = new Hub(sel.getSelectClass());

        final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(this.hubFrom);
        siblingHelper.add(OALoader.this.strPropertyPath);
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
        srvcOAThreadLocal.addSiblingHelper(siblingHelper); 
        try {
            for ( ;!bStop && (sel.hasMore() || hubFrom.size()>0); ) {
                for ( ;sel.hasMore() && hubFrom.size() < 200; ) {
                    if (bStop) break;
                    hubFrom.add(sel.next());
                }
                Object obj = hubFrom.getAt(0);
                hubFrom.remove(0);
                _load((F) obj);
            }        
        }
        finally {
            srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
            onThreadDone(true);
        }
    }
    
    /**
     * Loads objects by traversing the configured property path starting from
     * the specified root object.
     *
     * <p>An internal hub is created to manage traversal, thread execution is
     * initialized when applicable, and a sibling-helper context is registered
     * for recursive and linked navigation.</p>
     *
     * @param objectRoot the starting object for traversal
     */
    public void load(F objectRoot) {
        if (objectRoot == null) return;
        abMainThreadRunning.set(true);

        bStop = false;
        setup(objectRoot.getClass());
        if (threadCount > 0) executorService = new OAExecutorService(threadCount, "OALoader");

        hubFrom = new Hub(objectRoot.getClass());
        hubFrom.add(objectRoot);

        final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(this.hubFrom);
        siblingHelper.add(OALoader.this.strPropertyPath);
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
        srvcOAThreadLocal.addSiblingHelper(siblingHelper); 
        try {
            _load(objectRoot);
        }
        finally {
            srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
            onThreadDone(true);
        }
    }
    
    private void onThreadDone(boolean bMainThread) {
        if (bMainThread) abMainThreadRunning.set(false);
        else if (abMainThreadRunning.get()) return;
        
        if (aiThreadsUsed.get() == 0) {
            if (executorService != null) {  
                executorService.close();
                executorService = null;
            }
            this.hubFrom = null;
            this.cascades = null;
        }
    }
    
    /**
     * Blocks the calling thread until all worker threads have completed and
     * the loader has fully shut down.
     *
     * <p>The method periodically checks whether the executor service has
     * terminated and whether the main traversal thread has finished.</p>
     */
    public void waitUntilDone() {
        for (;;) {
            if (executorService == null) {
                if (!abMainThreadRunning.get()) break;
            }
            try {
                Thread.sleep(250);
            }
            catch (Exception e) {}
        }
    }
    
    /**
     * Begins loading for the specified object at the root position in
     * the property-path traversal.
     *
     * @param object the object to load
     */
    protected void _load(F object) {
        if (object == null) return;

        _load(object, 0);
    }

    /**
     * Recursively traverses the configured property path beginning at the
     * specified object and path position.
     *
     * <p>Handles hub iteration, cascade checks, recursive-link traversal,
     * thread-dispatch logic for unloaded properties, and sequential loading
     * when threading is not applicable.</p>
     *
     * @param obj the current object or hub being processed
     * @param pos the zero-based index of the current position within the
     *            property-path link sequence
     */
    private void _load(final Object obj, final int pos) {
        if (obj == null) return;
        aiVisitCnt.incrementAndGet();
        
        if (obj instanceof Hub) {
            for (Object objx : (Hub) obj) {
                _load(objx, pos);
                if (bStop) break;
            }
            return;
        }

        if (!(obj instanceof OAObject)) return;

        if (pos > 0 && cascades != null && (linkInfos != null && (pos+1) < linkInfos.length)) {
            if (cascades[pos-1].wasCascaded((OAObject) obj, true)) return;
        }
        
        // check if recursive
        if (pos == 0) {
            if (liRecursiveRoot != null) {
                Object objx = liRecursiveRoot.getValue(obj);
                _load(objx, pos); // go up a level to then go through hub
                if (bStop) return;
            }
        }
        else if (recursiveLinkInfos != null && pos <= recursiveLinkInfos.length && (recursiveLinkInfos[pos - 1] != null)) {
            boolean bLoaded = recursiveLinkInfos[pos - 1].isLoaded(obj);
            boolean bLocked = executorService != null && recursiveLinkInfos[pos - 1].isLocked(obj);
            if (!bLoaded && !bLocked) aiNotLoadedCnt.incrementAndGet();
            
            if (executorService != null && !bLoaded && !bLocked && aiThreadsUsed.get() < threadCount) {
                int x = aiThreadsUsed.incrementAndGet();
                if (x <= threadCount) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (bStop) return;
                            final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(OALoader.this.hubFrom);
                            siblingHelper.add(OALoader.this.strPropertyPath);
                			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
                            srvcOAThreadLocal.addSiblingHelper(siblingHelper); 
                            try {
                                Object objx = recursiveLinkInfos[pos - 1].getValue(obj);
                                _load(objx, pos);
                            }
                            catch (Exception e) {
                                LOG.log(Level.WARNING, "OALoader error while in run", e);
                            }
                            finally {
                                srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
                                aiThreadsUsed.decrementAndGet();
                                onThreadDone(false);
                            }
                        }
                    });
                    return;
                }
                aiThreadsUsed.decrementAndGet();
            }
            
            if (!bLocked) {
                Object objx = recursiveLinkInfos[pos - 1].getValue(obj);
                _load(objx, pos);
            }
            if (bStop) return;
        }

        if (linkInfos != null && pos < linkInfos.length) {
            boolean bLoaded = linkInfos[pos].isLoaded(obj);
            boolean bLocked = executorService != null && linkInfos[pos].isLocked(obj);
            if (!bLoaded && !bLocked) aiNotLoadedCnt.incrementAndGet();
            
            if (executorService != null && !bLoaded && !bLocked && aiThreadsUsed.get() < threadCount) {
                int x = aiThreadsUsed.incrementAndGet();
                if (x <= threadCount) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (bStop) return;

                            final OASiblingHelper<F> siblingHelper = new OASiblingHelper<F>(OALoader.this.hubFrom);
                            siblingHelper.add(OALoader.this.strPropertyPath);
                			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
                            srvcOAThreadLocal.addSiblingHelper(siblingHelper); 
                            try {
                                Object objx = linkInfos[pos].getValue(obj);
                                _load(objx, pos+1);
                            }
                            catch (Exception e) {
                                LOG.log(Level.WARNING, "OALoader error while in run", e);
                            }
                            finally {
                                srvcOAThreadLocal.removeSiblingHelper(siblingHelper);
                                aiThreadsUsed.decrementAndGet();
                                onThreadDone(false);
                            }
                        }
                    });
                    return;
                }
                aiThreadsUsed.decrementAndGet();
            }
            if (!bLocked) {
                Object objx = linkInfos[pos].getValue(obj);
                _load(objx, pos+1);
            }
            if (bStop) return;
        }
    }


    /**
     * Initializes the loader for traversal using the given root class.
     *
     * <p>This method constructs the {@link OAPath}, resets counters,
     * resolves link information, identifies recursive links, initializes
     * cascade handlers, and prepares method references used during traversal.</p>
     *
     * @param c the root class for the property-path traversal
     */
    protected void setup(Class c) {
        if (c == null) return;
        
        aiThreadsUsed.set(0); 
        aiVisitCnt.set(0);
        aiNotLoadedCnt.set(0);
        
        if (propertyPath == null) { 
        	propertyPath = new OAPath(c, strPropertyPath);
	        linkInfos = propertyPath.getLinkInfos();
	        recursiveLinkInfos = propertyPath.getRecursiveLinkInfos();
	        methods = propertyPath.getMethods();

	        int x = linkInfos == null ? 0 : linkInfos.length; 
	        if (x != methods.length) {
	            // oafinder is to get from one OAObj/Hub to another, not a property/etc
	            throw new RuntimeException("propertyPath " + strPropertyPath + " must end in an OAObject/Hub");
	        }

			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
	        OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(c);
	        liRecursiveRoot = oi.getRecursiveLinkInfo(OALinkInfo.MANY);
        }

        if (linkInfos != null && linkInfos.length > 0) {
            cascades = new OACascade[linkInfos.length];
            for (int i=0; i<linkInfos.length; i++) {
                cascades[i] = new OACascade(true);  // true= use lock
            }
        }
    }
}
