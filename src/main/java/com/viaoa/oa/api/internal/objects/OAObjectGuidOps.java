package com.viaoa.oa.api.internal.objects;

import java.util.UUID;

import com.viaoa.object.OAObject;

public interface OAObjectGuidOps {

	public void setGuid(OAObject oaObj, UUID iguid);
	public UUID getGuid(OAObject oaObj);
	
}
