package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;

/**
 * Internal named-property storage operations for Hub instances.
 */
public interface HubPropertyOps {

	
	/**
	 * Stores a named property on a Hub.
	 *
	 * @param hub the Hub to update
	 * @param name the property name
	 * @param obj the value to store
	 */
	public void setProperty(Hub<?> hub, String name, Object obj);
	/**
	 * Returns a named Hub property value.
	 *
	 * @param hub the Hub to inspect
	 * @param name the property name
	 * @return the stored value
	 */
	public Object getProperty(Hub<?> hub, String name);
	/**
	 * Removes a named Hub property.
	 *
	 * @param hub the Hub to update
	 * @param name the property name
	 */
	public void removeProperty(Hub<?> hub, String name);
	/**
	 * Sets the property used as the Hub unique key.
	 *
	 * @param hub the Hub to update
	 * @param propertyName the unique property name
	 */
	public void setUniqueProperty(Hub<?> hub, String propertyName);

}
