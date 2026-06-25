package com.viaoa.oa.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectInitializeOps {

	public boolean initialize(OAObject oaObj);
	public void initializeAfterLoading(OAObject oaObj);
	public void initializeAfterLoading(OAObject oaObj, boolean bAssignNewId, boolean bInitializeNulls, boolean bSetChangedToFalse);
	
}
