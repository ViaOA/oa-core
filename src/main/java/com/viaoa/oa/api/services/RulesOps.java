package com.viaoa.oa.api.services;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

public interface RulesOps {
	<T extends OAObject> boolean isEnabled(Hub<T> hub, T obj, String name);
	<T extends OAObject> boolean isVisible(Hub<T> hub, T obj, String name);
	
	boolean allowNew(Hub<?> hub);
	boolean allowNew(Class<? extends OAObject> type);

	<T extends OAObject> boolean allowAdd(Hub<T> hub, T obj);
	<T extends OAObject> boolean allowRemove(Hub<T> hub, T obj);
	<T extends OAObject> boolean allowRemoveAll(Hub<T> hub);
	<T extends OAObject> boolean allowDelete(Hub<T> hub, T obj);
	<T extends OAObject> boolean allowSave(T obj);
	<T extends OAObject> boolean allowSubmit(T obj);
	<T extends OAObject> boolean allowCopy(T obj);

	<T extends OAObject> boolean verifyPropertyChange(T obj, String propertyName, Object oldValue, Object newValue);
	<T extends OAObject> boolean verifyAdd(Hub<T> hub, T obj);
	<T extends OAObject> boolean verifyRemove(Hub<T> hub, T obj);
	<T extends OAObject> boolean verifyRemoveAll(Hub<T> hub);
	<T extends OAObject> boolean verifyDelete(Hub<T> hub, T obj);
	<T extends OAObject> boolean verifySave(T obj);
	<T extends OAObject> boolean verifyCommand(T obj, String methodName);
}
