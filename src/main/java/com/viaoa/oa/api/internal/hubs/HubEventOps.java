package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAObject;

public interface HubEventOps {

	
	public void fireOnNewListEvent(Hub<?> hub, boolean bAll);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, boolean bActiveObjectOnly);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, boolean bActiveObjectOnly);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl, String property, String[] dependentPropertyPaths, boolean bActiveObjectOnly, boolean bUseBackgroundThread);
	public <T extends OAObject> void addHubListener(Hub<T> hub, HubListener<T> hl);
	public <T extends OAObject> void removeHubListener(Hub<T> hub, HubListener<T> hl);
	public <T extends OAObject> void fireCalcPropertyChange(Hub<T> hub, T obj, String propertyName);
	public <T extends OAObject> void fireAfterChangeActiveObjectEvent(Hub<T> thisHub, T obj, int pos, boolean bAllShared);

}
