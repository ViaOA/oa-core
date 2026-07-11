package com.viaoa.oa.api.internal.objects;

import com.viaoa.callback.OACallbackLabel;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.hub.Hub;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.object.OAObject;

/**
 * Internal object-rule API backed by OAObjectRulesService. Methods either return boolean rule answers or the OAObjectCallback request/response used by the OA rules engine.
 */
public interface OAObjectRulesOps {
	/**
	 * Returns whether an object, property, or Hub context is visible according to OA object rules.
	 *
	 * @param hub the Hub context
	 * @param oaObj the target object
	 * @param name optional property or member name
	 * @return {@code true} if visible
	 */
	public <T extends OAObject> boolean getAllowVisible(Hub<T> hub, T oaObj, String name);
	/**
	 * Creates and processes an AllowVisible rule callback for an object or property.
	 *
	 * @param hub the Hub context
	 * @param oaObj the target object
	 * @param name optional property or member name
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name);
	/**
	 * Creates and processes an AllowVisible rule callback for a Hub context.
	 *
	 * @param hub the Hub context
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowVisibleObjectCallback(Hub<? extends OAObject> hub);
	/**
	 * Runs only the object callback portion of VerifyPropertyChange.
	 *
	 * @param obj the target object
	 * @param propertyName the changed property
	 * @param oldValue the previous value
	 * @param newValue the new value
	 * @return {@code true} if the callback allows the change
	 */
	public boolean getVerifyPropertyChangeCallbackOnly(OAObject obj, String propertyName, Object oldValue, Object newValue);

	public OAObjectCallback getVerifyPropertyChangeObjectCallback(OAObject obj, String propertyName, Object oldValue, Object newValue);
	
	/**
	 * Creates and processes a callback-only VerifyPropertyChange rule callback.
	 *
	 * @param oaObj the target object
	 * @param propertyName the changed property
	 * @param oldValue the previous value
	 * @param newValue the new value
	 * @return the processed callback
	 */
	public OAObjectCallback getVerifyPropertyChangeCallbackOnlyObjectCallback(OAObject oaObj, String propertyName, Object oldValue, Object newValue);
	/**
	 * Returns whether an object, property, or Hub context is enabled according to OA object rules.
	 *
	 * @param hub the Hub context
	 * @param obj the target object
	 * @param name optional property or member name
	 * @return {@code true} if enabled
	 */
	public <T extends OAObject> boolean getAllowEnabled(Hub<T> hub, T obj, String name);
	/**
	 * Runs only callback/listener enabled checks for an object or property.
	 *
	 * @param hub the Hub context
	 * @param obj the target object
	 * @param name optional property or member name
	 * @return {@code true} if enabled by callback processing
	 */
	public <T extends OAObject> boolean getAllowEnabledCallbackOnly(Hub<T> hub, T obj, String name);
	/**
	 * Creates and processes an AllowEnabled rule callback for an object or property.
	 *
	 * @param hub the Hub context
	 * @param oaObj the target object
	 * @param name optional property or member name
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getAllowEnabledObjectCallback(Hub<T> hub, T oaObj, String name);
	/**
	 * Creates and processes a VerifyCommand rule callback.
	 *
	 * @param oaObj the target object
	 * @param methodName the command method name
	 * @return the processed callback
	 */
	public OAObjectCallback getVerifyCommandObjectCallback(OAObject oaObj, String methodName);
	/**
	 * Creates and processes an AllowSubmit rule callback.
	 *
	 * @param oaObj the target object
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowSubmitObjectCallback(OAObject oaObj);
	/**
	 * Creates and processes a VerifySave rule callback.
	 *
	 * @param oaObj the target object
	 * @return the processed callback
	 */
	public OAObjectCallback getVerifySaveObjectCallback(OAObject oaObj);
	/**
	 * Returns whether an object is allowed to save.
	 *
	 * @param oaObj the target object
	 * @return {@code true} if save is allowed
	 */
	public boolean getAllowSave(OAObject oaObj);
	/**
	 * Creates and processes a VerifyDelete rule callback for a Hub delete context.
	 *
	 * @param hub the Hub context
	 * @param objDelete the object being deleted
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(Hub<T> hub, T objDelete);
	/**
	 * Returns whether an object in a Hub context is allowed to be deleted.
	 *
	 * @param hub the Hub context
	 * @param oaObj the object being deleted
	 * @return {@code true} if delete is allowed
	 */
	public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T oaObj);
	/**
	 * Creates and processes an AllowAdd rule callback.
	 *
	 * @param hub the Hub receiving the object
	 * @param objAdd the object being added
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(Hub<T> hub, T objAdd);


	public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(Hub<T> hub, T objAdd);
	
	/**
	 * Adds listeners for metadata dependencies that can change object-rule callback results.
	 *
	 * @param hub the Hub context
	 * @param cz the object class
	 * @param prop the property name
	 * @param ppPrefix optional property-path prefix
	 * @param changeListener the listener to notify
	 * @param bEnabled {@code true} for enabled dependencies, {@code false} for visible dependencies
	 */
	public <T extends OAObject> void addObjectCallbackChangeListeners(Hub<T> hub, Class<T> cz, String prop, String ppPrefix, HubChangeListener changeListener, boolean bEnabled);
	/**
	 * Creates and processes an AllowNew rule callback for a Hub context.
	 *
	 * @param hub the Hub context
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowNewObjectCallback(Hub<? extends OAObject> hub);
	/**
	 * Creates and processes an AllowDelete rule callback for a Hub delete context.
	 *
	 * @param hub the Hub context
	 * @param obj the object being deleted
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getAllowDeleteObjectCallback(Hub<T> hub, T obj);
	/**
	 * Creates and processes an AllowCopy rule callback.
	 *
	 * @param obj the object to copy
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowCopyObjectCallback(OAObject obj);
	/**
	 * Creates and processes an AllowEnabled rule callback for a Hub context.
	 *
	 * @param hub the Hub context
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowEnabledObjectCallback(Hub<? extends OAObject> hub);
	/**
	 * Creates and processes an AllowSave rule callback.
	 *
	 * @param obj the object to save
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowSaveObjectCallback(OAObject obj);
	/**
	 * Creates and processes an AllowDelete rule callback for an object.
	 *
	 * @param ao the object being deleted
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowDeleteObjectCallback(OAObject ao);
	/**
	 * Creates and processes an AllowRemove rule callback.
	 *
	 * @param hub the Hub losing the object
	 * @param objRemove the object being removed
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(Hub<T> hub, T objRemove);
	/**
	 * Creates and processes an AllowRemoveAll rule callback.
	 *
	 * @param hub the Hub being cleared
	 * @return the processed callback
	 */
	public OAObjectCallback getAllowRemoveAllObjectCallback(Hub<? extends OAObject> hub);
	/**
	 * Returns a copy of an object after OA copy rules are applied.
	 *
	 * @param obj the source object
	 * @return the copied object
	 */
	public <T extends OAObject> T getCopy(T obj);
	/**
	 * Creates a confirmation callback for a property change.
	 *
	 * @param oaObj the target object
	 * @param property the property name
	 * @param newValue the proposed value
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public OAObjectCallback getConfirmPropertyChangeObjectCallback(OAObject oaObj, String property, Object newValue, String confirmMessage, String confirmTitle);
	/**
	 * Creates a confirmation callback for saving an object.
	 *
	 * @param oaObj the target object
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public OAObjectCallback getConfirmSaveObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	/**
	 * Creates a confirmation callback for deleting an object.
	 *
	 * @param oaObj the target object
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public OAObjectCallback getConfirmDeleteObjectCallback(OAObject oaObj, String confirmMessage, String confirmTitle);
	/**
	 * Creates a confirmation callback for removing an object from a Hub.
	 *
	 * @param hub the Hub context
	 * @param oaObj the object being removed
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	/**
	 * Creates a confirmation callback for removing all objects from a Hub.
	 *
	 * @param hub the Hub context
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public OAObjectCallback getConfirmRemoveAllObjectCallback(Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle);
	/**
	 * Creates a confirmation callback for adding an object to a Hub.
	 *
	 * @param hub the Hub context
	 * @param oaObj the object being added
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(Hub<T> hub, T oaObj, String confirmMessage, String confirmTitle);
	/**
	 * Returns the display format for an object property after OA rules are applied.
	 *
	 * @param obj the target object
	 * @param propertyName the property name
	 * @param defaultFormat the fallback format
	 * @return the resolved format
	 */
	public String getFormat(OAObject obj, String propertyName, String defaultFormat);
	/**
	 * Returns the tooltip for an object property after OA rules are applied.
	 *
	 * @param obj the target object
	 * @param propertyName the property name
	 * @param defaultToolTip the fallback tooltip
	 * @return the resolved tooltip
	 */
	public String getToolTip(OAObject obj, String propertyName, String defaultToolTip);
	/**
	 * Creates a confirmation callback for a command method.
	 *
	 * @param oaObj the target object
	 * @param methodName the command method name
	 * @param confirmMessage the default confirmation message
	 * @param confirmTitle the default confirmation title
	 * @return the processed callback
	 */
	public OAObjectCallback getConfirmCommandObjectCallback(OAObject oaObj, String methodName, String confirmMessage, String confirmTitle);
	/**
	 * Returns whether an object can be added to a Hub.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object being added
	 * @return {@code true} if add is allowed
	 */
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj);
	/**
	 * Returns whether an object can be added to a Hub while ignoring processed-state checks.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object being added
	 * @return {@code true} if add is allowed
	 */
	public <T extends OAObject> boolean getAllowAddIgnoreProcessed(Hub<T> hub, T obj);
	/**
	 * Returns whether an object can be removed from a Hub.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove is allowed
	 */
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj);
	/**
	 * Runs only callback/listener checks for removing an object from a Hub.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove is allowed by callback processing
	 */
	public <T extends OAObject> boolean getAllowRemoveCallbackOnly(Hub<T> hub, T obj);
	/**
	 * Returns whether an object can be removed while ignoring processed-state checks.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove is allowed
	 */
	public <T extends OAObject> boolean getAllowRemoveIgnoreProcessed(Hub<T> hub, T obj);
	/**
	 * Returns whether removing an object from a Hub verifies successfully.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove verifies
	 */
	public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj);

	public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(Hub<T> hub, T obj);
/**
	 * 
	 * Runs only callback/listener checks for VerifyRemove.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove verifies by callback processing
	 */
	public <T extends OAObject> boolean getVerifyRemoveCallbackOnly(Hub<T> hub, T obj);
	/**
	 * Returns whether removing an object verifies while ignoring processed-state checks.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove verifies
	 */
	public <T extends OAObject> boolean getVerifyRemoveIgnoreProcessed(Hub<T> hub, T obj);
	/**
	 * Applies object-rule callback metadata to an OAObjectModel.
	 *
	 * @param clazz the object class
	 * @param property the property name
	 * @param model the model metadata to update
	 */
	public void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model);
	/**
	 * Returns whether an object is allowed to be deleted.
	 *
	 * @param obj the object being deleted
	 * @return {@code true} if delete is allowed
	 */
	public <T extends OAObject> boolean getAllowDelete(T obj);

	void updateLabel(OAObject obj, String propertyName, OACallbackLabel lbl);

	void renderLabel(OAObject obj, String propertyName, OACallbackLabel lbl);

}

