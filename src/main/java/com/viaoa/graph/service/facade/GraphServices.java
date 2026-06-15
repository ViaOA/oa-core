package com.viaoa.graph.service.facade;

import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.api.services.GraphServicesOps;
import com.viaoa.graph.api.services.HubsOps;
import com.viaoa.graph.api.services.ObjectsOps;
import com.viaoa.graph.api.services.TriggersOps;
import com.viaoa.graph.service.HubInternalService;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.graph.service.OATriggerService;

public class GraphServices implements GraphServicesOps {
	
	private final OAGraphInternal og;
	private ObjectsOpsImpl objects;
	private HubsOpsImpl hubs;
	private TriggersOpsImpl triggers;
	
	
	public GraphServices(OAGraphInternal og) {
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
	
	@Override
	public TriggersOps triggers() {
		if (triggers != null) return triggers;
		triggers = new TriggersOpsImpl((OATriggerService) og.triggerInternal());
		return triggers;
	}
}

