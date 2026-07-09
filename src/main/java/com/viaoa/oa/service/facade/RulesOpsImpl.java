package com.viaoa.oa.service.facade;

import com.viaoa.hub.Hub;
import com.viaoa.oa.api.services.RulesOps;
import com.viaoa.oa.service.object.OAObjectRulesService;
import com.viaoa.object.OAObject;

public class RulesOpsImpl implements RulesOps {

	private final OAObjectRulesService srvc;

	public RulesOpsImpl(OAObjectRulesService srvc) {
		this.srvc = srvc;
	}
	
	@Override
	public <T extends OAObject> boolean isEnabled(Hub<T> hub, T obj, String name) {
		return srvc.getAllowEnabled(hub, obj, name);
	}

	@Override
	public <T extends OAObject> boolean isVisible(Hub<T> hub, T obj, String name) {
		return srvc.getAllowVisible(hub, obj, name);
	}

	@Override
	public boolean allowNew(Hub<?> hub) {
		return srvc.getAllowNewObject(hub);
	}

	@Override
	public boolean allowNew(Class<? extends OAObject> type) {
		return srvc.getAllowNewObject(type);
	}

	@Override
	public <T extends OAObject> boolean allowAdd(Hub<T> hub, T obj) {
		return srvc.getAllowAdd(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean allowRemove(Hub<T> hub, T obj) {
		return srvc.getAllowRemove(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean allowRemoveAll(Hub<T> hub) {
		return srvc.getAllowRemoveAll(hub);
	}

	@Override
	public <T extends OAObject> boolean allowDelete(Hub<T> hub, T obj) {
		return srvc.getAllowDelete(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean allowSave(T obj) {
		return srvc.getAllowSave(obj);
	}

	@Override
	public <T extends OAObject> boolean allowSubmit(T obj) {
		return srvc.getAllowSubmit(obj);
	}

	@Override
	public <T extends OAObject> boolean allowCopy(T obj) {
		return srvc.getAllowCopy(obj);
	}

	@Override
	public <T extends OAObject> boolean verifyPropertyChange(T obj, String propertyName, Object oldValue, Object newValue) {
		return srvc.getVerifyPropertyChange(obj, propertyName, oldValue, newValue);
	}

	@Override
	public <T extends OAObject> boolean verifyAdd(Hub<T> hub, T obj) {
		return srvc.getVerifyAdd(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean verifyRemove(Hub<T> hub, T obj) {
		return srvc.getVerifyRemove(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean verifyRemoveAll(Hub<T> hub) {
		return srvc.getVerifyRemoveAll(hub);
	}

	@Override
	public <T extends OAObject> boolean verifyDelete(Hub<T> hub, T obj) {
		return srvc.getVerifyDelete(hub, obj);
	}

	@Override
	public <T extends OAObject> boolean verifySave(T obj) {
		return srvc.getVerifySave(obj);
	}

	@Override
	public <T extends OAObject> boolean verifyCommand(T obj, String methodName) {
		return srvc.getVerifyCommand(obj, methodName);
	}

}
