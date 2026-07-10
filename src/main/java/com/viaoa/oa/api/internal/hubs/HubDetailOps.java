package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

/**
 * Internal master/detail operations for Hubs, including detail Hub creation, master wiring, and relationship metadata lookup.
 */
public interface HubDetailOps {

    /**
     * Creates a detail Hub based on the supplied master Hub and property path.
     * <p>
     * {@code detail(...)} creates a {@link Hub} that represents the objects
     * referenced by the supplied property path from the active object of the
     * master Hub. The returned Hub is a live structure that automatically updates
     * as the active object or its referenced relationships change.
     * <p>
     * The {@code path} defines the relationship traversal using model property
     * names (for example, {@code "orders"} or {@code "orders.lineItems"}).
     * <p>
     * This is the primary verb for navigating relationships and creating
     * master/detail structures within the OA model.
     *
     * @param hub the master Hub
     * @param path the property path used to navigate relationships
     * @return a live detail Hub based on the supplied path
     */
    Hub<?> detail(Hub<?> hub, String path);

    
	/**
	 * Preloads detail data for the object at a Hub position.
	 *
	 * @param thisHub the master Hub
	 * @param pos the master object position
	 */
    public <T extends OAObject> void preloadDetailData(final Hub<T> thisHub, final int pos);	

    
	/**
	 * Returns link metadata from the master object to this detail Hub.
	 *
	 * @param hub the detail Hub
	 * @return the master-to-detail link metadata
	 */
	public OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub<?> hub);	
	/**
	 * Returns link metadata from master Hub to detail Hub.
	 *
	 * @param hub the detail Hub
	 * @return the master-to-detail link metadata
	 */
	public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> hub);
	/**
	 * Sets the master object for a detail Hub.
	 *
	 * @param hub the detail Hub
	 * @param masterObject the master object
	 */
	public void setMasterObject(Hub<?> hub, OAObject masterObject);
	/**
	 * Sets the master object and detail-to-master link metadata for a detail Hub.
	 *
	 * @param hub the detail Hub
	 * @param masterObject the master object
	 * @param liDetailToMaster the detail-to-master link metadata
	 */
	public void setMasterObject(Hub<?> hub, OAObject masterObject, OALinkInfo liDetailToMaster);
	/**
	 * Returns the internal master-data object for a Hub.
	 *
	 * @param hub the Hub to inspect
	 * @return the master-data object
	 */
	public HubDataMaster getDataMaster(Hub<?> hub);
	/**
	 * Returns whether a detail Hub is owned through its master relationship.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if owned
	 */
	public boolean isOwned(Hub<?> hub);
	/**
	 * Returns a detail Hub for a property path.
	 *
	 * @param hub the master Hub
	 * @param path the property path
	 * @return the detail Hub
	 */
	public Hub<?> getDetailHub(Hub<?> hub, String path);
	/**
	 * Returns a detail Hub for a property path with active-object sharing and select-order options.
	 *
	 * @param hub the master Hub
	 * @param path the property path
	 * @param bShareActive {@code true} to share active-object behavior
	 * @param selectOrder optional select order
	 * @return the detail Hub
	 */
	public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder);
	/**
	 * Returns a detail Hub for a property path with active-object sharing control.
	 *
	 * @param hub the master Hub
	 * @param path the property path
	 * @param bShareActive {@code true} to share active-object behavior
	 * @return the detail Hub
	 */
	public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive);
	/**
	 * Returns a detail Hub for a property path with select order.
	 *
	 * @param hub the master Hub
	 * @param path the property path
	 * @param selectOrder optional select order
	 * @return the detail Hub
	 */
	public Hub<?> getDetailHub(Hub<?> hub, String path, String selectOrder);
	/**
	 * Returns a typed detail Hub for a property path.
	 *
	 * @param hub the master Hub
	 * @param path the property path
	 * @param objectClass the detail object class
	 * @param bShareActive {@code true} to share active-object behavior
	 * @return the detail Hub
	 */
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive);
	/**
	 * Returns a typed detail Hub for a detail class.
	 *
	 * @param hub the master Hub
	 * @param clazz the detail object class
	 * @param bShareActive {@code true} to share active-object behavior
	 * @param selectOrder optional select order
	 * @return the detail Hub
	 */
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder);
	/**
	 * Returns a detail Hub by candidate detail classes.
	 *
	 * @param hub the master Hub
	 * @param classes candidate detail classes
	 * @return the detail Hub
	 */
	public Hub<?> getDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes);

	/**
	 * Sets the master Hub relationship for a detail Hub.
	 *
	 * @param thisHub the detail Hub
	 * @param masterHub the master Hub
	 * @param path the master-to-detail property path
	 * @param bShared {@code true} when the detail Hub is shared
	 * @param selectOrder optional select order
	 */
	public void setMasterHub(Hub<?> thisHub, Hub<?> masterHub, String path, boolean bShared, String selectOrder);
	/**
	 * Returns the master Hub for a detail Hub.
	 *
	 * @param hub the detail Hub
	 * @return the master Hub, or {@code null}
	 */
	public Hub<? extends OAObject> getMasterHub(Hub<?> hub);
	/**
	 * Returns the master object for a detail Hub.
	 *
	 * @param hub the detail Hub
	 * @return the master object, or {@code null}
	 */
	public OAObject getMasterObject(Hub<?> hub);
	/**
	 * Returns the master object class for a detail Hub.
	 *
	 * @param hub the detail Hub
	 * @return the master class, or {@code null}
	 */
	public Class<? extends OAObject> getMasterClass(Hub<?> hub);
	/**
	 * Removes a registered detail Hub from a master Hub.
	 *
	 * @param hub the master Hub
	 * @param hubDetail the detail Hub to remove
	 * @return {@code true} if removed
	 */
	public boolean removeDetailHub(Hub<?> hub, Hub<?> hubDetail);
	/**
	 * Returns link metadata from detail objects to the master object.
	 *
	 * @param hub the detail Hub
	 * @return the detail-to-master link metadata
	 */
	public OALinkInfo getLinkInfoFromDetailToMaster(Hub<?> hub);
	/**
	 * Returns the real backing Hub when the supplied Hub is a detail/shared view.
	 *
	 * @param hub the Hub to resolve
	 * @return the real backing Hub
	 */
	public <T extends OAObject> Hub<T> getRealHub(Hub<T> hub);
	/**
	 * Returns the property name from master to detail.
	 *
	 * @param hub the detail Hub
	 * @return the master-to-detail property name
	 */
	public String getPropertyFromMasterToDetail(Hub<?> hub);
	/**
	 * Returns the property name from detail to master.
	 *
	 * @param hub the detail Hub
	 * @return the detail-to-master property name
	 */
	public String getPropertyFromDetailToMaster(Hub<?> hub);
	/**
	 * Returns whether two Hubs come from the same master Hub.
	 *
	 * @param hub1 the first Hub
	 * @param hub2 the second Hub
	 * @return {@code true} if both Hubs share the same master Hub
	 */
	public boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2);
//remove, in Link.	public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> hub);

}
