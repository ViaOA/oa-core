package com.viaoa.graph.api;

import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;

public interface OAObjectCacheOps {

	public void setSelectAllHub(Hub hub);
	public void removeSelectAllHub(Hub hub);
	
	public <T extends OAObject> T getObject(Class<T> clazz, Object key);

}
