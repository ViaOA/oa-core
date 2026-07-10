package com.viaoa.oa.api.services.hubs;

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.hub.Hub.HubCurrentStateEnum;
import com.viaoa.object.OAObject;

/**
 * Public OA Hub status service operations.
 */
public interface HubStatusOps {
	/**
	 * Compares the current Hub state with a replacement Hub or list state.
	 *
	 * @param thisHub the existing Hub
	 * @param hubNew optional replacement Hub state
	 * @param alNew optional replacement list state
	 * @return the current-state comparison result
	 */
	public <T extends OAObject> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew);
}
