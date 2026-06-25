package com.viaoa.oa.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubDataOps {
	
	public <T extends OAObject> void setObjectClass(Hub<T> hubDetail, Class<T> clazz);
	public void ensureCapacity(Hub<?> hub, int size);
	public void resizeToFit(Hub<?> hub);
	public <T extends OAObject> void copyInto(Hub<T> hub, T[] anArray);
	public <T extends OAObject> T[] toArray(Hub<T> hub);
	public int getCurrentSize(Hub<?> hub);
	public <T extends OAObject> T getObject(Hub<T> hub, Object key);
	public <T extends OAObject> T getObjectAt(Hub<T> hub, int pos);
	public boolean contains(Hub<?> hub, Object obj);
	public int getPos(final Hub<?> hub, Object object, final boolean adjustMaster, final boolean bUpdateLink);
	public boolean setLoadingAllData(Hub<?> hub, boolean bIsLoading);
	public void setLoadingAllData(Hub<?> hub, boolean bIsLoadingAllData, Thread thread);
	public void clearHubChanges(Hub<?> hub);
	public <T extends OAObject> void _clone(Hub<T> thisHub, Hub<T> newHub);

}
