package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectLockOps {

	public void lock(OAObject oaObj);
	public void unlock(OAObject oaObj);
	public boolean isLocked(OAObject oaObj);
	
}
