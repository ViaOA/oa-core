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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.util.OAString;

/**
 * Event-driven refresher that listens to one or more {@link com.viaoa.hub.Hub}
 * instances and triggers periodic processing when changes occur. <p>
 *
 * OAChangeRefresher maintains a dedicated background thread that waits for
 * signals from Hub listeners or manual calls to {@link #refresh()}. When a
 * refresh is detected, the abstract {@link #process()} method is invoked.
 * Multiple refresh events are coalesced so that processing is not repeated
 * unnecessarily. <p>
 *
 * This class is useful for tasks that must react to changes across an OA
 * object graph but where work must be serialized or batched rather than
 * executed directly inside Hub event callbacks.
 */
public abstract class OAChangeRefresher {
    private static Logger LOG = Logger.getLogger(OAChangeRefresher.class.getName());

    /**
     * Counter used to assign unique thread names for refresher threads.
     */
    private static final AtomicInteger aiThreadId = new AtomicInteger();
    
    /**
     * Counter used to generate unique listener names for property-path listeners.
     */
    private static final AtomicInteger aiName = new AtomicInteger();
    
    /**
     * Tracks the number of refresh requests received.
     * <p>
     * Used to detect whether new changes have occurred since the last processing run.
     */
    private final AtomicInteger aiChange = new AtomicInteger();
    
    /**
     * Counter used to detect when the refresher has been started or stopped.
     * <p>
     * The thread loop exits when the counter changes.
     */
    private final AtomicInteger aiStartStop = new AtomicInteger();
    
    /**
     * Lock used to coordinate the refresher thread's wait/notify cycle.
     */
    private final Object lock = new Object();
    
    /**
     * Background thread responsible for executing {@link #runThread()}.
     */
    private Thread thread;
    
    /**
     * List of registered listeners added to monitored hubs.
     * <p>
     * Used for cleanup during finalization.
     */
    private ArrayList<MyListener> alMyListener;
    
    /**
     * Stores the last observed change counter value after processing runs.
     * <p>
     * Used in conjunction with {@link #hasChanged()} and {@link #isChanged()}.
     */
    private volatile int lastChange;

    /**
     * Simple container pairing a {@link Hub} with the {@link HubListener}
     * registered on it. Used for cleanup.
     */
    private static class MyListener {
    	/**
    	 * The hub this listener is attached to.
    	 */
        Hub hub;
        /**
         * The listener registered on the hub.
         */
        HubListener hl;

        /**
         * Creates a new listener record.
         *
         * @param h  the hub receiving events
         * @param hl the listener registered on the hub
         */
        public MyListener(Hub h, HubListener hl) {
            this.hub = h;
            this.hl = hl;
        }
    }

    /**
     * Creates a new change refresher with no initial refresh trigger.
     */
    public OAChangeRefresher() {
        this(false);
    }
    
    /**
     * Creates a new refresher with optional initialization behavior.
     * <p>
     * If {@code bInitialize} is true, the refresher will treat the start
     * operation as if a refresh had already occurred.
     *
     * @param bInitialize whether an initial refresh should be triggered on start
     */
    public OAChangeRefresher(boolean bInitialize) {
        if (bInitialize) lastChange = -1;
    }
    
    /**
     * Called when a refresh event is ready to be processed.
     * <p>
     * Subclasses implement the actual processing logic.
     *
     * @throws Exception if processing fails
     */
    protected abstract void process() throws Exception;
    
    /**
     * Signals that processing should occur.
     * <p>
     * Increments the refresh counter and notifies the background thread.
     */
    public void refresh() {
        aiChange.incrementAndGet();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * Registers a listener on the given hub for one or more property paths.
     * <p>
     * If {@code propertyPaths} is null or empty, delegates to
     * {@link #addListener(Hub, String)}.
     *
     * @param hub           the hub to observe
     * @param propertyPaths property paths to listen for
     */
    public void addListener(Hub hub, String... propertyPaths) {
        if (hub == null) return;
        if (propertyPaths == null || propertyPaths.length == 0) {
            addListener(hub, (String) null);
        }
        else {
            final String name = "OAChangeRefresher." + aiName.getAndIncrement();
            HubListener hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (name.equalsIgnoreCase(e.getPropertyName())) {
                        refresh();
                    }
                }
            };
            hub.addHubListener(hl, name, propertyPaths);
            MyListener ml = new MyListener(hub, hl);
            if (alMyListener == null) alMyListener = new ArrayList<MyListener>();
            alMyListener.add(ml);
        }
    }
    
    /**
     * Registers a listener on the specified hub for a single property path.
     * <p>
     * If the path contains no dot, the listener is registered directly for the
     * property name. Otherwise, nested property-path logic is used.
     *
     * @param hub          the hub to monitor
     * @param propertyPath the property path of interest
     */
    public void addListener(Hub hub, final String propertyPath) {
        if (hub == null) return;
        HubListener hl;

        if (propertyPath != null && propertyPath.indexOf(".") < 0) {
            hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (propertyPath.equalsIgnoreCase(e.getPropertyName())) {
                        refresh();
                    }
                }
            };
            hub.addHubListener(hl, propertyPath);
        }
        else if (OAString.isNotEmpty(propertyPath)) {
            final String name = "OAChangeRefresher" + aiName.getAndIncrement();
            hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (name.equalsIgnoreCase(e.getPropertyName())) {
                        refresh();
                    }
                }
            };
            hub.addHubListener(hl, name, new String[] { propertyPath });
        }
        else {
            hl = new HubListenerAdapter() {
                @Override
                public void afterAdd(HubEvent e) {
                    refresh();
                }

                @Override
                public void afterInsert(HubEvent e) {
                    refresh();
                }

                @Override
                public void afterNewList(HubEvent e) {
                    refresh();
                }

                @Override
                public void afterRemove(HubEvent e) {
                    refresh();
                }

                @Override
                public void afterRemoveAll(HubEvent e) {
                    refresh();
                }
            };
            hub.addHubListener(hl);
        }

        MyListener ml = new MyListener(hub, hl);
        if (alMyListener == null) alMyListener = new ArrayList<MyListener>();
        alMyListener.add(ml);
    }

    /**
     * Cleans up all registered listeners before garbage collection.
     * <p>
     * Removes each {@link HubListener} previously added to hubs. Then delegates
     * to {@link Object#finalize()}.
     *
     * @throws Throwable if superclass finalization fails
     */
    @Override
    protected void finalize() throws Throwable {
        if (alMyListener != null) {
            for (MyListener ml : alMyListener) {
                ml.hub.removeHubListener(ml.hl);
            }
        }
        super.finalize();
    };

    /**
     * Indicates whether changes have occurred since the last processing cycle.
     *
     * @return {@code true} if {@link #aiChange} differs from {@link #lastChange}
     */
    public boolean hasChanged() {
        int x = aiChange.get();
        return (x != lastChange);
    }

    /**
     * Alias for {@link #hasChanged()}.
     *
     * @return {@code true} if changes are pending
     */
    public boolean isChanged() {
        int x = aiChange.get();
        return (x != lastChange);
    }

    /**
     * Starts the refresher thread.
     * <p>
     * Generates a unique thread name, initializes tracking state, and
     * begins execution of {@link #runThread()} in a background thread.
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
        thread.setName("OAChangeRefresher." + aiThreadId.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
    }

    public Thread getThread() {
        return thread;
    }

    /**
     * Signals the refresher thread to terminate.
     * <p>
     * Increments {@link #aiStartStop} and notifies the lock, causing
     * {@link #runThread()} to exit its loop.
     */
    public void stop() {
        aiStartStop.incrementAndGet();

        synchronized (lock) {
            lock.notifyAll();
        }
        LOG.fine("stop called, aiStartStop=" + aiStartStop);
    }

    private int msWaitBetween = 3;
    
    /**
     * Main loop for the refresher thread.
     * <p>
     * Waits on {@link #lock} until a refresh is signaled. If changes occurred
     * after the last processing cycle, {@link #process()} is invoked. The loop
     * exits when {@link #aiStartStop} changes.
     */
    protected void runThread() {
        final int iStartStop = aiStartStop.get();
        LOG.fine("created queue processor, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
        long msLast = 0;
        for (;;) {
            try {
                if (iStartStop != aiStartStop.get()) break;
                synchronized (lock) {
                    if (!hasChanged()) {
                        lock.wait(60 * 1000);
                    }
                }
                if (iStartStop != aiStartStop.get()) break;

                if (!hasChanged()) continue;
                
                long x = System.currentTimeMillis() - msLast;
                if (x < msWaitBetween) {
                    Thread.sleep(msWaitBetween-x);
                    continue;
                }

                lastChange = aiChange.get();
                process();
                msLast = System.currentTimeMillis();                
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "error processing from queue", e);
            }
        }
        LOG.fine("stopped OARefresher thread, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
    }
}
