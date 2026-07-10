package com.viaoa.oa.api.services;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

/**
 * Public OA rules service facade.
 * <p>
 * This interface exposes boolean rule answers for common model operations. It
 * is the simplified public service layer over the OA rules engine, which uses
 * {@code OAObjectCallback} as its internal request/response carrier. Callers of
 * this interface do not need to construct callbacks or select rule-processing
 * stages directly.
 */
public interface RulesOps {
	/**
	 * Returns whether an object, property, or Hub context is enabled.
	 *
	 * @param hub the Hub context
	 * @param obj the target object
	 * @param name optional property or member name
	 * @return {@code true} if enabled
	 */
	<T extends OAObject> boolean isEnabled(Hub<T> hub, T obj, String name);

	/**
	 * Returns whether an object, property, or Hub context is visible.
	 *
	 * @param hub the Hub context
	 * @param obj the target object
	 * @param name optional property or member name
	 * @return {@code true} if visible
	 */
	<T extends OAObject> boolean isVisible(Hub<T> hub, T obj, String name);
	
	/**
	 * Returns whether a new object can be created for a Hub context.
	 *
	 * @param hub the Hub context
	 * @return {@code true} if new-object creation is allowed
	 */
	boolean allowNew(Hub<?> hub);

	/**
	 * Returns whether a new object can be created for a class.
	 *
	 * @param type the object class
	 * @return {@code true} if new-object creation is allowed
	 */
	boolean allowNew(Class<? extends OAObject> type);

	/**
	 * Returns whether an object can be added to a Hub.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object being added
	 * @return {@code true} if add is allowed
	 */
	<T extends OAObject> boolean allowAdd(Hub<T> hub, T obj);

	/**
	 * Returns whether an object can be removed from a Hub.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove is allowed
	 */
	<T extends OAObject> boolean allowRemove(Hub<T> hub, T obj);

	/**
	 * Returns whether all objects can be removed from a Hub.
	 *
	 * @param hub the Hub being cleared
	 * @return {@code true} if remove-all is allowed
	 */
	<T extends OAObject> boolean allowRemoveAll(Hub<T> hub);

	/**
	 * Returns whether an object can be deleted in a Hub context.
	 *
	 * @param hub the Hub context
	 * @param obj the object being deleted
	 * @return {@code true} if delete is allowed
	 */
	<T extends OAObject> boolean allowDelete(Hub<T> hub, T obj);

	/**
	 * Returns whether an object can be saved.
	 *
	 * @param obj the object being saved
	 * @return {@code true} if save is allowed
	 */
	<T extends OAObject> boolean allowSave(T obj);

	/**
	 * Returns whether an object can be submitted.
	 *
	 * @param obj the object being submitted
	 * @return {@code true} if submit is allowed
	 */
	<T extends OAObject> boolean allowSubmit(T obj);

	/**
	 * Returns whether an object can be copied.
	 *
	 * @param obj the object being copied
	 * @return {@code true} if copy is allowed
	 */
	<T extends OAObject> boolean allowCopy(T obj);

	/**
	 * Verifies whether a property value change is valid.
	 *
	 * @param obj the target object
	 * @param propertyName the changed property name
	 * @param oldValue the previous value
	 * @param newValue the proposed value
	 * @return {@code true} if the property change verifies
	 */
	<T extends OAObject> boolean verifyPropertyChange(T obj, String propertyName, Object oldValue, Object newValue);

	/**
	 * Verifies whether adding an object to a Hub is valid.
	 *
	 * @param hub the Hub receiving the object
	 * @param obj the object being added
	 * @return {@code true} if add verifies
	 */
	<T extends OAObject> boolean verifyAdd(Hub<T> hub, T obj);

	/**
	 * Verifies whether removing an object from a Hub is valid.
	 *
	 * @param hub the Hub losing the object
	 * @param obj the object being removed
	 * @return {@code true} if remove verifies
	 */
	<T extends OAObject> boolean verifyRemove(Hub<T> hub, T obj);

	/**
	 * Verifies whether removing all objects from a Hub is valid.
	 *
	 * @param hub the Hub being cleared
	 * @return {@code true} if remove-all verifies
	 */
	<T extends OAObject> boolean verifyRemoveAll(Hub<T> hub);

	/**
	 * Verifies whether deleting an object in a Hub context is valid.
	 *
	 * @param hub the Hub context
	 * @param obj the object being deleted
	 * @return {@code true} if delete verifies
	 */
	<T extends OAObject> boolean verifyDelete(Hub<T> hub, T obj);

	/**
	 * Verifies whether saving an object is valid.
	 *
	 * @param obj the object being saved
	 * @return {@code true} if save verifies
	 */
	<T extends OAObject> boolean verifySave(T obj);

	/**
	 * Verifies whether a command method can execute.
	 *
	 * @param obj the command target object
	 * @param methodName the command method name
	 * @return {@code true} if command execution verifies
	 */
	<T extends OAObject> boolean verifyCommand(T obj, String methodName);
}
