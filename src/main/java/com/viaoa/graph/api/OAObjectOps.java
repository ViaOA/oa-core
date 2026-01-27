package com.viaoa.graph.api;

import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public interface OAObjectOps {

	public OAObjectCacheOps cache();
	
	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade);
	
	public OAObjectInfo getOAObjectInfo(Class<?> c);
	public OAObjectInfo getOAObjectInfo(OAObject obj);
}
