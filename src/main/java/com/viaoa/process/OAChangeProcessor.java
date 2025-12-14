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
import java.util.logging.Logger;

import com.viaoa.concurrent.OAExecutorService;
import com.viaoa.hub.*;

/**
 * Listens to one or more {@link com.viaoa.hub.Hub} instances and their
 * property-path change events, and invokes a processing callback whenever
 * a matching change occurs. <p>
 *
 * Callers register hubs and optional property paths using
 * {@link #addListener(Hub, String...)}. When the Hub fires a property-change
 * event associated with the generated listener key, this class calls the
 * abstract {@link #process(com.viaoa.hub.HubEvent)} method. Processing may
 * run either on the current thread or on a background thread using an
 * {@link com.viaoa.concurrent.OAExecutorService}. <p>
 *
 * OAChangeProcessor provides a lightweight, event-driven mechanism for
 * responding to changes in OAObject graphs and Hub collections, without
 * requiring polling or explicit refresh logic.
 */
public abstract class OAChangeProcessor {
    private static Logger LOG = Logger.getLogger(OAChangeProcessor.class.getName());

    /**
     * Counter used to generate unique listener names for property-path listeners.
     */
    private static final AtomicInteger aiCount = new AtomicInteger();
    
    /**
     * Collection of active listeners registered by this processor.
     * <p>
     * Each entry stores the target {@link Hub} and its associated {@link HubListener}.
     */
    private ArrayList<MyListener> alMyListener;
    
    /**
     * Optional background executor used to dispatch processing callbacks.
     * <p>
     * When {@code null}, processing occurs on the calling thread.
     */
    private final OAExecutorService execService;
    
    
    /**
     * Constructs a processor that can run callbacks either synchronously or
     * using a background thread pool.
     *
     * @param bUseThreadPool if {@code true}, uses an {@link OAExecutorService}
     *                       to run {@link #process(HubEvent)} asynchronously;
     *                       otherwise processing occurs on the current thread
     */
    public OAChangeProcessor(boolean bUseThreadPool) {
        if (bUseThreadPool) {
            execService = new OAExecutorService();
        }
        else execService = null;
    }
    

    /**
     * Callback invoked when a registered hub fires a matching property-change event.
     *
     * @param evt the event describing the hub change
     */
    protected abstract void process(HubEvent evt) ;
    

    /**
     * Dispatches a processing callback either on the executor service or
     * directly on the current thread.
     *
     * @param evt the hub event to process
     */
    private void onProcess(final HubEvent evt) {
        if (execService != null) {
            execService.submit(new Runnable() {
                @Override
                public void run() {
                    OAChangeProcessor.this.process(evt);
                }
            });
        }
        else {
            process(evt);
        }
    }
    
    
    /**
     * Internal structure pairing a {@link Hub} with its associated
     * {@link HubListener}. Used for cleanup and tracking active listeners.
     */
    private static class MyListener {
    	/**
    	 * The hub that owns the registered listener.
    	 */
        Hub hub;
        /**
         * The listener attached to the hub for receiving change events.
         */
        HubListener hl;

        /**
         * Creates a new listener mapping.
         *
         * @param h  the hub to listen to
         * @param hl the listener registered on the hub
         */
        public MyListener(Hub h, HubListener hl) {
            this.hub = h;
            this.hl = hl;
        }
    }

    /**
     * Registers a listener on the given {@link Hub} for one or more property paths.
     * <p>
     * If {@code propertyPaths} is {@code null}, delegates to
     * {@link #addListener(Hub, String)}.
     * <p>
     * Otherwise, generates a unique listener name and registers a listener that
     * calls {@link #onProcess(HubEvent)} when the hub fires an event matching
     * that name.
     *
     * @param hub           the hub to monitor
     * @param propertyPaths property paths to listen for
     */
    public void addListener(Hub hub, String... propertyPaths) {
        if (hub == null) return;
        if (propertyPaths == null) {
            addListener(hub, (String) null);
        }
        else {
            final String name = "OAProcess." + aiCount.getAndIncrement();
            HubListener hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (name.equalsIgnoreCase(e.getPropertyName())) {
                        onProcess(e);
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
     * Registers a listener on the given {@link Hub} for a single property path.
     * <p>
     * If the path has no dot, listens directly for that property name.
     * Otherwise registers a generated-path listener similar to the varargs method.
     *
     * @param hub          the hub to monitor
     * @param propertyPath the property path to listen for
     */
    public void addListener(Hub hub, final String propertyPath) {
        if (hub == null) return;
        HubListener hl;

        if (propertyPath != null && propertyPath.indexOf(".") < 0) {
            hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (propertyPath.equalsIgnoreCase(e.getPropertyName())) {
                        onProcess(e);
                    }
                }
            };
        }
        else {
            final String name = "OARefresher." + aiCount.getAndIncrement();
            hl = new HubListenerAdapter() {
                @Override
                public void afterPropertyChange(HubEvent e) {
                    if (name.equalsIgnoreCase(e.getPropertyName())) {
                        onProcess(e);
                    }
                }
            };
            hub.addHubListener(hl, name, new String[] { propertyPath });
        }

        MyListener ml = new MyListener(hub, hl);
        if (alMyListener == null) alMyListener = new ArrayList<MyListener>();
        alMyListener.add(ml);
    }

    /**
     * Delegates to the superclass finalizer after local cleanup is complete.
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

}
