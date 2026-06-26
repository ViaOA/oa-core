package com.viaoa.oa.api.internal.objects;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.object.OAObject;

public interface OAObjectCallbackOps {

	
	public boolean getVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue);
	public OAObjectCallback getVerifyPropertyChangeObjectCallback(int checkType, OAObject oaObj, String propertyName, Object oldValue, Object newValue);
	public <T extends OAObject> boolean getAllowEnabled(int checkType, Hub<T> hub, T obj, String name);
	public <T extends OAObject> OAObjectCallback getAllowEnabledObjectCallback(int checkType, Hub<T> hub, T oaObj, String name);
	public <T extends OAObject> boolean getAllowVisible(Hub<T> hub, T oaObj, String name);
	public <T extends OAObject> OAObjectCallback getAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name);
	public OAObjectCallback getVerifyCommandObjectCallback(OAObject oaObj, String methodName, int checkType);
	public OAObjectCallback getAllowSubmitObjectCallback(OAObject oaObj);
	public OAObjectCallback getVerifySaveObjectCallback(OAObject oaObj, int checkType);
	public boolean getAllowSave(OAObject oaObj, int checkType);
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(Hub<T> hub, T objDelete, int checkType);
	public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T oaObj);
	public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(Hub<T> hub, T objAdd, int checkType);
    public <T extends OAObject> void addObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled);
	public OAObjectCallback getAllowNewObjectCallback(Hub<? extends OAObject> hub);
	public <T extends OAObject> OAObjectCallback getAllowDeleteObjectCallback(Hub<T> hub, T obj);
	public OAObjectCallback getAllowCopyObjectCallback(OAObject obj);
	public OAObjectCallback getAllowEnabledObjectCallback(Hub<? extends OAObject> hub);
	public OAObjectCallback getAllowSaveObjectCallback(OAObject obj, int checkType);
	public OAObjectCallback getAllowDeleteObjectCallback(OAObject ao);
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(Hub<T> hub, T objRemove, int checkType);
	public OAObjectCallback getAllowRemoveAllObjectCallback(Hub<? extends OAObject> hub, int checkType);
	public <T extends OAObject> T getCopy(T obj);
	public OAObjectCallback getConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle);
	public OAObjectCallback getConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	public OAObjectCallback getConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	public OAObjectCallback getConfirmRemoveAllObjectCallback(Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle);
	public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	public String getFormat(OAObject obj, String propertyName, String defaultFormat);
	public String getToolTip(OAObject obj, String propertyName, String defaultToolTip);
	public OAObjectCallback getConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle);
	public OAObjectCallback getAllowVisibleObjectCallback(Hub<? extends OAObject> hub);
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj, int checkType);
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj, int checkType);
	public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj, int checkType);
	public void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model);
	public <T extends OAObject> boolean getAllowDelete(T obj);
}


