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
package com.viaoa.cache;

import com.viaoa.datetime.OADateTime;
import com.viaoa.hub.Hub;
import com.viaoa.oa.OA;
import com.viaoa.oa.service.object.OAObjectCacheService;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Utility for monitoring property changes on all {@link OAObject} instances of
 * a specific class. When the specified property is modified and the change is
 * reported through the {@link OAObjectCacheDelegate}, this class captures the
 * current thread and stack trace and forwards the information to
 * {@link #onEvent(OAObject, String, Object, Object, String)}. <p>
 *
 * This is intended as a debugging or diagnostic aid for identifying which
 * thread or code path modified a particular property. The listener is
 * installed when the instance is created and can be removed using
 * {@link #close()}. Subclasses override {@code onEvent} to handle or log the
 * captured stack trace information.
 */
public class OACacheListenerUtil {

	/**
	 * The {@link Class} of {@link OAObject} instances to monitor for cache events.
	 */
    private final Class clazz;
    
    /**
     * The name of the property to monitor for changes.
     */
    private final String property;
    
    /**
     * The cache listener instance registered with {@link OAObjectCacheDelegate},
     * or {@code null} if not currently installed.
     */
    private OAObjectCacheListener listener;
    
    
    /**
     * Creates a new listener utility for monitoring changes to a specific property
     * on all {@link OAObject} instances of the given class.
     *
     * This constructor stores the provided parameters and initializes the
     * underlying cache listener.
     *
     * @param clazz the {@link Class} of {@link OAObject} instances to monitor
     * @param property the name of the property to monitor
     */
    public OACacheListenerUtil(Class clazz, String property) {
        this.clazz = clazz;
        this.property = property;
        init();
    }
    
    /**
     * Initializes and registers the {@link OAObjectCacheListener} if it has not
     * already been created.
     *
     * This method creates an anonymous listener implementation and registers it
     * with {@link OAObjectCacheDelegate} for the configured class.
     */
    protected void init() {
        if (listener != null) return;
        listener = new OAObjectCacheListener() {
            /**
             * Captures stack information for matching property changes.
             */
            @Override
            public void afterPropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
            	if (property != null) {
            		if (!property.equalsIgnoreCase(propertyName)) return;
            	}

                Thread t = Thread.currentThread();
                StringBuilder sb = new StringBuilder(2048);
                String s = (new OADateTime()) + ", Thread="+t.getName();
                sb.append(s + "\n");
                
                StackTraceElement[] stes = t.getStackTrace();
                if (stes != null) {
                    for (StackTraceElement ste : stes) {
                        sb.append(ste.toString());
                        sb.append("\n");
                    }
                }
                String sx = sb.toString();
                OACacheListenerUtil.this.onEvent(obj, propertyName, oldValue, newValue, sx);
            }
            /**
             * Object-add events are ignored by this property-change utility.
             */
            @Override
            public void afterAdd(OAObject obj) {
            }
            /**
             * Hub-add events are ignored by this property-change utility.
             */
            @Override
            public void afterAdd(Hub hub, OAObject obj) {
            }
            /**
             * Hub-remove events are ignored by this property-change utility.
             */
            @Override
            public void afterRemove(Hub hub, OAObject obj) {
            }
            /**
             * Load events are ignored by this property-change utility.
             */
            @Override
            public void afterLoad(OAObject obj) {
            }
        };

		final OA oa = OARuntime.oa(clazz);
		oa.internal().objects().cache().addListener(clazz, listener);
    }
    
    /**
     * Removes the registered {@link OAObjectCacheListener} from the
     * {@link OAObjectCacheDelegate} for the configured class.
     *
     * This method unregisters the listener and clears the internal reference,
     * preventing further cache events from being received.
     */
    public void close() {
		final OA oa = OARuntime.oa(clazz);
		oa.internal().objects().cache().removeListener(clazz, listener);
        listener = null;
    }

    /**
     * Called when the monitored property is changed on a matching {@link OAObject}.
     *
     * This method is invoked after a property change event has been detected and
     * the current thread stack trace has been captured.
     *
     * The default implementation performs no actions and is intended to be
     * overridden by subclasses.
     *
     * @param obj the {@link OAObject} whose property was changed
     * @param propertyName the name of the property that changed
     * @param oldValue the previous value of the property
     * @param newValue the new value of the property
     * @param stackTrace the captured stack trace of the current thread
     */
    public void onEvent(OAObject obj, String propertyName, Object oldValue, Object newValue, String stackTrace) {
    }
}

