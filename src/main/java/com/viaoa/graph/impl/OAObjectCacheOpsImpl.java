package com.viaoa.graph.impl;

import com.viaoa.graph.api.OAObjectCacheOps;
import com.viaoa.graph.object.OAObjectCacheService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;

public class OAObjectCacheOpsImpl implements OAObjectCacheOps {
	private OAObjectCacheService srvcOAObjectCache;
	
	public OAObjectCacheOpsImpl(OAObjectCacheService srvc) {
		this.srvcOAObjectCache = srvc;
	}

	@Override
	public void setSelectAllHub(Hub hub) {
		srvcOAObjectCache.setSelectAllHub(hub);
	}

	@Override
	public void removeSelectAllHub(Hub hub) {
		srvcOAObjectCache.removeSelectAllHub(hub);
	}


}
