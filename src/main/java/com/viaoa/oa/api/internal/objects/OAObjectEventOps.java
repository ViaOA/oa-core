package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

/**
 * Internal OAObject property and lifecycle event dispatch operations.
 */
public interface OAObjectEventOps {

	/**
	 * Fires the before-property-change event for an object property.
	 *
	 * @param oaObj the object whose property is changing
	 * @param propertyName the property name
	 * @param oldObj the previous value
	 * @param newObj the new value
	 * @param bLocalOnly {@code true} to keep the event local
	 * @param bSetChanged {@code true} to mark the object changed
	 */
	public void fireBeforePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	/**
	 * Fires the property-change event for an object property.
	 *
	 * @param oaObj the object whose property changed
	 * @param propertyName the property name
	 * @param oldObj the previous value
	 * @param newObj the new value
	 * @param bLocalOnly {@code true} to keep the event local
	 * @param bSetChanged {@code true} to mark the object changed
	 */
	public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	/**
	 * Fires the property-change event, optionally marking the event as containing unknown values.
	 *
	 * @param oaObj the object whose property changed
	 * @param propertyName the property name
	 * @param oldObj the previous value
	 * @param newObj the new value
	 * @param bLocalOnly {@code true} to keep the event local
	 * @param bSetChanged {@code true} to mark the object changed
	 * @param bUnknownValues {@code true} when old/new values are not fully known
	 */
	public void firePropertyChange(OAObject oaObj, String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged, boolean bUnknownValues);
	/**
	 * Fires the OA after-load event for an object.
	 *
	 * @param oaObj the loaded object
	 */
	public void fireAfterLoadEvent(OAObject oaObj);
	
}
