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
package com.viaoa.object;

/**
 * Callback adapter for OAObject serialization. Provides customization hooks
 * for controlling which properties and references are included when an
 * OAObject is serialized for caching or remote synchronization.
 *
 * <p>Callbacks are invoked during graph traversal by {@link OAObjectSerializer}
 * and allow filters to be applied per-object without requiring changes to
 * the domain model or metadata.</p>
 *
 * <p>When excluding properties or references, implementations must not
 * suppress required identity values such as GUID or primary-key fields,
 * so that the receiving side can correctly resolve object identity and
 * maintain Object Graph consistency.</p>
 *
 * <p>This class is not serializable and is intended to be stateless.
 * It is provided per serialization session and must not assume
 * multi-threaded reuse.</p>
 * note: do not serialize this, since it will also include it's outer class, etc.
 *
 * @see OAObjectSerializer
 */
public abstract class OAObjectSerializerCallback {
    private OAObjectSerializer os;
    

    /** 
     * Called by OAObjectSerializer
     */
    void setOAObjectSerializer(OAObjectSerializer os) {
        this.os = os;
    }
    
    
    protected void includeProperties(String... props) {
        if (os == null) return;
        os.includeProperties(props);
    }

    /*
    protected void excludeProperties(String[] props) {
        if (os == null) return;
        os.excludeProperties(props);
    }
    */
    
    protected void excludeProperties(String ... props) {
        if (os == null) return;
        os.excludeProperties(props);
    }
    protected void includeAllProperties() {
        if (os == null) return;
        os.includeAllProperties();
    }
    protected void excludeAllProperties() {
        if (os == null) return;
        os.excludeAllProperties();
    }
    protected int getStackSize() {
        if (os == null) return 0;
        return os.getStackSize();
    }
    protected Object getPreviousObject() {
        if (os == null) return null;
        return os.getPreviousObject();
    }
    protected Object getStackObject(int pos) {
        if (os == null) return null;
        return os.getStackObject(pos);
    }
    /**
     * first object is level 0
     * @return
     */
    public int getLevelsDeep() {
        if (os == null) return 0;
        return os.getLevelsDeep();
    }
    
    public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, boolean bDefault) {
        return bDefault;
    }
    
    /**
     * Callback from OAObjectSerializer.  Use this method to setup the include/exclude properties per object.
     */
    protected abstract void beforeSerialize(OAObject obj);
    // return IncludeProperties.DEFAULT;
    
    /**
     * Callback from OAObjectSerializer, to know when the object has been completed.
     */
    protected void afterSerialize(OAObject obj) {
    }
    
    
    /**
        called by: OAObjectSerializerDelegate for ref props 
        called by: HubDataMaster write, so key can be sent instead of masterObject 
     */
    public Object getReferenceValueToSend(Object obj) {
        return obj;
    }
}
