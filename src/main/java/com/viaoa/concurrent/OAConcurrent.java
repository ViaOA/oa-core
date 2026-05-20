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
package com.viaoa.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

/*qqqqqqqqqqqqqq
CODEX

9. OAConcurrent.run — worker failures are logged but not propagated to caller
     Severity: Low
     Bug/risk: each worker catches Exception, logs it, decrements the latch, and run() returns normally. The caller
     cannot detect that one or more tasks failed unless it monitors logs.
     Production impact: acceptable for a test helper, but if used in runtime validation/load/stress helpers, it can
     create false success.
     Minimal hardening: collect thrown exceptions in a thread-safe list and throw an aggregate exception after
     countDownLatch.await().
  10. OAConcurrent.run — interruption while awaiting completion is not cleanup-aware
     Severity: Low
     Bug/risk: if the caller thread is interrupted during countDownLatch.await(), run() throws but worker threads
     continue running. There is no cancellation/interrupt of spawned workers.
     Production impact: caller may abandon a concurrent batch while non-daemon workers keep running and mutating
     shared state.
     Minimal hardening: on interrupt, interrupt spawned threads or document that OAConcurrent has no cancellation
     semantics.

5. OAConcurrent.run — shared instance is not safe for concurrent or reentrant run() calls
     Severity: Low
     Bug/risk: countDownLatch and barrier are instance fields overwritten on each run(). If two threads call run() on
     the same OAConcurrent instance, worker threads from the first run can see the second run’s barrier/latch fields.
     Production impact: can cause broken barriers, hangs, or false completion if reused concurrently. Probably low
     risk if treated as one-shot test utility, but the class does not enforce that.
     Minimal hardening: make run() synchronized, or move latch/barrier to final local variables captured by workers.

*/

/**
 * Utility for executing a group of {@link Runnable} tasks concurrently with a
 * synchronized starting point. <p>
 *
 * OAConcurrent creates one thread per runnable, blocks each new thread on a
 * {@link java.util.concurrent.CyclicBarrier}, and then releases them so all
 * run simultaneously. A {@link java.util.concurrent.CountDownLatch} is used to
 * wait for all tasks to complete before returning from {@link #run()}. <p>
 *
 * This class does not interact with OAObject, Hub, or OA thread-context
 * mechanisms. It simply provides a deterministic way to launch a batch of
 * runnables with coordinated start timing. All threads are non-daemon and exit
 * only after their runnable finishes.
 */
public class OAConcurrent {
    private static Logger LOG = Logger.getLogger(OAConcurrent.class.getName());
    
    /**
     * Latch used to wait until all runnable tasks finish execution.
     * Initialized in {@link #run()} based on the number of runnables.
     */
    private CountDownLatch countDownLatch;
    
    /**
     * Barrier used to synchronize the start of all runnable threads. Each thread
     * blocks on this barrier until all threads have reached it.
     */
    private CyclicBarrier barrier;
    
    /**
     * Array of runnable tasks to execute concurrently. Each runnable is assigned
     * its own thread.
     */
    private Runnable[] runnables;
    
    /**
     * Constructs an OAConcurrent executor for the specified runnable tasks.
     * The caller supplies an array of runnables that will be executed in parallel.
     *
     * @param runnables the runnable tasks to execute concurrently
     */
    public OAConcurrent(Runnable[] runnables) {
        this.runnables = runnables;
    }
    
    /**
     * Executes all runnable tasks concurrently, ensuring that they begin at the
     * same synchronized moment.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Creates a {@link CountDownLatch} to detect when all tasks complete</li>
     *   <li>Creates a {@link CyclicBarrier} so that all threads start together</li>
     *   <li>Spawns one thread per runnable</li>
     *   <li>Each thread waits on the barrier, executes its runnable, logs any
     *       thrown exceptions, and then decrements the latch</li>
     *   <li>Blocks until all runnables have finished</li>
     * </ul>
     *
     * @throws Exception if waiting on the latch is interrupted
     */
    public void run() throws Exception {
        int max = (runnables == null) ? 0 : runnables.length;
        if (max == 0) return;
        
        countDownLatch = new CountDownLatch(max);
        barrier = new CyclicBarrier(max);
        
        for (int i=0; i<max; i++) {
            final int pos= i;
            Thread t = new Thread() {
                public void run() {
                    try {
                        barrier.await();
                        runnables[pos].run();
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "exception in OAThreadManager", e);
                    }
                    finally {
                        countDownLatch.countDown();
                    }
                }
            };
            t.setName("OAConcurrent."+pos);
            t.start();
        }

        countDownLatch.await();
    }
}
