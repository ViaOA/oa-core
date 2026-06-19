package com.viaoa.graph.internal.facade;

import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.api.internal.GraphInternalOps;
import com.viaoa.graph.api.internal.HubsOps;
import com.viaoa.graph.api.internal.ObjectsOps;
import com.viaoa.graph.service.HubInternalService;
import com.viaoa.graph.service.OAObjectInternalService;


public class GraphInternal implements GraphInternalOps {
	
	private final OAGraphInternal og;
	private ObjectsOpsImpl objects;
	private HubsOpsImpl hubs;
	
	
	public GraphInternal(OAGraphInternal og) {
		this.og = og;
	}

	@Override
	public ObjectsOps objects() {
		if (objects != null) return objects;

		objects = new ObjectsOpsImpl((OAObjectInternalService) og.objectsInternal());
		return objects;
	}

	@Override
	public HubsOps hubs() {
		if (hubs != null) return hubs;
		hubs = new HubsOpsImpl( (HubInternalService) og.hubsInternal());
		return hubs;
	}
	
}

