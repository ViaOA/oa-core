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
 * Represents a Hub that automatically mirrors a property collection from the
 * active object of another (master) Hub, implementing OA’s master-detail pattern.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Whenever the active object in the master Hub changes, this DetailHub
 *       updates to point to the Hub (or list) corresponding to the target
 *       property of that active object.</li>
 *   <li>Shares the same underlying data as the property’s Hub—no duplication.</li>
 *   <li>Optionally shares the same active object, preserving AO synchronization
 *       across master and detail levels.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Hub<Department> hubDept = new Hub<>(Department.class);
 * hubDept.select();
 * Hub<Employee> hubEmp = new DetailHub<>(hubDept, "employees");
 * }</pre>
 * When the active Department changes in {@code hubDept}, {@code hubEmp}
 * automatically represents that Department’s employees.
 *
 * <h3>Design Notes</h3>
 * <ul>
 *   <li>All constructors delegate to {@link Hub#setMasterHub} with variations
 *       for property path, class type, and AO sharing.</li>
 *   <li>Lifetime is managed by a weak reference in the master; explicit cleanup
 *       is unnecessary.</li>
 * </ul>
 */
public class DetailHub<TYPE> extends Hub<TYPE> {

    /**
        Create a new DetailHub based on a property path from a master Hub.
    */
    public DetailHub(Hub hubMaster, String propertyPath) {
        setMasterHub(hubMaster, null, propertyPath, false, null);
    }

    /**
        Create a new DetailHub based on a property path from a master Hub.
        @param bShareActiveObject if true, then detail Hub uses same active object as
        the property (if it is a Hub) that it is using.
    */
    public DetailHub(Hub hubMaster, String propertyPath, boolean bShareActiveObject) {
        setMasterHub(hubMaster, null, propertyPath, bShareActiveObject, null);
    }

    /**
        Create a new DetailHub based on a property path from a master Hub.
        @param selectOrder if value from property path has not been created/selected, then this
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, String propertyPath, String selectOrder) {
        setMasterHub(hubMaster, null, propertyPath, false, selectOrder);
    }

    /**
        Create a new DetailHub based on a property path from a master Hub.
        @param bShareActiveObject if true, then detail Hub uses same active object as
        @param selectOrder if value from property path has not been created/selected, then this
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, String propertyPath, boolean bShareActiveObject, String selectOrder) {
        setMasterHub(hubMaster, null, propertyPath, bShareActiveObject, selectOrder);
    }

    /**
        Create a new DetailHub based on a reference Class from a master Hub.
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz) {
        setMasterHub(hubMaster, clazz, null, false, null);
    }

    /**
        Create a new DetailHub based on a reference Class from a master Hub.
        @param bShareActiveObject if true, then detail Hub uses same active object as
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, boolean bShareActiveObject) {
        setMasterHub(hubMaster, clazz, null, bShareActiveObject, null);
    }

    /**
        Create a new DetailHub based on a reference Class from a master Hub.
        @param selectOrder if value from property path has not been created/selected, then this
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, String selectOrder) {
        setMasterHub(hubMaster, clazz, null, false, selectOrder);
    }

    /**
        Create a new DetailHub based on a reference Class from a master Hub.
        @param bShareActiveObject if true, then detail Hub uses same active object as
        @param selectOrder if value from property path has not been created/selected, then this
        will be the sort order used when it is selected.
    */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, boolean bShareActiveObject, String selectOrder) {
        setMasterHub(hubMaster, clazz, null, bShareActiveObject, selectOrder);
    }



    /*  Note:Dont need to finalize
        masterHub has a DetailHub that has a weak reference to this Hub, that will be removed when this object "goes away"
    */
}

