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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OATime;

/*qqqqqqqqqqqqq
CODEX

4. OAScheduledExecutorService — no shutdown/close lifecycle
     Severity: High
     Bug/risk: the wrapper creates a ScheduledExecutorService but exposes no close, shutdown, or cancellation
     lifecycle. Periodic tasks can continue for the lifetime of the JVM unless callers retain and cancel every
     ScheduledFuture.
     Production impact: recurring scheduled tasks can retain application objects, listeners, graph/runtime state, or
     closures after the owning subsystem is stopped. Because threads are daemon, this may not block JVM exit, but it
     can leak work and state during long-running server processes.
     Minimal hardening: add an idempotent close()/shutdown() method, reject new schedules after close, optionally
     cancel queued recurring tasks, and expose lifecycle state.
  5. OAScheduledExecutorService.scheduleEvery — recurring tasks silently stop after an exception
     Severity: High
     Bug/risk: scheduleAtFixedRate and scheduleWithFixedDelay suppress future executions if a task throws. The wrapper
     does not wrap runnables to log/contain exceptions or preserve recurring scheduling.
     Production impact: one uncaught task exception can permanently stop background runtime work with no package-level
     diagnostic. This is dangerous for sync/replication polling, cache cleanup, scheduled maintenance, or trigger-like
     background work.
     Minimal hardening: wrap recurring Runnables in a safe runner that logs/records exceptions and continues, unless
     the task explicitly requests cancellation.
  6. OAScheduledExecutorService.scheduleEvery(Runnable, OATime) — daily scheduling uses fixed 24-hour period
     Severity: Medium
     Bug/risk: daily schedule computes an initial delay to a wall-clock OATime, then repeats every 24 * 60 * 60
     seconds. Across DST transitions or timezone offset changes, it no longer runs at the intended local wall-clock
     time.
     Production impact: daily jobs can run one hour early/late after DST changes. That matters for production batch
     jobs, sync windows, report generation, and time-sensitive maintenance.
     Minimal hardening: after each run, recompute the next delay from current local date/time and target OATime, or
     document that this is fixed-duration scheduling rather than wall-clock daily scheduling.

6. OAScheduledExecutorService.schedule* — submission counter increments before schedule success
     Severity: Low
     Bug/risk: all schedule methods increment aiTotalSubmitted before calling the underlying scheduler. If scheduling
     throws due to invalid period, shutdown, rejected execution, or null task, the counter records work that was never
     accepted.
     Production impact: low direct correctness impact, but diagnostics/monitoring can lie during failure
     investigation.
     Minimal hardening: increment only after the ScheduledFuture is successfully returned, or track attempted vs
     accepted separately.


*/

/**
 * Scheduled executor service backed by a single daemon thread for executing
 * tasks at specific OA temporal values. <p>
 *
 * Provides scheduling based on:
 * <ul>
 *   <li>{@link com.viaoa.datetime.OADateTime}: run once at a specific date/time.</li>
 *   <li>{@link com.viaoa.datetime.OATime}: run every day when the given time occurs.</li>
 *   <li>Fixed delays or fixed-rate periodic execution.</li>
 * </ul>
 *
 * Internally uses a single-thread
 * {@link java.util.concurrent.ScheduledExecutorService}. All created threads
 * are daemon threads. If a task blocks for an extended period, subsequent
 * scheduled tasks will also be delayed. <p>
 *
 * Tasks are counted for basic metrics, but no OA thread context is propagated.
 */
public class OAScheduledExecutorService {
    private static Logger LOG = Logger.getLogger(OAScheduledExecutorService.class.getName());
    
    /**
     * Lazily created scheduled executor service backed by a single daemon thread.
     */
    private ScheduledExecutorService scheduledExecutorService;
    
    /** Counter tracking the number of tasks submitted to this executor. */
    private final AtomicInteger aiTotalSubmitted = new AtomicInteger();
    
    /**
     * Creates a new scheduled executor service. The underlying
     * {@link ScheduledExecutorService} is initialized immediately.
     */
    public OAScheduledExecutorService() {
        getScheduledExecutorService();
    }

    /**
     * Schedules a runnable to execute once at the specified {@link OADateTime}.
     * If {@code dt} is null or in the past relative to the current system time,
     * the task is executed immediately.
     *
     * @param r runnable to execute
     * @param dt desired execution date/time
     * @return ScheduledFuture representing the scheduled task
     * @throws Exception if scheduling fails
     */
    public ScheduledFuture<?> schedule(Runnable r, OADateTime dt) throws Exception {
        aiTotalSubmitted.incrementAndGet();
        
        long ms;
        OADateTime dtNow = new OADateTime();
        if (dt == null || dt.before(dtNow)) ms = 0;
        else ms = dt.betweenMilliSeconds(dtNow);
        
        ScheduledFuture<?> f = getScheduledExecutorService().schedule(r, ms, TimeUnit.MILLISECONDS);
        return f;
    }
    
    /**
     * Schedules a runnable to execute once after the specified delay.
     *
     * @param r runnable to execute
     * @param delay delay before execution
     * @param tu time unit for the delay
     * @return ScheduledFuture for the scheduled task
     * @throws Exception if scheduling fails
     */
    public ScheduledFuture<?> schedule(Runnable r, int delay, TimeUnit tu) throws Exception {
        aiTotalSubmitted.incrementAndGet();
        ScheduledFuture<?> f = getScheduledExecutorService().schedule(r, delay, tu);
        return f;
    }

    /**
     * Schedules a callable to execute once after the specified delay.
     *
     * @param c callable to execute
     * @param delay delay before execution
     * @param tu time unit for the delay
     * @return ScheduledFuture representing the result of the callable
     * @throws Exception if scheduling fails
     */
    public ScheduledFuture<?> schedule(Callable<?> c, int delay, TimeUnit tu) throws Exception {
        aiTotalSubmitted.incrementAndGet();
        ScheduledFuture<?> f = getScheduledExecutorService().schedule(c, delay, tu);
        return f;
    }

    /**
     * Schedules a runnable to execute once per day at the specified {@link OATime}.
     * The initial delay is calculated relative to the current time. Subsequent
     * executions occur every 24 hours.
     *
     * @param r runnable to execute daily
     * @param time time of day when execution should occur
     * @return ScheduledFuture representing the periodic task
     * @throws Exception if scheduling fails
     */
    public ScheduledFuture<?> scheduleEvery(Runnable r, OATime time) throws Exception {
        aiTotalSubmitted.incrementAndGet();
        
        final long secDay = (24 * 60 * 60);
        long secDelay;
        OATime tNow = new OATime();
        if (tNow.before(time)) secDelay = time.betweenSeconds(tNow);
        else {
            secDelay = tNow.betweenSeconds(time);
            secDelay = secDay - secDelay;
        }
        TimeUnit tu = TimeUnit.SECONDS;
        ScheduledFuture<?> f = getScheduledExecutorService().scheduleAtFixedRate(r, secDelay, secDay, tu);
        return f;
    }

    /**
     * Schedules a runnable for periodic execution with a fixed delay between
     * completions. The task begins after {@code initialDelay}.
     *
     * @param r runnable to execute periodically
     * @param initialDelay delay before first execution
     * @param period delay between the end of one execution and the start of the next
     * @param tu time unit for the delay values
     * @return ScheduledFuture representing the periodic task
     * @throws Exception if scheduling fails
     */
    public ScheduledFuture<?> scheduleEvery(Runnable r, int initialDelay, int period, TimeUnit tu) throws Exception {
        aiTotalSubmitted.incrementAndGet();
        ScheduledFuture<?> f = getScheduledExecutorService().scheduleWithFixedDelay(r, initialDelay, period, tu);
        return f;
    }
    
    /**
     * Lazily creates and returns the single-threaded scheduled executor service.
     * The thread factory produces daemon threads named
     * {@code "OAScheduledExecutorService.threadX"}.
     *
     * @return scheduled executor service instance
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        if (scheduledExecutorService != null) return scheduledExecutorService;
        ThreadFactory tf = new ThreadFactory() {
            AtomicInteger ai = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("OAScheduledExecutorService.thread"+ai.getAndIncrement());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
        scheduledExecutorService = Executors.newScheduledThreadPool(1, tf);  // core needs to be > 0
        return scheduledExecutorService;
    }
    
    
    /**
     * Test harness demonstrating recurring scheduling behavior. Not used by the
     * OA runtime. Runs several periodic tasks and prints execution counters.
     *
     * @param args ignored
     * @throws Exception if scheduling or thread sleep fails
     */
    public static void main(String[] args) throws Exception {
        OAScheduledExecutorService ses = new OAScheduledExecutorService();
        final AtomicInteger ai = new AtomicInteger();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ai.incrementAndGet();
                System.out.println("====> "+ai);
                try {
//                    Thread.sleep(900);
                }
                catch (Exception e) {
                    // TODO: handle exception
                }
            }
        };
        ses.scheduleEvery(r, 1, 1, TimeUnit.MILLISECONDS);
        ses.scheduleEvery(r, 5, 2, TimeUnit.SECONDS);
        ses.scheduleEvery(r, 1, 1, TimeUnit.SECONDS);
        ses.scheduleEvery(r, 1, 1, TimeUnit.MILLISECONDS);
        ses.scheduleEvery(r, 1, 1, TimeUnit.SECONDS);
        
        
        /*        
                ThreadFactory tf = new ThreadFactory() {
                    AtomicInteger ai = new AtomicInteger();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("ScheduledExecutorService.thread"+ai.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
        System.out.println("NEW THREAD ====> "+ai);
                        return t;
                    }
                };
                ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, tf);
                
                ScheduledFuture<?> f = scheduledExecutorService.scheduleAtFixedRate(r, 1, 5, TimeUnit.SECONDS);
        
                ScheduledFuture<?> fx = scheduledExecutorService.scheduleAtFixedRate(r, 100, 200, TimeUnit.MILLISECONDS);
        */        
        for (;;) {
            Thread.sleep(10 * 1000);
        }
    }
    
}
