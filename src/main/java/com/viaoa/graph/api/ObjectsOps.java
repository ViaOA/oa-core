package com.viaoa.graph.api;

import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;

public interface ObjectsOps {

	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade);
	
	
	public <T extends OAObject> T getObject(Class<T> clazz, Object key);

	
}
