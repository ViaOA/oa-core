package com.viaoa.session;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public class OASessionUser<T extends OAObject> {

	private final T userObject;
	private final Hub<T> userHub;
	
    private OASessionAccess sessionAccess;
	
	public OASessionUser(T userObject) {
		this.userObject = userObject;
		this.userHub = null;
	}

	public OASessionUser(Hub<T> userHub) {
		this.userObject = null;
		this.userHub = userHub;
	}

	public T getCalcUserObject() {
		if (userObject != null) return userObject;
		if (userHub == null) return null;
		return userHub.getActiveObject();
	}

	public Hub<T> getUserHub() {
		return userHub;
	}
	public T getUserObject() {
		return userObject;
	}
	
	public OASessionAccess getSessionAccess() {
		return sessionAccess;
	}
	
	public void setSessionAccess(OASessionAccess sa) {
		this.sessionAccess = sa;
	}


}
