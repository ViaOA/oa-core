package com.viaoa.graph.impl;

import com.viaoa.graph.api.OAObjectCacheOps;
import com.viaoa.graph.object.OAObjectCacheService;

public class OAObjectCacheOpsImpl implements OAObjectCacheOps {
	private OAObjectCacheService srvcOAObjectCache;
	
	public OAObjectCacheOpsImpl(OAObjectCacheService srvc) {
		this.srvcOAObjectCache = srvc;
	}
}
