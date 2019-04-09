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
package com.viaoa.hub;

import java.util.concurrent.ConcurrentHashMap;

import com.viaoa.util.OAPropertyPath;

/**
 * create an index for a Hub.
 * @author vvia
 */
public class HubUniqueIndex<TYPE> {
    
    private final Hub<TYPE> hub;
    private final String property;
    private final boolean bCaseSensitive;
    private final HubListener<TYPE> listener;
    private final ConcurrentHashMap<Object, TYPE> hm = new ConcurrentHashMap<Object, TYPE>();
    private final OAPropertyPath<TYPE> propertyPath;
    
    public HubUniqueIndex(Hub<TYPE> hub, String prop) {
        this(hub, prop, false);
    }
    
    public HubUniqueIndex(Hub<TYPE> hub, String prop, boolean bCaseSensitive) {
        this.hub = hub;
        this.property = prop;
        this.bCaseSensitive = bCaseSensitive;
        this.propertyPath = new OAPropertyPath(hub.getObjectClass(), prop);
        
        listener = new HubListenerAdapter<TYPE>() {
            @Override
            public void afterPropertyChange(HubEvent<TYPE> e) {
                if (e == null || !property.equalsIgnoreCase(e.getPropertyName())) return;
                TYPE object = e.getObject();
                if (object == null) return;
                Object old = e.getOldValue();
                if (old != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && old instanceof String) old = ((String)old).toUpperCase(); 
                    hm.remove(old);
                }
                
                Object value = propertyPath.getValue(object);
                if (value != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
                    hm.put(value, object);
                }
            }
            @Override
            public void afterAdd(HubEvent<TYPE> e) {
                add(e.getObject());
            }
            @Override
            public void afterInsert(HubEvent<TYPE> e) {
                add(e.getObject());
            }
            @Override
            public void afterRemove(HubEvent<TYPE> e) {
                if (e == null) return;
                TYPE object = e.getObject();
                if (object == null) return;
                Object value = propertyPath.getValue(object);
                if (value != null) {
                    if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
                    hm.remove(value);
                }
            }
            @Override
            public void onNewList(HubEvent<TYPE> e) {
                HubUniqueIndex.this.onNewList();
            }
            @Override
            public void afterRemoveAll(HubEvent<TYPE> e) {
                hm.clear();
            }
        };
        hub.addHubListener(listener);
    }

    private void add(TYPE object) {
        if (object == null) return;
        Object value = propertyPath.getValue(object);
        if (value != null) {
            if (!HubUniqueIndex.this.bCaseSensitive && value instanceof String) value = ((String)value).toUpperCase(); 
            hm.put(value, object);
        }
    }
    private void onNewList() {
        hm.clear();
        for (TYPE object : HubUniqueIndex.this.hub) {
            add(object);
        }
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    public void close() {
        hub.removeHubListener(listener);
    }

    public TYPE get(Object id) {
        if (id == null) return null;
        if (!bCaseSensitive && id instanceof String) id = ((String)id).toUpperCase(); 
        return hm.get(id);
    }
    
}
