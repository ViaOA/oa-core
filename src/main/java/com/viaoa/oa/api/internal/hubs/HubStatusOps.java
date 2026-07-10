package com.viaoa.oa.api.internal.hubs;

import java.util.ArrayList;
import com.viaoa.cascade.OACascade;
import com.viaoa.hub.Hub;
import com.viaoa.hub.Hub.HubCurrentStateEnum;
import com.viaoa.object.OAObject;

/**
 * Internal Hub validity, changed-state, and current-state comparison operations.
 */
public interface HubStatusOps {
	/**
	 * Returns whether a Hub is internally valid.
	 *
	 * @param hub the Hub to inspect
	 * @return {@code true} if valid
	 */
	public boolean isValid(Hub<?> hub);
	/**
	 * Returns whether a Hub or included objects have changed.
	 *
	 * @param thisHub the Hub to inspect
	 * @param iCascadeRule the cascade rule
	 * @param cascade the cascade context
	 * @return {@code true} if changed
	 */
	public boolean getChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade); 
	/**
	 * Compares current Hub state to a new Hub/list state.
	 *
	 * @param thisHub the existing Hub
	 * @param hubNew optional new Hub state
	 * @param alNew optional new list state
	 * @return the current-state comparison result
	 */
	public <T extends OAObject> HubCurrentStateEnum getCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew);
	/**
	 * Sets Hub changed state.
	 *
	 * @param hub the Hub to update
	 * @param bIsChanged {@code true} to mark changed
	 */
	public void setChanged(Hub<?> hub, boolean bIsChanged);
}
