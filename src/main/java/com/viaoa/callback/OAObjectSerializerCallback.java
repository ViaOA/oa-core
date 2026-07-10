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
package com.viaoa.callback;

import com.viaoa.object.OAObject;
import com.viaoa.serialize.OAObjectSerializer;

/**
 * Callback adapter for OAObject serialization. Provides customization hooks
 * for controlling which properties and references are included when an
 * OAObject is serialized for caching or remote synchronization.
 *
 * <p>Callbacks are invoked during object traversal by {@link OAObjectSerializer}
 * and allow filters to be applied per-object without requiring changes to
 * the domain model or metadata.</p>
 *
 * <p>When excluding properties or references, implementations must not
 * suppress required identity values such as GUID or primary-key fields,
 * so that the receiving side can correctly resolve object identity and
 * maintain OA object consistency.</p>
 *
 * <p>This class is not serializable and is intended to be stateless.
 * It is provided per serialization session and must not assume
 * multi-threaded reuse.</p>
 *
 * <p>Instances should not be serialized. Serializing an implementation can
 * accidentally include its enclosing object and unrelated runtime state.</p>
 *
 * @see OAObjectSerializer
 */
public abstract class OAObjectSerializerCallback {
    private OAObjectSerializer os;
    

    /**
     * Sets the {@link OAObjectSerializer} instance that drives the serialization
     * process. This is called internally by the serializer prior to invoking any
     * callback methods.
     *
     * @param os the serializer controlling the current serialization session
     */
    public void setOAObjectSerializer(OAObjectSerializer os) {
        this.os = os;
    }
    
    /**
     * Requests that the specified property names be explicitly included during
     * serialization. Delegates to the underlying serializer if available.
     *
     * @param props the property names to include
     */
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
    
    /**
     * Requests that the specified property names be explicitly excluded during
     * serialization. Delegates to the underlying serializer if available.
     *
     * @param props the property names to exclude
     */
    protected void excludeProperties(String ... props) {
        if (os == null) return;
        os.excludeProperties(props);
    }
    
    /**
     * Requests that all properties of the current object be included during
     * serialization. Delegates to the underlying serializer if available.
     */
    protected void includeAllProperties() {
        if (os == null) return;
        os.includeAllProperties();
    }

    /**
     * Requests that all properties of the current object be excluded during
     * serialization. Delegates to the underlying serializer if available.
     */
    protected void excludeAllProperties() {
        if (os == null) return;
        os.excludeAllProperties();
    }

    /**
     * Returns the number of objects currently in the serializer's traversal stack.
     *
     * @return the stack size, or {@code 0} if no serializer is assigned
     */
    protected int getStackSize() {
        if (os == null) return 0;
        return os.getStackSize();
    }

    /**
     * Returns the object immediately preceding the current one in the serializer's
     * traversal stack.
     *
     * @return the previous object, or {@code null} if unavailable
     */
    protected Object getPreviousObject() {
        if (os == null) return null;
        return os.getPreviousObject();
    }

    /**
     * Returns the object at the specified position within the serializer's
     * traversal stack.
     *
     * @param pos the stack index to retrieve
     * @return the object at the given position, or {@code null} if unavailable
     */
    protected Object getStackObject(int pos) {
        if (os == null) return null;
        return os.getStackObject(pos);
    }

    /**
     * Returns the current recursion depth of the serializer, where the first
     * object is level {@code 0}.
     *
     * @return the depth level, or {@code 0} if no serializer is assigned
     */
    public int getLevelsDeep() {
        if (os == null) return 0;
        return os.getLevelsDeep();
    }
    
    /**
     * Determines whether a reference property should be serialized. The default
     * implementation always returns the supplied {@code bDefault} value.
     *
     * @param oaObj the owning object being serialized
     * @param propertyName the name of the reference property
     * @param obj the referenced object value
     * @param bDefault the serializer's default decision
     * @return the decision to serialize the reference
     */
    public boolean shouldSerializeReference(OAObject oaObj, String propertyName, Object obj, boolean bDefault) {
        return bDefault;
    }
    
    /**
     * Callback invoked before an {@link OAObject} is serialized. Implementations
     * should use this method to configure include/exclude property rules for the
     * given object.
     *
     * @param obj the object about to be serialized
     */
    public abstract void beforeSerialize(OAObject obj);
    // return IncludeProperties.DEFAULT;
    
    /**
     * Callback invoked after an {@link OAObject} has completed serialization.
     * Default implementation performs no action.
     *
     * @param obj the object that was serialized
     */
    public void afterSerialize(OAObject obj) {
    }
    
    
    /**
     * Returns the reference value to send for a referenced object during
     * serialization. The default implementation returns the object unchanged.
     *
     * @param obj the referenced object value
     * @return the value to transmit for the reference
     */
    public Object getReferenceValueToSend(Object obj) {
        return obj;
    }
}
