package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDataMaster;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

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
     * master/detail structures within the Object Graph.
     *
     * @param hub the master Hub
     * @param path the property path used to navigate relationships
     * @return a live detail Hub based on the supplied path
     */
    Hub<?> detail(Hub<?> hub, String path);

    
    public <T extends OAObject> void preloadDetailData(final Hub<T> thisHub, final int pos);	

    
	public OALinkInfo getLinkInfoFromMasterObjectToDetail(Hub<?> hub);	
	public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> hub);
	public void setMasterObject(Hub<?> hub, OAObject masterObject);
	public void setMasterObject(Hub<?> hub, OAObject masterObject, OALinkInfo liDetailToMaster);
	public HubDataMaster getDataMaster(Hub<?> hub);
	public boolean isOwned(Hub<?> hub);
	public Hub<?> getDetailHub(Hub<?> hub, String path);
	public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive, String selectOrder);
	public Hub<?> getDetailHub(Hub<?> hub, String path, boolean bShareActive);
	public Hub<?> getDetailHub(Hub<?> hub, String path, String selectOrder);
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, String path, Class<T> objectClass, boolean bShareActive);
	public <T extends OAObject> Hub<T> getDetailHub(Hub<?> hub, Class<T> clazz, boolean bShareActive, String selectOrder);
	public Hub<?> getDetailHub(Hub<?> hub, Class<? extends OAObject>[] classes);

	public void setMasterHub(Hub<?> thisHub, Hub<?> masterHub, String path, boolean bShared, String selectOrder);
	public Hub<? extends OAObject> getMasterHub(Hub<?> hub);
	public OAObject getMasterObject(Hub<?> hub);
	public Class<? extends OAObject> getMasterClass(Hub<?> hub);
	public boolean removeDetailHub(Hub<?> hub, Hub<?> hubDetail);
	public OALinkInfo getLinkInfoFromDetailToMaster(Hub<?> hub);
	public <T extends OAObject> Hub<T> getRealHub(Hub<T> hub);
	public String getPropertyFromMasterToDetail(Hub<?> hub);
	public String getPropertyFromDetailToMaster(Hub<?> hub);
	public boolean getIsFromSameMasterHub(Hub<?> hub1, Hub<?> hub2);
//remove, in Link.	public OALinkInfo getLinkInfoFromMasterToDetail(Hub<?> hub);

}
