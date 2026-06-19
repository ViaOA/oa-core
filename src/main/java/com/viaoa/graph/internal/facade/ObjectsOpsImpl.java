package com.viaoa.graph.internal.facade;

import com.viaoa.callback.OACallbackLabel;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.graph.api.internal.ObjectsOps;
import com.viaoa.graph.api.internal.objects.OAObjectCacheOps;
import com.viaoa.graph.api.internal.objects.OAObjectCallbackOps;
import com.viaoa.graph.api.internal.objects.OAObjectDeleteOps;
import com.viaoa.graph.api.internal.objects.OAObjectReflectOps;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.object.OAObject;

public class ObjectsOpsImpl implements ObjectsOps {
	private final OAObjectInternalService srvc;
	
	private OAObjectCacheOps opsCache;
	private OAObjectReflectOps opsReflect;
	private OAObjectCallbackOps opsCallback;
	private OAObjectDeleteOps opsDelete;
	
	public ObjectsOpsImpl(OAObjectInternalService srvcObjectInternal) {
		this.srvc = srvcObjectInternal;
	}

	@Override
	public OAObjectCacheOps cache() {
		if (opsCache != null) return opsCache;
		
		opsCache = new OAObjectCacheOps() {
			//qqqqqqqq add here, using srvc
		};
		return opsCache;
	}

	@Override
	public OAObjectReflectOps reflect() {
		if (opsReflect != null) return opsReflect;
		
		opsReflect = new OAObjectReflectOps() {
			@Override
			public String getPropertyPathFromMaster(OAObject objParent, Hub<?> hubChild) {
				return srvc.getOAObjectReflectService().getPropertyPathFromMaster(objParent, hubChild);
			}

			@Override
			public Object getProperty(OAObject oaObj, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(oaObj, propPath);
			}

			@Override
			public Object getProperty(Hub<?> hub, String propPath) {
				return srvc.getOAObjectReflectService().getProperty(hub, propPath);
			}
		};
		return opsReflect;
	}

	@Override
	public OAObjectCallbackOps callbacks() {
		if (opsCallback != null) return opsCallback;
		opsCallback = new OAObjectCallbackOps() {
			@Override
			public <T extends OAObject> void addObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled) {
				srvc.getOAObjectCallbackService().addObjectCallbackChangeListeners(hub, cz, prop, ppPrefix, changeListener, bEnabled);
			}

			@Override
			public void updateLabel(OAObject obj, String propertyName, OACallbackLabel label) {
				srvc.getOAObjectCallbackService().updateLabel(obj, propertyName, label);
			}

			@Override
			public void renderLabel(OAObject obj, String propertyName, OACallbackLabel label) {
				srvc.getOAObjectCallbackService().renderLabel(obj, propertyName, label);
			}

			@Override
			public OAObjectCallback getConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmPropertyChangeObjectCallback(oaObj, property, newValue, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmDeleteObjectCallback(oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmRemoveObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(Hub<T> hub, T oaObj, int checkType) {
				return srvc.getOAObjectCallbackService().getVerifyAddObjectCallback(hub, oaObj, checkType);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmAddObjectCallback(hub, oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getAllowNewObjectCallback(Hub<? extends OAObject> hub) {
				return srvc.getOAObjectCallbackService().getAllowNewObjectCallback(hub);
			}

			@Override
			public OAObjectCallback getVerifySaveObjectCallback(OAObject oaObj, int checkType) {
				return srvc.getOAObjectCallbackService().getVerifySaveObjectCallback(oaObj, checkType);
			}

			@Override
			public OAObjectCallback getConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmSaveObjectCallback(oaObj, confirmMessage, confirmTitle);
			}

			@Override
			public OAObjectCallback getVerifyCommandObjectCallback(OAObject oaObj, String methodName, int checkType) {
				return srvc.getOAObjectCallbackService().getVerifyCommandObjectCallback(oaObj, methodName, checkType);
			}

			@Override
			public OAObjectCallback getConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle) {
				return srvc.getOAObjectCallbackService().getConfirmCommandObjectCallback(oaObj, methodName, confirmMessage, confirmTitle);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(Hub<T> hub, T objDelete, int checkType) {
				return srvc.getOAObjectCallbackService().getVerifyDeleteObjectCallback(hub, objDelete, checkType);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(Hub<T> hub, T objRemove, int checkType) {
				return srvc.getOAObjectCallbackService().getVerifyRemoveObjectCallback(hub, objRemove, checkType);
			}

			@Override
			public OAObjectCallback getVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue) {
				return srvc.getOAObjectCallbackService().getVerifyPropertyChangeObjectCallback(checkType, oaObj, propertyName, oldValue, newValue);
			}

			@Override
			public OAObjectCallback getCopyObjectCallback(OAObject oaObj) {
				return srvc.getOAObjectCallbackService().getCopyObjectCallback(oaObj);
			}

			@Override
			public <T extends OAObject> T getCopy(T oaObj) {
				return srvc.getOAObjectCallbackService().getCopy(oaObj);
			}

			@Override
			public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj, int checkType) {
				return srvc.getOAObjectCallbackService().getAllowRemove(hub, obj, checkType);
			}

			@Override
			public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T obj) {
				return srvc.getOAObjectCallbackService().getAllowDelete(hub, obj);
			}

			@Override
			public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj, int checkType) {
				return srvc.getOAObjectCallbackService().getAllowAdd(hub, obj, checkType);
			}

			@Override
			public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(Hub<T> hub, T objRemove, int checkType) {
				return srvc.getOAObjectCallbackService().getAllowRemoveObjectCallback(hub, objRemove, checkType);
			}

			@Override
			public OAObjectCallback getAllowCopyObjectCallback(OAObject oaObj) {
				return srvc.getOAObjectCallbackService().getAllowCopyObjectCallback(oaObj);
			}

			@Override
			public String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
				return srvc.getOAObjectCallbackService().getToolTip(obj, propertyName, defaultToolTip);
			}
		};
		return opsCallback;
	}

	@Override
	public OAObjectDeleteOps delete() {
		if (opsDelete != null) return opsDelete;
		opsDelete = new OAObjectDeleteOps() {
			@Override
			public OALinkInfo[] getMustBeEmptyBeforeDelete(OAObject oaObj) {
				return srvc.getOAObjectDeleteService().getMustBeEmptyBeforeDelete(oaObj);
			}
		}; 
		return opsDelete;
	}
}
