package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal lookup for unique OAObject instances by class, property, and key value.
 */
public interface OAObjectUniqueOps {

	/**
	 * Returns the unique object for a class/property/key combination.
	 *
	 * @param clazz the object class
	 * @param propertyName the unique property name
	 * @param uniqueKey the unique key value
	 * @param bAutoCreate {@code true} to create the object when missing
	 * @return the unique object, or {@code null}
	 */
	public OAObject getUnique(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey, final boolean bAutoCreate);

}
