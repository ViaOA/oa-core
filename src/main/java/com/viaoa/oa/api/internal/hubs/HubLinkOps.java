package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

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
     * one Hub to follow another through the Object Graph as navigation occurs.
     *
     * @param hub1 the Hub to be linked and updated
     * @param hub2 the source Hub whose active object drives the link
     * @param referenceName the relationship property name on the source object's type
     */
    void link(Hub<?> hub1, Hub<?> hub2, String referenceName);
	public <T extends OAObject> Hub<T> getHubWithLink(Hub<T> hub, boolean bIncludeCopiedHubs);
	public void setLinkHub(Hub<?> thisHub, String propertyFrom, Hub<?> linkToHub, String propertyTo, boolean linkPosFlag, boolean bAutoCreate, boolean bAutoCreateAllowDups);
	public String getLinkHubPath(Hub<?> hub, boolean bIncludeCopiedHubs);
	public <T extends OAObject> void updateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj);
	public <T extends OAObject> void updateLinkedToHub(Hub<T> hub, Hub<?> linkToHub, T obj, String changedPropName);
	public <T extends OAObject, U extends OAObject> Object getPropertyValueInLinkedToHub(Hub<T> hub, U linkObject); // returns OAOject, null, or int (position)
	public boolean getLinkedOnPos(Hub<?> hub);
	public <T extends OAObject> boolean getLinkedOnPos(final Hub<T> thisHub, boolean bIncludeCopiedHubs);
	public String getLinkToProperty(Hub<?> hub);
	public String getLinkFromProperty(Hub<?> thisHub);
	public String getLinkFromProperty(Hub<?> thisHub, boolean bIncludeCopiedHubs);
    
}
