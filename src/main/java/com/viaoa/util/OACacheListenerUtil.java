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
package com.viaoa.util;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectCacheListener;

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

    private final Class clazz;
    private final String property;
    private OAObjectCacheListener listener;
    
    
    public OACacheListenerUtil(Class clazz, String property) {
        this.clazz = clazz;
        this.property = property;
        init();
    }
    
    protected void init() {
        if (listener != null) return;
        listener = new OAObjectCacheListener() {
            @Override
            public void afterPropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
                if (!property.equalsIgnoreCase(propertyName)) return;

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
            @Override
            public void afterAdd(OAObject obj) {
            }
            @Override
            public void afterAdd(Hub hub, OAObject obj) {
            }
            @Override
            public void afterRemove(Hub hub, OAObject obj) {
            }
            @Override
            public void afterLoad(OAObject obj) {
            }
        };
        OAObjectCacheDelegate.addListener(clazz, listener);
    }
    
    public void close() {
        OAObjectCacheDelegate.removeListener(clazz, listener);
        listener = null;
    }

    /**
     * called when the property is changed.
     * @param stackTrace from current thread
     */
    public void onEvent(OAObject obj, String propertyName, Object oldValue, Object newValue, String stackTrace) {
    }
}

