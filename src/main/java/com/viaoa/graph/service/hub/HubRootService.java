package com.viaoa.graph.service.hub;

import java.util.logging.Logger;

import com.viaoa.annotation.OAParentProvided;
import com.viaoa.hub.*;
import com.viaoa.object.*;
import com.viaoa.util.OAFilter;

public abstract class HubRootService {
	private final Logger LOG = Logger.getLogger(HubRootService.class.getName());

	private final Hub.FriendAccess faHub;
	
	public HubRootService(Hub.FriendAccess faHub ) {
    	if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
    	this.faHub = faHub;
	}

	
	/**
	 * Determines and returns the root Hub for a recursive Hub hierarchy.
	 *
	 * <p>Behavior includes:</p>
	 * <ul>
	 *   <li>Checks whether the Hub’s object type has a recursive link.</li>
	 *   <li>If a root Hub is already registered via {@link OAObjectInfoDelegate}, returns it.</li>
	 *   <li>Examines shared Hubs, master/detail links, and ownership flags to
	 *       determine the correct root for the recursion chain.</li>
	 *   <li>Handles complex cases such as owner-based recursion, multiple master hubs,
	 *       or when the parent link is not part of the recursive relationship.</li>
	 *   <li>Returns {@code null} if no root Hub can be determined.</li>
	 * </ul>
	 *
	 * @param thisHub the Hub whose recursive root is being requested
	 * @return the root Hub for this recursive Hub, or {@code null} if not recursive
	 */
	public Hub getRootHub(final Hub thisHub) {
		if (thisHub == null) {
			return null;
		}
		OALinkInfo liRecursive = callObjectInfoGetRecursiveLinkInfo(faHub.getHubData(thisHub).getObjectInfo(), OALinkInfo.ONE);
		// 1: must be recursive
		if (liRecursive == null) {
			return null;
		}

		// 2: check for root hub
		Hub h = callObjectInfoGetRootHub(faHub.getHubData(thisHub).getObjectInfo());
		if (h != null) {
			return h;
		}

		// 3: get dm
		// 20120717 could be more then one master hub available, find the one that owns this object
		OAFilter<Hub> filter = new OAFilter<Hub>() {
			@Override
			public boolean isUsed(Hub hx) {
				return (faHub.getHubDataMaster(hx).getMasterHub() != null);
			}
		};
		Hub[] hubs = callHubShareGetAllSharedHubs(thisHub, filter);
		HubDataMaster dm = null;
		for (int i = 0; hubs != null && i < hubs.length; i++, dm = null) {
			dm = faHub.getHubDataMaster(hubs[i]);
			if (dm.getDetailToMasterLinkInfo() == null) {
				continue;
			}
			OALinkInfo rev = callObjectInfoGetReverseLinkInfo(faHub.getHubDataMaster(hubs[i]).getDetailToMasterLinkInfo());
			if (rev != null && rev.isOwner()) {
				if (rev.getType() == OALinkInfo.TYPE_MANY && rev.getToClass().equals(thisHub.getObjectClass())) {
					break;
				}
			}
		}
		if (dm == null) {
			dm = faHub.getHubDataMaster(thisHub);
			// was: HubDataMaster dm = srvcHub.getHubDetailService().getDataMaster(thisHub);
		}

		// 20120304 added other cases on how to find the root hub
		if (dm.getDetailToMasterLinkInfo() == null) {
			return callObjectInfoGetRootHub(faHub.getHubData(thisHub).getObjectInfo());
		}
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null && faHub.getHubDataMaster(thisHub).getMasterHub() == null) {
			return callObjectInfoGetRootHub(faHub.getHubData(thisHub).getObjectInfo());
		}
		if (faHub.getHubDataMaster(thisHub).getMasterObject() == null) {
			if (faHub.getHubDataMaster(thisHub).getMasterHub() != null) {
				Class mc = faHub.getHubDataMaster(thisHub).getMasterHub().getObjectClass();
				if (mc != null) {
					if (mc.equals(thisHub.getObjectClass())) {
						h = getRootHub(faHub.getHubDataMaster(thisHub).getMasterHub());
						if (h != null) {
							return h;
						}
					} else {
						// could be owner / master Hub
						if (callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo()).getOwner()) {
							return thisHub; // thisHub is a detail from the owner.  When the owner hub AO is changed, then thisHub will have root
						}
					}
				}
			}
			return callObjectInfoGetRootHub(faHub.getHubData(thisHub).getObjectInfo());
		}
		// End 20120304

		/*was
		// 4: check to see if there is a valid masterObject - must have a link to it
		if (faHub.getHubDataMaster(thisHub).masterObject == null || dm.liDetailToMaster == null) {
		    // does not belong to a owner or master object.
		    // The root hub needs to be manually set by calling Hub.setRootHub,
		    //     since the recursive hub does not have an owner object
		    return ObjectInfoGetRootHub(thisHub.datau.objectInfo);
		}
		*/

		// 5: if parent is not recursive - if the LinkInfos are different
		if (dm.getDetailToMasterLinkInfo() != callObjectInfoGetRecursiveLinkInfo(faHub.getHubData(thisHub).getObjectInfo(), OALinkInfo.ONE)) {
			// if dm.masterObject is owner, then it is owner
			OALinkInfo rli = callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo());
			if (rli == null) {
				LOG.warning("cant find reverse linkInfo, hub=" + thisHub);
			}

			if (rli != null && rli.getOwner()) {
				// found the root hub and owner
				// cant use the masterHub, need to get the "real" detail hub of master object
				//   For recursive hubs that are linked, the master (owner) might not be using the root hub.
				//   By getting the hub value of the masterObject, it will call its hub getMethod, which will be the root hub
				return (Hub) callObjectReflectGetProperty(	(OAObject) dm.getMasterObject(),
																	callObjectInfoGetReverseLinkInfo(dm.getDetailToMasterLinkInfo()).getName());
			}

			// the linkInfo for the parent is not the owner or a recursive parent
			// The root hub needs to be manually set by calling Hub.setRootHub,
			//     since the recursive hub does not have an owner object
			return callObjectInfoGetRootHub(faHub.getHubData(thisHub).getObjectInfo());
		}

		// 6: dm.masterObject is the same as this class - recursive parent hub
		//    use it to get the owner object and then the root hub (from owner object)
		// find owner link
		OALinkInfo linkOwner = callObjectInfoGetLinkToOwner(faHub.getHubData(thisHub).getObjectInfo());
		if (linkOwner != null) {
			OALinkInfo liRev = callObjectInfoGetReverseLinkInfo(linkOwner);
			if (liRev != null && liRev.getType() == OALinkInfo.MANY) {
				// get owner object:
				Object owner = callObjectReflectGetProperty((OAObject) dm.getMasterObject(), linkOwner.getName());
				if (owner != null) {
					Object root = callObjectReflectGetProperty((OAObject) owner, liRev.getName());
					if (!(root instanceof Hub)) {
						throw new RuntimeException("Hub.getRootHub() method from owner object not returning a Hub.");
					}
					return (Hub) root;
				}
			}
		}

		return null;
	}

	/**
	 * Explicitly assigns or removes the root Hub designation for a recursive
	 * Hub class.
	 *
	 * <p>If {@code b} is {@code true}, the supplied Hub becomes the root Hub
	 * for all recursive Hubs of its object class. If {@code false}, any
	 * previously registered root is cleared.</p>
	 *
	 * <p>Used when recursive relationships do not have an owner object to
	 * automatically determine the root Hub.</p>
	 *
	 * @param thisHub the Hub to set or clear as the root
	 * @param b       {@code true} to set thisHub as root, {@code false} to remove it
	 */
	public void setRootHub(Hub thisHub, boolean bIsRoot) {
		if (thisHub == null) return;
		callObjectInfoSetRootHub(faHub.getHubData(thisHub).getObjectInfo(), bIsRoot ? thisHub : null);
	}

	
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getRecursiveLinkInfo")
	public abstract OALinkInfo callObjectInfoGetRecursiveLinkInfo(OAObjectInfo thisOI, int type);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getRootHub")
	public abstract Hub callObjectInfoGetRootHub(OAObjectInfo thisOI);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getReverseLinkInfo")
	public abstract OALinkInfo callObjectInfoGetReverseLinkInfo(OALinkInfo thisLi);

	@OAParentProvided (example = "srvcObject.getOAObjectReflectService().getProperty")
	public abstract Object callObjectReflectGetProperty(OAObject oaObj, String propPath);
	
	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().getLinkToOwner")
	public abstract OALinkInfo callObjectInfoGetLinkToOwner(OAObjectInfo thisOI);

	@OAParentProvided (example = "srvcObject.getOAObjectInfoService().setRootHub")
	public abstract void callObjectInfoSetRootHub(OAObjectInfo thisOI, Hub h);

	@OAParentProvided (example = "srvcHub.getHubShareService().getAllSharedHubs")
	public abstract Hub[] callHubShareGetAllSharedHubs(Hub thisHub, OAFilter<Hub> filter);

}
