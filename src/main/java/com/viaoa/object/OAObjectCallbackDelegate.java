/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.swing.JLabel;

import com.viaoa.annotation.OAObjCallback;
import com.viaoa.context.OAContext;
import com.viaoa.context.OAUserAccess;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubChangeListener;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubEventDelegate;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAObjectCallback.Type;
import com.viaoa.sync.OASync;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Allows OA to be able to control permission to object/hub, and allow other code/compenents to interact with objects. Works with OAObject
 * and Hub to determine what is allowed/permitted. Uses OAObject annoations, specific methods (onObjectCallback*, *Callback), and
 * HubListeners. Used to query objects, and find out if certain functions are enabled/visible/allowed, along with other interactive
 * settings/data. Used by OAObject (beforePropChange), Hub (add/remove/removeAll) to check if method is permitted/enabled. Used by
 * OAJfcController and Jfc to set UI components (enabled, visible, tooltip, rendering, etc)
 *
 * @see OAObjectCallback for list of types that can be used.
 * @see OAObjCallback annotation that lists proppaths and values used for enabled/visible.
 * @see OAAnnotationDelegate to see how class and annotation information is stored in Info objects (class/prop/calc/link/method)
 * @author vvia
 */
public class OAObjectCallbackDelegate {
	private static Logger LOG = Logger.getLogger(OAObjectCallbackDelegate.class.getName());

	public static boolean getAllowVisible(Hub hub, OAObject obj, String name) {
		return getAllowVisibleObjectCallback(hub, obj, name).getAllowed();
	}

	public static boolean getAllowEnabled(int checkType, Hub hub, OAObject obj, String name) {
		return getAllowEnabledObjectCallback(checkType, hub, obj, name).getAllowed();
	}

	public static boolean getAllowCopy(OAObject oaObj) {
		if (oaObj == null) {
			return false;
		}
		return getAllowCopyObjectCallback(oaObj).getAllowed();
	}

	public static OAObject getCopy(OAObject oaObj) {
		if (oaObj == null) {
			return null;
		}
		OAObjectCallback eq = getCopyObjectCallback(oaObj);

		Object objx = eq.getValue();
		if (!(objx instanceof OAObject)) {
			if (!eq.getAllowed()) {
				return null;
			}
			objx = oaObj.createCopy();
		}

		getAfterCopyObjectCallback(oaObj, (OAObject) objx);
		return (OAObject) objx;
	}
	/*
	public static void afterCopy(OAObject oaObj, OAObject oaObjCopy) {
	    if (oaObj == null || oaObjCopy == null) return;
	    getAfterCopyObjectCallback(oaObj, oaObjCopy);
	}
	*/

	// OAObjectCallback.CHECK_*
	public static boolean getVerifyPropertyChange(int checkType, OAObject obj, String propertyName, Object oldValue, Object newValue) {
		return getVerifyPropertyChangeObjectCallback(checkType, obj, propertyName, oldValue, newValue).getAllowed();
	}

	public static boolean getAllowAdd(Hub hub, OAObject obj, int checkType) {
		return getAllowAddObjectCallback(hub, obj, checkType).getAllowed();
	}

	public static boolean getVerifyAdd(Hub hub, OAObject obj, int checkType) {
		return getVerifyAddObjectCallback(hub, obj, checkType).getAllowed();
	}

	public static boolean getAllowRemove(Hub hub, OAObject obj, int checkType) {
		return getAllowRemoveObjectCallback(hub, obj, checkType).getAllowed();
	}

	public static boolean getVerifyRemove(Hub hub, OAObject obj, int checkType) {
		return getVerifyRemoveObjectCallback(hub, obj, checkType).getAllowed();
	}

	public static boolean getAllowRemoveAll(Hub hub, int checkType) {
		return getAllowRemoveAllObjectCallback(hub, checkType).getAllowed();
	}

	public static boolean getVerifyRemoveAll(Hub hub, int checkType) {
		return getVerifyRemoveAllObjectCallback(hub, checkType).getAllowed();
	}

	public static boolean getAllowDelete(Hub hub, OAObject obj) {
		return getAllowDeleteObjectCallback(hub, obj).getAllowed();
	}

	public static boolean getVerifyDelete(Hub hub, OAObject obj, int checkType) {
		return getVerifyDeleteObjectCallback(hub, obj, checkType).getAllowed();
	}

	public static boolean getAllowSave(OAObject obj, int checkType) {
		return getAllowSaveObjectCallback(obj, checkType).getAllowed();
	}

	public static boolean getVerifySave(OAObject obj, int checkType) {
		return getVerifySaveObjectCallback(obj, checkType).getAllowed();
	}

	/**
	 * Call oaobject class callback with type=AllowSubmit, and then calls each prop, owned lins
	 *
	 * @param obj
	 * @return
	 */
	public static OAObjectCallback getAllowSubmitObjectCallback(OAObject obj) {
		if (obj == null) {
			return null;
		}

		OAObjectCallback em = new OAObjectCallback(Type.AllowSubmit);
		_getAllowSubmit(em, obj, new OACascade());

		return em;
	}

	private static void _getAllowSubmit(final OAObjectCallback em, final OAObject obj, final OACascade cascade) {
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

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(obj.getClass());
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			Object val = obj.getProperty(pi.getName());
			final OAObjectCallback emx = new OAObjectCallback(Type.VerifyPropertyChange, OAObjectCallback.CHECK_ALL, null, null, obj,
					pi.getName(), val);

			if (val instanceof String) {
				if (((String) val).length() > pi.getMaxLength()) {
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
						_getAllowSubmit(em, (OAObject) li.getValue(objx), cascade);
						em.setObject(obj);
						if (!em.isAllowed() || em.getThrowable() != null) {
							break;
						}
					}
				}
			}
		}
	}

	public static String getFormat(OAObject obj, String propertyName, String defaultFormat) {
		OAObjectCallback em = new OAObjectCallback(Type.GetFormat);
		em.setObject(obj);
		em.setFormat(defaultFormat);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
		return em.getFormat();
	}

	public static String getToolTip(OAObject obj, String propertyName, String defaultToolTip) {
		OAObjectCallback em = new OAObjectCallback(Type.GetToolTip);
		em.setObject(obj);
		em.setToolTip(defaultToolTip);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
		return em.getToolTip();
	}

	public static void renderLabel(OAObject obj, String propertyName, JLabel label) {
		OAObjectCallback em = new OAObjectCallback(Type.RenderLabel);
		em.setObject(obj);
		em.setLabel(label);
		callObjectCallbackMethod(em);
		em.setPropertyName(propertyName);
		callObjectCallbackMethod(em);
	}

	public static void updateLabel(OAObject obj, String propertyName, JLabel label) {
		OAObjectCallback em = new OAObjectCallback(Type.UpdateLabel);
		em.setObject(obj);
		em.setPropertyName(propertyName);
		em.setLabel(label);
		callObjectCallbackMethod(em);
	}

	public static OAObjectCallback getAllowVisibleObjectCallback(Hub hub, OAObject oaObj, String name) {
		if (hub == null && oaObj == null) {
			return null;
		}
		if (oaObj == null) {
			if (name == null) {
				name = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
				oaObj = hub.getMasterObject();
			} else {
				oaObj = (OAObject) hub.getAO();
			}
		}
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowVisible, OAObjectCallback.CHECK_ALL, hub, null, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getAllowEnabledObjectCallback(final int checkType, final Hub hub, OAObject oaObj, String name) {
		if (hub == null && oaObj == null) {
			return null;
		}
		if (oaObj == null) {
			if (name == null) {
				name = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
				oaObj = hub.getMasterObject();
			} else {
				oaObj = (OAObject) hub.getAO();
			}
		}
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled, checkType, hub, null, oaObj, name, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getAllowEnabledObjectCallback(Hub hub) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowEnabled);

		OAObject objMaster = hub.getMasterObject();
		if (objMaster == null) {
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
			objectCallback.setPropertyName(propertyName);
			objectCallback.setObject(objMaster);
			processObjectCallback(objectCallback);
		}
		return objectCallback;
	}

	public static OAObjectCallback getAllowCopyObjectCallback(final OAObject oaObj) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getCopyObjectCallback(final OAObject oaObj) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.GetCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getAfterCopyObjectCallback(final OAObject oaObj, final OAObject oaObjCopy) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AfterCopy, OAObjectCallback.CHECK_ALL, null, null, oaObj, null,
				oaObjCopy);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getVerifyPropertyChangeObjectCallback(final int checkType, final OAObject oaObj,
			final String propertyName,
			final Object oldValue, final Object newValue) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyPropertyChange, checkType, null, null, oaObj, propertyName,
				newValue);
		objectCallback.setOldValue(oldValue);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getVerifyCommandObjectCallback(final OAObject oaObj, final String methodName, int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifyCommand, checkType, null, null, oaObj, methodName, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static void updateEditProcessed(OAObjectCallback objectCallback) {
		if (objectCallback == null) {
			return;
		}
		if (!OAContext.getAllowEditProcessed()) {
			String sx = OAContext.getAllowEditProcessedPropertyPath();
			objectCallback.setResponse("User." + sx + "=false");
			objectCallback.setAllowed(false);
		}
	}

	public static OAObjectCallback getAllowAddObjectCallback(final Hub hub, OAObject objAdd, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;
		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowAdd, checkType, hub, null, null, null, objAdd);
			if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getVerifyAddObjectCallback(final Hub hub, final OAObject oaObj, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
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

	public static OAObjectCallback getAllowNewObjectCallback(final Class clazz) {
		if (clazz == null) {
			return null;
		}
		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		int ct = (OAObjectCallback.CHECK_Processed | OAObjectCallback.CHECK_UserEnabledProperty);
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowNew, ct, null, clazz, null, null, null);

		if (oi.getProcessed()) {
			updateEditProcessed(objectCallback);
		}
		if (objectCallback.getAllowed()) {
			String pp = oi.getContextEnabledProperty();
			if (OAString.isNotEmpty(pp)) {
				if (!OAContext.isEnabled(pp, oi.getContextEnabledValue())) {
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + clazz.getSimpleName() + ", ";
					OAObject user = OAContext.getContextObject();
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

	public static OAObjectCallback getAllowNewObjectCallback(final Hub hub) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
			processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
		} else {
			OALinkInfo liRev = li.getReverseLinkInfo();
			if (liRev != null && !liRev.getCalculated()) {
				objectCallback = new OAObjectCallback(Type.AllowNew, OAObjectCallback.CHECK_ALL, hub, null, objMaster, liRev.getName(),
						null);
				processObjectCallback(objectCallback);
			} else {
				objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		}
		return objectCallback;
	}

	public static OAObjectCallback getAllowRemoveObjectCallback(final Hub hub, final OAObject objRemove, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
			if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getVerifyRemoveObjectCallback(final Hub hub, final OAObject objRemove, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemove, checkType, hub, null, null, null, objRemove);
			if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getAllowRemoveAllObjectCallback(final Hub hub, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.AllowRemoveAll, checkType, hub, null, null, null, null);
			if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getVerifyRemoveAllObjectCallback(final Hub hub, final int checkType) {
		if (hub == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
		OAObject objMaster = hub.getMasterObject();

		OAObjectCallback objectCallback = null;

		if (li == null || (li.getPrivateMethod() && objMaster == null)) {
			objectCallback = new OAObjectCallback(Type.VerifyRemoveAll, checkType, hub, null, null, null, null);
			if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getAllowSaveObjectCallback(final OAObject oaObj, final int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowSave, checkType, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getVerifySaveObjectCallback(final OAObject oaObj, final int checkType) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.VerifySave, checkType, null, null, oaObj, null, null);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getAllowDeleteObjectCallback(final OAObject objDelete) {
		if (objDelete == null) {
			return null;
		}

		final Class clazz = objDelete.getClass();
		if (clazz == null) {
			return null;
		}
		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		int ct = (OAObjectCallback.CHECK_Processed | OAObjectCallback.CHECK_UserEnabledProperty);
		OAObjectCallback objectCallback = new OAObjectCallback(Type.AllowDelete, ct, null, clazz, null, null, objDelete);

		if (oi.getProcessed()) {
			updateEditProcessed(objectCallback);
		}
		if (objectCallback.getAllowed()) {
			String pp = oi.getContextEnabledProperty();
			if (OAString.isNotEmpty(pp)) {
				if (!OAContext.isEnabled(pp, oi.getContextEnabledValue())) {
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + clazz.getSimpleName() + ", ";
					OAObject user = OAContext.getContextObject();
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

	public static OAObjectCallback getAllowDeleteObjectCallback(final Hub hub, final OAObject objDelete) {
		if (hub == null || objDelete == null) {
			return null;
		}

		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
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
				objectCallback = getAllowNewObjectCallback(hub.getObjectClass());
				processObjectCallbackForHubListeners(objectCallback, hub, null, null, null, null);
			}
		}
		return objectCallback;
	}

	public static OAObjectCallback getVerifyDeleteObjectCallback(final Hub hub, final OAObject objDelete, final int checkType) {
		OAObjectCallback objectCallback = null;
		if (hub != null) {
			OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(hub);
			OAObject objMaster = hub.getMasterObject();

			if (li == null || (li.getPrivateMethod() && objMaster == null)) {
				objectCallback = new OAObjectCallback(Type.VerifyDelete, checkType, hub, null, null, null, objDelete);
				if ((checkType & OAObjectCallback.CHECK_Processed) > 0) {
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

	public static OAObjectCallback getConfirmPropertyChangeObjectCallback(final OAObject oaObj, String property, Object newValue,
			String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForPropertyChange, OAObjectCallback.CHECK_ALL, null,
				null,
				oaObj, property, newValue);
		//qqqqqqqqqqqqqqqqqqq needs to include OldValue qqqqqqqqqqqqq
		objectCallback.setValue(newValue);
		objectCallback.setPropertyName(property);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getConfirmCommandObjectCallback(final OAObject oaObj, String methodName, String confirmMessage,
			String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForCommand, OAObjectCallback.CHECK_ALL, null, null,
				oaObj,
				methodName, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getConfirmSaveObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForSave, OAObjectCallback.CHECK_ALL, null, null, oaObj,
				null, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);

		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getConfirmDeleteObjectCallback(final OAObject oaObj, String confirmMessage, String confirmTitle) {
		final OAObjectCallback objectCallback = new OAObjectCallback(Type.SetConfirmForDelete, OAObjectCallback.CHECK_ALL, null, null,
				oaObj,
				null, null);
		objectCallback.setConfirmMessage(confirmMessage);
		objectCallback.setConfirmTitle(confirmTitle);
		processObjectCallback(objectCallback);
		return objectCallback;
	}

	public static OAObjectCallback getConfirmRemoveObjectCallback(final Hub hub, final OAObject oaObj, String confirmMessage,
			String confirmTitle) {
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
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

	public static OAObjectCallback getConfirmAddObjectCallback(final Hub hub, final OAObject oaObj, String confirmMessage,
			String confirmTitle) {
		OAObjectCallback objectCallback;
		OAObject objMaster = hub.getMasterObject();
		if (objMaster != null) {
			String propertyName = HubDetailDelegate.getPropertyFromMasterToDetail(hub);
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

	/*qqqqqqq
	protected static void processObjectCallback(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName, final Object oldValue, final Object newValue) {
	    processObjectCallback(objectCallback, null, null, oaObj, propertyName, oldValue, newValue, false);
	}
	protected static void processObjectCallback(OAObjectCallback objectCallback, final Class<? extends OAObject> clazz, final String propertyName, final Object oldValue, final Object newValue) {
	    processObjectCallback(objectCallback, null, clazz, null, propertyName, oldValue, newValue, false);
	}
	*/
	protected static void processObjectCallback(OAObjectCallback objectCallback) {
		_processObjectCallback(objectCallback);
		if (DEMO_AllowAllToPass) {
			objectCallback.setThrowable(null);
			objectCallback.setAllowed(true);
		} else if ((!objectCallback.getAllowed() || objectCallback.getThrowable() != null)) {
			// allow AppUser.admin=true to always be valid
			if (OAContext.isSuperAdmin()) { // allow all if super admin
				objectCallback.setThrowable(null);
				objectCallback.setAllowed(true);
			}
		}
	}

	private static boolean DEMO_AllowAllToPass;

	public static void demoAllowAllToPass(boolean b) {
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
		OAObjectCallbackDelegate.DEMO_AllowAllToPass = b;
	}

	/**
	 * This will process an Edit Query, calling objectCallback methods on OAObject, properties, links, methods (depending on type of edit
	 * query) used by: OAJfcController to see if an UI component should be enabled OAObjetEventDelegate.fireBeforePropertyChange Hub
	 * add/remove/removeAll OAJaxb
	 */
	protected static void _processObjectCallback(final OAObjectCallback objectCallback) {
		final Hub hubThis = objectCallback.getHub();
		final Class clazz = objectCallback.getCalcClass();
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

		final OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		if (bCheckProcessedCheck) {
			if (objectCallback.getType() == Type.AllowDelete && value != null && OAString.isEmpty(propertyName)) {
				OAObjectInfo oix = OAObjectInfoDelegate.getOAObjectInfo(value.getClass());
				if (oix.getProcessed()) {
					updateEditProcessed(objectCallback);
				}
			}
		}

		// 20200217 add OAUserAccess
		if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType() == Type.AllowVisible) {
			final OAUserAccess userAccess = OAContext.getContextUserAccess();
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
				&& (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst
						|| objectCallback.getType() == Type.AllowVisible)) {
			OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterHubToDetail(hubThis);
			if (li != null && !li.getOwner()) {
				OAObject objx = hubThis.getMasterObject();
				if (objx != null) {
					if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst) {
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

		if (oaObj != null && objectCallback.getType().checkOwner) {
			ownerHierProcess(objectCallback, oaObj, propertyName);
		}
		if (bCheckProcessedCheck && oi.getProcessed() && OAString.isEmpty(propertyName) && objectCallback.getAllowed()
				&& ((objectCallback.getType() == Type.AllowEnabled) || objectCallback.getType().checkEnabledFirst)) {
			updateEditProcessed(objectCallback);
		}

		if (oaObj != null && objectCallback.getType() == Type.AllowVisible && OAString.isNotEmpty(propertyName)
				&& objectCallback.isAllowed()
				&& oi.getHasOneAndOnlyOneLink()) {
			OALinkInfo li = oi.getLinkInfo(propertyName);
			if (li != null && li.getOneAndOnlyOne()) {
				if (OAObjectPropertyDelegate.getProperty(oaObj, propertyName) == null) {
					for (OALinkInfo lix : oi.getLinkInfos()) {
						if (lix == li || !lix.getOneAndOnlyOne()) {
							continue;
						}
						if (OAObjectPropertyDelegate.getProperty(oaObj, lix.getName()) != null) {
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
				Object valx = OAObjectReflectDelegate.getProperty(oaObj, sx);
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
				OAObject user = OAContext.getContextObject();
				if (user == null) {
					if (!OASync.isServer()) {
						objectCallback.setAllowed(false);
					}
				} else {
					Object valx = OAObjectReflectDelegate.getProperty(user, sx);
					objectCallback.setAllowed(bx == OAConv.toBoolean(valx));
				}
				if (!objectCallback.getAllowed() && OAString.isEmpty(objectCallback.getResponse())) {
					objectCallback.setAllowed(false);
					String s = user == null ? "User" : user.getClass().getSimpleName();
					s = "Not visible, " + s + "." + sx + " is not " + bx;
					objectCallback.setResponse(s);
				}
			}
		} else if ((objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst)
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
				Object valx = OAObjectReflectDelegate.getProperty(oaObj, enabledName);
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
				boolean b = OAContext.isEnabled(enabledName, enabledValue);
				objectCallback.setAllowed(b);
				if (!b) {
					objectCallback.setAllowed(false);
					if (OAString.isEmpty(objectCallback.getResponse())) {
						objectCallback.setAllowed(false);
						OAObject user = OAContext.getContextObject();
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
		Hub[] hubs = OAObjectHubDelegate.getHubReferences(oaObj);

		// call the callback method, this can override eq.allowed
		if (OAString.isNotEmpty(propertyName) && objectCallback.getType().checkEnabledFirst) {
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
	 * Calls visible|enabled for this object and all of it's owner/parents
	 */
	protected static void ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName) {
		_ownerHierProcess(objectCallback, oaObj, propertyName, null);
	}

	protected static void _ownerHierProcess(OAObjectCallback objectCallback, final OAObject oaObj, final String propertyName,
			final OALinkInfo li) {
		if (oaObj == null) {
			return;
		}
		// recursive, goto top owner first
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObj);

		OALinkInfo lix = oi.getOwnedByOne();
		if (lix != null) {
			OAObject objOwner = (OAObject) lix.getValue(oaObj);
			if (objOwner != null) {
				lix = lix.getReverseLinkInfo();
				_ownerHierProcess(objectCallback, objOwner, lix.getName(), lix);
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
				valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
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
				OAObject user = OAContext.getContextObject();
				if (user == null) {
					if (!OASync.isServer()) {
						bPassed = false;
					}
				} else {
					valx = OAObjectReflectDelegate.getProperty(user, pp);
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
					valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
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
					OAObject user = OAContext.getContextObject();
					if (user == null) {
						if (!OASync.isServer()) {
							bPassed = false;
						}
					} else {
						valx = OAObjectReflectDelegate.getProperty(user, pp);
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
		} else if (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst) {
			//was:  else if ( (objectCallback.getType() == Type.AllowEnabled || objectCallback.getType().checkEnabledFirst) && !(OASync.isServer() && OAThreadLocalDelegate.getContext() == null)) {

			// final boolean bCheckProcessedCheck = (objectCallback.getCheckType() & OAObjectCallback.CHECK_Processed) != 0;
			final boolean bCheckEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_EnabledProperty) != 0;
			final boolean bCheckUserEnabledProperty = (objectCallback.getCheckType() & OAObjectCallback.CHECK_UserEnabledProperty) != 0;
			final boolean bCheckCallbackMethod = (objectCallback.getCheckType() & OAObjectCallback.CHECK_CallbackMethod) != 0;

			if (bPassed) {
				pp = oi.getEnabledProperty();
				if (OAString.isNotEmpty(pp) && bCheckEnabledProperty) {
					b = oi.getEnabledValue();
					valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
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
				if (!OAContext.isEnabled(pp, b)) {
					bPassed = false;
					objectCallback.setAllowed(false);
					String s = "Not enabled, user rule for " + oaObj.getClass().getSimpleName() + ", ";
					OAObject user = OAContext.getContextObject();
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
					valx = OAObjectReflectDelegate.getProperty(oaObj, pp);
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
					if (!OAContext.isEnabled(pp, b)) {
						OAObject user = OAContext.getContextObject();
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

	// called directly if hub.masterObject=null
	protected static void processObjectCallbackForHubListeners(OAObjectCallback objectCallback, final Hub hub, final OAObject oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		if (objectCallback.getType().checkEnabledFirst) {
			OAObjectCallback objectCallbackX = new OAObjectCallback(Type.AllowEnabled);
			objectCallbackX.setAllowed(objectCallback.getAllowed());
			objectCallbackX.setPropertyName(objectCallback.getPropertyName());
			_processObjectCallbackForHubListeners(objectCallbackX, hub, oaObj, propertyName, oldValue, newValue);
			objectCallback.setAllowed(objectCallbackX.getAllowed());
		}
		_processObjectCallbackForHubListeners(objectCallback, hub, oaObj, propertyName, oldValue, newValue);
	}

	protected static void _processObjectCallbackForHubListeners(OAObjectCallback objectCallback, final Hub hub, final OAObject oaObj,
			final String propertyName, final Object oldValue, final Object newValue) {
		HubListener[] hl = HubEventDelegate.getAllListeners(hub);
		if (hl == null) {
			return;
		}
		int x = hl.length;
		if (x == 0) {
			return;
		}
		final boolean bBefore = objectCallback.getAllowed();

		HubEvent hubEvent = null;
		try {
			for (int i = 0; i < x; i++) {
				boolean b = objectCallback.getAllowed();

				switch (objectCallback.getType()) {
				case AllowEnabled:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj, propertyName);
					}
					b = hl[i].getAllowEnabled(hubEvent, b);
					break;
				case AllowVisible:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, oaObj, propertyName);
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
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowAdd(hubEvent, b);
					break;
				case AllowAdd:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub);
					}
					b = hl[i].getAllowAdd(hubEvent, b);
					break;
				case VerifyAdd:
					if (hubEvent == null) {
						hubEvent = new HubEvent(hub, newValue);
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
						hubEvent = new HubEvent(hub, newValue);
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
						hubEvent = new HubEvent(hub, newValue);
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
						hubEvent = new HubEvent(hub, newValue);
					}
					b = hl[i].isValidDelete(hubEvent, b);
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

	protected static void callObjectCallbackMethod(final OAObjectCallback em) {
		callObjectCallbackMethod(em.getObject(), em.getPropertyName(), em);
	}

	protected static void callObjectCallbackMethod(final Object object, String propertyName, final OAObjectCallback em) {
		if (object == null) {
			return;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(object.getClass());

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
	 * Used by OAObjectModel objects to allow model object to be updated after it is created by calling ObjectCallback method.
	 *
	 * @param          clazz, ex: from SalesOrderModel, SalesOrder.class
	 * @param property ex: "SalesOrderItems"
	 * @param model    ex: SalesOrderItemModel
	 */
	public static void onObjectCallbackModel(Class clazz, String property, OAObjectModel model) {
		if (clazz == null || OAString.isEmpty(property) || model == null) {
			return;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		Method m = OAObjectInfoDelegate.getMethod(oi, "onObjectCallback" + property + "Model", 1);
		if (m == null) {
			m = OAObjectInfoDelegate.getMethod(oi, property + "ModelCallback", 1);
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
	 * Used by HubChangedListener.addXxx to listen to dependencies found for an ObjectCallback.
	 */
	public static void addObjectCallbackChangeListeners(final Hub hub, final Class cz, final String prop, String ppPrefix,
			final HubChangeListener changeListener, final boolean bEnabled) {
		if (ppPrefix == null) {
			ppPrefix = "";
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getObjectInfo(cz);
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

		final Hub hubUser = OAContext.getContextHub();
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

	protected static void addDependentProps(Hub hub, String prefix, String[] viewDependentProperties, String[] contextDependentProperties,
			boolean bProcessed, HubChangeListener changeListener) {
		if (viewDependentProperties != null) {
			for (String s : viewDependentProperties) {
				changeListener.add(hub, prefix + s);
			}
		}
		if (contextDependentProperties != null) {
			Hub hubUser = OAContext.getContextHub();
			if (contextDependentProperties.length > 0 && hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			for (String s : contextDependentProperties) {
				changeListener.add(hubUser, s);
			}
		}
		if (bProcessed) {
			Hub hubUser = OAContext.getContextHub();
			if (hubUser == null) {
				changeListener.addAlwaysFalse(hub);
			}
			changeListener.add(hubUser, OAContext.getAllowEditProcessedPropertyPath());
		}
	}
}
