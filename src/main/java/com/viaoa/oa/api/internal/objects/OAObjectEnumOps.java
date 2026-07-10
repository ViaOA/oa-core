package com.viaoa.oa.api.internal.objects;

import com.viaoa.hub.Hub;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.object.OAObject;

/**
 * Internal access to VEnum values associated with OA model object properties.
 */
public interface OAObjectEnumOps {

	/**
	 * Returns VEnum values for a model property.
	 *
	 * @param clazz the object class
	 * @param propertyName the enum-backed property name
	 * @return the Hub of VEnum values
	 */
	public Hub<VEnum> getVEnums(Class<? extends OAObject> clazz, String propertyName);
	
}
