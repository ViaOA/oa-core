package com.viaoa.oa.api.services.objects;

import com.viaoa.callback.OACallbackLabel;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.object.OAObject;

public interface OAObjectCallbackOps {

	
//qqqqqqqqqqqqq To Do:  remove "Callback" from name	
	
	public <T extends OAObject> void addObjectCallbackChangeListeners(
		final Hub<T> hub, final Class<T> cz, final String prop, String ppPrefix,
		final HubChangeListener changeListener, final boolean bEnabled
	);
	
	public OAObjectCallback getConfirmPropertyChangeObjectCallback(final OAObject oaObj, 
		String property, Object newValue,
		String confirmMessage, String confirmTitle
	);
	
	public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage, String confirmTitle);
	
	public OAObjectCallback getConfirmDeleteObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle);
	
	public void updateLabel(OAObject obj, String propertyName, OACallbackLabel label);	
	
	public void renderLabel(OAObject obj, String propertyName, OACallbackLabel label);	
	
	public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(final Hub<T> hub, final T oaObj, final int checkType);
	
	public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage, String confirmTitle);
	
	public OAObjectCallback getAllowNewObjectCallback(final Hub<? extends OAObject> hub);
	
	public OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj, final int checkType);
	
	public OAObjectCallback getConfirmSaveObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle);
	
	public OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName, int checkType);
	
	public OAObjectCallback getConfirmCommandObjectCallback(final OAObject oaObj, String methodName, String confirmMessage, String confirmTitle);
	
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(final Hub<T> hub, final T objDelete, final int checkType);

	public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(final Hub<T> hub, final T objRemove, final int checkType);
	
	public OAObjectCallback getVerifyPropertyChangeObjectCallback(final int checkType, final OAObject oaObj,
		final String propertyName, final Object oldValue, final Object newValue);
	
	public OAObjectCallback getCopyObjectCallback(final OAObject oaObj);
	
	public <T extends OAObject> T getCopy(T oaObj);
	
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj, int checkType);

	public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T obj);
	
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj, int checkType);
	
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(final Hub<T> hub, final T objRemove, final int checkType);
	
	public OAObjectCallback getAllowCopyObjectCallback(final OAObject oaObj);
	
	public String getToolTip(OAObject obj, String propertyName, String defaultToolTip);
}


