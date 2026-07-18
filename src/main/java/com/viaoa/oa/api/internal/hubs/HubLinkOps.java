package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal linked-Hub operations used to keep one Hub synchronized from another Hub or property relationship.
 */
public interface HubLinkOps {

    /**
     * Links one Hub to a reference property of another Hub's active object.
     * <p>
     * {@code link(...)} wires {@code hub1} to the reference defined by
     * {@code referenceName} on the active object of {@code hub2}. As the active
     * object of {@code hub2} changes, {@code hub1} is automatically updated to
     * reflect the referenced object or collection.
     * <p>
     * The {@code referenceName} must be a valid model property that defines a
     * relationship (reference or Hub) on the objects contained in {@code hub2}.
     * <p>
     * This is commonly used to synchronize Hubs based on relationships, allowing
     * one Hub to follow another through the OA model as navigation occurs.
     *
     * @param hub1 the Hub to be linked and updated
     * @param hub2 the source Hub whose active object drives the link
     * @param referenceName the relationship property name on the source object's type
     */
    void link(Hub<?> hub1, Hub<?> hub2, String referenceName);
	/**
	 * Returns the Hub that owns link configuration for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @param bIncludeCopiedHubs {@code true} to include copied Hubs
	 * @return the Hub with link configuration
	 */
	public <T extends OAObject> Hub<T> getHubWithLink(Hub<T> hub, boolean bIncludeCopiedHubs);
	/**
	 * Configures this Hub to link to another Hub.
	 *
	 * @param thisHub the Hub being linked
	 * @param propertyFrom the local property used for linking
	 * @param linkToHub the target Hub
	 * @param propertyTo the target property used for linking
	 * @param linkPosFlag {@code true} to link by position
	 * @param bAutoCreate {@code true} to auto-create missing linked objects
	 * @param bAutoCreateAllowDups {@code true} to allow duplicate auto-created links
	 */
	public void setLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups);
	/**
	 * Returns the linked-Hub property path.
	 *
	 * @param hub the Hub to inspect
	 * @param bIncludeCopiedHubs {@code true} to include copied Hubs
	 * @return the link path, or {@code null}
	 */
	public String getLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs);
	/**
	 * Updates the Hub linked to another Hub for an object.
	 *
	 * @param hub the linked Hub
	 * @param linkToHub the target Hub
	 * @param obj the object that changed
	 */
	public <T extends OAObject, U extends OAObject> void updateLinkedFromHub(Hub<T> fromHub, Hub<U> linkToHub, U linkToObject);
	/**
	 * Updates the Hub linked to another Hub for an object/property change.
	 *
	 * @param hub the linked Hub
	 * @param linkToHub the target Hub
	 * @param obj the object that changed
	 * @param changedPropName the changed property name
	 */
	public <T extends OAObject, U extends OAObject> void updateLinkedFromHub(final Hub<T> fromHub, Hub<U> linkToHub, final U linkToObject, final String changedPropName);
	
	public <T extends OAObject, U extends OAObject> Object getPropertyValueInLinkedToHub(Hub<T> hubFrom, U objLinkToHub);
	
	/**
	 * Returns whether a Hub is linked by position.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if linked by position
	 */
	public boolean getLinkedOnPos(Hub<?> hub);
	/**
	 * Returns whether a Hub or copied Hub is linked by position.
	 *
	 * @param thisHub the Hub to inspect
	 * @param bIncludeCopiedHubs {@code true} to include copied Hubs
	 * @return {@code true} if linked by position
	 */
	public <T extends OAObject> boolean getLinkedOnPos(final Hub<T> thisHub, boolean bIncludeCopiedHubs);
	/**
	 * Returns the target property used by Hub linking.
	 *
	 * @param hub the Hub to inspect
	 * @return the target property name
	 */
	public String getLinkToProperty(Hub<?> hub);
	/**
	 * Returns the source property used by Hub linking.
	 *
	 * @param thisHub the Hub to inspect
	 * @return the source property name
	 */
	public String getLinkFromProperty(Hub<?> thisHub);
	/**
	 * Returns the source property used by Hub linking, optionally including copied Hubs.
	 *
	 * @param thisHub the Hub to inspect
	 * @param bIncludeCopiedHubs {@code true} to include copied Hubs
	 * @return the source property name
	 */
	public String getLinkFromProperty(Hub<?> thisHub, boolean bIncludeCopiedHubs);
    
}
