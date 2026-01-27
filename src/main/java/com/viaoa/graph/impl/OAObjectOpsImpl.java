package com.viaoa.graph.impl;

import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.api.OAObjectCacheOps;
import com.viaoa.graph.api.OAObjectOps;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;

public class OAObjectOpsImpl implements OAObjectOps {

	private OAObjectService srvcOAObject;
	private OAObjectCacheOps opsOAObjectCache;
	
	public OAObjectOpsImpl(OAObjectService srvcOAObject) {
		this.srvcOAObject = srvcOAObject;
	}
	
	@Override
	public OAObjectCacheOps cache() {
		if (opsOAObjectCache != null) {
			opsOAObjectCache = new OAObjectCacheOpsImpl(srvcOAObject.getOAObjectCacheService());
		}
		return opsOAObjectCache;
	}

	@Override
	public void save(OAObject oaObj, int iCascadeRule, OACascade cascade) {
		srvcOAObject.getOAObjectSaveService().save(oaObj, iCascadeRule, cascade);
		
	}
	
	@Override
	public OAObjectInfo getOAObjectInfo(Class<?> c) {
		return srvcOAObject.getOAObjectInfoService().getObjectInfo(c);
	}

	@Override
	public OAObjectInfo getOAObjectInfo(OAObject obj) {
		if (obj == null) return null;
		return getOAObjectInfo(obj.getClass());
	}

	
	
	
}
