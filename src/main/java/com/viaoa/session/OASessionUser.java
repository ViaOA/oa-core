package com.viaoa.session;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public class OASessionUser<T extends OAObject> {

	private final Hub<T> userHub;
	
    private OASessionAccess sessionAccess;
	
	public OASessionUser(Hub<T> userHub) {
		this.userHub = userHub;
	}

	public Hub<T> getHub() {
		return userHub;
	}
	
	public OASessionAccess getSessionAccess() {
		return sessionAccess;
	}
	
	public void setSessionAccess(OASessionAccess sa) {
		this.sessionAccess = sa;
	}


}
