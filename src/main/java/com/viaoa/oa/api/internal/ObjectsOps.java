package com.viaoa.oa.api.internal;

import com.viaoa.oa.api.internal.objects.OAObjectAnnotationOps;
import com.viaoa.oa.api.internal.objects.OAObjectAutoAddOps;
import com.viaoa.oa.api.internal.objects.OAObjectCSOps;
import com.viaoa.oa.api.internal.objects.OAObjectCacheOps;
import com.viaoa.oa.api.internal.objects.OAObjectRulesOps;
import com.viaoa.oa.api.internal.objects.OAObjectChangeOps;
import com.viaoa.oa.api.internal.objects.OAObjectDSOps;
import com.viaoa.oa.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.oa.api.internal.objects.OAObjectEnumOps;
import com.viaoa.oa.api.internal.objects.OAObjectEventOps;
import com.viaoa.oa.api.internal.objects.OAObjectFindOps;
import com.viaoa.oa.api.internal.objects.OAObjectGuidOps;
import com.viaoa.oa.api.internal.objects.OAObjectHubOps;
import com.viaoa.oa.api.internal.objects.OAObjectInfoOps;
import com.viaoa.oa.api.internal.objects.OAObjectInitializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectKeyOps;
import com.viaoa.oa.api.internal.objects.OAObjectLockOps;
import com.viaoa.oa.api.internal.objects.OAObjectPropertyOps;
import com.viaoa.oa.api.internal.objects.OAObjectReflectOps;
import com.viaoa.oa.api.internal.objects.OAObjectSaveOps;
import com.viaoa.oa.api.internal.objects.OAObjectSchedulerOps;
import com.viaoa.oa.api.internal.objects.OAObjectSerializeOps;
import com.viaoa.oa.api.internal.objects.OAObjectSiblingOps;
import com.viaoa.oa.api.internal.objects.OAObjectStateOps;
import com.viaoa.oa.api.internal.objects.OAObjectUniqueOps;

/**
 * Internal OAObject operation families exposed through {@code OA.internal().objects()}.
 */
public interface ObjectsOps {

	/**
	 * Returns internal OAObject annotation operations.
	 *
	 * @return the annotation operations facade
	 */
	public OAObjectAnnotationOps annotation();

	/**
	 * Returns internal OAObject auto-add state operations.
	 *
	 * @return the auto-add operations facade
	 */
	public OAObjectAutoAddOps autoAdd();
	
	/**
	 * Returns internal OAObject cache operations.
	 *
	 * @return the cache operations facade
	 */
	public OAObjectCacheOps cache();

	/**
	 * Returns the internal OAObject rules engine API.
	 *
	 * @return the object rules operations facade
	 */
	public OAObjectRulesOps rules();

	/**
	 * Returns internal OAObject changed-state operations.
	 *
	 * @return the change operations facade
	 */
	public OAObjectChangeOps change();
	
	/**
	 * Returns internal client/server operations for the current object or Hub family.
	 *
	 * @return the client/server operations facade
	 */
	public OAObjectCSOps cs();

	/**
	 * Returns internal delete operations for the current object or Hub family.
	 *
	 * @return the delete operations facade
	 */
	public OAObjectDeleteOps delete();

	/**
	 * Returns internal datasource identity operations for OAObjects.
	 *
	 * @return the datasource operations facade
	 */
	public OAObjectDSOps ds();

	/**
	 * Returns internal VEnum operations for OAObject properties.
	 *
	 * @return the enum operations facade
	 */
	public OAObjectEnumOps enumx();
	
	/**
	 * Returns internal OAObject event operations.
	 *
	 * @return the event operations facade
	 */
	public OAObjectEventOps event();

	/**
	 * Returns internal find/search operations for the current object or Hub family.
	 *
	 * @return the find operations facade
	 */
	public OAObjectFindOps find();
	
	/**
	 * Returns internal OAObject GUID operations.
	 *
	 * @return the GUID operations facade
	 */
	public OAObjectGuidOps guid();

	/**
	 * Returns internal OAObject-to-Hub reference operations.
	 *
	 * @return the object Hub operations facade
	 */
	public OAObjectHubOps hub();

	/**
	 * Returns internal OAObject metadata operations.
	 *
	 * @return the metadata operations facade
	 */
	public OAObjectInfoOps info();

	/**
	 * Returns internal OAObject initialization operations.
	 *
	 * @return the initialization operations facade
	 */
	public OAObjectInitializeOps initialize();

	/**
	 * Returns internal OAObject key operations.
	 *
	 * @return the key operations facade
	 */
	public OAObjectKeyOps key();

	/**
	 * Returns internal OAObject lock operations.
	 *
	 * @return the lock operations facade
	 */
	public OAObjectLockOps lock();
	
	/**
	 * Returns internal property operations for the current object or Hub family.
	 *
	 * @return the property operations facade
	 */
	public OAObjectPropertyOps property();

	/**
	 * Returns internal OAObject reflection and property-path operations.
	 *
	 * @return the reflection operations facade
	 */
	public OAObjectReflectOps reflect();
	
	/**
	 * Returns internal save operations for the current object or Hub family.
	 *
	 * @return the save operations facade
	 */
	public OAObjectSaveOps save();
	
	/**
	 * Returns internal OAObject scheduler operations.
	 *
	 * @return the scheduler operations facade
	 */
	public OAObjectSchedulerOps scheduler();

	/**
	 * Returns internal serialization operations for the current object or Hub family.
	 *
	 * @return the serialization operations facade
	 */
	public OAObjectSerializeOps serialize();
	
	/**
	 * Returns internal OAObject sibling operations.
	 *
	 * @return the sibling operations facade
	 */
	public OAObjectSiblingOps sibling();

	/**
	 * Returns internal OAObject lifecycle-state operations.
	 *
	 * @return the state operations facade
	 */
	public OAObjectStateOps state();
	
	/**
	 * Returns internal unique-object lookup operations.
	 *
	 * @return the unique operations facade
	 */
	public OAObjectUniqueOps unique();

}
