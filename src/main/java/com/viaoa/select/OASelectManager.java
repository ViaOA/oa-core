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
package com.viaoa.select;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.object.OAObject;

/**
 * Global manager that tracks and maintains active {@link OASelect}
 * instances.
 * <p>
 * {@code OASelectManager} prevents resource leaks by holding weak
 * references to open selects and automatically canceling or removing
 * them after a configurable idle period.  It runs a lightweight
 * background thread that performs periodic cleanup.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Thread-safe tracking of active selects via
 *       {@link java.lang.ref.WeakReference}.</li>
 *   <li>Automatic cancellation of expired or orphaned queries.</li>
 *   <li>Daemon cleanup thread started on first use.</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>Minimizes memory usage by avoiding hard references to
 *       {@link OASelect} objects.</li>
 *   <li>Operates transparently to application code.</li>
 *   <li>Fully concurrent using {@link java.util.concurrent.ConcurrentHashMap}.</li>
 * </ul>
 *
 * @see OASelect
 * @see OADataSource
 */
public class OASelectManager {
    private static Logger LOG = Logger.getLogger(OASelectManager.class.getName());
    
    /**
     * Thread-safe map of active {@link OASelect} instances keyed by their
     * unique ID. Each entry holds a {@link WeakReference} so that selects
     * may be garbage-collected if no strong references remain.
     */
    private static ConcurrentHashMap<Integer, WeakReference<OASelect>> hmSelect = new ConcurrentHashMap<Integer, WeakReference<OASelect>>(23, .75f, 3);

    /**
     * Flag indicating whether the cleanup daemon thread has been started.
     * Ensures single-threaded startup even under concurrent access.
     */
    private static AtomicBoolean abStartThread = new AtomicBoolean(false);
    
    /**
     * Idle timeout threshold, in seconds. A select whose last activity
     * precedes this value will be automatically cancelled during cleanup.
     * Defaults to five minutes.
     */
    private static int timeLimitInSeconds = (5 * 60);

    /**
     * Sets the global idle timeout used to determine when a select should
     * be considered expired and eligible for automatic cancellation.
     *
     * @param seconds the number of seconds of allowed inactivity
     */
    public static void setTimeLimit(int seconds) {
    	if (seconds <= 0) throw new IllegalArgumentException("time limit must be greater than zero");
        timeLimitInSeconds = seconds;
    }
    
    /**
     * Registers a new {@link OASelect} with the manager. Stores the select in
     * the weak-reference map and initializes the cleanup daemon thread if it
     * has not yet been started.
     *
     * @param sel the select instance to track; ignored if null
     */
    public static void add(OASelect sel) {
        if (sel == null) return;
        final int id = sel.getId();
        hmSelect.put(id, new WeakReference(sel));
        if (!abStartThread.compareAndSet(false, true)) return;
        
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    try {
                        Thread.sleep(timeLimitInSeconds * 1000L);
                        performCleanup();
                    }
                    catch (Exception e) {
                    }
                }
            }
        }, "OASelectManager");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Removes the given {@link OASelect} from the tracking map, typically
     * invoked when a select is closed, completed, or cancelled.
     *
     * @param sel the select instance to remove
     */
    public static void remove(OASelect sel) {
        final int id = sel.getId();
        hmSelect.remove(id);
    }    

    /**
     * Performs a cleanup cycle over all tracked {@link OASelect} instances.
     * <p>
     * Behavior includes:
     * <ul>
     *   <li>Removing entries whose weak reference has been cleared.</li>
     *   <li>Skipping selects that were never started.</li>
     *   <li>Removing selects that are already cancelled.</li>
     *   <li>Checking each active select's last-read timestamp and cancelling
     *       selects that have exceeded the configured idle timeout.</li>
     *   <li>Logging warnings for selects cancelled due to timeout when OA is
     *       not in debug mode.</li>
     * </ul>
     * <p>
     * This method is invoked periodically by the background daemon thread.
     */
    protected static void performCleanup() {
        LOG.finer("checking selects");
        long time = new Date().getTime();
        time -= (timeLimitInSeconds * 1000L);

        int iTotal = hmSelect.size();
        Set<Map.Entry<Integer, WeakReference<OASelect>>> set = hmSelect.entrySet();
        
        for (Iterator<Map.Entry<Integer, WeakReference<OASelect>>> it = set.iterator() ; it.hasNext(); ) {
            Map.Entry<Integer, WeakReference<OASelect>> me = it.next();
            WeakReference<OASelect> ref = me.getValue();
            if (ref == null) continue;
            OASelect sel = ref.get();
            if (sel == null) {
                it.remove();
                continue;
            }
            if (sel.isCancelled()) {
                it.remove();
                continue;
            }
            
            if (!sel.hasBeenStarted()) continue;
            
            long t = sel.getLastReadTime();
            if (t == 0) continue;
            
            if (t < time) {
                if (!OAObject.getDebugMode()) {
                    LOG.warning("cancel select, after timeout.  Select="+sel.getSelectClass()+", where="+sel.getWhere());
                    sel.cancel();
                    it.remove();
                }
            }
        }
        LOG.finer("done, before="+iTotal+", after="+hmSelect.size());
    }
    
}
