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
package com.viaoa.hub.filter;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAObject;

/**
 * Produces a live {@link Hub} that contains all objects from one source Hub
 * minus the objects present in another Hub.
 *
 * <p>Essentially computes {@code hubResult = hubMaster − hubMinus}, updating in
 * real time as either source changes.</p>
 *
 * <p><b>Implementation</b>:
 * <ul>
 *   <li>Registers listeners on both {@code hubMaster} and {@code hubMinus}.</li>
 *   <li>On add/remove events, adjusts the result Hub accordingly.</li>
 *   <li>Automatically re-populates on {@code onNewList} events.</li>
 * </ul>
 *
 * <p>Useful for UI pick-lists and difference views between related collections.</p>
 */
public class HubMinusHubFilter {
	/**
	 * References to the three Hubs used by this filter:
	 * <ul>
	 *   <li>{@code hubMaster} – the source Hub containing all possible objects.</li>
	 *   <li>{@code hubMinus} – the Hub containing objects to exclude.</li>
	 *   <li>{@code hub} – the resulting Hub, containing all objects in {@code hubMaster} except those in {@code hubMinus}.</li>
	 * </ul>
	 */
    protected Hub hubMaster, hubMinus, hub;

    /**
     * Creates a new filter that maintains {@code hub} as the live difference
     * between {@code hubMaster} and {@code hubMinus}.
     *
     * <p>Initializes internal references, registers listeners, and populates
     * the resulting Hub.</p>
     *
     * @param hubMaster the Hub containing all objects
     * @param hubMinus the Hub of objects to exclude
     * @param hub the result Hub that will contain objects present in {@code hubMaster} but not in {@code hubMinus}
     */
    public HubMinusHubFilter(Hub hubMaster, Hub hubMinus, Hub hub) {
        if (hubMaster == null || hub == null || hubMinus == null) throw new IllegalArgumentException("hubMaster and hub can not be null");
        this.hubMaster = hubMaster;
        this.hubMinus = hubMinus;
        this.hub = hub;
        init();
        populate();
    }

    /**
     * Rebuilds the result Hub by clearing it and then adding all objects
     * from {@code hubMaster} that are not present in {@code hubMinus}.
     *
     * <p>Iterates sequentially through {@code hubMaster} using
     * {@code elementAt(i)} until a {@code null} is returned.</p>
     */
    protected void populate() {
        hub.clear();
        for (int i=0; ;i++) {
            Object obj = hubMaster.elementAt(i);
            if (obj == null) break;
            if (!hubMinus.contains(obj)) hub.add((OAObject) obj);
        }
    }

    /**
     * Registers listeners on {@code hubMaster} and {@code hubMinus} to maintain
     * the live difference Hub.
     *
     * <ul>
     *   <li>On add/insert to {@code hubMaster}, adds the object to {@code hub}
     *       if it is not present in {@code hubMinus}.</li>
     *   <li>On remove from {@code hubMaster}, removes the object from {@code hub}.</li>
     *   <li>On new list event for either Hub, repopulates {@code hub}.</li>
     *   <li>Listeners on {@code hubMinus} remove objects from {@code hub} when
     *       they appear in {@code hubMinus}, and re-add them when removed from
     *       {@code hubMinus} if they still exist in {@code hubMaster}.</li>
     * </ul>
     */
    protected void init() {
        hubMaster.addHubListener( new HubListenerAdapter() {
            /**
             * Handles the Hub after-add event.
             * @param e the Hub event
             */
            public @Override void afterAdd(HubEvent e) {
                OAObject obj = e.getObject();
                if (obj != null && !hubMinus.contains(obj)) hub.add(obj);
            }
            /**
             * Handles the Hub after-insert event.
             * @param e the Hub event
             */
            public @Override void afterInsert(HubEvent e) {
                afterAdd(e);
            }
            /**
             * Handles the Hub after-remove event.
             * @param e the Hub event
             */
            public @Override void afterRemove(HubEvent e) {
                Object obj = e.getObject();
                if (obj != null) hub.remove(obj);
            }
            /**
             * Handles replacement or refresh of the Hub list.
             * @param e the Hub event
             */
            public @Override void onNewList(HubEvent e) {
                populate();
            }
        });
        hubMinus.addHubListener( new HubListenerAdapter() {
            /**
             * Handles the Hub after-add event.
             * @param e the Hub event
             */
            public @Override void afterAdd(HubEvent e) {
                Object obj = e.getObject();
                if (obj != null && hub.contains(obj)) hub.remove(obj);
            }
            /**
             * Handles the Hub after-insert event.
             * @param e the Hub event
             */
            public @Override void afterInsert(HubEvent e) {
                afterAdd(e);
            }
            /**
             * Handles the Hub after-remove event.
             * @param e the Hub event
             */
            public @Override void afterRemove(HubEvent e) {
                OAObject obj = e.getObject();
                if (hubMaster.contains(obj)) hub.add(obj);
            }
            /**
             * Handles replacement or refresh of the Hub list.
             * @param e the Hub event
             */
            public @Override void onNewList(HubEvent e) {
                populate();
            }
        });
    }
}
