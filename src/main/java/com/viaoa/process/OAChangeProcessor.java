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

import com.viaoa.concurrent.OAExecutorService;
import com.viaoa.hub.*;

/**
 * Used to listen to one or more hubs + propertyPaths and run a process whenever a change is made. 
 * 
 * @author vvia
 */
public abstract class OAChangeProcessor {
    private static Logger LOG = Logger.getLogger(OAChangeProcessor.class.getName());

    private static final AtomicInteger aiCount = new AtomicInteger();
    private ArrayList<MyListener> alMyListener;
    private final OAExecutorService execService;
    
    
    /**
     * 
     * @param bUseThreadPool if false then use current thread.
     */
    public OAChangeProcessor(boolean bUseThreadPool) {
        if (bUseThreadPool) {
            execService = new OAExecutorService();
        }
        else execService = null;
    }
    

    /**
     * Called when it's time to process.
     */
    protected abstract void process(HubEvent evt) ;
    

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
    
    
    private static class MyListener {
        Hub hub;
        HubListener hl;

        public MyListener(Hub h, HubListener hl) {
            this.hub = h;
            this.hl = hl;
        }
    }

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
