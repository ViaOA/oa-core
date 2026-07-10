package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAObject;

/**
 * Internal Hub listener registration and event-dispatch operations.
 */
public interface HubEventOps {

	
	/**
	 * Fires a new-list event for a Hub.
	 *
	 * @param hub the Hub whose list changed
	 * @param bAll {@code true} when all contents changed
	 */
	public void fireOnNewListEvent(Hub<?> hub, boolean bAll);
	/**
	 * Adds a Hub listener for a property.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param property the property to observe
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property);
	/**
	 * Adds a Hub listener for a property with active-object-only control.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param property the property to observe
	 * @param bActiveObjectOnly {@code true} to listen only to the active object
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, boolean bActiveObjectOnly);
	/**
	 * Adds a Hub listener with active-object-only control.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param bActiveObjectOnly {@code true} to listen only to the active object
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, boolean bActiveObjectOnly);
	/**
	 * Adds a Hub listener with dependent property paths.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param property the property to observe
	 * @param dependentPaths property paths that also affect listener updates
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPaths);
	/**
	 * Adds a Hub listener with dependent property paths and active-object-only control.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param property the property to observe
	 * @param dependentPaths property paths that also affect listener updates
	 * @param bActiveObjectOnly {@code true} to listen only to the active object
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPaths, boolean bActiveObjectOnly);
	/**
	 * Adds a Hub listener with full dependency and threading options.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 * @param property the property to observe
	 * @param dependentPaths property paths that also affect listener updates
	 * @param bActiveObjectOnly {@code true} to listen only to the active object
	 * @param bUseBackgroundThread {@code true} to dispatch using a background thread
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread);
	/**
	 * Adds a Hub listener.
	 *
	 * @param hub the Hub to observe
	 * @param hl the listener to add
	 */
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl);
	/**
	 * Removes a Hub listener.
	 *
	 * @param hub the Hub being observed
	 * @param hl the listener to remove
	 */
	public <T extends OAObject> void removeHubListener(Hub<T> hub, HubListener<T> hl);
	/**
	 * Fires a calculated-property change event through a Hub.
	 *
	 * @param hub the Hub context
	 * @param obj the object whose calculated property changed
	 * @param propertyName the calculated property name
	 */
	public <T extends OAObject> void fireCalcPropertyChange(Hub<T> hub, T obj, String propertyName);
	/**
	 * Fires the after-change-active-object event.
	 *
	 * @param thisHub the Hub whose active object changed
	 * @param obj the new active object
	 * @param pos the active-object position
	 * @param bAllShared {@code true} when all shared Hubs are included
	 */
	public <T extends OAObject> void fireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared);

}
