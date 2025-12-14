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
package com.viaoa.process;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.viaoa.concurrent.OAExecutorService;
import com.viaoa.util.OADateTime;

/**
 * Manages and executes {@link com.viaoa.process.OACron} jobs. A background
 * daemon thread periodically evaluates all registered cron schedules and, when
 * a schedule matches the current minute, dispatches processing using an
 * {@link com.viaoa.concurrent.OAExecutorService}. <p>
 *
 * Cron jobs are executed at most once per minute, with duplicate firings
 * prevented internally. This processor supports dynamic registration and
 * removal of crons and can be started or stopped at runtime.
 */
public class OACronProcessor {
    private static Logger LOG = Logger.getLogger(OACronProcessor.class.getName());

    /**
     * Executor service used to asynchronously run cron job processing in
     * separate worker threads.
     */
    private final OAExecutorService execService;
    
    /**
     * Thread-safe list of registered cron entries. Uses CopyOnWriteArrayList
     * to support concurrent iteration and modification.
     */
    private CopyOnWriteArrayList<OACron> alCron;

    /**
     * Background daemon thread responsible for evaluating cron schedules
     * and dispatching executions.
     */
    private Thread thread;
    
    /**
     * Synchronization lock used to coordinate start/stop activity and timed
     * waiting within the cron processing loop.
     */
    private final Object lock = new Object();
    
    /**
     * Counter used to detect start/stop events. Incremented on each invocation
     * of {@link #start()} or {@link #stop()} and used by the worker thread to
     * determine whether it should exit.
     */
    private final AtomicInteger aiStartStop = new AtomicInteger();
    
    /**
     * Counter used to assign unique names to spawned processor threads.
     */
    private final AtomicInteger aiThreadId = new AtomicInteger();
    
    /**
     * Creates a new cron processor, initializing the executor service and the
     * internal list of cron entries.
     */
    public OACronProcessor() {
        execService = new OAExecutorService();
        alCron = new CopyOnWriteArrayList<OACron>();
    }
    
    /**
     * Returns all currently registered cron entries.
     *
     * @return array of registered {@link OACron} instances
     */
    public OACron[] getCrons() {
        return (OACron[]) alCron.toArray(new OACron[0]);
    }

    /**
     * Registers a new cron entry for processing. Logs the cron details, adds
     * it to the internal list if not already present, and wakes the processor
     * thread if needed.
     *
     * @param cron the cron entry to register
     */
    public void add(OACron cron) {
        LOG.fine("cron="+cron.getName()+", "+cron.getDescription());
        if (!alCron.contains(cron)) {
            alCron.add(cron);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }
    
    /**
     * Removes a previously registered cron entry and logs the removal.
     *
     * @param cron the cron entry to remove
     */
    public void remove(OACron cron) {
        LOG.fine("cron="+cron.getName()+", "+cron.getDescription());
        alCron.remove(cron);
    }

    /**
     * Determines whether the cron processor's background thread is active.
     *
     * @return true if the processor thread has been created
     */
    public boolean isRunning() {
        return (thread != null);
    }
    
    /**
     * Starts the cron processor. Increments the start/stop counter, wakes any
     * waiting thread, creates a new daemon worker thread, and begins cron
     * evaluation and dispatch processing.
     */
    public void start() {
        aiStartStop.incrementAndGet();

        synchronized (lock) {
            lock.notifyAll();
        }

        LOG.fine("start called, aiStartStop=" + aiStartStop);
        thread = new Thread() {
            @Override
            public void run() {
                runThread();
            }
        };
        thread.setName("OACronProcessor." + aiThreadId.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Signals the processor to stop. Increments the start/stop counter, wakes
     * the processor loop, and clears the thread reference so the worker thread
     * will terminate.
     */
    public void stop() {
        aiStartStop.incrementAndGet();

        synchronized (lock) {
            lock.notifyAll();
            thread = null;
        }
        LOG.fine("stop called, aiStartStop=" + aiStartStop);
    }

    /**
     * Executes the {@link OACron#process(boolean)} method for the given cron.
     * Updates the cron's last-processed timestamp and logs the execution.
     *
     * @param cron             the cron entry to process
     * @param bManuallyCalled  true if triggered manually
     */
    protected void callProcess(final OACron cron, boolean bManuallyCalled) {
        if (cron == null) return;
        cron.setLast(new OADateTime());
        LOG.fine("processing cron, name = "+cron.getName()+", description="+cron.getDescription());
        cron.process(bManuallyCalled);
    }

    
    /**
     * Submits a task to process the cron asynchronously using the executor
     * service. Delegates actual work to {@link #callProcess(OACron, boolean)}.
     *
     * @param cron             the cron entry to process
     * @param bManuallyCalled  true if triggered manually
     */
    public void callProcessInAnotherThread(final OACron cron, final boolean bManuallyCalled) {
        if (cron == null) return;
        execService.submit(new Runnable() {
            @Override
            public void run() {
                OACronProcessor.this.callProcess(cron, bManuallyCalled);
            }
        });
    }
    
    /**
     * Main loop executed by the cron processor thread. Evaluates all
     * registered crons once per minute, prevents duplicate firings within the
     * same minute, and dispatches matching crons for asynchronous processing.
     * Terminates when the start/stop counter changes.
     */
    protected void runThread() {
        final int iStartStop = aiStartStop.get();
        LOG.fine("created cron processor, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
        
        OADateTime dtLast = null;
        final ArrayList<OACron> alLast = new ArrayList<>();
                
        
        for (;;) {
            try {
                if (iStartStop != aiStartStop.get()) break;
                
                OADateTime dtNow = new OADateTime();
                dtNow.clearSecondAndMilliSecond();
                
                OADateTime dtCompare = dtNow.addMinutes(-1);
                dtCompare.clearSecondAndMilliSecond();

                if (dtLast == null || dtLast.before(dtCompare)) {
                    alLast.clear();
                }
                dtLast = dtCompare;
                
                beforeProcess(dtNow);
                for (OACron cron : alCron) {
                    if (!cron.getEnabled()) continue;
                    if (alLast.contains(cron)) {
                        continue;
                    }
                    alLast.add(cron);

                    OADateTime dt = new OADateTime(cron.findNext(dtCompare));
                    dt.clearSecondAndMilliSecond();
                    
                    int d = dt.compareTo(dtNow);
                    if (d == 0) {
                        callProcessInAnotherThread(cron, false);
                    }
                }
                
                synchronized (lock) {
                    lock.wait(30 * 1000);
                }
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "error processing from queue", e);
            }
        }
        LOG.fine("stopped OACronProcessor thread, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
    }
    
    /**
     * Hook method invoked before cron evaluations for the current cycle.
     * Designed for subclasses to override. Default implementation does nothing.
     *
     * @param dtNow the timestamp representing the current minute
     */
    protected void beforeProcess(OADateTime dtNow) {
    }
}
