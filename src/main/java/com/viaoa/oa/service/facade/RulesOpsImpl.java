package com.viaoa.oa.service.facade;

import com.viaoa.hub.Hub;
import com.viaoa.oa.api.services.RulesOps;
import com.viaoa.oa.service.object.OAObjectRulesService;
import com.viaoa.object.OAObject;

/**
 * Public rules facade backed by {@link OAObjectRulesService}.
 * <p>
 * The facade exposes boolean rule checks for application-facing code while the
 * OA rules engine continues to own {@code OAObjectCallback} creation,
 * processing, responses, and detailed rule state.
 * </p>
 */
public class RulesOpsImpl implements RulesOps {

	private final OAObjectRulesService srvc;

	/**
	 * Creates a rules facade backed by the OA rules engine.
	 *
	 * @param srvc object rules service used to evaluate rule checks
	 */
	public RulesOpsImpl(OAObjectRulesService srvc) {
		this.srvc = srvc;
	}
	
	/**
	 * Returns whether an object member is enabled for the supplied context.
	 *
	 * @param hub Hub context, when available
	 * @param obj object being evaluated
	 * @param name property, link, or member name
	 * @return {@code true} when enabled
	 */
	@Override
	public <T extends OAObject> boolean isEnabled(Hub<T> hub, T obj, String name) {
		return srvc.getAllowEnabled(hub, obj, name);
	}

	/**
	 * Returns whether an object member is visible for the supplied context.
	 *
	 * @param hub Hub context, when available
	 * @param obj object being evaluated
	 * @param name property, link, or member name
	 * @return {@code true} when visible
	 */
	@Override
	public <T extends OAObject> boolean isVisible(Hub<T> hub, T obj, String name) {
		return srvc.getAllowVisible(hub, obj, name);
	}

	/**
	 * Returns whether a new object can be created for the supplied context.
	 *
	 * @param hub Hub context used to infer the object type
	 * @return {@code true} when a new object is allowed
	 */
	@Override
	public boolean allowNew(Hub<?> hub) {
		return srvc.getAllowNewObject(hub);
	}

	/**
	 * Returns whether a new object can be created for the supplied type.
	 *
	 * @param type OAObject type being created
	 * @return {@code true} when a new object is allowed
	 */
	@Override
	public boolean allowNew(Class<? extends OAObject> type) {
		return srvc.getAllowNewObject(type);
	}

	/**
	 * Returns whether an object can be added to a Hub.
	 *
	 * @param hub Hub receiving the object
	 * @param obj object being added
	 * @return {@code true} when add is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowAdd(Hub<T> hub, T obj) {
		return srvc.getAllowAdd(hub, obj);
	}

	/**
	 * Returns whether an object can be removed from a Hub.
	 *
	 * @param hub Hub losing the object
	 * @param obj object being removed
	 * @return {@code true} when remove is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowRemove(Hub<T> hub, T obj) {
		return srvc.getAllowRemove(hub, obj);
	}

	/**
	 * Returns whether all objects can be removed from a Hub.
	 *
	 * @param hub Hub being cleared
	 * @return {@code true} when remove-all is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowRemoveAll(Hub<T> hub) {
		return srvc.getAllowRemoveAll(hub);
	}

	/**
	 * Returns whether an object can be deleted.
	 *
	 * @param hub Hub context, when available
	 * @param obj object being deleted
	 * @return {@code true} when delete is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowDelete(Hub<T> hub, T obj) {
		return srvc.getAllowDelete(hub, obj);
	}

	/**
	 * Returns whether an object can be saved.
	 *
	 * @param obj object being saved
	 * @return {@code true} when save is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowSave(T obj) {
		return srvc.getAllowSave(obj);
	}

	/**
	 * Returns whether an object can be submitted.
	 *
	 * @param obj object being submitted
	 * @return {@code true} when submit is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowSubmit(T obj) {
		return srvc.getAllowSubmit(obj);
	}

	/**
	 * Returns whether an object can be copied.
	 *
	 * @param obj object being copied
	 * @return {@code true} when copy is allowed
	 */
	@Override
	public <T extends OAObject> boolean allowCopy(T obj) {
		return srvc.getAllowCopy(obj);
	}

	/**
	 * Verifies whether a property change is valid.
	 *
	 * @param obj object whose property is changing
	 * @param propertyName property being changed
	 * @param oldValue previous value
	 * @param newValue proposed value
	 * @return {@code true} when the change is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyPropertyChange(T obj, String propertyName, Object oldValue, Object newValue) {
		return srvc.getVerifyPropertyChange(obj, propertyName, oldValue, newValue);
	}

	/**
	 * Verifies whether adding an object to a Hub is valid.
	 *
	 * @param hub Hub receiving the object
	 * @param obj object being added
	 * @return {@code true} when the add is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyAdd(Hub<T> hub, T obj) {
		return srvc.getVerifyAdd(hub, obj);
	}

	/**
	 * Verifies whether removing an object from a Hub is valid.
	 *
	 * @param hub Hub losing the object
	 * @param obj object being removed
	 * @return {@code true} when the remove is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyRemove(Hub<T> hub, T obj) {
		return srvc.getVerifyRemove(hub, obj);
	}

	/**
	 * Verifies whether removing all objects from a Hub is valid.
	 *
	 * @param hub Hub being cleared
	 * @return {@code true} when remove-all is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyRemoveAll(Hub<T> hub) {
		return srvc.getVerifyRemoveAll(hub);
	}

	/**
	 * Verifies whether deleting an object is valid.
	 *
	 * @param hub Hub context, when available
	 * @param obj object being deleted
	 * @return {@code true} when delete is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyDelete(Hub<T> hub, T obj) {
		return srvc.getVerifyDelete(hub, obj);
	}

	/**
	 * Verifies whether saving an object is valid.
	 *
	 * @param obj object being saved
	 * @return {@code true} when save is valid
	 */
	@Override
	public <T extends OAObject> boolean verifySave(T obj) {
		return srvc.getVerifySave(obj);
	}

	/**
	 * Verifies whether a model command can execute.
	 *
	 * @param obj object that owns the command
	 * @param methodName command method name
	 * @return {@code true} when command execution is valid
	 */
	@Override
	public <T extends OAObject> boolean verifyCommand(T obj, String methodName) {
		return srvc.getVerifyCommand(obj, methodName);
	}

}
