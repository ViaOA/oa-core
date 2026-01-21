package com.viaoa.graph.impl;

import com.viaoa.graph.OAObjectService;
import com.viaoa.graph.api.OAObjectCacheOps;
import com.viaoa.graph.api.OAObjectOps;

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

	
	
	
}
