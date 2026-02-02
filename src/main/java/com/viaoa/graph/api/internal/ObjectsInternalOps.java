package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.ObjectsOps;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public interface ObjectsInternalOps extends ObjectsOps {

	public OAObjectInfo getOAObjectInfo(Class<?> c);
	public OAObjectInfo getOAObjectInfo(OAObject obj);
	

//qqqqqqqq use Cache	
	public void setSelectAllHub(Hub hub);
	public void removeSelectAllHub(Hub hub);
	
	
}
