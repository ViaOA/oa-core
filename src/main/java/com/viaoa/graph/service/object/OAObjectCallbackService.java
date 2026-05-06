package com.viaoa.graph.service.object;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.swing.JLabel;

import com.viaoa.callback.OAObjectCallback;
import com.viaoa.callback.OAObjectCallback.Type;
import com.viaoa.cascade.OACascade;
import com.viaoa.compare.OAUnknownObject;
import com.viaoa.converter.OAConv;
import com.viaoa.graph.context.OAContext;
import com.viaoa.graph.context.OAUserAccess;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAMethodInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Rule and permission engine for {@link OAObject} interactions.
 * <p>
 * This service evaluates whether a user action on an object or hub is
 * permitted, visible, enabled, confirmable, or requires further UI messaging.
 * It unifies rule sources from annotations, object-level callback methods,
 * Hub listeners, user context, and metadata from {@code OAObjectInfo}.
 *
 * <h3>Primary Responsibilities</h3>
 * <ul>
 *   <li>Resolve enable/visible rules for properties, links, and methods</li>
 *   <li>Check add/remove/delete/save permissions for Hub operations</li>
 *   <li>Provide UI data (tooltips, labels, formatting)</li>
 *   <li>Execute annotation-driven and {@code callback*} methods on objects</li>
 *   <li>Propagate errors, responses, or confirmation titles/messages</li>
 *   <li>Traverse owner/master hierarchy to enforce inherited rules</li>
 * </ul>
 *
 * <h3>Evaluation Sources</h3>
 * <ul>
 *   <li>Metadata from {@code @OAObjCallback} on class/props/links/methods</li>
 *   <li>Domain logic in {@code callback*}(..) methods</li>
 *   <li>Hub listeners associated with UI contexts</li>
 *   <li>Context-driven {@link com.viaoa.context.OAUserAccess} constraints</li>
 *   <li>Process/edit flags on objects and hubs</li>
 * </ul>
 *
 * <h3>Correctness Guarantees</h3>
 * <ul>
 *   <li>No side effects on object identity, lifecycle, or loading</li>
 *   <li>All rule failures optionally provide actionable UI feedback</li>
 *   <li>User/role checks are always respected before UI updates</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * Invoked automatically by:
 * <ul>
 *   <li>{@link OAObject} mutation (before-change routing)</li>
 *   <li>{@link Hub} add/remove operations</li>
 *   <li>UI controllers to dynamically update visual state</li>
 * </ul>
 *
 * Application code should not call this directly except to query allowed actions.
 *
 * @see OAObjectCallback
 * @see com.viaoa.annotation.OAObjCallback
 */
public abstract class OAObjectCallbackService {
	private static final Logger LOG = Logger.getLogger(OAObjectCallbackService.class.getName());

	public OAObjectCallbackService() {
	}

	/**
	 * Returns whether the specified property or link is visible by evaluating
	 * the associated {@link OAObjectCallback}. The visibility decision is
	 * determined by {@code getAllowVisibleObjectCallback(...)}.
	 *
	 * @param hub  the hub providing contextual visibility rules, or {@code null}
	 * @param obj  the target object, or {@code null}
	 * @param name the property or link name, or {@code null}
	 * @return {@code true} if visibility is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowVisible(Hub<T> hub, T obj, String name) {
		return getAllowVisibleObjectCallback(hub, obj, name).getAllowed();
	}

	/**
	 * Returns whether the specified property, link, or command is enabled
	 * by evaluating the associated {@link OAObjectCallback}. The enabled
	 * state is determined by {@code getAllowEnabledObjectCallback(...)}.
	 *
	 * @param checkType the bitmask of checking options
	 * @param hub       the hub providing contextual rules, or {@code null}
	 * @param obj       the target object, or {@code null}
	 * @param name      the property, link, or method name, or {@code null}
	 * @return {@code true} if the action is enabled; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowEnabled(int checkType, Hub<T> hub, T obj, String name) {
		return getAllowEnabledObjectCallback(checkType, hub, obj, name).getAllowed();
	}

	/**
	 * Returns whether the specified object can be copied by evaluating the
	 * associated {@link OAObjectCallback}. If the object is {@code null},
	 * copying is not allowed.
	 *
	 * @param oaObj the object to evaluate
	 * @return {@code true} if copying is allowed; otherwise {@code false}
	 */
	public boolean getAllowCopy(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return getAllowCopyObjectCallback(oaObj).getAllowed();
	}

	/**
	 * Returns a copy of the specified object using the {@link OAObjectCallback}
	 * associated with copy behavior. If the callback does not provide a copy
	 * value and copying is allowed, {@code createCopy()} is invoked.
	 *
	 * @param oaObj the source object to copy
	 * @return the copied object, or {@code null} if copying is not allowed
	 */
	public <T extends OAObject> T getCopy(T oaObj) {
		if (oaObj == null) {
			return null;
		}
		OAObjectCallback eq = getCopyObjectCallback(oaObj);

		Object objx = eq.getValue();
		if (!(objx instanceof OAObject)) {
			if (!eq.getAllowed()) {
				return null;
			}
			objx = (T) oaObj.createCopy();
		}

		getAfterCopyObjectCallback(oaObj, (T) objx);
		return (T) objx;
	}

    
    
	/**
	 * Returns whether the specified property change is permitted by evaluating
	 * the associated {@link OAObjectCallback} for verification.
	 *
	 * @param checkType    the bitmask of checking options
	 * @param obj          the target object
	 * @param propertyName the property being changed
	 * @param oldValue     the previous value
	 * @param newValue     the proposed new value
	 * @return {@code true} if the property change is allowed; otherwise {@code false}
	 */
	public boolean getVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue) {
		return getVerifyPropertyChangeObjectCallback(checkType, obj, propertyName, oldValue, newValue).getAllowed();
	}

	/**
	 * Returns whether the specified object can be added to the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if the add operation is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj, int checkType) {
		return getAllowAddObjectCallback(hub, obj, checkType).getAllowed();
	}

	/**
	 * Returns whether adding the specified object to the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyAdd(Hub<T> hub, T obj, int checkType) {
		return getVerifyAddObjectCallback(hub, obj, checkType).getAllowed();
	}

	/**
	 * Returns whether the specified object can be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param obj       the object being removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if the remove operation is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj, int checkType) {
		return getAllowRemoveObjectCallback(hub, obj, checkType).getAllowed();
	}
    
	/**
	 * Returns whether removing the specified object from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param obj       the object being removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj, int checkType) {
		return getVerifyRemoveObjectCallback(hub, obj, checkType).getAllowed();
	}

	/**
	 * Returns whether all objects may be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose contents may be removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if removing all objects is allowed; otherwise {@code false}
	 */
	public boolean getAllowRemoveAll(Hub<? extends OAObject> hub, int checkType) {
		return getAllowRemoveAllObjectCallback(hub, checkType).getAllowed();
	}
    
	/**
	 * Returns whether removing all objects from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public boolean getVerifyRemoveAll(Hub<? extends OAObject> hub, int checkType) {
		return getVerifyRemoveAllObjectCallback(hub, checkType).getAllowed();
	}

	/**
	 * Returns whether the specified object may be deleted within the context
	 * of the given hub by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub the hub providing contextual rules
	 * @param obj the object to delete
	 * @return {@code true} if deletion is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowDelete(Hub<T> hub, T obj) {
		return getAllowDeleteObjectCallback(hub, obj).getAllowed();
	}
	/**
	 * Returns whether deleting the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param obj       the object to delete
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyDelete(Hub<T> hub, T obj, int checkType) {
		return getVerifyDeleteObjectCallback(hub, obj, checkType).getAllowed();
	}

	/**
	 * Returns whether the specified object may be saved by evaluating
	 * the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if saving is allowed; otherwise {@code false}
	 */
	public boolean getAllowSave(OAObject obj, int checkType) {
		return getAllowSaveObjectCallback(obj, checkType).getAllowed();
	}

	/**
	 * Returns whether saving the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param checkType the bitmask of checking options
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public boolean getVerifySave(OAObject obj, int checkType) {
		return getVerifySaveObjectCallback(obj, checkType).getAllowed();
	}

	/**
	 * Creates an {@link OAObjectCallback} used to determine whether the
	 * specified object satisfies all required submit-time rules. The callback
	 * is initialized with {@code Type.AllowSubmit} and evaluated across the
	 * object's properties and owned links.
	 *
	 * @param obj the object to evaluate
	 * @return the resulting callback, or {@code null} if the object is {@code null}
	 */
	public OAObjectCallback getAllowSubmitObjectCallback(OAObject obj) {
		if (obj == null) {
			return null;
		}

		OAObjectCallback em = new OAObjectCallback(Type.AllowSubmit);
		_getAllowSubmit(em, obj, new OACascade());

		return em;
	}
	
	/**
	 * Performs recursive submit-rule validation for the specified object,
	 * evaluating properties and owned links and updating the supplied
	 * {@link OAObjectCallback} accordingly. Processing stops if the callback
	 * becomes disallowed or a throwable is set.
	 *
	 * @param em      the callback being updated
	 * @param obj     the object to evaluate
	 * @param cascade the cascade tracker used to prevent repeated evaluation
	 */
	private void _getAllowSubmit(final OAObjectCallback em, final OAObject obj, final OACascade cascade) {
		if (em == null || obj == null) {
			return;
		}
		if (!em.isAllowed()) {
			return;
		}
		if (cascade.wasCascaded(obj, true)) {
			return;
		}

		em.setObject(obj);
		callObjectCallbackMethod(em, null, em);

		final OAObjectInfo oi = callInfoGetObjectInfo(obj.getClass());
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (OAString.isNotEmpty(pi.getEnumPropertyName())) continue;
			Object val = obj.getProperty(pi.getName());
			final OAObjectCallback emx = new OAObjectCallback(Type.VerifyPropertyChange, OAObjectCallback.CHECK_ALL, null, null, obj,
					pi.getName(), val);

			if (val instanceof String) {
				if (((String) val).length() > pi.getMaxLength() && pi.getMaxLength() > 0) {
					emx.setAllowed(false);
					String s = pi.getDisplayName() + " max length exceeded, max=" + pi.getMaxLength() + ", value="
							+ OAString.format((String) val, "40L..");
					emx.setResponse(s);
				}
			} else if (val == null && pi.getRequired()) {
				emx.setAllowed(false);
				String s = pi.getDisplayName() + " is required";
				emx.setResponse(s);
			}
			processObjectCallback(emx);

			if (!emx.isAllowed() || emx.getThrowable() != null) {
				em.setAllowed(emx.getAllowed());
				if (OAString.isEmpty(em.getResponse())) {
					em.setResponse(emx.getResponse());
				}
				break;
			}
			callObjectCallbackMethod(em, pi.getName(), em);
		}

		for (OALinkInfo li : oi.getLinkInfos()) {
			if (!em.isAllowed() || em.getThrowable() != null) {
				break;
			}
			Object val = li.getValue(obj);

			if (li.getType() == li.TYPE_ONE) {
				if (val == null && li.getRequired()) {
					em.setAllowed(false);
					String s = li.getDisplayName() + " is required";
					em.setResponse(s);
				} else if (li.getOwner()) {
					_getAllowSubmit(em, (OAObject) li.getValue(obj), cascade);
					em.setObject(obj);
				}
				callObjectCallbackMethod(em, li.getName(), em);

			} else {
				if (li.getOwner()) {
					for (OAObject objx : (Hub<OAObject>) li.getValue(obj)) {
						_getAllowSubmit(em, objx, cascade);
						em.setObject(obj);
						if (!em.isAllowed() || em.getThrowable() != null) {
							break;
						}
					}
				}
			}
		}
	}

	
	/**
	 * Returns the formatting string for the specified object and property by
	 * evaluating a {@link OAObjectCallback} of type {@code GetFormat}. The
	 * default format is set first, then evaluated again after assigning the
	 * property name.
	 *
	 * @param obj           the target object
	 * @param propertyName  the property whose format is requested
	 * @param defaultFormat the initial format value
	 * @return the resulting format string
	 */
	public String getFormat(OAObject obj, String propertyName, String defaultFormat) {
		OAObjectCallback em = new OAObjectCallback(Type.GetFormat);
		em.setObject(obj);
		em.setFormat(defaultFormat);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
		return em.getFormat();
	}
	
	/**
	 * Returns the tooltip text for the specified object and property by
	 * evaluating a {@link OAObjectCallback} of type {@code GetToolTip}. The
	 * default tooltip is assigned first, then evaluated again after assigning
	 * the property name.
	 *
	 * @param obj           the target object
	 * @param propertyName  the property whose tooltip is requested
	 * @param defaultToolTip the initial tooltip value
	 * @return the resulting tooltip text
	 */
	public String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
		OAObjectCallback em = new OAObjectCallback(Type.GetToolTip);
		em.setObject(obj);
		em.setToolTip(defaultToolTip);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
		return em.getToolTip();
	}
	
	/**
	 * Evaluates a {@link OAObjectCallback} of type {@code RenderLabel} to allow
	 * callback logic to update the given label used for rendering a component.
	 * Evaluation occurs once before and once after assigning the property name.
	 *
	 * @param obj          the target object
	 * @param propertyName the property associated with the label
	 * @param label        the label to be updated
	 */
	public void renderLabel(OAObject obj, String propertyName, JLabel label) {
		OAObjectCallback em = new OAObjectCallback(Type.RenderLabel);
		em.setObject(obj);
		em.setLabel(label);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
	}

	/**
	 * Evaluates a {@link OAObjectCallback} of type {@code UpdateLabel} to allow
	 * callback logic to modify the label associated with a component.
	 *
	 * @param obj          the target object
	 * @param propertyName the property associated with the label
	 * @param label        the label to update
	 */
	public void updateLabel(OAObject obj, String propertyName, JLabel label) {
		OAObjectCallback em = new OAObjectCallback(Type.UpdateLabel);
		em.setObject(obj);
		em.setPropertyName(propertyName);
		em.setLabel(label);
		callObjectCallbackMethod(em);
	}

	/**
	 * Creates and returns an {@link OAObjectCallback} of type
	 * {@code AllowVisible} for the given hub by delegating to the overloaded
	 * method with {@code null} object and property name.
	 *
	 * @param hub the hub providing visibility context
	 * @return the resulting callback
	 */
    public OAObjectCallback getAllowVisibleObjectCallback(Hub<? extends OAObject> hub) {
        return getAllowVisibleObjectCallback(hub, null, null);
    }

    /**
     * Creates and evaluates an {@link OAObjectCallback} of type
     * {@code AllowVisible} for the specified hub, object, and property or link
     * name. When the object is {@code null}, the appropriate master object is
     * resolved from the hub.
     *
     * @param hub   the hub providing contextual rules
     * @param oaObj the target object, or {@code null}
     * @param name  the property or link name, or {@code null}
     * @return the resulting callback, or {@code null} if hub and object are both null
     */
    @SuppressWarnings("unchecked")
    public <T extends OAObject> OAObjectCallback getAllowVisibleObjectCallback(Hub<T> hub, T oaObj, String name) {
		if (hub == null && oaObj == null) {
			return null;
		}
		if (oaObj == null) {
			if (name == null) {
				name = callHubDetailGetPropertyFromMasterToDetail(hub);
				oaObj = (T) hub.getMasterObject();
			} else {
				oaObj = (T) hub.getAO();
			}
		}
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowVisible, OAObjectCallback.CHECK_ALL, hub, null, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowEnabled} using the specified hub, object, property name,
	 * and check-type mask. When the object is {@code null}, the appropriate
	 * master object is resolved from the hub.
	 *
	 * @param checkType the bitmask of checking options
	 * @param hub       the hub providing contextual rules
	 * @param oaObj     the target object, or {@code null}
	 * @param name      the property, link, or method name, or {@code null}
	 * @return the resulting callback, or {@code null} if hub and object are both null
	 */
	public <T extends OAObject> OAObjectCallback getAllowEnabledObjectCallback(final int checkType, final Hub<T> hub, T oaObj, String name) {
		if (hub == null && oaObj == null) {
			return null;
		}
		if (oaObj == null) {
			if (name == null) {
				name = callHubDetailGetPropertyFromMasterToDetail(hub);
				oaObj = (T) hub.getMasterObject();
			} else {
				oaObj = (T) (OAObject) hub.getAO();
			}
		}
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled, checkType, hub, null, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowEnabled} for the given hub. If the hub has a master
	 * object, the callback is evaluated against that object; otherwise,
	 * only hub listeners are processed.
	 *
	 * @param hub the hub providing contextual rules
	 * @return the resulting callback
	 */
	public OAObjectCallback getAllowEnabledObjectCallback(Hub<? extends OAObject> hub) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled);

		OAObject objMaster = hub.getMasterObject();
		if (objMaster == null) {
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback.setPropertyName(propertyName);
			objectCallback.setObject(objMaster);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowCopy} for the specified object.
	 *
	 * @param oaObj the object being copied
	 * @return the resulting callback
	 */
	public OAObjectCallback getAllowCopyObjectCallback(final OAObject oaObj) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code GetCopy}
	 * to obtain a copy of the specified object or to allow callback logic to
	 * supply an alternate value.
	 *
	 * @param oaObj the object to copy
	 * @return the resulting callback
	 */
	public OAObjectCallback getCopyObjectCallback(final OAObject oaObj) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.GetCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AfterCopy}
	 * for the specified source object and its copy.
	 *
	 * @param oaObj     the original object
	 * @param oaObjCopy the copied object
	 * @return the resulting callback
	 */
	public <T extends OAObject> OAObjectCallback getAfterCopyObjectCallback(final T oaObj, final T oaObjCopy) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AfterCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				oaObjCopy);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyPropertyChange} to validate a property update.
	 * The old value is assigned before processing.
	 *
	 * @param checkType    the bitmask of checking options
	 * @param oaObj        the target object
	 * @param propertyName the property being changed
	 * @param oldValue     the previous value
	 * @param newValue     the proposed new value
	 * @return the resulting callback
	 */
	public OAObjectCallback getVerifyPropertyChangeObjectCallback(final int checkType, final OAObject oaObj,
			final String propertyName,
			final Object oldValue, final Object newValue) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyPropertyChange, checkType, null, null, oaObj, propertyName,
				newValue);
		objectCallback.setOldValue(oldValue);
		processObjectCallback(objectCallback);
		return objectCallback;
	}
	
	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyCommand} to validate invocation of the specified method.
	 *
	 * @param oaObj      the target object
	 * @param methodName the method to verify
	 * @param checkType  the bitmask of checking options
	 * @return the resulting callback
	 */
	public OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName, int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyCommand, checkType, null, null, oaObj, methodName, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}
	
	
	/**
	 * Updates the callback to disallow editing when context rules
	 * indicate that processed-state editing is not permitted.
	 *
	 * @param objectCallback the callback to update
	 */
	public void updateEditProcessed(OAObjectCallback objectCallback) {
		if (objectCallback == null) {
			return;
		}
		
		if (!callContextGetContext().getAllowEditProcessed()) {
			String sx = callContextGetContext().getAllowEditProcessedPropertyPath();
			objectCallback.setResponse("User." + sx + "=false");
			objectCallback.setAllowed(false);
		}
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowAdd}
	 * to determine whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on the
	 * hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param objAdd    the object being added
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(final Hub<T> hub, T objAdd, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;
		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowAdd, checkType, hub, null, null, null, objAdd);
			if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, objAdd);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowAdd, checkType, hub, null, objMaster, liRev.getName(), objAdd);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.AllowAdd, checkType, hub, null, null, null, objAdd);
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyAdd}
	 * to verify whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * the hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param oaObj     the object being added
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(final Hub<T> hub, final T oaObj, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();
		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyAdd, checkType, hub, null, oaObj, null, null);
			processObjectCallbackForHubListeners(objectCallback, hub, oaObj, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyAdd, checkType, hub, null, objMaster, liRev.getName(), oaObj);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.VerifyAdd, checkType, hub, null, null, null, oaObj);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}
	
	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowNew} to determine
	 * whether a new instance of the specified class may be created. Context and
	 * processed-state rules are evaluated before returning.
	 *
	 * @param clazz the class to evaluate
	 * @return the resulting callback, or {@code null} if the class is {@code null}
	 */
	public OAObjectCallback getAllowNewObjectCallback(final Class<? extends OAObject> clazz) {
		if (clazz == null) {
			return null;
		}
		final OAObjectInfo oi = callInfoGetObjectInfo(clazz);

		int ct = (OAObjectCallback.CHECK_Processed | OAObjectCallback.CHECK_UserEnabledProperty);
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowNew, ct, null, clazz, null, null, null);

		if (oi.getProcessed()) {
			updateEditProcessed(objectCallback);
		}
		if (objectCallback.getAllowed()) {
			String pp = oi.getContextEnabledProperty();
			if (OAString.isNotEmpty(pp)) {
				if (!callContextGetContext().isEnabled(pp, oi.getContextEnabledValue())) {
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + clazz.getSimpleName() + ", ";
					OAObject user = callContextGetContext().getContextObject();
					if (user == null) {
						s = "OAContext.getContextObject (User) returned null";
					} else {
						s = "User." + pp + " must be " + oi.getContextEnabledValue();
					}
					objectCallback.setResponse(s);
				}
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowNew}
	 * to determine whether a new object may be created for the given hub. When
	 * applicable, reverse-link rules or hub listeners are evaluated.
	 *
	 * @param hub the hub providing contextual rules
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public OAObjectCallback getAllowNewObjectCallback(final Hub<? extends OAObject> hub) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowNew, OAObjectCallback.CHECK_ALL, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			} else {
				objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowRemove}
	 * to determine whether the specified object may be removed from the hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * metadata.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param objRemove the object being removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(final Hub<T> hub, final T objRemove, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
			if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, objRemove);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowRemove, checkType, hub, null, objMaster, liRev.getName(), objRemove);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyRemove}
	 * to verify whether the specified object may be removed from the hub. Hub
	 * listener rules or reverse-link rules may be applied depending on metadata.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param objRemove the object being removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(final Hub<T> hub, final T objRemove, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyRemove, checkType, hub, null, null, null, objRemove);
			if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, objRemove);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyRemove, checkType, hub, null, objMaster, liRev.getName(), objRemove);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.VerifyRemove, checkType, hub, null, null, null, objRemove);
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowRemoveAll} to determine whether all objects may be
	 * removed from the specified hub. Hub listener rules or reverse-link
	 * rules may be applied depending on metadata.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public OAObjectCallback getAllowRemoveAllObjectCallback(final Hub<? extends OAObject> hub, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemoveAll, checkType, hub, null, null, null, null);
			if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowRemoveAll, checkType, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.AllowRemoveAll, checkType, hub, null, null, null, null);
		}
		return objectCallback;
	}
	
	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyRemoveAll} to verify whether all objects may be removed
	 * from the specified hub. Hub listener rules or reverse-link rules
	 * may be applied depending on metadata.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public OAObjectCallback getVerifyRemoveAllObjectCallback(final Hub<? extends OAObject> hub, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, checkType, hub, null, null, null, null);
			if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, checkType, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, checkType, hub, null, null, null, null);
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowSave} to determine whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public OAObjectCallback getAllowSaveObjectCallback(final OAObject oaObj, final int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowSave, checkType, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifySave} to verify whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj, final int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifySave, checkType, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowDelete}
	 * to determine whether the specified object may be deleted. Context
	 * and processed-state rules are evaluated before returning.
	 *
	 * @param objDelete the object to delete
	 * @return the resulting callback, or {@code null} if the object or its class is null
	 */
	public OAObjectCallback getAllowDeleteObjectCallback(final OAObject objDelete) {
		if (objDelete == null) {
			return null;
		}

		final Class clazz = objDelete.getClass();
		if (clazz == null) {
			return null;
		}
		final OAObjectInfo oi = callInfoGetObjectInfo(clazz);

		int ct = (OAObjectCallback.CHECK_Processed | OAObjectCallback.CHECK_UserEnabledProperty);
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowDelete, ct, null, clazz, null, null, objDelete);

		if (oi.getProcessed()) {
			updateEditProcessed(objectCallback);
		}
		if (objectCallback.getAllowed()) {
			String pp = oi.getContextEnabledProperty();
			if (OAString.isNotEmpty(pp)) {
				if (!callContextGetContext().isEnabled(pp, oi.getContextEnabledValue())) {
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + clazz.getSimpleName() + ", ";
					OAObject user = callContextGetContext().getContextObject();
					if (user == null) {
						s = "OAContext.getContextObject (User) returned null";
					} else {
						s = "User." + pp + " must be " + oi.getContextEnabledValue();
					}
					objectCallback.setResponse(s);
				}
			}
		}
		return objectCallback;
	}
	
	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowDelete} to determine whether the specified object
	 * may be deleted within the context of the given hub. Hub listener
	 * rules or reverse-link rules may be applied depending on metadata.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param objDelete the object to delete
	 * @return the resulting callback, or {@code null} if the hub or object is {@code null}
	 */
	public OAObjectCallback getAllowDeleteObjectCallback(final Hub<? extends OAObject> hub, final OAObject objDelete) {
		if (hub == null || objDelete == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = getAllowDeleteObjectCallback(objDelete);
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowDelete, OAObjectCallback.CHECK_ALL, hub, null, objMaster, liRev.getName(),
						objDelete);
				processObjectCallback(objectCallback);
			} else {
				objectCallback = getAllowDeleteObjectCallback(objDelete);
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyDelete} to verify whether the specified object may
	 * be deleted from the given hub. Hub listener rules or reverse-link
	 * rules may be applied depending on metadata.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param objDelete the object to delete
	 * @param checkType the bitmask of checking options
	 * @return the resulting callback
	 */
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(final Hub<T> hub, final T objDelete, final int checkType) {
		OAObjectCallback objectCallback = null;
		if (hub != null) {
			OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
			OAObject objMaster = hub.getMasterObject();

			if (li == null || (li.getPrivateMethod() && objMaster == null)) {
				objectCallback = new OAObjectCallback(Type.VerifyDelete, checkType, hub, null, null, null, objDelete);
				if ((checkType & OAObjectCallback.CHECK_Processed) != 0) {
					if (hub.getOAObjectInfo().getProcessed()) {
						updateEditProcessed(objectCallback);
					}
				}
				processObjectCallbackForHubListeners(objectCallback, hub, objDelete, null, null, null);
			} else {
				OALinkInfo liRev = li.getReverseLinkInfo();
				if (liRev != null && !li.getCalculated()) {
					objectCallback = new OAObjectCallback(Type.VerifyDelete, checkType, hub, null, objMaster, liRev.getName(), objDelete);
					processObjectCallback(objectCallback);
				}
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.VerifyDelete, checkType, hub, null, objDelete, null, null);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}
	
	/**
	 * Creates a confirmation {@link OAObjectCallback} for a property change
	 * using an unknown future value. This delegates to
	 * {@code getConfirmPropertyChangeObjectCallback} with the value set to
	 * {@code OAUnknownObject.instance}.
	 *
	 * @param oaObj          the target object
	 * @param property       the property being changed
	 * @param confirmMessage the message to present for confirmation
	 * @param confirmTitle   the title to present for confirmation
	 * @return the resulting callback
	 */
    public OAObjectCallback getPreConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, 
            String confirmMessage, String confirmTitle) {
        return getConfirmPropertyChangeObjectCallback(oaObj, property, OAUnknownObject.instance, confirmMessage, confirmTitle);
    }

    /**
     * Creates a confirmation {@link OAObjectCallback} for a property change
     * using an unknown future value. This delegates to
     * {@code getConfirmPropertyChangeObjectCallback} with the value set to
     * {@code OAUnknownObject.instance}.
     *
     * @param oaObj          the target object
     * @param property       the property being changed
     * @param confirmMessage the message to present for confirmation
     * @param confirmTitle   the title to present for confirmation
     * @return the resulting callback
     */
    public OAObjectCallback getConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, Object newValue,
			String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForPropertyChange, OAObjectCallback.CHECK_ALL, null,
				null,
				oaObj, property, newValue);
		objectCallback.setValue(newValue);
		objectCallback.setPropertyName(property);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}

    /**
     * Creates and evaluates an {@link OAObjectCallback} of type
     * {@code SetConfirmForCommand} to supply confirmation text for
     * invoking the specified method.
     *
     * @param oaObj          the target object
     * @param methodName     the method requiring confirmation
     * @param confirmMessage the confirmation message
     * @param confirmTitle   the confirmation title
     * @return the resulting callback
     */
	public OAObjectCallback getConfirmCommandObjectCallback(final OAObject oaObj, String methodName, String confirmMessage,
			String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForCommand, OAObjectCallback.CHECK_ALL, null, null,
				oaObj,
				methodName, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}
    
	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code SetConfirmForSave} to supply confirmation text for saving
	 * the specified object. The confirmation message and title are assigned
	 * to the callback before it is processed.
	 *
	 * @param oaObj          the object being saved
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public OAObjectCallback getConfirmSaveObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForSave, OAObjectCallback.CHECK_ALL, null, null, oaObj,
				null, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code SetConfirmForDelete} to supply confirmation text for deleting
	 * the specified object. The confirmation message and title are assigned
	 * to the callback before processing.
	 *
	 * @param oaObj          the object to delete
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public OAObjectCallback getConfirmDeleteObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForDelete, OAObjectCallback.CHECK_ALL, null, null,
				oaObj,
				null, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code SetConfirmForRemove}
	 * to supply confirmation text for removing the specified object from the
	 * given hub. When a master object exists, the property name for the
	 * master-to-detail link is assigned before processing.
	 *
	 * @param hub            the hub providing contextual rules
	 * @param oaObj          the object being removed
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
	public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage,
			String confirmTitle) {
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback = new OAObjectCallback(Type.SetConfirmForRemove, OAObjectCallback.CHECK_ALL, hub, null, oaObj, propertyName,
					null);
			objectCallback.setConfirmMessage(confirmMessage);
			objectCallback.setConfirmTitle(confirmTitle);
			processObjectCallback(objectCallback);
		} else {
			objectCallback = new OAObjectCallback(Type.SetConfirmForRemove, OAObjectCallback.CHECK_ALL, hub, null, oaObj, null, null);
			objectCallback.setConfirmMessage(confirmMessage);
			objectCallback.setConfirmTitle(confirmTitle);
		}
		return objectCallback;
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code SetConfirmForRemoveAll}
	 * to supply confirmation text for removing all objects from the given hub.
	 * When a master object exists, the master-to-detail property name is
	 * assigned before processing.
	 *
	 * @param hub            the hub whose objects may be removed
	 * @param confirmMessage the confirmation message to assign
	 * @param confirmTitle   the confirmation title to assign
	 * @return the resulting callback
	 */
    public OAObjectCallback getConfirmRemoveAllObjectCallback(final Hub<? extends OAObject> hub, String confirmMessage,
            String confirmTitle) {
        OAObjectCallback objectCallback;
        OAObject objMaster = hub.getMasterObject();
        if (objMaster != null) {
            String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
                objectCallback = new OAObjectCallback(Type.SetConfirmForRemoveAll, OAObjectCallback.CHECK_ALL, 
                    hub, null, null, propertyName, null);
            objectCallback.setConfirmMessage(confirmMessage);
            objectCallback.setConfirmTitle(confirmTitle);
            processObjectCallback(objectCallback);
        } else {
            objectCallback = new OAObjectCallback(Type.SetConfirmForRemoveAll, OAObjectCallback.CHECK_ALL, hub, null, null, null, null);
            objectCallback.setConfirmMessage(confirmMessage);
            objectCallback.setConfirmTitle(confirmTitle);
        }
        return objectCallback;
    }

	
    /**
     * Creates an {@link OAObjectCallback} of type {@code SetConfirmForAdd}
     * to supply confirmation text for adding the specified object to the
     * given hub. When a master object exists, the master-to-detail property
     * name is assigned before processing.
     *
     * @param hub            the hub receiving the object
     * @param oaObj          the object being added
     * @param confirmMessage the confirmation message to assign
     * @param confirmTitle   the confirmation title to assign
     * @return the resulting callback
     */
	public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage,
			String confirmTitle) {
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback = new OAObjectCallback(Type.SetConfirmForAdd, OAObjectCallback.CHECK_ALL, hub, null, oaObj, propertyName, null);
			objectCallback.setConfirmMessage(confirmMessage);
			objectCallback.setConfirmTitle(confirmTitle);
			processObjectCallback(objectCallback);
		} else {
			objectCallback = new OAObjectCallback(Type.SetConfirmForAdd, OAObjectCallback.CHECK_ALL, hub, null, oaObj, null, null);
			objectCallback.setConfirmMessage(confirmMessage);
			objectCallback.setConfirmTitle(confirmTitle);
		}
		return objectCallback;
	}

	/**
	 * Global demo-mode flag that forces all OAObjectCallback evaluations
	 * to succeed. When enabled, callback failures are overridden to allow
	 * all operations, useful for demos or testing scenarios.
	 */
	private static volatile boolean DEMO_AllowAllToPass;
	
	/**
	 * Processes the supplied callback by delegating to the internal
	 * {@code _processObjectCallback} method. After processing, the
	 * callback is updated to allow all operations when the demo flag
	 * is enabled, or when the current user is a super-admin.
	 *
	 * @param objectCallback the callback to process
	 */
	protected void processObjectCallback(OAObjectCallback objectCallback) {
		_processObjectCallback(objectCallback);
		if (DEMO_AllowAllToPass) {
			objectCallback.setThrowable(null);
			objectCallback.setAllowed(true);
		} else if ((!objectCallback.getAllowed() || objectCallback.getThrowable() != null)) {
			// allow AppUser.admin=true to always be valid
			if (callContextGetContext().isSuperAdmin()) { // allow all if super admin
				objectCallback.setThrowable(null);
				objectCallback.setAllowed(true);
			}
		}
	}
    
	/**
	 * Enables or disables demo mode for allowing all callbacks to pass.
	 * When enabled, warning messages are logged and printed to standard
	 * output. This flag affects subsequent callback processing.
	 *
	 * @param b {@code true} to allow all callbacks to pass; otherwise {@code false}
	 */
	public void demoAllowAllToPass(boolean b) {
		String msg = "WARNING: OAObjectCallbackDelegate.demoAllowAllToPass=" + b;
		if (b) {
			msg += " - all OAObjectCallback will be allowed";
		}
		LOG.warning(msg);
		for (int i = 0; i < 20; i++) {
			System.out.println(msg);
			if (!b) {
				break;
			}
		}
		DEMO_AllowAllToPass = b;
	}
	
	/**
	 * Processes the supplied callback by evaluating rules based on hub context,
	 * class metadata, object state, property-level definitions, user-access
	 * settings, and any callback methods defined on the object. This method
	 * updates the callback’s allowed state and response message as needed
	 * and invokes hub listeners when applicable.
	 *
	 * @param objectCallback the callback being evaluated
	 */
	protected void _processObjectCallback(final OAObjectCallback objectCallback) {
		final Hub hubThis = objectCallback.getHub();
		final Class<?> clazz = objectCallback.getCalcClass();
		final OAObject oaObj = objectCallback.getObject();
		final String propertyName = objectCallback.getPropertyName();
		final Object oldValue = objectCallback.getOldValue();
		final Object value = objectCallback.getValue();
		final int checkType = objectCallback.getCheckType();

		final boolean bCheckProcessedCheck = (objectCallback.getCheckType() & OAObjectCallback.CHECK_Processed) != 0;
		final boolean bCheckEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_EnabledProperty) != 0;
		final boolean bCheckUserEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_UserEnabledProperty) != 0;
		final boolean bCheckCallbackMethod = (objectCallback.getCheckType() & OAObjectCallback.CHECK_CallbackMethod) != 0;
		final boolean bCheckIncludeMaster = (objectCallback.getCheckType() & OAObjectCallback.CHECK_IncludeMaster) != 0;

		final OAObjectInfo oi = callInfoGetObjectInfo(clazz);

		if (bCheckProcessedCheck) {
			if (objectCallback.getType() == Type.AllowDelete && value != null && OAString.isEmpty(propertyName)) {
				OAObjectInfo oix = callInfoGetObjectInfo(value.getClass());
				if (oix.getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
		}

		final OAUserAccess userAccess = callContextGetContext().getContextUserAccess();
		
		// 20200217 add OAUserAccess
		if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType() == Type.AllowVisible) {
			if (userAccess != null) {
				boolean bx = true;
				if (objectCallback.getType() == Type.AllowEnabled) {
					if (oaObj != null) {
						bx = userAccess.getEnabled(oaObj);
					} else {
						bx = userAccess.getEnabled(clazz);
					}
				} else {
					if (oaObj != null) {
						bx = userAccess.getVisible(oaObj);
					} else {
						bx = userAccess.getVisible(clazz);
					}
				}
				if (!bx) {
					objectCallback.setAllowed(false);
					objectCallback.setResponse("UserAccess returned false");
					return;
				}
			}
		}

		// follow the first link (if any), if it is not owner
		if (bCheckIncludeMaster && hubThis != null
				&& (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().isCheckEnabledFirst()
						|| objectCallback.getType() == Type.AllowVisible)) {
			OALinkInfo li = callHubDetailGetLinkInfoFromMasterHubToDetail(hubThis);
			if (li != null && !li.getOwner()) {
				OAObject objx = hubThis.getMasterObject();
				if (objx != null) {
					if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().isCheckEnabledFirst()) {
						int ct = (objectCallback.getCheckType() ^ objectCallback.CHECK_IncludeMaster) ^ objectCallback.CHECK_Processed;
						OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, ct, hubThis.getMasterHub(), null, objx,
								li.getName(), null);
						objectCallbackX.setAllowed(objectCallback.getAllowed());
						_processObjectCallback(objectCallbackX);
						objectCallback.setAllowed(objectCallbackX.getAllowed());
						if (OAString.isEmpty(objectCallback.getResponse())) {
							objectCallback.setResponse(objectCallbackX.getResponse());
						}
					} else if (objectCallback.getType() == Type.AllowVisible) {
						int ct = (objectCallback.getCheckType() ^ objectCallback.CHECK_IncludeMaster) ^ objectCallback.CHECK_Processed;
						OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowVisible, ct, hubThis.getMasterHub(), null, objx,
								li.getName(), null);
						objectCallbackX.setAllowed(objectCallback.getAllowed());
						_processObjectCallback(objectCallbackX);
						objectCallback.setAllowed(objectCallbackX.getAllowed());
						if (OAString.isEmpty(objectCallback.getResponse())) {
							objectCallback.setResponse(objectCallbackX.getResponse());
						}
					}
				}
			}
		}

		if (oaObj != null && objectCallback.getType().isCheckOwner()) {
			ownerHierProcess(objectCallback, oaObj, propertyName);
		}
		if (bCheckProcessedCheck && oi.getProcessed() && OAString.isEmpty(propertyName) && objectCallback.getAllowed()
				&& ((objectCallback.getType() == Type.AllowEnabled) || objectCallback.getType().isCheckEnabledFirst())) {
			updateEditProcessed(objectCallback);
		}

		if (oaObj != null && objectCallback.getType() == Type.AllowVisible && OAString.isNotEmpty(propertyName)
				&& objectCallback.isAllowed()
				&& oi.getHasOneAndOnlyOneLink()) {
			OALinkInfo li = oi.getLinkInfo(propertyName);
			if (li != null && li.getOneAndOnlyOne()) {
				if (callPropertyGetProperty(oaObj, propertyName) == null) {
					for (OALinkInfo lix : oi.getLinkInfos()) {
						if (lix == li || !lix.getOneAndOnlyOne()) {
							continue;
						}
						if (callPropertyGetProperty(oaObj, lix.getName()) != null) {
							objectCallback.setAllowed(false);
						}
					}
				}
			}
		}

		// "allow" can be overwritten, if there is a lower level annotation/objectCallback defined
		if (objectCallback.getType() == Type.AllowVisible && OAString.isNotEmpty(propertyName)) {
			String sx = null;
			boolean bx = true;
			OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
			if (pi != null) {
				sx = pi.getVisibleProperty();
				bx = pi.getVisibleValue();
			} else {
				OALinkInfo li = oi.getLinkInfo(propertyName);
				if (li != null) {
					sx = li.getVisibleProperty();
					bx = li.getVisibleValue();
				} else {
					OACalcInfo ci = oi.getCalcInfo(propertyName);
					if (ci != null) {
						sx = ci.getVisibleProperty();
						bx = ci.getVisibleValue();
					} else {
						OAMethodInfo mi = oi.getMethodInfo(propertyName);
						if (mi != null) {
							sx = mi.getVisibleProperty();
							bx = mi.getVisibleValue();
						}
					}
				}
			}
			final boolean bHadVisibleProperty = (oaObj != null && OAString.isNotEmpty(sx));
			if (bHadVisibleProperty) {
				Object valx = callReflectGetProperty(oaObj, sx);
				objectCallback.setAllowed(bx == OAConv.toBoolean(valx));
				if (!objectCallback.getAllowed() && OAString.isEmpty(objectCallback.getResponse())) {
					objectCallback.setAllowed(false);
					String s = "Not visible, " + oaObj.getClass().getSimpleName() + "." + sx + " is not " + bx;
					objectCallback.setResponse(s);
				}
			}

			sx = null;
			bx = true;
			pi = oi.getPropertyInfo(propertyName);
			if (pi != null) {
				sx = pi.getContextVisibleProperty();
				bx = pi.getContextVisibleValue();
			} else {
				OALinkInfo li = oi.getLinkInfo(propertyName);
				if (li != null) {
					sx = li.getContextVisibleProperty();
					bx = li.getContextVisibleValue();
				} else {
					OACalcInfo ci = oi.getCalcInfo(propertyName);
					if (ci != null) {
						sx = ci.getContextVisibleProperty();
						bx = ci.getContextVisibleValue();
					} else {
						OAMethodInfo mi = oi.getMethodInfo(propertyName);
						if (mi != null) {
							sx = mi.getContextVisibleProperty();
							bx = mi.getContextVisibleValue();
						}
					}
				}
			}
			if ((!bHadVisibleProperty || objectCallback.getAllowed()) && OAString.isNotEmpty(sx)) {
				OAObject user = callContextGetContext().getContextObject();
				if (user == null) {
					
					if (!callSyncIsServer()) {
						objectCallback.setAllowed(false);
					}
				} else {
					Object valx = callReflectGetProperty(user, sx);
					objectCallback.setAllowed(bx == OAConv.toBoolean(valx));
				}
				if (!objectCallback.getAllowed() && OAString.isEmpty(objectCallback.getResponse())) {
					objectCallback.setAllowed(false);
					String s = user == null ? "User" : user.getClass().getSimpleName();
					s = "Not visible, " + s + "." + sx + " is not " + bx;
					objectCallback.setResponse(s);
				}
			}
		} else if ((objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().isCheckEnabledFirst())
				&& OAString.isNotEmpty(propertyName)) {
			// was: else if (objectCallback.getAllowed() && (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst) && OAString.isNotEmpty(propertyName)) {
			if (oaObj == null) {
				return;
			}
			String enabledName = null;
			boolean enabledValue = true;
			OAPropertyInfo pi = oi.getPropertyInfo(propertyName);
			boolean bIsProcessed = false;
			if (pi != null) {
				enabledName = pi.getEnabledProperty();
				enabledValue = pi.getEnabledValue();
				bIsProcessed = pi.getProcessed();
			} else {
				OALinkInfo li = oi.getLinkInfo(propertyName);
				if (li != null) {
					enabledName = li.getEnabledProperty();
					enabledValue = li.getEnabledValue();
					bIsProcessed = li.getProcessed();
				} else {
					OACalcInfo ci = oi.getCalcInfo(propertyName);
					if (ci != null) {
						enabledName = ci.getEnabledProperty();
						enabledValue = ci.getEnabledValue();
					} else {
						OAMethodInfo mi = oi.getMethodInfo(propertyName);
						if (mi != null) {
							enabledName = mi.getEnabledProperty();
							enabledValue = mi.getEnabledValue();
						}
					}
				}
			}

			if (bCheckProcessedCheck && bIsProcessed) {
				updateEditProcessed(objectCallback);
			}

			final boolean bHadEnabledProperty = (objectCallback.getAllowed() && OAString.isNotEmpty(enabledName));
			if (bHadEnabledProperty && bCheckEnabledProperty) {
				Object valx = callReflectGetProperty(oaObj, enabledName);
				objectCallback.setAllowed(enabledValue == OAConv.toBoolean(valx));
				if (!objectCallback.getAllowed() && OAString.isEmpty(objectCallback.getResponse())) {
					objectCallback.setAllowed(false);
					String s = "Not enabled, " + oaObj.getClass().getSimpleName() + "." + enabledName + " is not " + enabledValue;
					objectCallback.setResponse(s);
				}
			}

			enabledName = null;
			enabledValue = true;
			pi = oi.getPropertyInfo(propertyName);
			if (pi != null) {
				enabledName = pi.getContextEnabledProperty();
				enabledValue = pi.getContextEnabledValue();
			} else {
				OALinkInfo li = oi.getLinkInfo(propertyName);
				if (li != null) {
					enabledName = li.getContextEnabledProperty();
					enabledValue = li.getContextEnabledValue();
				} else {
					OACalcInfo ci = oi.getCalcInfo(propertyName);
					if (ci != null) {
						enabledName = ci.getContextEnabledProperty();
						enabledValue = ci.getContextEnabledValue();
					} else {
						OAMethodInfo mi = oi.getMethodInfo(propertyName);
						if (mi != null) {
							enabledName = mi.getContextEnabledProperty();
							enabledValue = mi.getContextEnabledValue();
						}
					}
				}
			}
			if ((!bHadEnabledProperty || objectCallback.getAllowed()) && OAString.isNotEmpty(enabledName) && bCheckUserEnabledProperty) {
				boolean b = callContextGetContext().isEnabled(enabledName, enabledValue);
				objectCallback.setAllowed(b);
				if (!b) {
					objectCallback.setAllowed(false);
					if (OAString.isEmpty(objectCallback.getResponse())) {
						objectCallback.setAllowed(false);
						OAObject user = callContextGetContext().getContextObject();
						String s = user == null ? "User" : user.getClass().getSimpleName();
						s = "Not enabled, " + s + "." + enabledName + " is not " + enabledValue;
						objectCallback.setResponse(s);
					}
				}
			}
		}

		if (oaObj == null) {
			return;
		}
		Hub[] hubs = callHubGetHubReferences(oaObj);

		// call the callback method, this can override eq.allowed
		if (OAString.isNotEmpty(propertyName) && objectCallback.getType().isCheckEnabledFirst()) {
			OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, OAObjectCallback.CHECK_CallbackMethod, null, null,
					oaObj, propertyName, null);
			objectCallbackX.setAllowed(objectCallback.getAllowed());
			callObjectCallbackMethod(objectCallbackX);

			// call hub listeners
			if (hubs != null) {
				for (Hub h : hubs) {
					if (h == null) {
						continue;
					}
					processObjectCallbackForHubListeners(objectCallbackX, h, oaObj, propertyName, oldValue, value);
				}
			}
			objectCallback.setAllowed(objectCallbackX.getAllowed());
			if (OAString.isEmpty(objectCallback.getResponse())) {
				objectCallback.setResponse(objectCallbackX.getResponse());
			}
		}

		if (bCheckCallbackMethod) {
			callObjectCallbackMethod(objectCallback);
		}

		// call hub listeners
		if (hubs != null) {
			for (Hub h : hubs) {
				if (h == null) {
					continue;
				}
				processObjectCallbackForHubListeners(objectCallback, h, oaObj, propertyName, oldValue, value);
			}
		}
	}

	/**
	 * Evaluates visibility or enabled-state rules for the specified object
	 * and all of its owners by delegating to the recursive
	 * {@code _ownerHierProcess} method starting at depth {@code 0}.
	 *
	 * @param objectCallback the callback being updated
	 * @param oaObj          the target object
	 * @param propertyName   the property or link name associated with the callback
	 */
	protected void ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName) {
		_ownerHierProcess(objectCallback, oaObj, propertyName, null, 0);
	}
	
	/**
	 * Recursively evaluates visibility rules for the specified object and its
	 * owner hierarchy. Starting from the topmost owner, class-level and
	 * context-level visibility settings are applied, followed by invocation
	 * of any object-level callback method. The callback’s allowed state and
	 * response message may be updated based on these evaluations.
	 *
	 * @param objectCallback the callback being updated
	 * @param oaObj          the current object being evaluated
	 * @param propertyName   the property or link name associated with the callback
	 * @param li             the link used when navigating the owner hierarchy
	 * @param cnter          the recursion depth counter
	 */
	protected void _ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName,
			final OALinkInfo li, final int cnter) {
		if (oaObj == null) {
			return;
		}
		if (cnter > 50) {
			return;
		}
		// recursive, goto top owner first
		OAObjectInfo oi = callInfoGetObjectInfo(oaObj.getClass());

		OALinkInfo lix = oi.getOwnedByOne();
		if (lix != null) {
			OAObject objOwner = (OAObject) lix.getValue(oaObj);
			if (objOwner != null) {
				lix = lix.getReverseLinkInfo();
				_ownerHierProcess(objectCallback, objOwner, lix.getName(), lix, cnter + 1);
			}
		}

		String pp;
		boolean b;
		Object valx;
		boolean bPassed = objectCallback.getAllowed();

		// check class level @OAObjCallback annotation
		if (objectCallback.getType() == Type.AllowVisible) {
			pp = oi.getVisibleProperty();
			if (bPassed && OAString.isNotEmpty(pp)) {
				b = oi.getVisibleValue();
				valx = callReflectGetProperty(oaObj, pp);
				bPassed = (b == OAConv.toBoolean(valx));
				if (!bPassed) {
					objectCallback.setAllowed(false);
					String s = "Not visible, rule for " + oaObj.getClass().getSimpleName() + ", " + pp + " != " + b;
					objectCallback.setResponse(s);
				}
			}
			pp = oi.getContextVisibleProperty();
			if (bPassed && OAString.isNotEmpty(pp)) {
				b = oi.getContextVisibleValue();

				OAObject user = callContextGetContext().getContextObject();
				if (user == null) {
					if (!callSyncIsServer()) {
						bPassed = false;
					}
				} else {
					valx = callReflectGetProperty(user, pp);
					bPassed = (b == OAConv.toBoolean(valx));
				}
				if (!bPassed) {
					objectCallback.setAllowed(false);
					String s = "Not visible, user rule for " + oaObj.getClass().getSimpleName() + ", ";
					if (user == null) {
						s = "OAAuthDelegate.getUser returned null";
					} else {
						s = "User." + pp + " != " + b;
					}
					objectCallback.setResponse(s);
				}
			}

			// this can overwrite objectCallback.allowed
			callObjectCallbackMethod(oaObj, null, objectCallback);
			bPassed = objectCallback.getAllowed();
			if (!bPassed && OAString.isEmpty(objectCallback.getResponse())) {
				String s = "Not visible, edit query for " + oaObj.getClass().getSimpleName() + " allowVisible returned false";
				objectCallback.setResponse(s);
			}

			if (bPassed && li != null) {
				pp = li.getVisibleProperty();
				if (OAString.isNotEmpty(pp)) {
					b = li.getVisibleValue();
					valx = callReflectGetProperty(oaObj, pp);
					bPassed = (b == OAConv.toBoolean(valx));
					if (!bPassed) {
						objectCallback.setAllowed(false);
						String s = "Not visible, rule for " + oaObj.getClass().getSimpleName() + "." + propertyName + ", " + pp + " != "
								+ b;
						objectCallback.setResponse(s);
					}
				}
			}
			if (bPassed && li != null) {
				pp = li.getContextVisibleProperty();
				if (OAString.isNotEmpty(pp)) {
					b = li.getContextVisibleValue();
					OAObject user = callContextGetContext().getContextObject();
					if (user == null) {
						if (!callSyncIsServer()) {
							bPassed = false;
						}
					} else {
						valx = callReflectGetProperty(user, pp);
						bPassed = (b == OAConv.toBoolean(valx));
					}
					if (!bPassed) {
						objectCallback.setAllowed(false);
						String s = "Not visible, user rule for " + oaObj.getClass().getSimpleName() + "." + propertyName + ", ";
						if (user == null) {
							s = "OAAuthDelegate.getUser returned null";
						} else {
							s = "User." + pp + " must be " + b;
						}
						objectCallback.setResponse(s);
					}
				}
			}

			// this can overwrite objectCallback.allowed
			if (li != null && OAString.isNotEmpty(propertyName)) {
				callObjectCallbackMethod(oaObj, propertyName, objectCallback);
				bPassed = objectCallback.getAllowed();
				if (!bPassed && OAString.isEmpty(objectCallback.getResponse())) {
					String s = "Not visible, edit query for " + oaObj.getClass().getSimpleName() + "." + propertyName
							+ " allowVisible returned false";
					objectCallback.setResponse(s);
				}
			}
		} else if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().isCheckEnabledFirst()) {
			//was:  else if ( (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst) && !(OASync.isServer() && OARuntime.threadService().getContext() == null)) {

			// final boolean bCheckProcessedCheck = (objectCallback.getCheckType() & OAObjectCallback.CHECK_Processed) != 0;
			final boolean bCheckEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_EnabledProperty) != 0;
			final boolean bCheckUserEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_UserEnabledProperty) != 0;
			final boolean bCheckCallbackMethod = (objectCallback.getCheckType() & OAObjectCallback.CHECK_CallbackMethod) != 0;

			if (bPassed) {
				pp = oi.getEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
					b = oi.getEnabledValue();
					valx = callReflectGetProperty(oaObj, pp);
					bPassed = (b == OAConv.toBoolean(valx));
					if (!bPassed) {
						objectCallback.setAllowed(false);
						String s = "Not enabled, rule for " + oaObj.getClass().getSimpleName() + ", " + pp + " != " + b;
						objectCallback.setResponse(s);
					}
				}
			}
			pp = oi.getContextEnabledProperty();
			if (bPassed && OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
				b = oi.getContextEnabledValue();
				if (!callContextGetContext().isEnabled(pp, b)) {
					bPassed = false;
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + oaObj.getClass().getSimpleName() + ", ";
					OAObject user = callContextGetContext().getContextObject();
					if (user == null) {
						s = "OAAuthDelegate.getUser returned null";
					} else {
						s = "User." + pp + " must be " + b;
					}
					objectCallback.setResponse(s);
				}
			}

			// this can overwrite objectCallback.allowed
			if (bCheckCallbackMethod) {
				OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, objectCallback.getCheckType(), objectCallback);

				callObjectCallbackMethod(oaObj, null, objectCallbackX);
				bPassed = objectCallbackX.getAllowed();
				objectCallback.setAllowed(bPassed);
				if (!bPassed && OAString.isEmpty(objectCallback.getResponse())) {
					String s = "Not enabled, edit query for " + oaObj.getClass().getSimpleName() + " allowEnabled returned false";
					objectCallback.setResponse(s);
				}
			}

			if (li != null && bPassed) {
				pp = li.getEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
					b = li.getEnabledValue();
					valx = callReflectGetProperty(oaObj, pp);
					bPassed = (b == OAConv.toBoolean(valx));
					if (!bPassed) {
						objectCallback.setAllowed(false);
						String s = "Not enabled, rule for " + oaObj.getClass().getSimpleName() + "." + propertyName + ", " + pp + " != "
								+ b;
						objectCallback.setResponse(s);
					}
				}
			}

			if (li != null && bPassed) {
				pp = li.getContextEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
					b = li.getContextEnabledValue();
					if (!callContextGetContext().isEnabled(pp, b)) {
						OAObject user = callContextGetContext().getContextObject();
						objectCallback.setAllowed(false);
						String s = "Not enabled, user rule for " + oaObj.getClass().getSimpleName() + "." + propertyName + ", ";
						if (user == null) {
							s = "OAAuthDelegate.getUser returned null";
						} else {
							s = "User." + pp + " must be " + b;
						}
						objectCallback.setResponse(s);
					}
				}
			}

			// this can overwrite objectCallback.allowed
			if (bCheckCallbackMethod && li != null && OAString.isNotEmpty(propertyName)) {
				OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, objectCallback.getCheckType(), objectCallback);
				callObjectCallbackMethod(oaObj, propertyName, objectCallbackX);
				bPassed = objectCallbackX.getAllowed();
				objectCallback.setAllowed(bPassed);
				if (!bPassed && OAString.isEmpty(objectCallback.getResponse())) {
					String s = "Not enabled, edit query for " + oaObj.getClass().getSimpleName() + "." + propertyName
							+ " allowEnabled returned false";
					objectCallback.setResponse(s);
				}
			}
		}
	}

	/**
	 * Notifies hub-level listeners of a callback event by creating or updating
	 * a {@link HubEvent} and invoking the appropriate listener method based
	 * on the callback type. The callback’s allowed state and response message
	 * may be updated according to listener results. Any thrown exception marks
	 * the callback as not allowed.
	 *
	 * @param objectCallback the callback being evaluated
	 * @param hub            the hub providing listeners
	 * @param oaObj          the target object
	 * @param propertyName   the property or link associated with the callback
	 * @param oldValue       the previous value for property-change callbacks
	 * @param newValue       the new value for property-change callbacks
	 */
	protected <T extends OAObject> void processObjectCallbackForHubListeners(OAObjectCallback objectCallback, final Hub<T> hub, final T oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		if (objectCallback.getType().isCheckEnabledFirst()) {
			OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled);
			objectCallbackX.setAllowed(objectCallback.getAllowed());
			objectCallbackX.setPropertyName(objectCallback.getPropertyName());
			_processObjectCallbackForHubListeners(objectCallbackX, hub, oaObj, propertyName, oldValue, newValue);
			objectCallback.setAllowed(objectCallbackX.getAllowed());
		}
		_processObjectCallbackForHubListeners(objectCallback, hub, oaObj, propertyName, oldValue, newValue);
	}
	
	/**
	 * Internal helper used to notify hub listeners of a callback event. A
	 * {@link HubEvent} is created if needed, and each listener is invoked
	 * according to the callback type. Listener results update the callback’s
	 * allowed state and response message. Exceptions mark the callback as not
	 * allowed.
	 *
	 * @param objectCallback the callback being evaluated
	 * @param hub            the hub providing listeners
	 * @param oaObj          the target object
	 * @param propertyName   the property or link associated with the callback
	 * @param oldValue       the previous property value
	 * @param newValue       the new property value
	 */
	protected <T extends OAObject> void _processObjectCallbackForHubListeners(OAObjectCallback objectCallback, final Hub<T> hub, final T oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		
		HubListener<T>[] hl = callHubEventGetAllListeners(hub);
		if (hl == null) {
			return;
		}
		int x = hl.length;
		if (x == 0) {
			return;
		}
		final boolean bBefore = objectCallback.getAllowed();

		HubEvent<T> hubEvent = null;
		try {
			for (int i = 0; i < x; i++) {
				boolean b = objectCallback.getAllowed();

				switch (objectCallback.getType()) {
				case AllowEnabled:
					if (hubEvent == null) {
						hubEvent = new HubEvent<T>(hub, oaObj, propertyName);
					}
					b = hl[i].getAllowEnabled(hubEvent, b);
					break;
				case AllowVisible:
					if (hubEvent == null) {
						hubEvent = new HubEvent<T>(hub, oaObj, propertyName);
					}
					b = hl[i].getAllowVisible(hubEvent, b);
					break;

				case VerifyPropertyChange:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj, propertyName, oldValue, newValue);
					}
					b = hl[i].isValidPropertyChange(hubEvent, b);
					break;

				case AllowNew:
				case AllowAdd:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowAdd(hubEvent, b);
					break;
				case VerifyAdd:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj);
					}
					b = hl[i].isValidAdd(hubEvent, b);
					break;
				case AllowRemove:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowRemove(hubEvent, b);
					break;
				case VerifyRemove:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj);
					}
					b = hl[i].isValidRemove(hubEvent, b);
					break;
				case AllowRemoveAll:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowRemoveAll(hubEvent, b);
					break;
				case VerifyRemoveAll:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].isValidRemoveAll(hubEvent, b);
					break;
				case AllowDelete:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowDelete(hubEvent, b);
					break;
				case VerifyDelete:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj);
					}
					b = hl[i].isValidDelete(hubEvent, b);
					break;
				default:
					break;
				}

				if (hubEvent == null) {
					break;
				}
				objectCallback.setAllowed(b);
				String s = hubEvent.getResponse();
				if (OAString.isNotEmpty(s)) {
					objectCallback.setResponse(s);
				}
			}
		} catch (Exception e) {
			objectCallback.setThrowable(e);
			objectCallback.setAllowed(false);
		}

		if (bBefore != objectCallback.getAllowed()) {
			String s = objectCallback.getResponse();
			if (OAString.isEmpty(s)) {
				s = objectCallback.getType() + " failed for " + oaObj.getClass().getSimpleName() + "." + propertyName;
			}
			objectCallback.setResponse(s);
		}
	}

	/**
	 * Invokes the callback method on the object referenced by the supplied
	 * {@link OAObjectCallback}. The method is resolved using the object's
	 * metadata and executed with the callback instance. If no method exists,
	 * the call is ignored.
	 *
	 * @param em the callback whose associated object method is invoked
	 */
	protected void callObjectCallbackMethod(final OAObjectCallback em) {
		callObjectCallbackMethod(em.getObject(), em.getPropertyName(), em);
	}

	/**
	 * Resolves and invokes the object-level callback method associated with
	 * the given property. If the resolved method exists, it is executed with
	 * the supplied callback instance. Any exception marks the callback as not
	 * allowed and stores the throwable.
	 *
	 * @param object        the target object on which the method may be invoked
	 * @param propertyName  the property whose callback method is requested
	 * @param em            the callback instance passed to the invoked method
	 */
	protected void callObjectCallbackMethod(final Object object, String propertyName, final OAObjectCallback em) {
		if (object == null) {
			return;
		}
		OAObjectInfo oi = callInfoGetObjectInfo(object.getClass());

		if (propertyName == null) {
			propertyName = ""; // blank will be method for class level:   onObjectCallback(..)  or callback(OAObjectCallback)
		}

		Method method = oi.getObjectCallbackMethod(propertyName);
		if (method == null) {
			return;
		}
		//Class[] cs = method.getParameterTypes();
		//if (cs[0].equals(OAObjectCallback.class)) {
		try {
			method.invoke(object, new Object[] { em });
		} catch (Exception e) {
			em.setThrowable(e);
			em.setAllowed(false);
		}
		//}
	}

	/**
	 * Invokes a model-level callback method for the specified class and
	 * property. The method is resolved from the class metadata and must
	 * accept an {@link OAObjectModel} parameter. If found, the method is
	 * invoked statically with the supplied model instance.
	 *
	 * @param clazz    the class defining the callback
	 * @param property the property name used to locate the callback method
	 * @param model    the model instance passed to the callback
	 */
	public void onObjectCallbackModel(Class<? extends OAObject> clazz, String property, OAObjectModel model) {
		if (clazz == null || OAString.isEmpty(property) || model == null) {
			return;
		}
		OAObjectInfo oi = callInfoGetObjectInfo(clazz);
		Method m = callInfoGetMethod(oi, property + "ModelCallback", OAObjectModel.class);
		if (m == null) {
			m = callInfoGetMethod(oi, "onObjectCallback" + property + "Model", 1);
		}
		if (m != null) {
			Class[] cs = m.getParameterTypes();
			if (cs[0].equals(OAObjectModel.class)) {
				try {
					m.invoke(null, new Object[] { model });
				} catch (Exception e) {
					throw new RuntimeException("Exception calling static method " + m, e);
				}
			}
		}
	}

	/**
	 * Registers change listeners on properties that influence the visibility
	 * or enabled state evaluated by an {@link OAObjectCallback}. Class-level
	 * and dependent properties are added to the supplied listener using the
	 * provided prefix.
	 *
	 * @param hub            the hub whose objects are monitored
	 * @param cz             the class whose metadata defines dependent properties
	 * @param prop           the property associated with the callback
	 * @param ppPrefix       an optional prefix for dependent property paths
	 * @param changeListener the listener to register dependencies with
	 * @param bEnabled       true to use enabled dependencies; false for visible
	 */
	public <T extends OAObject> void addObjectCallbackChangeListeners(final Hub<T> hub, final Class<T> cz, final String prop, String ppPrefix,
			final HubChangeListener changeListener, final boolean bEnabled) {
		if (ppPrefix == null) {
			ppPrefix = "";
		}
		OAObjectInfo oi = callInfoGetObjectInfo(cz);
		String s;

		if (bEnabled) {
			s = oi.getEnabledProperty();
		} else {
			s = oi.getVisibleProperty();
		}
		if (OAString.isNotEmpty(s)) {
			changeListener.add(hub, ppPrefix + s);
		}

		// dependent properties
		addDependentProps(	hub, ppPrefix,
							bEnabled ? null : oi.getViewDependentProperties(),
							bEnabled ? oi.getContextDependentProperties() : null,
							(OAString.isEmpty(prop) && oi.getProcessed()),
							changeListener);

		final Hub hubUser = callContextGetContext().getContextHub();
		if (bEnabled) {
			s = oi.getContextEnabledProperty();
		} else {
			s = oi.getContextVisibleProperty();
		}
		if (OAString.isNotEmpty(s)) {
			changeListener.add(hubUser, s);
		}

		if (OAString.isEmpty(prop)) {
			return;
		}

		OAPropertyInfo pi = oi.getPropertyInfo(prop);
		if (pi != null) {
			if (bEnabled) {
				s = pi.getEnabledProperty();
			} else {
				s = pi.getVisibleProperty();
			}
			if (OAString.isNotEmpty(s)) {
				changeListener.add(hub, ppPrefix + s);
			}
			addDependentProps(	hub, ppPrefix, pi.getViewDependentProperties(), pi.getContextDependentProperties(),
								(bEnabled && pi.getProcessed()), changeListener);

			if (bEnabled) {
				s = pi.getContextEnabledProperty();
			} else {
				s = pi.getContextVisibleProperty();
			}
			if (OAString.isNotEmpty(s)) {
				changeListener.add(hubUser, s);
			}
		} else {
			OALinkInfo li = oi.getLinkInfo(prop);
			if (li != null) {
				if (bEnabled) {
					s = li.getEnabledProperty();
				} else {
					s = li.getVisibleProperty();
				}
				if (OAString.isNotEmpty(s)) {
					changeListener.add(hub, ppPrefix + s);
				}
				addDependentProps(	hub, ppPrefix, li.getViewDependentProperties(), li.getContextDependentProperties(),
									(bEnabled && li.getProcessed()), changeListener);

				if (bEnabled) {
					s = li.getContextEnabledProperty();
				} else {
					s = li.getContextVisibleProperty();
				}
				if (OAString.isNotEmpty(s)) {
					changeListener.add(hubUser, s);
				}
			} else {
				OACalcInfo ci = oi.getCalcInfo(prop);
				if (ci != null) {
					if (bEnabled) {
						s = ci.getEnabledProperty();
					} else {
						s = ci.getVisibleProperty();
					}
					if (OAString.isNotEmpty(s)) {
						changeListener.add(hub, ppPrefix + s);
					}
					addDependentProps(	hub, ppPrefix, ci.getViewDependentProperties(), ci.getContextDependentProperties(), false,
										changeListener);

					if (bEnabled) {
						s = ci.getContextEnabledProperty();
					} else {
						s = ci.getContextVisibleProperty();
					}
					if (OAString.isNotEmpty(s)) {
						changeListener.add(hubUser, s);
					}
				} else {
					OAMethodInfo mi = oi.getMethodInfo(prop);
					if (mi != null) {
						if (bEnabled) {
							s = mi.getEnabledProperty();
						} else {
							s = mi.getVisibleProperty();
						}
						if (OAString.isNotEmpty(s)) {
							changeListener.add(hub, ppPrefix + s);
						}
						addDependentProps(	hub, ppPrefix, mi.getViewDependentProperties(), mi.getContextDependentProperties(), false,
											changeListener);

						if (bEnabled) {
							s = mi.getContextEnabledProperty();
						} else {
							s = mi.getContextVisibleProperty();
						}
						if (OAString.isNotEmpty(s)) {
							changeListener.add(hubUser, s);
						}
					}
				}
			}
		}
	}

	/**
	 * Adds dependent property paths to the supplied {@link HubChangeListener}.
	 * View-level, context-level, and processed-dependent properties are added
	 * using the provided prefix. Null or empty property arrays are ignored.
	 *
	 * @param hub                       the hub whose objects are monitored
	 * @param prefix                    optional property-path prefix
	 * @param viewDependentProperties   properties affecting visibility
	 * @param contextDependentProperties properties affecting context visibility or enabled state
	 * @param bProcessed                true to include processed-dependent properties
	 * @param changeListener            the listener that receives dependent paths
	 */
	protected void addDependentProps(final Hub<? extends OAObject> hub, String prefix, String[] viewDependentProperties, String[] contextDependentProperties,
			boolean bProcessed, HubChangeListener changeListener) {
		if (viewDependentProperties != null) {
			for (String s : viewDependentProperties) {
				changeListener.add(hub, prefix + s);
			}
		}
		if (contextDependentProperties != null) {
			Hub hubUser = callContextGetContext().getContextHub();
			if (contextDependentProperties.length > 0 && hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			for (String s : contextDependentProperties) {
				changeListener.add(hubUser, s);
			}
		}
		if (bProcessed) {
			Hub hubUser = callContextGetContext().getContextHub();
			if (hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			changeListener.add(hubUser, callContextGetContext().getAllowEditProcessedPropertyPath());
		}
	}

	
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz);	

	public abstract Object callPropertyGetProperty(OAObject oaObj, String propertyName);

	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);
	public abstract <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj);	
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, final Class<?> classParam);	
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);	
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<? extends OAObject> thisHub);	
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<? extends OAObject> hub);
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<? extends OAObject> hub);
	public abstract <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> hub);

	public abstract boolean callSyncIsServer();
	protected abstract OAContext callContextGetContext();
}

