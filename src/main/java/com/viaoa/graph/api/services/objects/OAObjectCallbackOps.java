package com.viaoa.graph.api.services.objects;

import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.object.OAObject;

public interface OAObjectCallbackOps {

	// methods will be added as needed by Apps
	
	public <T extends OAObject> void addObjectCallbackChangeListeners(
		final Hub<T> hub, final Class<T> cz, final String prop, String ppPrefix,
		final HubChangeListener changeListener, final boolean bEnabled
	);
}
