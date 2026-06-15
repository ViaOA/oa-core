package com.viaoa.graph.service.facade;

import com.viaoa.graph.api.services.ObjectsOps;
import com.viaoa.graph.api.services.objects.OAObjectCacheOps;
import com.viaoa.graph.service.OAObjectInternalService;

public class ObjectsOpsImpl implements ObjectsOps {
	private final OAObjectInternalService srvcObjectInternal;
	
	private OAObjectCacheOps opsCache;
	
	public ObjectsOpsImpl(OAObjectInternalService srvcObjectInternal) {
		this.srvcObjectInternal = srvcObjectInternal;
	}

	@Override
	public OAObjectCacheOps cache() {
		if (opsCache != null) return opsCache;
		
		opsCache = new OAObjectCacheOps() {
			//qqqqqqqq add here, using srvc
		};
		return opsCache;
	}

}
