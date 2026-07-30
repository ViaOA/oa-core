package com.viaoa.oa.service.object;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInternalBridge;

/**
 * Updates core OAObject lifecycle state flags.
 */
public abstract class OAObjectStateService {

	
	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();
	
	/**
	 * Performs OAObjectStateService behavior for the OA object service.
	 */
	public OAObjectStateService() {
    }
	
	/**
	 * Sets the new value.
	 *
	 * @param oaObj method input
	 * @param b method input
	 */
	public void setNew(final OAObject oaObj, final boolean b) {
		boolean old = faBridge.getObjectFriendAccess().getNewFlag(oaObj);
		if (b == old) {
			return;
		}
		faBridge.getObjectFriendAccess().setNew(oaObj, b);
		
		callEventFirePropertyChange(oaObj, "New", old, b, false, false);
		if (!b) {
			callAutoAddSetAutoAdd(oaObj, true);
		}
	}

	/**
	 * Dependency hook used by this service to eventFireBeforePropertyChange.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param oldObj method input
	 * @param newObj method input
	 * @param bLocalOnly method input
	 * @param bSetChanged method input
	 */
	public abstract void callEventFireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged);

	/**
	 * Dependency hook used by this service to eventFirePropertyChange.
	 *
	 * @param oaObj method input
	 * @param propertyName method input
	 * @param oldObj method input
	 * @param newObj method input
	 * @param bLocalOnly method input
	 * @param bSetChanged method input
	 */
	public abstract void callEventFirePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	/**
	 * Dependency hook used by this service to autoAddSetAutoAdd.
	 *
	 * @param oaObj method input
	 * @param bEnabled method input
	 */
	public abstract void callAutoAddSetAutoAdd(final OAObject oaObj, boolean bEnabled);
}
