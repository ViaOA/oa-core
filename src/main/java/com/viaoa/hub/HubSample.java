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
package com.viaoa.hub;

/**
 * Maintains a {@link Hub} containing the first N elements of another Hub.
 *
 * <p>This helper automatically mirrors the top portion of a master Hub’s
 * contents in a smaller “sample” Hub, updating it whenever the master Hub
 * changes. It is often used for UI previews, dashboards, or analytics views
 * that only require a subset of the full list.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Hub<Customer> hubAll = new Hub<>(Customer.class);
 * Hub<Customer> hubTop5 = new Hub<>(Customer.class);
 * new HubSample<>(hubAll, hubTop5, 5);
 * }</pre>
 * The {@code hubTop5} list will always contain the first five
 * customers from {@code hubAll}.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Listen for changes on the master Hub (add, remove, insert, sort,
 *       new list) and refresh the sample list accordingly.</li>
 *   <li>Keep the sample Hub synchronized with the master’s ordering and
 *       content, trimming excess items as needed.</li>
 *   <li>Detach its listener via {@link #close()} when the sample is no
 *       longer needed to prevent memory leaks.</li>
 * </ul>
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>Implements a simple “live subset” pattern using a
 *       {@link HubListenerAdapter} for minimal overhead.</li>
 *   <li>Sampling is index-based, not value-based; the top N positions
 *       are always mirrored.</li>
 *   <li>Supports both forward and reverse sorting since updates respond
 *       to {@code afterSort} events.</li>
 * </ul>
 */
public class HubSample<T> {
    protected final Hub<T> hubMaster;
    protected final Hub<T> hubSample;
    protected final int amtSample;
    protected HubListener<T> hubListener;

    /**
     * Create a hubSample instance.
     * @param hubMaster
     * @param hubSample will always be populated with hubMaster<0> .. <n-1>
     * @param sampleAmount number of objects in hubMaster to have in hubSample
     */
    public HubSample(Hub<T> hubMaster, Hub<T> hubSample, int sampleAmount) {
        this.hubMaster = hubMaster;
        this.hubSample = hubSample;
        this.amtSample = sampleAmount;
        setup();
    }
    
    protected void setup() {
        if (hubMaster == null && hubSample == null) return;
        hubListener = new HubListenerAdapter<T>() {
            @Override
            public void afterAdd(HubEvent<T> e) {
                int pos = hubMaster.getPos();
                if (e.getPos() < amtSample) refresh();
            }
            @Override
            public void afterInsert(HubEvent e) {
                if (e.getPos() < amtSample) refresh();
            }
            @Override
            public void afterNewList(HubEvent e) {
                refresh();
            }
            @Override
            public void afterRemove(HubEvent e) {
                if (e.getPos() < amtSample) refresh();
            }
            @Override
            public void afterRemoveAll(HubEvent e) {
                refresh();
            }
            @Override
            public void afterSort(HubEvent e) {
                refresh();
            }
        };
        hubMaster.addHubListener(hubListener);
        refresh();
    }
    
    protected void refresh() {
        for (int i=0; i<amtSample; i++) {
            T obj = hubMaster.getAt(i);
            if (obj == null) {
                hubSample.remove(i);
            }
            else {
                if (hubSample.getAt(i) != obj) {
                    hubSample.remove(obj);
                    hubSample.insert(obj, i);
                }
            }
        }
        for ( ; (hubSample.size() > amtSample) ; ) {
            hubSample.remove(amtSample);
        }
    }
    
    public void close() {
        if (hubListener != null) {
            hubMaster.removeListener(hubListener);
            hubListener = null;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
}
