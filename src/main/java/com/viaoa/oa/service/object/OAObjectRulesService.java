package com.viaoa.oa.service.object;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.viaoa.callback.OACallbackLabel;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.callback.OAObjectCallback.CheckType;
import com.viaoa.callback.OAObjectCallback.Type;
import com.viaoa.cascade.OACascade;
import com.viaoa.compare.match.OAMatchUnknown;
import com.viaoa.converter.OAConv;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.listener.HubChangeListener;
import com.viaoa.lang.OAStr;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OACalcInfo;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAMethodInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAObjectModel;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.session.OASessionAccess;
import com.viaoa.session.OASessionUser;

/**
 * OA 4.0 object rules engine.
 * <p>
 * {@code OAObjectRulesService} evaluates model-rule questions carried by
 * {@link OAObjectCallback}. The callback {@link Type} defines the semantic
 * question being asked, and {@link CheckType} values define which rules-engine
 * stages are active through {@link OAObjectCallback#isUsed(CheckType)}.
 * </p>
 *
 * <p>The primary processing order is:</p>
 * <ol>
 *   <li>Session checks</li>
 *   <li>Metadata and object-state checks</li>
 *   <li>Object callback methods</li>
 *   <li>Hub listeners</li>
 *   <li>SuperAdmin override</li>
 * </ol>
 *
 * <p>Later stages may intentionally refine or override earlier rule responses.
 * Owner hierarchy processing is a lightweight containment gate for owner
 * visible/enabled state, not a full secondary rules pipeline.</p>
 *
 * <p>Callback context follows the {@link OAObjectCallback} contract:</p>
 * <ul>
 *   <li>{@code object} is the callback receiver or target context.</li>
 *   <li>{@code propertyName} is the member/property on that object.</li>
 *   <li>{@code value} is the operation operand.</li>
 *   <li>{@code oldValue} is the previous value when applicable.</li>
 * </ul>
 */
public abstract class OAObjectRulesService {
	private static final Logger LOG = Logger.getLogger(OAObjectRulesService.class.getName());

	/**
	 * Creates the rules service.
	 */
	public OAObjectRulesService() {
	}

	/**
	 * Returns whether an object/member is visible for the supplied Hub/object context.
	 *
	 * @param hub Hub context, or {@code null}
	 * @param obj callback receiver/target context, or {@code null}
	 * @param name member/property name on {@code obj}, or {@code null}
	 * @return {@code true} if visible
	 */
	public <T extends OAObject> boolean getAllowVisible(Hub<T> hub, T obj, String name) {
		OAObjectCallback cb = getAllowVisibleObjectCallback(hub, obj, name);
		return cb == null ? false : cb.getAllowed();
	}
    @SuppressWarnings("unchecked")
    /**
     * Creates and processes an {@link OAObjectCallback} for {@link Type#AllowVisible}.
     *
     * @param hub Hub context, or {@code null}
     * @param oaObj callback receiver/target context, or {@code null}
     * @param name member/property name on {@code oaObj}, or {@code null}
     * @return processed callback, or {@code null} when no Hub or object is supplied
     */
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
		Class<? extends OAObject> type = hub != null ? hub.getObjectClass() : oaObj.getClass();
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowVisible, hub, type, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
    }


	/**
	 * Returns whether a property change passes rule verification.
	 *
	 * @param obj callback receiver/target object
	 * @param propertyName property being changed
	 * @param oldValue previous property value
	 * @param newValue proposed property value
	 * @return {@code true} if the change is allowed
	 */
	public boolean getVerifyPropertyChange(OAObject obj, String propertyName, Object oldValue, Object newValue) {
		OAObjectCallback cb = getVerifyPropertyChangeObjectCallback(null, obj, propertyName, oldValue, newValue);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether a property change is allowed by object callback methods only.
	 *
	 * @param obj callback receiver/target object
	 * @param propertyName property being changed
	 * @param oldValue previous property value
	 * @param newValue proposed property value
	 * @return {@code true} if the callback-only check allows the change
	 */
	public boolean getVerifyPropertyChangeCallbackOnly(OAObject obj, String propertyName, Object oldValue, Object newValue) {
		OAObjectCallback cb = getVerifyPropertyChangeObjectCallback(OAObjectCallback.getCallbackOnlyCheckType(), obj, propertyName, oldValue, newValue);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Creates and processes a {@link Type#VerifyPropertyChange} callback.
	 *
	 * @param checkTypeOnly optional rules-engine checks to use instead of the type defaults
	 * @param oaObj callback receiver/target object
	 * @param propertyName property being changed
	 * @param oldValue previous property value
	 * @param newValue proposed property value
	 * @return processed callback
	 */
	public OAObjectCallback getVerifyPropertyChangeObjectCallback(OAObjectCallback.CheckType[] checkTypeOnly, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) 
	{
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyPropertyChange, checkTypeOnly, null, null, oaObj, propertyName, newValue);
		objectCallback.setOldValue(oldValue);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public OAObjectCallback getVerifyPropertyChangeObjectCallback(final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) 
	{
		return getVerifyPropertyChangeObjectCallback(null, oaObj, propertyName, oldValue, newValue);
	}
	
	/**
	 * Creates and processes a property-change callback using only object callback methods.
	 *
	 * @param oaObj callback receiver/target object
	 * @param propertyName property being changed
	 * @param oldValue previous property value
	 * @param newValue proposed property value
	 * @return processed callback
	 */
	public OAObjectCallback getVerifyPropertyChangeCallbackOnlyObjectCallback(final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
		return getVerifyPropertyChangeObjectCallback(OAObjectCallback.getCallbackOnlyCheckType(), oaObj, propertyName, oldValue, newValue);
	}

	/**
	 * Returns whether an object/member is enabled for the supplied Hub/object context.
	 *
	 * @param hub Hub context, or {@code null}
	 * @param obj callback receiver/target context, or {@code null}
	 * @param name member/property name on {@code obj}, or {@code null}
	 * @return {@code true} if enabled
	 */
	public <T extends OAObject> boolean getAllowEnabled(Hub<T> hub, T obj, String name) {
		OAObjectCallback cb = getAllowEnabledObjectCallback(null, hub, obj, name);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object/member is enabled by object callback methods only.
	 *
	 * @param hub Hub context, or {@code null}
	 * @param obj callback receiver/target context, or {@code null}
	 * @param name member/property name on {@code obj}, or {@code null}
	 * @return {@code true} if callback-only evaluation allows it
	 */
	public <T extends OAObject> boolean getAllowEnabledCallbackOnly(Hub<T> hub, T obj, String name) {
		OAObjectCallback cb = getAllowEnabledObjectCallback(OAObjectCallback.getCallbackOnlyCheckType(), hub, obj, name);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Creates and processes an {@link Type#AllowEnabled} callback.
	 *
	 * @param onlyCheckType optional rules-engine checks to use instead of the type defaults
	 * @param hub Hub context, or {@code null}
	 * @param oaObj callback receiver/target context, or {@code null}
	 * @param name member/property name on {@code oaObj}, or {@code null}
	 * @return processed callback, or {@code null} when no Hub or object is supplied
	 */
	public <T extends OAObject> OAObjectCallback getAllowEnabledObjectCallback(OAObjectCallback.CheckType[] onlyCheckType, final Hub<T> hub, T oaObj, String name) {
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
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled, onlyCheckType, hub, null, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and processes an {@link Type#AllowEnabled} callback for Hub context.
	 *
	 * @param hub Hub context
	 * @return processed callback
	 */
	public OAObjectCallback getAllowEnabledObjectCallback(Hub<? extends OAObject> hub) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled);

		OAObject objMaster = hub.getMasterObject();
		if (objMaster == null) {
			processHubOnlyCallback(objectCallback, hub, null);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		} else {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback.setPropertyName(propertyName);
			objectCallback.setObject(objMaster);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}

	/**
	 * Returns whether an object copy operation is allowed.
	 *
	 * @param oaObj object to copy
	 * @return {@code true} if copying is allowed
	 */
	public boolean getAllowCopy(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		OAObjectCallback cb = getAllowCopyObjectCallback(oaObj);
		return cb == null ? false : cb.getAllowed();
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
	 * Returns whether the specified object can be added to the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if the add operation is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getAllowAddObjectCallback(hub, obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object can be added to a Hub using default checks.
	 *
	 * @param hub Hub receiving the object
	 * @param obj object being added
	 * @return {@code true} if add is allowed
	 */
	public <T extends OAObject> boolean getAllowAdd(Hub<T> hub, T obj) {
		OAObjectCallback cb = getAllowAddObjectCallback(hub, obj);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object can be added while excluding processed-state checks.
	 *
	 * @param hub Hub receiving the object
	 * @param obj object being added
	 * @return {@code true} if add is allowed
	 */
	public <T extends OAObject> boolean getAllowAddIgnoreProcessed(Hub<T> hub, T obj) {
		OAObjectCallback cb = getAllowAddObjectCallback(hub, obj, OAObjectCallback.getAllCheckTypesButProcessed(Type.AllowAdd));
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether adding the specified object to the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub receiving the object
	 * @param obj       the object being added
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyAdd(Hub<T> hub, T obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getVerifyAddObjectCallback(hub, obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether adding an object to a Hub passes verification using default checks.
	 *
	 * @param hub Hub receiving the object
	 * @param obj object being added
	 * @return {@code true} if verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyAdd(Hub<T> hub, T obj) {
		return getVerifyAdd(hub, obj, null);
	}

	/**
	 * Returns whether the specified object can be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param obj       the object being removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if the remove operation is allowed; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getAllowRemoveObjectCallback(hub, obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object can be removed from a Hub using default checks.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if remove is allowed
	 */
	public <T extends OAObject> boolean getAllowRemove(Hub<T> hub, T obj) {
		return getAllowRemove(hub, obj, null);
	}

	/**
	 * Returns whether an object can be removed using object callback methods only.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if callback-only evaluation allows removal
	 */
	public <T extends OAObject> boolean getAllowRemoveCallbackOnly(Hub<T> hub, T obj) {
		return getAllowRemove(hub, obj, OAObjectCallback.getCallbackOnlyCheckType());
	}

	/**
	 * Returns whether an object can be removed while excluding processed-state checks.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if remove is allowed
	 */
	public <T extends OAObject> boolean getAllowRemoveIgnoreProcessed(Hub<T> hub, T obj) {
		return getAllowRemove(hub, obj, OAObjectCallback.getAllCheckTypesButProcessed(Type.AllowRemove));
	}

	/**
	 * Returns whether removing the specified object from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param obj       the object being removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getVerifyRemoveObjectCallback(hub, obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether removing an object from a Hub passes verification using default checks.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyRemove(Hub<T> hub, T obj) {
		return getVerifyRemove(hub, obj, null);
	}

	/**
	 * Returns whether removing an object passes verification using object callback methods only.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if callback-only verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyRemoveCallbackOnly(Hub<T> hub, T obj) {
		return getVerifyRemove(hub, obj, OAObjectCallback.getCallbackOnlyCheckType());
	}

	/**
	 * Returns whether removing an object passes verification while excluding processed-state checks.
	 *
	 * @param hub Hub containing the object
	 * @param obj object being removed
	 * @return {@code true} if verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyRemoveIgnoreProcessed(Hub<T> hub, T obj) {
		return getVerifyRemove(hub, obj, OAObjectCallback.getAllCheckTypesButProcessed(Type.VerifyRemove));
	}

	/**
	 * Returns whether all objects may be removed from the given hub by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose contents may be removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if removing all objects is allowed; otherwise {@code false}
	 */
	public boolean getAllowRemoveAll(Hub<? extends OAObject> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getAllowRemoveAllObjectCallback(hub, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether all objects can be removed from a Hub using default checks.
	 *
	 * @param hub Hub whose objects may be removed
	 * @return {@code true} if remove-all is allowed
	 */
	public boolean getAllowRemoveAll(Hub<? extends OAObject> hub) {
		return getAllowRemoveAll(hub, null);
	}

	/**
	 * Returns whether removing all objects from the given hub passes
	 * verification by evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public boolean getVerifyRemoveAll(Hub<? extends OAObject> hub, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getVerifyRemoveAllObjectCallback(hub, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether removing all objects from a Hub passes verification using default checks.
	 *
	 * @param hub Hub whose objects may be removed
	 * @return {@code true} if verification succeeds
	 */
	public boolean getVerifyRemoveAll(Hub<? extends OAObject> hub) {
		return getVerifyRemoveAll(hub, null);
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
		OAObjectCallback cb = getAllowDeleteObjectCallback(hub, obj);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object can be deleted without Hub context.
	 *
	 * @param obj object to delete
	 * @return {@code true} if delete is allowed
	 */
	public <T extends OAObject> boolean getAllowDelete(T obj) {
		OAObjectCallback cb = getAllowDeleteObjectCallback(obj);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether deleting the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param hub       the hub providing contextual rules
	 * @param obj       the object to delete
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public <T extends OAObject> boolean getVerifyDelete(Hub<T> hub, T obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getVerifyDeleteObjectCallback(hub, obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether deleting an object passes verification using default checks.
	 *
	 * @param hub Hub context, or {@code null}
	 * @param obj object to delete
	 * @return {@code true} if verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyDelete(Hub<T> hub, T obj) {
		return getVerifyDelete(hub, obj, null);
	}

	/**
	 * Returns whether deleting an object passes verification without Hub context.
	 *
	 * @param obj object to delete
	 * @return {@code true} if verification succeeds
	 */
	public <T extends OAObject> boolean getVerifyDelete(T obj) {
		return getVerifyDelete(null, obj, null);
	}

	/**
	 * Returns whether the specified object may be saved by evaluating
	 * the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if saving is allowed; otherwise {@code false}
	 */
	public boolean getAllowSave(OAObject obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getAllowSaveObjectCallback(obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether an object can be saved using default checks.
	 *
	 * @param obj object to save
	 * @return {@code true} if save is allowed
	 */
	public boolean getAllowSave(OAObject obj) {
		return getAllowSave(obj, null);
	}

	/**
	 * Returns whether saving the specified object passes verification by
	 * evaluating the associated {@link OAObjectCallback}.
	 *
	 * @param obj       the object to save
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return {@code true} if verification succeeds; otherwise {@code false}
	 */
	public boolean getVerifySave(OAObject obj, OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback cb = getVerifySaveObjectCallback(obj, onlyCheckTypes);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Returns whether saving an object passes verification using default checks.
	 *
	 * @param obj object to save
	 * @return {@code true} if verification succeeds
	 */
	public boolean getVerifySave(OAObject obj) {
		return getVerifySave(obj, null);
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
	 * Returns whether an object passes submit-time rules.
	 *
	 * @param obj object to submit
	 * @return {@code true} if submit is allowed
	 */
	public boolean getAllowSubmit(OAObject obj) {
		if (obj == null) return false;
		OAObjectCallback cb = getAllowSubmitObjectCallback(obj);
		return cb == null ? false : cb.getAllowed();
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
			final OAObjectCallback emx = new OAObjectCallback(Type.VerifyPropertyChange, null, null, obj, pi.getName(), val);

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
				if (em.getThrowable() == null) {
					em.setThrowable(emx.getThrowable());
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
	public void renderLabel(OAObject obj, String propertyName, OACallbackLabel label) {
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
	public void updateLabel(OAObject obj, String propertyName, OACallbackLabel label) {
		OAObjectCallback em = new OAObjectCallback(Type.UpdateLabel);
		em.setObject(obj);
		em.setPropertyName(propertyName);
		em.setLabel(label);
		callObjectCallbackMethod(em);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowCopy} for the specified object.
	 *
	 * @param oaObj the object being copied
	 * @return the resulting callback
	 */
	public OAObjectCallback getAllowCopyObjectCallback(final OAObject oaObj) {
		if (oaObj == null) return null;
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowCopy, null, null, oaObj, null, null);
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
		if (oaObj == null) return null;
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.GetCopy, null, null, oaObj, null, null);
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
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AfterCopy, null, null, oaObj, null,
				oaObjCopy);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyCommand} to validate invocation of the specified method.
	 *
	 * @param oaObj      the target object
	 * @param methodName the method to verify
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback
	 */
	public OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName, OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (oaObj == null) return null;
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyCommand, onlyCheckTypes, null, null, oaObj, methodName, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and processes a {@link Type#VerifyCommand} callback using default checks.
	 *
	 * @param oaObj callback receiver/target object
	 * @param methodName command or method name
	 * @return processed callback, or {@code null} when {@code oaObj} is {@code null}
	 */
	public OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName) {
		return getVerifyCommandObjectCallback(oaObj, methodName, null);
	}

	/**
	 * Returns whether invoking a command or method passes verification.
	 *
	 * @param oaObj callback receiver/target object
	 * @param methodName command or method name
	 * @return {@code true} if command verification succeeds
	 */
	public boolean getVerifyCommand(final OAObject oaObj, final String methodName) {
		OAObjectCallback cb = getVerifyCommandObjectCallback(oaObj, methodName, null);
		return cb == null ? false : cb.getAllowed();
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowAdd}
	 * to determine whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on the
	 * hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param objAdd    the object being added
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(final Hub<T> hub, T objAdd, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;
		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowAdd, onlyCheckTypes, hub, null, null, null, objAdd);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, objAdd);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, objAdd, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowAdd, onlyCheckTypes, hub, null, objMaster, liRev.getName(), objAdd);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.AllowAdd, onlyCheckTypes, hub, null, null, null, objAdd);
	            processHubOnlyCallback(objectCallback, hub, objAdd);
			    if (objectCallback.getAllowed()) {
			    	processObjectCallbackForHubListeners(objectCallback, hub, objAdd, null, null, null);
			    }
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and processes an {@link Type#AllowAdd} callback using default checks.
	 *
	 * @param hub Hub receiving the object
	 * @param objAdd object being added
	 * @return processed callback, or {@code null} when {@code hub} is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowAddObjectCallback(final Hub<T> hub, T objAdd) {
		return getAllowAddObjectCallback(hub, objAdd, null);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyAdd}
	 * to verify whether the specified object may be added to the given hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * the hub's metadata.
	 *
	 * @param hub       the hub receiving the object
	 * @param oaObj     the object being added
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getVerifyAddObjectCallback(final Hub<T> hub, final T oaObj, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();
		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyAdd, onlyCheckTypes, hub, null, null, null, oaObj);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}			
			processHubOnlyCallback(objectCallback, hub, oaObj);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, oaObj, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyAdd, onlyCheckTypes, hub, null, objMaster, liRev.getName(), oaObj);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.VerifyAdd, onlyCheckTypes, hub, null, null, null, oaObj);
	            processHubOnlyCallback(objectCallback, hub, oaObj);
			    if (objectCallback.getAllowed()) {
			    	processObjectCallbackForHubListeners(objectCallback, hub, oaObj, null, null, null);
			    }
			}
		}
		return objectCallback;
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowNew} to determine
	 * whether a new instance of the specified class may be created. Rules-engine checks are evaluated before returning.
	 *
	 * @param clazz the class to evaluate
	 * @return the resulting callback, or {@code null} if the class is {@code null}
	 */
	public OAObjectCallback getAllowNewObjectCallback(final Class<? extends OAObject> clazz) {
		if (clazz == null) return null;
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowNew, null, clazz, null, null, null);
		processObjectCallback(objectCallback);
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
			processHubOnlyCallback(objectCallback, hub, null);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowNew, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			} else {
				objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
				processHubOnlyCallback(objectCallback, hub, null);
				if (objectCallback.getAllowed()) {
					processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
				}
			}
		}
		return objectCallback;
	}

	/**
	 * Returns whether a new object can be created for a Hub using default checks.
	 *
	 * @param hub Hub context
	 * @return {@code true} if new object creation is allowed
	 */
	public boolean getAllowNewObject(final Hub<? extends OAObject> hub) {
		if (hub == null) return false;
		OAObjectCallback cb = getAllowNewObjectCallback(hub);
		return cb == null ? false : cb.isAllowed();
	}

	/**
	 * Returns whether a new object can be created for a class using default checks.
	 *
	 * @param type object class
	 * @return {@code true} if new object creation is allowed
	 */
	public boolean getAllowNewObject(final Class<? extends OAObject> type) {
		if (type == null) return false;
		OAObjectCallback cb = getAllowNewObjectCallback(type);
		return cb == null ? false : cb.isAllowed();
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code AllowRemove}
	 * to determine whether the specified object may be removed from the hub.
	 * Hub listener rules or reverse-link rules may be applied depending on
	 * metadata.
	 *
	 * @param hub       the hub from which the object may be removed
	 * @param objRemove the object being removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(final Hub<T> hub, final T objRemove, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemove, onlyCheckTypes, hub, null, null, null, objRemove);

			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, objRemove);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, objRemove);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowRemove, onlyCheckTypes, hub, null, objMaster, liRev.getName(), objRemove);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.AllowRemove, onlyCheckTypes, hub, null, null, null, objRemove);
	            processHubOnlyCallback(objectCallback, hub, objRemove);
			    if (objectCallback.getAllowed()) {
			    	processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, objRemove);
			    }
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and processes an {@link Type#AllowRemove} callback using default checks.
	 *
	 * @param hub Hub containing the object
	 * @param objRemove object being removed
	 * @return processed callback, or {@code null} when {@code hub} is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getAllowRemoveObjectCallback(final Hub<T> hub, final T objRemove) {
		return getAllowRemoveObjectCallback(hub, objRemove, null);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type {@code VerifyRemove}
	 * to verify whether the specified object may be removed from the hub. Hub
	 * listener rules or reverse-link rules may be applied depending on metadata.
	 *
	 * @param hub       the hub from which the object is being removed
	 * @param objRemove the object being removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public <T extends OAObject> OAObjectCallback getVerifyRemoveObjectCallback(final Hub<T> hub, final T objRemove, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyRemove, onlyCheckTypes, hub, null, null, null, objRemove);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, objRemove);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, objRemove, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyRemove, onlyCheckTypes, hub, null, objMaster, liRev.getName(), objRemove);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.VerifyRemove, onlyCheckTypes, hub, null, null, null, objRemove);
	            processHubOnlyCallback(objectCallback, hub, objRemove);
			    if (objectCallback.getAllowed()) {
			    	processObjectCallbackForHubListeners(objectCallback, hub, objRemove, null, null, null);
			    }
			}
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
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public OAObjectCallback getAllowRemoveAllObjectCallback(final Hub<? extends OAObject> hub, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemoveAll, onlyCheckTypes, hub, null, null, null, null);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, null);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowRemoveAll, onlyCheckTypes, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.AllowRemoveAll, onlyCheckTypes, hub, null, null, null, null);
	            processHubOnlyCallback(objectCallback, hub, null);
			    if (objectCallback.getAllowed()) {
			    	processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			    }
			}
		}
		return objectCallback;
	}
	/**
	 * Creates and processes an {@link Type#AllowRemoveAll} callback using default checks.
	 *
	 * @param hub Hub whose objects may be removed
	 * @return processed callback, or {@code null} when {@code hub} is {@code null}
	 */
	public OAObjectCallback getAllowRemoveAllObjectCallback(final Hub<? extends OAObject> hub) {
		return getAllowRemoveAllObjectCallback(hub, null);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifyRemoveAll} to verify whether all objects may be removed
	 * from the specified hub. Hub listener rules or reverse-link rules
	 * may be applied depending on metadata.
	 *
	 * @param hub       the hub whose objects may be removed
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback, or {@code null} if the hub is {@code null}
	 */
	public OAObjectCallback getVerifyRemoveAllObjectCallback(final Hub<? extends OAObject> hub, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, onlyCheckTypes, hub, null, null, null, null);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, null);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !li.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, onlyCheckTypes, hub, null, objMaster, liRev.getName(), null);
				processObjectCallback(objectCallback);
			}
			else {
				objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, onlyCheckTypes, hub, null, null, null, null);
				processHubOnlyCallback(objectCallback, hub, null);
				if (objectCallback.getAllowed()) {
					processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
				}
			}
		}
		return objectCallback;
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code AllowSave} to determine whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback
	 */
	public OAObjectCallback getAllowSaveObjectCallback(final OAObject oaObj, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (oaObj == null) return null;
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowSave, onlyCheckTypes, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and processes an {@link Type#AllowSave} callback using default checks.
	 *
	 * @param oaObj object to save
	 * @return processed callback, or {@code null} when {@code oaObj} is {@code null}
	 */
	public OAObjectCallback getAllowSaveObjectCallback(final OAObject oaObj) {
		return getAllowSaveObjectCallback(oaObj, null);
	}

	/**
	 * Creates and evaluates an {@link OAObjectCallback} of type
	 * {@code VerifySave} to verify whether the specified object may
	 * be saved.
	 *
	 * @param oaObj     the object to save
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback
	 */
	public OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		if (oaObj == null) return null;
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifySave, onlyCheckTypes, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Creates and processes a {@link Type#VerifySave} callback using default checks.
	 *
	 * @param oaObj object to save
	 * @return processed callback, or {@code null} when {@code oaObj} is {@code null}
	 */
	public OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj) {
		return getVerifySaveObjectCallback(oaObj, null);
	}

	/**
	 * Creates an {@link OAObjectCallback} of type {@code AllowDelete}
	 * to determine whether the specified object may be deleted. Rules-engine checks are evaluated before returning.
	 *
	 * @param objDelete the object to delete
	 * @return the resulting callback, or {@code null} if the object or its class is null
	 */
	public OAObjectCallback getAllowDeleteObjectCallback(final OAObject objDelete) {
		if (objDelete == null) return null;

		final Class<? extends OAObject> clazz = objDelete.getClass();
		final OAObjectInfo oi = callInfoGetObjectInfo(clazz);

		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowDelete, null, clazz, objDelete);
		processObjectCallback(objectCallback);
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
	public <T extends OAObject> OAObjectCallback getAllowDeleteObjectCallback(final Hub<T> hub, final T objDelete) {
		if (hub == null || objDelete == null) {
			return null;
		}

		OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = getAllowDeleteObjectCallback(objDelete);
			if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
				if (hub.getOAObjectInfo().getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
			processHubOnlyCallback(objectCallback, hub, objDelete);
			if (objectCallback.getAllowed()) {
				processObjectCallbackForHubListeners(objectCallback, hub, objDelete, null, null, null);
			}
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowDelete, hub, null, objMaster, liRev.getName(), objDelete);
				processObjectCallback(objectCallback);
			} else {
				objectCallback = getAllowDeleteObjectCallback(objDelete);
				processHubOnlyCallback(objectCallback, hub, objDelete);
				if (objectCallback.getAllowed()) {
					processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
				}
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
	 * @param onlyCheckTypes optional rules-engine checks to use instead of the type defaults
	 * @return the resulting callback
	 */
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(final Hub<T> hub, final T objDelete, final OAObjectCallback.CheckType[] onlyCheckTypes) {
		OAObjectCallback objectCallback = null;
		if (hub != null) {
			OALinkInfo li = callHubDetailGetLinkInfoFromDetailToMaster(hub);
			OAObject objMaster = hub.getMasterObject();

			if (li == null || (li.getPrivateMethod() && objMaster == null)) {
				objectCallback = new OAObjectCallback(Type.VerifyDelete, onlyCheckTypes, hub, null, null, null, objDelete);
				if (objectCallback.isUsed(OAObjectCallback.CheckType.Processed)) {
					if (hub.getOAObjectInfo().getProcessed()) {
						updateEditProcessed(objectCallback);
					}
				}
				processHubOnlyCallback(objectCallback, hub, objDelete);
				if (objectCallback.getAllowed()) {
					processObjectCallbackForHubListeners(objectCallback, hub, objDelete, null, null, null);
				}
			} else {
				OALinkInfo liRev = li.getReverseLinkInfo();
				if (liRev != null && !li.getCalculated()) {
					objectCallback = new OAObjectCallback(Type.VerifyDelete, onlyCheckTypes, hub, null, objMaster, liRev.getName(), objDelete);
					processObjectCallback(objectCallback);
				}
			}
		}
		if (objectCallback == null) {
			objectCallback = new OAObjectCallback(Type.VerifyDelete, onlyCheckTypes, hub, null, objDelete, null, null);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}

	/**
	 * Creates and processes a {@link Type#VerifyDelete} callback using default checks.
	 *
	 * @param hub Hub context, or {@code null}
	 * @param objDelete object to delete
	 * @return processed callback
	 */
	public <T extends OAObject> OAObjectCallback getVerifyDeleteObjectCallback(final Hub<T> hub, final T objDelete) {
		return getVerifyDeleteObjectCallback(hub, objDelete, null);
	}

	/**
	 * Creates a confirmation {@link OAObjectCallback} for a property change
	 * using an unknown future value. This delegates to
	 * {@code getConfirmPropertyChangeObjectCallback} with the value set to
	 * {@code OAMatchUnknown.instance}.
	 *
	 * @param oaObj          the target object
	 * @param property       the property being changed
	 * @param confirmMessage the message to present for confirmation
	 * @param confirmTitle   the title to present for confirmation
	 * @return the resulting callback
	 */
    public OAObjectCallback getPreConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, 
            String confirmMessage, String confirmTitle) {
        return getConfirmPropertyChangeObjectCallback(oaObj, property, OAMatchUnknown.instance, confirmMessage, confirmTitle);
    }

    /**
     * Creates a confirmation {@link OAObjectCallback} for a property change
     * using an unknown future value. This delegates to
     * {@code getConfirmPropertyChangeObjectCallback} with the value set to
     * {@code OAMatchUnknown.instance}.
     *
     * @param oaObj          the target object
     * @param property       the property being changed
     * @param confirmMessage the message to present for confirmation
     * @param confirmTitle   the title to present for confirmation
     * @return the resulting callback
     */
    public OAObjectCallback getConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, Object newValue,
			String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForPropertyChange, null, null, oaObj, property, newValue);
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
	public OAObjectCallback getConfirmCommandObjectCallback(final OAObject oaObj, String methodName, String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForCommand, null, null, oaObj, methodName, null);
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
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForSave, null, null, oaObj,
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
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForDelete, null, null, oaObj, null, null);
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
	public <T extends OAObject> OAObjectCallback getConfirmRemoveObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage, String confirmTitle) {
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback = new OAObjectCallback(Type.SetConfirmForRemove, hub, null, objMaster, propertyName, oaObj);
		} else {
			objectCallback = new OAObjectCallback(Type.SetConfirmForRemove, hub, null, oaObj, null, null);
		}
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);
		processObjectCallback(objectCallback);
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
    public OAObjectCallback getConfirmRemoveAllObjectCallback(final Hub<? extends OAObject> hub, String confirmMessage, String confirmTitle) {
        OAObjectCallback objectCallback;
        OAObject objMaster = hub.getMasterObject();
        if (objMaster != null) {
            String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
            objectCallback = new OAObjectCallback(Type.SetConfirmForRemoveAll, hub, null, null, propertyName, null);
        } else {
            objectCallback = new OAObjectCallback(Type.SetConfirmForRemoveAll, hub, null, null, null, null);
        }
        objectCallback.setConfirmMessage(confirmMessage);
        objectCallback.setConfirmTitle(confirmTitle);
        processObjectCallback(objectCallback);
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
	public <T extends OAObject> OAObjectCallback getConfirmAddObjectCallback(final Hub<T> hub, final T oaObj, String confirmMessage, String confirmTitle) {
		if (hub == null) return null;
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = callHubDetailGetPropertyFromMasterToDetail(hub);
			objectCallback = new OAObjectCallback(Type.SetConfirmForAdd, (CheckType[]) null, hub, null, objMaster, propertyName, oaObj);
		} else {
			objectCallback = new OAObjectCallback(Type.SetConfirmForAdd, hub, null, null, null, oaObj);
		}
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	/**
	 * Processes an {@link OAObjectCallback} through the OA object rules engine.
	 * <p>
	 * The callback {@link Type} supplies the semantic question and its active
	 * {@link CheckType} stages. The pipeline evaluates session rules, metadata and
	 * object state, object callback methods, Hub listeners, then SuperAdmin override
	 * when enabled. Object callback methods and Hub listeners may intentionally
	 * refine or override earlier allowed/response/throwable results.
	 * </p>
	 *
	 * @param cb callback request/response carrier to process
	 */
	protected void processObjectCallback(OAObjectCallback cb) {
		if (cb == null) return;
		_processObjectCallback(cb);

		if ((!cb.getAllowed() || cb.getThrowable() != null) && cb.isUsed(CheckType.SuperAdminOverride)) {
			Class<? extends OAObject> c = cb.getCalcClass();
			if (c != null) {
				OA oa = OARuntime.oa(c);
			    if (oa.modelUser().isSuperAdmin()) {
			    	cb.setThrowable(null);
			    	cb.setAllowed(true);
				}
			}
		}
	}

	private class ProcessInfo {
		final OAObjectCallback cb;
		final Hub hubThis;
		final Class<?> clazz;
		final OAObject oaObj;
		final String propertyName;
		final Object oldValue;
		final Object value;
		final Type type;

		final OAObjectInfo oi;
		final OA oa;
		final Hub<?> hubModelUser;
		final OASessionUser sessionUser;
		final OASessionAccess sessionAccess;

		public ProcessInfo(final OAObjectCallback cb) {
			this.cb = cb;
			hubThis = cb.getHub();
			clazz = cb.getCalcClass();
			oaObj = cb.getObject();
			propertyName = cb.getPropertyName();
			oldValue = cb.getOldValue();
			value = cb.getValue();
			type = cb.getType();

			oi = callInfoGetObjectInfo(cb.getCalcClass());
			oa = OARuntime.oa(clazz);
			hubModelUser = oa.modelUser().getCalc();
			sessionUser = oa.sessionUser().get();
			sessionAccess = (sessionUser == null) ? null : sessionUser.getSessionAccess();
		}
	}

	/**
	 * Runs the main rules pipeline for a callback request.
	 * <p>
	 * Session checks run first, followed by metadata/object-state checks, object
	 * callback methods, and Hub listeners. SuperAdmin override is applied by
	 * {@link #processObjectCallback(OAObjectCallback)} after this method returns.
	 * </p>
	 *
	 * @param cb callback request/response carrier
	 */
	protected void _processObjectCallback(final OAObjectCallback cb) {
		ProcessInfo pi = new ProcessInfo(cb);

		_processObjectCallback_1A(pi);
		if (cb.isAllowed()) {
			_processObjectCallback_1B(pi);
		}

		_processObjectCallback_2(pi); // callback

		_processObjectCallback_3(pi); // hublisteners
	}

	/**
	 * Applies session access checks for the callback when the corresponding
	 * {@link CheckType#SessionEnabled} or {@link CheckType#SessionVisible} stage is
	 * active.
	 *
	 * @param pi resolved processing context
	 */
	protected void _processObjectCallback_1A(ProcessInfo pi) {
		OAObjectCallback cb = pi.cb;		

		if (pi.sessionAccess == null) return;
		boolean bx = true;
		// checkSessionEnabled
		if (cb.isUsed(CheckType.SessionEnabled)) {
			if (pi.oaObj != null) {
				bx = pi.sessionAccess.getEnabled(pi.oaObj, pi.propertyName);
			} else {
				bx = pi.sessionAccess.getEnabled(pi.clazz, pi.propertyName);
			}
		}

		// checkSessionVisible
		if (bx && cb.isUsed(CheckType.SessionVisible)) {
			if (pi.oaObj != null) {
				bx = pi.sessionAccess.getVisible(pi.oaObj, pi.propertyName);
			} else {
				bx = pi.sessionAccess.getVisible(pi.clazz, pi.propertyName);
			}
		}
		if (!bx) {
			cb.setAllowed(false);
			cb.setResponse("SessionAccess returned false");
		}
	}

	/**
	 * Applies owner, processed, metadata, and ModelUser checks for the callback.
	 * <p>
	 * Each stage runs only when enabled by {@link OAObjectCallback#isUsed(CheckType)}.
	 * This phase can deny the callback, but later object callback and Hub listener
	 * phases may intentionally refine or override the result.
	 * </p>
	 *
	 * @param pi resolved processing context
	 */
	protected void _processObjectCallback_1B(ProcessInfo pi) {
		final OAObjectCallback cb = pi.cb;		
		// checkOwner
		if (cb.isUsed(CheckType.Owner)) {
			if (pi.oaObj != null) {
				ownerHierProcess(cb, pi.oaObj, pi.propertyName);
			}
			if (pi.hubThis != null) {
				OALinkInfo li = callHubDetailGetLinkInfoFromMasterHubToDetail(pi.hubThis);
				if (li != null && li.getOwner()) {
					final OAObject objMaster = pi.hubThis.getMasterObject();
					if (objMaster != null) {
						if (pi.type == Type.AllowVisible) {
							OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowVisible, pi.cb.getCheckTypes() , pi.hubThis.getMasterHub(), null, objMaster, li.getName(), null);
							objectCallbackX.setAllowed(cb.getAllowed());
							_processObjectCallback(objectCallbackX);
							cb.setAllowed(objectCallbackX.getAllowed());
							if (OAString.isEmpty(cb.getResponse())) {
								cb.setResponse(objectCallbackX.getResponse());
							}
							if (cb.getThrowable() == null) {
								cb.setThrowable(objectCallbackX.getThrowable());
							}
						}
						else {
							CheckType[] cts = cb.getCheckTypesExcept(CheckType.Owner, CheckType.Processed);
							OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, cts, pi.hubThis.getMasterHub(), null, objMaster, li.getName(), null);
							objectCallbackX.setAllowed(cb.getAllowed());
							_processObjectCallback(objectCallbackX);
							cb.setAllowed(objectCallbackX.getAllowed());
							if (OAString.isEmpty(cb.getResponse())) {
								cb.setResponse(objectCallbackX.getResponse());
							}
							if (cb.getThrowable() == null) {
								cb.setThrowable(objectCallbackX.getThrowable());
							}
						}
					}
				}
			}
		}		
		if (!cb.isAllowed()) return;

		// checkProcessed
		if (cb.isUsed(CheckType.Processed)) {
			if (pi.oi.getProcessed()) {
				updateEditProcessed(cb);
			}
		}
		if (!cb.isAllowed()) return;


		// checkEnabled
		if (cb.isUsed(CheckType.Enabled)) {
			String enabledName = null;
			boolean enabledValue = true;

			enabledName = pi.oi.getEnabledProperty();
			if (OAStr.isNotEmpty(enabledName)) {
				enabledValue = pi.oi.getEnabledValue();
				evaluateObject(cb, enabledName, enabledValue);
				if (!cb.isAllowed()) return;
			}

			if (pi.oaObj != null && OAString.isNotEmpty(pi.propertyName)) {
				OAPropertyInfo pix = pi.oi.getPropertyInfo(pi.propertyName);
				boolean bIsProcessed = false;
				if (pix != null) {
					enabledName = pix.getEnabledProperty();
					enabledValue = pix.getEnabledValue();
					bIsProcessed = pix.getProcessed();
				} else {
					OALinkInfo li = pi.oi.getLinkInfo(pi.propertyName);
					if (li != null) {
						enabledName = li.getEnabledProperty();
						enabledValue = li.getEnabledValue();
						bIsProcessed = li.getProcessed();
					} else {
						OACalcInfo ci = pi.oi.getCalcInfo(pi.propertyName);
						if (ci != null) {
							enabledName = ci.getEnabledProperty();
							enabledValue = ci.getEnabledValue();
						} else {
							OAMethodInfo mi = pi.oi.getMethodInfo(pi.propertyName);
							if (mi != null) {
								enabledName = mi.getEnabledProperty();
								enabledValue = mi.getEnabledValue();
							}
						}
					}
				}
				if (OAStr.isNotEmpty(enabledName)) {
					evaluateObject(cb, enabledName, enabledValue);
					if (bIsProcessed && cb.isUsed(CheckType.Processed)) {
						updateEditProcessed(cb);
					}
				}

				if (!cb.isAllowed()) return;
			}		
		}

		// checkUserEnabled
		if (cb.isUsed(CheckType.UserEnabled)) {
			String enabledName = null;
			boolean enabledValue = true;

			enabledName = pi.oi.getModelUserEnabledProperty();
			if (OAStr.isNotEmpty(enabledName)) {
				enabledValue = pi.oi.getModelUserEnabledValue();
				evaluateUser(cb, enabledName, enabledValue);
				if (!cb.isAllowed()) return;
			}

			if (pi.oaObj != null && OAString.isNotEmpty(pi.propertyName)) {
				OAPropertyInfo pix = pi.oi.getPropertyInfo(pi.propertyName);
				boolean bIsProcessed = false;
				if (pix != null) {
					enabledName = pix.getModelUserEnabledProperty();
					enabledValue = pix.getModelUserEnabledValue();
					bIsProcessed = pix.getProcessed();
				} else {
					OALinkInfo li = pi.oi.getLinkInfo(pi.propertyName);
					if (li != null) {
						enabledName = li.getModelUserEnabledProperty();
						enabledValue = li.getModelUserEnabledValue();
						bIsProcessed = li.getProcessed();
					} else {
						OACalcInfo ci = pi.oi.getCalcInfo(pi.propertyName);
						if (ci != null) {
							enabledName = ci.getModelUserEnabledProperty();
							enabledValue = ci.getModelUserEnabledValue();
						} else {
							OAMethodInfo mi = pi.oi.getMethodInfo(pi.propertyName);
							if (mi != null) {
								enabledName = mi.getModelUserEnabledProperty();
								enabledValue = mi.getModelUserEnabledValue();
							}
						}
					}
				}
				if (OAStr.isNotEmpty(enabledName)) {
					evaluateUser(cb, enabledName, enabledValue);
				}
				if (!cb.isAllowed()) return;

				if (bIsProcessed && cb.isUsed(CheckType.Processed)) {
					updateEditProcessed(cb);
				}
				if (!cb.isAllowed()) return;
			}		
		}

		// checkVisible
		if (cb.isUsed(CheckType.Visible)) {
			String visibleName = null;
			boolean visibleValue = true;

			visibleName = pi.oi.getVisibleProperty();
			if (OAStr.isNotEmpty(visibleName)) {
				visibleValue = pi.oi.getVisibleValue();
				evaluateObject(cb, visibleName, visibleValue);
				if (!cb.isAllowed()) return;
			}

			if (pi.oaObj != null && OAString.isNotEmpty(pi.propertyName)) {
				OAPropertyInfo pix = pi.oi.getPropertyInfo(pi.propertyName);
				boolean bIsProcessed = false;
				if (pix != null) {
					visibleName = pix.getVisibleProperty();
					visibleValue = pix.getVisibleValue();
					bIsProcessed = pix.getProcessed();
				} else {
					OALinkInfo li = pi.oi.getLinkInfo(pi.propertyName);
					if (li != null) {
						visibleName = li.getVisibleProperty();
						visibleValue = li.getVisibleValue();
						bIsProcessed = li.getProcessed();
					} else {
						OACalcInfo ci = pi.oi.getCalcInfo(pi.propertyName);
						if (ci != null) {
							visibleName = ci.getVisibleProperty();
							visibleValue = ci.getVisibleValue();
						} else {
							OAMethodInfo mi = pi.oi.getMethodInfo(pi.propertyName);
							if (mi != null) {
								visibleName = mi.getVisibleProperty();
								visibleValue = mi.getVisibleValue();
							}
						}
					}
				}
				if (OAStr.isNotEmpty(visibleName)) {
					evaluateObject(cb, visibleName, visibleValue);
				}
				if (!cb.isAllowed()) return;

				if (bIsProcessed && cb.isUsed(CheckType.Processed)) {
					updateEditProcessed(cb);
				}
				if (!cb.isAllowed()) return;
			}		
		}

		// checkUserVisible
		if (cb.isUsed(CheckType.UserVisible)) {
			String visibleName = null;
			boolean visibleValue = true;

			visibleName = pi.oi.getModelUserVisibleProperty();
			if (OAStr.isNotEmpty(visibleName)) {
				visibleValue = pi.oi.getModelUserVisibleValue();
				evaluateUser(cb, visibleName, visibleValue);
				if (!cb.isAllowed()) return;
			}

			if (pi.oaObj != null && OAString.isNotEmpty(pi.propertyName)) {
				OAPropertyInfo pix = pi.oi.getPropertyInfo(pi.propertyName);
				boolean bIsProcessed = false;
				if (pix != null) {
					visibleName = pix.getModelUserVisibleProperty();
					visibleValue = pix.getModelUserVisibleValue();
					bIsProcessed = pix.getProcessed();
				} else {
					OALinkInfo li = pi.oi.getLinkInfo(pi.propertyName);
					if (li != null) {
						visibleName = li.getModelUserVisibleProperty();
						visibleValue = li.getModelUserVisibleValue();
						bIsProcessed = li.getProcessed();
					} else {
						OACalcInfo ci = pi.oi.getCalcInfo(pi.propertyName);
						if (ci != null) {
							visibleName = ci.getModelUserVisibleProperty();
							visibleValue = ci.getModelUserVisibleValue();
						} else {
							OAMethodInfo mi = pi.oi.getMethodInfo(pi.propertyName);
							if (mi != null) {
								visibleName = mi.getModelUserVisibleProperty();
								visibleValue = mi.getModelUserVisibleValue();
							}
						}
					}
				}
				if (OAStr.isNotEmpty(visibleName)) {
					evaluateUser(cb, visibleName, visibleValue);
				}
				if (!cb.isAllowed()) return;

				if (bIsProcessed && cb.isUsed(CheckType.Processed)) {
					updateEditProcessed(cb);
				}
				if (!cb.isAllowed()) return;
			}		
		}
	}

	/**
	 * Invokes the object callback method stage when {@link CheckType#CallbackMethod}
	 * is active.
	 * <p>
	 * Object/model callback code is allowed to refine or override earlier metadata,
	 * session, or ModelUser results.
	 * </p>
	 *
	 * @param pi resolved processing context
	 */
	protected void _processObjectCallback_2(ProcessInfo pi) {
		if (pi.oaObj == null) return;
		OAObjectCallback cb = pi.cb;		

		if (cb.isUsed(CheckType.CallbackMethod)) {
			callObjectCallbackMethod(cb);
		}
	}

	/**
	 * Invokes Hub listener participation when {@link CheckType#HubListeners} is
	 * active.
	 * <p>
	 * Hub listeners run after object callback methods and may refine or override
	 * earlier rule results.
	 * </p>
	 *
	 * @param pi resolved processing context
	 */
	protected void _processObjectCallback_3(ProcessInfo pi) {
		if (pi.oaObj == null) return;
		OAObjectCallback cb = pi.cb;		
		final Hub[] hubs = callHubGetHubReferences(pi.oaObj);

		// checkHubListeners
		if (hubs != null && cb.isUsed(CheckType.HubListeners)) { 
			for (Hub h : hubs) {
				if (h == null) {
					continue;
				}
				processObjectCallbackForHubListeners(cb, h, pi.oaObj, pi.propertyName, pi.oldValue, pi.value);
			}
		}
	}

	/**
	 * Applies the lightweight owner-hierarchy gate for a callback.
	 * <p>
	 * Owner hierarchy processing checks whether the containing owner path is visible
	 * or enabled enough for the original callback to continue. It does not re-run the
	 * full original rule type against each owner. {@link Type#AllowVisible} maps to
	 * owner visibility checks; other rule types map to owner enabled checks.
	 * </p>
	 *
	 * @param objectCallback callback request/response carrier being updated
	 * @param oaObj          target object whose owner chain is checked
	 * @param propertyName   member/property associated with the original callback
	 */
	protected void ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName) {
		_ownerHierProcess(objectCallback, oaObj, propertyName, null, 0);
	}

	/**
	 * Recursively applies owner visible/enabled checks from the top owner back down
	 * to the supplied object.
	 * <p>
	 * This is part of the owner gate only. It uses {@link Type#AllowVisible} owner
	 * semantics for visible requests and {@link Type#AllowEnabled} owner semantics
	 * for other requests, while preserving the active {@link CheckType} selection.
	 * </p>
	 *
	 * @param objectCallback callback request/response carrier being updated
	 * @param oaObj          current object in the owner chain
	 * @param propertyName   member/property associated with the current owner step
	 * @param li             link used while navigating owner hierarchy
	 * @param cnter          recursion depth counter
	 */
	protected void _ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName, final OALinkInfo li, final int cnter) {
		if (oaObj == null) return;
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
		boolean bWasAllowed = objectCallback.getAllowed();

		final OA oa = OARuntime.oa(oaObj);
		final Hub<?> hub = oa.modelUser().getCalc();
		final OAObject user = hub == null ? null : hub.getAO();

		// check class level @OAObjCallback annotation
		if (objectCallback.getType() == Type.AllowVisible) {
			pp = oi.getVisibleProperty();
			if (bWasAllowed && OAString.isNotEmpty(pp)) {
				b = oi.getVisibleValue();
				valx = callReflectGetProperty(oaObj, pp);
				bWasAllowed = (b == OAConv.toBoolean(valx));
				if (!bWasAllowed) {
					objectCallback.setAllowed(false);
					String s = "Not visible, rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + ", " + pp + " != " + b;
					objectCallback.setResponse(s);
				}
			}
			pp = oi.getModelUserVisibleProperty();
			if (bWasAllowed && OAString.isNotEmpty(pp) && objectCallback.isUsed(CheckType.UserVisible)) {
				b = oi.getModelUserVisibleValue();

				if (user == null) {
					if (callSyncIsClient()) {
						bWasAllowed = false;
					}
				} else {
					valx = callReflectGetProperty(user, pp);
					bWasAllowed = (b == OAConv.toBoolean(valx));
				}
				if (!bWasAllowed) {
					objectCallback.setAllowed(false);
					String s = "Not visible, user rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + ", ";
					if (user == null) {
						s = "ModelUser is null";
					} else {
						s = "User." + pp + " != " + b;
					}
					objectCallback.setResponse(s);
				}
			}

			if (objectCallback.isUsed(CheckType.CallbackMethod)) {
				// this can overwrite objectCallback.allowed
				callObjectCallbackMethod(oaObj, null, objectCallback);
				bWasAllowed = objectCallback.getAllowed();
				if (!bWasAllowed && OAString.isEmpty(objectCallback.getResponse())) {
					String s = "Not visible, edit query for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + " allowVisible returned false";
					objectCallback.setResponse(s);
				}
			}

			if (bWasAllowed && li != null && objectCallback.isUsed(CheckType.Visible)) {
				pp = li.getVisibleProperty();
				if (OAString.isNotEmpty(pp)) {
					b = li.getVisibleValue();
					valx = callReflectGetProperty(oaObj, pp);
					bWasAllowed = (b == OAConv.toBoolean(valx));
					if (!bWasAllowed) {
						objectCallback.setAllowed(false);
						String s = "Not visible, rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + "." + propertyName + ", " + pp + " != "
								+ b;
						objectCallback.setResponse(s);
					}
				}
			}
			if (bWasAllowed && li != null && objectCallback.isUsed(CheckType.UserVisible)) {
				pp = li.getModelUserVisibleProperty();
				if (OAString.isNotEmpty(pp)) {
					b = li.getModelUserVisibleValue();
					if (user == null) {
						if (callSyncIsClient()) {
							bWasAllowed = false;
						}
					} else {
						valx = callReflectGetProperty(user, pp);
						bWasAllowed = (b == OAConv.toBoolean(valx));
					}
					if (!bWasAllowed) {
						objectCallback.setAllowed(false);
						String s = "Not visible, user rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + "." + propertyName + ", ";
						if (user == null) {
							s = "ModelUser is null";
						} else {
							s = "User." + pp + " must be " + b;
						}
						objectCallback.setResponse(s);
					}
				}
			}

			// this can overwrite objectCallback.allowed
			if (li != null && OAString.isNotEmpty(propertyName)) {
				if (objectCallback.isUsed(CheckType.CallbackMethod)) {
					callObjectCallbackMethod(oaObj, propertyName, objectCallback);
					bWasAllowed = objectCallback.getAllowed();
					if (!bWasAllowed && OAString.isEmpty(objectCallback.getResponse())) {
						String s = "Not visible, edit query for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + "." + propertyName
								+ " allowVisible returned false";
						objectCallback.setResponse(s);
					}
				}
			}
		} else if (objectCallback.isUsed(CheckType.Enabled)) {
			final boolean bCheckEnabledProperty = objectCallback.isUsed(OAObjectCallback.CheckType.Enabled);
			final boolean bCheckUserEnabledProperty = objectCallback.isUsed(OAObjectCallback.CheckType.UserEnabled);
			final boolean bCheckCallbackMethod = objectCallback.isUsed(OAObjectCallback.CheckType.CallbackMethod);

			if (bWasAllowed) {
				pp = oi.getEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
					b = oi.getEnabledValue();
					valx = callReflectGetProperty(oaObj, pp);
					bWasAllowed = (b == OAConv.toBoolean(valx));
					if (!bWasAllowed) {
						objectCallback.setAllowed(false);
						String s = "Not enabled, rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + ", " + pp + " != " + b;
						objectCallback.setResponse(s);
					}
				}
			}
			pp = oi.getModelUserEnabledProperty();
			if (bWasAllowed && OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
				evaluateUser(objectCallback, pp, oi.getModelUserEnabledValue());
			}

			// this can overwrite objectCallback.allowed
			if (bCheckCallbackMethod) {

				OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, null, objectCallback);

				if (objectCallback.isUsed(CheckType.CallbackMethod)) {
					callObjectCallbackMethod(oaObj, null, objectCallbackX);
					bWasAllowed = objectCallbackX.getAllowed();
					objectCallback.setAllowed(bWasAllowed);
					if (!bWasAllowed && OAString.isEmpty(objectCallback.getResponse())) {
						String s = "Not enabled, edit query for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + " allowEnabled returned false";
						objectCallback.setResponse(s);
					}
				}
			}

			if (li != null && bWasAllowed) {
				pp = li.getEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
					b = li.getEnabledValue();
					valx = callReflectGetProperty(oaObj, pp);
					bWasAllowed = (b == OAConv.toBoolean(valx));
					if (!bWasAllowed) {
						objectCallback.setAllowed(false);
						String s = "Not enabled, rule for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + "." + propertyName + ", " + pp + " != "
								+ b;
						objectCallback.setResponse(s);
					}
				}
			}

			if (li != null && bWasAllowed) {
				pp = li.getModelUserEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckUserEnabledProperty) {
					evaluateUser(objectCallback, pp, li.getModelUserEnabledValue());
				}
			}

			// this can overwrite objectCallback.allowed
			if (bCheckCallbackMethod && li != null && OAString.isNotEmpty(propertyName)) {
				OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled, null, objectCallback);
				if (objectCallback.isUsed(CheckType.CallbackMethod)) {
					callObjectCallbackMethod(oaObj, propertyName, objectCallbackX);
					bWasAllowed = objectCallbackX.getAllowed();
					objectCallback.setAllowed(bWasAllowed);
					if (!bWasAllowed && OAString.isEmpty(objectCallback.getResponse())) {
						String s = "Not enabled, edit query for " + (hub == null ? "Unknown" :hub.getObjectClass().getSimpleName()) + "." + propertyName
								+ " allowEnabled returned false";
						objectCallback.setResponse(s);
					}
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
		if (objectCallback.isUsed(CheckType.Enabled)) {
			OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled);
			objectCallbackX.setAllowed(objectCallback.getAllowed());
			objectCallbackX.setPropertyName(objectCallback.getPropertyName());
			_processObjectCallbackForHubListeners(objectCallbackX, hub, oaObj, propertyName, oldValue, newValue);
			objectCallback.setAllowed(objectCallbackX.getAllowed());
			if (OAString.isEmpty(objectCallback.getResponse())) {
				objectCallback.setResponse(objectCallbackX.getResponse());
			}
			if (objectCallback.getThrowable() == null) {
				objectCallback.setThrowable(objectCallbackX.getThrowable());
			}
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
				s = objectCallback.getType() + " failed for " + (oaObj == null ? "Unknown" : oaObj.getClass().getSimpleName()) + "." + propertyName;
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
	public <T extends OAObject> void addObjectCallbackChangeListeners(
		final Hub<T> hub, final Class<T> cz, final String prop, String ppPrefix,
		final HubChangeListener changeListener, final boolean bEnabled) 
	{
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
		addDependentProps(	
			hub, ppPrefix,
			bEnabled ? null : oi.getViewDependentProperties(),
			bEnabled ? oi.getModelUserDependentProperties() : null,
			(OAString.isEmpty(prop) && oi.getProcessed()),
			changeListener
		);


		final OA oa = OARuntime.oa(cz);
		final Hub hubUser =  oa.modelUser().getCalc();

		if (bEnabled) {
			s = oi.getModelUserEnabledProperty();
		} else {
			s = oi.getModelUserVisibleProperty();
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
			addDependentProps(	hub, ppPrefix, pi.getViewDependentProperties(), pi.getModelUserDependentProperties(),
								(bEnabled && pi.getProcessed()), changeListener);

			if (bEnabled) {
				s = pi.getModelUserEnabledProperty();
			} else {
				s = pi.getModelUserVisibleProperty();
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
				addDependentProps(	hub, ppPrefix, li.getViewDependentProperties(), li.getModelUserDependentProperties(),
									(bEnabled && li.getProcessed()), changeListener);

				if (bEnabled) {
					s = li.getModelUserEnabledProperty();
				} else {
					s = li.getModelUserVisibleProperty();
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
					addDependentProps(	hub, ppPrefix, ci.getViewDependentProperties(), ci.getModelUserDependentProperties(), false,
										changeListener);

					if (bEnabled) {
						s = ci.getModelUserEnabledProperty();
					} else {
						s = ci.getModelUserVisibleProperty();
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
						addDependentProps(	hub, ppPrefix, mi.getViewDependentProperties(), mi.getModelUserDependentProperties(), false,
											changeListener);

						if (bEnabled) {
							s = mi.getModelUserEnabledProperty();
						} else {
							s = mi.getModelUserVisibleProperty();
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
	 * View-level, ModelUser-level, and processed-dependent properties are added
	 * using the provided prefix. Null or empty property arrays are ignored.
	 *
	 * @param hub                       the hub whose objects are monitored
	 * @param prefix                    optional property-path prefix
	 * @param viewDependentProperties   properties affecting visibility
	 * @param userModelDependentProperties properties affecting ModelUser visibility or enabled state
	 * @param bProcessed                true to include processed-dependent properties
	 * @param changeListener            the listener that receives dependent paths
	 */
	protected void addDependentProps(
		final Hub<? extends OAObject> hub, String prefix, 
		String[] viewDependentProperties, String[] userModelDependentProperties,
		boolean bProcessed, HubChangeListener changeListener) 
	{

		final OA oa = OARuntime.oa(hub);
		if (viewDependentProperties != null) {
			for (String s : viewDependentProperties) {
				changeListener.add(hub, prefix + s);
			}
		}
		if (userModelDependentProperties != null) {
			Hub hubUser = oa.modelUser().getCalc();
			if (userModelDependentProperties.length > 0 && hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			for (String s : userModelDependentProperties) {
				changeListener.add(hubUser, s);
			}
		}
		if (bProcessed) {
			Hub hubUser = oa.modelUser().getCalc();
			if (hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			changeListener.add(hubUser, oa.modelUser().getEditProcessedPropertyName());
		}
	}

	/**
	 * Applies the ModelUser edit-processed rule to a callback when processed-state checks require it.
	 *
	 * @param objectCallback callback request/response carrier being updated
	 */
	public void updateEditProcessed(OAObjectCallback objectCallback) {
		if (objectCallback == null) return;

		Class<? extends OAObject> c = objectCallback.getCalcClass();
		OA oa = OARuntime.oa(c);

		evaluateUser(objectCallback, oa.modelUser().getEditProcessedPropertyName(), true);
	}

	protected boolean evaluateUser(OAObjectCallback objectCallback, final String path, final boolean bMatchValue) {
		if (objectCallback == null) return false;
		if (OAStr.isEmpty(path)) return true;

		final OA oa = OARuntime.oa(objectCallback.getCalcClass());
		final Hub hub = oa.modelUser().getCalc();

		if (hub == null) {
			objectCallback.setAllowed(false);
			String s = "ModelUser Hub is null";
			objectCallback.setResponse(s);
			return false;
		}

		boolean b = false;
		OAObject objUser = hub.getAO();
		if (objUser != null) {
			Object val = objUser.getProperty(path);
			b = (OAConv.toBoolean(val) == bMatchValue);
		}

		if (b || objectCallback == null) return b;

		objectCallback.setAllowed(false);
		Class clazz = objectCallback.getCalcClass();

		String s = "Type is "+ objectCallback.getType().name() +", user rule for ";

		if (clazz == null) s += "UnknownClass"; 
		else s += clazz.getSimpleName();
		s += ", ";

		if (objUser == null) s += "ModelUser returned null";
		else s += objUser.getClass().getSimpleName() + "." + path + " must be " + bMatchValue;
		objectCallback.setResponse(s);

		return b;
	}

	protected boolean evaluateObject(OAObjectCallback objectCallback, final String path, final boolean bMatchValue) {
		if (objectCallback == null) return false;
		if (OAStr.isEmpty(path)) return true;

		final OAObject obj = objectCallback.getObject(); 

		if (obj == null) {
			objectCallback.setAllowed(false);
			String s = "Object is null";
			objectCallback.setResponse(s);
			return false;
		}

		boolean b = false;
		if (obj != null) {
			Object val = obj.getProperty(path);
			b = (OAConv.toBoolean(val) == bMatchValue);
		}

		if (b) return b;

		objectCallback.setAllowed(false);
		Class clazz = objectCallback.getCalcClass();

		String s = "Type is "+ objectCallback.getType().name() +", rule for ";

		if (clazz == null) s += "UnknownClass"; 
		else s += clazz.getSimpleName();
		s += "." + path + " must be " + bMatchValue;
		objectCallback.setResponse(s);

		return b;
	}

	protected void processHubOnlyCallback(OAObjectCallback cb, Hub<? extends OAObject> hub, OAObject valueObj) {
	    if (cb == null || hub == null || !cb.getAllowed()) return;

	    Class<? extends OAObject> clazz = hub.getObjectClass();
	    if (clazz == null) return;

	    OA oa = OARuntime.oa(clazz);
	    OASessionUser sessionUser = oa.sessionUser().get();
	    OASessionAccess sa = sessionUser == null ? null : sessionUser.getSessionAccess();

	    if (sa != null) {
		    if (cb.isUsed(CheckType.SessionEnabled)) {
		        boolean ok = sa.getEnabled(clazz);
		        if (ok && valueObj != null) ok = sa.getEnabled(valueObj);

		        if (!ok) {
		            cb.setAllowed(false);
		            cb.setResponse("SessionAccess returned false");
		            return;
		        }
		    }
		    if (cb.isUsed(CheckType.SessionVisible)) {
		        boolean ok = sa.getVisible(clazz);
		        if (ok && valueObj != null) ok = sa.getVisible(valueObj);

		        if (!ok) {
		            cb.setAllowed(false);
		            cb.setResponse("SessionAccess returned false");
		            return;
		        }
		    }
	    }

		if (cb.isUsed(OAObjectCallback.CheckType.UserEnabled)) {
	        OAObjectInfo oi = callInfoGetObjectInfo(clazz);
	        evaluateUser(cb, oi.getModelUserEnabledProperty(), oi.getModelUserEnabledValue());
	    }
	}

	protected void processClassOnlyCallback(final OAObjectCallback cb, final Class<? extends OAObject> clazz, OAObject valueObj) {
	    if (cb == null || clazz == null || !cb.getAllowed()) return;

	    OA oa = OARuntime.oa(clazz);
	    OASessionUser sessionUser = oa.sessionUser().get();
	    OASessionAccess sa = sessionUser == null ? null : sessionUser.getSessionAccess();

	    if (sa != null) {
		    if (cb.isUsed(CheckType.SessionEnabled)) {
		        boolean ok = sa.getEnabled(clazz);
		        if (ok && valueObj != null) ok = sa.getEnabled(valueObj);

		        if (!ok) {
		            cb.setAllowed(false);
		            cb.setResponse("SessionAccess returned false");
		            return;
		        }
		    }
		    if (cb.isUsed(CheckType.SessionVisible)) {
		        boolean ok = sa.getVisible(clazz);
		        if (ok && valueObj != null) ok = sa.getVisible(valueObj);

		        if (!ok) {
		            cb.setAllowed(false);
		            cb.setResponse("SessionAccess returned false");
		            return;
		        }
		    }
	    }

		if (cb.isUsed(OAObjectCallback.CheckType.UserEnabled)) {
	        OAObjectInfo oi = callInfoGetObjectInfo(clazz);
	        evaluateUser(cb, oi.getModelUserEnabledProperty(), oi.getModelUserEnabledValue());
	    }
	}

	/**
	 * Resolves OA metadata for a model class.
	 *
	 * @param clazz model class
	 * @return object metadata
	 */
	public abstract OAObjectInfo callInfoGetObjectInfo(Class<?> clazz);	
	/**
	 * Reads a direct property value from an OAObject.
	 *
	 * @param oaObj source object
	 * @param propertyName property name
	 * @return property value
	 */
	public abstract Object callPropertyGetProperty(OAObject oaObj, String propertyName);
	/**
	 * Reads a property-path value from an OAObject.
	 *
	 * @param oaObj source object
	 * @param propPath property path
	 * @return resolved value
	 */
	public abstract Object callReflectGetProperty(OAObject oaObj, String propPath);
	/**
	 * Returns Hubs that reference an object and can provide Hub listener context.
	 *
	 * @param oaObj object whose Hub references are requested
	 * @return Hub references, or {@code null}
	 */
	public abstract <T extends OAObject> Hub<T>[] callHubGetHubReferences(T oaObj);	
	/**
	 * Resolves a metadata method by name and parameter class.
	 *
	 * @param oi object metadata
	 * @param methodName method name
	 * @param classParam expected parameter class
	 * @return matching method, or {@code null}
	 */
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, final Class<?> classParam);	
	/**
	 * Resolves a metadata method by name and argument count.
	 *
	 * @param oi object metadata
	 * @param methodName method name
	 * @param argumentCount expected argument count
	 * @return matching method, or {@code null}
	 */
	public abstract Method callInfoGetMethod(OAObjectInfo oi, String methodName, int argumentCount);	
	/**
	 * Returns the master-to-detail property name for a detail Hub.
	 *
	 * @param thisHub Hub to inspect
	 * @return master-to-detail property name, or {@code null}
	 */
	public abstract String callHubDetailGetPropertyFromMasterToDetail(Hub<? extends OAObject> thisHub);	
	/**
	 * Returns detail-to-master link metadata for a Hub.
	 *
	 * @param hub Hub to inspect
	 * @return detail-to-master link metadata, or {@code null}
	 */
	public abstract OALinkInfo callHubDetailGetLinkInfoFromDetailToMaster(Hub<? extends OAObject> hub);
	/**
	 * Returns master-Hub-to-detail link metadata for a Hub.
	 *
	 * @param hub Hub to inspect
	 * @return master-to-detail link metadata, or {@code null}
	 */
	public abstract OALinkInfo callHubDetailGetLinkInfoFromMasterHubToDetail(Hub<? extends OAObject> hub);
	/**
	 * Returns Hub listeners that can participate in rules evaluation.
	 *
	 * @param hub Hub whose listeners are requested
	 * @return listener array, or {@code null}
	 */
	public abstract <T extends OAObject> HubListener<T>[] callHubEventGetAllListeners(Hub<T> hub);
	/**
	 * Returns whether the runtime is currently operating as a sync client.
	 *
	 * @return {@code true} for client runtime behavior
	 */
	public abstract boolean callSyncIsClient();
}
