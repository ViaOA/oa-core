package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Internal Hub search operation using property-path matching.
 */
public interface HubFindOps {

	
	/**
	 * Finds the first object matching a property-path value.
	 *
	 * @param hub the Hub to search
	 * @param path the property path to evaluate
	 * @param findValue the value to match
	 * @param bSetAO {@code true} to make the found object active
	 * @param lastFoundObject optional object after which searching continues
	 * @return the matching object, or {@code null}
	 */
	public <T extends OAObject> T findFirst(Hub<T> hub, String path, Object findValue, boolean bSetAO, T lastFoundObject);

}
