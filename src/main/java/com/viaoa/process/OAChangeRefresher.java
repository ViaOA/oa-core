/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.process;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.hub.*;
import com.viaoa.util.OAString;

/**
 * Used to listen to one or more hubs + propertyPaths and run a process whenever a change is made. Uses
 * a single thread to rerun a process whenever refresh is called.  
 * 
 * Note: since process is ran in another thread, it can find out if any more changes have happened since it has started.
 * 
 * @see #start() to start the thread
 * @see #refresh() to manually run process
 * @see #stop() to stop the thread.
 * @author vvia
 */
public abstract class OAChangeRefresher {
    private static Logger LOG = Logger.getLogger(OAChangeRefresher.class.getName());

    private static final AtomicInteger aiCount = new AtomicInteger();

    private final AtomicInteger aiChange = new AtomicInteger();
    private final AtomicInteger aiStartStop = new AtomicInteger();
    private final AtomicInteger aiThreadId = new AtomicInteger();
    private final AtomicInteger aiName = new AtomicInteger();
    
    private final Object lock = new Object();
    private Thread thread;
    private ArrayList<MyListener> alMyListener;
    private volatile int lastChange;

    private static class MyListener {
        Hub hub;
        HubListener hl;

        public MyListener(Hub h, HubListener hl) {
            this.hub = h;
            this.hl = hl;
        }
    }

    /**
     * create a new change refresher.
     */
    public OAChangeRefresher() {
        this(false);
    }
    
    /**
     * ceate a new change refresher, with the option to have refress called once started.
     * @param bInitialize if true, then the refresh process is called when start() is called.
     */
    public OAChangeRefresher(boolean bInitialize) {
        if (bInitialize) lastChange = -1;
    }
    
    /**
     * Called when it's time to process.
     * 
     * @see #isChanged() to know if refresh has been called since process started.
     */
    protected abstract void process() throws Exception;
    
    /**
     * used to have the process rerun.  Can be called manually.
     */
    public void refresh() {
        aiChange.incrementAndGet();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void addListener(Hub hub, String... propertyPaths) {
        if (hub == null) return;
        if (propertyPaths == null) {
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
     * used to know if refresh has been called since processing.
     */
    public boolean hasChanged() {
        int x = aiChange.get();
        return (x != lastChange);
    }
    public boolean isChanged() {
        int x = aiChange.get();
        return (x == lastChange);
    }

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

    public void stop() {
        aiStartStop.incrementAndGet();

        synchronized (lock) {
            lock.notifyAll();
        }
        LOG.fine("stop called, aiStartStop=" + aiStartStop);
    }

    protected void runThread() {
        final int iStartStop = aiStartStop.get();
        LOG.fine("created queue processor, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
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
                
                lastChange = aiChange.get();

                process();
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "error processing from queue", e);
            }
        }
        LOG.fine("stopped OARefresher thread, cntStartStop=" + iStartStop + ", thread name=" + Thread.currentThread().getName());
    }
}
