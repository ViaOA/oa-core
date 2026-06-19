package com.viaoa.graph.api.internal.hubs;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface HubAddRemoveOps {

	public <T extends OAObject> boolean add(Hub<T> hub, T obj);
	public void swap(Hub<?> hub, int pos1, int pos2);
	public void move(Hub<?> hub, int posFrom, int posTo);
	public <T extends OAObject> boolean insert(Hub<T> hub, T obj, int pos);
	public void clear(Hub<?> hub);
	public <T extends OAObject> boolean canAdd(Hub<T> hub, T object);
	public <T extends OAObject> String canAddMsg(Hub<T> hub, T obj);
	public String getCantRemoveAllMessage(Hub<?> hub, int checkType);
	public <T extends OAObject> void add(Hub<T> hub, T obj, boolean bAlreadyCalledContains);
	public void clear(Hub<?> thisHub, boolean bSetAOtoNull, boolean bSendNewList);
	public <T extends OAObject> boolean remove(Hub<T> hub, T obj);
	public <T extends OAObject> T remove(Hub<T> hub, int pos);
	public <T extends OAObject> boolean remove(Hub<T> hub, Object obj);
	public <T extends OAObject> void remove(Hub<T> thisHub, T obj, boolean bForce, boolean bSendEvent, boolean bDeleting, boolean bSetAO, boolean bSetPropToMaster, boolean bIsRemovingAll);
	public <T extends OAObject> void sortMove(Hub<T> hub, T object);
	public <T extends OAObject> void refresh(Hub<T> hub, Hub<T> hubNew);
	public boolean isAllowAddRemove(Hub<?> thisHub);	
	public boolean isAllowRemove(Hub<?> thisHub);	
	
}
