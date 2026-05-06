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
package com.viaoa.hub.detail;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

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
public class DetailHub<TYPE extends OAObject> extends Hub<TYPE> {

	/**
	 * Constructs a DetailHub that mirrors the collection identified by the
	 * specified property path on the active object of the given master Hub.
	 * Delegates initialization to {@link Hub#setMasterHub}.
	 *
	 * @param hubMaster     the master Hub whose active object supplies the detail
	 *                      collection.
	 * @param propertyPath  the property path identifying the detail collection to
	 *                      mirror.
	 */
    public DetailHub(Hub hubMaster, String propertyPath) {
        setMasterHub(hubMaster, null, propertyPath, false, null);
    }

    /**
     * Constructs a DetailHub that mirrors the collection identified by the
     * specified property path on the active object of the given master Hub, with
     * optional sharing of the active object.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster           the master Hub whose active object supplies the
     *                            detail collection.
     * @param propertyPath        the property path identifying the detail
     *                            collection to mirror.
     * @param bShareActiveObject  if true, the DetailHub shares the same active
     *                            object as the property’s Hub (when applicable).
     */
    public DetailHub(Hub hubMaster, String propertyPath, boolean bShareActiveObject) {
        setMasterHub(hubMaster, null, propertyPath, bShareActiveObject, null);
    }

    /**
     * Constructs a DetailHub that mirrors the collection identified by the
     * specified property path on the active object of the given master Hub,
     * applying the provided select order when the underlying Hub is created or
     * selected.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster     the master Hub whose active object supplies the detail
     *                      collection.
     * @param propertyPath  the property path identifying the detail collection to
     *                      mirror.
     * @param selectOrder   the sort order to apply when the detail Hub is created
     *                      or selected.
     */
    public DetailHub(Hub hubMaster, String propertyPath, String selectOrder) {
        setMasterHub(hubMaster, null, propertyPath, false, selectOrder);
    }

    /**
     * Constructs a DetailHub that mirrors the collection identified by the
     * specified property path on the active object of the given master Hub,
     * supporting both active-object sharing and custom select order.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster           the master Hub whose active object supplies the
     *                            detail collection.
     * @param propertyPath        the property path identifying the detail
     *                            collection to mirror.
     * @param bShareActiveObject  if true, the DetailHub shares the same active
     *                            object as the property’s Hub (when applicable).
     * @param selectOrder         the sort order to apply when the detail Hub is
     *                            created or selected.
     */
    public DetailHub(Hub hubMaster, String propertyPath, boolean bShareActiveObject, String selectOrder) {
        setMasterHub(hubMaster, null, propertyPath, bShareActiveObject, selectOrder);
    }

    /**
     * Constructs a DetailHub based on a reference class rather than a property
     * path, using the active object of the master Hub as the source.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster  the master Hub providing the active object.
     * @param clazz      the class type used to identify the detail collection.
     */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz) {
        setMasterHub(hubMaster, clazz, null, false, null);
    }

    /**
     * Constructs a DetailHub based on a reference class rather than a property
     * path, with optional sharing of the active object between master and detail.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster           the master Hub providing the active object.
     * @param clazz               the class type used to identify the detail
     *                            collection.
     * @param bShareActiveObject  if true, the DetailHub shares the same active
     *                            object as the referenced Hub (when applicable).
     */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, boolean bShareActiveObject) {
        setMasterHub(hubMaster, clazz, null, bShareActiveObject, null);
    }

    /**
     * Constructs a DetailHub based on a reference class rather than a property
     * path, applying the provided select order when the underlying Hub is created
     * or selected.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster   the master Hub providing the active object.
     * @param clazz       the class type used to identify the detail collection.
     * @param selectOrder the sort order to apply when the detail Hub is created
     *                    or selected.
     */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, String selectOrder) {
        setMasterHub(hubMaster, clazz, null, false, selectOrder);
    }

    /**
     * Constructs a DetailHub based on a reference class rather than a property
     * path, supporting both active-object sharing and custom select order.
     * Delegates initialization to {@link Hub#setMasterHub}.
     *
     * @param hubMaster           the master Hub providing the active object.
     * @param clazz               the class type used to identify the detail
     *                            collection.
     * @param bShareActiveObject  if true, the DetailHub shares the active object
     *                            with the referenced Hub (when applicable).
     * @param selectOrder         the sort order to apply when the detail Hub is
     *                            created or selected.
     */
    public DetailHub(Hub hubMaster, Class<TYPE> clazz, boolean bShareActiveObject, String selectOrder) {
        setMasterHub(hubMaster, clazz, null, bShareActiveObject, selectOrder);
    }

    /*  Note:Dont need to finalize
        masterHub has a DetailHub that has a weak reference to this Hub, that will be removed when this object "goes away"
    */
}

