package com.viaoa.graph.api.internal.hubs;

import java.util.ArrayList;

import com.viaoa.cascade.OACascade;
import com.viaoa.graph.service.hub.HubStatusService.HubCurrentStateEnum;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubStatusOps {
	public boolean isValid(Hub<?> hub);
	public boolean getChanged(Hub<?> thisHub, int iCascadeRule, OACascade cascade); 
	public <T extends OAObject> HubCurrentStateEnum getCurrentState(Hub<T> thisHub, Hub<T> hubNew, ArrayList<T> alNew);
	public void setChanged(Hub<?> hub, boolean bIsChanged);
}
