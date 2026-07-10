package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

/**
 * Internal OAObjectKey creation and comparison operations.
 */
public interface OAObjectKeyOps {

	/**
	 * Returns the current OAObjectKey for an object.
	 *
	 * @param oaObj the object to inspect
	 * @return the object key
	 */
	public OAObjectKey getKey(OAObject oaObj);
	/**
	 * Creates an OAObjectKey for an object.
	 *
	 * @param oaObj the object to inspect
	 * @return the created object key
	 */
	public OAObjectKey createObjectKey(OAObject oaObj);
	/**
	 * Creates an OAObjectKey for a class and id values.
	 *
	 * @param clazz the object class
	 * @param ids the id values
	 * @return the created object key
	 */
	public OAObjectKey createObjectKey(Class<? extends OAObject> clazz, final Object ...ids);
	/**
	 * Returns whether two keys identify the same OAObject for a class.
	 *
	 * @param clazz the object class
	 * @param ok1 the first key
	 * @param ok2 the second key
	 * @return {@code true} if both keys identify the same object
	 */
	public boolean isForSameOAObject(final Class<? extends OAObject> clazz, final OAObjectKey ok1, final OAObjectKey ok2);
	/**
	 * Creates an OAObjectKey for a single id value.
	 *
	 * @param id the id value
	 * @return the created object key
	 */
	public OAObjectKey createObjectKey(Object id);
	
}
