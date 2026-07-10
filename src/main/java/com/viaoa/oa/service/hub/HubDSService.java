package com.viaoa.oa.service.hub;

import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.*;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.*;
import com.viaoa.runtime.OARuntime;

/**
 * Coordinates datasource-backed Hub selection and load state.
 */

public abstract class HubDSService {
	private final Logger LOG = Logger.getLogger(HubDSService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubDSService(Hub.FriendAccess faHub) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Returns the {@link OADataSource} associated with the specified class.
	 * Delegates directly to {@link OADataSource#getDataSource(Class)}.
	 *
	 * @param c the class used to look up its data source
	 * @return the data source for the class, or null if none exists
	 */
	public OADataSource getDataSource(Class<?> c) {
	    return OARuntime.datasource().get(c);
	}
    
	/**
	 * Updates many-to-many link-table records for the specified master object.
	 * Retrieves the appropriate data source and forwards the request to its
	 * {@code updateMany2ManyLinks} method.
	 *
	 * @param masterObject   the master object whose link table is updated
	 * @param adds           objects to add to the link table
	 * @param removes        objects to remove from the link table
	 * @param propFromMaster the name of the master-side property for the link
	 */
	public <T extends OAObject> void updateMany2ManyLinks(OAObject masterObject, T[] adds, T[] removes, String propFromMaster) {
		OADataSource ds = OARuntime.datasource().get(masterObject.getClass());
		if (ds != null) ds.updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
	}

	/**
	 * Removes many-to-many link-table records associated with the removed
	 * objects in the given hub. Only applies when the hub represents a
	 * many-to-many relationship and has removed objects tracked.
	 *
	 * @param hub the hub whose removed objects should have link records deleted
	 */
    public <T extends OAObject> void removeMany2ManyLinks(Hub<T> hub) {
        if (hub == null) return;
        OAObject objMaster = hub.getMasterObject();
        if (objMaster == null) return;
        OALinkInfo link = faHub.getHubDataMaster(hub).getDetailToMasterLinkInfo();
        if (link == null) return;
        if (!callObjectInfoIsMany2Many(link)) return;
        
        String propFromMaster = callObjectInfoGetReverseLinkInfo(link).getName();

        T[] objs = callHubAddRemoveGetRemovedObjects(hub);
        if (objs == null || objs.length == 0) return;
       
        OADataSource ds = OARuntime.datasource().get(objMaster.getClass());
        if (ds == null) return;
        
        ds.updateMany2ManyLinks(objMaster, null, objs, propFromMaster);
    }

	/**
	 * Dependency hook used by this service for ObjectInfoIsMany2Many behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */

	public abstract boolean callObjectInfoIsMany2Many(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for ObjectInfoGetReverseLinkInfo behavior.
	 *
	 * @param thisLi method input
	 * @return result value
	 */
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);
	/**
	 * Dependency hook used by this service for HubAddRemoveGetRemovedObjects behavior.
	 *
	 * @param thisHub method input
	 * @return result value
	 */
	public abstract <T extends OAObject> T[] callHubAddRemoveGetRemovedObjects(Hub<T> thisHub);
}


