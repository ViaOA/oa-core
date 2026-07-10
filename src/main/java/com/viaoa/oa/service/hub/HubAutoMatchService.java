package com.viaoa.oa.service.hub;

import java.util.logging.Logger;
import com.viaoa.hub.*;
import com.viaoa.hub.auto.HubAutoMatch;
import com.viaoa.object.OAObject;

/**
 * Manages automatic matching between Hubs and related master data.
 */

public abstract class HubAutoMatchService {
	private final Logger LOG = Logger.getLogger(HubAutoMatchService.class.getName());

	private final Hub.FriendAccess faHub;

	public HubAutoMatchService(Hub.FriendAccess faHub) {
		if (faHub == null) throw new IllegalArgumentException("Hub.FriendAccess can not be null");
		this.faHub = faHub;
	}

	/**
	 * Ensures that for every object in the master hub, there is a corresponding
	 * object in this hub whose specified property points to that master object.
	 * Existing auto-match handlers are closed before creating a new one. The match
	 * logic supports server-side restriction.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property on this hub's objects used to match
	 * @param hubMaster       the hub whose objects must be mirrored
	 * @param bServerSideOnly whether matching should only be enforced on the server
	 */
	public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T, T2> setAutoMatch(Hub<T> thisHub, String property, Hub<T2> hubMaster, boolean bServerSideOnly) {
		if (thisHub == null) return null;
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getAutoMatch() != null) {
			hd.getAutoMatch().close();
			hd.setAutoMatch(null);
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch<T, T2> am = new HubAutoMatch<>();
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, null, null);
		hd.setAutoMatch(am);
		return am;
		// }
	}

	/**
	 * Variant of auto-match initialization that includes a stopping condition. For
	 * each object in the master hub, this hub ensures a corresponding object exists
	 * unless the match path encounters the specified stop object and property.
	 *
	 * @param thisHub         the hub being synchronized
	 * @param property        the property used to link to master hub objects
	 * @param hubMaster       the hub being mirrored
	 * @param bServerSideOnly whether matching is server-only
	 * @param objStop         optional object used to limit matching
	 * @param stopProperty    the property that defines the stopping condition
	 */
	public <T extends OAObject, T2 extends OAObject> HubAutoMatch<T, T2> setAutoMatch(Hub<T> thisHub, String property, Hub<T2> hubMaster, boolean bServerSideOnly, OAObject objStop, String stopProperty) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		if (hd.getAutoMatch() != null) {
			hd.getAutoMatch().close();
			hd.setAutoMatch(null);
		}
		// 20220802 now works with Enum (name/value) property
		// if (hubMaster != null) {
		HubAutoMatch<T, T2> am = new HubAutoMatch<>();
		am.setServerSideOnly(bServerSideOnly);
		am.init(thisHub, property, hubMaster, objStop, stopProperty);
		hd.setAutoMatch(am);
		return am;
		// }
	}

	/**
	 * Returns the auto-match controller for this hub, or {@code null} if no
	 * auto-match logic is configured.
	 *
	 * @param thisHub the hub whose auto-match handler is requested
	 * @return the auto-match object, or {@code null} if none exists
	 */
	@SuppressWarnings({"unchecked"})
	public <T extends OAObject> HubAutoMatch<T, ? extends OAObject> getAutoMatch(Hub<T> thisHub) {
		final HubData<T> hd = faHub.getHubData(thisHub);
		return hd.getAutoMatch();
	}


}
