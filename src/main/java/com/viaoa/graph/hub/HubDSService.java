package com.viaoa.graph.hub;

import java.util.*;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.graph.HubService;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.*;
import com.viaoa.object.*;

public class HubDSService {
	private final Logger LOG = Logger.getLogger(HubDSService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	public HubDSService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub ) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
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
	public OADataSource getDataSource(Class c) {
	    return OADataSource.getDataSource(c);
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
	public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
		//qqqqqqqqqq method was protected
		OADataSource ds = OADataSource.getDataSource(masterObject.getClass());
		if (ds != null) ds.updateMany2ManyLinks(masterObject, adds, removes, propFromMaster);
	}

	/**
	 * Removes many-to-many link-table records associated with the removed
	 * objects in the given hub. Only applies when the hub represents a
	 * many-to-many relationship and has removed objects tracked.
	 *
	 * @param hub the hub whose removed objects should have link records deleted
	 */
    public void removeMany2ManyLinks(Hub hub) {
        if (hub == null) return;
        Object objMaster = hub.getMasterObject();
        if (objMaster == null) return;
        if (!OAObject.class.isAssignableFrom(hub.getObjectClass())) {
            return;
        }
        OALinkInfo link = faHub.getHubDataMaster(hub).getDetailToMasterLinkInfo();
        if (link == null) return;
        if (!srvcObject.getOAObjectInfoService().isMany2Many(link)) return;
        
        String propFromMaster = srvcObject.getOAObjectInfoService().getReverseLinkInfo(link).getName();

        OAObject[] objs = srvcHub.getHubAddRemoveService().getRemovedObjects(hub);
        if (objs == null || objs.length == 0) return;
       
        OADataSource ds = OADataSource.getDataSource(objMaster.getClass());
        if (ds == null) return;
        
        ds.updateMany2ManyLinks((OAObject)objMaster, null, objs, propFromMaster);
    }


	
}


