package com.viaoa.graph.api;

import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;

public interface HubOps {

	public void save(Hub hub, int iCascadeRule, OACascade cascade);

}
