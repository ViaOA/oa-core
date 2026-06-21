package com.viaoa.graph.service.object;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInternalBridge;

public abstract class OAObjectStateService {

	
	private final OAObjectInternalBridge faBridge = new OAObjectInternalBridge();
	
	public OAObjectStateService() {
    }
	
	public void setNew(final OAObject oaObj, final boolean b) {
		boolean old = faBridge.getObjectFriendAccess().getNewFlag(oaObj);
		if (b == old) {
			return;
		}
		callEventFireBeforePropertyChange(oaObj, "New", old, b, false, false);

		faBridge.getObjectFriendAccess().setNew(oaObj, b);
		
		callEventFirePropertyChange(oaObj, "New", old, b, false, false);
		if (!b) {
			callAutoAddSetAutoAdd(oaObj, true);
		}
	}

	public abstract void callEventFireBeforePropertyChange(final OAObject oaObj, final String propertyName,
			Object oldObj, final Object newObj, final boolean bLocalOnly, final boolean bSetChanged);

	public abstract void callEventFirePropertyChange(final OAObject oaObj, final String propertyName, Object oldObj, Object newObj, boolean bLocalOnly, boolean bSetChanged);
	public abstract void callAutoAddSetAutoAdd(final OAObject oaObj, boolean bEnabled);
}
