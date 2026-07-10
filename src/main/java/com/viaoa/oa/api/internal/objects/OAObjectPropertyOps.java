package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal raw property storage operations for OAObject instances.
 */
public interface OAObjectPropertyOps {

	/**
	 * Returns a raw property value from an object.
	 *
	 * @param oaObj the object to inspect
	 * @param name the property name
	 * @return the property value
	 */
	public Object getProperty(OAObject oaObj, String name);
	/**
	 * Returns a raw property value with control over not-exist and weak-reference handling.
	 *
	 * @param oaObj the object to inspect
	 * @param name the property name
	 * @param bReturnNotExist {@code true} to return the not-exist marker when absent
	 * @param bConvertWeakRef {@code true} to resolve weak-reference values
	 * @return the property value
	 */
	public Object getProperty(OAObject oaObj, String name, boolean bReturnNotExist, boolean bConvertWeakRef);
	/**
	 * Sets a raw property value on an object.
	 *
	 * @param oaObj the object to update
	 * @param name the property name
	 * @param value the value to store
	 */
	public void setProperty(OAObject oaObj, String name, Object value);
	/**
	 * Removes a stored property value from an object.
	 *
	 * @param oaObj the object to update
	 * @param name the property name
	 * @param bFirePropertyChange {@code true} to fire change events
	 */
	public void removeProperty(OAObject oaObj, String name, boolean bFirePropertyChange);
	/**
	 * Sets a property using compare-and-set semantics.
	 *
	 * @param oaObj the object to update
	 * @param name the property name
	 * @param newValue the value to store
	 * @param matchValue the required current value
	 * @param bMustNotExist {@code true} when the property must be absent
	 * @param bReturnNotExist {@code true} to use the not-exist marker when applicable
	 */
	public void setPropertyCAS(OAObject oaObj, String name, Object newValue, Object matchValue, boolean bMustNotExist, boolean bReturnNotExist);
	
	/**
	 * Returns stored property names for an object.
	 *
	 * @param oaObj the object to inspect
	 * @return the property names
	 */
	public String[] getPropertyNames(OAObject oaObj);
	/**
	 * Returns whether a property has been loaded.
	 *
	 * @param oaObj the object to inspect
	 * @param prop the property name
	 * @return {@code true} if loaded
	 */
	public boolean isPropertyLoaded(OAObject oaObj, String prop);
	/**
	 * Returns whether a reference property is known to be null.
	 *
	 * @param oaObj the object to inspect
	 * @param prop the reference property name
	 * @return {@code true} if the reference is known null
	 */
	public boolean isReferenceNull(OAObject oaObj, String prop);
	/**
	 * Sets whether an object can be held as a reference.
	 *
	 * @param oaObj the object to update
	 * @param bIsReferenceable {@code true} if the object is referenceable
	 */
	public void setReferenceable(OAObject oaObj, boolean bIsReferenceable);
	/**
	 * Clears internally stored property values for an object.
	 *
	 * @param oaObj the object to clear
	 */
	public void clearProperties(OAObject oaObj);
	
}
