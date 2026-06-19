package com.viaoa.graph.api.internal.objects;

import com.viaoa.object.OAObject;

public interface OAObjectDSOps {

	public boolean getAssignIdOnCreate(OAObject oaObj);
	public void assignId(OAObject oaObj);
	public void setAssigningId(OAObject oaObj, boolean bIsAssigningId);

}
