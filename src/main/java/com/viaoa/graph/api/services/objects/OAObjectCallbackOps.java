package com.viaoa.graph.api.services.objects;

import com.viaoa.callback.OACallbackLabel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.object.OAObject;

public interface OAObjectCallbackOps {

	public <T extends OAObject> void addObjectCallbackChangeListeners(
		final Hub<T> hub, final Class<T> cz, final String prop, String ppPrefix,
		final HubChangeListener changeListener, final boolean bEnabled
	);
	
	public void updateLabel(OAObject obj, String propertyName, OACallbackLabel label);	
	
	public void renderLabel(OAObject obj, String propertyName, OACallbackLabel label);	
}
