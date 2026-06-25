package com.viaoa.oa.api.services.hubs;

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.hub.Hub.HubCurrentStateEnum;
import com.viaoa.object.OAObject;

public interface HubStatusOps {
	public <T extends OAObject> HubCurrentStateEnum getCurrentState(final Hub<T> thisHub, final Hub<T> hubNew, final ArrayList<T> alNew);
}
