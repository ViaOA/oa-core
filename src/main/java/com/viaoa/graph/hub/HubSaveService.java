package com.viaoa.graph.hub;

import java.util.logging.Logger;

import com.viaoa.graph.HubService;
import com.viaoa.graph.OAObjectService;
import com.viaoa.hub.*;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;

public class HubSaveService {
	private final Logger LOG = Logger.getLogger(HubSaveService.class.getName());

	private final OAObjectService srvcObject;
	private final HubService srvcHub;
	private final Hub.FriendAccess faHub;
	
	public HubSaveService(OAObjectService srvcObject, HubService srvcHub, Hub.FriendAccess faHub ) {
    	if (srvcObject == null) throw new IllegalArgumentException("OAObjectService can not be null");
    	this.srvcObject = srvcObject;
    	if (srvcHub == null) throw new IllegalArgumentException("HubService can not be null");
    	this.srvcHub = srvcHub;
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	/**
	 * Saves all objects in the specified Hub using the given cascade rule.
	 *
	 * <p>Creates a new {@link OACascade} instance and delegates to
	 * {@link #saveAll(Hub, int, OACascade)}.</p>
	 *
	 * @param thisHub      the Hub whose contents are to be saved
	 * @param cascadeRule  the cascade behavior to apply during persistence
	 */
    public void saveAll(Hub thisHub, int cascadeRule) {
        OACascade cascade = new OACascade(); 
        srvcHub.getHubSaveService().saveAll(thisHub, cascadeRule, cascade);
    }
	
    /*
     * Note: setting iCascadeRule to OAObject.CASCADE_NONE will not save the objects, but will update the M2M links.
     */
    /**
     * Saves all objects in the specified Hub according to the supplied cascade rule
     * and cascade-tracking context.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Returns immediately if the Hub is {@code null}.</li>
     *   <li>Uses {@link OACascade#wasCascaded} to prevent duplicate processing.</li>
     *   <li>If cascading is enabled (rule not {@code CASCADE_NONE}):
     *     <ul>
     *       <li>Iterates through loaded objects and delegates persistence to
     *           {@link OAObjectSaveDelegate} for OAObjects.</li>
     *     </ul>
     *   </li>
     *   <li>If cascading is disabled ({@code CASCADE_NONE}):
     *     <ul>
     *       <li>Detects Many-to-Many relationships.</li>
     *       <li>Saves newly added objects to ensure they have valid DB records
     *           before link updates occur.</li>
     *     </ul>
     *   </li>
     *   <li>Calls {@link HubDelegate#_updateHubAddsAndRemoves} to update links.</li>
     *   <li>Clears change tracking and referenceable state afterward.</li>
     * </ul>
     *
     * @param thisHub      the Hub to save
     * @param iCascadeRule the cascade rule controlling persistence behavior
     * @param cascade      the cascade tracker preventing repeat processing
     */
    public void saveAll(Hub thisHub, int iCascadeRule, OACascade cascade) {
        if (thisHub == null) return; //qq need to log this
        if (cascade.wasCascaded(thisHub, true)) return;

        boolean bM2M = false;
        if (iCascadeRule != OAObject.CASCADE_NONE) {
	        boolean b = thisHub.isOAObject();
	        int x = thisHub.getCurrentSize(); // only check the objects that are loaded
	        for (int i=0; i<x ; i++) {
	            Object obj = thisHub.elementAt(i);
	            if (obj == null) break;
	            if (b) {
	            	srvcObject.getOAObjectSaveService().save((OAObject)obj, iCascadeRule, cascade);
	            }
	            else {
	            	// srvcObject.getOAObjectDSService().save(obj, true);  // true=insert.  Could be update?
	            	//todo: qqqqqqqq 
	            }
	        }
        }
        else {
	        // if Many2Many, then save all Added objects that are New, so that a valid DB record exists before calling updateHubAddsAndRemoves()
			HubDataMaster dm = srvcHub.getHubDetailService().getDataMaster(thisHub);
	        bM2M = dm.getDetailToMasterLinkInfo() != null && srvcObject.getOAObjectInfoService().isMany2Many(dm.getDetailToMasterLinkInfo());
	        
	        if (bM2M) {
		        OAObject[] objAdds = srvcHub.getHubDataService().getAddedObjects(thisHub);
	        	for (int i=0; objAdds!=null && i<objAdds.length; i++) {
	        		OAObject obj = objAdds[i];
	        		if (obj != null && ((OAObject)obj).getNew()) {
	        			srvcObject.getOAObjectSaveService()._saveObjectOnly((OAObject) obj, cascade);
	        		}
	        	}
	        }
        }
        
        srvcHub._updateHubAddsAndRemoves(thisHub, iCascadeRule, cascade, true);
    	thisHub.setChanged(false); // removes all vecAdd, vecRemove objects
    	
    	srvcHub.setReferenceable(thisHub, false);
    }

	
	
	
}


